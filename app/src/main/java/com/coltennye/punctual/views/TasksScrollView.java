package com.coltennye.punctual.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.DeadlineActivity;
import com.coltennye.punctual.deadline.tasks.TaskView;

import java.util.List;

public class TasksScrollView extends ScrollView {
    private OnClickListener activeToCompleteOCL;
    private OnClickListener completeToActiveOCL;
    private OnLongClickListener editOLCL;
    private ActiveTasksListView activeTasks;
    private LinearLayout doneTasks;
    protected LayoutInflater inflater;
    private DeadlineActivity context;

    public TasksScrollView(final Context context, AttributeSet attrs) {
        super(context, attrs);


        inflater = LayoutInflater.from(context);
        this.context = (DeadlineActivity) context;


    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();


        activeTasks = findViewById(R.id.lv_tasks_active);
        doneTasks = findViewById(R.id.lv_tasks_done);



        activeToCompleteOCL = new OnClickListener() {
            @Override
            public void onClick(View view) {
                context.setTaskCompletionState(((TaskView)view).getTaskId(), true);
                doneTasks.getChildAt(activeTasks.indexOfChild(view)).setVisibility(VISIBLE);
                view.setVisibility(GONE);
            }
        };

        completeToActiveOCL = new OnClickListener() {
            @Override
            public void onClick(View view) {
                context.setTaskCompletionState(((TaskView)view).getTaskId(), false);
                activeTasks.getChildAt(doneTasks.indexOfChild(view)).setVisibility(VISIBLE);
                view.setVisibility(GONE);
            }
        };

        editOLCL = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return context.editTask(((TaskView)view).getTaskId());
            }
        };

        activeTasks.setListeners(context, activeToCompleteOCL, editOLCL);

    }

    public void setTasks(List<Task> newTasks){

        activeTasks.addTasks(newTasks);


        doneTasks.removeAllViews();
        for(Task t : newTasks) {
            TaskView child = (TaskView) inflater.inflate(R.layout.item_done_task, doneTasks, false);
            child.init(t, completeToActiveOCL, null);
            child.setStrike(true);
            child.setVisibility(t.isCompleted()? VISIBLE : GONE);
            doneTasks.addView(child);
        }


    }
}
