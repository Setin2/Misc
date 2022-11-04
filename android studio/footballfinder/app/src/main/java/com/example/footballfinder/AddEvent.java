package com.example.footballfinder;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import com.example.footballfinder.classes.Event;
import com.example.footballfinder.utilities.Internet;
import com.example.footballfinder.classes.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.footballfinder.databinding.ActivityAddEventBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEvent extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityAddEventBinding binding;
    private int fieldID;

    Button btnDatePicker, btnTimePicker;
    TextView txtDate, txtTime;
    private int mYear, mMonth, mDay, mHour, mMinute;
    Calendar cal;
    private boolean createSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fieldID = getIntent().getIntExtra("fieldID", -1);

        // get binding
        binding = ActivityAddEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        // add ability to create the event
        FloatingActionButton fab = findViewById(R.id.fab2);
        fab.setOnClickListener(view -> {
            addEvent();
        });

        pickDateTime();
        cal = Calendar.getInstance();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_add_event);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /*
     * If back button is pressed, terminate activity
     */
    @Override
    public void onBackPressed() {
        finish();
    }

    /*
     * Create a new event with given data
     */
    private synchronized void addEvent(){
        if(createSent){
            return;
        }
        // some fields are left empty
        if (getMaxParticipants() == 0 || getDescription().isEmpty() || txtDate.getText().toString().isEmpty() || txtTime.getText().toString().isEmpty())
            Snackbar.make(findViewById(R.id.addEvent), "invalid details", Snackbar.LENGTH_SHORT).show();
        else {
            if (Internet.internetConnectionAvailable(this)){

                Date startDate = cal.getTime();

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);
                calendar.add(Calendar.HOUR_OF_DAY, getDuration());
                Date endDate = calendar.getTime();

                String pattern = "yyyy-MM-dd HH:mm:SS";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.GERMAN);

                String start = simpleDateFormat.format(startDate);
                String end = simpleDateFormat.format(endDate);

                Event event = new Event(
                        -1,
                        fieldID,
                        getMaxParticipants(),
                        User.getCurrentUser().id,
                        getDescription(),
                        start,
                        end
                );

                createSent = true;
                Event.addEvent(this, event, createdEvent -> {
                    finish();
                }, error -> {
                    createSent = false;
                    if(error.networkResponse.statusCode == 409){
                        Snackbar.make(findViewById(R.id.addEvent), "Overlaps another event, change time", Snackbar.LENGTH_SHORT).show();
                    }else{
                        Snackbar.make(findViewById(R.id.addEvent), "Server error", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
            else
                Snackbar.make(findViewById(R.id.addEvent), "no internet connection", Snackbar.LENGTH_SHORT).show();
        }
    }

    /*
     * Get event description from input
     */
    private String getDescription(){
        EditText des = findViewById(R.id.descriptionInput);
        return des.getText().toString().trim();
    }

    /*
     * Get max participants from input
     */
    private int getMaxParticipants(){
        EditText max = findViewById(R.id.maxParticipantsInput);
        // input left empty
        if (max.getText().toString().trim().isEmpty())
            return 0;
        else
            return Integer.parseInt(max.getText().toString().trim());
    }

    /*
     * Get hours of playtime from input
     */
    private int getDuration(){
        EditText max = findViewById(R.id.playTime);
        return Integer.parseInt(max.getText().toString().trim());
    }

    /*
     * Add functionality to pick a date and time
     */
    private void pickDateTime(){
        // get buttons and textviews
        btnDatePicker = findViewById(R.id.btn_date);
        btnTimePicker = findViewById(R.id.btn_time);
        txtDate = findViewById(R.id.in_date);
        txtTime = findViewById(R.id.in_time);

        // date button onclicklistener
        btnDatePicker.setOnClickListener(view -> {
            // Get Current Date
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            @SuppressLint("SetTextI18n") DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view1, year, monthOfYear, dayOfMonth) -> {

                        // set the text to the date chosen for visibility
                        txtDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, monthOfYear);
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        });
        // time button onclicklistener
        btnTimePicker.setOnClickListener(view -> {
            final Calendar c = Calendar.getInstance();
            mHour = c.get(Calendar.HOUR_OF_DAY);
            mMinute = c.get(Calendar.MINUTE);

            // Launch Time Picker Dialog
            @SuppressLint("SetTextI18n") TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view12, hourOfDay, minute) -> {

                        txtTime.setText(hourOfDay + ":" + minute);
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        cal.set(Calendar.MINUTE, minute);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                    }, mHour, mMinute, false);
            timePickerDialog.show();
        });
    }

}