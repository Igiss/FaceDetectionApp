package com.example.facedetectionapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.face.Face;

import java.util.List;

public class FaceOverlayView extends View {
    private final Paint paint;
    private Paint textPaint;

    private List<Face> faces;
    private List<String> recognizedNameList;  // Danh sách tên tương ứng với các khuôn mặt
    private int imageWidth, imageHeight;
    private boolean isFrontCamera;

    // Biến tái sử dụng để tối ưu hiệu suất
    private final RectF reusableBounds = new RectF();
    private final Matrix transformationMatrix = new Matrix();

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        textPaint = new Paint();
        textPaint.setColor(Color.RED);  // Màu chữ
        textPaint.setTextSize(50);      // Kích thước chữ
        textPaint.setStyle(Paint.Style.FILL);
    }

    // Cập nhật phương thức để nhận danh sách tên khuôn mặt
    public void setFaces(List<Face> faces, List<String> names, int imgWidth, int imgHeight, boolean frontCamera) {
        this.faces = faces;
        this.recognizedNameList = names;  // Cập nhật danh sách tên khuôn mặt
        this.imageWidth = imgWidth;
        this.imageHeight = imgHeight;
        this.isFrontCamera = frontCamera; // Cập nhật thông tin camera
        invalidate();  // Cập nhật giao diện
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (faces == null || imageWidth == 0 || imageHeight == 0 || recognizedNameList == null) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // Tính toán tỷ lệ scale và offset căn giữa
        float scale = Math.min(viewWidth / (float) imageWidth, viewHeight / (float) imageHeight);
        float dx = (viewWidth - (imageWidth * scale)) / 2f;
        float dy = (viewHeight - (imageHeight * scale)) / 2f;

        // Reset và thiết lập ma trận chuyển đổi
        transformationMatrix.reset();
        transformationMatrix.setScale(scale, scale);
        transformationMatrix.postTranslate(dx, dy);

        // Xử lý lật gương nếu camera trước
        if (isFrontCamera) {
            transformationMatrix.postScale(-1, 1, viewWidth / 2f, 0);
        }

        // Vẽ khung và tên tương ứng cho mỗi khuôn mặt
        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.get(i);
            String name = recognizedNameList.get(i);  // Lấy tên tương ứng với khuôn mặt

            reusableBounds.set(face.getBoundingBox()); // Dùng biến tái sử dụng
            transformationMatrix.mapRect(reusableBounds);
            adjustBounds(reusableBounds);  // Điều chỉnh kích thước khung

            // Vẽ khung và tên trên khuôn mặt
            canvas.drawRect(reusableBounds, paint);
            canvas.drawText(name, reusableBounds.centerX(), reusableBounds.top - 20, textPaint);  // Vẽ tên khuôn mặt
        }
    }

    private void adjustBounds(RectF bounds) {
        // Mở rộng bề ngang 80% và chiều cao 60%
        float extraWidth = bounds.width() * 0.8f;
        float extraHeight = bounds.height() * 0.6f;

        bounds.left -= extraWidth / 2;
        bounds.right += extraWidth / 2;
        bounds.top -= extraHeight / 2;
        bounds.bottom += extraHeight / 2;

        // Dịch lên trên 10% chiều cao
        float shiftUp = bounds.height() * 0.1f;
        bounds.top -= shiftUp;
        bounds.bottom -= shiftUp;
    }
}
