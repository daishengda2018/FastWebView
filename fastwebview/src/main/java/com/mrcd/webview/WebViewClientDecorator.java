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

import com.mrcd.webview.cache.interceptor.CacheInterceptor;
import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.FastCacheMode;

import javax.security.auth.Destroyable;

/**
 * 用于拦截资源加载的 WebViewClient 装饰器。
 */
class WebViewClientDecorator extends WebViewClient implements FastOpenApi, Destroyable {
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String METHOD_GET = "GET";
    private WebViewClient mDelegate;
    private final WebViewCache mWebViewCache;
    private final int mWebViewCacheMode;
    private final String mUserAgent;
    private final FastWebView mWebView;

    WebViewClientDecorator(FastWebView webView) {
        mWebView = webView;
        final WebSettings settings = webView.getSettings();
        mWebViewCacheMode = settings.getCacheMode();
        mUserAgent = settings.getUserAgentString();
        mWebViewCache = new WebViewCacheImpl(webView.getContext());
    }

    void setupDelegateWith(WebViewClient client) {
        mDelegate = client;
    }
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (mDelegate != null) {
            return mDelegate.shouldOverrideUrlLoading(view, url);
        }
        view.loadUrl(url);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (mDelegate != null) {
            return mDelegate.shouldOverrideUrlLoading(view, request);
        }
        view.loadUrl(request.getUrl().toString());
        return true;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (mDelegate != null) {
            final WebResourceResponse response = mDelegate.shouldInterceptRequest(view, request);
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
            return mWebViewCache.getResource(request, mWebViewCacheMode, mUserAgent);
        }
        return null;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (mWebView.isRecycled() && !url.equals("about:blank")) {
            mWebView.setRecycled(false);
            // TODO: 2023/1/9 这么弄的目的是什么？ 是为了清除预加载、WebView Pool 残留的历史记录吗？
            mWebView.clearHistory();
        }
        if (mDelegate != null) {
            mDelegate.onPageFinished(view, url);
            return;
        }
        super.onPageFinished(view, url);
    }

    @Override
    public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
        if (mDelegate != null) {
            mDelegate.onTooManyRedirects(view, cancelMsg, continueMsg);
            return;
        }
        super.onTooManyRedirects(view, cancelMsg, continueMsg);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        if (mDelegate != null) {
            mDelegate.onReceivedHttpError(view, request, errorResponse);
            return;
        }
        super.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        if (mDelegate != null) {
            mDelegate.onFormResubmission(view, dontResend, resend);
            return;
        }
        super.onFormResubmission(view, dontResend, resend);
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        if (mDelegate != null) {
            mDelegate.doUpdateVisitedHistory(view, url, isReload);
            return;
        }
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        if (mDelegate != null) {
            mDelegate.onReceivedSslError(view, handler, error);
            return;
        }
        super.onReceivedSslError(view, handler, error);
    }

    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
        if (mDelegate != null) {
            mDelegate.onReceivedClientCertRequest(view, request);
            return;
        }
        super.onReceivedClientCertRequest(view, request);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        if (mDelegate != null) {
            mDelegate.onReceivedHttpAuthRequest(view, handler, host, realm);
            return;
        }
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        if (mDelegate != null) {
            return mDelegate.shouldOverrideKeyEvent(view, event);
        }
        return super.shouldOverrideKeyEvent(view, event);
    }

    @Override
    public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
        if (mDelegate != null) {
            mDelegate.onUnhandledKeyEvent(view, event);
            return;
        }
        super.onUnhandledKeyEvent(view, event);
    }

    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        if (mDelegate != null) {
            mDelegate.onScaleChanged(view, oldScale, newScale);
            return;
        }
        super.onScaleChanged(view, oldScale, newScale);
    }

    @Override
    public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
        if (mDelegate != null) {
            mDelegate.onReceivedLoginRequest(view, realm, account, args);
            return;
        }
        super.onReceivedLoginRequest(view, realm, account, args);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        if (mDelegate != null) {
            return mDelegate.onRenderProcessGone(view, detail);
        }
        return super.onRenderProcessGone(view, detail);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (mDelegate != null) {
            mDelegate.onReceivedError(view, errorCode, description, failingUrl);
            return;
        }
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (mDelegate != null) {
            mDelegate.onReceivedError(view, request, error);
            return;
        }
        super.onReceivedError(view, request, error);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (mDelegate != null) {
            mDelegate.onPageStarted(view, url, favicon);
            return;
        }
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        if (mDelegate != null) {
            mDelegate.onLoadResource(view, url);
            return;
        }
        super.onLoadResource(view, url);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        if (mDelegate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDelegate.onPageCommitVisible(view, url);
            return;
        }
        super.onPageCommitVisible(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        // don't intercept request below android 5.0
        // bc we can not get request method, request body and request headers
        // delegate intercept first
        return mDelegate != null ? mDelegate.shouldInterceptRequest(view, url) : null;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.O_MR1)
    public void onSafeBrowsingHit(final WebView view, final WebResourceRequest request, final int threatType, final SafeBrowsingResponse callback) {
        if (mDelegate != null) {
            mDelegate.onSafeBrowsingHit(view, request, threatType, callback);
        }
        super.onSafeBrowsingHit(view, request, threatType, callback);
    }

    @Override
    public void setCacheMode(FastCacheMode mode, CacheConfig cacheConfig) {
        if (mWebViewCache != null) {
            mWebViewCache.setCacheMode(mode, cacheConfig);
        }
    }

    @Override
    public void addResourceInterceptor(CacheInterceptor interceptor) {
        if (mWebViewCache != null) {
            mWebViewCache.addResourceInterceptor(interceptor);
        }
    }

    @Override
    public void destroy() {
        if (mWebViewCache != null) {
            mWebViewCache.destroy();
        }
    }
}
