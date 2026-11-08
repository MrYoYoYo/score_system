package com.example.score_system.ui.score;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;
import com.example.score_system.model.CourseConfig;
import com.example.score_system.model.Student;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreAnalysisActivity extends AppCompatActivity {
    private static final String TAG = "ScoreAnalysisActivity";

    private DatabaseHelper dbHelper;
    private Spinner spCourse;
    private PieChart pieChart;
    private BarChart barChart;
    private Button btnBack;

    private List<Course> courseList;
    private int selectedCourseId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: 开始创建");
        try {
            setContentView(R.layout.activity_score_analysis);
            Log.d(TAG, "onCreate: 布局加载成功");

            dbHelper = new DatabaseHelper(this);
            Log.d(TAG, "onCreate: 数据库初始化成功");

            initViews();
            Log.d(TAG, "onCreate: 视图初始化成功");

            loadCourses();
            Log.d(TAG, "onCreate: 课程加载成功");

            setupChartStyles();
            Log.d(TAG, "onCreate: 图表样式设置成功");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: 初始化失败", e);
            e.printStackTrace();
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        Log.d(TAG, "initViews: 开始初始化视图");
        try {
            spCourse = findViewById(R.id.sp_course);
            Log.d(TAG, "initViews: spCourse = " + spCourse);

            pieChart = findViewById(R.id.pie_chart);
            Log.d(TAG, "initViews: pieChart = " + pieChart);

            barChart = findViewById(R.id.bar_chart);
            Log.d(TAG, "initViews: barChart = " + barChart);

            btnBack = findViewById(R.id.btn_back);
            Log.d(TAG, "initViews: btnBack = " + btnBack);
        } catch (Exception e) {
            Log.e(TAG, "initViews: 视图初始化失败", e);
            throw e;
        }

        // 课程选择监听
        spCourse.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected: position=" + position + ", id=" + id);
                if (position > 0 && courseList.size() > position - 1) {
                    selectedCourseId = courseList.get(position - 1).getCourseId();
                    Log.d(TAG, "onItemSelected: selectedCourseId=" + selectedCourseId);
                    updateCharts();
                } else {
                    Log.d(TAG, "onItemSelected: position=0, 不更新图表");
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                Log.d(TAG, "onNothingSelected: 没有选择任何项");
            }
        });

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCourses() {
        Log.d(TAG, "loadCourses: 开始加载课程");
        try {
            courseList = dbHelper.getAllCourses();
            Log.d(TAG, "loadCourses: 获取到 " + courseList.size() + " 门课程");

            List<String> courseNames = new ArrayList<>();
            courseNames.add("请选择课程");

            for (Course course : courseList) {
                courseNames.add(course.getCourseName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, courseNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCourse.setAdapter(adapter);
            Log.d(TAG, "loadCourses: Spinner适配器设置成功");
        } catch (Exception e) {
            Log.e(TAG, "loadCourses: 加载课程失败", e);
            throw e;
        }
    }

    private void setupChartStyles() {
        Log.d(TAG, "setupChartStyles: 开始设置图表样式");
        try {
        // 扇形图样式设置
        pieChart.setUsePercentValues(true);
        pieChart.getLegend().setEnabled(true);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDescription(null);
        pieChart.setCenterText("成绩分布");
        pieChart.setCenterTextSize(16f);

        // 柱状图样式设置
        barChart.getLegend().setEnabled(true);
        barChart.setDescription(null);

        // X轴设置
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(0f);
        xAxis.setTextSize(11f);
        xAxis.setGranularity(1f);

        // Y轴设置
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(100);
        barChart.getAxisRight().setEnabled(false);
        Log.d(TAG, "setupChartStyles: 图表样式设置完成");
        } catch (Exception e) {
            Log.e(TAG, "setupChartStyles: 图表样式设置失败", e);
            throw e;
        }
    }

    private void updateCharts() {
        Log.d(TAG, "updateCharts: selectedCourseId=" + selectedCourseId);
        if (selectedCourseId == 0) {
            Log.d(TAG, "updateCharts: selectedCourseId为0，返回");
            return;
        }

        // 更新扇形图
        updatePieChart();

        // 更新柱状图
        updateBarChart();
    }

    /**
     * 更新成绩分布扇形图
     */
    private void updatePieChart() {
        Log.d(TAG, "updatePieChart: 开始更新饼图");
        try {
            // 只获取选了该课程的学生
            List<Student> students = dbHelper.getCourseStudents(selectedCourseId);
            CourseConfig config = dbHelper.getCourseConfig(selectedCourseId);

            if (students.isEmpty()) {
                Toast.makeText(this, "该课程暂无选课学生", Toast.LENGTH_SHORT).show();
                pieChart.setData(null);
                pieChart.invalidate();
                return;
            }

            Log.d(TAG, "updatePieChart: 选课学生数=" + students.size() + ", config=" + config);

            if (config == null) {
                Toast.makeText(this, "该课程未配置权重", Toast.LENGTH_SHORT).show();
                pieChart.setData(null);
                pieChart.invalidate();
                return;
            }

            // 统计各等级人数
            int excellent = 0; // 85-100
            int good = 0;      // 75-84
            int medium = 0;    // 70-74
            int pass = 0;      // 60-69
            int fail = 0;      // <60

            for (Student student : students) {
                double totalScore = dbHelper.calculateTotalScore(selectedCourseId, student.getStudentId(), config);
                Log.d(TAG, "updatePieChart: 学生=" + student.getStudentName() + ", 总分=" + totalScore);

                // 统计所有学生（没有成绩的会按满分计算）
                if (totalScore >= 85) {
                    excellent++;
                } else if (totalScore >= 75) {
                    good++;
                } else if (totalScore >= 70) {
                    medium++;
                } else if (totalScore >= 60) {
                    pass++;
                } else {
                    fail++;
                }
            }

            Log.d(TAG, "updatePieChart: 优秀=" + excellent + ", 良好=" + good + ", 中等=" + medium + ", 及格=" + pass + ", 不及格=" + fail);

            // 准备扇形图数据
            List<PieEntry> entries = new ArrayList<>();
            if (excellent > 0) entries.add(new PieEntry(excellent, "优秀"));
            if (good > 0) entries.add(new PieEntry(good, "良好"));
            if (medium > 0) entries.add(new PieEntry(medium, "中等"));
            if (pass > 0) entries.add(new PieEntry(pass, "及格"));
            if (fail > 0) entries.add(new PieEntry(fail, "不及格"));

            if (entries.isEmpty()) {
                Toast.makeText(this, "暂无成绩数据", Toast.LENGTH_SHORT).show();
                pieChart.setData(null);
                pieChart.invalidate();
                return;
            }

            // 设置扇形图数据
            PieDataSet dataSet = new PieDataSet(entries, "成绩等级分布");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextSize(12f);

            PieData pieData = new PieData(dataSet);
            pieChart.setData(pieData);
            pieChart.invalidate(); // 刷新图表
            Log.d(TAG, "updatePieChart: 饼图更新完成");
        } catch (Exception e) {
            Log.e(TAG, "updatePieChart: 更新饼图失败", e);
            Toast.makeText(this, "更新饼图失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //更新班级学情柱状图
    private void updateBarChart() {
        try {
            // 只获取选了该课程的学生
            List<Student> students = dbHelper.getCourseStudents(selectedCourseId);
            CourseConfig config = dbHelper.getCourseConfig(selectedCourseId);

            if (students.isEmpty()) {
                Toast.makeText(this, "该课程暂无选课学生", Toast.LENGTH_SHORT).show();
                barChart.setData(null);
                barChart.invalidate();
                return;
            }

            if (config == null) {
                Toast.makeText(this, "该课程未配置权重", Toast.LENGTH_SHORT).show();
                barChart.setData(null);
                barChart.invalidate();
                return;
            }

            // 按班级统计平均分（统计所有选课学生）
            Map<String, List<Double>> classScores = new HashMap<>();

            for (Student student : students) {
                String className = student.getClassName();
                double totalScore = dbHelper.calculateTotalScore(selectedCourseId, student.getStudentId(), config);

                // 统计所有学生（没有成绩的会按满分计算）
                if (!classScores.containsKey(className)) {
                    classScores.put(className, new ArrayList<>());
                }
                classScores.get(className).add(totalScore);
            }

            if (classScores.isEmpty()) {
                Toast.makeText(this, "暂无班级数据", Toast.LENGTH_SHORT).show();
                barChart.setData(null);
                barChart.invalidate();
                return;
            }

            // 计算各班平均分并准备柱状图数据
            List<BarEntry> entries = new ArrayList<>();
            List<String> classNames = new ArrayList<>();
            int index = 0;

            for (Map.Entry<String, List<Double>> entry : classScores.entrySet()) {
                String className = entry.getKey();
                List<Double> scores = entry.getValue();

                // 计算平均分
                double sum = 0;
                for (double score : scores) {
                    sum += score;
                }
                double avgScore = sum / scores.size();

                entries.add(new BarEntry(index, (float) avgScore));
                classNames.add(className);
                index++;
            }

            // 设置柱状图数据
            BarDataSet dataSet = new BarDataSet(entries, "班级平均分");
            dataSet.setColors(ColorTemplate.COLORFUL_COLORS);

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.6f);

            barChart.setData(barData);
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(classNames));
            barChart.setFitBars(true); // 使X轴标签适配
            barChart.invalidate(); // 刷新图表
        } catch (Exception e) {
            Log.e(TAG, "updateBarChart: 更新柱状图失败", e);
            Toast.makeText(this, "更新柱状图失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}