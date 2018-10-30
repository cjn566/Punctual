package com.coltennye.punctual.deadline;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import com.coltennye.punctual.App;
import com.coltennye.punctual.db.Task;
import com.coltennye.punctual.deadline.tasks.TaskAdaptorActive;
import com.coltennye.punctual.main.MainActivity;
import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Deadline;
import com.coltennye.punctual.db.Deadline_;

import javax.annotation.Nullable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;

public class DeadlineActivity extends AppCompatActivity {

    private Toolbar toolBar;
    private Switch activeTogglerActionView;
    private MenuItem resetTasksMenuItem;
    private MenuItem editDeadlineMenuItem;

    private Deadline deadline;
    private Box<Deadline> deadlineBox;
    private Query<Deadline> deadlineQuery;

    private FragmentDeadlineEdit editFrag;
    private FragmentDeadlineActive activeFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deadline);
        toolBar = findViewById(R.id.toolbar_deadline);
        setSupportActionBar(toolBar);

        Intent intent = getIntent();
        long deadlineId = intent.getLongExtra(MainActivity.EXTRA_DEADLINE_ID, 0);
        BoxStore boxStore = ((App) getApplication()).getBoxStore();
        deadlineBox = boxStore.boxFor(com.coltennye.punctual.db.Deadline.class);
        deadlineQuery = deadlineBox.query().equal(Deadline_.__ID_PROPERTY, deadlineId).build();
        deadline = deadlineQuery.findUnique();

        FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
        editFrag = new FragmentDeadlineEdit();
        activeFrag = new FragmentDeadlineActive();

        if(deadline.isActive())
            tr.add(R.id.fragment_container, activeFrag).commit();
        else
            tr.add(R.id.fragment_container, editFrag).commit();
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

                Toast.makeText(getApplicationContext(), "toggle: " + (b? "on":"off"), Toast.LENGTH_SHORT).show();
                toggleActive(b);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.reset_tasks:
                activeFrag.resetTasks();
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

    private void goBack(){
        NavUtils.navigateUpFromSameTask(this);
    }

    private void toggleActive(boolean isActive){
        deadline = getDeadline();
        deadline.setActive(isActive);
        deadlineBox.put(deadline);
        resetTasksMenuItem.setVisible(isActive);
        editDeadlineMenuItem.setVisible(!isActive);
        invalidateOptionsMenu();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, (isActive? activeFrag : editFrag)).commit();
    }

    public Deadline getDeadline(){
        Deadline dl =deadlineQuery.findUnique();
        return dl;
    }

    public void updateDeadline(Deadline dl){
        deadlineBox.put(dl);
    }
}
