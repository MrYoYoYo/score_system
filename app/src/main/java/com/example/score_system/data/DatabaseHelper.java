package com.example.score_system.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.score_system.model.Course;
import com.example.score_system.model.CourseConfig;
import com.example.score_system.model.Score;
import com.example.score_system.model.Student;
import com.example.score_system.model.Teacher;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "score_system.db";
    private static final int DATABASE_VERSION = 4;
    private final Context appContext;

    // 表名
    private static final String TABLE_TEACHER = "teacher";
    private static final String TABLE_STUDENT = "student";
    private static final String TABLE_COURSE = "course";
    private static final String TABLE_COURSE_CONFIG = "course_config";
    private static final String TABLE_SCORE = "score";
    private static final String TABLE_STUDENT_COURSE = "student_course";

    // Teacher表字段
    private static final String TEACHER_ID = "teacher_id";
    private static final String TEACHER_USERNAME = "username";
    private static final String TEACHER_PASSWORD = "password";

    // Student表字段
    private static final String STUDENT_ID = "student_id";
    private static final String STUDENT_NUMBER = "student_number";
    private static final String STUDENT_NAME = "student_name";
    private static final String CLASS_NAME = "class_name";

    // Course表字段
    private static final String COURSE_ID = "course_id";
    private static final String COURSE_NUMBER = "course_number";
    private static final String COURSE_NAME = "course_name";

    // CourseConfig表字段
    private static final String CONFIG_ID = "config_id";
    private static final String CONFIG_COURSE_ID = "course_id";
    private static final String ATTENDANCE_WEIGHT = "attendance_weight";
    private static final String HOMEWORK_WEIGHT = "homework_weight";
    private static final String LAB_WEIGHT = "lab_weight";

    // Score表字段
    private static final String SCORE_ID = "score_id";
    private static final String SCORE_COURSE_ID = "course_id";
    private static final String SCORE_STUDENT_ID = "student_id";
    private static final String SCORE_TYPE = "score_type";
    private static final String SCORE_INDEX = "score_index";
    private static final String SCORE_VALUE = "score_value";
    private static final String SCORE_REMARK = "remark";

    // Student_Course表字段
    private static final String ENROLLMENT_ID = "enrollment_id";
    private static final String ENROLLMENT_STUDENT_ID = "student_id";
    private static final String ENROLLMENT_COURSE_ID = "course_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建Teacher表
        String CREATE_TEACHER_TABLE = "CREATE TABLE " + TABLE_TEACHER + "("
                + TEACHER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TEACHER_USERNAME + " TEXT UNIQUE NOT NULL,"
                + TEACHER_PASSWORD + " TEXT NOT NULL"
                + ")";

        // 创建Student表
        String CREATE_STUDENT_TABLE = "CREATE TABLE " + TABLE_STUDENT + "("
                + STUDENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + STUDENT_NUMBER + " TEXT UNIQUE NOT NULL,"
                + STUDENT_NAME + " TEXT NOT NULL,"
                + CLASS_NAME + " TEXT NOT NULL"
                + ")";

        // 创建Course表
        String CREATE_COURSE_TABLE = "CREATE TABLE " + TABLE_COURSE + "("
                + COURSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COURSE_NUMBER + " TEXT UNIQUE NOT NULL,"
                + COURSE_NAME + " TEXT NOT NULL"
                + ")";

        // 创建CourseConfig表
        String CREATE_COURSE_CONFIG_TABLE = "CREATE TABLE " + TABLE_COURSE_CONFIG + "("
                + CONFIG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CONFIG_COURSE_ID + " INTEGER UNIQUE NOT NULL,"
                + ATTENDANCE_WEIGHT + " REAL DEFAULT 0.2,"
                + HOMEWORK_WEIGHT + " REAL DEFAULT 0.3,"
                + LAB_WEIGHT + " REAL DEFAULT 0.5,"
                + "FOREIGN KEY(" + CONFIG_COURSE_ID + ") REFERENCES " + TABLE_COURSE + "(" + COURSE_ID + ")"
                + ")";

        // 创建Score表
        String CREATE_SCORE_TABLE = "CREATE TABLE " + TABLE_SCORE + "("
                + SCORE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SCORE_COURSE_ID + " INTEGER NOT NULL,"
                + SCORE_STUDENT_ID + " INTEGER NOT NULL,"
                + SCORE_TYPE + " INTEGER NOT NULL,"
                + SCORE_INDEX + " INTEGER NOT NULL,"
                + SCORE_VALUE + " REAL NOT NULL,"
                + SCORE_REMARK + " TEXT,"
                + "FOREIGN KEY(" + SCORE_COURSE_ID + ") REFERENCES " + TABLE_COURSE + "(" + COURSE_ID + "),"
                + "FOREIGN KEY(" + SCORE_STUDENT_ID + ") REFERENCES " + TABLE_STUDENT + "(" + STUDENT_ID + ")"
                + ")";

        // 创建Student_Course表（学生选课关联表）
        String CREATE_STUDENT_COURSE_TABLE = "CREATE TABLE " + TABLE_STUDENT_COURSE + "("
                + ENROLLMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ENROLLMENT_STUDENT_ID + " INTEGER NOT NULL,"
                + ENROLLMENT_COURSE_ID + " INTEGER NOT NULL,"
                + "UNIQUE(" + ENROLLMENT_STUDENT_ID + "," + ENROLLMENT_COURSE_ID + "),"
                + "FOREIGN KEY(" + ENROLLMENT_STUDENT_ID + ") REFERENCES " + TABLE_STUDENT + "(" + STUDENT_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + ENROLLMENT_COURSE_ID + ") REFERENCES " + TABLE_COURSE + "(" + COURSE_ID + ") ON DELETE CASCADE"
                + ")";

        db.execSQL(CREATE_TEACHER_TABLE);
        db.execSQL(CREATE_STUDENT_TABLE);
        db.execSQL(CREATE_COURSE_TABLE);
        db.execSQL(CREATE_COURSE_CONFIG_TABLE);
        db.execSQL(CREATE_SCORE_TABLE);
        db.execSQL(CREATE_STUDENT_COURSE_TABLE);

        // 插入默认教师账号
        insertDefaultTeacher(db);
        AttendanceRepository.install(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // admin的密码为234512
            ContentValues values = new ContentValues();
            values.put(TEACHER_PASSWORD, "234512");
            db.update(TABLE_TEACHER, values, TEACHER_USERNAME + "=?", new String[]{"admin"});
        }
        if (oldVersion < 3) {
            // 创建student_course表
            String CREATE_STUDENT_COURSE_TABLE = "CREATE TABLE " + TABLE_STUDENT_COURSE + "("
                    + ENROLLMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ENROLLMENT_STUDENT_ID + " INTEGER NOT NULL,"
                    + ENROLLMENT_COURSE_ID + " INTEGER NOT NULL,"
                    + "UNIQUE(" + ENROLLMENT_STUDENT_ID + "," + ENROLLMENT_COURSE_ID + "),"
                    + "FOREIGN KEY(" + ENROLLMENT_STUDENT_ID + ") REFERENCES " + TABLE_STUDENT + "(" + STUDENT_ID + ") ON DELETE CASCADE,"
                    + "FOREIGN KEY(" + ENROLLMENT_COURSE_ID + ") REFERENCES " + TABLE_COURSE + "(" + COURSE_ID + ") ON DELETE CASCADE"
                    + ")";
            db.execSQL(CREATE_STUDENT_COURSE_TABLE);

            // 为现有数据自动创建选课关系（所有学生选所有课程）
            String MIGRATE_ENROLLMENTS = "INSERT INTO " + TABLE_STUDENT_COURSE +
                    "(" + ENROLLMENT_STUDENT_ID + "," + ENROLLMENT_COURSE_ID + ") " +
                    "SELECT " + STUDENT_ID + ", " + COURSE_ID +
                    " FROM " + TABLE_STUDENT + ", " + TABLE_COURSE;
            db.execSQL(MIGRATE_ENROLLMENTS);
        }
        if (oldVersion < 4) AttendanceRepository.install(db);
    }

    private void insertDefaultTeacher(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(TEACHER_USERNAME, "admin");
        values.put(TEACHER_PASSWORD, "234512");
        db.insert(TABLE_TEACHER, null, values);
    }

    // ==================== Teacher相关操作 ====================

    public boolean login(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TEACHER,
                new String[]{TEACHER_ID},
                TEACHER_USERNAME + "=? AND " + TEACHER_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    // ==================== Student相关操作 ====================

    public long insertStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STUDENT_NUMBER, student.getStudentNumber());
        values.put(STUDENT_NAME, student.getStudentName());
        values.put(CLASS_NAME, student.getClassName());
        return db.insert(TABLE_STUDENT, null, values);
    }

    public int updateStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STUDENT_NUMBER, student.getStudentNumber());
        values.put(STUDENT_NAME, student.getStudentName());
        values.put(CLASS_NAME, student.getClassName());
        return db.update(TABLE_STUDENT, values, STUDENT_ID + "=?",
                new String[]{String.valueOf(student.getStudentId())});
    }

    public int deleteStudent(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM account WHERE student_id=? UNION SELECT 1 FROM attendance_record WHERE student_id=?", new String[]{String.valueOf(studentId), String.valueOf(studentId)})) {
            if (cursor.moveToFirst()) return 0;
        }
        db.beginTransaction();
        try {
            db.delete(TABLE_SCORE, SCORE_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)});
            db.delete(TABLE_STUDENT_COURSE, ENROLLMENT_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)});
            int deleted = db.delete(TABLE_STUDENT, STUDENT_ID + "=?", new String[]{String.valueOf(studentId)});
            db.setTransactionSuccessful();
            return deleted;
        } finally {
            db.endTransaction();
        }
    }

    public Student getStudentById(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENT, null, STUDENT_ID + "=?",
                new String[]{String.valueOf(studentId)}, null, null, null);
        Student student = null;
        if (cursor != null && cursor.moveToFirst()) {
            student = new Student();
            student.setStudentId(cursor.getInt(cursor.getColumnIndexOrThrow(STUDENT_ID)));
            student.setStudentNumber(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NUMBER)));
            student.setStudentName(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NAME)));
            student.setClassName(cursor.getString(cursor.getColumnIndexOrThrow(CLASS_NAME)));
            cursor.close();
        }
        return student;
    }

    public List<Student> getAllStudents() {
        List<Student> studentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENT, null, null, null, null, null, STUDENT_NUMBER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Student student = new Student();
                student.setStudentId(cursor.getInt(cursor.getColumnIndexOrThrow(STUDENT_ID)));
                student.setStudentNumber(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NUMBER)));
                student.setStudentName(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NAME)));
                student.setClassName(cursor.getString(cursor.getColumnIndexOrThrow(CLASS_NAME)));
                studentList.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return studentList;
    }

    public List<Student> getStudentsByClass(String className) {
        List<Student> studentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENT, null, CLASS_NAME + "=?",
                new String[]{className}, null, null, STUDENT_NUMBER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Student student = new Student();
                student.setStudentId(cursor.getInt(cursor.getColumnIndexOrThrow(STUDENT_ID)));
                student.setStudentNumber(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NUMBER)));
                student.setStudentName(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NAME)));
                student.setClassName(cursor.getString(cursor.getColumnIndexOrThrow(CLASS_NAME)));
                studentList.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return studentList;
    }

    // ==================== Course相关操作 ====================

    public long insertCourse(Course course) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COURSE_NUMBER, course.getCourseNumber());
        values.put(COURSE_NAME, course.getCourseName());
        long courseId = db.insert(TABLE_COURSE, null, values);

        // 同时创建默认配置
        if (courseId > 0) {
            CourseConfig config = new CourseConfig((int) courseId);
            insertCourseConfig(config);
            ContentValues owner = new ContentValues();
            owner.put("course_id", courseId);
            owner.put("account_id", new SessionManager(appContext).accountId());
            db.insertOrThrow("course_owner", null, owner);
        }
        return courseId;
    }

    public int updateCourse(Course course) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COURSE_NUMBER, course.getCourseNumber());
        values.put(COURSE_NAME, course.getCourseName());
        return db.update(TABLE_COURSE, values, COURSE_ID + "=?",
                new String[]{String.valueOf(course.getCourseId())});
    }

    public int deleteCourse(int courseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] courseArgs = {String.valueOf(courseId)};
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM attendance_task WHERE course_id=?", courseArgs)) {
            if (cursor.moveToFirst()) return 0;
        }
        db.beginTransaction();
        try {
            db.delete(TABLE_SCORE, SCORE_COURSE_ID + "=?", courseArgs);
            db.delete(TABLE_COURSE_CONFIG, CONFIG_COURSE_ID + "=?", courseArgs);
            db.delete(TABLE_STUDENT_COURSE, ENROLLMENT_COURSE_ID + "=?", courseArgs);
            db.delete("schedule", "course_id=?", courseArgs);
            db.delete("course_owner", "course_id=?", courseArgs);
            int deleted = db.delete(TABLE_COURSE, COURSE_ID + "=?", courseArgs);
            db.setTransactionSuccessful();
            return deleted;
        } finally {
            db.endTransaction();
        }
    }

    public Course getCourseById(int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSE, null, COURSE_ID + "=?",
                new String[]{String.valueOf(courseId)}, null, null, null);
        Course course = null;
        if (cursor != null && cursor.moveToFirst()) {
            course = new Course();
            course.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(COURSE_ID)));
            course.setCourseNumber(cursor.getString(cursor.getColumnIndexOrThrow(COURSE_NUMBER)));
            course.setCourseName(cursor.getString(cursor.getColumnIndexOrThrow(COURSE_NAME)));
            cursor.close();
        }
        return course;
    }

    public List<Course> getAllCourses() {
        List<Course> courseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT c.* FROM course c JOIN course_owner o ON o.course_id=c.course_id WHERE o.account_id=? ORDER BY c.course_number", new String[]{String.valueOf(new SessionManager(appContext).accountId())});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Course course = new Course();
                course.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(COURSE_ID)));
                course.setCourseNumber(cursor.getString(cursor.getColumnIndexOrThrow(COURSE_NUMBER)));
                course.setCourseName(cursor.getString(cursor.getColumnIndexOrThrow(COURSE_NAME)));
                courseList.add(course);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return courseList;
    }

    public List<Integer> getAllCourseIds() {
        List<Integer> courseIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSE, new String[]{COURSE_ID}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                courseIds.add(cursor.getInt(cursor.getColumnIndexOrThrow(COURSE_ID)));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return courseIds;
    }

    // ==================== CourseConfig相关操作 ====================

    public long insertCourseConfig(CourseConfig config) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CONFIG_COURSE_ID, config.getCourseId());
        values.put(ATTENDANCE_WEIGHT, config.getAttendanceWeight());
        values.put(HOMEWORK_WEIGHT, config.getHomeworkWeight());
        values.put(LAB_WEIGHT, config.getLabWeight());
        return db.insert(TABLE_COURSE_CONFIG, null, values);
    }

    public int updateCourseConfig(CourseConfig config) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ATTENDANCE_WEIGHT, config.getAttendanceWeight());
        values.put(HOMEWORK_WEIGHT, config.getHomeworkWeight());
        values.put(LAB_WEIGHT, config.getLabWeight());
        return db.update(TABLE_COURSE_CONFIG, values, CONFIG_COURSE_ID + "=?",
                new String[]{String.valueOf(config.getCourseId())});
    }

    public CourseConfig getCourseConfig(int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSE_CONFIG, null, CONFIG_COURSE_ID + "=?",
                new String[]{String.valueOf(courseId)}, null, null, null);
        CourseConfig config = new CourseConfig(courseId);
        if (cursor != null && cursor.moveToFirst()) {
            config.setConfigId(cursor.getInt(cursor.getColumnIndexOrThrow(CONFIG_ID)));
            config.setAttendanceWeight(cursor.getDouble(cursor.getColumnIndexOrThrow(ATTENDANCE_WEIGHT)));
            config.setHomeworkWeight(cursor.getDouble(cursor.getColumnIndexOrThrow(HOMEWORK_WEIGHT)));
            config.setLabWeight(cursor.getDouble(cursor.getColumnIndexOrThrow(LAB_WEIGHT)));
        }
        if (cursor != null) {
            cursor.close();
        }
        return config;
    }

    // ==================== Score相关操作 ====================

    public long insertScore(Score score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SCORE_COURSE_ID, score.getCourseId());
        values.put(SCORE_STUDENT_ID, score.getStudentId());
        values.put(SCORE_TYPE, score.getScoreType());
        values.put(SCORE_INDEX, score.getScoreIndex());
        values.put(SCORE_VALUE, score.getScoreValue());
        values.put(SCORE_REMARK, score.getRemark());
        return db.insert(TABLE_SCORE, null, values);
    }

    public long insertOrUpdateScore(Score score) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 先查询是否存在
        Cursor cursor = db.query(TABLE_SCORE, null,
                SCORE_COURSE_ID + "=? AND " + SCORE_STUDENT_ID + "=? AND " +
                        SCORE_TYPE + "=? AND " + SCORE_INDEX + "=?",
                new String[]{
                        String.valueOf(score.getCourseId()),
                        String.valueOf(score.getStudentId()),
                        String.valueOf(score.getScoreType()),
                        String.valueOf(score.getScoreIndex())
                },
                null, null, null);

        long result;
        if (cursor != null && cursor.getCount() > 0) {
            // 存在则更新
            cursor.moveToFirst();
            int scoreId = cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_ID));
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(SCORE_VALUE, score.getScoreValue());
            values.put(SCORE_REMARK, score.getRemark());
            result = db.update(TABLE_SCORE, values, SCORE_ID + "=?",
                    new String[]{String.valueOf(scoreId)});
        } else {
            // 不存在则插入
            if (cursor != null) cursor.close();
            result = insertScore(score);
        }
        return result;
    }

    public int deleteScore(int scoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_SCORE, SCORE_ID + "=?",
                new String[]{String.valueOf(scoreId)});
    }

    public int updateScore(Score score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SCORE_COURSE_ID, score.getCourseId());
        values.put(SCORE_STUDENT_ID, score.getStudentId());
        values.put(SCORE_TYPE, score.getScoreType());
        values.put(SCORE_INDEX, score.getScoreIndex());
        values.put(SCORE_VALUE, score.getScoreValue());
        values.put(SCORE_REMARK, score.getRemark());

        return db.update(TABLE_SCORE, values, SCORE_ID + "=?",
                new String[]{String.valueOf(score.getScoreId())});
    }

    public List<Score> getScoresByCourseAndStudent(int courseId, int studentId) {
        List<Score> scoreList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCORE, null,
                SCORE_COURSE_ID + "=? AND " + SCORE_STUDENT_ID + "=?",
                new String[]{String.valueOf(courseId), String.valueOf(studentId)},
                null, null, SCORE_TYPE + "," + SCORE_INDEX);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Score score = new Score();
                score.setScoreId(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_ID)));
                score.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_COURSE_ID)));
                score.setStudentId(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_STUDENT_ID)));
                score.setScoreType(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_TYPE)));
                score.setScoreIndex(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_INDEX)));
                score.setScoreValue(cursor.getDouble(cursor.getColumnIndexOrThrow(SCORE_VALUE)));
                score.setRemark(cursor.getString(cursor.getColumnIndexOrThrow(SCORE_REMARK)));
                scoreList.add(score);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return scoreList;
    }

    public List<Score> getAllScoresByCourse(int courseId) {
        List<Score> scoreList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCORE, null,
                SCORE_COURSE_ID + "=?",
                new String[]{String.valueOf(courseId)},
                null, null, SCORE_TYPE + "," + SCORE_INDEX);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Score score = new Score();
                score.setScoreId(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_ID)));
                score.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_COURSE_ID)));
                score.setStudentId(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_STUDENT_ID)));
                score.setScoreType(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_TYPE)));
                score.setScoreIndex(cursor.getInt(cursor.getColumnIndexOrThrow(SCORE_INDEX)));
                score.setScoreValue(cursor.getDouble(cursor.getColumnIndexOrThrow(SCORE_VALUE)));
                score.setRemark(cursor.getString(cursor.getColumnIndexOrThrow(SCORE_REMARK)));
                scoreList.add(score);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return scoreList;
    }

    public double calculateTotalScore(int courseId, int studentId, CourseConfig config) {
        List<Score> scores = getScoresByCourseAndStudent(courseId, studentId);

        double attendanceSum = 0;
        double homeworkSum = 0;
        double labSum = 0;

        int attendanceCount = 0;
        int homeworkCount = 0;
        int labCount = 0;

        for (Score score : scores) {
            switch (score.getScoreType()) {
                case Score.TYPE_ATTENDANCE:
                    attendanceSum += score.getScoreValue();
                    attendanceCount++;
                    break;
                case Score.TYPE_HOMEWORK:
                    homeworkSum += score.getScoreValue();
                    homeworkCount++;
                    break;
                case Score.TYPE_LAB:
                    labSum += score.getScoreValue();
                    labCount++;
                    break;
            }
        }

        // 获取课程中该类型的总次数
        int totalAttendanceIndex = getTotalIndexCount(courseId, Score.TYPE_ATTENDANCE);
        int totalHomeworkIndex = getTotalIndexCount(courseId, Score.TYPE_HOMEWORK);
        int totalLabIndex = getTotalIndexCount(courseId, Score.TYPE_LAB);

        // 计算各部分平均分
        // 如果学生完全没参加该类型考试，则按满分计算
        // 如果学生参加了部分考试，则按实际参加考试的平均分计算
        double attendanceAvg = 0;
        if (attendanceCount == 0) {
            attendanceAvg = 100; // 完全没参加考试按满分
        } else {
            // 按实际参加考试的平均分计算
            attendanceAvg = (attendanceSum / attendanceCount) * 10;
            // 按参加比例调整
            attendanceAvg = attendanceAvg * ((double) attendanceCount / totalAttendanceIndex);
        }

        double homeworkAvg = 0;
        if (homeworkCount == 0) {
            homeworkAvg = 100;
        } else {
            homeworkAvg = (homeworkSum / homeworkCount) * 10;
            homeworkAvg = homeworkAvg * ((double) homeworkCount / totalHomeworkIndex);
        }

        double labAvg = 0;
        if (labCount == 0) {
            labAvg = 100;
        } else {
            labAvg = (labSum / labCount) * 10;
            labAvg = labAvg * ((double) labCount / totalLabIndex);
        }

        // 限制最高100分
        attendanceAvg = Math.min(attendanceAvg, 100);
        homeworkAvg = Math.min(homeworkAvg, 100);
        labAvg = Math.min(labAvg, 100);

        // 按权重计算总评
        return attendanceAvg * config.getAttendanceWeight() +
                homeworkAvg * config.getHomeworkWeight() +
                labAvg * config.getLabWeight();
    }
    //获取某课程某类型考试的总次数
    private int getTotalIndexCount(int courseId, int scoreType) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCORE,
                new String[]{"MAX(" + SCORE_INDEX + ")"},
                SCORE_COURSE_ID + "=? AND " + SCORE_TYPE + "=?",
                new String[]{String.valueOf(courseId), String.valueOf(scoreType)},
                null, null, null);

        int maxIndex = 0;
        if (cursor != null && cursor.moveToFirst()) {
            maxIndex = cursor.getInt(0);
            cursor.close();
        }
        return maxIndex;
    }


    // 学生选课相关方法

    //让学生选修课程
    public boolean enrollStudentInCourse(int studentId, int courseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ENROLLMENT_STUDENT_ID, studentId);
        values.put(ENROLLMENT_COURSE_ID, courseId);

        try {
            db.insert(TABLE_STUDENT_COURSE, null, values);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //取消学生选课
    public boolean removeStudentFromCourse(int studentId, int courseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_STUDENT_COURSE,
                ENROLLMENT_STUDENT_ID + "=? AND " + ENROLLMENT_COURSE_ID + "=?",
                new String[]{String.valueOf(studentId), String.valueOf(courseId)});
        return result > 0;
    }

    //获取学生选修的所有课程ID
    public List<Integer> getStudentCourseIds(int studentId) {
        List<Integer> courseIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENT_COURSE,
                new String[]{ENROLLMENT_COURSE_ID},
                ENROLLMENT_STUDENT_ID + "=?",
                new String[]{String.valueOf(studentId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                courseIds.add(cursor.getInt(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return courseIds;
    }

    //获取选修某课程的所有学生
    public List<Student> getCourseStudents(int courseId) {
        List<Student> studentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT s.* FROM " + TABLE_STUDENT + " s " +
                "INNER JOIN " + TABLE_STUDENT_COURSE + " sc " +
                "ON s." + STUDENT_ID + " = sc." + ENROLLMENT_STUDENT_ID + " " +
                "WHERE sc." + ENROLLMENT_COURSE_ID + "=?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(courseId)});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Student student = new Student();
                student.setStudentId(cursor.getInt(cursor.getColumnIndexOrThrow(STUDENT_ID)));
                student.setStudentNumber(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NUMBER)));
                student.setStudentName(cursor.getString(cursor.getColumnIndexOrThrow(STUDENT_NAME)));
                student.setClassName(cursor.getString(cursor.getColumnIndexOrThrow(CLASS_NAME)));
                studentList.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return studentList;
    }

    //检查学生是否选修了某课程
    public boolean isStudentEnrolled(int studentId, int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENT_COURSE,
                new String[]{ENROLLMENT_ID},
                ENROLLMENT_STUDENT_ID + "=? AND " + ENROLLMENT_COURSE_ID + "=?",
                new String[]{String.valueOf(studentId), String.valueOf(courseId)},
                null, null, null);

        boolean enrolled = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return enrolled;
    }

    //获取某课程的选课人数
    public int getCourseEnrollmentCount(int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENT_COURSE,
                new String[]{"COUNT(*)"},
                ENROLLMENT_COURSE_ID + "=?",
                new String[]{String.valueOf(courseId)},
                null, null, null);

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    //更新学生的选课列表
    public void updateStudentEnrollments(int studentId, List<Integer> courseIds) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 先删除该学生的所有选课记录
        db.delete(TABLE_STUDENT_COURSE,
                ENROLLMENT_STUDENT_ID + "=?",
                new String[]{String.valueOf(studentId)});

        // 重新插入选课记录
        for (Integer courseId : courseIds) {
            ContentValues values = new ContentValues();
            values.put(ENROLLMENT_STUDENT_ID, studentId);
            values.put(ENROLLMENT_COURSE_ID, courseId);
            db.insert(TABLE_STUDENT_COURSE, null, values);
        }
    }
}
