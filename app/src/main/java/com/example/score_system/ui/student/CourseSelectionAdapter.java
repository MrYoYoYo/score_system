package com.example.score_system.ui.student;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.model.Course;

import java.util.ArrayList;
import java.util.List;

public class CourseSelectionAdapter extends RecyclerView.Adapter<CourseSelectionAdapter.CourseViewHolder> {
    private List<Course> courseList;
    private List<Integer> selectedCourseIds;
    private Context context;

    public CourseSelectionAdapter(List<Course> courseList, List<Integer> selectedCourseIds, Context context) {
        this.courseList = courseList;
        this.selectedCourseIds = selectedCourseIds != null ? selectedCourseIds : new ArrayList<>();
        this.context = context;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course_checkbox, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.tvCourseName.setText(course.getCourseName());

        // 设置选中状态
        holder.checkBox.setChecked(selectedCourseIds.contains(course.getCourseId()));

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedCourseIds.contains(course.getCourseId())) {
                    selectedCourseIds.add(course.getCourseId());
                }
            } else {
                selectedCourseIds.remove(Integer.valueOf(course.getCourseId()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public List<Integer> getSelectedCourseIds() {
        return selectedCourseIds;
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName;
        CheckBox checkBox;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            checkBox = itemView.findViewById(R.id.checkbox_course);
        }
    }
}
