package com.coltennye.punctual.deadline.tasks;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.views.MyListView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdaptor extends BaseAdapter {

    private int baseHeight;

    private MyListView listView;
    private int minDuration;
    private int minutesOnActiveTasks;
    private int minutesOnAllTasks;
    private int secondsUntilDue;
    private ViewTask divider;
    private View divView;
    private List<ViewTask> tasks;
    private List<ViewTask> doneTasks;


    public TaskAdaptor(Context context, MyListView listView){
        this.listView = listView;
        tasks = new ArrayList<>();
        doneTasks = new ArrayList<>();
        baseHeight =  context.getResources().getDimensionPixelSize(R.dimen.shortest_task_height);
        divider = new ViewTask(-1, "", 0, false);
    }

    private static class TaskViewHolder {

        public TextView text;
        public TextView duration;
        public boolean active;

        private TaskViewHolder(View itemView, boolean active) {
            text = itemView.findViewById(R.id.task_text);
            this.active = active;
            if(!active){
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }

    private View makeView(ViewGroup vg, boolean isActive) {
        View view = LayoutInflater.from(vg.getContext())
                .inflate(isActive ? R.layout.item_task : R.layout.item_task_done, vg, false);
        return view;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        int numActive = tasks.size();

        // divider
        if (i == numActive) {
            if (divView == null)
                divView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.task_list_divider, viewGroup, false);
            return divView;
        } else {

            boolean isActive = (i < numActive);
            TaskViewHolder holder = null;
            if (view != null) {
                holder = (TaskViewHolder) view.getTag();
                if (holder == null || (holder.active != isActive)) {
                    view = makeView(viewGroup, isActive);
                    holder = new TaskViewHolder(view, isActive);
                    view.setTag(holder);
                }
            }
            else{
                view = makeView(viewGroup, isActive);
                holder = new TaskViewHolder(view, isActive);
                view.setTag(holder);
            }

            ViewTask task = (isActive? tasks.get(i) : doneTasks.get(i - numActive - 1));

            String txt = task.name;

            // Active task
            if (isActive) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = ((task.duration * baseHeight) / minDuration);
                view.setLayoutParams(lp);

                /*
                // Falls entirely within remaining time
                if ((task.startTime + task.duration) <= (secondsUntilDue / 60)) {

                }
                // Task is entirely after remaining time
                else if (task.startTime > (secondsUntilDue / 60)) {

                }
                // Task is split
                else {
                    //view.setLineValues((secondsUntilDue / 60) + "m", (secondsUntilDue - (task.startTime * 60 )));
                }
                */
            }

            holder.text.setText(txt);

            return view;
        }
    }

    public void notifyTasksChanged(){
        // Get smallest task as base unit for layout heights
        minDuration = 1000;
        minutesOnAllTasks = 0;
        for (ViewTask t : tasks) {
            minutesOnAllTasks += t.duration;
            if (t.duration < minDuration)
                minDuration = t.duration;
        }
        for (ViewTask t : doneTasks) {
            minutesOnAllTasks += t.duration;
            if (t.duration < minDuration)
                minDuration = t.duration;
        }

        listView.setMinMinutes(minDuration);
    }

    public void setTasks(List<Task> newTasks) {

        this.tasks.clear();
        this.doneTasks.clear();
        for(Task t : newTasks){
            if(t.isCompleted())
                doneTasks.add(t.toViewTask());
            else{
                tasks.add(t.toViewTask());
            }
        }



        // get minutes left on tasks
        minutesOnActiveTasks = 0;
        for(ViewTask t : tasks){
            minutesOnActiveTasks += t.duration;
        }

        listView.setActiveMinutes(minutesOnActiveTasks);

        notifyDataSetChanged();
    }



    @Override
    public int getCount() {
        return tasks.size() + doneTasks.size() + 1;
    }

    @Override
    public ViewTask getItem(int i) {
        int size = tasks.size();
        if(i == size)
            return divider;
        else if(i < size)
            return tasks.get(i);
        else return  doneTasks.get(i - size - 1);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).id;
    }
}
