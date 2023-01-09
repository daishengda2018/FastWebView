package com.mrcd.webview.cache.interceptor;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.Chain;

/**
 * 缓存连接器
 */
public interface CacheInterceptor {
    /**
     * 加载缓存
     */
    WebResource load(Chain chain);
}
