package com.ryan.github.view.cache.interceptor;

import com.ryan.github.view.WebResource;
import com.ryan.github.view.cache.Chain;

/**
 * 缓存连接器
 */
public interface CacheInterceptor {
    /**
     * 加载缓存
     */
    WebResource load(Chain chain);
}
