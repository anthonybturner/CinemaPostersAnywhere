package com.anthonybturner.cinemapostersanywhere.services;

import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.PLEX_BRIDGE_ADDRESS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.anthonybturner.cinemapostersanywhere.BuildConfig;
import com.anthonybturner.cinemapostersanywhere.NowPlayingWebSocketListener;
import com.anthonybturner.cinemapostersanywhere.R;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;

public class WebSocketService extends Service {
    //public static String PLEX_BRIDGE_ADDRESS = "https://plexbridgeandroid-fd0cdaf65913.herokuapp.com/"; // Default address
    private NowPlayingWebSocketListener listener;
    private ApexLegendsWebSocketListener steamListener;
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        PLEX_BRIDGE_ADDRESS = BuildConfig.BASE_URL;  // Use the base URL from BuildConfig
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "CHANNEL_ID",
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setContentTitle("Service Running")
                .setContentText("Now Playing...")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
    }

    private void setupWebSocket() {
        try {
            socket = IO.socket(PLEX_BRIDGE_ADDRESS);
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("WebSocket", "Connected to server");
                listener.onOpen();
                steamListener.onOpen();
            });
            socket.on("plex_event", args -> {//Triggered when a movie starts playing in plex media server (webhook)
                Log.d("WebSocket", "Plex event received: " + args[0]);
                listener.onMessage(args[0]);  // Pass data to listener
            });
            socket.on("apex_event", args -> {//Used for match history webhooks, not yet available
                Log.d("WebSocket", "Apex event received: " + args[0]);
                steamListener.onMessage(args[0]);  // Pass data to listener
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
        steamListener = new ApexLegendsWebSocketListener(this);
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
