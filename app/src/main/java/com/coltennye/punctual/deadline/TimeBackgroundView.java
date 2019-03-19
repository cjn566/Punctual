package com.coltennye.punctual.deadline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.coltennye.punctual.R;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.deadline.DeadlineActivity;

import java.util.Calendar;
import java.util.Date;

public class TimeBackgroundView extends View {

    Paint mLinePaint;
    Paint mTxtPaint;
    Paint tickMarkPaint;

    String mLabel;
    boolean showLine = false;

    private float minuteTickWidth;
    private float mLineY;
    private float mTextPad;
    private float mStrokeWidth;
    private Picture ticksPicture;
    private float minMinutesHeightPx;

    private float[][] showOnMinimumPixelHeight = new float[2][5];

    private static final int TICK = 0;
    private static final int TIME = 1;
    private static final int MINUTE = 0;
    private static final int FIVE = 1;
    private static final int FIFTEEN = 2;
    private static final int THIRTY = 3;
    private static final int HOUR = 4;

    private float heightPerMinutePx;

    private DeadlineActivity dlActivity;


    public TimeBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        if(context.getClass() == DeadlineActivity.class){
            dlActivity = (DeadlineActivity) context;
        }

        Resources res = getResources();

        showOnMinimumPixelHeight[TICK][MINUTE] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_minute);
        showOnMinimumPixelHeight[TICK][FIVE] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_five);
        showOnMinimumPixelHeight[TICK][FIFTEEN] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_fifteen);
        showOnMinimumPixelHeight[TICK][THIRTY] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_thirty);

        showOnMinimumPixelHeight[TIME][MINUTE] = res.getDimensionPixelSize(R.dimen.time_min_height_break_minute);
        showOnMinimumPixelHeight[TIME][FIVE] = res.getDimensionPixelSize(R.dimen.time_min_height_break_five);
        showOnMinimumPixelHeight[TIME][FIFTEEN] = res.getDimensionPixelSize(R.dimen.time_min_height_break_fifteen);
        showOnMinimumPixelHeight[TIME][THIRTY] = res.getDimensionPixelSize(R.dimen.time_min_height_break_thirty);


        minMinutesHeightPx =    res.getDimensionPixelSize(R.dimen.shortest_task_height);
        mStrokeWidth =          res.getDimensionPixelSize(R.dimen.due_line_thickness);

        // Paint for tick marks
        tickMarkPaint = new Paint();
        tickMarkPaint.setColor(res.getColor(R.color.tick_marks));
        tickMarkPaint.setStrokeWidth(res.getDimensionPixelSize(R.dimen.tick_mark_thickness));

        // Paint for the remaining seconds line
        mLinePaint = new Paint();
        mLinePaint.setColor(Color.RED);
        mLinePaint.setStrokeWidth(mStrokeWidth);

        // Paint for the remaining minutes text
        mTxtPaint = new Paint();
        mTxtPaint.setColor(Color.BLACK);
        mTxtPaint.setTextSize(res.getDimensionPixelSize(R.dimen.min_rem_text_size));
        // mTxtPaint.setTextAlign(Paint.Align.CENTER);
        mTextPad = res.getDimensionPixelSize(R.dimen.min_rem_text_pad);

        // Ticks / clock stuff
        minuteTickWidth = res.getDimensionPixelSize(R.dimen.minute_tick_width);
//        fiveMinTickWidth = res.getDimensionPixelSize(R.dimen.five_min_tick_width);
//        fifteenTickWidth = res.getDimensionPixelSize(R.dimen.fifteen_tick_width);
//        hourTickWidth = res.getDimensionPixelSize(R.dimen.hour_tick_width);

        //Rect bounds = new Rect();
        //mTxtPaint.measureText("12:55 PM");
        //taskRightMargin = mTxtPaint.measureText("12:55 PM") + res.getDimensionPixelSize(R.dimen.min_rem_text_pad)*2;

    }

    public void setParams(int heightPerMinutePx, int totalMinutes, int currentTaskMinutes, long dueDate) {
        int w = getWidth();
        int h = heightPerMinutePx * totalMinutes;
        if (h == 0) return; // todo prevent the call in the first place?

        int showTickOnLevel = HOUR, showTimeOnLevel = HOUR;
        for(int i = THIRTY; i >= 0; i--){
            if (heightPerMinutePx > showOnMinimumPixelHeight[TICK][i]){
                showTickOnLevel = i;
            }
            if (heightPerMinutePx > showOnMinimumPixelHeight[TIME][i]){
                showTimeOnLevel = i;
            }
        }

        Calendar endTime = Calendar.getInstance();
        endTime.setTimeInMillis(dueDate);
        Calendar startTime = (Calendar) endTime.clone();
        startTime.add(Calendar.MINUTE, -(totalMinutes));

        ticksPicture = new Picture();
        Canvas canvas = ticksPicture.beginRecording(w, (int)h);
        canvas.drawColor(Color.GREEN);

        int minute, level;
        float drawY = 0, startX;
        for (; startTime.compareTo(endTime) <= 0; startTime.add(Calendar.MINUTE, 1)) {
            minute = startTime.get(Calendar.MINUTE);

            level = MINUTE;
            startX = w;
            if ((minute % 5) == 0) {
                level++;
                if ((minute % 15) == 0) {
                    level++;
                    if ((minute % 30) == 0) {
                        level++;
                        if ((minute % 60) == 0) {
                            level++;
                        }
                    }
                }
            }

            if (level >= showTickOnLevel) {
                /*switch (level){
                    case MINUTE:
                        tickMarkPaint.setStrokeWidth(1);
                        break;
                    case FIVE:
                        tickMarkPaint.setStrokeWidth(1);
                        break;
                    case FIFTEEN:
                        tickMarkPaint.setStrokeWidth(1);
                        break;
                    case THIRTY:
                        tickMarkPaint.setStrokeWidth(1);
                        break;
                    case HOUR:
                        tickMarkPaint.setStrokeWidth(1);
                        break;
                }*/
                tickMarkPaint.setStrokeWidth(level + 1);
                canvas.drawLine(0, drawY, w, drawY, tickMarkPaint);
            }
            if (level >= showTimeOnLevel) {
                String text = TimeConverter.timeOfDayString(startTime.getTime());
                Rect bounds = new Rect();
                mTxtPaint.getTextBounds(text, 0, text.length(), bounds);
                canvas.drawText(text, w - bounds.width(), drawY + (bounds.height() / 2), mTxtPaint);
            }
            drawY += heightPerMinutePx;
        }

        ticksPicture.endRecording();
        PictureDrawable pic = new PictureDrawable(ticksPicture);
        this.setBackground(pic);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if(ticksPicture != null) ticksPicture.draw(canvas);
        super.dispatchDraw(canvas);
        if(showLine){
            //    canvas.drawLine(0,mLineY, getWidth(), mLineY, mLinePaint);
            // canvas.drawText(mLabel, getWidth() - (mRightPad / 2), mLineY - (mTextPad  + (mStrokeWidth / 2)), mTxtPaint);
        }
    }
}
