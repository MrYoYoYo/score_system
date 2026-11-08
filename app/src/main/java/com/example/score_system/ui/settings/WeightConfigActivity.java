package com.example.score_system.ui.settings;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.CourseConfig;

public class WeightConfigActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int courseId;
    private String courseName;
    
    private TextView tvCourseName;
    private EditText etAttendanceWeight;
    private EditText etHomeworkWeight;
    private EditText etLabWeight;
    private Button btnSave;
    private Button btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_config);
        
        courseId = getIntent().getIntExtra("courseId", 0);
        courseName = getIntent().getStringExtra("courseName");
        
        dbHelper = new DatabaseHelper(this);
        
        initViews();
        loadConfig();
    }
    
    private void initViews() {
        tvCourseName = findViewById(R.id.tv_course_name);
        etAttendanceWeight = findViewById(R.id.et_attendance_weight);
        etHomeworkWeight = findViewById(R.id.et_homework_weight);
        etLabWeight = findViewById(R.id.et_lab_weight);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);
        
        tvCourseName.setText(courseName);
        
        btnSave.setOnClickListener(v -> saveConfig());
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void loadConfig() {
        CourseConfig config = dbHelper.getCourseConfig(courseId);
        etAttendanceWeight.setText(String.valueOf(config.getAttendanceWeight()));
        etHomeworkWeight.setText(String.valueOf(config.getHomeworkWeight()));
        etLabWeight.setText(String.valueOf(config.getLabWeight()));
    }
    
    private void saveConfig() {
        String attendanceStr = etAttendanceWeight.getText().toString().trim();
        String homeworkStr = etHomeworkWeight.getText().toString().trim();
        String labStr = etLabWeight.getText().toString().trim();
        
        if (attendanceStr.isEmpty() || homeworkStr.isEmpty() || labStr.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double attendance = Double.parseDouble(attendanceStr);
            double homework = Double.parseDouble(homeworkStr);
            double lab = Double.parseDouble(labStr);
            
            double total = attendance + homework + lab;
            if (Math.abs(total - 1.0) > 0.01) {
                Toast.makeText(this, "权重总和必须等于1.0", Toast.LENGTH_SHORT).show();
                return;
            }
            
            CourseConfig config = new CourseConfig(courseId);
            config.setAttendanceWeight(attendance);
            config.setHomeworkWeight(homework);
            config.setLabWeight(lab);
            
            dbHelper.updateCourseConfig(config);
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }
}
