package com.coltennye.punctual.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskView;

import java.util.List;

public class ActiveTasksListView extends LinearLayout {


    Paint mLinePaint;
    Paint mTxtPaint;
    Paint tickMarkPaint;

    String mLabel;
    boolean showLine = false;
    private int maxSeconds;
    private int mCurrentTaskSeconds;
    private float pxInDivider;
    private float minMinutesHeightPx;
    private float heightPerMinutePx;
    private float heightPerSecondPx;
    private float mLineY;
    private float mTextPad;
    private float mStrokeWidth;
    private float mRightPad;
    private Picture ticksPicture;
    private OnClickListener OCL;
    private OnLongClickListener OLCL;
    protected LayoutInflater inflater;
    private int dueMinute;

    public ActiveTasksListView(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);

        inflater = LayoutInflater.from(context);
        this.setOrientation(VERTICAL);

        Resources res = getResources();
        minMinutesHeightPx =   res.getDimensionPixelSize(R.dimen.shortest_task_height);
        mRightPad =             res.getDimensionPixelSize(R.dimen.task_right_pad);
        mStrokeWidth =          res.getDimensionPixelSize(R.dimen.due_line_thickness);
        pxInDivider =           res.getDimensionPixelSize(R.dimen.divider_height);

        // Paint for the remaining seconds line
        mLinePaint = new Paint();
        mLinePaint.setColor(Color.RED);
        mLinePaint.setStrokeWidth(mStrokeWidth);

        // Paint for the remaining minutes text
        mTxtPaint = new Paint();
        mTxtPaint.setColor(Color.BLACK);
        mTxtPaint.setTextSize(res.getDimensionPixelSize(R.dimen.min_rem_text_size));
        mTxtPaint.setTextAlign(Paint.Align.CENTER);
        mTextPad = res.getDimensionPixelSize(R.dimen.min_rem_text_pad);

        tickMarkPaint = new Paint();
        tickMarkPaint.setColor(Color.BLACK);
        tickMarkPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics()));
    }

    public void setListeners(OnClickListener OCL, OnLongClickListener OLCL){
        this.OCL = OCL;
        this.OLCL = OLCL;
    }

    public void addTasks(List<Task> tasks){
        removeAllViews();

        if(tasks.size() <= 0) return;
        int minDuration = 1000, d, activeDuration = 0;
        mCurrentTaskSeconds = (tasks.get(tasks.size() - 1).getDuration() * 60);

        for(Task t : tasks) {
            d = t.getDuration();
            activeDuration += d;
            if (d < minDuration) {
                minDuration = d;
            }
        }

        heightPerMinutePx = minMinutesHeightPx / minDuration;
        heightPerSecondPx = heightPerMinutePx / 60;
        maxSeconds = (int)((activeDuration + (pxInDivider / heightPerMinutePx)) * 60);

        for(Task t : tasks) {
            TaskView child = (TaskView) inflater.inflate(R.layout.item_active_task, this , false);
            child.init(t, OCL, OLCL);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            lp.height = (int) (t.getDuration() * heightPerMinutePx);
            child.setLayoutParams(lp);
            child.setVisibility(t.isCompleted()? GONE : VISIBLE);
            addView(child);
        }

        requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        makeTicksPicture(h, w);
    }

    private void makeTicksPicture(int h, int w){

        float heightPerFiveMinutes = (heightPerMinutePx * 5);
        int lineWidth = 25;

        int hour = (dueMinute / 60) % 12;
        int minutesFrom5 = (dueMinute % 5);
        int numberOf5minutesFromNearestHour = ((dueMinute - minutesFrom5) % 60) / 5;
        int numberOfFiveMinuteIncrements = (int)(h / heightPerFiveMinutes) - 1;
        int indexStart = (12 - numberOf5minutesFromNearestHour);
        int indexTarget = indexStart + numberOfFiveMinuteIncrements;


        float drawY = minutesFrom5 * heightPerMinutePx;

        ticksPicture = new Picture();
        Canvas canvas = ticksPicture.beginRecording(w, h);

        for(int i = indexStart; i < indexTarget; i++){
            // Hour
            if((i%12) == 0){
                canvas.drawLine(w - (lineWidth * 3), drawY, w, drawY, tickMarkPaint);

                String text = hour + ":00";
                Rect bounds = new Rect();
                mTxtPaint.getTextBounds(text, 0, text.length(), bounds);
                canvas.drawText(text, w - (lineWidth * 3) - bounds.width() - 5, drawY + (bounds.height() / 2), mTxtPaint);
                hour = (hour - 1) % 12;
            }

            // 15 Minute
            else if((i%3) == 0){
                canvas.drawLine(w - (lineWidth * 2), drawY, w, drawY, tickMarkPaint);
            }

            else {
                canvas.drawLine(w - lineWidth, drawY, w, drawY, tickMarkPaint);
            }

            drawY += heightPerFiveMinutes;
        }

        ticksPicture.endRecording();
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
    }

    public void noLine(){
        this.showLine = false;
    }

    public void setSeconds(int seconds){
        showLine = (seconds < maxSeconds);
        if (!showLine) return;
        mLabel = (seconds / 60) + "m";
        mLineY = (seconds * heightPerSecondPx) - (mStrokeWidth / 2);
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        ticksPicture.draw(canvas);

        super.dispatchDraw(canvas);

        if(showLine){
            canvas.drawLine(0,mLineY, getWidth(), mLineY, mLinePaint);
            //canvas.drawText(mLabel, getWidth() - (mRightPad / 2), mLineY - (mTextPad  + (mStrokeWidth / 2)), mTxtPaint);
        }
    }

    public void setDueMinute(int minute) {
        this.dueMinute = minute;
        this.makeTicksPicture(getHeight(), getWidth());
    }
}
