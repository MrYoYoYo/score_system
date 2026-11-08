package com.example.score_system.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecyclerView rvCourses;
    private CourseConfigAdapter courseAdapter;
    private Button btnEmailConfig;
    private Button btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        dbHelper = new DatabaseHelper(this);
        
        initViews();
        loadCourses();
    }
    
    private void initViews() {
        rvCourses = findViewById(R.id.rv_courses);
        rvCourses.setLayoutManager(new LinearLayoutManager(this));

        btnEmailConfig = findViewById(R.id.btn_email_config);
        btnBack = findViewById(R.id.btn_back);

        btnEmailConfig.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmailConfigActivity.class);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }
    
    private void loadCourses() {
        List<Course> courseList = dbHelper.getAllCourses();
        courseAdapter = new CourseConfigAdapter(courseList, this, course -> {
            Intent intent = new Intent(this, WeightConfigActivity.class);
            intent.putExtra("courseId", course.getCourseId());
            intent.putExtra("courseName", course.getCourseName());
            startActivity(intent);
        });
        rvCourses.setAdapter(courseAdapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }
}
