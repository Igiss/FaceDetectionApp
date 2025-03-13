package com.example.facedetectionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu); // R.menu.menu là ID của file menu.xml
        return true;
    }

    // Xử lý sự kiện khi người dùng chọn một mục trong menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Kiểm tra ID của mục được chọn
        if (item.getTitle().equals("details")) {
            // Xử lý khi chọn "details"
            // Ví dụ: Hiển thị thông báo
            showToast("Bạn đã chọn Details");
            return true;
        } else if (item.getTitle().equals("information")) {
            // Xử lý khi chọn "information"
            // Ví dụ: Hiển thị thông báo
            showToast("Bạn đã chọn Information");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Phương thức hiển thị thông báo
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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