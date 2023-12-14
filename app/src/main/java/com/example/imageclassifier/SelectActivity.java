package com.example.imageclassifier;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SelectActivity extends AppCompatActivity {

    private static final String SELECTED_MODEL_EXTRA = "SELECTED_MODEL";

    private Button selectedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        Button leftButton = findViewById(R.id.left);
        Button rightButton = findViewById(R.id.right);

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 현재 선택된 버튼을 추적하고, 이전에 선택된 버튼을 되돌리기
                updateButtonState(leftButton);
                startMainActivity("turtlemodel_2.tflite");
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 현재 선택된 버튼을 추적하고, 이전에 선택된 버튼을 되돌리기
                updateButtonState(rightButton);
                startMainActivity("model_unquant.tflite");
            }
        });
    }

    private void updateButtonState(Button newSelectedButton) {
        // 이전에 선택된 버튼이 있다면, 원래의 상태로 되돌리기
        if (selectedButton != null) {
            selectedButton.setBackgroundResource(R.drawable.btn_background);
            selectedButton.setTextColor(getResources().getColor(R.color.white));
        }

        // 새로 선택된 버튼을 추적
        selectedButton = newSelectedButton;
        selectedButton.setBackgroundResource(R.drawable.btn_background2);
        selectedButton.setTextColor(getResources().getColor(R.color.black));
    }

    private void startMainActivity(String selectedModel) {
        Intent intent = new Intent(SelectActivity.this, MainActivity.class);
        intent.putExtra(SELECTED_MODEL_EXTRA, selectedModel);
        startActivity(intent);
    }
}