package com.jetbrains.android.teamcity;

import android.util.Base64;

import retrofit.RequestInterceptor;

/**
 * Interceptor used to authorize requests.
 */
public class ApiRequestInterceptor implements RequestInterceptor {
    @Override
    public void intercept(RequestFacade requestFacade) {
        final String authorizationValue = encodeCredentialsForBasicAuthorization();
        requestFacade.addHeader("Authorization", authorizationValue);
        requestFacade.addHeader("Accept", "application/json");
    }

    private String encodeCredentialsForBasicAuthorization() {
        final String userAndPassword = "breandan" + ":" + "devtools";
        return "Basic " + Base64.encodeToString(userAndPassword.getBytes(), Base64.NO_WRAP);
    }
}