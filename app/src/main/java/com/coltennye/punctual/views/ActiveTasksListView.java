package com.coltennye.punctual.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskView;

import java.util.ArrayList;
import java.util.List;

public class ActiveTasksListView extends LinearLayout {


    private MyListView listView;
    private int minDuration;
    private int minutesOnActiveTasks;
    private int minutesOnAllTasks;
    private int secondsUntilDue;
    Paint mLinePaint;
    Paint mTxtPaint;
    String mLabel;
    boolean showLine = false;
    int mSeconds;
    int mMinMinutesHeightPx;
    int mCurrentTaskSeconds;
    int mActiveMinutes;
    int mMaxSeconds;
    int mSecondsInDivider;
    int mPxInDivider;
    private int baseHeight;
    int mh;
    int mw;
    float mLineY;
    float mHeightPerSecondPx;
    int mTextPad;
    int mStrokeWidth;
    int mRightPad;
    Context mContext;
    private View divView;

    private List<TaskView> tasks;

    private OnClickListener taskOCL;

    public ActiveTasksListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOrientation(VERTICAL);

        divView = LayoutInflater.from(context).inflate(R.layout.task_list_divider, this, false);

        tasks = new ArrayList<>();
        baseHeight =  context.getResources().getDimensionPixelSize(R.dimen.shortest_task_height);

        Resources res = getResources();
        mMinMinutesHeightPx = res.getDimensionPixelSize(R.dimen.shortest_task_height);
        mPxInDivider = res.getDimensionPixelSize(R.dimen.divider_height);
        mRightPad = res.getDimensionPixelSize(R.dimen.task_right_pad);
        mStrokeWidth = res.getDimensionPixelSize(R.dimen.due_line_thickness);

        mLinePaint = new Paint();
        mLinePaint.setColor(Color.RED);
        mLinePaint.setStrokeWidth(mStrokeWidth);

        mTxtPaint = new Paint();
        mTxtPaint.setColor(Color.BLACK);
        mTxtPaint.setTextSize(res.getDimensionPixelSize(R.dimen.min_rem_text_size));
        mTxtPaint.setTextAlign(Paint.Align.CENTER);
        mTextPad = res.getDimensionPixelSize(R.dimen.min_rem_text_pad);
    }

    public void setOnItemClickListener(View.OnClickListener onItemClickListener) {
        taskOCL = onItemClickListener;
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

    public void notifyTasksChanged(){
        // Get smallest task as base unit for layout heights
        minDuration = 1000;
        minutesOnAllTasks = 0;
        for (TaskView t : tasks) {
            minutesOnAllTasks += t.duration;
            if (t.duration < minDuration)
                minDuration = t.duration;
        }


        mHeightPerSecondPx = mMinMinutesHeightPx / (minDuration * 60);
        mSecondsInDivider = (int)(mPxInDivider / mHeightPerSecondPx);
    }

    public void makeViews(){
        this.removeAllViews();

        for (TaskView t : tasks){
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_task, this, false);
            ViewGroup.LayoutParams lp =  v.getLayoutParams();
            lp.height = (t.duration / minDuration) * mMinMinutesHeightPx;
            v.setLayoutParams(lp);
            v.setOnClickListener(taskOCL);
            this.addView(v);
        }
        addView(divView);
        this.requestLayout();
    }

    public void setTasks(List<Task> newTasks) {

        this.tasks.clear();
        for(Task t : newTasks){
            tasks.add(t.toViewTask());
        }

        // get minutes left on tasks
        minutesOnActiveTasks = 0;
        for(TaskView t : tasks){
            minutesOnActiveTasks += t.duration;
        }


        mCurrentTaskSeconds = (minutesOnActiveTasks * 60);
        mMaxSeconds = mCurrentTaskSeconds + mSecondsInDivider;

        //makeViews();
    }

    public void noLine(){
        this.showLine = false;
    }

    public void setSeconds(int seconds){
        showLine = (seconds <= mMaxSeconds);
        if (!showLine) return;
        mSeconds = seconds;
        mLabel = (seconds / 60) + "m";
        mLineY = (mSeconds * mHeightPerSecondPx) - (mStrokeWidth / 2);
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if(showLine){
            canvas.drawLine(0,mLineY, getWidth(), mLineY, mLinePaint);
            canvas.drawText(mLabel, getWidth() - (mRightPad / 2), mLineY - (mTextPad  + (mStrokeWidth / 2)), mTxtPaint);
        }
        super.dispatchDraw(canvas);
    }
}
