package com.mrcd.webview.cache;

import android.content.Context;
import android.webkit.WebResourceResponse;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.interceptor.CacheInterceptor;
import com.mrcd.webview.cache.interceptor.NetworkCacheInterceptor;
import com.mrcd.webview.cache.interceptor.DiskCacheInterceptor;
import com.mrcd.webview.cache.interceptor.ForceRemoteCacheInterceptor;
import com.mrcd.webview.cache.interceptor.MemCacheInterceptor;
import com.mrcd.webview.config.CacheConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存提供器
 */
public class CacheProviderImpl implements CacheProvider {

    private final Context mContext;
    private final CacheConfig mCacheConfig;
    private final WebResourceResponseGenerator mResourceResponseGenerator;

    private List<CacheInterceptor> mUserInjectInterceptorList;
    private List<CacheInterceptor> mForceModeChainList;
    private List<CacheInterceptor> mDefaultModeChainList;

    public CacheProviderImpl(Context context, CacheConfig cacheConfig) {
        mContext = context.getApplicationContext();
        mCacheConfig = cacheConfig;
        mResourceResponseGenerator = new DefaultWebResponseGenerator();
    }

    @Override
    public WebResourceResponse get(CacheRequest request) {
        final List<CacheInterceptor> interceptors = buildCacheInterceptors(request);
        final WebResource resource = callChain(interceptors, request);
        return mResourceResponseGenerator.generate(resource, request.getMime());
    }

    private List<CacheInterceptor> buildCacheInterceptors(final CacheRequest request) {
        final boolean isForceMode = request.isForceMode();
        return isForceMode ? buildForceModeInterceptors(mContext, mCacheConfig) : buildDefaultModeInterceptors(mContext);
    }

    /**
     * 构建强制缓存模式拦截器 List
     */
    private List<CacheInterceptor> buildForceModeInterceptors(Context context, CacheConfig cacheConfig) {
        if (mForceModeChainList == null) {
            final int interceptorsCount = 3 + getUserInjectInterceptorsCount();
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

    private List<CacheInterceptor> buildDefaultModeInterceptors(Context context) {
        if (mDefaultModeChainList == null) {
            int interceptorsCount = 1 + getUserInjectInterceptorsCount();
            List<CacheInterceptor> interceptors = new ArrayList<>(interceptorsCount);
            if (mUserInjectInterceptorList != null && !mUserInjectInterceptorList.isEmpty()) {
                interceptors.addAll(mUserInjectInterceptorList);
            }
            interceptors.add(new NetworkCacheInterceptor(context));
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

    @Override
    public synchronized void destroy() {
        destroyAll(mDefaultModeChainList);
        destroyAll(mForceModeChainList);
    }

    private WebResource callChain(List<CacheInterceptor> interceptors, CacheRequest request) {
        Chain chain = new Chain(interceptors);
        return chain.process(request);
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

    private int getUserInjectInterceptorsCount() {
        return mUserInjectInterceptorList == null ? 0 : mUserInjectInterceptorList.size();
    }
}
