package com.coltennye.punctual.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.coltennye.punctual.db.Task;

import java.util.List;

public abstract class TaskListView extends LinearLayout {

    protected OnClickListener taskOCL;
    protected OnLongClickListener taskOLCL;
    protected LayoutInflater layoutInflater;

    public TaskListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setClickable(false);
        layoutInflater = LayoutInflater.from(context);
    }

    public void setItemListeners(ViewGroup.OnClickListener OCL, ViewGroup.OnLongClickListener OLCL){
        this.taskOCL = OCL;
        this.taskOLCL = OLCL;
    }


    abstract void  setTasks(List<Task> newTasks);

}
