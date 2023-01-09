package com.mrcd.webview;

import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.FastCacheMode;
import com.mrcd.webview.cache.interceptor.CacheInterceptor;

/**
 * Created by Ryan
 * at 2019/11/1
 */
public interface FastOpenApi {

    void setCacheMode(FastCacheMode mode, CacheConfig cacheConfig);

    void addResourceInterceptor(CacheInterceptor interceptor);
}
