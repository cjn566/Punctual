package com.coltennye.punctual.deadline;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.coltennye.punctual.App;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskView;
import com.coltennye.punctual.main.MainActivity;
import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Deadline;
import com.coltennye.punctual.db.Deadline_;

import java.util.Calendar;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;
import mobi.upod.timedurationpicker.TimeDurationPicker;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class DeadlineActivity extends AppCompatActivity {

    private final int REFRESH_UI_DELAY = 5 * 1000;
    private Handler intervalTimer;
    private Runnable intervalMethod;

    private long dueDateMillis;

    private TextView tv_dueTime;
    private TextView tv_deadlineName;
    private LinearLayout activeTasks;
    private LinearLayout doneTasks;
    private View backGroundView;

    private Deadline deadline;
    private Box<Task> taskBox;
    private Box<Deadline> deadlineBox;
    private Query<Deadline> deadlineQuery;

    private Toolbar toolBar;
    private MenuItem resetTasksMenuItem;
    private MenuItem editDeadlineMenuItem;

    private View.OnClickListener activeToCompleteOCL;
    private View.OnClickListener completeToActiveOCL;
    private View.OnLongClickListener editOLCL;

    private float heightPerMinutePx;
    private int activeMinutes;
    private int totalMinutes;
    private int currentTaskMinutes;
    private float taskLeftMargin;
    private float minMinutesHeightPx;

    private Paint mLinePaint;
    private Paint mTxtPaint;
    private Paint tickMarkPaint;

    private float minuteTickWidth;
    private float mLineY;
    private float mTextPad;
    private float mStrokeWidth;
    private int beforeListPadding;
    private int afterListPadding;
    private int activeMinutesBackgroundWasDrawnAt;
    private int screenheight;
    private int screenwidth;
    private int fullBackgroundHeight;
    private PictureDrawable fullBackground;
    private ClipDrawable clippedBackground;


    private float[][] showOnMinimumPixelHeight = new float[2][5];

    private static final int TICK = 0;
    private static final int TIME = 1;
    private static final int MINUTE = 0;
    private static final int FIVE = 1;
    private static final int FIFTEEN = 2;
    private static final int THIRTY = 3;
    private static final int HOUR = 4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deadline);

        Intent intent = getIntent();
        long deadlineId = intent.getLongExtra(MainActivity.EXTRA_DEADLINE_ID, 0);
        BoxStore boxStore = ((App) this.getApplication()).getBoxStore();
        taskBox = boxStore.boxFor(com.coltennye.punctual.db.Task.class);
        deadlineBox = boxStore.boxFor(com.coltennye.punctual.db.Deadline.class);
        deadlineQuery = deadlineBox.query().equal(Deadline_.__ID_PROPERTY, deadlineId).build();

        toolBar = findViewById(R.id.toolbar_deadline);
        activeTasks = findViewById(R.id.lv_tasks_active);
        doneTasks = findViewById(R.id.lv_tasks_done);
        backGroundView = findViewById(R.id.time_background);
        tv_deadlineName = findViewById(R.id.deadline_name);
        tv_dueTime = findViewById(R.id.due_time);

        setSupportActionBar(toolBar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        initializeBackground();
        clippedBackground = new ClipDrawable(new ColorDrawable(Color.RED), Gravity.BOTTOM, ClipDrawable.VERTICAL);
        backGroundView.setBackground(clippedBackground);

        intervalTimer = new Handler();
        intervalMethod = new Runnable() {
            @Override
            public void run() {
                updateTime();
                intervalTimer.postDelayed(this, REFRESH_UI_DELAY);
            }
        };


        activeToCompleteOCL =new View.OnClickListener(){
            @Override
            public void onClick (View view){
                activeMinutes -= setTaskCompletionState(((TaskView) view).getTaskId(), true);
                checkBackgroundRatio();
                doneTasks.getChildAt(activeTasks.indexOfChild(view)).setVisibility(VISIBLE);
                view.setVisibility(GONE);
            }
        };

        completeToActiveOCL =new View.OnClickListener()
        {
            @Override
            public void onClick (View view){
                activeMinutes += setTaskCompletionState(((TaskView) view).getTaskId(), false);
                checkBackgroundRatio();
                activeTasks.getChildAt(doneTasks.indexOfChild(view)).setVisibility(VISIBLE);
                view.setVisibility(GONE);
            }
        };

        editOLCL =new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick (View view){
                editTask(((TaskView) view).getTaskId());
                return true;
            }
        };

        minMinutesHeightPx = getResources().getDimensionPixelSize(R.dimen.shortest_task_height);

        updateTasksInViews();

        tv_deadlineName.setText(deadline.getName());

        // Set new task FAB listener
        findViewById(R.id.newTaskFAB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });

        tv_dueTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(DeadlineActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        deadline.setHour(selectedHour);
                        deadline.setMinute(selectedMinute);
                        deadlineBox.put(deadline);
                        setDueTime();
                    }
                }, deadline.getHour(), deadline.getMinute(), TimeConverter.is24hr());
                mTimePicker.setTitle("Deadline Due Time:");
                mTimePicker.show();
            }
        });
        setDueTime();
    }

    @Override //TODO unfuck this
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.deadline_menu, menu);

        resetTasksMenuItem = menu.findItem(R.id.reset_tasks);
        editDeadlineMenuItem = menu.findItem(R.id.edit_deadline);
        Switch activeTogglerActionView = menu.findItem(R.id.menu_deadline_active_switch).getActionView().findViewById(R.id.active_switch);
        if (deadline.isActive()) {
            editDeadlineMenuItem.setVisible(false);
            activeTogglerActionView.setChecked(true);
        } else {
            resetTasksMenuItem.setVisible(false);
        }
        activeTogglerActionView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                toggleActive(b);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
            case R.id.reset_tasks:
                resetTasks();
                break;
            case R.id.edit_deadline:

                break;
        }
        return true;
    }

    //Todo: move to main activity
    private void deleteDeadline() {


        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:

                        BoxStore boxStore = ((App) getApplication()).getBoxStore();
                        Box<Task> taskBox = boxStore.boxFor(com.coltennye.punctual.db.Task.class);
                        taskBox.remove(deadline.tasks);

                        deadlineBox.remove(deadline.id);
                        goBack();

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }


    private void setDueTime() {
        int dueMinute = deadline.getMinute();
        int dueHour = deadline.getHour();
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        c.clear(Calendar.MINUTE);
        c.clear(Calendar.HOUR);
        c.set(Calendar.HOUR_OF_DAY, dueHour);
        c.set(Calendar.MINUTE, dueMinute);
        if (now > c.getTimeInMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        tv_dueTime.setText(TimeConverter.timeOfDayString(c.getTime()));
        dueDateMillis = c.getTimeInMillis();
        updateTime();
        drawBackground();
    }

    private void updateTime() {
        int minutesRemaining = (int) ((dueDateMillis - System.currentTimeMillis()) / (1000 * 60));
        if (minutesRemaining < 0) {
            setDueTime();
        }
    }

    public void resetTasks() {
        for (Task t : deadline.tasks) {
            t.setCompleted(false);
        }
        taskBox.put(deadline.tasks);
        updateTasksInViews();
    }

    private void goBack() {
        NavUtils.navigateUpFromSameTask(this);
    }

    private void toggleActive(boolean isActive) {
        deadline.setActive(isActive);
        deadlineBox.put(deadline);
        resetTasksMenuItem.setVisible(isActive);
        editDeadlineMenuItem.setVisible(!isActive);
        invalidateOptionsMenu();
    }

    private void addTask() {
        DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                addOrEditTaskFromDialogResults(new Task(), dialogInterface);
                switch (i) {
                    // Add another task
                    case DialogInterface.BUTTON_POSITIVE:
                        addTask();
                        break;

                    // Done adding tasks
                    case DialogInterface.BUTTON_NEGATIVE:
                        updateTasksInViews();
                        break;
                }
            }
        };

        // Get dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layout = getLayoutInflater().inflate(R.layout.dlg_new_task, null);

        // Show Dialog for editing or making a new Task
        builder.setView(layout)
                .setTitle("Add Task")
                .setCancelable(true)
                .setPositiveButton("Add Another Task", ocl)
                .setNegativeButton("Done", ocl)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        updateTasksInViews();
                    }
                }).show();
    }

    public boolean editTask(long taskId) {
        final Task task = deadline.tasks.getById(taskId);

        // Get dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layout = getLayoutInflater().inflate(R.layout.dlg_new_task, null);

        // Set UI values (If task is being edited)
        ((EditText) layout.findViewById(R.id.new_task_text)).setText(task.getName());
        ((TimeDurationPicker) layout.findViewById(R.id.timeDurationInput)).setDuration(task.getDuration() * 60 * 1000);

        // Show Dialog for editing or making a new Task
        builder.setView(layout)
                .setTitle("Edit Task")
                .setCancelable(true)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addOrEditTaskFromDialogResults(task, dialogInterface);
                        updateTasksInViews();
                    }
                }).show();
        return true;
    }

    private void addOrEditTaskFromDialogResults(Task task, DialogInterface DI) {
        EditText et = ((AlertDialog) DI).findViewById(R.id.new_task_text);
        long duration = ((TimeDurationPicker) (((AlertDialog) DI).findViewById(R.id.timeDurationInput))).getDuration();
        String text = et.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "No name, task not saved/changed", Toast.LENGTH_LONG).show();
            return;
        }
        int minutes = (int) (duration / (60 * 1000));

        // Set Task values
        task.setName(text);
        task.setDuration(minutes);

        // Commit to DB, redraw UI
        task.deadline.setTarget(deadline);
        taskBox.put(task);
        updateTasksInViews();
    }

    private void updateDeadline() {
        deadline = deadlineQuery.findUnique();
    }

    private void updateTasksInViews() {
        updateDeadline();

        List<Task> tasks = deadline.tasks;

        if (tasks.size() <= 0) return;
        int minDuration = 1000, d, newTotalMinutes = 0, newActiveMinutes = 0;
        currentTaskMinutes = tasks.get(0).getDuration();

        for (Task t : tasks) {
            d = t.getDuration();
            newTotalMinutes += d;
            if(!t.isCompleted()){
                newActiveMinutes += d;
            }
            if (d < minDuration) {
                minDuration = d;
            }
        }

        boolean updateTimeScale = false;


        float newHPM = (minMinutesHeightPx / minDuration);
        if (newHPM != heightPerMinutePx){
            updateTimeScale = true;
            heightPerMinutePx = newHPM;
        }
        if (newActiveMinutes != activeMinutes){
            updateTimeScale = true;
            activeMinutes = newActiveMinutes;
        }
        if (newTotalMinutes != totalMinutes){
            updateTimeScale = true;
            totalMinutes = newTotalMinutes;
        }

        if(updateTimeScale) drawBackground();

        LayoutInflater inflater = getLayoutInflater();

        activeTasks.removeAllViews();
        doneTasks.removeAllViews();

        for (Task t : tasks) {

            TaskView child = (TaskView) inflater.inflate(R.layout.item_active_task, activeTasks, false);
            child.init(t, activeToCompleteOCL, editOLCL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
            lp.leftMargin = (int) taskLeftMargin;
            lp.height = (int) (t.getDuration() * heightPerMinutePx);
            child.setLayoutParams(lp);
            child.setVisibility(t.isCompleted() ? GONE : VISIBLE);
            activeTasks.addView(child);

            TaskView doneChild = (TaskView) inflater.inflate(R.layout.item_done_task, doneTasks, false);
            doneChild.init(t, completeToActiveOCL, null);
            doneChild.setStrike(true);
            doneChild.setVisibility(t.isCompleted() ? VISIBLE : GONE);
            doneTasks.addView(doneChild);
        }

    }

    private void checkBackgroundRatio(){
        if( activeMinutes > activeMinutesBackgroundWasDrawnAt ) {
            setBackground();
            return;
        }

        int diff = backGroundView.getHeight() - activeTasks.getHeight() - afterListPadding;
        if((diff / screenheight) > 0.5) setBackground();
    }

    private void setBackground(){
        int h = (int)(heightPerMinutePx * activeMinutes) + afterListPadding + beforeListPadding;
        backGroundView.setLayoutParams(new ConstraintLayout.LayoutParams(screenwidth, h));
        clippedBackground.setLevel(10000*( h / fullBackgroundHeight));
        activeMinutesBackgroundWasDrawnAt = activeMinutes;
    }

    private void drawBackground(){
        //backGroundView.setParams((int)heightPerMinutePx, totalMinutes, activeMinutes, dueDateMillis);
        fullBackgroundHeight = (int)(heightPerMinutePx * totalMinutes) + afterListPadding + beforeListPadding;

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
        endTime.setTimeInMillis(dueDateMillis);
        Calendar startTime = (Calendar) endTime.clone();
        startTime.add(Calendar.MINUTE, -(totalMinutes));

        Picture ticksPicture = new Picture();
        Canvas canvas = ticksPicture.beginRecording(screenwidth, fullBackgroundHeight);
        canvas.drawColor(Color.GREEN);

        int minute, level;
        float drawY = 0, startX;
        for (; startTime.compareTo(endTime) <= 0; startTime.add(Calendar.MINUTE, 1)) {
            minute = startTime.get(Calendar.MINUTE);

            level = MINUTE;
            startX = screenwidth;
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
                canvas.drawLine(0, drawY, screenwidth, drawY, tickMarkPaint);
            }
            if (level >= showTimeOnLevel) {
                String text = TimeConverter.timeOfDayString(startTime.getTime());
                Rect bounds = new Rect();
                mTxtPaint.getTextBounds(text, 0, text.length(), bounds);
                canvas.drawText(text, screenwidth - bounds.width(), drawY + (bounds.height() / 2), mTxtPaint);
            }
            drawY += heightPerMinutePx;
        }

        ticksPicture.endRecording();
        //fullBackground.setPicture(ticksPicture);
        clippedBackground = new ClipDrawable(new PictureDrawable(ticksPicture), Gravity.BOTTOM, ClipDrawable.VERTICAL);
        //clippedBackground.setDrawable(new PictureDrawable(ticksPicture));
        backGroundView.setBackground(clippedBackground);
        setBackground();
    }

    private void initializeBackground(){
        Resources res = getResources();

        showOnMinimumPixelHeight[TICK][MINUTE] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_minute);
        showOnMinimumPixelHeight[TICK][FIVE] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_five);
        showOnMinimumPixelHeight[TICK][FIFTEEN] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_fifteen);
        showOnMinimumPixelHeight[TICK][THIRTY] = res.getDimensionPixelSize(R.dimen.tick_min_height_break_thirty);

        showOnMinimumPixelHeight[TIME][MINUTE] = res.getDimensionPixelSize(R.dimen.time_min_height_break_minute);
        showOnMinimumPixelHeight[TIME][FIVE] = res.getDimensionPixelSize(R.dimen.time_min_height_break_five);
        showOnMinimumPixelHeight[TIME][FIFTEEN] = res.getDimensionPixelSize(R.dimen.time_min_height_break_fifteen);
        showOnMinimumPixelHeight[TIME][THIRTY] = res.getDimensionPixelSize(R.dimen.time_min_height_break_thirty);

        screenheight = res.getDisplayMetrics().heightPixels;
        screenwidth = res.getDisplayMetrics().widthPixels;

        beforeListPadding = res.getDimensionPixelSize(R.dimen.before_list_padding);
        afterListPadding = res.getDimensionPixelSize(R.dimen.after_list_padding);
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

        taskLeftMargin = 150;
    }

    @Override
    public void onPause() {
        super.onPause();
        intervalTimer.removeCallbacks(intervalMethod);
    }

    @Override
    public void onResume() {
        super.onResume();
        intervalTimer.post(intervalMethod);
    }

    public int setTaskCompletionState(long taskId, boolean isComplete) {
        Task t = deadline.tasks.getById(taskId);
        t.setCompleted(isComplete);
        taskBox.put(t);
        return t.getDuration();
    }

}

