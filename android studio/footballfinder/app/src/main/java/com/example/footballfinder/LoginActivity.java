package com.example.footballfinder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.footballfinder.utilities.Internet;
import com.example.footballfinder.classes.User;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Debug.testActivity(this, MapsActivity.class); //Debugging
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        loginListener();
        signupListener();
    }

    private void loginListener() {

        final Button login = findViewById(R.id.login);
        login.setOnClickListener((View v) -> {
            if (Internet.internetConnectionAvailable(this)){
                User.userLogin(this, getUsername(), getPassword(), user -> {
                    openMapActivity();
                }, error -> {
                    if(error.networkResponse.statusCode == 401){
                        Snackbar.make(findViewById(R.id.mainActivity), "Invalid login", Snackbar.LENGTH_SHORT).show();
                    }else{
                        Snackbar.make(findViewById(R.id.mainActivity), "Server error", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }else{
                Snackbar.make(findViewById(R.id.mainActivity), "No internet connection", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void openMapActivity(){
        if(MapsActivity.active == false){
            Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
            startActivity(intent);
        }

    }

    /*
     * Go to SignUp activity
     */
    private void signupListener() {
        final Button signup = findViewById(R.id.signUp);
        signup.setOnClickListener((View v) -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    /*
     * Take the username from the input
     */
    private String getUsername(){
        EditText name = findViewById(R.id.user_name);
        return name.getText().toString().trim();
    }

    /*
     * Take the password from the input
     */
    private String getPassword(){
        EditText pass = findViewById(R.id.user_password);
        return pass.getText().toString().trim();
    }
}