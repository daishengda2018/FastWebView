package com.mrcd.webview.cache;

import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.CommonApi;
import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.intercept.CacheInterceptor;
import com.mrcd.webview.cache.intercept.Chain;
import com.mrcd.webview.cache.intercept.DefaultRemoteCacheInterceptor;
import com.mrcd.webview.cache.intercept.DiskCacheInterceptor;
import com.mrcd.webview.cache.intercept.ForceRemoteCacheInterceptor;
import com.mrcd.webview.cache.intercept.MemCacheInterceptor;
import com.mrcd.webview.config.CacheConfig;
import com.mrcd.webview.config.CacheMode;
import com.mrcd.webview.utils.MimeTypeMapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 缓存管理器
 */
public class CacheManager implements CommonApi, Destroyable {

    private final WebResourceResponseGenerator mResourceResponseGenerator;
    private CacheMode mCacheMode;
    private CacheConfig mCacheConfig;
    private Context mContext;
    private List<CacheInterceptor> mUserInjectInterceptorList;
    private List<CacheInterceptor> mForceModeChainList;
    private List<CacheInterceptor> mDefaultModeChainList;

    public CacheManager(Context context) {
        mContext = context;
        mResourceResponseGenerator = new WebResourceResponseGenerator();
    }

    @Override
    public void setCacheMode(CacheMode mode, CacheConfig cacheConfig) {
        mCacheMode = mode;
        mCacheConfig = cacheConfig;
    }

    @WorkerThread
    public WebResourceResponse load(WebResourceRequest request, int cacheMode, String userAgent) {
        if (mCacheMode == CacheMode.DEFAULT) {
            return null;
        }
        final String url = request.getUrl().toString();
        final String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
        final String mimeType = MimeTypeMapUtils.getMimeTypeFromExtension(extension);
        final Map<String, String> headers = request.getRequestHeaders();
        final CacheRequest cacheRequest = new CacheRequest();
        cacheRequest.setUrl(url);
        cacheRequest.setMime(mimeType);
        cacheRequest.setForceMode(mCacheMode == CacheMode.FORCE);
        cacheRequest.setUserAgent(userAgent);
        cacheRequest.setWebViewCacheMode(cacheMode);
        cacheRequest.setHeaders(headers);
        return load(cacheRequest);
    }

    @WorkerThread
    private WebResourceResponse load(CacheRequest request) {
        final List<CacheInterceptor> interceptors = buildCacheInterceptors(request);
        final Chain chain = new Chain(interceptors);
        final WebResource resource = chain.process(request);
        return mResourceResponseGenerator.generate(resource, request.getMime());
    }

    @WorkerThread
    private List<CacheInterceptor> buildCacheInterceptors(final CacheRequest request) {
        final boolean isForceMode = request.isForceMode();
        return isForceMode ? buildForceModeInterceptors(mContext, getCacheConfig()) : buildDefaultModeInterceptors(mContext);
    }

    @WorkerThread
    private CacheConfig getCacheConfig() {
        return mCacheConfig != null ? mCacheConfig : generateDefaultCacheConfig();
    }

    @WorkerThread
    private CacheConfig generateDefaultCacheConfig() {
        return new CacheConfig.Builder(mContext).build();
    }

    /**
     * 构建强制缓存模式拦截器 List
     */
    @WorkerThread
    private List<CacheInterceptor> buildForceModeInterceptors(Context context, CacheConfig cacheConfig) {
        if (mForceModeChainList == null) {
            final int interceptorsCount = 3 + getUserInjectInterceptorsCount();
            // 不对全局变量的集合最修改，保证线程安全
            final List<CacheInterceptor> interceptors = new ArrayList<>(interceptorsCount);
            if (mUserInjectInterceptorList != null && !mUserInjectInterceptorList.isEmpty()) {
                interceptors.addAll(mUserInjectInterceptorList);
            }
            interceptors.add(MemCacheInterceptor.getInstance(cacheConfig));
            interceptors.add(new DiskCacheInterceptor(cacheConfig));
            interceptors.add(new ForceRemoteCacheInterceptor(context, cacheConfig));
            mForceModeChainList = interceptors;
        }
        return mForceModeChainList;
    }

    @WorkerThread
    private List<CacheInterceptor> buildDefaultModeInterceptors(Context context) {
        if (mDefaultModeChainList == null) {
            int interceptorsCount = 1 + getUserInjectInterceptorsCount();
            // 不对全局变量的集合最修改，保证线程安全
            List<CacheInterceptor> interceptors = new ArrayList<>(interceptorsCount);
            if (mUserInjectInterceptorList != null && !mUserInjectInterceptorList.isEmpty()) {
                interceptors.addAll(mUserInjectInterceptorList);
            }
            interceptors.add(new DefaultRemoteCacheInterceptor(context));
            mDefaultModeChainList = interceptors;
        }
        return mDefaultModeChainList;
    }

    @Override
    public synchronized void addResourceInterceptor(CacheInterceptor interceptor) {
        if (mUserInjectInterceptorList == null) {
            mUserInjectInterceptorList = new ArrayList<>();
        }
        mUserInjectInterceptorList.add(interceptor);
    }

    private int getUserInjectInterceptorsCount() {
        return mUserInjectInterceptorList == null ? 0 : mUserInjectInterceptorList.size();
    }

    @Override
    public void destroy() {
        destroyAll(mDefaultModeChainList);
        destroyAll(mForceModeChainList);
        // help gc
        mCacheConfig = null;
        mContext = null;
    }

    private void destroyAll(List<CacheInterceptor> interceptors) {
        if (interceptors == null || interceptors.isEmpty()) {
            return;
        }
        for (CacheInterceptor interceptor : interceptors) {
            if (interceptor instanceof Destroyable) {
                ((Destroyable) interceptor).destroy();
            }
        }
    }
}