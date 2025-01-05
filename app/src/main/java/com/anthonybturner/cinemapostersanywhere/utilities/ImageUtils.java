package com.anthonybturner.cinemapostersanywhere.utilities;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageUtils {

    public static Bitmap downloadImage(String posterPath) {
        InputStream inputStream = null;
        try {
            URL url = new URL(posterPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            inputStream = connection.getInputStream();
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e("ImageUtils", "Error downloading image", e);
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Log.e("ImageUtils", "Error closing input stream", e);
            }
        }
    }

    public static String saveImageToStorage(Context context, Bitmap bitmap, String fileName) {
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Movies");
        if (!directory.exists()) {
            boolean dirCreated = directory.mkdirs();
            if (!dirCreated) {
                Log.e("ImageUtils", "Error creating directory for storing images.");
                return null;
            }
        }
        File imageFile = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (IOException e) {
            Log.e("ImageUtils", "Error saving image to storage", e);
            return null;
        }
        return imageFile.getAbsolutePath();
    }

    public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream); // Compress bitmap to PNG or JPEG
        return byteArrayOutputStream.toByteArray();
    }

}