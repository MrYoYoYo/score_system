package com.example.score_system.model;

public class Course {
    private int courseId;
    private String courseNumber;
    private String courseName;

    public Course() {}

    public Course(String courseNumber, String courseName) {
        this.courseNumber = courseNumber;
        this.courseName = courseName;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseNumber() {
        return courseNumber;
    }

    public void setCourseNumber(String courseNumber) {
        this.courseNumber = courseNumber;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
}

