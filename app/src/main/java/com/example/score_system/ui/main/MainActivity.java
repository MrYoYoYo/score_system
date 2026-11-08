package com.example.score_system.ui.main;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.score_system.R;
import com.example.score_system.data.AttendanceRepository;
import com.example.score_system.data.AttendanceRepository.Account;
import com.example.score_system.data.AttendanceRepository.Lesson;
import com.example.score_system.data.AttendanceRepository.Record;
import com.example.score_system.data.AttendanceRepository.Request;
import com.example.score_system.data.AttendanceRepository.Statistic;
import com.example.score_system.data.AttendanceRepository.Task;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.data.SessionManager;
import com.example.score_system.model.Course;
import com.example.score_system.model.Student;
import com.example.score_system.ui.backup.BackupActivity;
import com.example.score_system.ui.common.Ui;
import com.example.score_system.ui.course.CourseActivity;
import com.example.score_system.ui.login.LoginActivity;
import com.example.score_system.ui.score.ScoreActivity;
import com.example.score_system.ui.score.ShakeActivity;
import com.example.score_system.ui.student.StudentActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String[] DAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private AttendanceRepository repository;
    private DatabaseHelper helper;
    private Account account;
    private LinearLayout body;
    private BottomNavigationView navigation;
    private int selectedTab = R.id.navigation_home;
    private boolean resumed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            if (selectedTab != R.id.navigation_profile) render();
            handler.postDelayed(this, 15000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new AttendanceRepository(this);
        helper = new DatabaseHelper(this);
        account = repository.currentAccount();
        if (account == null) { openLogin(); return; }
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.main_root);
        body = findViewById(R.id.main_content);
        navigation = findViewById(R.id.bottom_navigation);
        int navigationPaddingLeft = navigation.getPaddingLeft();
        int navigationPaddingTop = navigation.getPaddingTop();
        int navigationPaddingRight = navigation.getPaddingRight();
        int navigationPaddingBottom = navigation.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            Insets navigationBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
            );
            view.setPadding(statusBars.left, statusBars.top, statusBars.right, 0);
            navigation.setPadding(
                    navigationPaddingLeft,
                    navigationPaddingTop,
                    navigationPaddingRight,
                    navigationPaddingBottom + navigationBars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
        if (state != null) selectedTab = state.getInt("tab", R.id.navigation_home);
        navigation.setOnItemSelectedListener(item -> {
            selectedTab = item.getItemId();
            ((ScrollView) findViewById(R.id.main_scroll)).scrollTo(0, 0);
            render();
            return true;
        });
        navigation.setSelectedItemId(selectedTab);
    }

    private void render() {
        if (body == null || isFinishing()) return;
        account = repository.currentAccount();
        if (account == null) { openLogin(); return; }
        body.removeAllViews();
        if (selectedTab == R.id.navigation_home) home();
        else if (selectedTab == R.id.navigation_timetable) timetable();
        else if (selectedTab == R.id.navigation_attendance) attendance();
        else if (selectedTab == R.id.navigation_statistics) statistics();
        else profile();
    }

    private void home() {
        Ui.heading(body, "班级点名", date(System.currentTimeMillis(), "M月d日 EEEE") + " · " + (account.isTeacher() ? "教师空间" : "学生空间"));
        List<Task> tasks = repository.tasks();
        int active = 0;
        for (Task task : tasks) if (task.active()) active++;
        Ui.hero(body, "你好，" + account.name, active > 0 ? "有 " + active + " 个课堂签到正在进行\n" + (account.isTeacher() ? "查看实时到课情况，让课堂更从容。" : "进入签到页面，轻轻摇动手机即可签到。") : "");
        if (active > 0) {
            Ui.section(body, "正在签到");
            for (Task task : tasks) if (task.active()) taskCard(body, task);
        }
        Ui.section(body, "今日课程");
        int today = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
        int count = 0;
        for (Lesson lesson : repository.lessons()) {
            if (lesson.weekday == today) { lessonCard(body, lesson, false); count++; }
        }
        if (count == 0) Ui.card(body, "今天暂无课程", account.isTeacher() ? "前往「课表」添加课程安排，上课时即可发起签到。" : "前往「课表」选择课程后，即可查看自己的上课安排。");
    }

    private void timetable() {
        Ui.heading(body, "我的课表", account.isTeacher() ? "" : "");
        if (account.isTeacher()) {
            LinearLayout tools = Ui.card(body, "课程与名单", "先创建课程、安排上课时间，再维护课程选课名单。");
            Ui.button(tools, "管理课程", () -> launch(CourseActivity.class));
            Ui.button(tools, "添加课表安排", true, () -> editLesson(null));
            Ui.button(tools, "管理学生与选课名单", true, () -> launch(StudentActivity.class));
        } else Ui.button(body, "管理我的选课", this::enrollmentDialog);
        List<Lesson> lessons = repository.lessons();
        int previous = -1;
        for (Lesson lesson : lessons) {
            if (lesson.weekday != previous) { Ui.section(body, DAYS[lesson.weekday - 1]); previous = lesson.weekday; }
            lessonCard(body, lesson, true);
        }
        if (lessons.isEmpty()) Ui.card(body, "课表还是空的", account.isTeacher() ? "添加课程后，点击「添加课表安排」设置时间和教室。" : "选择课程后，将显示教师已设置的每周课表。");
    }

    private void lessonCard(LinearLayout parent, Lesson lesson, boolean editing) {
        LinearLayout card = Ui.card(parent, lesson.courseName, clock(lesson.start) + " — " + clock(lesson.end) + "  ·  " + lesson.room + "\n" + lesson.teacher + (lesson.isNow() ? "  ·  正在上课" : ""));
        if (account.isTeacher()) {
            if (lesson.isNow()) Ui.button(card, "发起摇一摇签到", () -> publishDialog(lesson));
            if (editing) {
                Ui.button(card, "编辑安排", true, () -> editLesson(lesson));
                Ui.button(card, "查看选课名单", true, () -> roster(lesson));
            }
        }
    }

    private void editLesson(Lesson lesson) {
        List<Course> courses = helper.getAllCourses();
        if (courses.isEmpty()) { toast("请先创建课程"); return; }
        LinearLayout form = Ui.dialogBody(this);
        List<String> courseNames = new ArrayList<>();
        for (Course course : courses) courseNames.add(course.getCourseName());
        Spinner course = spinner(form, "课程", courseNames);
        if (lesson != null) for (int index = 0; index < courses.size(); index++) if (courses.get(index).getCourseId() == lesson.courseId) course.setSelection(index);
        Spinner day = spinner(form, "每周上课日", java.util.Arrays.asList(DAYS));
        Calendar now = Calendar.getInstance();
        day.setSelection(lesson == null ? (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 : lesson.weekday - 1);
        int currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int[] times = {lesson == null ? currentMinute : lesson.start, lesson == null ? Math.min(1439, currentMinute + 60) : lesson.end};
        MaterialButton start = Ui.button(form, "开始时间  " + clock(times[0]), true, () -> {});
        MaterialButton end = Ui.button(form, "结束时间  " + clock(times[1]), true, () -> {});
        start.setOnClickListener(view -> new TimePickerDialog(this, (picker, hour, minute) -> { times[0] = hour * 60 + minute; start.setText("开始时间  " + clock(times[0])); }, times[0] / 60, times[0] % 60, true).show());
        end.setOnClickListener(view -> new TimePickerDialog(this, (picker, hour, minute) -> { times[1] = hour * 60 + minute; end.setText("结束时间  " + clock(times[1])); }, times[1] / 60, times[1] % 60, true).show());
        EditText room = Ui.field(form, "教室 / 上课地点", lesson == null ? "" : lesson.room, InputType.TYPE_CLASS_TEXT);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(lesson == null ? "添加课表" : "编辑课表").setView(Ui.scroll(form)).setPositiveButton("保存", null).setNegativeButton("取消", null).setNeutralButton(lesson == null ? null : "删除安排", null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> perform(dialog, () -> repository.saveLesson(lesson == null ? 0 : lesson.id, courses.get(course.getSelectedItemPosition()).getCourseId(), day.getSelectedItemPosition() + 1, times[0], times[1], value(room))));
            if (lesson != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> new AlertDialog.Builder(this).setTitle("删除课表安排？").setMessage("已保存的历史考勤会继续保留。").setPositiveButton("删除", (confirmation, which) -> perform(dialog, () -> repository.deleteLesson(lesson.id))).setNegativeButton("取消", null).show());
        });
        dialog.show();
    }

    private void publishDialog(Lesson lesson) {
        String[] durations = {"5 分钟", "10 分钟", "15 分钟", "30 分钟"};
        int[] minutes = {5, 10, 15, 30};
        new AlertDialog.Builder(this).setTitle("发起签到 · " + lesson.courseName).setItems(durations, (dialog, which) -> perform(null, () -> repository.publish(lesson.id, minutes[which]))).setNegativeButton("取消", null).show();
    }

    private void roster(Lesson lesson) {
        List<Student> students = helper.getCourseStudents(lesson.courseId);
        StringBuilder message = new StringBuilder("共 " + students.size() + " 人\n\n");
        for (Student student : students) message.append(student.getStudentName()).append(" · ").append(student.getStudentNumber()).append("\n").append(student.getClassName()).append("\n\n");
        new AlertDialog.Builder(this).setTitle(lesson.courseName + " · 选课名单").setMessage(message.toString()).setPositiveButton("知道了", null).show();
    }

    private void enrollmentDialog() {
        List<String[]> options = repository.enrollmentOptions();
        if (options.isEmpty()) { toast("暂无可选课程，请教师先创建课程"); return; }
        List<Integer> selected = repository.enrolledIds();
        boolean[] checked = new boolean[options.size()];
        String[] labels = new String[options.size()];
        for (int index = 0; index < options.size(); index++) {
            String[] option = options.get(index);
            labels[index] = option[1] + " · " + option[2];
            checked[index] = selected.contains(Integer.parseInt(option[0]));
        }
        new AlertDialog.Builder(this).setTitle("选择我的课程").setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked).setPositiveButton("保存选课", (dialog, which) -> {
            List<Integer> courseIds = new ArrayList<>();
            for (int index = 0; index < options.size(); index++) if (checked[index]) courseIds.add(Integer.parseInt(options.get(index)[0]));
            perform(null, () -> repository.saveEnrollment(courseIds));
        }).setNegativeButton("取消", null).show();
    }

    private void attendance() {
        Ui.heading(body, "课堂考勤", account.isTeacher() ? "" : "");
        Ui.button(body, "刷新考勤", true, this::render);
        List<Task> tasks = repository.tasks();
        Ui.section(body, "签到任务");
        if (tasks.isEmpty()) Ui.card(body, "还没有签到任务", account.isTeacher() ? "请在上课时间前往「课表」发起签到。" : "教师发布签到后，可在这里进入摇一摇页面。");
        for (Task task : tasks) taskCard(body, task);
        if (!account.isTeacher()) {
            Ui.section(body, "我的考勤记录");
            List<Record> records = repository.records(0);
            if (records.isEmpty()) Ui.card(body, "暂无考勤记录", "参加课堂签到后，记录会自动保存在这里。");
            for (Record record : records) recordCard(body, record, null);
        }
        Ui.section(body, account.isTeacher() ? "考勤修改申请" : "我的申请");
        List<Request> requests = repository.requests();
        if (requests.isEmpty()) Ui.card(body, "暂无申请", account.isTeacher() ? "学生提交申请后，可在这里审批。" : "");
        for (Request request : requests) requestCard(request);
    }

    private void taskCard(LinearLayout parent, Task task) {
        String state = task.active() ? "进行中" : "已结束";
        String detail = date(task.start, "MM月dd日 HH:mm") + " — " + date(task.end, "HH:mm") + "  ·  " + task.room + "\n" + state + (account.isTeacher() ? "  ·  已到 " + task.present + " / 应到 " + task.total : "  ·  " + task.ownStatus);
        LinearLayout card = Ui.card(parent, task.courseName, detail);
        if (account.isTeacher()) {
            Ui.button(card, "查看与修改考勤", true, () -> taskRecords(task));
            if (task.active()) Ui.button(card, "结束本次签到", () -> new AlertDialog.Builder(this).setTitle("结束签到？").setMessage("未签到学生将记为缺课，之后仍可由教师修改。").setPositiveButton("结束签到", (dialog, which) -> perform(null, () -> repository.closeTask(task.id))).setNegativeButton("取消", null).show());
        } else if (task.active() && AttendanceRepository.WAITING.equals(task.ownStatus)) Ui.button(card, "进入摇一摇签到", () -> startActivity(new Intent(this, ShakeActivity.class).putExtra("task_id", task.id)));
    }

    private void taskRecords(Task task) {
        LinearLayout content = Ui.dialogBody(this);
        List<Record> records = repository.records(task.id);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(task.courseName + " · 考勤名单").setView(Ui.scroll(content)).setPositiveButton("关闭", null).create();
        for (Record record : records) recordCard(content, record, dialog);
        dialog.show();
    }

    private void recordCard(LinearLayout parent, Record record, AlertDialog parentDialog) {
        LinearLayout card = Ui.card(parent, account.isTeacher() ? record.name + " · " + record.number : record.courseName, date(record.lessonTime, "MM月dd日 HH:mm") + "  ·  " + record.status + (record.time > 0 ? "\n签到时间 " + date(record.time, "HH:mm:ss") : "") + (record.source.isEmpty() ? "" : "\n" + record.source));
        if (account.isTeacher()) Ui.button(card, "修改考勤状态", true, () -> changeDialog(record, parentDialog));
        else if (record.closed) Ui.button(card, "申请修改", true, () -> changeDialog(record, null));
        Ui.button(card, "修改记录", true, () -> {
            List<String> changes = repository.changes(record.id);
            new AlertDialog.Builder(this).setTitle("考勤修改记录").setMessage(changes.isEmpty() ? "暂无人工修改记录" : android.text.TextUtils.join("\n\n", changes)).setPositiveButton("关闭", null).show();
        });
    }

    private void changeDialog(Record record, AlertDialog parentDialog) {
        LinearLayout form = Ui.dialogBody(this);
        String[] statuses = {AttendanceRepository.PRESENT, AttendanceRepository.ABSENT, AttendanceRepository.LEAVE};
        Spinner status = spinner(form, "调整为", java.util.Arrays.asList(statuses));
        for (int index = 0; index < statuses.length; index++) if (statuses[index].equals(record.status)) status.setSelection(index);
        EditText reason = Ui.field(form, account.isTeacher() ? "修改原因" : "申请原因", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(account.isTeacher() ? "修改考勤" : "申请修改考勤").setView(Ui.scroll(form)).setPositiveButton(account.isTeacher() ? "保存修改" : "提交申请", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            boolean success = perform(dialog, () -> {
                if (account.isTeacher()) repository.correctRecord(record.id, statuses[status.getSelectedItemPosition()], value(reason));
                else repository.requestChange(record.id, statuses[status.getSelectedItemPosition()], value(reason));
            });
            if (success && parentDialog != null) parentDialog.dismiss();
        }));
        dialog.show();
    }

    private void requestCard(Request request) {
        LinearLayout card = Ui.card(body, request.courseName + " · " + request.state, (account.isTeacher() ? request.name + "\n" : "") + date(request.lessonTime, "MM月dd日 HH:mm") + "  ·  申请改为「" + request.desired + "」\n申请原因：" + request.reason + (request.reply.isEmpty() ? "" : "\n处理意见：" + request.reply));
        if (account.isTeacher() && "待审批".equals(request.state)) Ui.button(card, "处理申请", () -> {
            LinearLayout form = Ui.dialogBody(this);
            EditText reply = Ui.field(form, "处理意见", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("处理 " + request.name + " 的申请").setView(Ui.scroll(form)).setPositiveButton("通过", null).setNeutralButton("驳回", null).setNegativeButton("取消", null).create();
            dialog.setOnShowListener(ignored -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> perform(dialog, () -> repository.review(request.id, true, value(reply))));
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> perform(dialog, () -> repository.review(request.id, false, value(reply))));
            });
            dialog.show();
        });
    }

    private void statistics() {
        Ui.heading(body, "考勤统计", "");
        List<Statistic> statistics = repository.statistics(false);
        int total = 0;
        int absent = 0;
        for (Statistic statistic : statistics) { total += statistic.total; absent += statistic.absent; }
        Ui.hero(body, total == 0 ? "暂无已结束考勤" : String.format(Locale.CHINA, "%.1f%% 缺课率", absent * 100.0 / total), "累计应到 " + total + (account.isTeacher() ? " 人次" : " 次") + "  ·  缺课 " + absent + (account.isTeacher() ? " 人次" : " 次"));
        Ui.section(body, "各课程统计");
        if (statistics.isEmpty()) Ui.card(body, "等待第一份考勤", "签到结束后，将显示课程缺课次数与缺课率。");
        for (Statistic statistic : statistics) statisticCard(statistic);
        if (account.isTeacher()) {
            Ui.section(body, "每个学生的缺课次数");
            for (Statistic statistic : repository.statistics(true)) statisticCard(statistic);
            Ui.section(body, "每堂课的缺课率");
            for (Task task : repository.tasks()) {
                if (!task.closed) continue;
                int missing = 0;
                for (Record record : repository.records(task.id)) if (AttendanceRepository.ABSENT.equals(record.status)) missing++;
                Ui.card(body, task.courseName + " · " + date(task.start, "MM-dd HH:mm"), "应到 " + task.total + " 人 · 缺课 " + missing + " 人 · 缺课率 " + percentage(missing, task.total));
            }
            body.addView(Ui.text(this, "课程缺课率 = 累计缺课人次 ÷ 累计应到人次。", 12, Ui.MUTED));
        }
    }

    private void statisticCard(Statistic statistic) {
        LinearLayout card = Ui.card(body, statistic.label, "已到 " + statistic.present + "  ·  缺课 " + statistic.absent + "  ·  请假 " + statistic.leave + "\n缺课率 " + percentage(statistic.absent, statistic.total));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, statistic.total));
        progress.setProgress(statistic.absent);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.BLUE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.dp(this, 8));
        params.topMargin = Ui.dp(this, 12);
        card.addView(progress, params);
    }

    private void profile() {
        Ui.heading(body, "我的", "管理个人资料与账号");
        Ui.hero(body, account.name, (account.isTeacher() ? "教师" : "学生 · " + account.className) + "\n账号 " + account.username + (account.isTeacher() ? "" : "  ·  学号 " + account.number));
        Ui.section(body, "个人资料");
        LinearLayout profile = Ui.card(body, "保持信息准确", "手机号：" + (account.phone.isEmpty() ? "未填写" : account.phone));
        Ui.button(profile, "编辑个人资料", this::profileDialog);
        Ui.button(profile, "修改密码", true, this::passwordDialog);
        if (account.isTeacher()) {
            Ui.section(body, "教学工具");
            LinearLayout tools = Ui.card(body, "更多工具", "");
            Ui.button(tools, "学生与选课名单", true, () -> launch(StudentActivity.class));
            Ui.button(tools, "平时成绩管理", true, () -> launch(ScoreActivity.class));
            Ui.button(tools, "成绩数据导出", true, () -> launch(BackupActivity.class));
        }
        Ui.section(body, "账号");
        Ui.button(body, "退出登录 / 切换账号", true, () -> {
            new SessionManager(this).signOut();
            openLogin();
        });
    }

    private void profileDialog() {
        LinearLayout form = Ui.dialogBody(this);
        EditText name = Ui.field(form, "姓名", account.name, InputType.TYPE_CLASS_TEXT);
        EditText phone = Ui.field(form, "手机号（选填）", account.phone, InputType.TYPE_CLASS_PHONE);
        EditText number = account.isTeacher() ? null : Ui.field(form, "学号", account.number, InputType.TYPE_CLASS_TEXT);
        EditText className = account.isTeacher() ? null : Ui.field(form, "班级", account.className, InputType.TYPE_CLASS_TEXT);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("编辑个人资料").setView(Ui.scroll(form)).setPositiveButton("保存", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> perform(dialog, () -> repository.updateProfile(value(name), value(phone), number == null ? "" : value(number), className == null ? "" : value(className)))));
        dialog.show();
    }

    private void passwordDialog() {
        LinearLayout form = Ui.dialogBody(this);
        EditText previous = Ui.field(form, "原密码", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText password = Ui.field(form, "新密码（至少 6 位）", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirm = Ui.field(form, "再次输入新密码", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("修改密码").setView(Ui.scroll(form)).setPositiveButton("保存", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!password.getText().toString().equals(confirm.getText().toString())) { toast("两次新密码输入不一致"); return; }
            perform(dialog, () -> repository.changePassword(previous.getText().toString(), password.getText().toString()));
        }));
        dialog.show();
    }

    private Spinner spinner(LinearLayout parent, String label, List<String> options) {
        parent.addView(Ui.text(this, label, 13, Ui.MUTED));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        parent.addView(spinner, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        return spinner;
    }

    private boolean perform(AlertDialog dialog, Runnable operation) {
        try {
            operation.run();
            if (dialog != null) dialog.dismiss();
            toast("操作成功");
            render();
            return true;
        } catch (IllegalArgumentException exception) {
            toast(exception.getMessage());
        } catch (android.database.SQLException exception) {
            toast("保存失败，请检查信息后重试");
        }
        return false;
    }

    private String value(EditText input) { return input.getText().toString().trim(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void launch(Class<?> activity) { startActivity(new Intent(this, activity)); }
    private static String clock(int minute) { return String.format(Locale.CHINA, "%02d:%02d", minute / 60, minute % 60); }
    private static String date(long time, String pattern) { return new SimpleDateFormat(pattern, Locale.CHINA).format(new Date(time)); }
    private static String percentage(int count, int total) { return total == 0 ? "0.0%" : String.format(Locale.CHINA, "%.1f%%", count * 100.0 / total); }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (repository != null && body != null) render();
        handler.postDelayed(refresh, 15000);
    }

    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("tab", selectedTab);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (repository != null) repository.close();
        if (helper != null) helper.close();
        super.onDestroy();
    }
}
