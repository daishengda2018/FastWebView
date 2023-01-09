package com.mrcd.webview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mrcd.webview.cache.intercept.CacheInterceptor;
import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.CacheMode;
import com.mrcd.webview.cookie.FastCookieManager;
import com.mrcd.webview.utils.LogUtils;

/**
 * 带有三级缓存的 WebView
 */
public class CacheWebView extends WebView implements CommonApi {

    private WebViewClientWrapper mWrapperClient;
    private WebViewClient mUserClient;
    private boolean mRecycled = false;

    public CacheWebView(Context context) {
        this(context, null);
    }

    public CacheWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CacheWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 预加载，加载完毕后会缓存在本地，无网络也可以访问
     *
     * @param url 网络地址
     */
    public static void preload(Context context, String url) {
        new CacheWebView(context.getApplicationContext()).loadUrl(url);
    }

    public void setCacheMode(CacheMode mode) {
        setCacheMode(mode, null);
    }

    @Override
    public void setCacheMode(CacheMode mode, CacheConfig cacheConfig) {
        if (mode == CacheMode.FORCE) {
            mWrapperClient = new WebViewClientWrapper(this);
            if (mUserClient != null) {
                mWrapperClient.setupWrapperFor(mUserClient);
            }
            mWrapperClient.setCacheMode(mode, cacheConfig);
            super.setWebViewClient(mWrapperClient);
            return;
        }

        mWrapperClient = null;
        if (mUserClient != null) {
            setWebViewClient(mUserClient);
        }
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        if (mWrapperClient != null) {
            mWrapperClient.setupWrapperFor(client);
        } else {
            super.setWebViewClient(client);
        }
        mUserClient = client;
    }

    public void addResourceInterceptor(CacheInterceptor interceptor) {
        if (mWrapperClient != null) {
            mWrapperClient.addResourceInterceptor(interceptor);
        }
    }

    public void runJs(String function, Object... args) {
        StringBuilder script = new StringBuilder("javascript:");
        script.append(function).append("(");
        if (null != args && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (null == arg) {
                    continue;
                }
                if (arg instanceof String) {
                    arg = "'" + arg + "'";
                }
                script.append(arg);
                if (i != args.length - 1) {
                    script.append(",");
                }
            }
        }
        script.append(");");
        runJs(script.toString());
    }

    private void runJs(String script) {
        this.loadUrl(script);
    }

    @Override
    public WebViewClient getWebViewClient() {
        return mUserClient != null ? mUserClient : super.getWebViewClient();
    }

    public FastCookieManager getFastCookieManager() {
        return FastCookieManager.getInstance();
    }

    @Override
    public boolean canGoBack() {
        return !mRecycled && super.canGoBack();
    }

    boolean isRecycled() {
        return mRecycled;
    }

    void setRecycled(boolean recycled) {
        this.mRecycled = recycled;
    }

    public static void setDebug(boolean debug) {
        LogUtils.DEBUG = debug;
    }

    @Override
    public void destroy() {
        release();
        super.destroy();
    }

    public void release() {
        stopLoading();
        loadUrl("");
        setRecycled(true);
        setWebViewClient(null);
        setWebChromeClient(null);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setBlockNetworkImage(false);
        clearHistory();
        clearCache(true);
        removeAllViews();
        final ViewParent viewParent = this.getParent();
        if (viewParent instanceof ViewGroup) {
            ((ViewGroup) viewParent).removeView(this);
        }
        if (mWrapperClient != null) {
            mWrapperClient.destroy();
        }
        getFastCookieManager().destroy();
    }
}
