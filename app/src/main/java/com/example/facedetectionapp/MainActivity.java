package com.example.facedetectionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView tvResult;
    private FaceOverlayView faceOverlayView;
    private ExecutorService cameraExecutor;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private boolean useFrontCamera = false; // Mặc định dùng camera sau

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo các view
        previewView = findViewById(R.id.previewView);
        tvResult = findViewById(R.id.tvResult);
        faceOverlayView = findViewById(R.id.faceOverlayView);

        // Kiểm tra quyền Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Nếu có nút chuyển camera, xử lý sự kiện (ví dụ: imgBtnSwitchCamera)
        ImageButton btnSwitchCamera = findViewById(R.id.imgBtnSwitchCamera);
        if (btnSwitchCamera != null) {
            btnSwitchCamera.setOnClickListener(v -> {
                useFrontCamera = !useFrontCamera;
                startCamera();
            });
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(useFrontCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::detectFaces);

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                Log.d("CameraX", "Camera đã khởi động thành công!");
            } catch (Exception e) {
                Log.e("CameraX", "Lỗi khi khởi động camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void detectFaces(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }
        try {
            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
                        );

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        runOnUiThread(() -> {
                            if (faces.size() > 0) {
                                tvResult.setText("Phát hiện " + faces.size() + " khuôn mặt!");
                            } else {
                                tvResult.setText("Không tìm thấy khuôn mặt.");
                            }
                            // Sử dụng biến useFrontCamera để xác định xem có lật ảnh hay không
                            faceOverlayView.setFaces(faces, image.getWidth(), image.getHeight(), useFrontCamera);
                        });
                    })
                    .addOnFailureListener(e -> Log.e("ML Kit", "Lỗi phát hiện khuôn mặt: " + e.getMessage()))
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            Log.e("ML Kit", "Exception trong detectFaces: " + e.getMessage());
            imageProxy.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.e("CameraX", "Quyền camera bị từ chối!");
            }
        }
    }
}
