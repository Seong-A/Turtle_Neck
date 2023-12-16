package com.example.imageclassifier;

import android.Manifest;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Fragment;

import com.example.imageclassifier.camera.CameraFragment;
import com.example.imageclassifier.tflite.Classifier;
import com.example.imageclassifier.utils.YuvToRgbConverter;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "[IC]MainActivity";

    private static final String SELECTED_MODEL_EXTRA = "SELECTED_MODEL";

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private String selectedModel;

    private TextView textView;
    private Classifier cls;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private int sensorOrientation = 0;

    private Bitmap rgbFrameBitmap = null;

    private static final long PROB_UPDATE_DELAY_MILLIS = 1000; // 1초
    private static final long TARGET_DELAY_MILLIS = 10000; // 10초
    private static final float TARGET_PROBABILITY = 0.7f; // 70프로
    private long turtleStartTimeMillis = 0;
    private boolean audioPlayed = false;

    private Handler probUpdateHandler;

    private HandlerThread handlerThread;
    private Handler handler;
    private Handler delayHandler;
    private Button stretchButton;
    private long startTimeMillis;

    private boolean isProcessingFrame = false;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textView = findViewById(R.id.textView);

        cls = new Classifier(this);

        selectedModel = getIntent().getStringExtra(SELECTED_MODEL_EXTRA);

        try {
            cls.init(selectedModel);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if(checkSelfPermission(CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            setFragment();
        } else {
            requestPermissions(new String[]{CAMERA_PERMISSION},
                    PERMISSION_REQUEST_CODE);
        }

        delayHandler = new Handler();
        stretchButton = findViewById(R.id.stretchButton);
        stretchButton.setVisibility(View.GONE);

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

        mediaPlayer = MediaPlayer.create(this, R.raw.turtle_neck);
        mediaPlayer.setLooping(false);

        startTimeMillis = System.currentTimeMillis();

    }




    @Override
    protected synchronized void onDestroy() {
        cls.finish();
        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
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
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

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
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(grantResults.length > 0 && allPermissionsGranted(grantResults)) {
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
        final CameraManager manager =
                (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics =
                        manager.getCameraCharacteristics(cameraId);

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return "";
    }


    protected int getScreenOrientation() {
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
            Log.d(TAG, "Classified result: " + output.first + ", Probability: " + output.second);

            runOnUiThread(() -> {
                String resultStr = String.format(Locale.ENGLISH,
                        "class : %s, prob : %.2f%%",
                        output.first, output.second * 100);
                textView.setText(resultStr);

                if (output.first.equals("Turtle") && output.second >= TARGET_PROBABILITY) {
                    if (!audioPlayed) {
                        // 타이머 측정
                        turtleStartTimeMillis = System.currentTimeMillis();
                        audioPlayed = true; // 한 번만 재생되도록 설정
                    } else {
                        // 타이머가 10초를 초과했는지 확인
                        if (System.currentTimeMillis() - turtleStartTimeMillis >= TARGET_DELAY_MILLIS) {
                            delayHandler.removeCallbacksAndMessages(null);

                            stretchButton.setVisibility(View.VISIBLE);
                            stretchButton.setOnClickListener(v -> {
                                Intent intent = new Intent(MainActivity.this, StretchActivity.class);
                                startActivity(intent);
                            });

                            // 조건이 충족되면 오디오 재생
                            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                                mediaPlayer.start();
                            }

                            // 버튼 표시 후 타이머 및 오디오 중지
                            turtleStartTimeMillis = 0;
                            delayHandler.postDelayed(() -> {
                                if (mediaPlayer != null) {
                                    mediaPlayer.stop();
                                }
                                audioPlayed = false; // 재생이 중지되면 재설정
                            }, TARGET_DELAY_MILLIS);
                        }
                    }
                } else {
                    // 감지된 클래스가 변경되거나 확률이 70% 미만으로 떨어지면 타이머 및 재생 상태 재설정
                    turtleStartTimeMillis = 0;
                    audioPlayed = false;
                }

                // Log to check if stretchButton is null or not
                Log.d(TAG, "stretchButton visibility: " + stretchButton.getVisibility());
            });
        }
    }



    protected void processImage(ImageReader reader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }

        if(rgbFrameBitmap == null) {
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
                // 백그라운드 스레드에서 확률 표시 업데이트 논리를 주석 처리
                // final Pair<String, Float> output = cls.classify(rgbFrameBitmap, sensorOrientation);
            }
            image.close();
            isProcessingFrame = false;
        });
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
}