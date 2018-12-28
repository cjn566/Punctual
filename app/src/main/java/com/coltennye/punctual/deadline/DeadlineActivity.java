package com.coltennye.punctual.deadline;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
    private TimeBackgroundView timeBackgroundView;
    private LinearLayout activeTasks;
    private LinearLayout doneTasks;

    private Deadline deadline;
    private Box<Task> taskBox;
    private Box<Deadline> deadlineBox;
    private Query<Deadline> deadlineQuery;

    private Toolbar toolBar;
    private MenuItem resetTasksMenuItem;
    private MenuItem editDeadlineMenuItem;

    private int activeMinutes;


    private View.OnClickListener activeToCompleteOCL;
    private View.OnClickListener completeToActiveOCL;
    private View.OnLongClickListener editOLCL;
    private int currentTaskMinutes;
    private float heightPerMinutePx;
    private int dueMinuteOfDay;
    private float taskRightMargin;
    private float minMinutesHeightPx;
    private TimeBackgroundView backGroundView;

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
                doneTasks.getChildAt(activeTasks.indexOfChild(view)).setVisibility(VISIBLE);
                view.setVisibility(GONE);
            }
        };

        completeToActiveOCL =new View.OnClickListener()
        {
            @Override
            public void onClick (View view){
                activeMinutes += setTaskCompletionState(((TaskView) view).getTaskId(), false);
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
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, dueHour);
        c.set(Calendar.MINUTE, dueMinute);
        if (now > c.getTimeInMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        tv_dueTime.setText(TimeConverter.timeOfDayString(c.getTime()));
        dueDateMillis = c.getTimeInMillis();
        updateTime();
    }

    private void updateTime() {
        int minutesRemaining = (int) (dueDateMillis - System.currentTimeMillis()) / (1000 * 60);
        if (minutesRemaining < 0) {
            setDueTime();
            return;
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
        int minDuration = 1000, d;
        int totalMinutes = 0;
        activeMinutes = 0;
        currentTaskMinutes = tasks.get(0).getDuration();

        for (Task t : tasks) {
            d = t.getDuration();
            totalMinutes += d;
            if(!t.isCompleted()){
                activeMinutes += d;
            }
            if (d < minDuration) {
                minDuration = d;
            }
        }

        heightPerMinutePx = minMinutesHeightPx / minDuration;
        backGroundView.setParams(heightPerMinutePx, totalMinutes, activeMinutes, dueDateMillis);

        LayoutInflater inflater = getLayoutInflater();

        activeTasks.removeAllViews();
        doneTasks.removeAllViews();

        for (Task t : tasks) {

            TaskView child = (TaskView) inflater.inflate(R.layout.item_active_task, activeTasks, false);
            child.init(t, activeToCompleteOCL, editOLCL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
            lp.rightMargin = (int) taskRightMargin;
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

