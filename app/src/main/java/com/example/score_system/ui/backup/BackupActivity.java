package com.example.score_system.ui.backup;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.data.PreferencesManager;
import com.example.score_system.model.Course;
import com.example.score_system.model.Score;
import com.example.score_system.model.Student;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BackupActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private PreferencesManager prefsManager;

    private TextView tvEmailStatus;
    private Button btnBackup;
    private Button btnConfigEmail;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        dbHelper = new DatabaseHelper(this);
        prefsManager = new PreferencesManager(this);

        initViews();
        updateEmailStatus();
    }

    private void initViews() {
        tvEmailStatus = findViewById(R.id.tv_email_status);
        btnBackup = findViewById(R.id.btn_backup);
        btnConfigEmail = findViewById(R.id.btn_config_email);
        btnBack = findViewById(R.id.btn_back);

        btnBackup.setOnClickListener(v -> backupData());
        btnConfigEmail.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.score_system.ui.settings.EmailConfigActivity.class);
            startActivity(intent);
        });
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateEmailStatus();
    }

    private void updateEmailStatus() {
        String email = prefsManager.getEmail();
        if (email.isEmpty()) {
            tvEmailStatus.setText("未配置邮箱\n请先配置邮箱地址");
        } else {
            tvEmailStatus.setText("已配置邮箱: " + email + "\n点击发送将给配置邮箱账号发送备份邮件");
        }
    }

    private void backupData() {
        String email = prefsManager.getEmail();
        if (email.isEmpty()) {
            Toast.makeText(this, "请先配置邮箱地址", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 生成CSV文件
            File csvFile = generateCsvFile();

            if (csvFile == null || !csvFile.exists()) {
                Toast.makeText(this, "生成备份文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用 FileProvider 生成安全 URI
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    csvFile
            );

            // 创建邮件Intent，附带CSV文件
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            intent.putExtra(Intent.EXTRA_SUBJECT, "学生成绩数据备份_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
            intent.putExtra(Intent.EXTRA_TEXT, "附件是学生成绩数据备份文件（CSV格式），可用Excel打开");

            // 添加附件
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(Intent.createChooser(intent, "选择邮件应用发送"));
                Toast.makeText(this, "请选择邮件应用发送备份", Toast.LENGTH_SHORT).show();
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "未检测到邮件应用，请安装后重试", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "备份失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    private File generateCsvFile() {
        File csvFile = null;
        FileOutputStream fos = null;

        try {
            // 创建临时文件
            String fileName = "学生成绩备份_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
            csvFile = new File(getExternalCacheDir(), fileName);

            fos = new FileOutputStream(csvFile);

            // 获取所有课程
            List<Course> courses = dbHelper.getAllCourses();
            if (courses.isEmpty()) {
                Toast.makeText(this, "暂无课程数据", Toast.LENGTH_SHORT).show();
                return null;
            }

            StringBuilder csv = new StringBuilder();
            byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            fos.write(bom);

            // 构建CSV表头
            List<String> headers = new ArrayList<>();
            headers.add("课程名称");
            headers.add("学号");
            headers.add("姓名");
            headers.add("班级");

            // 按类型和次数分组统计
            Map<Integer, Integer> attendanceIndexes = new HashMap<>(); // 点名次数
            Map<Integer, Integer> homeworkIndexes = new HashMap<>();   // 作业次数
            Map<Integer, Integer> labIndexes = new HashMap<>();        // 上机次数

            for (Course course : courses) {
                List<Score> allScores = dbHelper.getAllScoresByCourse(course.getCourseId());

                for (Score score : allScores) {
                    switch (score.getScoreType()) {
                        case 1:
                            attendanceIndexes.put(score.getScoreIndex(), score.getScoreIndex());
                            break;
                        case 2:
                            homeworkIndexes.put(score.getScoreIndex(), score.getScoreIndex());
                            break;
                        case 3:
                            labIndexes.put(score.getScoreIndex(), score.getScoreIndex());
                            break;
                    }
                }
            }

            // 排序次数
            List<Integer> sortedAttendanceIndexes = new ArrayList<>(attendanceIndexes.keySet());
            Collections.sort(sortedAttendanceIndexes);

            List<Integer> sortedHomeworkIndexes = new ArrayList<>(homeworkIndexes.keySet());
            Collections.sort(sortedHomeworkIndexes);

            List<Integer> sortedLabIndexes = new ArrayList<>(labIndexes.keySet());
            Collections.sort(sortedLabIndexes);

            // 动态添加点名列
            for (Integer index : sortedAttendanceIndexes) {
                headers.add("点名第" + index + "次");
            }

            // 动态添加作业列
            for (Integer index : sortedHomeworkIndexes) {
                headers.add("作业第" + index + "次");
            }

            // 动态添加上机列
            for (Integer index : sortedLabIndexes) {
                headers.add("上机第" + index + "次");
            }

            // 写入表头
            csv.append(String.join(",", headers)).append("\n");

            // 遍历所有课程和学生
            for (Course course : courses) {
                List<Student> students = dbHelper.getCourseStudents(course.getCourseId());
                for (Student student : students) {
                    // 获取该学生该课程的所有成绩
                    List<Score> studentScores = dbHelper.getScoresByCourseAndStudent(
                            course.getCourseId(), student.getStudentId());

                    // 创建成绩映射
                    Map<String, Double> scoreMap = new HashMap<>();
                    for (Score score : studentScores) {
                        String key = score.getScoreType() + "_" + score.getScoreIndex();
                        scoreMap.put(key, score.getScoreValue());
                    }

                    List<String> row = new ArrayList<>();
                    row.add(escapeCSV(course.getCourseName()));  // 课程名称
                    row.add("'" + student.getStudentNumber());
                    row.add(escapeCSV(student.getStudentName()));
                    row.add(escapeCSV(student.getClassName()));

                    // 添加点名成绩
                    for (Integer index : sortedAttendanceIndexes) {
                        String key = "1_" + index;
                        if (scoreMap.containsKey(key)) {
                            double value = scoreMap.get(key);
                            row.add(String.valueOf((int) value)); // 点名是整数0或10
                        } else {
                            row.add("N/A");
                        }
                    }

                    // 添加作业成绩
                    for (Integer index : sortedHomeworkIndexes) {
                        String key = "2_" + index;
                        if (scoreMap.containsKey(key)) {
                            double value = scoreMap.get(key);
                            row.add(String.valueOf(value));
                        } else {
                            row.add("N/A");
                        }
                    }

                    // 添加上机成绩
                    for (Integer index : sortedLabIndexes) {
                        String key = "3_" + index;
                        if (scoreMap.containsKey(key)) {
                            double value = scoreMap.get(key);
                            row.add(String.valueOf(value));
                        } else {
                            row.add("N/A");
                        }
                    }

                    csv.append(String.join(",", row)).append("\n");
                }
            }

            // 写入文件
            String fileContent = csv.toString();
            fos.write(fileContent.getBytes("UTF-8"));

            return csvFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // CSV字段转义，处理包含逗号、引号、换行符的字段
    private String escapeCSV(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
