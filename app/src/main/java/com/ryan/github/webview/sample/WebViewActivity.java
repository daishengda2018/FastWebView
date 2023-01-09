package com.ryan.github.webview.sample;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.mrcd.webview.CacheWebView;
import com.mrcd.webview.WebResource;
import com.mrcd.webview.WebViewPool;
import com.mrcd.webview.cache.intercept.CacheInterceptor;
import com.mrcd.webview.cache.intercept.Chain;
import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.CacheMode;
import com.mrcd.webview.config.DefaultMimeTypeFilter;
import com.mrcd.webview.cookie.CookieInterceptor;
import com.mrcd.webview.cookie.FastCookieManager;
import com.mrcd.webview.utils.LogUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ryan.github.webview.sample.MainActivity.sUseWebViewPool;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * Created by Ryan
 * at 2019/11/4
 */
public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "FastWebView";
    private CacheWebView mCachedWebView;
    private long initStartTime;
    private long startTime;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CacheWebView.setDebug(true);
        LogUtils.d("------------- start once load -------------");
        startTime = SystemClock.uptimeMillis();
        initStartTime = SystemClock.uptimeMillis();
        if (sUseWebViewPool) {
            mCachedWebView = WebViewPool.acquire(this);
        } else {
            LogUtils.d("create new webview instance.");
            mCachedWebView = new CacheWebView(this);
        }
        mCachedWebView.setWebChromeClient(new MonitorWebChromeClient());
        mCachedWebView.setWebViewClient(new MonitorWebViewClient());
        setContentView(mCachedWebView);
        mCachedWebView.setFocusable(true);
        mCachedWebView.setFocusableInTouchMode(true);
        WebSettings webSettings = mCachedWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDefaultTextEncodingName("UTF-8");
        // TODO: 2023/1/9 这么弄可以吗？ 会不会带啦啥问题
//        webSettings.setBlockNetworkImage(true);

        // 设置正确的cache mode以支持离线加载
        int cacheMode = NetworkUtils.isAvailable(this) ?
                WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK;
        webSettings.setCacheMode(cacheMode);

        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(mCachedWebView, true);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        CacheConfig config = new CacheConfig.Builder(this)
                .setCacheDir(getCacheDir() + File.separator + "custom")
                .setExtensionFilter(new CustomMimeTypeFilter())
                .build();
        mCachedWebView.setCacheMode(CacheMode.FORCE, config);
        mCachedWebView.addResourceInterceptor(new CacheInterceptor() {
            @Override
            public WebResource load(Chain chain) {
                return chain.process(chain.getRequest());
            }
        });
        mCachedWebView.addJavascriptInterface(this, "android");
        Map<String, String> headers = new HashMap<>();
        headers.put("custom", "test");

        String url = "https://github.com/Ryan-Shz";

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.removeSessionCookie();// 移除旧的[可以省略]
        cookieManager.setCookie(url, "custom=12345678910;");
        CookieSyncManager.getInstance().sync();

        FastCookieManager fastCookieManager = mCachedWebView.getFastCookieManager();
        fastCookieManager.addRequestCookieInterceptor(new CookieInterceptor() {
            @Override
            public List<Cookie> newCookies(HttpUrl url, List<Cookie> originCookies) {
                for (Cookie cookie : originCookies) {
                    Log.v(TAG, "request cookies: " + cookie.toString());
                }
                return originCookies;
            }
        });
        fastCookieManager.addResponseCookieInterceptor(new CookieInterceptor() {
            @Override
            public List<Cookie> newCookies(HttpUrl url, List<Cookie> originCookies) {
                for (Cookie cookie : originCookies) {
                    Log.v(TAG, "response cookies: " + cookie.toString());
                }
                return originCookies;
            }
        });

        mCachedWebView.loadUrl(url, headers);
    }

    @JavascriptInterface
    public void sendResource(String timing) {
        Performance performance = new Gson().fromJson(timing, Performance.class);
        Log.v(TAG, "request cost time: " + (performance.getResponseEnd() - performance.getRequestStart()) + "ms");
        Log.v(TAG, "dom build time: " + (performance.getDomComplete() - performance.getDomInteractive()) + "ms.");
        Log.v(TAG, "dom ready time: " + (performance.getDomContentLoadedEventEnd() - performance.getNavigationStart()) + "ms.");
        Log.v(TAG, "load time: " + (performance.getLoadEventEnd() - performance.getNavigationStart()) + "ms.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCachedWebView != null) {
            if (sUseWebViewPool) {
                WebViewPool.release(mCachedWebView);
            } else {
                mCachedWebView.destroy();
            }
        }
    }

    public class MonitorWebViewClient extends WebViewClient {

        private boolean first = true;

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
//            view.getSettings().setBlockNetworkImage(false);
//            view.loadUrl("javascript:android.sendResource(JSON.stringify(window.performance.timing))");
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (first) {
                LogUtils.d("init cost time: " + (SystemClock.uptimeMillis() - initStartTime));
                first = false;
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCachedWebView.canGoBack()) {
                mCachedWebView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public class MonitorWebChromeClient extends WebChromeClient {

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.d(TAG, "white screen time: " + (SystemClock.uptimeMillis() - startTime));
        }
    }

    public class CustomMimeTypeFilter extends DefaultMimeTypeFilter {
        CustomMimeTypeFilter() {
            addMimeType("text/html");
        }
    }
}
