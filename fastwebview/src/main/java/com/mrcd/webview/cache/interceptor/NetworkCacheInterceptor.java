package com.mrcd.webview.cache.interceptor;

import android.content.Context;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.CacheRequest;
import com.mrcd.webview.cache.Chain;
import com.mrcd.webview.loader.OkHttpResourceLoader;
import com.mrcd.webview.loader.ResourceLoader;
import com.mrcd.webview.loader.SourceRequest;

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
