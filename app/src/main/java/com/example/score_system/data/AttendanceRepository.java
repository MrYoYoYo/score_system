package com.example.score_system.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class AttendanceRepository implements AutoCloseable {
    public static final String PRESENT = "已到";
    public static final String ABSENT = "缺课";
    public static final String LEAVE = "请假";
    public static final String WAITING = "未签到";
    private final DatabaseHelper helper;
    private final SessionManager session;

    public static class Account {
        public long id;
        public String username;
        public String name;
        public String role;
        public String phone;
        public int studentId;
        public String number = "";
        public String className = "";
        public boolean isTeacher() { return "teacher".equals(role); }
    }

    public static class Lesson {
        public long id;
        public int courseId;
        public String courseName;
        public String teacher;
        public int weekday;
        public int start;
        public int end;
        public String room;
        public boolean isNow() {
            Calendar calendar = Calendar.getInstance();
            int day = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
            int minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            return day == weekday && minute >= start && minute < end;
        }
    }

    public static class Task {
        public long id;
        public String courseName;
        public String room;
        public long start;
        public long end;
        public boolean closed;
        public int total;
        public int present;
        public String ownStatus;
        public boolean active() { return !closed && System.currentTimeMillis() >= start && System.currentTimeMillis() < end; }
    }

    public static class Record {
        public long id;
        public long taskId;
        public String courseName;
        public String name;
        public String number;
        public String status;
        public String source;
        public long time;
        public long lessonTime;
        public boolean closed;
    }

    public static class Request {
        public long id;
        public long recordId;
        public String name;
        public String courseName;
        public String desired;
        public String reason;
        public String state;
        public String reply;
        public long lessonTime;
    }

    public static class Statistic {
        public String label;
        public int total;
        public int absent;
        public int present;
        public int leave;
    }

    public AttendanceRepository(Context context) {
        helper = new DatabaseHelper(context);
        session = new SessionManager(context);
    }

    public static void install(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE account (account_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('teacher','student')), name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', student_id INTEGER UNIQUE)");
        database.execSQL("CREATE TABLE course_owner (course_id INTEGER PRIMARY KEY, account_id INTEGER NOT NULL)");
        database.execSQL("CREATE TABLE schedule (schedule_id INTEGER PRIMARY KEY AUTOINCREMENT, course_id INTEGER NOT NULL, teacher_id INTEGER NOT NULL, weekday INTEGER NOT NULL CHECK(weekday BETWEEN 1 AND 7), start_minute INTEGER NOT NULL, end_minute INTEGER NOT NULL, room TEXT NOT NULL)");
        database.execSQL("CREATE TABLE attendance_task (task_id INTEGER PRIMARY KEY AUTOINCREMENT, course_id INTEGER NOT NULL, teacher_id INTEGER NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER NOT NULL, room TEXT NOT NULL, closed INTEGER NOT NULL DEFAULT 0)");
        database.execSQL("CREATE TABLE attendance_record (record_id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER NOT NULL, student_id INTEGER NOT NULL, status TEXT NOT NULL DEFAULT '未签到', signed_at INTEGER NOT NULL DEFAULT 0, source TEXT NOT NULL DEFAULT '', UNIQUE(task_id, student_id))");
        database.execSQL("CREATE TABLE attendance_request (request_id INTEGER PRIMARY KEY AUTOINCREMENT, record_id INTEGER NOT NULL, account_id INTEGER NOT NULL, desired TEXT NOT NULL, reason TEXT NOT NULL, state TEXT NOT NULL DEFAULT '待审批', reply TEXT NOT NULL DEFAULT '')");
        database.execSQL("CREATE TABLE attendance_change (change_id INTEGER PRIMARY KEY AUTOINCREMENT, record_id INTEGER NOT NULL, account_id INTEGER NOT NULL, old_status TEXT NOT NULL, new_status TEXT NOT NULL, reason TEXT NOT NULL, changed_at INTEGER NOT NULL)");
        database.execSQL("CREATE INDEX attendance_task_teacher ON attendance_task(teacher_id, end_at)");
        database.execSQL("CREATE INDEX attendance_record_student ON attendance_record(student_id, task_id)");
        database.execSQL("CREATE UNIQUE INDEX attendance_request_pending ON attendance_request(record_id) WHERE state='待审批'");
        try (Cursor teachers = database.rawQuery("SELECT username, password FROM teacher", null)) {
            while (teachers.moveToNext()) {
                ContentValues values = new ContentValues();
                values.put("username", teachers.getString(0));
                values.put("password_hash", PasswordHasher.hash(teachers.getString(1)));
                values.put("role", "teacher");
                values.put("name", "admin".equals(teachers.getString(0)) ? "管理员老师" : teachers.getString(0));
                database.insertOrThrow("account", null, values);
            }
        }
        database.execSQL("INSERT INTO course_owner(course_id, account_id) SELECT course_id, (SELECT MIN(account_id) FROM account WHERE role='teacher') FROM course");
        database.execSQL("UPDATE teacher SET password='' ");
    }

    public Account currentAccount() {
        return account(session.accountId());
    }

    private Account account(long accountId) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT a.*, s.student_number, s.class_name, s.student_name FROM account a LEFT JOIN student s ON s.student_id=a.student_id WHERE a.account_id=?", args(accountId))) {
            if (!cursor.moveToFirst()) return null;
            Account account = new Account();
            account.id = number(cursor, "account_id");
            account.username = string(cursor, "username");
            account.role = string(cursor, "role");
            account.name = string(cursor, "name");
            account.phone = string(cursor, "phone");
            account.studentId = (int) number(cursor, "student_id");
            if (!account.isTeacher()) {
                account.number = string(cursor, "student_number");
                account.className = string(cursor, "class_name");
                account.name = string(cursor, "student_name");
            }
            return account;
        }
    }

    public Account login(String username, String password, boolean teacher) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT account_id, password_hash FROM account WHERE username=? AND role=?", new String[]{username, teacher ? "teacher" : "student"})) {
            if (!cursor.moveToFirst() || !PasswordHasher.verify(password, cursor.getString(1))) throw new IllegalArgumentException("账号、密码或身份不正确");
            Account account = account(cursor.getLong(0));
            session.signIn(account.id);
            return account;
        }
    }

    public void register(String username, String password, String name, String phone, boolean teacher, String studentNumber, String className) {
        require(!username.isEmpty() && !name.isEmpty(), "请填写账号和姓名");
        require(password.length() >= 6, "密码至少需要 6 位");
        validatePhone(phone);
        require(teacher || (!studentNumber.isEmpty() && !className.isEmpty()), "请填写学号和班级");
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            require(!exists(database, "SELECT 1 FROM account WHERE username=?", new String[]{username}), "该账号已注册");
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("password_hash", PasswordHasher.hash(password));
            values.put("name", name);
            values.put("phone", phone);
            values.put("role", teacher ? "teacher" : "student");
            if (!teacher) {
                long studentId;
                try (Cursor cursor = database.rawQuery("SELECT student_id, student_name, class_name FROM student WHERE student_number=?", new String[]{studentNumber})) {
                    if (cursor.moveToFirst()) {
                        studentId = cursor.getLong(0);
                        require(name.equals(cursor.getString(1)) && className.equals(cursor.getString(2)), "该学号已有档案，请填写与名单一致的姓名和班级");
                    } else {
                        ContentValues student = new ContentValues();
                        student.put("student_number", studentNumber);
                        student.put("student_name", name);
                        student.put("class_name", className);
                        studentId = database.insertOrThrow("student", null, student);
                    }
                }
                require(!exists(database, "SELECT 1 FROM account WHERE student_id=?", args(studentId)), "该学号已绑定账号");
                values.put("student_id", studentId);
            }
            database.insertOrThrow("account", null, values);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void updateProfile(String name, String phone, String studentNumber, String className) {
        Account current = requireAccount();
        require(!name.isEmpty(), "姓名不能为空");
        validatePhone(phone);
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (!current.isTeacher()) {
                require(!studentNumber.isEmpty() && !className.isEmpty(), "请填写学号和班级");
                require(!exists(database, "SELECT 1 FROM student WHERE student_number=? AND student_id<>?", new String[]{studentNumber, String.valueOf(current.studentId)}), "该学号已被使用");
                ContentValues student = new ContentValues();
                student.put("student_number", studentNumber);
                student.put("student_name", name);
                student.put("class_name", className);
                database.update("student", student, "student_id=?", args(current.studentId));
            }
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("phone", phone);
            database.update("account", values, "account_id=?", args(current.id));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void changePassword(String previous, String password) {
        Account current = requireAccount();
        require(password.length() >= 6, "新密码至少需要 6 位");
        SQLiteDatabase database = helper.getWritableDatabase();
        try (Cursor cursor = database.rawQuery("SELECT password_hash FROM account WHERE account_id=?", args(current.id))) {
            require(cursor.moveToFirst() && PasswordHasher.verify(previous, cursor.getString(0)), "原密码不正确");
        }
        ContentValues values = new ContentValues();
        values.put("password_hash", PasswordHasher.hash(password));
        database.update("account", values, "account_id=?", args(current.id));
    }

    public List<Lesson> lessons() {
        Account current = requireAccount();
        String filter = current.isTeacher() ? "o.account_id=?" : "c.course_id IN (SELECT course_id FROM student_course WHERE student_id=?)";
        List<Lesson> lessons = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT l.*, c.course_name, a.name AS teacher_name FROM schedule l JOIN course c ON c.course_id=l.course_id JOIN course_owner o ON o.course_id=c.course_id JOIN account a ON a.account_id=o.account_id WHERE " + filter + " ORDER BY weekday, start_minute", args(current.isTeacher() ? current.id : current.studentId))) {
            while (cursor.moveToNext()) {
                Lesson lesson = new Lesson();
                lesson.id = number(cursor, "schedule_id");
                lesson.courseId = (int) number(cursor, "course_id");
                lesson.courseName = string(cursor, "course_name");
                lesson.teacher = string(cursor, "teacher_name");
                lesson.weekday = (int) number(cursor, "weekday");
                lesson.start = (int) number(cursor, "start_minute");
                lesson.end = (int) number(cursor, "end_minute");
                lesson.room = string(cursor, "room");
                lessons.add(lesson);
            }
        }
        return lessons;
    }

    public void saveLesson(long lessonId, int courseId, int weekday, int start, int end, String room) {
        Account current = requireTeacher();
        requireOwnCourse(courseId, current.id);
        require(weekday >= 1 && weekday <= 7 && start >= 0 && end <= 1440 && start < end, "结束时间必须晚于开始时间");
        require(!room.isEmpty(), "请填写上课地点");
        require(!exists(helper.getReadableDatabase(), "SELECT 1 FROM schedule WHERE teacher_id=? AND weekday=? AND start_minute<? AND end_minute>? AND schedule_id<>?", new String[]{String.valueOf(current.id), String.valueOf(weekday), String.valueOf(end), String.valueOf(start), String.valueOf(lessonId)}), "该时段已有课程安排");
        ContentValues values = new ContentValues();
        values.put("course_id", courseId);
        values.put("teacher_id", current.id);
        values.put("weekday", weekday);
        values.put("start_minute", start);
        values.put("end_minute", end);
        values.put("room", room);
        if (lessonId == 0) helper.getWritableDatabase().insertOrThrow("schedule", null, values);
        else require(helper.getWritableDatabase().update("schedule", values, "schedule_id=? AND teacher_id=?", args(lessonId, current.id)) > 0, "课表不存在");
    }

    public void deleteLesson(long lessonId) {
        Account current = requireTeacher();
        helper.getWritableDatabase().delete("schedule", "schedule_id=? AND teacher_id=?", args(lessonId, current.id));
    }

    public List<String[]> enrollmentOptions() {
        List<String[]> options = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT c.course_id, c.course_name, a.name FROM course c JOIN course_owner o ON o.course_id=c.course_id JOIN account a ON a.account_id=o.account_id ORDER BY c.course_name", null)) {
            while (cursor.moveToNext()) options.add(new String[]{cursor.getString(0), cursor.getString(1), cursor.getString(2)});
        }
        return options;
    }

    public List<Integer> enrolledIds() {
        return helper.getStudentCourseIds(requireAccount().studentId);
    }

    public void saveEnrollment(List<Integer> courseIds) {
        Account current = requireAccount();
        require(!current.isTeacher(), "请使用学生账号选课");
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            for (int courseId : courseIds) require(exists(database, "SELECT 1 FROM course WHERE course_id=?", args(courseId)), "课程已被删除");
            helper.updateStudentEnrollments(current.studentId, courseIds);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void publish(long lessonId, int durationMinutes) {
        Account current = requireTeacher();
        Lesson selected = null;
        for (Lesson lesson : lessons()) if (lesson.id == lessonId) selected = lesson;
        require(selected != null && selected.isNow(), "请在课表规定的上课时间内发布签到");
        require(durationMinutes >= 1 && durationMinutes <= 30, "签到时长应为 1—30 分钟");
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            finalizeExpired(database);
            require(!exists(database, "SELECT 1 FROM attendance_task WHERE course_id=? AND closed=0", args(selected.courseId)), "该课程已有进行中的签到");
            List<com.example.score_system.model.Student> students = helper.getCourseStudents(selected.courseId);
            require(!students.isEmpty(), "请先为课程添加选课学生");
            long now = System.currentTimeMillis();
            Calendar classEnd = Calendar.getInstance();
            classEnd.set(Calendar.HOUR_OF_DAY, selected.end / 60);
            classEnd.set(Calendar.MINUTE, selected.end % 60);
            classEnd.set(Calendar.SECOND, 0);
            classEnd.set(Calendar.MILLISECOND, 0);
            ContentValues task = new ContentValues();
            task.put("course_id", selected.courseId);
            task.put("teacher_id", current.id);
            task.put("start_at", now);
            task.put("end_at", Math.min(now + durationMinutes * 60000L, classEnd.getTimeInMillis()));
            task.put("room", selected.room);
            long taskId = database.insertOrThrow("attendance_task", null, task);
            for (com.example.score_system.model.Student student : students) {
                ContentValues record = new ContentValues();
                record.put("task_id", taskId);
                record.put("student_id", student.getStudentId());
                database.insertOrThrow("attendance_record", null, record);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void finalizeExpired(SQLiteDatabase database) {
        database.execSQL("UPDATE attendance_task SET closed=1 WHERE closed=0 AND end_at<=?", new Object[]{System.currentTimeMillis()});
        database.execSQL("UPDATE attendance_record SET status='缺课', source='签到结束' WHERE status='未签到' AND task_id IN (SELECT task_id FROM attendance_task WHERE closed=1)");
    }

    public List<Task> tasks() {
        Account current = requireAccount();
        SQLiteDatabase database = helper.getWritableDatabase();
        finalizeExpired(database);
        List<Task> tasks = new ArrayList<>();
        String filter = current.isTeacher() ? "t.teacher_id=?" : "t.task_id IN (SELECT task_id FROM attendance_record WHERE student_id=?)";
        try (Cursor cursor = database.rawQuery("SELECT t.*, c.course_name, (SELECT COUNT(*) FROM attendance_record r WHERE r.task_id=t.task_id) AS total, (SELECT COUNT(*) FROM attendance_record r WHERE r.task_id=t.task_id AND r.status='已到') AS present, (SELECT status FROM attendance_record r WHERE r.task_id=t.task_id AND r.student_id=" + current.studentId + ") AS own_status FROM attendance_task t JOIN course c ON c.course_id=t.course_id WHERE " + filter + " ORDER BY t.start_at DESC, t.task_id DESC", args(current.isTeacher() ? current.id : current.studentId))) {
            while (cursor.moveToNext()) {
                Task task = new Task();
                task.id = number(cursor, "task_id");
                task.courseName = string(cursor, "course_name");
                task.room = string(cursor, "room");
                task.start = number(cursor, "start_at");
                task.end = number(cursor, "end_at");
                task.closed = number(cursor, "closed") == 1;
                task.total = (int) number(cursor, "total");
                task.present = (int) number(cursor, "present");
                task.ownStatus = string(cursor, "own_status");
                tasks.add(task);
            }
        }
        return tasks;
    }

    public void closeTask(long taskId) {
        Account current = requireTeacher();
        ContentValues values = new ContentValues();
        values.put("closed", 1);
        values.put("end_at", System.currentTimeMillis());
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            database.update("attendance_task", values, "task_id=? AND teacher_id=? AND closed=0", args(taskId, current.id));
            finalizeExpired(database);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void signByShake(long taskId) {
        Account current = requireAccount();
        require(!current.isTeacher(), "请使用学生账号签到");
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            finalizeExpired(database);
            require(exists(database, "SELECT 1 FROM attendance_task t JOIN student_course sc ON sc.course_id=t.course_id WHERE t.task_id=? AND sc.student_id=? AND t.closed=0 AND t.start_at<=? AND t.end_at>?", args(taskId, current.studentId, System.currentTimeMillis(), System.currentTimeMillis())), "签到已结束或你未选修该课程");
            ContentValues values = new ContentValues();
            values.put("status", PRESENT);
            values.put("signed_at", System.currentTimeMillis());
            values.put("source", "摇一摇");
            require(database.update("attendance_record", values, "task_id=? AND student_id=? AND status=?", new String[]{String.valueOf(taskId), String.valueOf(current.studentId), WAITING}) == 1, "已签到、已有考勤结果，或不在本次签到名单中");
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public List<Record> records(long taskId) {
        Account current = requireAccount();
        SQLiteDatabase database = helper.getWritableDatabase();
        finalizeExpired(database);
        String filter = current.isTeacher() ? "t.teacher_id=?" : "r.student_id=?";
        List<Record> records = new ArrayList<>();
        try (Cursor cursor = database.rawQuery("SELECT r.*, t.start_at, t.closed, c.course_name, s.student_name, s.student_number FROM attendance_record r JOIN attendance_task t ON t.task_id=r.task_id JOIN course c ON c.course_id=t.course_id JOIN student s ON s.student_id=r.student_id WHERE " + filter + (taskId > 0 ? " AND t.task_id=" + taskId : "") + " ORDER BY t.start_at DESC, s.student_number", args(current.isTeacher() ? current.id : current.studentId))) {
            while (cursor.moveToNext()) {
                Record record = new Record();
                record.id = number(cursor, "record_id");
                record.taskId = number(cursor, "task_id");
                record.courseName = string(cursor, "course_name");
                record.name = string(cursor, "student_name");
                record.number = string(cursor, "student_number");
                record.status = string(cursor, "status");
                record.source = string(cursor, "source");
                record.time = number(cursor, "signed_at");
                record.lessonTime = number(cursor, "start_at");
                record.closed = number(cursor, "closed") == 1;
                records.add(record);
            }
        }
        return records;
    }

    public void correctRecord(long recordId, String status, String reason) {
        Account current = requireTeacher();
        require(!reason.trim().isEmpty(), "请填写修改原因");
        require(PRESENT.equals(status) || ABSENT.equals(status) || LEAVE.equals(status), "考勤状态不正确");
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            changeRecord(database, recordId, status, reason, current.id);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void changeRecord(SQLiteDatabase database, long recordId, String status, String reason, long teacherId) {
        String previous;
        try (Cursor cursor = database.rawQuery("SELECT r.status FROM attendance_record r JOIN attendance_task t ON t.task_id=r.task_id WHERE r.record_id=? AND t.teacher_id=?", args(recordId, teacherId))) {
            require(cursor.moveToFirst(), "无权修改此考勤");
            previous = cursor.getString(0);
        }
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("source", "教师修改：" + reason);
        database.update("attendance_record", values, "record_id=?", args(recordId));
        ContentValues change = new ContentValues();
        change.put("record_id", recordId);
        change.put("account_id", teacherId);
        change.put("old_status", previous);
        change.put("new_status", status);
        change.put("reason", reason);
        change.put("changed_at", System.currentTimeMillis());
        database.insertOrThrow("attendance_change", null, change);
    }

    public List<String> changes(long recordId) {
        boolean permitted = false;
        for (Record record : records(0)) if (record.id == recordId) permitted = true;
        require(permitted, "无权查看此记录");
        List<String> changes = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT h.changed_at, h.old_status, h.new_status, h.reason, a.name FROM attendance_change h JOIN account a ON a.account_id=h.account_id WHERE record_id=? ORDER BY changed_at DESC", args(recordId))) {
            while (cursor.moveToNext()) changes.add(android.text.format.DateFormat.format("MM-dd HH:mm", cursor.getLong(0)) + " · " + cursor.getString(4) + "\n" + cursor.getString(1) + " → " + cursor.getString(2) + "\n" + cursor.getString(3));
        }
        return changes;
    }

    public void requestChange(long recordId, String desired, String reason) {
        Account current = requireAccount();
        require(!current.isTeacher(), "请使用学生账号申请");
        require(!reason.trim().isEmpty(), "请填写申请原因");
        require(PRESENT.equals(desired) || ABSENT.equals(desired) || LEAVE.equals(desired), "申请状态不正确");
        SQLiteDatabase database = helper.getWritableDatabase();
        finalizeExpired(database);
        try (Cursor cursor = database.rawQuery("SELECT r.status, t.closed FROM attendance_record r JOIN attendance_task t ON t.task_id=r.task_id WHERE r.record_id=? AND r.student_id=?", args(recordId, current.studentId))) {
            require(cursor.moveToFirst(), "无权修改此记录");
            require(cursor.getInt(1) == 1, "请在签到结束后申请修改");
            require(!desired.equals(cursor.getString(0)), "申请状态与当前状态相同");
        }
        require(!exists(database, "SELECT 1 FROM attendance_request WHERE record_id=? AND state='待审批'", args(recordId)), "该记录已有待审批申请");
        ContentValues values = new ContentValues();
        values.put("record_id", recordId);
        values.put("account_id", current.id);
        values.put("desired", desired);
        values.put("reason", reason);
        database.insertOrThrow("attendance_request", null, values);
    }

    public List<Request> requests() {
        Account current = requireAccount();
        List<Request> requests = new ArrayList<>();
        String filter = current.isTeacher() ? "t.teacher_id=?" : "q.account_id=?";
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT q.*, s.student_name, c.course_name, t.start_at FROM attendance_request q JOIN attendance_record r ON r.record_id=q.record_id JOIN attendance_task t ON t.task_id=r.task_id JOIN student s ON s.student_id=r.student_id JOIN course c ON c.course_id=t.course_id WHERE " + filter + " ORDER BY (q.state='待审批') DESC, q.request_id DESC", args(current.id))) {
            while (cursor.moveToNext()) {
                Request request = new Request();
                request.id = number(cursor, "request_id");
                request.recordId = number(cursor, "record_id");
                request.name = string(cursor, "student_name");
                request.courseName = string(cursor, "course_name");
                request.desired = string(cursor, "desired");
                request.reason = string(cursor, "reason");
                request.state = string(cursor, "state");
                request.reply = string(cursor, "reply");
                request.lessonTime = number(cursor, "start_at");
                requests.add(request);
            }
        }
        return requests;
    }

    public void review(long requestId, boolean approve, String reply) {
        Account current = requireTeacher();
        require(!reply.trim().isEmpty(), "请填写处理意见");
        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            try (Cursor cursor = database.rawQuery("SELECT q.record_id, q.desired FROM attendance_request q JOIN attendance_record r ON r.record_id=q.record_id JOIN attendance_task t ON t.task_id=r.task_id WHERE q.request_id=? AND q.state='待审批' AND t.teacher_id=?", args(requestId, current.id))) {
                require(cursor.moveToFirst(), "申请已处理或无权审批");
                if (approve) changeRecord(database, cursor.getLong(0), cursor.getString(1), "申请通过：" + reply, current.id);
            }
            ContentValues values = new ContentValues();
            values.put("state", approve ? "已通过" : "已驳回");
            values.put("reply", reply);
            database.update("attendance_request", values, "request_id=?", args(requestId));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public List<Statistic> statistics(boolean byStudent) {
        Account current = requireAccount();
        SQLiteDatabase database = helper.getWritableDatabase();
        finalizeExpired(database);
        String filter = current.isTeacher() ? "t.teacher_id=?" : "r.student_id=?";
        String group = current.isTeacher() && byStudent ? "t.course_id, r.student_id" : "t.course_id";
        String label = current.isTeacher() && byStudent ? "c.course_name || ' · ' || s.student_name || ' (' || s.student_number || ')'" : "c.course_name";
        List<Statistic> statistics = new ArrayList<>();
        try (Cursor cursor = database.rawQuery("SELECT " + label + " AS label, COUNT(*) AS total, SUM(r.status='缺课') AS absent, SUM(r.status='已到') AS present, SUM(r.status='请假') AS leave_count FROM attendance_record r JOIN attendance_task t ON t.task_id=r.task_id JOIN course c ON c.course_id=t.course_id JOIN student s ON s.student_id=r.student_id WHERE t.closed=1 AND " + filter + " GROUP BY " + group + " ORDER BY c.course_name, s.student_number", args(current.isTeacher() ? current.id : current.studentId))) {
            while (cursor.moveToNext()) {
                Statistic statistic = new Statistic();
                statistic.label = string(cursor, "label");
                statistic.total = (int) number(cursor, "total");
                statistic.absent = (int) number(cursor, "absent");
                statistic.present = (int) number(cursor, "present");
                statistic.leave = (int) number(cursor, "leave_count");
                statistics.add(statistic);
            }
        }
        return statistics;
    }

    private Account requireAccount() {
        Account account = currentAccount();
        require(account != null, "请先登录");
        return account;
    }

    private Account requireTeacher() {
        Account account = requireAccount();
        require(account.isTeacher(), "此操作仅教师可用");
        return account;
    }

    private void requireOwnCourse(int courseId, long accountId) {
        require(exists(helper.getReadableDatabase(), "SELECT 1 FROM course_owner WHERE course_id=? AND account_id=?", args(courseId, accountId)), "只能管理自己任教的课程");
    }

    private static void validatePhone(String phone) {
        require(phone.isEmpty() || phone.matches("[0-9+ -]{6,20}"), "请输入有效手机号，或留空");
    }

    private static boolean exists(SQLiteDatabase database, String query, String[] arguments) {
        try (Cursor cursor = database.rawQuery(query, arguments)) { return cursor.moveToFirst(); }
    }

    private static String[] args(long... values) {
        String[] arguments = new String[values.length];
        for (int index = 0; index < values.length; index++) arguments[index] = String.valueOf(values[index]);
        return arguments;
    }

    private static String string(Cursor cursor, String column) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(column));
        return value == null ? "" : value;
    }

    private static long number(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    @Override public void close() { helper.close(); }
}
