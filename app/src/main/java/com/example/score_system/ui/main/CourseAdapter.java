package com.example.score_system.ui.main;

import android.content.Context;
import android.graphics.Color;
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

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private List<Course> courseList;
    private Context context;
    private DatabaseHelper dbHelper;

    // 6种不同的颜色资源ID
    private static final int[] COURSE_COLORS = {
            R.color.course_color_1,
            R.color.course_color_2,
            R.color.course_color_3,
            R.color.course_color_4,
            R.color.course_color_5,
            R.color.course_color_6
    };

    public CourseAdapter(List<Course> courseList, Context context) {
        this.courseList = courseList;
        this.context = context;
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

        // 根据位置设置不同的背景颜色
        int colorIndex = position % COURSE_COLORS.length;
        int color = context.getResources().getColor(COURSE_COLORS[colorIndex]);
        holder.cardView.setCardBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseNumber;
        TextView tvCourseName;
        TextView tvEnrollmentCount;
        androidx.cardview.widget.CardView cardView;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseNumber = itemView.findViewById(R.id.tv_course_number);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            tvEnrollmentCount = itemView.findViewById(R.id.tv_enrollment_count);
            cardView = (androidx.cardview.widget.CardView) itemView;
        }
    }
}
