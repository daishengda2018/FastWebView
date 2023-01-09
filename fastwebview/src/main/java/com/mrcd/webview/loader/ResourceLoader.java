package com.mrcd.webview.loader;

import com.mrcd.webview.WebResource;

/**
 * 资源加载
 */
public interface ResourceLoader {
    /**
     * 加载资源
     */
    WebResource loadResource(SourceRequest request);
}



