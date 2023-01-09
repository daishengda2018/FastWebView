package com.mrcd.webview.cache.intercept;

import android.text.TextUtils;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.CacheRequest;
import com.mrcd.webview.cache.Destroyable;
import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.utils.HeaderUtils;
import com.mrcd.webview.utils.LogUtils;
import com.mrcd.webview.utils.StreamUtils;
import com.mrcd.webview.utils.lru.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import okhttp3.Headers;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class DiskCacheInterceptor implements Destroyable, CacheInterceptor {

    private static final int ENTRY_META = 0;
    private static final int ENTRY_BODY = 1;
    private static final int ENTRY_COUNT = 2;
    private DiskLruCache mDiskLruCache;
    private final CacheConfig mCacheConfig;

    public DiskCacheInterceptor(CacheConfig cacheConfig) {
        mCacheConfig = cacheConfig;
    }

    @Override
    @WorkerThread
    public WebResource load(Chain chain) {
        final CacheRequest request = chain.getRequest();
        ensureDiskLruCacheCreate();
        // DiskLruCache 内部使用唯一线程驱动，所以线程安全
        WebResource webResource = getFromDiskCache(request.getKey());
        if (isRealMimeTypeCacheable(webResource)) {
            LogUtils.d(String.format("disk cache hit: %s", request.getUrl()));
            return webResource;
        }

        webResource = chain.process(request);
        if (webResource != null && (webResource.isCacheByOurselves() || isRealMimeTypeCacheable(webResource))) {
            cacheToDisk(request.getKey(), webResource);
        }
        return webResource;
    }

    private synchronized void ensureDiskLruCacheCreate() {
        if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
            return;
        }
        final String dir = mCacheConfig.getCacheDir();
        final int version = mCacheConfig.getVersion();
        final long cacheSize = mCacheConfig.getDiskCacheSize();
        try {
            mDiskLruCache = DiskLruCache.open(new File(dir), version, ENTRY_COUNT, cacheSize);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private WebResource getFromDiskCache(String key) {
        try {
            if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                return null;
            }
            final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                final BufferedSource entrySource = Okio.buffer(Okio.source(snapshot.getInputStream(ENTRY_META)));
                // 1. read status
                final String responseCode = entrySource.readUtf8LineStrict();
                final String reasonPhrase = entrySource.readUtf8LineStrict();
                // 2. read headers
                long headerSize = entrySource.readDecimalLong();
                final Headers.Builder responseHeadersBuilder = new Headers.Builder();
                // read first placeholder line
                final String placeHolder = entrySource.readUtf8LineStrict();
                if (!TextUtils.isEmpty(placeHolder.trim())) {
                    responseHeadersBuilder.add(placeHolder);
                    headerSize--;
                }
                for (int i = 0; i < headerSize; i++) {
                    String line = entrySource.readUtf8LineStrict();
                    if (!TextUtils.isEmpty(line)) {
                        responseHeadersBuilder.add(line);
                    }
                }
                Map<String, String> headers = HeaderUtils.generateHeadersMap(responseHeadersBuilder.build());
                // 3. read body
                InputStream inputStream = snapshot.getInputStream(ENTRY_BODY);
                if (inputStream != null) {
                    WebResource webResource = new WebResource();
                    webResource.setReasonPhrase(reasonPhrase);
                    webResource.setResponseCode(Integer.parseInt(responseCode));
                    webResource.setOriginBytes(StreamUtils.streamToBytes(inputStream));
                    webResource.setResponseHeaders(headers);
                    webResource.setModified(false);
                    return webResource;
                }
                snapshot.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void cacheToDisk(String key, WebResource webResource) {
        if (webResource == null || !webResource.isCacheable()) {
            return;
        }
        if (mDiskLruCache.isClosed()) {
            return;
        }
        try {
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor == null) {
                LogUtils.d("Another edit is in progress!");
                return;
            }
            OutputStream metaOutput = editor.newOutputStream(ENTRY_META);
            BufferedSink sink = Okio.buffer(Okio.sink(metaOutput));
            // 1. write status
            sink.writeUtf8(String.valueOf(webResource.getResponseCode())).writeByte('\n');
            sink.writeUtf8(webResource.getReasonPhrase()).writeByte('\n');
            // 2. write response header
            Map<String, String> headers = webResource.getResponseHeaders();
            sink.writeDecimalLong(headers.size()).writeByte('\n');
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String headerKey = entry.getKey();
                String headerValue = entry.getValue();
                sink.writeUtf8(headerKey)
                        .writeUtf8(": ")
                        .writeUtf8(headerValue)
                        .writeByte('\n');
            }
            sink.flush();
            sink.close();
            // 3. write response body
            OutputStream bodyOutput = editor.newOutputStream(ENTRY_BODY);
            sink = Okio.buffer(Okio.sink(bodyOutput));
            byte[] originBytes = webResource.getOriginBytes();
            if (originBytes != null && originBytes.length > 0) {
                sink.write(originBytes);
                sink.flush();
                editor.commit();
            }
            sink.close();
        } catch (IOException e) {
            LogUtils.e("cache to disk failed. cause by: " + e.getMessage());
            try {
                // clean the redundant data
                mDiskLruCache.remove(key);
            } catch (IOException ignore) {
            }
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }
    }

    private boolean isRealMimeTypeCacheable(WebResource resource) {
        if (resource == null) {
            return false;
        }
        Map<String, String> headers = resource.getResponseHeaders();
        String contentType = null;
        if (headers != null) {
            String uppercaseKey = "Content-Type";
            String lowercaseKey = uppercaseKey.toLowerCase();
            String contentTypeValue = headers.containsKey(uppercaseKey) ? headers.get(uppercaseKey) : headers.get(lowercaseKey);
            if (!TextUtils.isEmpty(contentTypeValue)) {
                String[] contentTypeArray = contentTypeValue.split(";");
                if (contentTypeArray.length >= 1) {
                    contentType = contentTypeArray[0];
                }
            }
        }
        return contentType != null && !mCacheConfig.getFilter().shouldRetain(contentType);
    }

    @Override
    public void destroy() {
        if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
            try {
                mDiskLruCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

