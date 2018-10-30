package com.coltennye.punctual.deadline.tasks;

public class ViewTask implements Comparable<ViewTask>{

    public ViewTask(long id, String name, int duration, boolean completed) {
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
    public int compareTo(ViewTask t) {
        if (this.completed == t.completed)
            return 0;
        return completed? 1:-1;
    }
}
