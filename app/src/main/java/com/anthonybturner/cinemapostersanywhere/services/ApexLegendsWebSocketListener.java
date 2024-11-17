package com.anthonybturner.cinemapostersanywhere.services;

import static com.anthonybturner.cinemapostersanywhere.utilities.ApexLegendsAPIConstants.APEX_LEGENDS_API_UPDATE_ACTION;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

public class ApexLegendsWebSocketListener {
    private final WebSocketService service;

    public ApexLegendsWebSocketListener(WebSocketService service) {
        this.service = service;
    }
    public void onOpen() {
        // Handle connection opened
    }

    public void onMessage(Object data) {
        try {
            JSONObject jsonObject = new JSONObject(data.toString());
            String payload = jsonObject.optString("payload", "{}"); // Fallback to empty JSON if missing
            JSONObject payloadObject = new JSONObject(payload);

            if (payloadObject.has("Metadata")) {
                JSONObject metadata = payloadObject.getJSONObject("Metadata");
                Intent intent = new Intent(APEX_LEGENDS_API_UPDATE_ACTION);
                //intent.putExtra("actorBundle", bundle);
                LocalBroadcastManager.getInstance(service.getApplicationContext()).sendBroadcast(intent);
            }
        } catch (JSONException e) {
            Log.e("WebSocket", "Failed to parse JSON message", e);
        }
    }

    public void onClose() {
        // Handle connection closed
    }
}
