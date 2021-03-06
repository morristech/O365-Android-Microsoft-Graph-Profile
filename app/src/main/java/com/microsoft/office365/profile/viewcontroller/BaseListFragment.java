/*
 * Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license. See full license at the bottom of this file.
 */
package com.microsoft.office365.profile.viewcontroller;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.office365.profile.ProfileApplication;
import com.microsoft.office365.profile.R;
import com.microsoft.office365.profile.util.AuthenticationManager;
import com.microsoft.office365.profile.util.JsonRequestListener;
import com.microsoft.office365.profile.util.RequestManager;

import java.net.URL;

/**
 * Base fragment for entities that are displayed in a list.
 */
public abstract class BaseListFragment extends ListFragment implements JsonRequestListener, AuthenticationCallback {
    private static final String TAG = "BaseListFragment";
    private static final String ACCEPT_HEADER = "application/json;odata.metadata=minimal;odata.streaming=true";

    private ProfileApplication mApplication;

    /**
     * Child classes can specify the endpoint that they're using with this method
     * @return A URL object with the endpoint that receives the request.
     */
    protected abstract URL getEndpoint();

    /**
     * Returns the message to display when an empty array returned by a request.
     * For example, if the request looks for the direct reports and there's none.
     * @return The message to display when a an empty array is returned.
     */
    String getEmptyArrayMessage(){
        return getResources().getString(R.string.empty_array_default_message);
    }

    /**
     * Verifies that the app has signed in. If not, asks for credentials.
     * Then issues a request to the endpoint returned by getEndpoint();
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = (ProfileApplication)getActivity().getApplication();

        AuthenticationManager
                .getInstance()
                .setContextActivity(getActivity());

        if(!mApplication.isUserSignedIn()) {
            AuthenticationManager
                    .getInstance()
                    .getTokens(this);
        } else {
            sendRequest();
        }
    }

    /**
     * Uses the RequestManager object to send a request. It gets the endpoint from the abstract
     * method {@link BaseListFragment#getEndpoint()} implemented by subclasses.
     */
    private void sendRequest(){
        RequestManager
                .getInstance()
                .executeRequest(getEndpoint(),
                        ACCEPT_HEADER,
                        this);
    }

    /**
     * Callback for the onSuccess event of the AuthenticationManager.getTokens method
     * @param authenticationResult The AuthenticationResult object with information about the user
     *                             and tokens.
     */
    @Override
    public void onSuccess(Object authenticationResult) {
        mApplication.onSuccess(authenticationResult);
        sendRequest();
    }

    /**
     * Callback for the onError event of the AuthenticationManager.getTokens method.
     * @param e The exception object with information about the error.
     */
    @Override
    public void onError(Exception e) {
        Log.e(TAG, e.getMessage());
        Toast.makeText(
                getActivity(),
                R.string.auth_failure_toast_text,
                Toast.LENGTH_LONG).show();
        AuthenticationManager
                .getInstance()
                .getTokens(this);
    }

    /**
     * Callback for the onRequestFailure event of the RequestManager.executeRequest method.
     * @param requestedEndpoint The requested endpoint. Objects that send multiple requests can
     *                          use this parameter to differentiate from what endpoint the request
     *                          comes from.
     * @param e Exception object with details about the error.
     */
    @Override
    public void onRequestFailure(URL requestedEndpoint, Exception e) {
        Log.e(TAG, e.getMessage());
        Toast.makeText(
                getActivity(),
                R.string.http_failure_toast_text,
                Toast.LENGTH_LONG).show();
    }
}

// *********************************************************
//
// O365-Android-Profile, https://github.com/OfficeDev/O365-Android-Profile
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// *********************************************************