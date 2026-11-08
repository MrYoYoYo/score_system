package com.example.score_system.ui.student;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Student;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.documentfile.provider.DocumentFile;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelImporter {
    private Context context;
    private DatabaseHelper dbHelper;

    public ExcelImporter(Context context, DatabaseHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    public int importStudents(Uri uri) {
        List<Student> students = new ArrayList<>();

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String studentNumber = getCellValue(row.getCell(0));
                String studentName = getCellValue(row.getCell(1));
                String className = getCellValue(row.getCell(2));

                if (!studentNumber.isEmpty() && !studentName.isEmpty() && !className.isEmpty()) {
                    Student student = new Student(studentNumber, studentName, className);
                    students.add(student);
                }
            }

            workbook.close();
            inputStream.close();

            int successCount = 0;
            for (Student student : students) {
                long result = dbHelper.insertStudent(student);
                if (result > 0) {
                    int studentId = (int) result;
                    // Excel导入的学生默认选修所有现有课程
                    List<Integer> allCourseIds = dbHelper.getAllCourseIds();
                    dbHelper.updateStudentEnrollments(studentId, allCourseIds);
                    successCount++;
                }
            }

            return successCount;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
