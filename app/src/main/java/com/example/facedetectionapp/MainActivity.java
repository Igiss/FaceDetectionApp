package com.example.facedetectionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private boolean isFrontCamera = false;
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

        //nút chuyển camera
        ImageButton btnSwitchCamera = findViewById(R.id.imgBtnSwitchCamera);
        if (btnSwitchCamera != null) {
            btnSwitchCamera.setOnClickListener(v -> {
                useFrontCamera = !useFrontCamera;
                startCamera();
            });
        }

    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(useFrontCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                        .build();

                isFrontCamera = useFrontCamera;

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::detectFaces);

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
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
        } // kiểm tra ảnh có null

        try {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage()// chuển đổi ảnh ImageProxy thành InputImage do ml kit yêu cầu InputImage
                    , imageProxy.getImageInfo().getRotationDegrees());// giúp ảnh quay đúng góc

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) //Độ chính xác cao
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // Phát hiện các đặc điểm như mắt, mũi, miệng
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)   // Nhận diện đường viền khuôn mặt
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) //Phân loại trạng thái khuôn mặt (mắt mở, miệng cười, v.v.)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.isEmpty()) {
                            Log.e("FaceRecognition", "No faces detected!");
                            faceOverlayView.setFaces(faces, new ArrayList<>(), image.getWidth(), image.getHeight(), isFrontCamera);// cập nhật faceOverlayView
                            return;
                        }

                        List<String> recognizedNameList = new ArrayList<>(); //Tạo một danh sách lưu tên khuôn mặt được nhận diện
                        for (Face face : faces) { // Duyệt qua từng khuôn mặt được phát hiện
                            float[] featureVector = extractFaceEmbedding(face); // Trích xuất vector đặc trưng

                            if (featureVector.length == 0) { // nếu length = 0 thì ko trích xuất đc đặc trưng
                                Log.e("FaceRecognition", "Feature vector is empty!");
                                recognizedNameList.add("Error");
                                continue;
                            }

                            Log.d("FaceRecognition", "Extracted feature vector: " + Arrays.toString(featureVector));

                            NameFace nameFace = new NameFace(getApplicationContext());// khởi tạo đối tượng NameFace để truy cập CSDL
                            String recognizedName = nameFace.getNameFromEmbedding(featureVector);// lấy tên khuôn mặt dựa trên vector đặc trưng
                            recognizedNameList.add(recognizedName); // Kết quả được thêm vào recognizedName
                        }

                        faceOverlayView.setFaces(faces, recognizedNameList, image.getWidth(), image.getHeight(), isFrontCamera); // cập nhật faceOverlayView
                    })
                    .addOnFailureListener(e -> Log.e("ML Kit", "Lỗi phát hiện khuôn mặt: " + e.getMessage()))// nếu có lỗi xảy ra trong quá trình phát hiện khuôn mặt
                    .addOnCompleteListener(task -> imageProxy.close());// đóng ảnh sau khi xử lý
        } catch (Exception e) {
            Log.e("ML Kit", "Exception trong detectFaces: " + e.getMessage());
            imageProxy.close();
        }
    }

    private float[] extractFaceEmbedding(Face face)  {// lấy khuôn mặt Từ ML kit
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
            if (i < keyPoints.size())  { //Nếu đủ keypoints
                featureVector[i * 2] = keyPoints.get(i).x;
                featureVector[i * 2 + 1] = keyPoints.get(i).y;
            } else {
                featureVector[i * 2] = -1.0f; // Nếu không đủ keypoints gàn = 1.0f do dễ dàng nhận diện và bỏ qua khi so sánh
                featureVector[i * 2 + 1] = -1.0f;
            }
        }

        Log.d("FaceRecognition", "Feature vector mới: " + Arrays.toString(featureVector) + " | Kích thước: " + featureVector.length);
        return featureVector;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy(); // Giải phóng tài nguyên khi Activity bị hủy.
        cameraExecutor.shutdown(); // tắt camera
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
