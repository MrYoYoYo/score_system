package com.example.score_system.ui.score;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;
import android.widget.AdapterView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.CourseConfig;
import com.example.score_system.model.Score;
import com.example.score_system.model.Student;

import java.util.ArrayList;
import java.util.List;

public class ScoreSummaryActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int courseId;
    private String courseName;
    
    private Spinner spStudents;
    private RecyclerView rvScores;
    private TextView tvTotalScore;
    private Button btnBack;
    
    private List<Student> studentList;
    private Student selectedStudent;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_summary);
        
        courseId = getIntent().getIntExtra("courseId", 0);
        courseName = getIntent().getStringExtra("courseName");
        
        dbHelper = new DatabaseHelper(this);
        
        initViews();
        loadStudents();
    }
    
    private void initViews() {
        spStudents = findViewById(R.id.sp_students);
        rvScores = findViewById(R.id.rv_scores);
        tvTotalScore = findViewById(R.id.tv_total_score);
        btnBack = findViewById(R.id.btn_back);
        
        rvScores.setLayoutManager(new LinearLayoutManager(this));
        btnBack.setOnClickListener(v -> finish());
        
        spStudents.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedStudent = studentList.get(position);
                loadScores();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void loadStudents() {
        // 只获取选修了该课程的学生
        studentList = dbHelper.getCourseStudents(courseId);

        if (studentList.isEmpty()) {
            // 显示提示信息
            tvTotalScore.setText("该课程暂无选课学生");
            return;
        }

        List<String> studentNames = new ArrayList<>();
        for (Student student : studentList) {
            studentNames.add(student.getStudentName() + " (" + student.getStudentNumber() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, studentNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStudents.setAdapter(adapter);
    }
    
    private void loadScores() {
        if (selectedStudent == null) return;

        List<Score> scores = dbHelper.getScoresByCourseAndStudent(courseId, selectedStudent.getStudentId());
        CourseConfig config = dbHelper.getCourseConfig(courseId);

        ScoreSummaryAdapter scoreAdapter = new ScoreSummaryAdapter(scores, this);
        scoreAdapter.setOnScoreChangedListener(() -> {
            // 刷新总评成绩
            double totalScore = dbHelper.calculateTotalScore(courseId, selectedStudent.getStudentId(), config);
            tvTotalScore.setText(String.format("总评成绩: %.2f", totalScore));
        });
        rvScores.setAdapter(scoreAdapter);

        double totalScore = dbHelper.calculateTotalScore(courseId, selectedStudent.getStudentId(), config);
        tvTotalScore.setText(String.format("总评成绩: %.2f", totalScore));
    }
}
