package com.coltennye.punctual.deadline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
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

        int resID;
        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 5; j++){
                resID = (i==TICK)?
                        (j==MINUTE?     R.dimen.tick_min_height_break_minute :
                                j==FIVE?       R.dimen.tick_min_height_break_five:
                                        j==FIFTEEN?    R.dimen.tick_min_height_break_fifteen :
                                                j==THIRTY?     R.dimen.tick_min_height_break_thirty :
                                                        R.dimen.tick_min_height_break_hour
                        ):
                        (j==MINUTE?     R.dimen.time_min_height_break_minute :
                                j==FIVE?       R.dimen.time_min_height_break_five:
                                        j==FIFTEEN?    R.dimen.time_min_height_break_fifteen :
                                                j==THIRTY?     R.dimen.time_min_height_break_thirty :
                                                        R.dimen.time_min_height_break_hour);

                showOnMinimumPixelHeight[i][j] = res.getDimensionPixelSize(resID);
            }
        }

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

    public void setParams(float heightPerMinutePx, int totalMinutes, int currentTaskMinutes, long dueDate) {
        this.heightPerMinutePx = heightPerMinutePx;
        int h = getHeight();
        int w = getWidth();
        if (h == 0) return; // todo prevent the call in the first place?

        int showTickOnLevel =
                heightPerMinutePx > showOnMinimumPixelHeight[TICK][MINUTE] ? MINUTE :
                        heightPerMinutePx > showOnMinimumPixelHeight[TICK][FIVE] ? FIVE :
                                heightPerMinutePx > showOnMinimumPixelHeight[TICK][FIFTEEN] ? FIFTEEN :
                                        heightPerMinutePx > showOnMinimumPixelHeight[TICK][THIRTY] ? THIRTY : HOUR;

        int showTimeOnLevel =
                heightPerMinutePx > showOnMinimumPixelHeight[TIME][MINUTE] ? MINUTE :
                        heightPerMinutePx > showOnMinimumPixelHeight[TIME][FIVE] ? FIVE :
                                heightPerMinutePx > showOnMinimumPixelHeight[TIME][FIFTEEN] ? FIFTEEN :
                                        heightPerMinutePx > showOnMinimumPixelHeight[TIME][THIRTY] ? THIRTY : HOUR;

        float drawY = 0, startX;
        int level;

        Calendar endTime = Calendar.getInstance();
        endTime.setTimeInMillis(dueDate);
        Calendar startTime = (Calendar) endTime.clone();
        startTime.add(Calendar.MINUTE, -(totalMinutes));
        int minute;

        ticksPicture = new Picture();
        Canvas canvas = ticksPicture.beginRecording(w, h);

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
