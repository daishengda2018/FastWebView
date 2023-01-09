package com.mrcd.webview;

import android.content.Context;
import android.content.MutableContextWrapper;

import androidx.core.util.Pools;

import com.mrcd.webview.utils.LogUtils;

/**
 * A simple webview instance pool.
 * Reduce webview initialization time about 100ms.
 * my test env: vivo-x23, android api: 8.1
 * <p>
 * Created by Ryan
 * at 2019/11/4
 */
public class WebViewPool {

    private static final int MAX_POOL_SIZE = 2;
    private static final Pools.Pool<CacheWebView> sPool = new Pools.SynchronizedPool<>(MAX_POOL_SIZE);

    public static void prepare(Context context) {
        release(acquire(context.getApplicationContext()));
    }

    public static CacheWebView acquire(Context context) {
        CacheWebView webView = sPool.acquire();
        if (webView == null) {
            MutableContextWrapper wrapper = new MutableContextWrapper(context);
            webView = new CacheWebView(wrapper);
            LogUtils.d("create new webview instance.");
        } else {
            MutableContextWrapper wrapper = (MutableContextWrapper) webView.getContext();
            wrapper.setBaseContext(context);
            LogUtils.d("obtain webview instance from pool.");
        }
        return webView;
    }

    public static void release(CacheWebView webView) {
        if (webView == null) {
            return;
        }
        webView.release();
        MutableContextWrapper wrapper = (MutableContextWrapper) webView.getContext();
        wrapper.setBaseContext(wrapper.getApplicationContext());
        sPool.release(webView);
        LogUtils.d("release webview instance to pool.");
    }
}
