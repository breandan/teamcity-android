package com.jetbrains.android.teamcity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        MessageApi.MessageListener, GoogleApiClient.OnConnectionFailedListener {
    private String transcriptionNodeId;
    private GoogleApiClient mGoogleApiClient;

    private TextView mTextView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        setAmbientEnabled();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                final Button button = (Button) stub.findViewById(R.id.button);
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Toast t = Toast.makeText(getApplicationContext(), "Stop it!", Toast
                                .LENGTH_LONG);

                        Log.i("MainAcitivyt", "Sending message: " + transcriptionNodeId);
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, transcriptionNodeId,
                                "/button", "hello".getBytes()).setResultCallback(
                                new ResultCallback() {
                                    @Override
                                    public void onResult(Result result) {
                                        Log.i("Received result", result.getStatus().toString());
                                    }
                                }
                        );

                        t.setGravity(Gravity.BOTTOM, 0, 0);
                        t.show();
                    }
                });
            }
        });
    }

    private static final String
            VOICE_TRANSCRIPTION_CAPABILITY_NAME = "voice_transcription";

    private void setupVoiceTranscription() {
        CapabilityApi.GetCapabilityResult result =
                Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient, VOICE_TRANSCRIPTION_CAPABILITY_NAME,
                        CapabilityApi.FILTER_REACHABLE).await();

        updateTranscriptionCapability(result.getCapability());

        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updateTranscriptionCapability(capabilityInfo);
                    }
                };

        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient,
                capabilityListener,
                VOICE_TRANSCRIPTION_CAPABILITY_NAME);
    }

    public static final String VOICE_TRANSCRIPTION_MESSAGE_PATH = "/voice_transcription";

    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        transcriptionNodeId = pickBestNodeId(connectedNodes);
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            Log.i("ActivityMain", "No" +
                    "de: " + node.getId());
            results.add(node.getId());
        }
        return results;
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            Log.i("Node", node.getDisplayName());
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void requestTranscription(byte[] voiceData) {
        if (transcriptionNodeId != null) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, transcriptionNodeId,
                    VOICE_TRANSCRIPTION_MESSAGE_PATH, voiceData).setResultCallback(
                    new ResultCallback() {
                        @Override
                        public void onResult(Result result) {
                            Log.i("Received result", result.toString());
                        }
                    }
            );
        } else {
            Log.e("Transcription", "Unable to receive transcription");
        }
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Do something with spokenText
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected(Bundle bundle) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setupVoiceTranscription();
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String message = new String(messageEvent.getData());
        if (messageEvent.getPath().equals(VOICE_TRANSCRIPTION_MESSAGE_PATH)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("VOICE_DATA", messageEvent.getData());
            startActivity(startIntent);
        }

        Log.i("WearMainActivity", message);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MainActivity", "onStop");
        if (mGoogleApiClient != null)
            if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            // Display message in UI
            ((TextView) findViewById(R.id.text)).setText(message);
        }
    }
}