package com.coltennye.punctual.deadline.tasks;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.views.TaskView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskAdaptor extends BaseAdapter {

    private final int BASE_HEIGHT = 80;

    private int minDuration;
    private int minutesOnActiveTasks;
    private int minutesOnAllTasks;
    private int secondsUntilDue;
    protected List<ViewTask> tasks;

    public TaskAdaptor(){
        tasks = new ArrayList<>();
    }

    private static class TaskViewHolder {
        public TextView text;
        public TextView duration;
        private TaskViewHolder(View itemView) {
            text = itemView.findViewById(R.id.task_text);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TaskView mView = (TaskView) view;
        TaskViewHolder holder;
        ViewTask task = getItem(i);

        if(view == null){
            mView = (TaskView)LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_task, viewGroup, false);
            mView.setPositions(task.duration * 60);

            holder = new TaskViewHolder(mView);
            mView.setTag(holder);
        } else {
            holder = (TaskViewHolder) mView.getTag();
        }


        String txt = task.name;
        ViewGroup.LayoutParams lp =  mView.getLayoutParams();

        mView.noLine();

        // Active task
        if(!task.completed){
            lp.height = ((task.duration * BASE_HEIGHT)/ minDuration); //todo: find the base height
            txt += " (" + TimeConverter.timeRemainingString(task.duration) + ")";
            holder.text.setPaintFlags(holder.text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

            // Falls entirely within remaining time
            if((task.startTime + task.duration) <= (secondsUntilDue / 60)){

            }
            // Task is entirely after remaining time
            else if(task.startTime > (secondsUntilDue / 60)) {

            }
            // Task is split
            else {
                mView.setLineValues((secondsUntilDue / 60) + "m", (secondsUntilDue - (task.startTime * 60 )));
            }
        }
        // Completed task
        else {
            lp.height = BASE_HEIGHT;
            holder.text.setPaintFlags(holder.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            //view.setBackgroundColor(Color.LTGRAY);

        }

        mView.setLayoutParams(lp);
        holder.text.setText(txt);

        return mView;
    }

    // Todo: update single view on line change
    // Todo: Show more info

    public void updateTimeTillDue(int secondsUntilDue){
        this.secondsUntilDue = secondsUntilDue;
        notifyDataSetChanged(); //Todo: update single view
    }

    public void setTasks(List<Task> newTasks) {

        this.tasks.clear();
        for(Task t : newTasks){
            tasks.add(t.toViewTask());
        }

        minutesOnActiveTasks = 0;
        minutesOnAllTasks = 0;
        minDuration = 1000;

        // Get smallest task as base unit for layout heights
        for (ViewTask t : tasks) {
            if (t.duration < minDuration)
                minDuration = t.duration;
        }


        // Sort tasks by completion
        Collections.sort(tasks);


        // get minutes left on tasks
        for(ViewTask t : tasks){
            t.startTime = minutesOnAllTasks;
            minutesOnAllTasks += t.duration;
            if(!t.completed)
                minutesOnActiveTasks += t.duration;
        }

        notifyDataSetChanged();
    }



    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public ViewTask getItem(int i) {
        return tasks.get(i);
    }

    @Override
    public long getItemId(int i) {
        return tasks.get(i).id;
    }
}
