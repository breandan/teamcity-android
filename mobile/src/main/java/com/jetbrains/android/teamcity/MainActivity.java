package com.jetbrains.android.teamcity;

import android.app.Activity;
import android.app.PendingIntent;
import android.support.v4.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.jetbrains.android.teamcity.rest.RestClient;
import com.jetbrains.android.teamcity.rest.Builds;

import java.util.List;
import java.util.Set;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;

public class MainActivity extends Activity implements ItemFragment.OnFragmentInteractionListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {
    private static final String TAG = "MobileMainActivity";
    private static String PATH = "/mes";

    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManagerCompat mNotificationManager;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Wearable.MessageApi.addListener(mGoogleApiClient, MainActivity.this);
    }

    private void sendData(String toSend) {
        final String WEARABLE_DATA_PATH = "/wearable_data";

        // Create a DataMap object and send it to the data layer
        final DataMap dataMap = new DataMap();
        dataMap.putString("message", toSend);

        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                        .await();
                for (Node node : nodes.getNodes()) {
                    // Construct a DataRequest and send over the data layer
                    PutDataMapRequest putDMR = PutDataMapRequest.create(WEARABLE_DATA_PATH);
                    putDMR.getDataMap().putAll(dataMap);
                    PutDataRequest request = putDMR.asPutDataRequest();
                    DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mGoogleApiClient,request)
                            .await();
                    if (result.getStatus().isSuccess()) {
                        Log.v("myTag", "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                    } else {
                        // Log an error
                        Log.v("myTag", "ERROR: failed to send DataMap");
                    }
                }
            }}).start();
    }

    private void sendMessage(final String toSend) {
        PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes
                (mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                {
                    for (Node node : result.getNodes()) {
                        String nName = node.getDisplayName();
                        String nId = node.getId();
                        Log.d("t", "Node name and ID: " + nName + " | " + nId);

                        Wearable.MessageApi.addListener(mGoogleApiClient, MainActivity.this);

                        PendingResult<MessageApi.SendMessageResult> messageResult =
                                Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient,
                                        node.getId(),
                                        PATH,
                                        toSend.getBytes());
                        messageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                Status status = sendMessageResult.getStatus();
                                Log.d(TAG, "Status: " + status.toString());
                            }
                        });
                    }
                }
            }
        });
    }

    private void sendNotification(String toSend) {
        // Create an intent for the reply action
        Intent replyIntent = new Intent(this, MainActivity.class);
        PendingIntent replyPendingIntent =
                PendingIntent.getActivity(this, 0, replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        String[] replyChoices = new String[]{"yes", "no", "maybe"};

        RemoteInput remoteInput = new RemoteInput.Builder("extra_voice_reply")
                .setLabel("Reply")
                .setChoices(replyChoices)
                .build();

        // Create the reply action and add the remote input
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(
                        R.drawable.cast_ic_notification_0,
                        "Label", replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        mNotificationManager.notify(1,
                mNotificationBuilder
                        .setContentText("Text: " + toSend)
                        .extend(new NotificationCompat.WearableExtender().addAction(action))
                        .build());
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

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if ("/buttonPress".equals(item.getUri().getPath())) {
                    Toast t = Toast.makeText(getApplicationContext(), "Button pressed!", Toast
                            .LENGTH_SHORT);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onFragmentInteraction(String id) {
        sendData("hello");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i("MainActivity", "Message received: " + messageEvent.getPath());
        if (messageEvent.getPath().equals("/button")) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast t = Toast.makeText(getApplicationContext
                            (), "Button " +
                            "pressed!", Toast
                            .LENGTH_SHORT);
                    t.show();
                }
            });
        }
    }
}