package com.example.model;

public class clsFace {
    private String id;    // Đổi từ ma thành id
    private String name;

    public clsFace() {
    }

    public clsFace(String id, String name) {
        this.id = id;
        this.name = name;
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

    @Override
    public String toString() {
        return getId() + "\t" + getName();
    }
}
