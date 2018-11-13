package com.coltennye.punctual.deadline.tasks;

public class TaskView implements Comparable<TaskView>{

    public TaskView(long id, String name, int duration, boolean completed) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.completed = completed;
    }

    public String name;
    public long id;
    public int duration;
    public int startTime;
    public boolean completed;

    @Override
    public int compareTo(TaskView t) {
        if (this.completed == t.completed)
            return 0;
        return completed? 1:-1;
    }
}
