package com.example.imageclassifier;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        ImageView imageView = findViewById(R.id.logo);

        // 초기 크기
        float initialScale = 1.0f;

        // 줌 인 애니메이션 설정
        float pivotX = 0.5f; // 가로 축에서의 피벗 위치 (0.0 ~ 1.0)
        float pivotY = 0.5f; // 세로 축에서의 피벗 위치 (0.0 ~ 1.0)
        float toX = 1.5f; // 가로 축에서의 끝 크기 (0.0 ~ 무한대)
        float toY = 1.5f; // 세로 축에서의 끝 크기 (0.0 ~ 무한대)
        long duration = 2000; // 애니메이션 지속 시간

        // 초기 화면 크기
        imageView.setScaleX(initialScale);
        imageView.setScaleY(initialScale);

        // 줌인
        ScaleAnimation scaleAnimation = new ScaleAnimation(initialScale, toX, initialScale, toY, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
        scaleAnimation.setDuration(duration);

        imageView.startAnimation(scaleAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, duration);
    }
}
