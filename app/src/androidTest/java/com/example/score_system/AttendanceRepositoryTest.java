package com.example.score_system;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.score_system.data.AttendanceRepository;
import com.example.score_system.data.AttendanceRepository.Lesson;
import com.example.score_system.data.AttendanceRepository.Record;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;
import com.example.score_system.model.Score;
import com.example.score_system.model.Student;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AttendanceRepositoryTest {
    private Context context;
    private AttendanceRepository repository;
    private DatabaseHelper helper;

    @Before public void prepare() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("repository_test_" + name, mode);
            }
            @Override public File getDatabasePath(String name) { return new File(getCacheDir(), "repository_test_" + name); }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(), factory, errorHandler);
            }
        };
        SQLiteDatabase.deleteDatabase(context.getDatabasePath("score_system.db"));
        context.getSharedPreferences("attendance_session", Context.MODE_PRIVATE).edit().clear().commit();
        helper = new DatabaseHelper(context);
        repository = new AttendanceRepository(context);
        repository.login("admin", "234512", true);
    }

    @After public void cleanup() {
        repository.close();
        helper.close();
        SQLiteDatabase.deleteDatabase(context.getDatabasePath("score_system.db"));
        context.getSharedPreferences("attendance_session", Context.MODE_PRIVATE).edit().clear().commit();
    }

    private long createTask() {
        int courseId = (int) helper.insertCourse(new Course("CS101", "移动应用开发"));
        int studentId = (int) helper.insertStudent(new Student("20260001", "小林", "软件一班"));
        helper.updateStudentEnrollments(studentId, Collections.singletonList(courseId));
        repository.register("student", "123456", "小林", "", false, "20260001", "软件一班");
        int day = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
        repository.saveLesson(0, courseId, day, 0, 1440, "教学楼 301");
        Lesson lesson = repository.lessons().get(0);
        repository.publish(lesson.id, 10);
        return repository.tasks().get(0).id;
    }

    private void rejected(Runnable action) {
        try { action.run(); fail("操作应被拒绝"); }
        catch (IllegalArgumentException expected) { assertFalse(expected.getMessage().isEmpty()); }
    }

    @Test public void shakeSignInPersistsAndRejectsDuplicates() {
        long taskId = createTask();
        repository.login("student", "123456", false);
        repository.signByShake(taskId);
        rejected(() -> repository.signByShake(taskId));
        Record record = repository.records(taskId).get(0);
        assertEquals(AttendanceRepository.PRESENT, record.status);
        assertEquals("摇一摇", record.source);
        assertTrue(record.time > 0);
        repository.close();
        repository = new AttendanceRepository(context);
        assertEquals(AttendanceRepository.PRESENT, repository.records(taskId).get(0).status);
    }

    @Test public void expiredTaskBecomesAbsentAndCannotSignIn() {
        long taskId = createTask();
        helper.getWritableDatabase().execSQL("UPDATE attendance_task SET end_at=? WHERE task_id=?", new Object[]{System.currentTimeMillis() - 1000, taskId});
        repository.login("student", "123456", false);
        rejected(() -> repository.signByShake(taskId));
        assertTrue(repository.tasks().get(0).closed);
        assertEquals(AttendanceRepository.ABSENT, repository.records(taskId).get(0).status);
        assertEquals(1, repository.statistics(false).get(0).absent);
    }

    @Test public void approvedRequestUpdatesRecordAndStatistics() {
        long taskId = createTask();
        repository.closeTask(taskId);
        repository.login("student", "123456", false);
        long recordId = repository.records(taskId).get(0).id;
        assertEquals(1, repository.statistics(false).get(0).absent);
        repository.requestChange(recordId, AttendanceRepository.PRESENT, "课堂签到时手机故障");
        rejected(() -> repository.requestChange(recordId, AttendanceRepository.PRESENT, "重复申请"));
        long requestId = repository.requests().get(0).id;
        rejected(() -> repository.review(requestId, true, "越权审批"));
        repository.login("admin", "234512", true);
        repository.review(requestId, true, "已核实到课");
        assertEquals(0, repository.statistics(false).get(0).absent);
        assertEquals(1, repository.statistics(true).get(0).present);
        assertEquals(1, repository.changes(recordId).size());
        rejected(() -> repository.review(requestId, true, "重复处理"));
        repository.login("student", "123456", false);
        assertEquals("已通过", repository.requests().get(0).state);
        assertEquals(AttendanceRepository.PRESENT, repository.records(taskId).get(0).status);
    }

    @Test public void accountsAreIsolatedAndOtherTeachersCannotModifyAttendance() {
        long taskId = createTask();
        long recordId = repository.records(taskId).get(0).id;
        repository.register("teacher2", "123456", "李老师", "", true, "", "");
        repository.login("teacher2", "123456", true);
        assertTrue(repository.tasks().isEmpty());
        assertTrue(helper.getAllCourses().isEmpty());
        rejected(() -> repository.correctRecord(recordId, AttendanceRepository.PRESENT, "不能修改他人的课程"));
        repository.register("student2", "123456", "小陈", "", false, "20260002", "软件一班");
        repository.login("student2", "123456", false);
        assertTrue(repository.records(0).isEmpty());
        rejected(() -> repository.signByShake(taskId));
        rejected(() -> repository.correctRecord(recordId, AttendanceRepository.PRESENT, "学生不能直接改考勤"));
    }

    @Test public void publishRequiresClassTimeAndSnapshotsRoster() {
        long taskId = createTask();
        Lesson lesson = repository.lessons().get(0);
        rejected(() -> repository.publish(lesson.id, 10));
        int lateStudentId = (int) helper.insertStudent(new Student("20260003", "小周", "软件一班"));
        helper.updateStudentEnrollments(lateStudentId, Collections.singletonList(lesson.courseId));
        assertEquals(1, repository.tasks().get(0).total);
        repository.closeTask(taskId);
        int tomorrow = lesson.weekday % 7 + 1;
        repository.saveLesson(lesson.id, lesson.courseId, tomorrow, 0, 1440, "教学楼 301");
        rejected(() -> repository.publish(lesson.id, 10));
    }

    @Test public void profileAndPasswordChangesSurviveRelogin() {
        createTask();
        repository.login("student", "123456", false);
        repository.updateProfile("小林", "13800000000", "20269999", "软件二班");
        repository.changePassword("123456", "new-password");
        rejected(() -> repository.login("student", "123456", false));
        repository.login("student", "new-password", false);
        assertEquals("20269999", repository.currentAccount().number);
        assertEquals("软件二班", repository.currentAccount().className);
        assertEquals("13800000000", repository.currentAccount().phone);
        assertEquals(1, repository.records(0).size());
    }

    @Test public void upgradeFromVersionOneRetainsStudentsCoursesAndScores() {
        int courseId = (int) helper.insertCourse(new Course("OLD101", "历史课程"));
        int studentId = (int) helper.insertStudent(new Student("OLD001", "历史学生", "历史班级"));
        helper.insertScore(new Score(courseId, studentId, Score.TYPE_HOMEWORK, 1, 8));
        repository.close();
        helper.close();
        try (SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("score_system.db"), null)) {
            String[] tables = {"attendance_change", "attendance_request", "attendance_record", "attendance_task", "schedule", "course_owner", "account", "student_course"};
            for (String table : tables) database.execSQL("DROP TABLE " + table);
            database.execSQL("UPDATE teacher SET password='old-password'");
            database.setVersion(1);
        }
        helper = new DatabaseHelper(context);
        repository = new AttendanceRepository(context);
        repository.login("admin", "234512", true);
        assertEquals("历史课程", helper.getAllCourses().get(0).getCourseName());
        assertEquals("历史学生", helper.getStudentById(studentId).getStudentName());
        assertEquals(8, helper.getScoresByCourseAndStudent(courseId, studentId).get(0).getScoreValue(), 0.001);
        assertEquals(1, helper.getCourseEnrollmentCount(courseId));
        assertEquals(4, helper.getReadableDatabase().getVersion());
    }
}
