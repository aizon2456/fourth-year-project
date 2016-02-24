package wong.ian.fourth_year_project_glass;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {

    SurfaceHolder mHolder;
    Camera mCamera = null;


    public Preview(Context context) {
        super(context);

        safeCameraOpen();

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) { }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        safeCameraOpen();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            Log.e("Issue (MAJOR)", "Preview does not exist!");
            return;
        }
        else if (mCamera == null) {
            Log.e("Issue (MAJOR)", "Camera does not exist!");
            return;
        }

        // set the display holder
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void releaseCameraAndPreview() {
        stopPreviewAndFreeCamera();
        mCamera = null;
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean safeCameraOpen() {
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();

            mCamera = Camera.open();

            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewFpsRange(30000, 30000);
                mCamera.setParameters(parameters);
                qOpened = true;
                Log.d("Preview", "Camera opened successfully!");
            }
        } catch (Exception e) {
            Log.e("Preview", "Failed to open Camera!");
            e.printStackTrace();
        }

        return qOpened;
    }
}
