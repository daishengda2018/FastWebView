package com.mrcd.webview.cache.intercept;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.CacheRequest;
import com.mrcd.webview.loader.OkHttpResourceLoader;
import com.mrcd.webview.loader.ResourceLoader;
import com.mrcd.webview.loader.SourceRequest;

/**
 * 网络缓存：直接通过网络获取
 */
public class DefaultRemoteCacheInterceptor implements CacheInterceptor {

    private final ResourceLoader mResourceLoader;

    public DefaultRemoteCacheInterceptor(Context context) {
        mResourceLoader = new OkHttpResourceLoader(context);
    }

    @Override
    @WorkerThread
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
