package com.mrcd.webview.config;

/**
 * 过滤一些 mime 类型的资源。
 */
public interface MimeTypeFilter {

    /**
     * 需要保留
     */
    boolean shouldRetain(String mimeType);

    /**
     * 需要拒绝
     */
    boolean shouldReject(String mimeType);

    void addMimeType(String mimeType);

    void removeMimeType(String mimeType);

    void clear();

}
