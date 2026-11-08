package com.example.score_system.ui.score;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;

import java.util.List;

public class ScoreActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecyclerView rvCourses;
    private CourseScoreAdapter courseAdapter;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        dbHelper = new DatabaseHelper(this);

        initViews();
        loadCourses();
    }

    private void initViews() {
        rvCourses = findViewById(R.id.rv_courses);
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCourses() {
        List<Course> courseList = dbHelper.getAllCourses();
        courseAdapter = new CourseScoreAdapter(courseList, this, course -> {
            showScoreTypeDialog(course);
        });
        rvCourses.setAdapter(courseAdapter);
    }

    private void showScoreTypeDialog(Course course) {
        String[] options = {"考勤录入（点名模式）", "平时作业录入", "上机实验录入", "成绩汇总录入"};
        
        new AlertDialog.Builder(this)
                .setTitle("选择录入方式 - " + course.getCourseName())
                .setItems(options, (dialog, which) -> {
                    Intent intent = new Intent();
                    switch (which) {
                        case 0:
                            intent = new Intent(this, AttendanceActivity.class);
                            break;
                        case 1:
                            intent = new Intent(this, HomeworkActivity.class);
                            break;
                        case 2:
                            intent = new Intent(this, LabActivity.class);
                            break;
                        case 3:
                            intent = new Intent(this, ScoreSummaryActivity.class);
                            break;
                    }
                    intent.putExtra("courseId", course.getCourseId());
                    intent.putExtra("courseName", course.getCourseName());
                    startActivity(intent);
                })
                .show();
    }
}
