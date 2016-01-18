package wong.ian.augmentedrealitysystem20;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {

    SurfaceHolder mHolder;
    Camera mCamera = null;


    public Preview(Context context) {
        super(context);

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
}
