package com.example.score_system.ui.course;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.model.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private List<Course> courseList;
    private Context context;
    private CourseClickListener listener;

    public interface CourseClickListener {
        void onEditClick(Course course);
        void onDeleteClick(Course course);
    }

    public CourseAdapter(List<Course> courseList, Context context, CourseClickListener listener) {
        this.courseList = courseList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.tvCourseNumber.setText("课程号: " + course.getCourseNumber());
        holder.tvCourseName.setText("课程名称: " + course.getCourseName());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(course);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(course);
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseNumber;
        TextView tvCourseName;
        ImageButton btnEdit;
        ImageButton btnDelete;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseNumber = itemView.findViewById(R.id.tv_course_number);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
