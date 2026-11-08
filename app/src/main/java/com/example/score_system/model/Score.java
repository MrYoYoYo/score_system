package com.example.score_system.model;

public class Score {
    public static final int TYPE_ATTENDANCE = 1; // 考勤
    public static final int TYPE_HOMEWORK = 2; // 作业
    public static final int TYPE_LAB = 3; // 上机

    private int scoreId;
    private int courseId;
    private int studentId;
    private int scoreType; // 1-考勤, 2-作业, 3-上机
    private int scoreIndex; // 第几次
    private double scoreValue; // 分数（0-10）
    private String remark; // 备注

    public Score() {}

    public Score(int courseId, int studentId, int scoreType, int scoreIndex, double scoreValue) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.scoreType = scoreType;
        this.scoreIndex = scoreIndex;
        this.scoreValue = scoreValue;
    }

    public int getScoreId() {
        return scoreId;
    }

    public void setScoreId(int scoreId) {
        this.scoreId = scoreId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getScoreType() {
        return scoreType;
    }

    public void setScoreType(int scoreType) {
        this.scoreType = scoreType;
    }

    public int getScoreIndex() {
        return scoreIndex;
    }

    public void setScoreIndex(int scoreIndex) {
        this.scoreIndex = scoreIndex;
    }

    public double getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(double scoreValue) {
        this.scoreValue = scoreValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

