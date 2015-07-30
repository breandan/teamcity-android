package com.jetbrains.android.teamcity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.jetbrains.android.teamcity.rest.RestClient;
import com.jetbrains.android.teamcity.rest.Builds;

import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;

public class MainActivity extends Activity implements ItemFragment.OnFragmentInteractionListener {


    @Override
    public void onFragmentInteraction(String id) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RestClient.get().getBuilds(new Callback<Builds>() {
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
