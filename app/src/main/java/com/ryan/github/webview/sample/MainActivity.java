package com.ryan.github.webview.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static boolean sUseWebViewPool = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CheckBox checkBox = findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> sUseWebViewPool = isChecked);

        findViewById(R.id.test_btn).setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WebViewActivity.class);
            startActivity(intent);
        });
    }
}


