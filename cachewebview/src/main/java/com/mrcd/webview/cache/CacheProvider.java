package com.mrcd.webview.cache;

import android.webkit.WebResourceResponse;

import com.mrcd.webview.cache.interceptor.CacheInterceptor;

/**
 * 缓存获取器
 */
public interface CacheProvider {

    WebResourceResponse get(CacheRequest request);

    void addResourceInterceptor(CacheInterceptor interceptor);

    void destroy();
}
