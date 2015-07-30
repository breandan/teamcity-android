package com.jetbrains.android.teamcity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.mime.TypedByteArray;

public class MainActivity extends Activity {

    private static final String API_URL = "http://localhost:8888/app/rest";

    static class Builds {
        List<Build> build;

        static class Build {
            String buildTypeId;
            String href;
            int id;
            int number;
            String state;
            String status;
            String webUrl;
        }

        @Override
        public String toString() {
            String s = "";
            for (Build build : this.build) {
                s = s + ", " + build.href + ":" + build.number;
            }

            return s;
        }
    }

    interface TeamCity {
        @GET("/builds")
        void build(Callback<Builds> cb);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApiRequestInterceptor requestInterceptor = new ApiRequestInterceptor();

        // Create a very simple REST adapter which points the GitHub API endpoint.
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setRequestInterceptor(requestInterceptor)
                .setEndpoint(API_URL)
                .build();

        // Create an instance of our GitHub API interface.
        final TeamCity teamcity = restAdapter.create(TeamCity.class);

        teamcity.build(new Callback<Builds>() {
            @Override
            public void success(Builds builds, Response response) {
                updateTextView(builds.toString());
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("MainActivity", error.getResponse().getReason() + " : " + error.toString());
            }
        });
    }

    public void updateTextView(String s) {
        TextView tv = (TextView) findViewById(R.id.text_view);
        tv.setText(s);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
