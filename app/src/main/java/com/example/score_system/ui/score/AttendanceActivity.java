package com.example.score_system.ui.score;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Score;
import com.example.score_system.model.Student;

import java.util.ArrayList;
import java.util.List;

public class AttendanceActivity extends AppCompatActivity {
    private static final int TYPE_ATTENDANCE = 1;
    private static final String TAG_ADD_NEW = "新增次数";

    private DatabaseHelper dbHelper;
    private int courseId;
    private String courseName;

    private List<Student> allStudentList;
    private List<Student> filteredStudentList = new ArrayList<>();
    private List<String> attendanceIndexList;
    private List<Integer> existingAttendanceIndexes; // 已存在的次数
    private int currentIndex = 0;
    private int selectedAttendanceIndex = 0;
    private int maxAttendanceIndex = 0; // 当前最大次数

    private TextView tvCourseName;
    private TextView tvStudentName;
    private TextView tvStudentNumber;
    private EditText etSearch;
    private Spinner spAttendanceIndex;
    private ImageButton btnPreviousStudent;
    private ImageButton btnNextStudent;
    private Button btnAllPresent;
    private Button btnPresent;
    private Button btnAbsent;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        courseId = getIntent().getIntExtra("courseId", 0);
        courseName = getIntent().getStringExtra("courseName");

        dbHelper = new DatabaseHelper(this);

        initViews();
        loadStudents();
        loadExistingAttendanceIndexes();
        initAttendanceIndexSpinner();
    }

    private void initViews() {
        tvCourseName = findViewById(R.id.tv_course_name);
        tvStudentName = findViewById(R.id.tv_student_name);
        tvStudentNumber = findViewById(R.id.tv_student_number);
        etSearch = findViewById(R.id.et_search);
        spAttendanceIndex = findViewById(R.id.sp_attendance_index);
        btnPreviousStudent = findViewById(R.id.btn_previous_student);
        btnNextStudent = findViewById(R.id.btn_next_student);
        btnAllPresent = findViewById(R.id.btn_all_present);
        btnPresent = findViewById(R.id.btn_present);
        btnAbsent = findViewById(R.id.btn_absent);
        btnFinish = findViewById(R.id.btn_finish);

        tvCourseName.setText(courseName);

        btnPresent.setOnClickListener(v -> recordAttendance(true));
        btnAbsent.setOnClickListener(v -> recordAttendance(false));
        btnAllPresent.setOnClickListener(v -> markAllPresent());
        btnFinish.setOnClickListener(v -> finish());

        btnPreviousStudent.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showCurrentStudent();
            }
        });

        btnNextStudent.setOnClickListener(v -> {
            if (currentIndex < filteredStudentList.size() - 1) {
                currentIndex++;
                showCurrentStudent();
            }
        });

        // 搜索功能
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadExistingAttendanceIndexes() {
        // 从数据库加载已存在的次数
        existingAttendanceIndexes = new ArrayList<>();
        List<Score> allScores = dbHelper.getAllScoresByCourse(courseId);

        for (Score score : allScores) {
            if (score.getScoreType() == TYPE_ATTENDANCE) {
                if (!existingAttendanceIndexes.contains(score.getScoreIndex())) {
                    existingAttendanceIndexes.add(score.getScoreIndex());
                }
            }
        }

        // 找出最大次数
        maxAttendanceIndex = 0;
        for (Integer index : existingAttendanceIndexes) {
            if (index > maxAttendanceIndex) {
                maxAttendanceIndex = index;
            }
        }
    }

    private void initAttendanceIndexSpinner() {
        attendanceIndexList = new ArrayList<>();

        // 添加已存在的次数
        for (Integer index : existingAttendanceIndexes) {
            attendanceIndexList.add("第 " + index + " 次点名");
        }

        // 添加"新增次数"选项
        attendanceIndexList.add(TAG_ADD_NEW);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, attendanceIndexList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAttendanceIndex.setAdapter(adapter);

        spAttendanceIndex.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == attendanceIndexList.size() - 1) {
                    // 点击了"新增次数"
                    addNewAttendanceIndex();
                } else {
                    // 选择了已存在的次数
                    if (!existingAttendanceIndexes.isEmpty() && position < existingAttendanceIndexes.size()) {
                        selectedAttendanceIndex = existingAttendanceIndexes.get(position);
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 如果有已存在的次数，默认选第一个
        if (!existingAttendanceIndexes.isEmpty()) {
            selectedAttendanceIndex = existingAttendanceIndexes.get(0);
        }
    }

    private void addNewAttendanceIndex() {
        // 新增次数：最大次数+1
        int newIndex = maxAttendanceIndex + 1;
        existingAttendanceIndexes.add(newIndex);
        maxAttendanceIndex = newIndex;

        // 重新初始化Spinner
        initAttendanceIndexSpinner();

        // 选中新添加的次数
        spAttendanceIndex.setSelection(existingAttendanceIndexes.size() - 1);
        selectedAttendanceIndex = newIndex;

        Toast.makeText(this, "已添加第 " + newIndex + " 次点名", Toast.LENGTH_SHORT).show();
    }

    private void loadStudents() {
        allStudentList = dbHelper.getCourseStudents(courseId);
        if (allStudentList.isEmpty()) {
            Toast.makeText(this, "该课程暂无选课学生", Toast.LENGTH_SHORT).show();
            return;
        }
        filteredStudentList = new ArrayList<>(allStudentList);
        showCurrentStudent();
    }

    private void filterStudents(String query) {
        filteredStudentList.clear();

        if (query.isEmpty()) {
            filteredStudentList.addAll(allStudentList);
        } else {
            for (Student student : allStudentList) {
                if (student.getStudentName().contains(query) ||
                    student.getStudentNumber().contains(query)) {
                    filteredStudentList.add(student);
                }
            }
        }

        if (!filteredStudentList.isEmpty()) {
            currentIndex = 0;
            showCurrentStudent();
        } else {
            tvStudentName.setText("无匹配学生");
            tvStudentNumber.setText("");
        }
    }

    private void showCurrentStudent() {
        if (filteredStudentList.isEmpty()) {
            tvStudentName.setText("无学生信息");
            tvStudentNumber.setText("");
            return;
        }

        if (currentIndex >= filteredStudentList.size()) {
            currentIndex = filteredStudentList.size() - 1;
        }

        Student student = filteredStudentList.get(currentIndex);
        tvStudentName.setText(student.getStudentName());
        tvStudentNumber.setText(student.getStudentNumber());
    }

    private void recordAttendance(boolean isPresent) {
        if (filteredStudentList.isEmpty()) {
            Toast.makeText(this, "暂无学生信息", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAttendanceIndex == 0) {
            Toast.makeText(this, "请先选择或新增点名次数", Toast.LENGTH_SHORT).show();
            return;
        }

        Student student = filteredStudentList.get(currentIndex);
        double score = isPresent ? 10.0 : 0.0;

        Score attendanceScore = new Score();
        attendanceScore.setCourseId(courseId);
        attendanceScore.setStudentId(student.getStudentId());
        attendanceScore.setScoreType(TYPE_ATTENDANCE);
        attendanceScore.setScoreIndex(selectedAttendanceIndex);
        attendanceScore.setScoreValue(score);

        dbHelper.insertOrUpdateScore(attendanceScore);

        // 自动切换到下一个学生
        goToNextStudent();
    }

    private void goToNextStudent() {
        if (currentIndex < filteredStudentList.size() - 1) {
            currentIndex++;
            showCurrentStudent();
        } else {
            Toast.makeText(this, "已是最后一名学生", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAllPresent() {
        if (allStudentList.isEmpty()) {
            Toast.makeText(this, "暂无学生信息", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAttendanceIndex == 0) {
            Toast.makeText(this, "请先选择或新增点名次数", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("一键全到")
                .setMessage("确定为所有学生记录到课吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    int count = 0;
                    for (Student student : allStudentList) {
                        Score attendanceScore = new Score();
                        attendanceScore.setCourseId(courseId);
                        attendanceScore.setStudentId(student.getStudentId());
                        attendanceScore.setScoreType(TYPE_ATTENDANCE);
                        attendanceScore.setScoreIndex(selectedAttendanceIndex);
                        attendanceScore.setScoreValue(10.0);

                        dbHelper.insertOrUpdateScore(attendanceScore);
                        count++;
                    }
                    Toast.makeText(this, "已为 " + count + " 名学生记录到课", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
