package com.example.score_system.ui.course;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;

import java.util.List;

public class CourseActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecyclerView rvCourses;
    private CourseAdapter courseAdapter;
    private Button btnAddCourse;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);

        dbHelper = new DatabaseHelper(this);
        
        initViews();
        loadCourses();
    }

    private void initViews() {
        rvCourses = findViewById(R.id.rv_courses);
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        
        btnAddCourse = findViewById(R.id.btn_add_course);
        btnBack = findViewById(R.id.btn_back);
        
        btnAddCourse.setOnClickListener(v -> showAddCourseDialog());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCourses() {
        List<Course> courseList = dbHelper.getAllCourses();
        courseAdapter = new CourseAdapter(courseList, this, new CourseAdapter.CourseClickListener() {
            @Override
            public void onEditClick(Course course) {
                showEditCourseDialog(course);
            }

            @Override
            public void onDeleteClick(Course course) {
                showDeleteConfirmDialog(course);
            }
        });
        rvCourses.setAdapter(courseAdapter);
    }

    private void showAddCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加课程");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_course, null);
        EditText etCourseNumber = dialogView.findViewById(R.id.et_course_number);
        EditText etCourseName = dialogView.findViewById(R.id.et_course_name);

        builder.setView(dialogView);
        builder.setPositiveButton("添加", (dialog, which) -> {
            String courseNumber = etCourseNumber.getText().toString().trim();
            String courseName = etCourseName.getText().toString().trim();

            if (courseNumber.isEmpty() || courseName.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }

            Course course = new Course(courseNumber, courseName);
            long result = dbHelper.insertCourse(course);
            if (result > 0) {
                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                loadCourses();
            } else {
                Toast.makeText(this, "添加失败，课程号可能已存在", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showEditCourseDialog(Course course) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑课程");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_course, null);
        EditText etCourseNumber = dialogView.findViewById(R.id.et_course_number);
        EditText etCourseName = dialogView.findViewById(R.id.et_course_name);

        etCourseNumber.setText(course.getCourseNumber());
        etCourseName.setText(course.getCourseName());

        builder.setView(dialogView);
        builder.setPositiveButton("保存", (dialog, which) -> {
            course.setCourseNumber(etCourseNumber.getText().toString().trim());
            course.setCourseName(etCourseName.getText().toString().trim());

            int result = dbHelper.updateCourse(course);
            if (result > 0) {
                Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show();
                loadCourses();
            } else {
                Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteConfirmDialog(Course course) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除课程 " + course.getCourseName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    int result = dbHelper.deleteCourse(course.getCourseId());
                    if (result > 0) {
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        loadCourses();
                    } else {
                        Toast.makeText(this, "删除失败：已有签到记录的课程需保留", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }
}
