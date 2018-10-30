package com.coltennye.punctual.deadline.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Task;

import java.util.List;

public class TaskAdaptorEdit extends TaskAdaptor {

    private static class TaskViewHolder {
        public TextView text;
        public TextView duration;

        public TaskViewHolder(View itemView) {
            text = itemView.findViewById(R.id.task_text);
            duration = itemView.findViewById(R.id.task_minutes);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TaskViewHolder holder;
        if(view == null){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.task_edit_mode, viewGroup, false);
            holder = new TaskViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (TaskViewHolder) view.getTag();
        }

        ViewTask task = getItem(i);

        holder.text.setText(task.name);
        holder.duration.setText(task.duration + "m");

        return view;
    }

    @Override
    public void setTasks(List<Task> newTasks) {
        super.setTasks(newTasks);
        notifyDataSetChanged();
    }
}
