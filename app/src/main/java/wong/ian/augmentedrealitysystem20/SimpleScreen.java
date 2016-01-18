package wong.ian.augmentedrealitysystem20;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimpleScreen extends GLSurfaceView {
    private final int WAIT_TIME = 150;

    private float[] lastArray;
    private FloatBuffer vertexBuffer;
    private float currentRatio = 1f;
    private int screenWidth = 0, screenHeight = 0;
    protected float passedX = 0, passedY = 0;
    protected float offsetX = 0, offsetY = 0;
    private boolean singleTouched = true;
    private long lastTouchTime = System.currentTimeMillis();


    public SimpleScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setRenderer(new ScreenRenderer());
        setRenderMode(RENDERMODE_CONTINUOUSLY);

        createMatrixFromPoints(new float[]{
                0f, 0f, 0f,
                400f, 0f, 0f,
                0f, 150f, 0f,
                400f, 150f, 0f
        });
    }

    public void createMatrixFromPoints(float[] baseMatrix) {
        lastArray = baseMatrix;

        // create a matrix of vertices based on these base vertices that make up a hexagon
        float vertices[] = new float[baseMatrix.length];
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = baseMatrix[i];
        }

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    public boolean handleScreenTouch(MotionEvent motion, TextView textArea) {
        int numOfTouches = motion.getPointerCount();

        if (numOfTouches == 1) {
            if (!singleTouched) {
                if ((System.currentTimeMillis() - lastTouchTime) < WAIT_TIME) {
                    return true;
                }
            }

            float x = motion.getX();
            float y = motion.getY();

            // get box's boundaries to compare (initialize to polar opposites
            float top = getBottom(), left = getRight();
            float bottom = 0f, right = 0f;

            // increment by each vertex (set of 3 points)
            for (int i = 0; i < lastArray.length; i+=3) {
                left = Math.min(left, lastArray[i]);
                top = Math.min(top, lastArray[i+1]);
                right = Math.max(right, lastArray[i]);
                bottom = Math.max(bottom, lastArray[i + 1]);
            }

            // add the translation and modification
            left = left / currentRatio + passedX;
            right = right / currentRatio +  passedX;
            top = top / currentRatio +  passedY;
            bottom = bottom / currentRatio +  passedY;

            // move the box based on a touch within the boundaries without changing the dimensions
            if (x > (left + 0.02*getRight()) && x < (right - 0.02*getRight()) && y < (bottom - 0.02*getBottom()) && y > (top + 0.02*getBottom())) {
                if (((x - offsetX) == left && (y - offsetY) == top) ||
                        offsetX == 0 || offsetY == 0) {
                    passedX = left;
                    passedY = top;
                    offsetX = x - left;
                    offsetY = y - top;
                }
                else {
                    if ((x - offsetX) != left && (x - offsetX) >= 0) {
                        passedX = x - offsetX;
                    }
                    if ((y - offsetY) != top && (y - offsetY) >= 0) {
                        passedY = y - offsetY;
                    }
                }
            }
            // move the box without changing the dimensions
            else {
                passedX = x;
                passedY = y;
                offsetX = 0;
                offsetY = 0;
            }

            singleTouched = true;
            lastTouchTime = System.currentTimeMillis();
        }
        else if (numOfTouches == 2) {
            if (singleTouched) {
                if ((System.currentTimeMillis() - lastTouchTime) < WAIT_TIME) {
                    return true;
                }
            }

            float x1 = motion.getX(0);
            float x2 = motion.getX(1);
            float y1 = motion.getY(0);
            float y2 = motion.getY(1);

            // setup the size of the window
            float leftX = Math.min(x1, x2);
            float rightX = Math.max(x1, x2);
            float topY = Math.min(y1, y2);
            float bottomY = Math.max(y1, y2);
            float width = (rightX - leftX) * 0.6f;
            float height = (bottomY - topY) * 0.6f;

            // make the matrix
            createMatrixFromPoints(new float[] {
                0f, 0f,  0f,
                width, 0f,  0f,
                0f,  height,  0f,
                width,  height,  0f
            });

            // find the top left corner
            passedX = leftX;
            passedY = topY;

            singleTouched = false;
            lastTouchTime = System.currentTimeMillis();
        }

        return true;
    }

    public void resetOffsets() {
        offsetY = 0;
        offsetX = 0;
    }

    private class ScreenRenderer implements GLSurfaceView.Renderer {
        private float width, height;

        public final void onDrawFrame(GL10 gl) {
            gl.glPushMatrix();
            gl.glClear(GLES10.GL_COLOR_BUFFER_BIT);
            gl.glTranslatef(passedX / 2, passedY / 2, 0);
            //gl.glScalef(60, 60, 0);

            gl.glColor4f(255, 255, 255, 0.3f);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
            gl.glPopMatrix();
        }

        public final void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glClearColor(0, 0, 0, 0);

            if (width > height) {
                this.height = 600;
                this.width = width * this.height / height;
                currentRatio = currentRatio * this.height / height;
            } else {
                this.width = 600;
                this.height = height * this.width / width;
                currentRatio = currentRatio * this.width / width;
            }

            screenWidth = width;
            screenHeight = height;
            gl.glViewport(0, 0, screenWidth, screenHeight);

            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0, this.width, this.height, 0, -1, 1);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        @Override
        public final void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // Set up alpha blending
            gl.glEnable(GL10.GL_ALPHA_TEST);
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            // Enable vertex arrays (we'll use them to draw primitives).
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

            // Enable texture coordinate arrays.
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        }
    }
}
