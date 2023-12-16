package com.example.imageclassifier;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SuccessActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        Button againButton = findViewById(R.id.again);
        Button endButton = findViewById(R.id.end);

        // MediaPlayer 초기화 및 mp3 재생
        mediaPlayer = MediaPlayer.create(this, R.raw.stretch_success);
        mediaPlayer.start();

        // "도전" 버튼 클릭 시 SelectActivity로 이동
        againButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SuccessActivity.this, SelectActivity.class);
                startActivity(intent);
            }
        });

        // "종료" 버튼 클릭 시 InfoActivity로 이동
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SuccessActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        // MediaPlayer 정리
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        super.onDestroy();
    }
}
