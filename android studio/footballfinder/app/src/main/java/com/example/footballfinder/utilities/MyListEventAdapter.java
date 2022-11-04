package com.example.footballfinder.utilities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.footballfinder.JoinActivity;
import com.example.footballfinder.R;
import com.example.footballfinder.classes.Event;

import java.util.ArrayList;

/*
 * This class represents the adapter for the recyclerView containing events
 */
public class MyListEventAdapter extends RecyclerView.Adapter<MyListEventAdapter.ViewHolder>{
    private ArrayList<Event> events;
    private Context con;

    /*
     * Constructor
     */
    public MyListEventAdapter(ArrayList<Event> viewableEvents, Context con) {
        this.events = viewableEvents;
        this.con = con;
    }

    /*
     * Create new view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_events, parent, false);
        return new ViewHolder(view);
    }

    /*
     * Replace the content of a view
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Event event = this.events.get(position);

        // gonna have to find owner username based on event owner id
        holder.eventOwner.setText(event.owner_username);

        holder.description.setText(event.description);
        holder.maxParticipants.setText("Max participants: " + event.maxParticipant);

        holder.event_time.setText(event.start_time + " - " + event.end_time);

        holder.join.setOnClickListener( v -> {
            Intent intent = new Intent(con, JoinActivity.class);
            intent.putExtra("eventID", event.id);

            con.startActivity(intent);
        });
    }

    /*
     * Set max number of posts to be displayed
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    /*
     * Provide reference to the view for each item
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView eventOwner;
        public TextView maxParticipants;
        public TextView description;
        public TextView event_time;
        public Button join;
        ViewHolder(View itemView) {
            super(itemView);
            this.eventOwner = itemView.findViewById(R.id.eventOwner);
            this.maxParticipants = itemView.findViewById(R.id.maxParticipants);
            this.description = itemView.findViewById(R.id.description);
            this.event_time = itemView.findViewById(R.id.event_time);
            this.join = itemView.findViewById(R.id.join);

        }
    }
}