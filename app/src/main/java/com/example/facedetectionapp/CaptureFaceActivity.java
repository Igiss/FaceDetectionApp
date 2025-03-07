package com.example.facedetectionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureFaceActivity extends AppCompatActivity {

    private PreviewView previewView;
    private FaceOverlayView faceOverlayView;
    private static final int REQUEST_CAMERA_PERMISSION = 100;


    ImageButton imgbtnCapture;
    private ExecutorService cameraExecutor;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_capture_face);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        previewView = findViewById(R.id.previewView);
        faceOverlayView = findViewById(R.id.faceOverlayView);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
            handleEvent();


        }
        cameraExecutor = Executors.newSingleThreadExecutor();




    }

    private void handleEvent() {
        imgbtnCapture = findViewById(R.id.imgbtnCapture);
        imgbtnCapture.setOnClickListener(view -> {
            takePicture();
        });

    }

    private void showNameInputDialog(byte[] imageBytes) {
        runOnUiThread(() -> {
            final EditText nameInput = new EditText(CaptureFaceActivity.this);
            nameInput.setHint("Nhập tên");

            AlertDialog.Builder builder = new AlertDialog.Builder(CaptureFaceActivity.this);
            builder.setTitle("Nhập tên")
                    .setMessage("Vui lòng nhập tên của bạn:")
                    .setView(nameInput)
                    .setPositiveButton("OK", (dialog, which) -> {
                        String name = nameInput.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(CaptureFaceActivity.this, "Tên không được để trống!", Toast.LENGTH_LONG).show();
                        } else {
                            // Sau khi có tên, lưu ảnh và tên vào cơ sở dữ liệu
                            saveImageAndNameToDatabase(name, imageBytes);
                            Intent intent = new Intent(CaptureFaceActivity.this, Home.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Để xóa CaptureFaceActivity khỏi stack
                            startActivity(intent);
                            finish(); // Đóng activity hiện tại để không quay lại
                        }
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

            builder.create().show();
        });
    }


    private ImageCapture imageCapture;
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture =new ImageCapture.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();


                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::detectFaces);

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis,imageCapture);

                Log.d("CameraX", "Camera đã khởi động thành công!");
            } catch (Exception e) {
                Log.e("CameraX", "Lỗi khi khởi động camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));

    }
    private File createTempFile() {
        try {
            // Tạo file tạm trong thư mục cache của ứng dụng
            File storageDir = getExternalFilesDir(null); // Hoặc dùng getCacheDir() nếu muốn lưu vào bộ nhớ trong
            return File.createTempFile("face_image_", ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void takePicture() {
        // Lấy ảnh đã chụp từ ImageCapture
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(createTempFile()).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Sau khi ảnh được chụp và lưu thành công vào file tạm, đọc ảnh vào bộ nhớ
                File photoFile = new File(outputFileResults.getSavedUri().getPath());
                try {
                    // Đọc ảnh vào một mảng byte
                    FileInputStream fileInputStream = new FileInputStream(photoFile);
                    byte[] imageBytes = new byte[(int) photoFile.length()];
                    fileInputStream.read(imageBytes);
                    fileInputStream.close();

                    // Gọi hàm lưu ảnh vào cơ sở dữ liệu
                    showNameInputDialog(imageBytes);




                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Lỗi khi chụp ảnh: " + exception.getMessage());
            }
        });
    }

    private void saveImageAndNameToDatabase(String name, byte[] imageBytes) {
        SQLiteDatabase db = openOrCreateDatabase("database_FaceDetection.db", MODE_PRIVATE, null);

        // Giả sử bạn có bảng 'faces' với các trường 'id', 'name' và 'face_data' kiểu BLOB
        ContentValues contentValues = new ContentValues();

        contentValues.put("face_data", imageBytes);
        contentValues.put("name",name);// Lưu mảng byte của ảnh vào cơ sở dữ liệu

        long result = db.insert("faces", null, contentValues);

        db.close();

        runOnUiThread(() -> {
            if (result == -1) {
                Log.e("Database", "Lưu ảnh vào cơ sở dữ liệu thất bại");
                Toast.makeText(this, "Lưu ảnh thất bại", Toast.LENGTH_LONG).show();
            } else {
                Log.d("Database", "Lưu ảnh thành công vào cơ sở dữ liệu");
                Toast.makeText(this, "Lưu ảnh thành công", Toast.LENGTH_LONG).show();
            }
        });
    }



    @OptIn(markerClass = ExperimentalGetImage.class)
    private void detectFaces(ImageProxy imageProxy) {
        if (imageProxy.getImage()== null){
            imageProxy.close();
        return; }
        try {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);
            detector.process(image)

                    .addOnFailureListener(e -> Log.e("ML Kit", "Lỗi phát hiện khuôn mặt: " + e.getMessage()))
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            Log.e("ML Kit", "Exception trong detectFaces: " + e.getMessage());
            imageProxy.close();
        }
    }
    protected void onDestroy(){
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}