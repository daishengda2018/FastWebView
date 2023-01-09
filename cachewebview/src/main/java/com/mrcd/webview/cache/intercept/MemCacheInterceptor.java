package com.mrcd.webview.cache.intercept;

import android.util.LruCache;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.CacheRequest;
import com.mrcd.webview.cache.Destroyable;
import com.mrcd.webview.config.CacheConfig;

/**
 * 内存缓存
 */
public class MemCacheInterceptor implements CacheInterceptor, Destroyable {

    private LruCache<String, WebResource> mLruCache;
    private static volatile MemCacheInterceptor sInstance;

    public static MemCacheInterceptor getInstance(CacheConfig cacheConfig) {
        if (sInstance == null) {
            synchronized (MemCacheInterceptor.class) {
                if (sInstance == null) {
                    sInstance = new MemCacheInterceptor(cacheConfig);
                }
            }
        }
        return sInstance;
    }

    private MemCacheInterceptor(CacheConfig cacheConfig) {
        int memorySize = cacheConfig.getMemCacheSize();
        if (memorySize > 0) {
            mLruCache = new ResourceMemCache(memorySize);
        }
    }

    @Override
    @WorkerThread
    public WebResource load(Chain chain) {
        final CacheRequest request = chain.getRequest();
        if (mLruCache != null) {
            // LruCache 是线程安全的
            WebResource resource = mLruCache.get(request.getKey());
            if (checkResourceValid(resource)) {
                // 命中缓存，直接返回
                return resource;
            }
        }

        WebResource resource = chain.process(request);
        if (mLruCache != null && checkResourceValid(resource) && resource.isCacheable()) {
            // 添加到内存缓存
            mLruCache.put(request.getKey(), resource);
        }
        return resource;
    }

    private boolean checkResourceValid(WebResource resource) {
        return resource != null
                && resource.getOriginBytes() != null
                && resource.getOriginBytes().length >= 0
                && resource.getResponseHeaders() != null
                && !resource.getResponseHeaders().isEmpty();
    }

    @Override
    public void destroy() {
        if (mLruCache != null) {
            mLruCache.evictAll();
            mLruCache = null;
        }
    }

    private static class ResourceMemCache extends LruCache<String, WebResource> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        ResourceMemCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, WebResource value) {
            int size = 0;
            if (value != null && value.getOriginBytes() != null) {
                size = value.getOriginBytes().length;
            }
            return size;
        }
    }
}
