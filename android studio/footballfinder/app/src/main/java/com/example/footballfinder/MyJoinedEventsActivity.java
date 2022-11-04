package com.example.footballfinder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.footballfinder.classes.Event;
import com.example.footballfinder.classes.User;
import com.example.footballfinder.utilities.Logger;

import java.util.ArrayList;

public class MyJoinedEventsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.log("startedf");
        setTitle(R.string.my_joined_events);
        setContentView(R.layout.activity_my_joined_events);
        loadEvents();
    }

    private void loadEvents(){
        Logger.log("Load events!!");
        Event.getJoinedEvents(this, User.getCurrentUser().id, events -> {
            setContentView(R.layout.activity_my_joined_events);

            ListView eventListview = findViewById(R.id.joinedEventList);

            ArrayList<String> eventsText = new ArrayList<>();

            for(int i = 0; i < events.size(); i++){
                Event e = events.get(i);
                eventsText.add(
                        "\n" +
                        "Creator: " + e.owner_username + "\n" +
                        "Location: " + e.location + "\n" +
                        "Participants: " + e.participants + "\n" +
                        "Start: " + e.start_time + "\n" +
                        "End: " + e.end_time
                        + "\n"
                );
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, eventsText);

            eventListview.setAdapter(arrayAdapter);

        }, err -> {
            Logger.log("error occured");

        });

    }
}