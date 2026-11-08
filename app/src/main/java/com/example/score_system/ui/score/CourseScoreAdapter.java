package com.example.score_system.ui.score;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Course;

import java.util.List;

public class CourseScoreAdapter extends RecyclerView.Adapter<CourseScoreAdapter.CourseViewHolder> {
    private List<Course> courseList;
    private Context context;
    private OnCourseClickListener listener;
    private DatabaseHelper dbHelper;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public CourseScoreAdapter(List<Course> courseList, Context context, OnCourseClickListener listener) {
        this.courseList = courseList;
        this.context = context;
        this.listener = listener;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course_card, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.tvCourseNumber.setText("课程号: " + course.getCourseNumber());
        holder.tvCourseName.setText("课程名称: " + course.getCourseName());

        // 获取并显示选课人数
        int enrollmentCount = dbHelper.getCourseEnrollmentCount(course.getCourseId());
        holder.tvEnrollmentCount.setText("选课人数: " + enrollmentCount);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseNumber;
        TextView tvCourseName;
        TextView tvEnrollmentCount;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseNumber = itemView.findViewById(R.id.tv_course_number);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            tvEnrollmentCount = itemView.findViewById(R.id.tv_enrollment_count);
        }
    }
}
