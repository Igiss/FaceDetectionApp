package com.example.facedetectionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureFaceActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_capture_face);
        previewView = findViewById(R.id.previewView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
        cameraExecutor = Executors.newSingleThreadExecutor();

        ImageButton imgbtnCapture = findViewById(R.id.imgbtnCapture);
        imgbtnCapture.setOnClickListener(view -> takePicture());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                Log.e("CameraX", "Lỗi khi khởi động camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                processImage(image);
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Lỗi khi chụp ảnh: " + exception.getMessage());
            }
        });
    }
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy) {
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        Log.e("FaceRecognition", "Không tìm thấy khuôn mặt!");
                        return;
                    }
                    float[] featureVector = extractFaceEmbedding(faces.get(0));
                    showNameInputDialog(featureVector);
                })
                .addOnFailureListener(e -> Log.e("ML Kit", "Lỗi nhận diện khuôn mặt: " + e.getMessage()));
    }

    private void showNameInputDialog(float[] featureVector) {
        runOnUiThread(() -> {
            final EditText nameInput = new EditText(CaptureFaceActivity.this);
            nameInput.setHint("Nhập tên");

            new AlertDialog.Builder(CaptureFaceActivity.this)
                    .setTitle("Nhập tên")
                    .setMessage("Vui lòng nhập tên của bạn:")
                    .setView(nameInput)
                    .setPositiveButton("OK", (dialog, which) -> {
                        String name = nameInput.getText().toString().trim();
                        if (!name.isEmpty()) {
                            saveImageAndNameToDatabase(name, featureVector);
                        }
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .create().show();
        });
    }

    private void saveImageAndNameToDatabase(String name, float[] featureVector) {
        if (featureVector == null || featureVector.length == 0) {
            Log.e("Database", "Feature vector trống! Không lưu vào database.");
            return;
        }

        Log.d("Database", "Đang lưu feature vector vào database: " + Arrays.toString(featureVector) + " | Kích thước: " + featureVector.length);

        SQLiteDatabase db = openOrCreateDatabase("database_FaceDetection.db", MODE_PRIVATE, null);

        // Chuyển đổi float[] thành byte[]
        ByteBuffer buffer = ByteBuffer.allocate(featureVector.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        for (float v : featureVector) {
            buffer.putFloat(v);
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("face_data", buffer.array());

        long result = db.insert("faces", null, contentValues);
        db.close();

        if (result == -1) {
            Toast.makeText(this, "Lưu vào database thất bại!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Lưu vào database thành công!", Toast.LENGTH_LONG).show();
        }
    }



    private float[] extractFaceEmbedding(Face face) {
        if (face == null) {
            Log.e("FaceRecognition", "Face object is null!");
            return new float[0];
        }

        List<PointF> keyPoints = new ArrayList<>();

        if (face.getContour(FaceContour.LEFT_EYE) != null)
            keyPoints.addAll(face.getContour(FaceContour.LEFT_EYE).getPoints());

        if (face.getContour(FaceContour.RIGHT_EYE) != null)
            keyPoints.addAll(face.getContour(FaceContour.RIGHT_EYE).getPoints());

        if (face.getContour(FaceContour.NOSE_BRIDGE) != null)
            keyPoints.addAll(face.getContour(FaceContour.NOSE_BRIDGE).getPoints());

        if (face.getContour(FaceContour.NOSE_BOTTOM) != null)
            keyPoints.addAll(face.getContour(FaceContour.NOSE_BOTTOM).getPoints());

        if (face.getContour(FaceContour.UPPER_LIP_TOP) != null)
            keyPoints.addAll(face.getContour(FaceContour.UPPER_LIP_TOP).getPoints());

        if (face.getContour(FaceContour.LOWER_LIP_BOTTOM) != null)
            keyPoints.addAll(face.getContour(FaceContour.LOWER_LIP_BOTTOM).getPoints());

        if (face.getContour(FaceContour.FACE) != null)
            keyPoints.addAll(face.getContour(FaceContour.FACE).getPoints());

        Log.d("FaceRecognition", "Số key points trích xuất: " + keyPoints.size());

        // Nếu số keypoints < 64, không đủ dữ liệu để tạo vector 128 phần tử
        if (keyPoints.size() < 64) {
            Log.e("FaceRecognition", "Không đủ keypoints! Số lượng: " + keyPoints.size());
            return new float[0];
        }

        // Đảm bảo feature vector luôn có đúng 128 phần tử
        float[] featureVector = new float[128];
        for (int i = 0; i < 64; i++) {
            if (i < keyPoints.size()) {
                featureVector[i * 2] = keyPoints.get(i).x;
                featureVector[i * 2 + 1] = keyPoints.get(i).y;
            } else {
                featureVector[i * 2] = -1.0f; // Gán giá trị -1 thay vì 0.0 để dễ debug
                featureVector[i * 2 + 1] = -1.0f;
            }
        }

        Log.d("FaceRecognition", "Feature vector mới: " + Arrays.toString(featureVector) + " | Kích thước: " + featureVector.length);
        return featureVector;
    }



}
