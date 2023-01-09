package com.ryan.github.view.cache.interceptor;

import android.content.Context;

import com.ryan.github.view.WebResource;
import com.ryan.github.view.cache.CacheRequest;
import com.ryan.github.view.cache.Chain;
import com.ryan.github.view.loader.OkHttpResourceLoader;
import com.ryan.github.view.loader.ResourceLoader;
import com.ryan.github.view.loader.SourceRequest;

/**
 * 网络缓存：直接通过网络获取
 */
public class NetworkCacheInterceptor implements CacheInterceptor {

    private final ResourceLoader mResourceLoader;

    public NetworkCacheInterceptor(Context context) {
        mResourceLoader = new OkHttpResourceLoader(context);
    }

    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        SourceRequest sourceRequest = new SourceRequest(request, true);
        WebResource resource = mResourceLoader.loadResource(sourceRequest);
        if (resource != null) {
            return resource;
        }
        return chain.process(request);
    }
}
