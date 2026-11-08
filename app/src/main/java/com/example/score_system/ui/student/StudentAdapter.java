package com.example.score_system.ui.student;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.model.Student;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<Student> studentList;
    private Context context;
    private StudentClickListener listener;

    public interface StudentClickListener {
        void onEditClick(Student student);
        void onDeleteClick(Student student);
    }

    public StudentAdapter(List<Student> studentList, Context context, StudentClickListener listener) {
        this.studentList = studentList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.tvStudentNumber.setText("学号: " + student.getStudentNumber());
        holder.tvStudentName.setText("姓名: " + student.getStudentName());
        holder.tvClassName.setText("班级: " + student.getClassName());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(student);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(student);
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentNumber;
        TextView tvStudentName;
        TextView tvClassName;
        ImageButton btnEdit;
        ImageButton btnDelete;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentNumber = itemView.findViewById(R.id.tv_student_number);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvClassName = itemView.findViewById(R.id.tv_class_name);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
