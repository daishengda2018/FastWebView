package com.mrcd.webview.config;

/**
 * 缓存模式
 */
public enum CacheMode {
    /*** 默认缓存模式，和原生 WebView 无任何差异，无任何侵入 */
    DEFAULT,
    /*** 强制缓存模式，切换为 OkHttp 加载资源，使用内存、磁盘、网络三级缓存、强制缓存不被过滤器过滤的资源*/
    FORCE
}
