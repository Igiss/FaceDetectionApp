package com.example.facedetectionapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Home extends AppCompatActivity {

    Button btnAddFace,btnFaceDetection, btnListFace;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        handleEvents();
    }

    private void handleEvents() {
        btnAddFace = findViewById(R.id.btnAddFace);
        btnFaceDetection = findViewById(R.id.btnFaceDetection);
        btnListFace = findViewById(R.id.btnListFace);
        btnAddFace.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, CaptureFaceActivity.class);
            startActivity(intent);
        });
        btnFaceDetection.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
        });
        btnListFace.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, ListFace.class);

            startActivity(intent);
        });


    }
}