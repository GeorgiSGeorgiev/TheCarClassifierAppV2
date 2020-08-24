package com.example.thecarrecognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.widget.ImageView;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageBuilder {
    public static File createEmptyPhotoFile(Context currentContext) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String photoFileName = "JPEG_" + timeStamp + "_";
        File storageDir = currentContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                photoFileName,
                ".jpg",
                storageDir
        );
    }

    public static Bitmap decodePhoto(Context currentContext, String path, ImageView imageView) {
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

    public static Bitmap decodeAndShowPhoto(Context currentContext, String path, ImageView imageView) {
        Bitmap resultBitmap = ImageBuilder.decodePhoto(currentContext, path, imageView);
        imageView.setImageBitmap(resultBitmap);
        return resultBitmap;
    }

    public static File convertBitmapToFile(Context currentContext, Bitmap bitmap) {
        File resultFile = new File(currentContext.getCacheDir(), "temp.png");

        System.out.println("Starting debugging");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapData = bos.toByteArray();

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(resultFile);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultFile;
    }
}
