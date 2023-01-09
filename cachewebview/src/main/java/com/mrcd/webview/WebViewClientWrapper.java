package com.mrcd.webview;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.cache.CacheManager;
import com.mrcd.webview.cache.intercept.CacheInterceptor;
import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.CacheMode;
import com.mrcd.webview.utils.LogUtils;

import javax.security.auth.Destroyable;

/**
 * 用于拦截资源加载的 WebViewClient 装饰器。
 */
class WebViewClientWrapper extends WebViewClient implements CommonApi, Destroyable {
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String METHOD_GET = "GET";
    private WebViewClient mClient;
    private final CacheManager mCacheManager;
    private final int mWebViewCacheMode;
    private final String mUserAgent;
    private final CacheWebView mWebView;

    WebViewClientWrapper(CacheWebView webView) {
        mWebView = webView;
        final WebSettings settings = webView.getSettings();
        mWebViewCacheMode = settings.getCacheMode();
        mUserAgent = settings.getUserAgentString();
        mCacheManager = new CacheManager(webView.getContext());
    }

    void setupWrapperFor(WebViewClient client) {
        mClient = client;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (mClient != null) {
            return mClient.shouldOverrideUrlLoading(view, url);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (mClient != null) {
            return mClient.shouldOverrideUrlLoading(view, request);
        }
        return false;
    }

    @Override
    @WorkerThread
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        // 此方法执行在异步线程
        if (mClient != null) {
            final WebResourceResponse response = mClient.shouldInterceptRequest(view, request);
            if (response != null) {
                return response;
            }
        }
        return loadFromWebViewCache(request);
    }

    private WebResourceResponse loadFromWebViewCache(WebResourceRequest request) {
        final String scheme = request.getUrl().getScheme().trim();
        final String method = request.getMethod().trim();
        if ((TextUtils.equals(SCHEME_HTTP, scheme)
                || TextUtils.equals(SCHEME_HTTPS, scheme))
                && method.equalsIgnoreCase(METHOD_GET)) {
            return mCacheManager.load(request, mWebViewCacheMode, mUserAgent);
        }
        return null;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (mWebView.isRecycled() && !url.equals("about:blank")) {
            mWebView.setRecycled(false);
            // 修复 WebView 复用时 goBack 可能出现白屏的问题
            mWebView.clearHistory();
        }
        if (mClient != null) {
            mClient.onPageFinished(view, url);
            return;
        }
        super.onPageFinished(view, url);
    }

    @Override
    public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
        if (mClient != null) {
            mClient.onTooManyRedirects(view, cancelMsg, continueMsg);
            return;
        }
        super.onTooManyRedirects(view, cancelMsg, continueMsg);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        if (mClient != null) {
            mClient.onReceivedHttpError(view, request, errorResponse);
            return;
        }
        super.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        if (mClient != null) {
            mClient.onFormResubmission(view, dontResend, resend);
            return;
        }
        super.onFormResubmission(view, dontResend, resend);
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        if (mClient != null) {
            mClient.doUpdateVisitedHistory(view, url, isReload);
            return;
        }
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        if (mClient != null) {
            mClient.onReceivedSslError(view, handler, error);
            return;
        }
        super.onReceivedSslError(view, handler, error);
    }

    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
        if (mClient != null) {
            mClient.onReceivedClientCertRequest(view, request);
            return;
        }
        super.onReceivedClientCertRequest(view, request);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        if (mClient != null) {
            mClient.onReceivedHttpAuthRequest(view, handler, host, realm);
            return;
        }
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        if (mClient != null) {
            return mClient.shouldOverrideKeyEvent(view, event);
        }
        return super.shouldOverrideKeyEvent(view, event);
    }

    @Override
    public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
        if (mClient != null) {
            mClient.onUnhandledKeyEvent(view, event);
            return;
        }
        super.onUnhandledKeyEvent(view, event);
    }

    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        if (mClient != null) {
            mClient.onScaleChanged(view, oldScale, newScale);
            return;
        }
        super.onScaleChanged(view, oldScale, newScale);
    }

    @Override
    public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
        if (mClient != null) {
            mClient.onReceivedLoginRequest(view, realm, account, args);
            return;
        }
        super.onReceivedLoginRequest(view, realm, account, args);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        if (mClient != null) {
            return mClient.onRenderProcessGone(view, detail);
        }
        return super.onRenderProcessGone(view, detail);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (mClient != null) {
            mClient.onReceivedError(view, errorCode, description, failingUrl);
            return;
        }
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (mClient != null) {
            mClient.onReceivedError(view, request, error);
            return;
        }
        super.onReceivedError(view, request, error);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (mClient != null) {
            mClient.onPageStarted(view, url, favicon);
            return;
        }
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        if (mClient != null) {
            mClient.onLoadResource(view, url);
            return;
        }
        super.onLoadResource(view, url);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        if (mClient != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mClient.onPageCommitVisible(view, url);
            return;
        }
        super.onPageCommitVisible(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        // don't intercept request below android 5.0
        // bc we can not get request method, request body and request headers
        // delegate intercept first
        return mClient != null ? mClient.shouldInterceptRequest(view, url) : null;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.O_MR1)
    public void onSafeBrowsingHit(final WebView view, final WebResourceRequest request, final int threatType, final SafeBrowsingResponse callback) {
        if (mClient != null) {
            mClient.onSafeBrowsingHit(view, request, threatType, callback);
        }
        super.onSafeBrowsingHit(view, request, threatType, callback);
    }

    @Override
    public void setCacheMode(CacheMode mode, CacheConfig cacheConfig) {
        if (mCacheManager != null) {
            mCacheManager.setCacheMode(mode, cacheConfig);
        }
    }

    @Override
    public void addResourceInterceptor(CacheInterceptor interceptor) {
        if (mCacheManager != null) {
            mCacheManager.addResourceInterceptor(interceptor);
        }
    }

    @Override
    public void destroy() {
        if (mCacheManager != null) {
            mCacheManager.destroy();
        }
    }
}
