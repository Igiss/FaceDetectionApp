package com.example.facedetectionapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class NameFace extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "database_FaceDetection.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "faces";


    public NameFace(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Không cần tạo bảng vì đã có database sẵn
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Không cần nâng cấp database
    }

    public String getNameFromEmbedding(float[] featureVector) {
        if (featureVector == null || featureVector.length == 0) {
            Log.e("FaceRecognition", "Feature vector is empty or null!");
            return "Error";
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name, face_data FROM " + TABLE_NAME, null);

        String bestMatch = "Error";
        double bestSimilarity = -1;

        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            byte[] storedVectorBytes = cursor.getBlob(1); // Lấy dữ liệu khuôn mặt từ cột face_data
            float[] storedVector = convertBlobToFeatureVector(storedVectorBytes); // Chuyển đổi dữ liệu thành vector đặc trưng

            if (storedVector.length == 0) {
                Log.e("FaceRecognition", "Stored feature vector is empty for: " + name);
                continue;
            }// Nếu vector đặc trưng rỗng thì bỏ qua

            double similarity = calculateCosineSimilarity(featureVector, storedVector);// Tính toán độ tương đồng
            Log.d("FaceRecognition", "Checking name: " + name + " with similarity: " + similarity);

            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = name;
            }
        }
        cursor.close();
        return bestSimilarity >= 0.998 ? bestMatch : "Error";
    }

    private float[] convertBlobToFeatureVector(byte[] blob) {
        if (blob == null || blob.length == 0) {
            Log.e("FaceRecognition", "Stored face data is empty or null!");
            return new float[0];
        } //nếu mảng rổng

        if (blob.length != 512) { // 128 phần tử * 4 bytes mỗi phần tử = 512 bytes do mỗi float = 4
            Log.e("FaceRecognition", "Feature vector có kích thước sai: " + (blob.length / 4));
            return new float[0];
        }

        FloatBuffer buffer = ByteBuffer.wrap(blob).order(ByteOrder.nativeOrder()).asFloatBuffer();// chuyển đổi mảng blob thành FloatBuffer
        float[] array = new float[128];// Tạo mảng float có kích thước 128
        buffer.get(array); // Lấy dữ liệu từ FloatBuffer và lưu vào mảng float

        Log.d("FaceRecognition", "Feature vector từ database: " + Arrays.toString(array) + " | Kích thước: " + array.length);
        return array;
    }

    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            Log.e("FaceRecognition", "Một trong hai vector rỗng! a.length=" + (a != null ? a.length : 0) + ", b.length=" + (b != null ? b.length : 0));
            return 0.0;
        }

        if (a.length != b.length) {
            Log.e("FaceRecognition", "Vector có kích thước không khớp! a.length=" + a.length + ", b.length=" + b.length);
            return 0.0;
        } // Cosine cần 2 vector cùng kích thuóc

        double dotProduct = 0.0, normA = 0.0, normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i]; // Tính tích vô hướng
            normA += a[i] * a[i];// Tính bình phương độ dài vector A
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            Log.e("FaceRecognition", "Có một vector toàn số 0! Không thể tính cosine similarity.");
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)); // Tính cosine similarity
        Log.d("FaceRecognition", "Cosine Similarity: " + similarity);
        return similarity;
    }

}
