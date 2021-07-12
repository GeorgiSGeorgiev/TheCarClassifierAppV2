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
        // Create a timestamp which will be part of the name. This way we create a pseudo-unique
        // file name.
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String photoFileName = "JPEG_" + timeStamp + "_";
        // Get the phone image storage directory.
        File storageDir = currentContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Return a temporary file created in the storage directory.
        return File.createTempFile(
                photoFileName,
                ".jpg",
                storageDir
        );
    }

    /**
     * Gets the image bitmap from the selected path and resizes its bitmap so it can fit in the
     * selected ImageView.
     * @param path The path of the image from which the bitmap will be taken.
     * @param imageView The target ImageView component. Needed just to resize the bitmap.
     * @return The extracted bitmap.
     */
    public static Bitmap decodePhoto(String path, ImageView imageView) {
        if (path == null || path.equals("")) {
            throw new IllegalArgumentException("Path is empty.");
        }
        Bitmap resultBitmap;
        // Get the imageView width and Height.
        int targetWidth = imageView.getWidth();
        int targetHeight = imageView.getHeight();


        BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
        // When set to "true", the following method will change only the size of the bitmapFactory
        bitmapFactoryOptions.inJustDecodeBounds = true;
        // Change the size of the bitmapFactory according to the size of the original image.
        // Don't return anything, just set the size of the original bitmap directly into our
        // bitmapFactoryOptions instance.
        BitmapFactory.decodeFile(path, bitmapFactoryOptions);

        // Get the bitmap height and width we just got from the original image bitmap.
        int photoWidth = bitmapFactoryOptions.outWidth;
        int photoHeight = bitmapFactoryOptions.outHeight;

        // Find the maximum scaling factor so we can resize with it the original image
        // to be able to put it directly into the imageView without any additional cutting.
        int scaleFactor = Math.
                max(1, Math.min(photoWidth / targetWidth, photoHeight / targetHeight));

        // Set the decoder to return a new bitmap instead of modifying the options.
        bitmapFactoryOptions.inJustDecodeBounds = false;
        bitmapFactoryOptions.inSampleSize = scaleFactor; // Set the scaling factor.
        resultBitmap = BitmapFactory.decodeFile(path, bitmapFactoryOptions);
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

    /**
     * Convert RGB image to a ARGB_8888 grayscale image. The alpha channel is completely ignored,
     * may result into issues if it is used in the original image.
     * @param originalBitmap The original image.
     * @return Converted image into ARGB_8888 grayscale.
     */
    public static Bitmap fromRGBtoGrayscale(Bitmap originalBitmap) {
        int height = originalBitmap.getHeight();
        int width = originalBitmap.getWidth();

        // Set the new grayscale bitmap.
        Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // Set the canvas where the result grayscale image will be drawn.
        // The grayscaleBitmap will be in the Canvas.
        Canvas grayscaleCanvas = new Canvas(grayscaleBitmap);
        Paint paint = new Paint(); // Init a new paint.
        ColorMatrix colorMatrix = new ColorMatrix(); // Matrix which will set tha saturation value.

        colorMatrix.setSaturation(0f); // set saturation to 0, i.e. no colors will be visible
        // Use the above matrix to create a new filter which is changing the saturation to 0.
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        // Create a paint which contains the saturation filter.
        paint.setColorFilter(colorFilter);
        // Apply the new paint to the original Image and draw the result on the canvas.
        // The canvas updates the grayscale Bitmap as well.
        grayscaleCanvas.drawBitmap(originalBitmap, 0, 0, paint);
        return grayscaleBitmap;
    }
}
