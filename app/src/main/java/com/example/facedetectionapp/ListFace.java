package com.example.facedetectionapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.model.clsFace;

import java.util.ArrayList;
import java.util.List;

public class ListFace extends AppCompatActivity {

    ListView lvDanhsach;
    Button btnDeleteFace,btnAddFace;
    ArrayAdapter<clsFace> adapterUser;
    clsFace selectedFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_face);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        lvDanhsach = findViewById(R.id.lvDanhsach);

        // Lấy danh sách đối tượng clsFace từ cơ sở dữ liệu
        List<clsFace> faceList = getFacesFromDatabase();

        // Sử dụng ArrayAdapter để hiển thị dữ liệu
        adapterUser = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, faceList);
        lvDanhsach.setAdapter(adapterUser);
        handleEvent();
    }

    private void handleEvent() {
        btnDeleteFace = findViewById(R.id.btnDeleteFace);
        btnAddFace = findViewById(R.id.btnAddFace);
        btnDeleteFace.setOnClickListener(v -> {
            if (selectedFace != null) {
                // Hiển thị hộp thoại xác nhận
                new AlertDialog.Builder(ListFace.this)
                        .setTitle("Xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa không?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            // Nếu chọn "Có", xóa khuôn mặt trong cơ sở dữ liệu
                            DeleteFace();
                        })
                        .setNegativeButton("Không", null)
                        .show();
            } else {
                Toast.makeText(ListFace.this, "Vui lòng chọn để xóa.", Toast.LENGTH_SHORT).show();
            }
        });
        lvDanhsach.setOnItemClickListener((parent, view, position, id) -> {
            // Lấy khuôn mặt đã chọn từ ListView
            selectedFace = (clsFace) parent.getItemAtPosition(position);
            Toast.makeText(ListFace.this, "Đã chọn: " + selectedFace.getName(), Toast.LENGTH_SHORT).show();
        });
        btnAddFace.setOnClickListener(v -> {
            Intent intent = new Intent(ListFace.this, CaptureFaceActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); //
        });
        lvDanhsach.setOnItemLongClickListener((parent, view, position, id) -> {
            // Lấy khuôn mặt đã chọn từ ListView
            selectedFace = (clsFace) parent.getItemAtPosition(position);

            // Hiển thị hình ảnh trong hộp thoại
            showImageDialog(selectedFace);

            // Trả về true để chỉ ra rằng sự kiện đã được xử lý
            return true;
        });

    }

    private List<clsFace> getFacesFromDatabase() {
        List<clsFace> faceList = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase("database_FaceDetection.db", MODE_PRIVATE, null);

        Cursor cursor = db.rawQuery("SELECT id, name, face_image FROM faces", null);

        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String name = cursor.getString(1);
            byte[] imageData = cursor.getBlob(2); // Đọc ảnh từ database

            clsFace face = new clsFace(id, name, imageData);
            faceList.add(face);
        }

        cursor.close();
        db.close();

        return faceList;
    }

    private void showImageDialog(clsFace face) {
        if (face.getFace_image() == null || face.getFace_image().length == 0) {
            Toast.makeText(this, "Không tìm thấy ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap faceBitmap = BitmapFactory.decodeByteArray(face.getFace_image(), 0, face.getFace_image().length);

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(faceBitmap);
        imageView.setPadding(20, 20, 20, 20);
        imageView.setAdjustViewBounds(true);

        new AlertDialog.Builder(this)
                .setTitle("Ảnh khuôn mặt: " + face.getName())
                .setView(imageView)
                .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void DeleteFace() {
        SQLiteDatabase db = openOrCreateDatabase("database_FaceDetection.db", MODE_PRIVATE, null);

        // Lấy id của khuôn mặt cần xóa
        String IdToDelete = selectedFace.getId();

        // Câu lệnh SQL để xóa khuôn mặt có face_id = faceIdToDelete
        int rowsDeleted = db.delete("faces", "id = ?", new String[]{IdToDelete});

        if (rowsDeleted > 0) {
            // Cập nhật lại danh sách sau khi xóa
            List<clsFace> updatedFaceList = getFacesFromDatabase();
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM faces", null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            adapterUser.clear();  // Xóa dữ liệu cũ trong adapter
            adapterUser.addAll(updatedFaceList); // Thêm dữ liệu mới vào adapter
            adapterUser.notifyDataSetChanged();  // Cập nhật giao diện
            if (count == 0) {
                // Nếu không còn dữ liệu thì reset ID về 1
                db.execSQL("DELETE FROM sqlite_sequence WHERE name='faces';");
            }

            // Hiển thị thông báo
            Toast.makeText(this, "Khuôn mặt đã được xóa", Toast.LENGTH_SHORT).show();
        } else {
            // Nếu không có dòng nào bị xóa, thông báo lỗi
            Toast.makeText(this, "Không tìm thấy khuôn mặt cần xóa", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }




}