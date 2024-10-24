package com.anthonybturner.cinemapostersanywhere.utilities;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class TileTransformation extends BitmapTransformation {

    private final float scale; // Scale factor for making the tiles smaller

    // Constructor to accept scale factor
    public TileTransformation(float scale) {
        this.scale = scale;
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        // Create a scaled bitmap
        int scaledWidth = (int) (toTransform.getWidth() * scale);
        int scaledHeight = (int) (toTransform.getHeight() * scale);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(toTransform, scaledWidth, scaledHeight, true);

        // Create a new bitmap to draw the tiled image
        Bitmap result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        // Use the scaled bitmap as a tile
        BitmapDrawable drawable = new BitmapDrawable(scaledBitmap);
        drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        drawable.setBounds(0, 0, outWidth, outHeight);
        drawable.draw(canvas);

        return result;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(("tileTransformation" + scale).getBytes());
    }
}
