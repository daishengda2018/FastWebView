package com.mrcd.webview.cache.intercept;

import androidx.annotation.WorkerThread;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.cache.CacheRequest;

import java.util.List;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class Chain {

    private final List<CacheInterceptor> mInterceptors;
    private int mIndex = -1;
    private CacheRequest mRequest;

    public Chain(List<CacheInterceptor> interceptors) {
        mInterceptors = interceptors;
    }

    @WorkerThread
    public WebResource process(CacheRequest request) {
        if (++mIndex >= mInterceptors.size()) {
            return null;
        }
        mRequest = request;
        CacheInterceptor interceptor = mInterceptors.get(mIndex);
        return interceptor.load(this);
    }

    public CacheRequest getRequest() {
        return mRequest;
    }
}
