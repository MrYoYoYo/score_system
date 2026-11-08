package com.example.score_system.ui.score;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.score_system.R;
import com.example.score_system.data.DatabaseHelper;
import com.example.score_system.model.Score;

import java.util.List;

public class ScoreSummaryAdapter extends RecyclerView.Adapter<ScoreSummaryAdapter.ScoreViewHolder> {
    private List<Score> scoreList;
    private Context context;
    private DatabaseHelper dbHelper;
    private OnScoreChangedListener listener;

    public interface OnScoreChangedListener {
        void onScoreChanged();
    }

    public ScoreSummaryAdapter(List<Score> scoreList, Context context) {
        this.scoreList = scoreList;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    public void setOnScoreChangedListener(OnScoreChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_score_summary, parent, false);
        return new ScoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreViewHolder holder, int position) {
        Score score = scoreList.get(position);

        String typeStr = "";
        switch (score.getScoreType()) {
            case Score.TYPE_ATTENDANCE:
                typeStr = "考勤";
                break;
            case Score.TYPE_HOMEWORK:
                typeStr = "作业";
                break;
            case Score.TYPE_LAB:
                typeStr = "上机";
                break;
        }

        holder.tvScoreType.setText(typeStr + " 第" + score.getScoreIndex() + "次");
        holder.etScoreValue.setText(String.valueOf(score.getScoreValue()));
        holder.tvRemark.setText(score.getRemark() != null ? score.getRemark() : "");

        // 编辑按钮点击事件
        holder.btnEdit.setOnClickListener(v -> {
            holder.etScoreValue.setEnabled(true);
            holder.etScoreValue.requestFocus();
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnSave.setVisibility(View.VISIBLE);
        });

        // 保存按钮点击事件
        holder.btnSave.setOnClickListener(v -> {
            String valueStr = holder.etScoreValue.getText().toString().trim();
            if (TextUtils.isEmpty(valueStr)) {
                Toast.makeText(context, "请输入成绩", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double newValue = Double.parseDouble(valueStr);
                if (newValue < 0 || newValue > 10) {
                    Toast.makeText(context, "成绩必须在0-10之间", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 更新数据库
                score.setScoreValue(newValue);
                dbHelper.updateScore(score);

                holder.etScoreValue.setEnabled(false);
                holder.btnEdit.setVisibility(View.VISIBLE);
                holder.btnSave.setVisibility(View.GONE);

                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show();

                // 通知Activity刷新总评成绩
                if (listener != null) {
                    listener.onScoreChanged();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            }
        });

        // 删除按钮点击事件
        holder.btnDelete.setOnClickListener(v -> {
            dbHelper.deleteScore(score.getScoreId());
            scoreList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, scoreList.size());

            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();

            // 通知Activity刷新总评成绩
            if (listener != null) {
                listener.onScoreChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return scoreList.size();
    }

    static class ScoreViewHolder extends RecyclerView.ViewHolder {
        TextView tvScoreType;
        EditText etScoreValue;
        TextView tvRemark;
        Button btnEdit;
        Button btnSave;
        Button btnDelete;

        public ScoreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScoreType = itemView.findViewById(R.id.tv_score_type);
            etScoreValue = itemView.findViewById(R.id.et_score_value);
            tvRemark = itemView.findViewById(R.id.tv_remark);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnSave = itemView.findViewById(R.id.btn_save);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
