package com.ryan.github.view.cache;

import com.ryan.github.view.WebResource;
import com.ryan.github.view.cache.interceptor.CacheInterceptor;

import java.util.List;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class Chain {

    private final List<CacheInterceptor> mInterceptors;
    private int mIndex = -1;
    private CacheRequest mRequest;

    Chain(List<CacheInterceptor> interceptors) {
        mInterceptors = interceptors;
    }

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
