package wong.ian.augmentedrealitysystem20;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

public class TesseractEngine {
    private static String DATA_PATH;
    private static final String TESSERACT_PATH = "tessdata/";

    // You should have the trained data file in assets folder
    // You can get them at:
    // http://code.google.com/p/tesseract-ocr/downloads/list
    private static final String lang = "eng";

    private static final String TAG = "SimpleAndroidOCR.java";

    protected String _path;

    public TesseractEngine(Context context, AssetManager assets) {

        DATA_PATH = context.getFilesDir().getAbsolutePath() + "/";

        Log.i("test", "filename: " + DATA_PATH);

        AssetManager assetManager = assets;

        String[] paths = new String[] { DATA_PATH, DATA_PATH + TESSERACT_PATH};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.i(TAG, "ERROR: Creation of directory " + path + " failed!");
                    return;
                } else {
                    Log.i(TAG, "Created directory " + path + " successfully!");
                }
            }
            else {
                Log.i(TAG, "Directory " + path + " already exists!");
            }
        }

        // lang.traineddata file with the app (in assets folder)
        // You can get them at:
        // http://code.google.com/p/tesseract-ocr/downloads/list
        // This area needs work and optimization
        if (!(new File(DATA_PATH + TESSERACT_PATH + lang + ".traineddata")).exists()) {
            try {
                InputStream in = assetManager.open(TESSERACT_PATH + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + TESSERACT_PATH + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

        _path = DATA_PATH + "/ocr.jpg";
    }

    public String onPhotoTaken(byte[] data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

//        boolean pictureProcessed = false;
//        while (!pictureProcessed) {
//            if (new File(_path).exists()) {
//                pictureProcessed = true;
//            }
//            else {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {}
//            }
//        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        try {
//            ExifInterface exif = new ExifInterface(_path);
//            int exifOrientation = exif.getAttributeInt(
//                    ExifInterface.TAG_ORIENTATION,
//                    ExifInterface.ORIENTATION_NORMAL);
//
//            Log.v(TAG, "Orient: " + exifOrientation);

            int rotate = 0;

//            switch (exifOrientation) {
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    rotate = 90;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    rotate = 180;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    rotate = 270;
//                    break;
//            }

            // hard-coded to 90 degrees (portrait)
            //rotate = Exif.getOrientation(data);
            rotate = 90;

            Log.v(TAG, "Rotation: " + rotate);

            //if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            //}

            // Convert to ARGB_8888, required by tess
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        } catch (Exception e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }

        // _image.setImageBitmap( bitmap );

        Log.v(TAG, "Before baseApi");

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, lang);
        baseApi.setImage(bitmap);

        String recognizedText = baseApi.getUTF8Text();

        baseApi.end();

        // You now have the text in recognizedText var, you can do anything with it.
        // We will display a stripped out trimmed alpha-numeric version of it (if lang is eng)
        // so that garbage doesn't make it to the display.

        Log.v(TAG, "OCRED TEXT: " + recognizedText);

        if ( lang.equalsIgnoreCase("eng") ) {
            recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
        }

        // parse the results by length and content
        Log.d("OCRED_Text", "OCR text results: " + recognizedText.trim());
        String[] values = recognizedText.trim().split(" ");
        ArrayList<String> dipResults = new ArrayList<>();
        for (String option : values) {
            // if the length is 5 or more, add it to the list of results to check
            if (option.length() >= 3) {
                dipResults.add(option.toLowerCase());
            }
        }

        String[] spaceDelimArray = dipResults.toArray(new String[dipResults.size()]);

        Log.d(TAG, "OCRED RESULTS: " + Arrays.toString(spaceDelimArray));

        // get instance of the database and query the name
        DatabaseConnection db = DatabaseConnection.getInstance();
        return db.queryChemicals(spaceDelimArray);

        // Cycle done.
    }
}