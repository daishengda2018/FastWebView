package com.ryan.github.view.cache;

import android.webkit.WebResourceResponse;

import com.ryan.github.view.cache.interceptor.CacheInterceptor;

/**
 * 缓存获取器
 */
public interface CacheProvider {

    WebResourceResponse get(CacheRequest request);

    void addResourceInterceptor(CacheInterceptor interceptor);

    void destroy();
}
