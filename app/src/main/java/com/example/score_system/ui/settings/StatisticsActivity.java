package com.example.score_system.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;
import com.example.score_system.model.CourseConfig;
import com.example.score_system.model.Student;
import com.example.score_system.ui.score.ScoreAnalysisActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private List<Course> courseList;
    private int selectedCourseId = -1;

    private Spinner spinnerCourse;
    private TextView tvTotalStudents;
    private TextView tvAverageScore;
    private TextView tvPassRate;
    private TextView tvExcellentRate;
    private Button btnBack;
    private Button btnChartAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dbHelper = new DatabaseHelper(this);

        initViews();
        loadCourses();
        loadStatistics();
    }

    private void initViews() {
        spinnerCourse = findViewById(R.id.spinner_course);
        tvTotalStudents = findViewById(R.id.tv_total_students);
        tvAverageScore = findViewById(R.id.tv_average_score);
        tvPassRate = findViewById(R.id.tv_pass_rate);
        tvExcellentRate = findViewById(R.id.tv_excellent_rate);
        btnBack = findViewById(R.id.btn_back);
        btnChartAnalysis = findViewById(R.id.btn_chart_analysis);

        btnBack.setOnClickListener(v -> finish());
        btnChartAnalysis.setOnClickListener(v -> {
            if (selectedCourseId == -1) {
                Intent intent = new Intent(this, ScoreAnalysisActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ScoreAnalysisActivity.class);
                intent.putExtra("course_id", selectedCourseId);
                startActivity(intent);
            }
        });

        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < courseList.size()) {
                    selectedCourseId = courseList.get(position).getCourseId();
                    loadStatistics();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCourseId = -1;
                loadStatistics();
            }
        });
    }

    private void loadCourses() {
        courseList = dbHelper.getAllCourses();
        List<String> courseNames = new ArrayList<>();
        for (Course course : courseList) {
            courseNames.add(course.getCourseName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, courseNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(adapter);

        if (!courseList.isEmpty()) {
            selectedCourseId = courseList.get(0).getCourseId();
        }
    }

    private void loadStatistics() {
        try {
            if (selectedCourseId == -1) {
                // 显示所有课程的统计
                List<Course> courses = dbHelper.getAllCourses();
                List<Student> allStudents = dbHelper.getAllStudents();
                tvTotalStudents.setText("总学生数: " + allStudents.size());

                if (allStudents.isEmpty() || courses.isEmpty()) {
                    tvAverageScore.setText("平均分: N/A");
                    tvPassRate.setText("及格率: N/A");
                    tvExcellentRate.setText("优秀率: N/A");
                    return;
                }

                double totalScore = 0;
                int studentCourseCount = 0; // 学生-课程组合数
                int passCount = 0;
                int excellentCount = 0;

                for (Course course : courses) {
                    CourseConfig config = dbHelper.getCourseConfig(course.getCourseId());
                    if (config == null) {
                        continue;
                    }
                    // 只统计选了该课程的学生
                    List<Student> courseStudents = dbHelper.getCourseStudents(course.getCourseId());
                    for (Student student : courseStudents) {
                        double score = dbHelper.calculateTotalScore(
                                course.getCourseId(), student.getStudentId(), config
                        );
                        // 统计所有选课学生（没有成绩的会按满分计算）
                        totalScore += score;
                        studentCourseCount++;
                        if (score >= 60) passCount++;
                        if (score >= 85) excellentCount++;
                    }
                }

                if (studentCourseCount > 0) {
                    double average = totalScore / studentCourseCount;
                    double passRate = (passCount * 100.0 / studentCourseCount);
                    double excellentRate = (excellentCount * 100.0 / studentCourseCount);

                    tvAverageScore.setText(String.format("平均分: %.2f", average));
                    tvPassRate.setText(String.format("及格率: %.1f%%", passRate));
                    tvExcellentRate.setText(String.format("优秀率: %.1f%% (>=85分)", excellentRate));
                } else {
                    tvAverageScore.setText("平均分: N/A");
                    tvPassRate.setText("及格率: N/A");
                    tvExcellentRate.setText("优秀率: N/A");
                }
            } else {
                // 显示选中课程的统计
                Course selectedCourse = null;
                for (Course course : courseList) {
                    if (course.getCourseId() == selectedCourseId) {
                        selectedCourse = course;
                        break;
                    }
                }

                if (selectedCourse == null) {
                    tvTotalStudents.setText("该课程暂无学生");
                    tvAverageScore.setText("平均分: N/A");
                    tvPassRate.setText("及格率: N/A");
                    tvExcellentRate.setText("优秀率: N/A");
                    return;
                }

                // 只获取选了该课程的学生
                List<Student> courseStudents = dbHelper.getCourseStudents(selectedCourseId);
                tvTotalStudents.setText("课程选课学生数: " + courseStudents.size());

                if (courseStudents.isEmpty()) {
                    tvAverageScore.setText("平均分: N/A");
                    tvPassRate.setText("及格率: N/A");
                    tvExcellentRate.setText("优秀率: N/A");
                    return;
                }

                CourseConfig config = dbHelper.getCourseConfig(selectedCourseId);
                if (config == null) {
                    tvAverageScore.setText("平均分: N/A");
                    tvPassRate.setText("及格率: N/A");
                    tvExcellentRate.setText("优秀率: N/A");
                    return;
                }

                double totalScore = 0;
                int passCount = 0;
                int excellentCount = 0;

                for (Student student : courseStudents) {
                    double score = dbHelper.calculateTotalScore(
                            selectedCourseId, student.getStudentId(), config
                    );
                    // 统计所有选课学生（没有成绩的会按满分计算）
                    totalScore += score;
                    if (score >= 60) passCount++;
                    if (score >= 85) excellentCount++;
                }

                int studentCount = courseStudents.size();
                if (studentCount > 0) {
                    double average = totalScore / studentCount;
                    double passRate = (passCount * 100.0 / studentCount);
                    double excellentRate = (excellentCount * 100.0 / studentCount);

                    tvAverageScore.setText(String.format("平均分: %.2f", average));
                    tvPassRate.setText(String.format("及格率: %.1f%%", passRate));
                    tvExcellentRate.setText(String.format("优秀率: %.1f%% (>=85分)", excellentRate));
                } else {
                    tvAverageScore.setText("平均分: N/A");
                    tvPassRate.setText("及格率: N/A");
                    tvExcellentRate.setText("优秀率: N/A");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvAverageScore.setText("平均分: 计算错误");
            tvPassRate.setText("及格率: 计算错误");
            tvExcellentRate.setText("优秀率: 计算错误");
        }
    }
}
