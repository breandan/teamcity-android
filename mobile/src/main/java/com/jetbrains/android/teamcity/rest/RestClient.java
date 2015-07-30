package com.jetbrains.android.teamcity.rest;

import android.util.Base64;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class RestClient {
    private static TeamCity teamCity;
    private static final String API_URL = "http://localhost:8888/app/rest";

    public static class TCRequestInterceptor implements RequestInterceptor {
        @Override
        public void intercept(retrofit.RequestInterceptor.RequestFacade requestFacade) {
            final String auth = encodeCredentialsForBasicAuthorization();
            requestFacade.addHeader("Authorization", auth);
            requestFacade.addHeader("Accept", "application/json");
        }

        private String encodeCredentialsForBasicAuthorization() {
            return "Basic " + Base64.encodeToString("breandan:devtools".getBytes(), Base64.NO_WRAP);
        }
    }

    public static TeamCity get() {
        return teamCity;
    }

    static {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setRequestInterceptor(new TCRequestInterceptor())
                .setEndpoint(API_URL)
                .build();
        teamCity = restAdapter.create(TeamCity.class);

    }
}
