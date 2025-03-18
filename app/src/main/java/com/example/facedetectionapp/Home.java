package com.example.facedetectionapp;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Home extends AppCompatActivity {
    static final String DATA_NAME = "database_FaceDetection.db";
    static final String DB_PATH = "/databases/";
    SQLiteDatabase database = null;
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
        processCopyDatabase();
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
    private void processCopyDatabase() {
        try {
            File file = getDatabasePath(DATA_NAME);
            if (!file.exists()) {
                copyDatabase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void copyDatabase() {
        try {
            InputStream myInput = getAssets().open(DATA_NAME);
            String outputFilename = getDatabasePath();
            File fileDb = new File(getApplicationInfo().dataDir + DB_PATH);
            if (!fileDb.exists()) {
                fileDb.mkdirs();
            }
            OutputStream output = new FileOutputStream(outputFilename);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();
            output.close();
            myInput.close();
            File copiedFile = new File(outputFilename);
            if (copiedFile.exists()) {
                Log.d("Database", "Sao chép database thành công! Kích thước: " + copiedFile.length() + " bytes");
            } else {
                Log.e("Database", "Database không tồn tại sau khi sao chép!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Database", "Lỗi khi sao chép database: " + e.getMessage());
        }
    }
    private String getDatabasePath() {
        return getApplicationInfo().dataDir + DB_PATH + DATA_NAME;
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