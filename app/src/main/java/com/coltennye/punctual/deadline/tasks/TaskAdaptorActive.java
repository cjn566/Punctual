package com.coltennye.punctual.deadline.tasks;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class TaskAdaptorActive extends TaskAdaptor {

    private final int clrActiveOnTime = 0x6d126c2c;

    private int minDuration;
    private int minutesOnActiveTasks;
    private int minutesOnAllTasks;
    private int minutesUntilDue;

    private static class TaskViewHolder {
        public TextView text;
        public TaskViewHolder(View itemView) {
            text = itemView.findViewById(R.id.task_text);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TaskViewHolder holder;
        if(view == null){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.task_active_mode, viewGroup, false);
            holder = new TaskViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (TaskViewHolder) view.getTag();
        }

        ViewTask task = getItem(i);
        holder.text.setText(task.name + " (" + TimeConverter.timeRemainingString(task.duration) + ")");

        ViewGroup.LayoutParams lp =  view.getLayoutParams();
        lp.height = ((task.duration *150 )/ minDuration); //todo: find the base height
        view.setLayoutParams(lp);




        // Active task
        if(!task.completed){
            holder.text.setPaintFlags(holder.text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

            // Falls entirely within remaining time
            if((task.startTime + task.duration) < minutesUntilDue){
                view.setBackgroundColor((minutesUntilDue > minutesOnActiveTasks)? R.color.taskActiveOnTime : Color.argb(125, 255,0,0));
            }
            // Task is entirely after remaining time
            else if(task.startTime > minutesUntilDue) {
                view.setBackgroundColor(Color.RED);
            }
            // Task is split
            else {
                int topHalf = (lp.height * (minutesUntilDue - task.startTime)) / task.duration;
                ShapeDrawable blorp = new ShapeDrawable(new RectShape());
                blorp.getPaint().setShader(new LinearGradient(0,topHalf,0,topHalf+1, Color.argb(125, 255,0,0), Color.RED, Shader.TileMode.CLAMP));
                view.setBackground(blorp);
            }
        }
        // Completed task
        else {
            holder.text.setPaintFlags(holder.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Falls entirely within remaining time
            if((task.startTime + task.duration) < minutesUntilDue){
                view.setBackgroundColor(Color.DKGRAY);
            }
            // Task is entirely after remaining time
            else if(task.startTime > minutesUntilDue) {
                view.setBackgroundColor(Color.LTGRAY);
            }
            // Task is split
            else {
                int topHalf = (lp.height * (minutesUntilDue - task.startTime)) / task.duration;
                ShapeDrawable blorp = new ShapeDrawable(new RectShape());
                blorp.getPaint().setShader(new LinearGradient(0,topHalf,0,topHalf+1, Color.DKGRAY, Color.LTGRAY, Shader.TileMode.CLAMP));
                view.setBackground(blorp);
            }
        }

        return view;
    }

    public void updateTimeTillDue(int minutesTillDue){
        this.minutesUntilDue = minutesTillDue;
        notifyDataSetChanged();
    }

    @Override
    public void setTasks(List<Task> newTasks) {
        super.setTasks(newTasks);

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



}
