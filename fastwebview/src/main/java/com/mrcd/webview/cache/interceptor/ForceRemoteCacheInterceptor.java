package com.mrcd.webview.cache.interceptor;

import android.content.Context;
import android.text.TextUtils;

import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.MimeTypeFilter;
import com.mrcd.webview.WebResource;
import com.mrcd.webview.loader.OkHttpResourceLoader;
import com.mrcd.webview.loader.ResourceLoader;
import com.mrcd.webview.loader.SourceRequest;
import com.mrcd.webview.cache.CacheRequest;
import com.mrcd.webview.cache.Chain;
import com.mrcd.webview.cache.Destroyable;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class ForceRemoteCacheInterceptor implements Destroyable, CacheInterceptor {

    private ResourceLoader mResourceLoader;
    private MimeTypeFilter mMimeTypeFilter;

    public ForceRemoteCacheInterceptor(Context context, CacheConfig cacheConfig) {
        mResourceLoader = new OkHttpResourceLoader(context);
        mMimeTypeFilter = cacheConfig != null ? cacheConfig.getFilter() : null;
    }

    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        String mime = request.getMime();
        boolean isFilter;
        if (TextUtils.isEmpty(mime)) {
            isFilter = isFilterHtml();
        } else {
            isFilter = mMimeTypeFilter.isFilter(mime);
        }
        SourceRequest sourceRequest = new SourceRequest(request, isFilter);
        WebResource resource = mResourceLoader.loadResource(sourceRequest);
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

    private boolean isFilterHtml() {
        return mMimeTypeFilter.isFilter("text/html");
    }
}
