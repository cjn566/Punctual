package com.coltennye.punctual.deadline;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.TimeConverter;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskAdaptorActive;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FragmentDeadlineActive extends DeadlineFragment {

    private final int REFRESH_UI_DELAY = 60 * 1000;
    private Handler intervalTimer;
    private Runnable intervalMethod;
    private TaskAdaptorActive adaptor;
    //private int minutes;
    private long dueTime;
    private TextView timeLeft;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        getDeadline();

        //minutes = (60*deadline.getHour()) + deadline.getMinute();
        int mins = deadline.getMinute();

        Calendar c = Calendar.getInstance();
        int nowMinutes = TimeConverter.toMinutes(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        c.set(Calendar.HOUR_OF_DAY, TimeConverter.getHour(mins));
        c.set(Calendar.MINUTE, TimeConverter.getMinute(mins));

        if(nowMinutes > mins){
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        dueTime = c.getTimeInMillis();


        adaptor = new TaskAdaptorActive();

        intervalTimer = new Handler();
        intervalMethod = new Runnable() {
            @Override
            public void run() {
                updateTime();
                intervalTimer.postDelayed(this, REFRESH_UI_DELAY);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_deadline_active, container, false);
        timeLeft = view.findViewById(R.id.timeLeft);

        listView = view.findViewById(R.id.lv_tasks_active);
        listView.setAdapter(adaptor);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                toggleComplete(adaptor.getItemId(i));
            }
        });

        adaptor.setTasks(deadline.tasks);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        intervalTimer.post(intervalMethod);
    }

    private void updateTime(){
        int minutesRemaining = (int) (dueTime - System.currentTimeMillis()) / (60000);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.add(Calendar.MINUTE, deadline.getMinute());
        String dueText = (new SimpleDateFormat("h:mm a")).format(cal.getTime());

        timeLeft.setText("Due: " + dueText + "\nTime remaining:\n" + TimeConverter.timeRemainingString(minutesRemaining));
        adaptor.updateTimeTillDue(minutesRemaining);
    }

    private void toggleComplete(long id){
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

    @Override
    public void onPause() {
        super.onPause();
        intervalTimer.removeCallbacks(intervalMethod);
    }
}
