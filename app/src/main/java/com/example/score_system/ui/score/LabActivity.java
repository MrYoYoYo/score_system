package com.example.score_system.ui.score;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class LabActivity extends AppCompatActivity {
    private static final int TYPE_LAB = 3;
    private static final String TAG_ADD_NEW = "新增次数";

    private DatabaseHelper dbHelper;
    private int courseId;
    private String courseName;

    private List<Student> allStudentList;
    private List<Student> filteredStudentList;
    private List<String> labIndexList;
    private List<Integer> existingLabIndexes; // 已存在的次数
    private int currentIndex = 0;
    private int selectedLabIndex = 0;
    private int maxLabIndex = 0; // 当前最大次数
    private boolean hasUnsavedChanges = false;

    private TextView tvCourseName;
    private TextView tvStudentName;
    private EditText etSearch;
    private Spinner spLabIndex;
    private EditText etScore;
    private EditText etRemark;
    private ImageButton btnPreviousStudent;
    private ImageButton btnNextStudent;
    private Button btnSave;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab);

        courseId = getIntent().getIntExtra("courseId", 0);
        courseName = getIntent().getStringExtra("courseName");

        dbHelper = new DatabaseHelper(this);

        initViews();
        loadStudents();
        loadExistingLabIndexes();
        initLabIndexSpinner();
    }

    private void initViews() {
        tvCourseName = findViewById(R.id.tv_course_name);
        tvStudentName = findViewById(R.id.tv_student_name);
        etSearch = findViewById(R.id.et_search);
        spLabIndex = findViewById(R.id.sp_lab_index);
        etScore = findViewById(R.id.et_score);
        etRemark = findViewById(R.id.et_remark);
        btnPreviousStudent = findViewById(R.id.btn_previous_student);
        btnNextStudent = findViewById(R.id.btn_next_student);
        btnSave = findViewById(R.id.btn_save);
        btnFinish = findViewById(R.id.btn_finish);

        tvCourseName.setText(courseName);

        btnSave.setOnClickListener(v -> saveScore());
        btnFinish.setOnClickListener(v -> {
            if (hasUnsavedChanges) {
                showUnsavedWarning(() -> finish());
            } else {
                finish();
            }
        });

        btnPreviousStudent.setOnClickListener(v -> {
            if (currentIndex > 0) {
                if (hasUnsavedChanges) {
                    showUnsavedWarning(() -> {
                        currentIndex--;
                        showCurrentStudent();
                    });
                } else {
                    currentIndex--;
                    showCurrentStudent();
                }
            }
        });

        btnNextStudent.setOnClickListener(v -> {
            if (currentIndex < filteredStudentList.size() - 1) {
                if (hasUnsavedChanges) {
                    showUnsavedWarning(() -> {
                        currentIndex++;
                        showCurrentStudent();
                    });
                } else {
                    currentIndex++;
                    showCurrentStudent();
                }
            }
        });

        // 监听输入变化，标记为未保存
        etScore.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges = true;
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        etRemark.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges = true;
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
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

    private void loadExistingLabIndexes() {
        // 从数据库加载已存在的次数
        existingLabIndexes = new ArrayList<>();
        List<Score> allScores = dbHelper.getAllScoresByCourse(courseId);

        for (Score score : allScores) {
            if (score.getScoreType() == TYPE_LAB) {
                if (!existingLabIndexes.contains(score.getScoreIndex())) {
                    existingLabIndexes.add(score.getScoreIndex());
                }
            }
        }

        // 找出最大次数
        maxLabIndex = 0;
        for (Integer index : existingLabIndexes) {
            if (index > maxLabIndex) {
                maxLabIndex = index;
            }
        }
    }

    private void initLabIndexSpinner() {
        labIndexList = new ArrayList<>();

        // 添加已存在的次数
        for (Integer index : existingLabIndexes) {
            labIndexList.add("第 " + index + " 次上机");
        }

        // 添加"新增次数"选项
        labIndexList.add(TAG_ADD_NEW);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labIndexList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLabIndex.setAdapter(adapter);

        spLabIndex.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position == labIndexList.size() - 1) {
                    // 点击了"新增次数"
                    addNewLabIndex();
                } else {
                    // 选择了已存在的次数
                    if (!existingLabIndexes.isEmpty() && position < existingLabIndexes.size()) {
                        selectedLabIndex = existingLabIndexes.get(position);
                        // 切换次数时加载已有的分数
                        loadExistingScore();
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 如果有已存在的次数，默认选第一个
        if (!existingLabIndexes.isEmpty()) {
            selectedLabIndex = existingLabIndexes.get(0);
            loadExistingScore();
        }
    }

    private void addNewLabIndex() {
        // 新增次数：最大次数+1
        int newIndex = maxLabIndex + 1;
        existingLabIndexes.add(newIndex);
        maxLabIndex = newIndex;

        // 重新初始化Spinner
        initLabIndexSpinner();

        // 选中新添加的次数
        spLabIndex.setSelection(existingLabIndexes.size() - 1);
        selectedLabIndex = newIndex;

        Toast.makeText(this, "已添加第 " + newIndex + " 次上机", Toast.LENGTH_SHORT).show();
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
            etScore.setText("");
            etRemark.setText("");
        }
    }

    private void showCurrentStudent() {
        if (filteredStudentList.isEmpty()) {
            tvStudentName.setText("无学生信息");
            etScore.setText("");
            etRemark.setText("");
            return;
        }

        // 确保索引在有效范围内
        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= filteredStudentList.size()) currentIndex = filteredStudentList.size() - 1;

        Student student = filteredStudentList.get(currentIndex);
        tvStudentName.setText(student.getStudentName());

        // 加载当前学生当前次数的已有分数
        loadExistingScore();

        // 重置未保存标记
        hasUnsavedChanges = false;
    }

    private void loadExistingScore() {
        if (filteredStudentList.isEmpty() || selectedLabIndex == 0) return;

        Student student = filteredStudentList.get(currentIndex);
        List<Score> scores = dbHelper.getScoresByCourseAndStudent(courseId, student.getStudentId());

        etScore.setText("");
        etRemark.setText("");

        for (Score score : scores) {
            if (score.getScoreType() == TYPE_LAB && score.getScoreIndex() == selectedLabIndex) {
                etScore.setText(String.valueOf(score.getScoreValue()));
                if (score.getRemark() != null) {
                    etRemark.setText(score.getRemark());
                }
                break;
            }
        }

        // 重置未保存标记
        hasUnsavedChanges = false;
    }

    private void saveScore() {
        if (filteredStudentList.isEmpty()) {
            Toast.makeText(this, "暂无学生信息", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLabIndex == 0) {
            Toast.makeText(this, "请先选择或新增上机次数", Toast.LENGTH_SHORT).show();
            return;
        }

        Student student = filteredStudentList.get(currentIndex);
        String scoreStr = etScore.getText().toString().trim();

        if (scoreStr.isEmpty()) {
            Toast.makeText(this, "请输入分数", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double score = Double.parseDouble(scoreStr);
            if (score < 0 || score > 10) {
                Toast.makeText(this, "分数必须在0-10之间", Toast.LENGTH_SHORT).show();
                return;
            }

            String remark = etRemark.getText().toString().trim();

            Score labScore = new Score();
            labScore.setCourseId(courseId);
            labScore.setStudentId(student.getStudentId());
            labScore.setScoreType(TYPE_LAB);
            labScore.setScoreIndex(selectedLabIndex);
            labScore.setScoreValue(score);
            labScore.setRemark(remark);

            dbHelper.insertOrUpdateScore(labScore);
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();

            // 保存成功后重置标记
            hasUnsavedChanges = false;

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的分数", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUnsavedWarning(Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle("未保存的更改")
                .setMessage("当前输入的成绩未保存，确定要离开吗？")
                .setPositiveButton("确定离开", (dialog, which) -> onConfirm.run())
                .setNegativeButton("取消", null)
                .show();
    }
}
