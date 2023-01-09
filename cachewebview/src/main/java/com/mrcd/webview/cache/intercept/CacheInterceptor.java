package com.mrcd.webview.cache.intercept;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.WebResource;

/**
 * 缓存连接器
 */
public interface CacheInterceptor {
    /**
     * 加载缓存: 注意此方法运行在非 UI 线程
     */
    @WorkerThread
    WebResource load(Chain chain);
}
