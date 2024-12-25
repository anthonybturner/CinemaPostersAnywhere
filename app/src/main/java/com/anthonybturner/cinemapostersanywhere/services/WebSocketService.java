package com.anthonybturner.cinemapostersanywhere.services;

import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.PLEX_BRIDGE_ADDRESS;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.anthonybturner.cinemapostersanywhere.BuildConfig;
import com.anthonybturner.cinemapostersanywhere.NowPlayingWebSocketListener;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;

public class WebSocketService extends Service {
    //public static String PLEX_BRIDGE_ADDRESS = "https://plexbridgeandroid-fd0cdaf65913.herokuapp.com/"; // Default address
    private NowPlayingWebSocketListener listener;
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        PLEX_BRIDGE_ADDRESS = BuildConfig.BASE_URL;  // Use the base URL from BuildConfig
    }

    private void setupWebSocket() {
        try {
            socket = IO.socket(PLEX_BRIDGE_ADDRESS);
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("WebSocket", "Connected to server");
                listener.onOpen();
            });
            socket.on("plex_event", args -> {//Triggered when a movie starts playing in plex media server (webhook)
                Log.d("WebSocket", "Plex event received: " + args[0]);
                listener.onMessage(args[0]);  // Pass data to listener
            });
            socket.on("movieUpdate", args -> {//Trigger when a server update occurs for new movies added
                Log.d("WebSocket", "Movie update received: ");
                listener.onMovieUpdate(args[0]);  // Pass data to listener
            });
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d("WebSocket", "Disconnected from server");
                listener.onClose();
            });
            socket.connect();
        } catch (URISyntaxException e) {
            Log.e("WebSocket", "Failed to connect to WebSocket", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Notification notification = createNotification();
       //startForeground(1, notification);  // Start service in the foreground
        listener = new NowPlayingWebSocketListener(this);
        setupWebSocket();
        return START_NOT_STICKY;  // Keep service running
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // We don't provide binding
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
            Log.d("WebSocket", "WebSocket disconnected");
        }
    }
}
