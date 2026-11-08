package com.example.score_system.ui.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import android.widget.AdapterView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;
import com.example.score_system.model.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecyclerView rvStudents;
    private StudentAdapter studentAdapter;
    private Button btnAddStudent;
    private Button btnImportExcel;
    private Button btnBack;

    private static final int EXCEL_PICK_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        dbHelper = new DatabaseHelper(this);

        initViews();
        loadStudents();
    }

    private void initViews() {
        rvStudents = findViewById(R.id.rv_students);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        btnAddStudent = findViewById(R.id.btn_add_student);
        btnImportExcel = findViewById(R.id.btn_import_excel);
        btnBack = findViewById(R.id.btn_back);

        btnAddStudent.setOnClickListener(v -> showAddStudentDialog());
        btnImportExcel.setOnClickListener(v -> importExcel());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadStudents() {
        List<Student> studentList = dbHelper.getAllStudents();
        studentAdapter = new StudentAdapter(studentList, this, new StudentAdapter.StudentClickListener() {
            @Override
            public void onEditClick(Student student) {
                showEditStudentDialog(student);
            }

            @Override
            public void onDeleteClick(Student student) {
                showDeleteConfirmDialog(student);
            }
        });
        rvStudents.setAdapter(studentAdapter);
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加学生");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_student, null);
        EditText etStudentNumber = dialogView.findViewById(R.id.et_student_number);
        EditText etStudentName = dialogView.findViewById(R.id.et_student_name);
        EditText etClassName = dialogView.findViewById(R.id.et_class_name);
        RecyclerView rvCourses = dialogView.findViewById(R.id.rv_courses);

        // 获取所有课程
        List<Course> allCourses = dbHelper.getAllCourses();
        List<Integer> selectedCourseIds = new ArrayList<>();

        // 设置课程选择列表
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        CourseSelectionAdapter courseAdapter = new CourseSelectionAdapter(allCourses, selectedCourseIds, this);
        rvCourses.setAdapter(courseAdapter);

        builder.setView(dialogView);
        builder.setPositiveButton("添加", (dialog, which) -> {
            String studentNumber = etStudentNumber.getText().toString().trim();
            String studentName = etStudentName.getText().toString().trim();
            String className = etClassName.getText().toString().trim();

            if (studentNumber.isEmpty() || studentName.isEmpty() || className.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }

            Student student = new Student(studentNumber, studentName, className);
            long result = dbHelper.insertStudent(student);
            if (result > 0) {
                // 保存选课信息
                int studentId = (int) result;
                List<Integer> courseIds = courseAdapter.getSelectedCourseIds();
                if (!courseIds.isEmpty()) {
                    dbHelper.updateStudentEnrollments(studentId, courseIds);
                }

                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                loadStudents();
            } else {
                Toast.makeText(this, "添加失败，学号可能已存在", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showEditStudentDialog(Student student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑学生");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_student, null);
        EditText etStudentNumber = dialogView.findViewById(R.id.et_student_number);
        EditText etStudentName = dialogView.findViewById(R.id.et_student_name);
        EditText etClassName = dialogView.findViewById(R.id.et_class_name);
        RecyclerView rvCourses = dialogView.findViewById(R.id.rv_courses);

        etStudentNumber.setText(student.getStudentNumber());
        etStudentName.setText(student.getStudentName());
        etClassName.setText(student.getClassName());

        // 获取所有课程和该学生已选的课程
        List<Course> allCourses = dbHelper.getAllCourses();
        List<Integer> selectedCourseIds = dbHelper.getStudentCourseIds(student.getStudentId());

        // 设置课程选择列表
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        CourseSelectionAdapter courseAdapter = new CourseSelectionAdapter(allCourses, selectedCourseIds, this);
        rvCourses.setAdapter(courseAdapter);

        builder.setView(dialogView);
        builder.setPositiveButton("保存", (dialog, which) -> {
            student.setStudentNumber(etStudentNumber.getText().toString().trim());
            student.setStudentName(etStudentName.getText().toString().trim());
            student.setClassName(etClassName.getText().toString().trim());

            int result = dbHelper.updateStudent(student);
            if (result > 0) {
                // 更新选课信息
                List<Integer> courseIds = courseAdapter.getSelectedCourseIds();
                dbHelper.updateStudentEnrollments(student.getStudentId(), courseIds);

                Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show();
                loadStudents();
            } else {
                Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteConfirmDialog(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除学生 " + student.getStudentName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    int result = dbHelper.deleteStudent(student.getStudentId());
                    if (result > 0) {
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        loadStudents();
                    } else {
                        Toast.makeText(this, "删除失败：已绑定账号或已有考勤的学生需保留", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void importExcel() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择Excel文件"), EXCEL_PICK_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXCEL_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            ExcelImporter importer = new ExcelImporter(this, dbHelper);
            int importedCount = importer.importStudents(uri);

            if (importedCount > 0) {
                Toast.makeText(this, "成功导入 " + importedCount + " 名学生", Toast.LENGTH_SHORT).show();
                loadStudents();
            } else {
                Toast.makeText(this, "导入失败，请检查文件格式", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudents();
    }
}
