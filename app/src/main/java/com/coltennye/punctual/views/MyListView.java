package com.coltennye.punctual.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ListView;

import com.coltennye.punctual.R;

public class MyListView extends ListView {

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
    int mh;
    int mw;
    float mLineY;
    float mHeightPerSecondPx;
    int mTextPad;
    int mStrokeWidth;
    int mRightPad;
    Context mContext;


    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

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

        this.setDivider(null);
        this.setDividerHeight(0);

        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                Log.d("BOOOP", "State Changed: " + i);
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                Log.d("BOOOP", "scroll: " + getScrollY());
            }
        });
    }

    public void setMinMinutes(int minutes){
        mHeightPerSecondPx = mMinMinutesHeightPx / ( minutes * 60 );
        mSecondsInDivider = (int)(mPxInDivider / mHeightPerSecondPx);
    }

    public void setActiveMinutes(int minutes){
        mMaxSeconds = (minutes * 60) + mSecondsInDivider;
    }

    public void setCurrentTaskMinutes(int minutes){
        mCurrentTaskSeconds = (minutes * 60);
    }

/*
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mh = h;
        mw = w;
        mHeightPerSecondPx = (h / mActiveMinutes);
        setY();
    }
*/
    public void noLine(){
        showLine = false;
    }

    public void setSeconds(int seconds){
        showLine = (seconds <= mMaxSeconds);
        if (!showLine) return;
        mSeconds = seconds;
        mLabel = (seconds / 60) + "m";
        setY();
    }

    // Todo: Maybe merge these ^v
    private void setY(){
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
