package com.coltennye.punctual.deadline;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.coltennye.punctual.R;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskAdaptorEdit;


import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FragmentDeadlineEdit extends DeadlineFragment {


    TextView time;
    TaskAdaptorEdit adaptor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDeadline();
        adaptor = new TaskAdaptorEdit();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_deadline_edit, container, false);
        listView = view.findViewById(R.id.lv_tasks_edit);
        listView.setAdapter(adaptor);
        adaptor.setTasks(deadline.tasks);

        time = view.findViewById(R.id.dueTime);
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        deadline.setMinute(TimeConverter.toMinutes(selectedHour, selectedMinute));
                        updateDeadline();
                        getDeadline();
                        updateTime();
                    }
                },TimeConverter.getHour(deadline.getMinute()),TimeConverter.getMinute(deadline.getMinute()), DateFormat.is24HourFormat(getActivity()));
                mTimePicker.setTitle("Deadline Due Time:");
                mTimePicker.show();
            }
        });
        updateTime();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                editTask(deadline.tasks.get(i));
            }
        });
        // Set new task FAB listener
        view.findViewById(R.id.newTaskFAB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });

        return view;
    }

    public void updateTime(){
        Calendar c = Calendar.getInstance();
        c.clear();
        c.add(Calendar.MINUTE, deadline.getMinute());
        time.setText(new SimpleDateFormat("h:mm a").format(c.getTime()));
    }

    private void addTask(){
        // Get dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View layout = getLayoutInflater().inflate(R.layout.new_task,null);

        DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                addOrUpdateTask(new Task(), dialogInterface);
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
                })
                .show();
    }

    private void editTask(final Task task){
        // Get dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View layout = getLayoutInflater().inflate(R.layout.new_task,null);

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
                        addOrUpdateTask(task, dialogInterface);
                        adaptor.setTasks(deadline.tasks);
                    }
                })
                .show();
    }

    private void addOrUpdateTask(Task task, DialogInterface DI){
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
    }


}
