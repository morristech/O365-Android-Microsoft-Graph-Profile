package com.microsoft.office365.profile;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.office365.profile.model.BasicInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Administrator on 4/6/2015.
 */
public class RequestManager {
    protected ExecutorService mExecutor;
    protected static final int MAX_NUM_THREADS = 4;
    private static final String THUMBNAIL_PHOTO_ENDPOINT = "patsoldemo4.onmicrosoft.com/users('de2cbe30-d534-4ff0-ad44-f28b4c56eb96')/thumbnailPhoto";

    private static RequestManager INSTANCE;

    public static synchronized RequestManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RequestManager();
            int numProcessors = Runtime.getRuntime().availableProcessors();
            INSTANCE.mExecutor = MAX_NUM_THREADS < numProcessors ?
                    Executors.newFixedThreadPool(MAX_NUM_THREADS) :
                    Executors.newFixedThreadPool(numProcessors);
        }
        return INSTANCE;
    }

    protected void sendRequest(URL endpoint, RequestListener requestListener){
        RequestRunnable requestRunnable = new RequestRunnable(endpoint, requestListener);
        mExecutor.submit(requestRunnable);
    }

    private class RequestRunnable implements Runnable {
        protected static final String TAG = "RequestRunnable";
        protected static final String ACCEPT_HEADER = "application/json;odata.metadata=minimal;odata.streaming=true";
        protected URL mEndpoint;
        protected RequestListener mRequestListener;

        protected RequestRunnable(URL endpoint, RequestListener requestListener) {
            mEndpoint = endpoint;
            mRequestListener = requestListener;
        }

        @Override
        public void run(){
            InputStream responseStream = null;
            HttpsURLConnection httpsConnection = null;
            JsonElement jsonElement = null;

            try {
                disableSSLVerification();
                httpsConnection = (HttpsURLConnection) mEndpoint.openConnection();

                httpsConnection.setRequestMethod("GET");
                httpsConnection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                httpsConnection.setRequestProperty("accept", ACCEPT_HEADER);

                httpsConnection.connect();

                // Get the contents
                responseStream = httpsConnection.getInputStream();

                JsonReader jsonReader = new JsonReader(new InputStreamReader(responseStream));
                JsonParser jsonParser = new JsonParser();
                jsonElement =  jsonParser.parse(jsonReader).getAsJsonObject();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                //TODO: Handle the case where the execution is cancelled
            } finally {
                //TODO: Figure out if we need to close these objects or not.
                if(httpsConnection != null){
                    httpsConnection.disconnect();
                    httpsConnection = null;
                }
                if (responseStream != null) {
                    try {
                        responseStream.close();
                        responseStream = null;
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }

                mRequestListener.onRequestSuccess(jsonElement);
            }
        }

        protected String getAccessToken(){
            String accessToken = null;
            try {
                AuthenticationResult authenticationResult = AuthenticationManager
                        .getInstance()
                        .initialize()
                        .get();
                accessToken = authenticationResult.getAccessToken();
            } catch (InterruptedException | ExecutionException e){
                Log.e(TAG, e.getMessage());
                //TODO: Handle the case where the execution is cancelled
            }

            return accessToken;
        }

        //Method used for bypassing SSL verification
        protected void disableSSLVerification() {

            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

            } };

            SSLContext sc = null;
            try {
                sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }
    }
}
