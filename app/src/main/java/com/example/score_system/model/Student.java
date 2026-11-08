package com.example.score_system.model;

public class Student {
    private int studentId;
    private String studentNumber;
    private String studentName;
    private String className;

    public Student() {}

    public Student(String studentNumber, String studentName, String className) {
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.className = className;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}

