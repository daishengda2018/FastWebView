package com.mrcd.webview;

import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.CacheMode;
import com.mrcd.webview.cache.interceptor.CacheInterceptor;

/**
 * Created by Ryan
 * at 2019/11/1
 */
public interface FastOpenApi {

    void setCacheMode(CacheMode mode, CacheConfig cacheConfig);

    void addResourceInterceptor(CacheInterceptor interceptor);
}
