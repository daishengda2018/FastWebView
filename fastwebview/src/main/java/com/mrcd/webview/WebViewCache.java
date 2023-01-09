package com.mrcd.webview;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.mrcd.webview.cache.Destroyable;

import org.jetbrains.annotations.Nullable;

/**
 * Created by Ryan
 * 2018/2/7 下午5:06
 */
public interface WebViewCache extends FastOpenApi, Destroyable {

    @Nullable
    WebResourceResponse getResource(WebResourceRequest request, int webViewCacheMode, String userAgent);
}
