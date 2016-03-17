package wong.ian.augmentedrealitysystem20;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private Camera mCamera = null;
    private String imagePath = null;
    private TesseractEngine tesseract = null;
    private String stringResults = null;

    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = new File(imagePath);
            Log.i("FileLocation", "File will be saved to: " + imagePath);
            if (pictureFile == null) {
                Log.e("FileLocation", "Error setting image file location to: " + imagePath);
                return;
            }
            try {
                stringResults = tesseract.onPhotoTaken(data);
            } catch (Exception e) {
                Log.e("FileParsing", e.getMessage());
            }
            finally {
                mCamera.stopPreview();
                mCamera.startPreview();
            }
        }
    };

    private ShutterCallback mShutter = new ShutterCallback(){
        @Override
        public void onShutter() {Log.i("Callback", "Shutter Callback triggered.");}
    };

    PictureCallback mPictureRaw = new PictureCallback(){
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {Log.i("Callback", "Raw Picture Callback triggered.");}
    };

    public Preview(Context context, TesseractEngine tesseract) {
        super(context);

        this.tesseract = tesseract;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            Log.e("Issue (MAJOR)", "Preview does not exist!");
            return;
        }

        mCamera.stopPreview();

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setRotation(0);
        Camera.Size previewSize = parameters.getSupportedPreviewSizes().get(0);

        parameters.setPreviewSize(previewSize.width, previewSize.height);
        requestLayout();
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setCamera(Camera camera) {
        if (mCamera == camera) {
            return;
        }

        // reset camera, even on first pass
        stopPreviewAndFreeCamera();
        mCamera = camera;

        if (mCamera != null) {
            requestLayout();

            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

            surfaceChanged(null, 0, 0, 0);
        }
    }

    public void takePicture() {
        mCamera.lock();
        this.imagePath = tesseract.getImagePath();
        Log.d("Deleting File", Boolean.toString(new File(imagePath).delete()));
        mCamera.takePicture(mShutter, mPictureRaw, mPicture);
        Log.i("TakePicture", "Picture taken. Processing...");
        try {
            mCamera.reconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getStringResults() {
        return stringResults;
    }
}
