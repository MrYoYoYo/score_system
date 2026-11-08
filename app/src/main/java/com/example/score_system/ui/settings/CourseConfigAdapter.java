package com.example.score_system.ui.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.model.Course;

import java.util.List;

public class CourseConfigAdapter extends RecyclerView.Adapter<CourseConfigAdapter.CourseViewHolder> {
    private List<Course> courseList;
    private Context context;
    private OnCourseClickListener listener;
    
    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }
    
    public CourseConfigAdapter(List<Course> courseList, Context context, OnCourseClickListener listener) {
        this.courseList = courseList;
        this.context = context;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course_config, parent, false);
        return new CourseViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.tvCourseName.setText(course.getCourseName());
        holder.btnConfig.setOnClickListener(v -> {
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
        TextView tvCourseName;
        Button btnConfig;
        
        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            btnConfig = itemView.findViewById(R.id.btn_config);
        }
    }
}
