package com.example.imageclassifier;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        Button againButton = findViewById(R.id.again);
        Button endButton = findViewById(R.id.end);

        // "도전" 버튼 클릭 시 MainActivity로 이동
        againButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SuccessActivity.this, MainActivity.class);
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
}
