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
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.coltennye.punctual.App;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskAdaptor;
import com.coltennye.punctual.main.MainActivity;
import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Deadline;
import com.coltennye.punctual.db.Deadline_;
import com.coltennye.punctual.views.MyListView;

import java.util.Calendar;
import java.util.Locale;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;

public class DeadlineActivity extends AppCompatActivity {

    private final int REFRESH_UI_DELAY = 1000;
    private Handler intervalTimer;
    private Runnable intervalMethod;
    private TaskAdaptor adaptor;
    //private int minutes;
    private long dueTime;
    private TextView timeLeft;


    private MyListView listView;
    private Deadline deadline;
    private Box<Task> taskBox;

    private Toolbar toolBar;
    private Switch activeTogglerActionView;
    private MenuItem resetTasksMenuItem;
    private MenuItem editDeadlineMenuItem;

    private Box<Deadline> deadlineBox;
    private Query<Deadline> deadlineQuery;

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
        deadline = deadlineQuery.findUnique();





        setSupportActionBar(toolBar);
        toolBar = findViewById(R.id.toolbar_deadline);






        intervalTimer = new Handler();
        intervalMethod = new Runnable() {
            @Override
            public void run() {
                updateTime();
                intervalTimer.postDelayed(this, REFRESH_UI_DELAY);
            }
        };





        listView = findViewById(R.id.lv_tasks_active);
        adaptor = new TaskAdaptor(this, listView);
        listView.setAdapter(adaptor);
        adaptor.setTasks(deadline.tasks);
        adaptor.notifyTasksChanged();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                toggleComplete(adaptor.getItemId(i));
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return editTask(deadline.tasks.get(i));
            }
        });

        // Set new task FAB listener
        findViewById(R.id.newTaskFAB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });


        timeLeft = findViewById(R.id.timeLeft);
        timeLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(DeadlineActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        deadline.setMinute(TimeConverter.toMinutes(selectedHour, selectedMinute));
                        deadlineBox.put(deadline);
                        setDueTime();
                    }
                },TimeConverter.getHour(deadline.getMinute()),TimeConverter.getMinute(deadline.getMinute()), DateFormat.is24HourFormat(getApplicationContext()));
                mTimePicker.setTitle("Deadline Due Time:");
                mTimePicker.show();
            }
        });
        setDueTime();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.deadline_menu, menu);

        resetTasksMenuItem = menu.findItem(R.id.reset_tasks);
        editDeadlineMenuItem = menu.findItem(R.id.edit_deadline);
        activeTogglerActionView = menu.findItem(R.id.menu_deadline_active_switch).getActionView().findViewById(R.id.active_switch);
        if(deadline.isActive()){
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
        switch (item.getItemId()){
            case R.id.reset_tasks:
                resetTasks();
                break;
            case R.id.edit_deadline:

                break;
            case R.id.delete_deadline:


                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
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
        return true;
    }


    public void setDueTime() {

        int mins = deadline.getMinute();
        Calendar c = Calendar.getInstance();
        int nowMinutes = TimeConverter.toMinutes(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        c.set(Calendar.HOUR_OF_DAY, TimeConverter.getHour(mins));
        c.set(Calendar.MINUTE, TimeConverter.getMinute(mins));
        if(nowMinutes > mins){
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        dueTime = c.getTimeInMillis();

        java.text.DateFormat df = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.US);
        String dueTime = df.format(c.getTime());
        String title = deadline.getName() + " @ " + dueTime;
        toolBar.setTitle(title);

        updateTime();

    }

    private void updateTime(){
        int secondsRemaining = (int) (dueTime - System.currentTimeMillis()) / (1000);
        int minutesRemaining = secondsRemaining / 60;
        timeLeft.setText("Time left: " + TimeConverter.timeRemainingString(minutesRemaining));
        listView.setSeconds(secondsRemaining);
    }

    private void toggleComplete(long id){
        if(id == -1) return;
        Task t =  deadline.tasks.getById(id);
        t.toggleComplete();
        taskBox.put(t);
        adaptor.setTasks(deadline.tasks);
    }

    public void resetTasks(){
        for(Task t : deadline.tasks){
            t.setCompleted(false);
        }
        taskBox.put(deadline.tasks);
        adaptor.setTasks(deadline.tasks);
    }

    private void goBack(){
        NavUtils.navigateUpFromSameTask(this);
    }

    private void toggleActive(boolean isActive){
        deadline.setActive(isActive);
        deadlineBox.put(deadline);
        resetTasksMenuItem.setVisible(isActive);
        editDeadlineMenuItem.setVisible(!isActive);
        invalidateOptionsMenu();
    }

    public Deadline getDeadline(){
        Deadline dl = deadlineQuery.findUnique();
        return dl;
    }

    private void addTask(){

        DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                addOrEditTask(new Task(), dialogInterface);
                switch (i){
                    case DialogInterface.BUTTON_POSITIVE:
                        addTask();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        getDeadline();
                        adaptor.setTasks(deadline.tasks);
                        break;
                }
            }
        };

        // Get dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layout = getLayoutInflater().inflate(R.layout.dlg_new_task,null);

        // Show Dialog for editing or making a new Task
        builder.setView(layout)
                .setTitle("Add Task")
                .setCancelable(true)
                .setPositiveButton("Next", ocl)
                .setNegativeButton("Done", ocl)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        getDeadline();
                        adaptor.setTasks(deadline.tasks);
                    }
                }).show();
    }

    private boolean editTask(final Task task){
        // Get dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layout = getLayoutInflater().inflate(R.layout.dlg_new_task,null);

        // Set UI values (If task is being edited)
        ((EditText)layout.findViewById(R.id.new_task_text)).setText(task.getName());
        ((NumberPicker)layout.findViewById(R.id.new_task_minutes)).setValue(task.getDuration());

        // Show Dialog for editing or making a new Task
        builder.setView(layout)
                .setTitle("Edit Task")
                .setCancelable(true)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addOrEditTask(task, dialogInterface);
                        adaptor.setTasks(deadline.tasks);
                    }
                })
                .show();
        return true;
    }

    private void addOrEditTask(Task task, DialogInterface DI){
        EditText et = ((AlertDialog) DI).findViewById(R.id.new_task_text);
        NumberPicker np = ((AlertDialog) DI).findViewById(R.id.new_task_minutes);
        String text = et.getText().toString();
        if(text == "")
            return;
        int minutes = np.getValue();

        // Set Task values
        task.setName(text);
        task.setDuration(minutes);

        // Commit to DB, redraw UI
        task.deadline.setTarget(deadline);
        taskBox.put(task);
        adaptor.setTasks(deadline.tasks);
        adaptor.notifyTasksChanged();
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

}
