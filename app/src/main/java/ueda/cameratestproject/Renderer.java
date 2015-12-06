package ueda.cameratestproject;

import android.app.Activity;
import android.graphics.Point;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Renderer implements GLSurfaceView.Renderer {
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
                    "attribute vec2 texcoord;\n" +
                    "varying vec2 texcoordVarying;\n" +
                    "void main() {\n" +
                    "    gl_Position = position;\n" +
                    "    texcoordVarying = texcoord;\n" +
                    "}\n";
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 texcoordVarying;\n" +
                    "uniform samplerExternalOES texture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(texture, texcoordVarying);\n" +
                    "}\n";

    private static final float TEX_COORDS_ROTATION_0[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    private static final float TEX_COORDS_ROTATION_90[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };
    private static final float TEX_COORDS_ROTATION_180[] = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f
    };
    private static final float TEX_COORDS_ROTATION_270[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };
    private static final float VERTECES[] = {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f
    };

    private Activity mActivity;
    private int mProgram;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mTextureHandle;
    private int mTextureID;
    private FloatBuffer mTexCoordBuffer;
    private FloatBuffer mVertexBuffer;

    private Camera mCamera;
    private boolean mConfigured = false;

    public Renderer(Activity activity) {
        mActivity = activity;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        mCamera = new Camera(mActivity, mTextureID);
        mCamera.open();

        mTexCoordBuffer =
                ByteBuffer.allocateDirect(TEX_COORDS_ROTATION_0.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer =
                ByteBuffer.allocateDirect(VERTECES.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(VERTECES).position(0);

        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "texcoord");
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        checkGlError("glGetAttribLocation");

        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "texture");
        checkGlError("glGetUniformLocation");
    }

    public void onDrawFrame(GL10 unused ) {
        GLES20.glClearColor(0.5f, 0.5f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        if (!mConfigured) {
            if (mConfigured = mCamera.getInitialized()) {
                mCamera.setCameraRotation();
                setConfig();
            } else {
                return;
            }
        }

        mCamera.updateTexture();

        GLES20.glUseProgram(mProgram);

        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        checkGlError("glVertexAttribPointer");

        GLES20.glUniform1i(mTextureHandle, 0);
        checkGlError("glUniform1i");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        checkGlError("glBindTexture");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glUseProgram(0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    public void onSurfaceChanged (GL10 unused, int width, int height) {
        mConfigured = false;
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            return 0;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, pixelShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    private void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    private void setConfig() {
        switch(mCamera.getCameraRotation()) {
            case ROTATION_0:
                mTexCoordBuffer.put(TEX_COORDS_ROTATION_0);
                break;
            case ROTATION_90:
                mTexCoordBuffer.put(TEX_COORDS_ROTATION_90);
                break;
            case ROTATION_180:
                mTexCoordBuffer.put(TEX_COORDS_ROTATION_180);
                break;
            case ROTATION_270:
                mTexCoordBuffer.put(TEX_COORDS_ROTATION_270);
                break;
        }
        mTexCoordBuffer.position(0);

        Point displaySize = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        Size textureSize = mCamera.getCameraSize();
        Point textureOrigin = new Point(
                (displaySize.x - textureSize.getWidth()) / 2,
                (displaySize.y - textureSize.getHeight()) / 2);

        GLES20.glViewport(textureOrigin.x, textureOrigin.y, textureSize.getWidth(), textureSize.getHeight());
    }
}