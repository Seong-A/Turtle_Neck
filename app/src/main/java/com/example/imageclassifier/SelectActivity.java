package com.example.imageclassifier;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SelectActivity extends AppCompatActivity {

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
                leftButton.setBackgroundResource(R.drawable.btn_background2);
                leftButton.setTextColor(getResources().getColor(R.color.black));
                Intent intent = new Intent(SelectActivity.this, MainActivity.class);
                startActivity(intent);


            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rightButton.setBackgroundResource(R.drawable.btn_background2);
                rightButton.setTextColor(getResources().getColor(R.color.black));
                Intent intent = new Intent(SelectActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });
    }
}
