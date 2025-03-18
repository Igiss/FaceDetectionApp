package com.example.model;

public class clsFace {
    private String id;    // Đổi từ ma thành id
    private String name;
    private  byte[] face_image;; // Dữ liệu khuôn mặt

    public clsFace() {
    }

    public clsFace(String id, String name, byte[] face_image) {
        this.id = id;
        this.name = name;
        this.face_image = face_image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getFace_image() {
        return face_image;
    }

    public void setFace_image(byte[] face_image) {
        this.face_image = face_image;
    }

    @Override
    public String toString() {
        return getId() + " \t" + getName();
    }
}
