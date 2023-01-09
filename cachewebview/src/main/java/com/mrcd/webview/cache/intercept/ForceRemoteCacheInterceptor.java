package com.mrcd.webview.cache.intercept;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.CacheRequest;
import com.mrcd.webview.cache.Destroyable;
import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.MimeTypeFilter;
import com.mrcd.webview.loader.OkHttpResourceLoader;
import com.mrcd.webview.loader.ResourceLoader;
import com.mrcd.webview.loader.SourceRequest;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class ForceRemoteCacheInterceptor implements Destroyable, CacheInterceptor {

    private final ResourceLoader mResourceLoader;
    private final MimeTypeFilter mMimeTypeFilter;

    public ForceRemoteCacheInterceptor(Context context, CacheConfig cacheConfig) {
        mResourceLoader = new OkHttpResourceLoader(context);
        mMimeTypeFilter = cacheConfig != null ? cacheConfig.getFilter() : null;
    }

    @Override
    @WorkerThread
    public WebResource load(Chain chain) {
        final CacheRequest request = chain.getRequest();
        final String mime = request.getMime();
        final boolean isCacheable = TextUtils.isEmpty(mime) ? shouldCacheHtml() : mMimeTypeFilter.shouldRetain(mime);
        final SourceRequest sourceRequest = new SourceRequest(request, isCacheable);
        final WebResource resource = mResourceLoader.loadResource(sourceRequest);
        if (resource != null) {
            return resource;
        }
        return chain.process(request);
    }

    @Override
    public void destroy() {
        if (mMimeTypeFilter != null) {
            mMimeTypeFilter.clear();
        }
    }

    private boolean shouldCacheHtml() {
        return mMimeTypeFilter.shouldRetain("text/html");
    }
}
