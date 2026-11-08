package com.example.score_system.model;

public class CourseConfig {
    private int configId;
    private int courseId;
    private double attendanceWeight; // 考勤权重，默认0.2
    private double homeworkWeight; // 作业权重，默认0.3
    private double labWeight; // 上机权重，默认0.5

    public CourseConfig() {
        // 默认权重：考勤20%，作业30%，上机50%
        this.attendanceWeight = 0.2;
        this.homeworkWeight = 0.3;
        this.labWeight = 0.5;
    }

    public CourseConfig(int courseId) {
        this();
        this.courseId = courseId;
    }

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public double getAttendanceWeight() {
        return attendanceWeight;
    }

    public void setAttendanceWeight(double attendanceWeight) {
        this.attendanceWeight = attendanceWeight;
    }

    public double getHomeworkWeight() {
        return homeworkWeight;
    }

    public void setHomeworkWeight(double homeworkWeight) {
        this.homeworkWeight = homeworkWeight;
    }

    public double getLabWeight() {
        return labWeight;
    }

    public void setLabWeight(double labWeight) {
        this.labWeight = labWeight;
    }
}
