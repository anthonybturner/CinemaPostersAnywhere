package com.anthonybturner.cinemapostersanywhere.utilities;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ImageDownloadWorker extends Worker {

    public ImageDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Get image URL passed as input data
        String imageUrl = getInputData().getString("image_url");

        if (imageUrl != null) {
            Bitmap downloadedImage = downloadImage(imageUrl);
            if (downloadedImage != null) {
                // Save image and perform other necessary actions
                // You can store it locally or return the result in the output data
                return Result.success();
            }
        }

        return Result.failure();
    }

    private Bitmap downloadImage(String imageUrl) {
        try {
            // Assuming ImageUtils is your utility class to download and handle the image
            return ImageUtils.downloadImage(imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
