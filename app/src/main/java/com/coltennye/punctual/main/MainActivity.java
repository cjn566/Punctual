package com.coltennye.punctual.main;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.MenuCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

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
import com.coltennye.punctual.R;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskView;

import net.steamcrafted.materialiconlib.MaterialMenuInflater;

import java.util.Calendar;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import mobi.upod.timedurationpicker.TimeDurationPicker;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "default_channel";
    private final int REFRESH_UI_DELAY = 5 * 1000;
    private Handler intervalTimer;
    private Runnable intervalMethod;

    private long dueDateMillis;

    private TextView tv_dueTime;
    private TextView tv_timeLeft;
    private LinearLayout activeTasks;
    private LinearLayout doneTasks;

    private Box<Task> taskBox;

    private Toolbar toolBar;
    private MenuItem resetTasksMenuItem;
    private MenuItem editDeadlineMenuItem;
    private MenuItem alarmOnMenuItem;
    private MenuItem alarmOffMenuItem;

    private View.OnClickListener activeToCompleteOCL;
    private View.OnClickListener completeToActiveOCL;
    private View.OnLongClickListener editOLCL;

    private int activeMinutes;
    private int totalMinutes;
    private int minutesRemaining;

    private SharedPreferences prefs;
    private boolean notifyAtLowTime;
    private int spareTimeNotify;
    private boolean notifyAtDueTime;
    private boolean lowTimeNotificationDelivered = false;
    private boolean lowTimeNotificationDismissed = false;
    private final int lowTimeNtfId = 1337;
    private ntfDismissReceiver dismissReceiver;
    private boolean deadlineIsActive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deadline);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        BoxStore boxStore = ((App) this.getApplication()).getBoxStore();
        taskBox = boxStore.boxFor(com.coltennye.punctual.db.Task.class);

        toolBar = findViewById(R.id.toolbar_deadline);
        activeTasks = findViewById(R.id.lv_tasks_active);
        doneTasks = findViewById(R.id.lv_tasks_done);
        tv_dueTime = findViewById(R.id.tv_dueTime);
        tv_timeLeft = findViewById(R.id.tv_timeLeft);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Default", importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        dismissReceiver = new ntfDismissReceiver(this);
        registerReceiver(dismissReceiver, new IntentFilter("blorp"));

        setSupportActionBar(toolBar);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        //actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);

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
                toggleTaskCompletion(view, true);
            }
        };

        completeToActiveOCL =new View.OnClickListener()
        {
            @Override
            public void onClick (View view){
                toggleTaskCompletion(view, false);
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
                mTimePicker = new TimePickerDialog(com.coltennye.punctual.main.MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        prefs.edit().putInt(getString(R.string.due_hour_key), selectedHour).apply();
                        prefs.edit().putInt( getString(R.string.due_minute_key), selectedMinute).apply();
                        // TODO: set time in Shared PreferencesFragment
                        setDueTime();
                    }
                }, prefs.getInt( getString(R.string.due_hour_key),8 ), prefs.getInt(getString(R.string.due_minute_key),0), TimeConverter.is24hr());
                mTimePicker.setTitle("Deadline Due Time:");
                mTimePicker.show();
            }
        });
        setDueTime();
    }

    private void toggleTaskCompletion(View view, boolean isNowComplete){
        int taskMinutes = setTaskCompletionState(((TaskView) view).getTaskId(), isNowComplete);
        activeMinutes += (isNowComplete? -taskMinutes : taskMinutes);
        if(isNowComplete) {
            doneTasks.getChildAt(activeTasks.indexOfChild(view)).setVisibility(VISIBLE);
        } else {
            activeTasks.getChildAt(doneTasks.indexOfChild(view)).setVisibility(VISIBLE);
        }
        view.setVisibility(GONE);
        updateTime();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MaterialMenuInflater.with(this)
                .setDefaultColor(Color.BLACK)
                .inflate(R.menu.deadline_menu, menu);

        MenuCompat.setGroupDividerEnabled(menu, true);

        resetTasksMenuItem = menu.findItem(R.id.reset_tasks);
        alarmOnMenuItem = menu.findItem(R.id.toggle_icon_on);
        alarmOffMenuItem = menu.findItem(R.id.toggle_icon_off);

        Switch activeTogglerActionView = menu.findItem(R.id.menu_deadline_active_switch).getActionView().findViewById(R.id.active_switch);

        deadlineIsActive = prefs.getBoolean(getString(R.string.is_active_key), false);
        activeTogglerActionView.setChecked(deadlineIsActive);
        alarmOffMenuItem.setVisible(!deadlineIsActive);
        alarmOnMenuItem.setVisible(deadlineIsActive);

        activeTogglerActionView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isActive) {
                deadlineIsActive = isActive;
                prefs.edit().putBoolean(getString(R.string.is_active_key), isActive).apply();
                alarmOffMenuItem.setVisible(!isActive);
                alarmOnMenuItem.setVisible(isActive);
                invalidateOptionsMenu();
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset_tasks:
                resetTasks();
                break;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }


    private void setDueTime() {
        int hour = prefs.getInt(getString(R.string.due_hour_key), 8);
        int minute = prefs.getInt(getString(R.string.due_minute_key), 0);
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        c.clear(Calendar.MINUTE);
        c.clear(Calendar.HOUR);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        if (now > c.getTimeInMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        tv_dueTime.setText(TimeConverter.timeOfDayString(c.getTime()));
        dueDateMillis = c.getTimeInMillis();
        updateTime();
    }

    public void markNotificationDismissed(){
        lowTimeNotificationDismissed = true;
    }

    private void updateTime() {
        minutesRemaining = (int) ((dueDateMillis - System.currentTimeMillis()) / (1000 * 60));
        int timeStatus = minutesRemaining - activeMinutes;

        if(timeStatus <= 0)
            tv_timeLeft.setTextColor(Color.RED);
        else
            tv_timeLeft.setTextColor(Color.rgb(33,201,14));
        tv_timeLeft.setText(TimeConverter.timeRemainingString(timeStatus));

        // Due time has passed for today, reset for tomorrow
        if (minutesRemaining < 0) {
            setDueTime();
            return;
        }

        if (deadlineIsActive && notifyAtLowTime) {

            // Is there low time left? If not, reset delivery state
            if (timeStatus <= spareTimeNotify) {

                // Has it already been shown dismissed? If not, show / update it. Otherwise don't show it.
                if (!lowTimeNotificationDismissed) {

                    String explanation;
                    if (timeStatus != 0) {
                        explanation = "You only have " + minutesRemaining + " minutes to do " +
                                activeMinutes + " minutes worth of tasks. That puts you " +
                                (timeStatus > 0 ?
                                        ("only " + timeStatus + " minutes ahead of") :
                                        (-timeStatus + " minutes behind")) +
                                " schedule.";
                    } else {
                        explanation = "You have only just enough time (" + minutesRemaining + " minutes) to complete your tasks.";
                    }
                    explanation += " Get to it!";

                    // Go to app on notification tap
                    PendingIntent onTapIntent = PendingIntent.getActivity(this,
                            0, new Intent(this, this.getClass()), 0);

                    // Record if notification was dismissed
                    Intent dismissIntent = new Intent("blorp");
                    dismissIntent.putExtra("id", lowTimeNtfId);
                    PendingIntent dismissPI =
                            PendingIntent.getBroadcast(getApplicationContext(), 0, dismissIntent, 0);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle("You're low on time!")
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(explanation))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .setContentIntent(onTapIntent)
                            .setDeleteIntent(dismissPI)
                            .setOnlyAlertOnce(true);

                    NotificationManagerCompat nm = NotificationManagerCompat.from(this);
                    nm.notify(lowTimeNtfId, builder.build());
                }
            } else {
                lowTimeNotificationDelivered = false;
            }
        }
    }

    public void resetTasks() {
        List<Task> tasks = taskBox.getAll();
        for (Task t : tasks) {
            t.setCompleted(false);
        }
        taskBox.put(tasks);
        updateTasksInViews();
    }

    private void toggleActive(boolean isActive) {
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
        View layout = getLayoutInflater().inflate(R.layout.new_task, null);

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
        final Task task = taskBox.get(taskId);

        // Get dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layout = getLayoutInflater().inflate(R.layout.new_task, null);

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
        taskBox.put(task);
        updateTasksInViews();
    }

    private void updateTasksInViews() {

        List<Task> tasks = taskBox.getAll();

        if (tasks.size() <= 0) return;
        int d, newTotalMinutes = 0, newActiveMinutes = 0;
        int currentTaskMinutes = tasks.get(0).getDuration();

        for (Task t : tasks) {
            d = t.getDuration();
            newTotalMinutes += d;
            if(!t.isCompleted()){
                newActiveMinutes += d;
            }
        }

        activeMinutes = newActiveMinutes;

        LayoutInflater inflater = getLayoutInflater();
        activeTasks.removeAllViews();
        doneTasks.removeAllViews();

        for (Task t : tasks) {
            TaskView child = (TaskView) inflater.inflate(R.layout.item_active_task, activeTasks, false);
            child.init(t, activeToCompleteOCL, editOLCL);
            child.setVisibility(t.isCompleted() ? GONE : VISIBLE);
            activeTasks.addView(child);

            TaskView doneChild = (TaskView) inflater.inflate(R.layout.item_done_task, doneTasks, false);
            doneChild.init(t, completeToActiveOCL, editOLCL);
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

        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        notifyAtLowTime = prefs.getBoolean("notify_at_low_time", true);
        if(notifyAtLowTime) spareTimeNotify = prefs.getInt("spare_time", 5);
        notifyAtDueTime = prefs.getBoolean("notify_at_due_time", true);
        deadlineIsActive = prefs.getBoolean(getString(R.string.is_active_key), false);

    }

    public int setTaskCompletionState(long taskId, boolean isComplete) {
        Task t = taskBox.get(taskId);
        t.setCompleted(isComplete);
        taskBox.put(t);
        return t.getDuration();
    }
}

