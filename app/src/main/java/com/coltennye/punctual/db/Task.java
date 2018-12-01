package com.coltennye.punctual.db;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.coltennye.punctual.deadline.tasks.TaskView;

import javax.annotation.Nullable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class Task{

    @Id public long id;
    public ToOne<Deadline> deadline;
    private String name;
    private int duration;
    private boolean completed;

    public Task() {}

    public Task(String name, int duration) {
        this.name = name;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void toggleComplete() {this.completed = !this.completed;}

}
