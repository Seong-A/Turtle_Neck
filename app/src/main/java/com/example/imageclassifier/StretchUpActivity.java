package com.example.imageclassifier;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.imageclassifier.camera.CameraFragment;
import com.example.imageclassifier.tflite.Classifier_st_Up;
import com.example.imageclassifier.utils.YuvToRgbConverter;

import java.io.IOException;
import java.util.Locale;

public class StretchUpActivity extends AppCompatActivity {
    public static final String TAG = "[IC]StretchUpActivity";

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView textView;
    private Classifier_st_Up cls;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private int sensorOrientation = 0;

    private Bitmap rgbFrameBitmap = null;

    private HandlerThread handlerThread;
    private Handler handler;

    private boolean isProcessingFrame = false;

    private Handler probUpdateHandler;
    private static final long PROB_UPDATE_DELAY_MILLIS = 1000; // 1초
    private static final long TARGET_STAY_DELAY_MILLIS = 15000; // 15초
    private static final float TARGET_PROBABILITY = 0.7f; // 70%
    private long stayStartTimeMillis = 0;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stretching_u);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        textView = findViewById(R.id.textView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cls = new Classifier_st_Up(this);

        try {
            cls.init();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.stretch_up);
        mediaPlayer.start();

        if (checkSelfPermission(CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setFragment();
        } else {
            requestPermissions(new String[]{CAMERA_PERMISSION}, PERMISSION_REQUEST_CODE);
        }

        // 1초마다 확률 업데이트를 위한 핸들러 초기화
        probUpdateHandler = new Handler();

        // 1초마다 확률을 업데이트하는 타이머 시작
        probUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 확률 표시 업데이트
                updateProbability();
                // 다음 업데이트를 1초 후에 예약
                probUpdateHandler.postDelayed(this, PROB_UPDATE_DELAY_MILLIS);
            }
        }, PROB_UPDATE_DELAY_MILLIS);
    }

    @Override
    protected synchronized void onDestroy() {
        // MediaPlayer 정리
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        cls.finish();
        super.onDestroy();
    }


    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("InferenceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    protected void setFragment() {
        Size inputSize = cls.getModelInputSize();
        String cameraId = chooseCamera();

        if(inputSize.getWidth() > 0 && inputSize.getHeight() > 0 && !cameraId.isEmpty()) {
            Fragment fragment = CameraFragment.newInstance(
                    (size, rotation) -> {
                        previewWidth = size.getWidth();
                        previewHeight = size.getHeight();
                        sensorOrientation = rotation - getScreenOrientation();
                    },
                    reader->processImage(reader),
                    inputSize,
                    cameraId);

            Log.d(TAG, "inputSize : " + cls.getModelInputSize() +
                    "sensorOrientation : " + sensorOrientation);
            getFragmentManager().beginTransaction().replace(
                    R.id.fragment, fragment).commit();
        } else {
            Toast.makeText(this, "Can't find front camera", Toast.LENGTH_SHORT).show();
        }
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.d(TAG, "Camera Id: " + cameraId + ", Facing: " + facing);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return "";
    }

    private int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    private void updateProbability() {
        if (cls != null && cls.isInitialized() && rgbFrameBitmap != null) {
            final Pair<String, Float> output = cls.classify(rgbFrameBitmap, sensorOrientation);
            runOnUiThread(() -> {
                String resultStr = String.format(Locale.ENGLISH,
                        "클래스 : %s, 확률 : %.2f%%",
                        output.first, output.second * 100);
                textView.setText(resultStr);

                if (output.first.equals("Up") && output.second >= TARGET_PROBABILITY) {
                    if (stayStartTimeMillis == 0) {
                        // 타이머 측정 시작
                        stayStartTimeMillis = System.currentTimeMillis();
                    } else {
                        // stay 클래스가 70% 이상을 15초 이상 유지하면 전환
                        if (System.currentTimeMillis() - stayStartTimeMillis >= TARGET_STAY_DELAY_MILLIS) {
                            // 여기에서 stretchUpActivity로 전환하는 코드 추가
                            Intent intent = new Intent(StretchUpActivity.this, SuccessActivity.class);
                            startActivity(intent);
                            finish(); // 현재 Activity 종료
                        }
                    }
                } else {
                    // stay 클래스가 아닌 경우 또는 확률이 70% 미만으로 떨어진 경우 타이머 초기화
                    stayStartTimeMillis = 0;
                }
            });
        }
    }

    protected void processImage(ImageReader reader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }

        if (rgbFrameBitmap == null) {
            rgbFrameBitmap = Bitmap.createBitmap(
                    previewWidth,
                    previewHeight,
                    Bitmap.Config.ARGB_8888);
        }

        if (isProcessingFrame) {
            return;
        }

        isProcessingFrame = true;

        final Image image = reader.acquireLatestImage();
        if (image == null) {
            isProcessingFrame = false;
            return;
        }

        YuvToRgbConverter.yuvToRgb(this, image, rgbFrameBitmap);

        runInBackground(() -> {
            if (cls != null && cls.isInitialized()) {
//                final Pair<String, Float> output = cls.classify(rgbFrameBitmap, sensorOrientation);
//
//                runOnUiThread(() -> {
//                    String resultStr = String.format(Locale.ENGLISH,
//                            "class : %s, prob : %.2f%%",
//                            output.first, output.second * 100);
//
//                    // Display the result in TextView
//                    TextView textView = findViewById(R.id.textView);
//                    textView.setText(resultStr);
//                });
            }
            image.close();
            isProcessingFrame = false;
        });
    }

    private synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
}