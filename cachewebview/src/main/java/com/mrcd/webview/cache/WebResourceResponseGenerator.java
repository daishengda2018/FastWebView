package com.mrcd.webview.cache;

import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.utils.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by Ryan
 * at 2019/10/8
 */
public class WebResourceResponseGenerator {

    private static final String KEY_CONTENT_TYPE = "Content-Type";

    @WorkerThread
    public WebResourceResponse generate(WebResource resource, String urlMime) {
        if (resource == null) {
            return null;
        }
        final Map<String, String> headers = resource.getResponseHeaders();
        String contentType = null;
        String charset = null;
        if (headers != null) {
            String contentTypeValue = getContentType(headers, KEY_CONTENT_TYPE);
            if (!TextUtils.isEmpty(contentTypeValue)) {
                String[] contentTypeArray = contentTypeValue.split(";");
                if (contentTypeArray.length >= 1) {
                    contentType = contentTypeArray[0];
                }
                if (contentTypeArray.length >= 2) {
                    charset = contentTypeArray[1];
                    String[] charsetArray = charset.split("=");
                    if (charsetArray.length >= 2) {
                        charset = charsetArray[1];
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(contentType)) {
            urlMime = contentType;
        }
        if (TextUtils.isEmpty(urlMime)) {
            return null;
        }
        final byte[] resourceBytes = resource.getOriginBytes();
        if (resourceBytes == null) {
            return null;
        }
        if (resourceBytes.length == 0 && resource.getResponseCode() == 304) {
            LogUtils.d("the response bytes can not be empty if we get 304.");
            return null;
        }
        final InputStream bis = new ByteArrayInputStream(resourceBytes);
        final int status = resource.getResponseCode();
        String reasonPhrase = resource.getReasonPhrase();
        if (TextUtils.isEmpty(reasonPhrase)) {
            reasonPhrase = PhraseList.getPhrase(status);
        }
        return new WebResourceResponse(urlMime, charset, status, reasonPhrase, resource.getResponseHeaders(), bis);
    }

    private String getContentType(Map<String, String> headers, String key) {
        if (headers != null) {
            String value = headers.get(key);
            return value != null ? value : headers.get(key.toLowerCase());
        }
        return null;
    }
}
