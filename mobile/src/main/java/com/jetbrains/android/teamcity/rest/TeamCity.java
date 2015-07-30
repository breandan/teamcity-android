package com.jetbrains.android.teamcity.rest;

import retrofit.Callback;
import retrofit.http.GET;

/**
 * Created by breandan on 7/30/2015.
 */
public interface TeamCity {
    @GET("/builds")
    void getBuilds(Callback<Builds> cb);
}
