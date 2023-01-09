package com.mrcd.webview.cache;

import android.webkit.WebResourceResponse;

import com.mrcd.webview.WebResource;

/**
 * Created by Ryan
 * at 2019/10/8
 */
public interface WebResourceResponseGenerator {

    WebResourceResponse generate(WebResource resource, String urlMime);

}
