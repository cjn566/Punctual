package com.coltennye.punctual.deadline;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.ListView;

import com.coltennye.punctual.App;
import com.coltennye.punctual.db.Deadline;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskAdaptor;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public abstract class DeadlineFragment extends Fragment {

    ListView listView;
    protected Deadline deadline;
    protected Box<Task> taskBox;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BoxStore boxStore = ((App) getActivity().getApplication()).getBoxStore();
        taskBox = boxStore.boxFor(com.coltennye.punctual.db.Task.class);
    }

    protected void getDeadline(){
        this.deadline = ((DeadlineActivity)getActivity()).getDeadline();
    }

    protected void updateDeadline(){
        ((DeadlineActivity)getActivity()).updateDeadline(this.deadline);
    }
}
