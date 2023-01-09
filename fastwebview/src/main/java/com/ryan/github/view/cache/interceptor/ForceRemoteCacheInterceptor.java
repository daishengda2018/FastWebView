package com.ryan.github.view.cache.interceptor;

import android.content.Context;
import android.text.TextUtils;

import com.ryan.github.view.config.CacheConfig;
import com.ryan.github.view.config.MimeTypeFilter;
import com.ryan.github.view.WebResource;
import com.ryan.github.view.loader.OkHttpResourceLoader;
import com.ryan.github.view.loader.ResourceLoader;
import com.ryan.github.view.loader.SourceRequest;
import com.ryan.github.view.cache.CacheRequest;
import com.ryan.github.view.cache.Chain;
import com.ryan.github.view.cache.Destroyable;

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
