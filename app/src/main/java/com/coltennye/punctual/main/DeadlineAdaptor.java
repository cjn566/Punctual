package com.coltennye.punctual.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coltennye.punctual.R;
import com.coltennye.punctual.db.Deadline;

import java.util.ArrayList;
import java.util.List;

public class DeadlineAdaptor extends BaseAdapter {

    private List<Deadline> deadlines;

    private static class DeadlineViewHolder {
        public TextView text;

        public DeadlineViewHolder(View itemView) {
            text = itemView.findViewById(R.id.deadline_text);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DeadlineViewHolder holder;
        if(view == null){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_deadline, viewGroup, false);
            holder = new DeadlineViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (DeadlineViewHolder) view.getTag();
        }

        Deadline deadline = getItem(i);
        holder.text.setText(deadline.getName() + (deadline.isActive()? " (active)":""));

        return view;
    }

    public DeadlineAdaptor() {
        this.deadlines = new ArrayList<>();
    }

    public void setDeadlines(List<Deadline> dl){
        deadlines = dl;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return deadlines.size();
    }

    @Override
    public Deadline getItem(int i) {
        return deadlines.get(i);
    }

    @Override
    public long getItemId(int i) {
        return deadlines.get(i).id;
    }
}
