package com.example.score_system.model;

public class Teacher {
    private int teacherId;
    private String username;
    private String password;

    public Teacher() {}

    public Teacher(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
