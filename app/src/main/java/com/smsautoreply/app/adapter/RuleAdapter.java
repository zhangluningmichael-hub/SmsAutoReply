package com.smsautoreply.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smsautoreply.app.R;
import com.smsautoreply.app.db.RuleEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则列表适配器 - 纯原生组件版
 */
public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.RuleViewHolder> {

    private List<RuleEntity> rules = new ArrayList<>();
    private final Context context;
    private OnItemClickListener onItemClickListener;
    private OnItemToggleListener onItemToggleListener;
    private OnItemDeleteListener onItemDeleteListener;

    public RuleAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_rule, parent, false);
        return new RuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        RuleEntity rule = rules.get(position);
        holder.bind(rule, position);
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    public void setRules(List<RuleEntity> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<RuleEntity> getRules() {
        return rules;
    }

    public RuleEntity getItem(int position) {
        if (position >= 0 && position < rules.size()) {
            return rules.get(position);
        }
        return null;
    }

    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(rules, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    // === 接口 ===
    public interface OnItemClickListener {
        void onItemClick(RuleEntity rule, int position);
    }
    public interface OnItemToggleListener {
        void onItemToggle(RuleEntity rule, boolean enabled, int position);
    }
    public interface OnItemDeleteListener {
        void onItemDelete(RuleEntity rule, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    public void setOnItemToggleListener(OnItemToggleListener listener) {
        this.onItemToggleListener = listener;
    }
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onItemDeleteListener = listener;
    }

    // === ViewHolder ===
    class RuleViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvDescription;
        private final Switch switchEnabled;
        private final ImageButton btnDelete;

        RuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_rule_name);
            tvDescription = itemView.findViewById(R.id.tv_rule_description);
            switchEnabled = itemView.findViewById(R.id.switch_rule_enabled);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(RuleEntity rule, int position) {
            tvName.setText(rule.getName() != null ? rule.getName() : "未命名规则");

            StringBuilder desc = new StringBuilder();
            if (rule.getKeyword() != null && !rule.getKeyword().isEmpty()) {
                desc.append("关键词: ").append(rule.getKeyword()).append("  ");
            }
            if (rule.getSenderPattern() != null && !rule.getSenderPattern().isEmpty()) {
                desc.append("号码: ").append(rule.getSenderPattern()).append("  ");
            }
            String actionStr = "回复";
            if (rule.getAction() != null) {
                switch (rule.getAction()) {
                    case "forward": actionStr = "转发"; break;
                    case "both": actionStr = "回复+转发"; break;
                }
            }
            desc.append("动作: ").append(actionStr);
            tvDescription.setText(desc.toString());

            switchEnabled.setChecked(rule.isEnabled());
            switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (onItemToggleListener != null) {
                    onItemToggleListener.onItemToggle(rule, isChecked, position);
                }
            });

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(rule, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (onItemDeleteListener != null) {
                    onItemDeleteListener.onItemDelete(rule, position);
                }
            });
        }
    }
}
