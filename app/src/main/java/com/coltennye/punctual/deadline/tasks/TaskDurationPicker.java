package com.coltennye.punctual.deadline.tasks;

import mobi.upod.timedurationpicker.TimeDurationPicker;
import mobi.upod.timedurationpicker.TimeDurationPickerDialogFragment;

public class TaskDurationPicker extends TimeDurationPickerDialogFragment {

    public TaskDurationPicker(){

    }

    @Override
    protected int setTimeUnits() {
        return TimeDurationPicker.HH_MM;
    }

    @Override
    public void onDurationSet(TimeDurationPicker view, long duration) {

    }
}
