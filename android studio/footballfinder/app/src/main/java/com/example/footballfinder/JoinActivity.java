package com.example.footballfinder;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.footballfinder.classes.Event;
import com.example.footballfinder.classes.User;
import com.google.android.material.snackbar.Snackbar;

public class JoinActivity extends AppCompatActivity {
    private int eventID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        setTitle(R.string.join_event);
        eventID = getIntent().getIntExtra("eventID", -1);
        loadEvent();
    }

    private void loadEvent(){
        Event.getEventOnId(this, eventID, event -> {
            setContentView(R.layout.activity_join);
            LinearLayout layout = (LinearLayout) findViewById(R.id.join_layout);
            addTextToLayout(layout, "Event ID: " + event.id, 0);
            addTextToLayout(layout, "Owner: " + event.owner_username, 1);
            addTextToLayout(layout, "Participants: " + event.participants + " / " + event.maxParticipant, 2);

            findViewById(R.id.join_event).setOnClickListener( v -> {
                joinEvent();
            });

        }, error -> {
        });
    }

    private void addTextToLayout(LinearLayout layout, String text, int index){
        TextView v = new TextView(this);
        v.setText(text);
        layout.addView(v, index);
    }

    private void joinEvent(){
        Event.joinEvent(this, eventID, User.getCurrentUser().id, joined -> {
            Snackbar.make(findViewById(R.id.joinActivity), "Event joined", Snackbar.LENGTH_SHORT).show();
            loadEvent();
        }, err -> {
            if(err.networkResponse.statusCode == 409){
                Snackbar.make(findViewById(R.id.joinActivity), "Already joined or too many participants", Snackbar.LENGTH_SHORT).show();
            }
        });

    }

}