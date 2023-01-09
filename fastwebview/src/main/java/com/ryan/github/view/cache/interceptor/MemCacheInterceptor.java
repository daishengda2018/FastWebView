package com.ryan.github.view.cache.interceptor;

import android.util.LruCache;

import com.ryan.github.view.WebResource;
import com.ryan.github.view.config.CacheConfig;
import com.ryan.github.view.cache.CacheRequest;
import com.ryan.github.view.cache.Chain;
import com.ryan.github.view.cache.Destroyable;

/**
 * 内存资源
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
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        if (mLruCache != null) {
            WebResource resource = mLruCache.get(request.getKey());
            if (checkResourceValid(resource)) {
                return resource;
            }
        }
        WebResource resource = chain.process(request);
        if (mLruCache != null && checkResourceValid(resource) && resource.isCacheable()) {
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
