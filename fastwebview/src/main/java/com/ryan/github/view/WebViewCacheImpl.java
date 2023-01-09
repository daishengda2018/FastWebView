package com.ryan.github.view;

import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.ryan.github.view.cache.CacheProvider;
import com.ryan.github.view.cache.CacheProviderImpl;
import com.ryan.github.view.cache.CacheRequest;
import com.ryan.github.view.cache.interceptor.CacheInterceptor;
import com.ryan.github.view.config.CacheConfig;
import com.ryan.github.view.config.FastCacheMode;
import com.ryan.github.view.utils.MimeTypeMapUtils;

import java.util.Map;

/**
 * Created by Ryan
 * 2018/2/7 下午5:07
 */
public class WebViewCacheImpl implements WebViewCache {

    private FastCacheMode mFastCacheMode;
    private CacheConfig mCacheConfig;
    private CacheProvider mCacheProvider;
    private Context mContext;

    WebViewCacheImpl(Context context) {
        mContext = context;
    }

    @Override
    public WebResourceResponse getResource(WebResourceRequest webResourceRequest, int cacheMode, String userAgent) {
        if (mFastCacheMode == FastCacheMode.DEFAULT) {
            return null;
        }
        final String url = webResourceRequest.getUrl().toString();
        final String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
        final String mimeType = MimeTypeMapUtils.getMimeTypeFromExtension(extension);
        final Map<String, String> headers = webResourceRequest.getRequestHeaders();
        CacheRequest cacheRequest = new CacheRequest();
        cacheRequest.setUrl(url);
        cacheRequest.setMime(mimeType);
        cacheRequest.setForceMode(mFastCacheMode == FastCacheMode.FORCE);
        cacheRequest.setUserAgent(userAgent);
        cacheRequest.setWebViewCacheMode(cacheMode);
        cacheRequest.setHeaders(headers);
        return getCacheProvider().get(cacheRequest);
    }

    @Override
    public void setCacheMode(FastCacheMode mode, CacheConfig cacheConfig) {
        mFastCacheMode = mode;
        mCacheConfig = cacheConfig;
    }

    @Override
    public void addResourceInterceptor(CacheInterceptor interceptor) {
        getCacheProvider().addResourceInterceptor(interceptor);
    }

    private synchronized CacheProvider getCacheProvider() {
        if (mCacheProvider == null) {
            mCacheProvider = new CacheProviderImpl(mContext, getCacheConfig());
        }
        return mCacheProvider;
    }

    private CacheConfig getCacheConfig() {
        return mCacheConfig != null ? mCacheConfig : generateDefaultCacheConfig();
    }

    private CacheConfig generateDefaultCacheConfig() {
        return new CacheConfig.Builder(mContext).build();
    }

    @Override
    public void destroy() {
        if (mCacheProvider != null) {
            mCacheProvider.destroy();
        }
        // help gc
        mCacheConfig = null;
        mCacheProvider = null;
        mContext = null;
    }
}