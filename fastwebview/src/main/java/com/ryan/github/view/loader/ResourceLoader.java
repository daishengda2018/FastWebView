package com.ryan.github.view.loader;

import com.ryan.github.view.WebResource;

/**
 * 资源加载
 */
public interface ResourceLoader {
    /**
     * 加载资源
     */
    WebResource loadResource(SourceRequest request);
}



