package com.coltennye.punctual.deadline.tasks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coltennye.punctual.db.Task;

import java.util.ArrayList;
import java.util.List;

public abstract class TaskAdaptor extends BaseAdapter {

    protected List<ViewTask> tasks;
    protected Context mContext;


    public TaskAdaptor() {
        this.tasks = new ArrayList<>();
    }

    public TaskAdaptor(Context context) {
        this.tasks = new ArrayList<>();
        mContext = context;
    }

    public void setTasks(List<Task> newTasks){
        this.tasks.clear();
        for(Task t : newTasks){
            tasks.add(t.toViewTask());
        }
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
