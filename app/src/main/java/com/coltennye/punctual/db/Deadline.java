package com.coltennye.punctual.db;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class Deadline implements java.io.Serializable {
    public Deadline(){}

    @Id
    public long id;
    private String name;
    private Integer minute;
    private Integer hour;


    private boolean active;

    @Backlink(to = "deadline")
    public ToMany<Task> tasks;

    public boolean isActive() {        return active;    }

    public void setActive(boolean active) {        this.active = active;    }

    public Integer getMinute() {
        return this.minute;
    }
    public Integer getHour() {
        return this.hour;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }
    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public String getName(){
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



}

