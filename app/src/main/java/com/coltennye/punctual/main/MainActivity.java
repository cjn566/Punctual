package com.coltennye.punctual.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.coltennye.punctual.App;
import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Deadline;
import com.coltennye.punctual.deadline.DeadlineActivity;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_DEADLINE_ID = "com.coltennye.punctual.DEADLINE_ID";

    private ListView listView;
    DeadlineAdaptor deadlineAdaptor;

    private Box<Deadline> deadlineBox;
    private Query<Deadline> deadlineQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(myToolbar);
        ActionBar actionBar = getSupportActionBar();

        listView = findViewById(R.id.lv_deadlines);

        BoxStore boxStore = ((App) getApplication()).getBoxStore();
        deadlineBox = boxStore.boxFor(Deadline.class);
        deadlineQuery = deadlineBox.query().build();



        deadlineAdaptor = new DeadlineAdaptor();
        listView.setAdapter(deadlineAdaptor);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, DeadlineActivity.class);
                long deadlineId = deadlineAdaptor.getItem(i).id;
                intent.putExtra(EXTRA_DEADLINE_ID, deadlineId);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        updateDeadlines();
    }

    private void updateDeadlines(){
        List<Deadline> deadlines = deadlineQuery.find();
        deadlineAdaptor.setDeadlines(deadlines);
    }

    private void onListItemClick(ListView l, View v, int position, long id) {
        goToDeadline(deadlineAdaptor.getItem(position).id);
    }

    private void goToDeadline(long deadlineID){
        Intent intent = new Intent(this, DeadlineActivity.class);
        intent.putExtra(this.EXTRA_DEADLINE_ID, deadlineID);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void addDeadline(View v){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_deadline_dialog_title)
                .setView(R.layout.new_deadline)
                .setCancelable(true)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText et = ((AlertDialog) dialogInterface).findViewById(R.id.newDeadlineText);
                        String text = et.getText().toString();
                        Deadline dl = new Deadline();
                        dl.setName(text);
                        dl.setMinute(60*8);
                        long id = deadlineBox.put(dl);
                        updateDeadlines();
                        goToDeadline(id);
                    }
                })
                .setCancelable(true)
                .show();
    }
}