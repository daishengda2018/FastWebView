package com.mrcd.webview.loader;

import android.util.Log;

import com.mrcd.webview.WebResource;
import com.mrcd.webview.utils.HeaderUtils;
import com.mrcd.webview.utils.StreamUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * load remote resources using HttpURLConnection.
 * <p>
 * Created by Ryan
 * 2018/2/7 下午7:55
 */
public class DefaultResourceLoader implements ResourceLoader {

    private static final String TAG = DefaultResourceLoader.class.getName();

    @Override
    public WebResource loadResource(SourceRequest sourceRequest) {
        String url = sourceRequest.getUrl();
        try {
            URL urlRequest = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlRequest.openConnection();
            putHeader(httpURLConnection, sourceRequest.getHeaders());
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setUseCaches(true);
            httpURLConnection.setConnectTimeout(20000);
            httpURLConnection.setReadTimeout(20000);
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                WebResource remoteResource = new WebResource();
                remoteResource.setOriginBytes(StreamUtils.streamToBytes(httpURLConnection.getInputStream()));
                remoteResource.setResponseCode(responseCode);
                remoteResource.setReasonPhrase(httpURLConnection.getResponseMessage());
                remoteResource.setResponseHeaders(HeaderUtils.generateHeadersMap(httpURLConnection.getHeaderFields()));
                return remoteResource;
            }
            httpURLConnection.disconnect();
        } catch (MalformedURLException e) {
            Log.d(TAG, e.toString() + " " + url);
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, e.toString() + " " + url);
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, e.toString() + " " + url);
            e.printStackTrace();
        }
        return null;
    }

    private void putHeader(HttpURLConnection httpURLConnection, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

}
