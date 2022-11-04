package com.example.hoply;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/*
 * App main activity. Here you can log into the app and/or register a new user
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Splash);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // we change the color of the action bar to match the style of the app
        this.getWindow().setStatusBarColor(this.getResources().getColor(R.color.yes));
        // we get an instance of the database
        database = AppDatabase.getInstance(getApplicationContext());

        // we get the date for two weeks ago so that we can get all posts and comments that are newer than two weeks from the remote database
        String oneWeekAgo = ZonedDateTime
                .ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() - 604800000), ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .replace("+", "%2B");
        if (checkInternetConnection()){
            RemoteDatabase.getAllFromDatabase("user", getApplicationContext(), "?stamp=gt." + oneWeekAgo);
            RemoteDatabase.getAllFromDatabase("post", getApplicationContext(), "?stamp=gt." + oneWeekAgo);
            RemoteDatabase.getAllFromDatabase("comment", getApplicationContext(), "?stamp=gt." + oneWeekAgo);
        }

        // we make login available
        setUserId();
        login();
    }

    private AppDatabase database;   // instance of the local database
    private String userId;          // current user id

    /*
     * Open register activity - happens on register button click
     */
    public void register(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    /*
     * Take the user id from the input
     */
    public void setUserId(){
        // once the confirm id button is clicked, the current text in the edit-text is stored as the userId
        final Button confirmID = findViewById(R.id.confirm_id);
        EditText id = findViewById(R.id.register_Id);
        confirmID.setOnClickListener((View v) -> {
            userId = id.getText().toString().trim();
            Snackbar.make(findViewById(R.id.mainActivity), "ID confirmed", Snackbar.LENGTH_SHORT).show();
        });
    }

    public boolean checkInternetConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // we return true if connection is available
        assert connectivityManager != null;
        return Objects.requireNonNull(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)).getState() == NetworkInfo.State.CONNECTED ||
                Objects.requireNonNull(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).getState() == NetworkInfo.State.CONNECTED;
    }

    /*
     * Log in and open next activity
     */
    public void login(){
        // once the login button is clicked
        final Button login = findViewById(R.id.login_button);
        // we check if the user (the one stored using confirm id button) exists in the database and log in
        login.setOnClickListener((View v) -> {
            // if user exists in the database, log in. else throw message
            if (database.userExists(userId)){
                Intent intent = new Intent(getApplicationContext(), Hoply.class);
                intent.putExtra("userId", userId);                              // pass user id to the next activity
                startActivity(intent);                                                 // start next activity
                finish();                                                              // terminate this activity
            }
            else {
                Snackbar.make(findViewById(R.id.mainActivity), "User does not exist", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}