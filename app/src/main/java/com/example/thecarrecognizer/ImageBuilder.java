package com.example.thecarrecognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Environment;
import android.widget.ImageView;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class which serves as an image processing and reformatting tool.
 * Contains only static methods.
 */
public class ImageBuilder {
    /**
     * Creates an empty photo file.
     * @param currentContext The main application environment.
     * @return The created file.
     * @throws IOException The file creation may fail.
     */
    public static File createEmptyPhotoFile(Context currentContext) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String photoFileName = "JPEG_" + timeStamp + "_";
        File storageDir = currentContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                photoFileName,
                ".jpg",
                storageDir
        );
    }

    /**
     * Gets the image bitmap from the selected path and resizes its bitmap so it can fit in the selected ImageView.
     * @param path The path of the image from which the bitmap will be taken.
     * @param imageView The target ImageView component. Needed just to resize the bitmap.
     * @return The extracted bitmap.
     */
    public static Bitmap decodePhoto(String path, ImageView imageView) {
        if (path == null || path.equals("")) {
            throw new IllegalArgumentException("Path is empty.");
        }
        Bitmap resultBitmap;
        int targetWidth = imageView.getWidth();
        int targetHeight = imageView.getHeight();

        BitmapFactory.Options bmfOptions = new BitmapFactory.Options();
        bmfOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmfOptions);

        int photoWidth = bmfOptions.outWidth;
        int photoHeight = bmfOptions.outHeight;

        int scaleFactor = Math.max(1, Math.min(photoWidth / targetWidth, photoHeight / targetHeight));

        bmfOptions.inJustDecodeBounds = false;
        bmfOptions.inSampleSize = scaleFactor;
        resultBitmap = BitmapFactory.decodeFile(path, bmfOptions);
        return resultBitmap;
    }

    /**
     * Gets the image bitmap from the selected path and shows it on the selected ImageView.
     * @param path The path of the image from which the bitmap will be taken.
     * @param imageView The target ImageView component where the image will be shown.
     * @return The result image bitmap
     */
    public static Bitmap decodeAndShowPhoto(String path, ImageView imageView) {
        Bitmap resultBitmap = ImageBuilder.decodePhoto(path, imageView);
        imageView.setImageBitmap(resultBitmap);
        return resultBitmap;
    }

    public static Bitmap fromRGBtoGrayscale(Bitmap origBitmap)
    {
        int width, height;
        height = origBitmap.getHeight();
        width = origBitmap.getWidth();

        Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(grayscaleBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();

        colorMatrix.setSaturation(0f); // set saturation to 0, i.e. no colors will be visible
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        c.drawBitmap(origBitmap, 0, 0, paint);
        return grayscaleBitmap;
    }
}
