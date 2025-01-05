package com.anthonybturner.cinemapostersanywhere.services;

import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.HOSTING_SERVER_ADDRESS;

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
    private NowPlayingWebSocketListener listener;
    private Socket socket;
    private static final String CHANNEL_ID = "websocket_service_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        HOSTING_SERVER_ADDRESS = BuildConfig.BASE_URL;  // Use the base URL from BuildConfig

        // Create the notification channel (for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "WebSocket Service Channel";
            String description = "Channel for WebSocket Service notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupWebSocket() {
        try {
            socket = IO.socket(HOSTING_SERVER_ADDRESS);
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("WebSocket", "Connected to server");
                listener.onOpen();
            });
            socket.on("plex_event", args -> { // Triggered when a movie starts playing in plex media server (webhook)
                Log.d("WebSocket", "Plex event received: " + args[0]);
                listener.onMessage(args[0]);  // Pass data to listener
            });
            socket.on("movieUpdate", args -> { // Trigger when a server update occurs for new movies added
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
        // Create a notification for the foreground service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WebSocket Service")
                .setContentText("WebSocket is running in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
        startForeground(1, notification);  // Start service in the foreground
        listener = new NowPlayingWebSocketListener(this);
        setupWebSocket();
        return START_STICKY;  // Keep service running
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
