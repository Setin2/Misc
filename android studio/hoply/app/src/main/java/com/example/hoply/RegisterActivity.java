package com.example.hoply;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import java.util.Objects;

/*
 * Class representing an activity that lets you register a new user
 */
public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // we change the color of the action bar to match the style of the app
        this.getWindow().setStatusBarColor(this.getResources().getColor(R.color.yes));
        // get instance of the database
        database = AppDatabase.getInstance(getApplicationContext());
        // make user creation available
        createUser();
    }

    private AppDatabase database; // instance of the database

    /*
     * If back button is pressed, terminate activity
     */
    @Override
    public void onBackPressed() {
        finish();
    }

    /*
     * Return true if the device is connected to the internet
     */
    public boolean checkInternetConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // we return true if connection is available
        assert connectivityManager != null;
        if (Objects.requireNonNull(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)).getState() == NetworkInfo.State.CONNECTED ||
                Objects.requireNonNull(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        // else we alert the user that connection to internet is missing
        else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(RegisterActivity.this);
            dialog.setTitle( "ERROR" )
                    .setMessage("Task failed due to missing connection to internet")
                    .setPositiveButton("OK", (dialoginterface, i) -> {
                        dialoginterface.cancel();
                    }).show();
            return false;
        }
    }

    /*
     * Create a new user
     */
    public void createUser(){
        // once this button is clicked
        final Button createUser = findViewById(R.id.register_create_account);
        // create new account
        createUser.setOnClickListener((View v) -> {
            // take all input text and the current time
            final EditText id = findViewById(R.id.register_Id);
            String userID = id.getText().toString().trim();
            final EditText name = findViewById(R.id.register_name);
            String fullName = name.getText().toString().trim();
            long stamp = System.currentTimeMillis();

            // if the input is not empty
            if (!userID.isEmpty() && !fullName.isEmpty()){
                // we check to see if the user id is already taken
                if (!database.userExists(userID)){
                    // we then check for internet connection
                    if (checkInternetConnection()){
                        //create a new user with the given texts and time
                        User user = new User(userID, fullName, stamp);
                        // insert user into local and remote database
                        database.insertUser(user);
                        RemoteDatabase.insertIntoDatabase("user", user, null, null, getApplicationContext());
                        Snackbar.make(findViewById(R.id.registerActivity), "User created successfully", Snackbar.LENGTH_SHORT).show();
                    }
                }
                else {
                    Snackbar.make(findViewById(R.id.registerActivity), "User id already in use", Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(findViewById(R.id.registerActivity), "Please fill in the text", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
