package com.coltennye.punctual.deadline.tasks;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Task;

public class TaskView extends LinearLayout {

    private long taskId;
    private TextView text;

    public TaskView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setLongClickable(true);
    }

    public void init(Task t, @Nullable OnClickListener taskOCL, @Nullable OnLongClickListener taskOLCL) {
        this.taskId = t.id;

        text = findViewById(R.id.task_text);
        text.setText(t.getName());

        if(taskOCL != null) setOnClickListener(taskOCL);
        if(taskOLCL != null) setOnLongClickListener(taskOLCL);
    }

    public void setStrike(boolean isStriked){
        if (isStriked){
            text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        else {
            text.setPaintFlags(text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    public long getTaskId() {
        return taskId;
    }

}
