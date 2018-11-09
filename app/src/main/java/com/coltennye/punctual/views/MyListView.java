package com.coltennye.punctual.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ListView;

import com.coltennye.punctual.R;

public class MyListView extends ListView {

    Paint mLinePaint;
    Paint mTxtPaint;
    String mLabel;
    boolean showLine = false;
    int mLinePosition;
    int mPositions;
    int mh;
    int mw;
    int mLineY;
    int mHeightPerPosition;
    int mTextPad;
    int mStrokeWidth;
    int mRightPad;
    Context mContext;


    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        Resources res = getResources();
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

        mPositions = 1;
    }

    public void setPositions(int positions){
        mPositions = positions;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mh = h;
        mw = w;
        mHeightPerPosition = (h / mPositions);
        setY();
    }

    public void noLine(){
        showLine = false;
    }

    public void setLineValues(String label, int linePosition){
        showLine = true;
        mLinePosition = linePosition;
        mLabel = label + "m";
        setY();
    }

    private void setY(){
        mLineY = (mLinePosition * mHeightPerPosition) - (mStrokeWidth / 2);
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        if(showLine){
            canvas.drawLine(0,mLineY, mw,mLineY,mLinePaint);
            canvas.drawText(mLabel, mw - (mRightPad / 2), mLineY - (mTextPad  + (mStrokeWidth / 2)), mTxtPaint);
        }



        super.dispatchDraw(canvas);
    }
}
