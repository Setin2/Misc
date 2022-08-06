package com.example.hoply;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import pl.droidsonroids.gif.GifImageView;

/*
 * This class represents all the "true main activity of the app" here users will make and view posts
 */
public class Hoply extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoply);
        // we change the color of the action bar to match the style of the app
        this.getWindow().setStatusBarColor(this.getResources().getColor(R.color.yes));
        // we get an instance of the database
        database = AppDatabase.getInstance(getApplicationContext());
        // we get the userId from the previous activity.
        userId = getIntent().getStringExtra("userId");

        // we get a list of all posts currently in the database
        ArrayList<Post> posts = getAllPosts();
        assert posts != null;
        // create a list of posts for the recyclerView, this is a subset of the previous list
        ArrayList<Post> posts1 = new ArrayList<>(posts.subList(0, 20));
        // we create a new recyclerView and adapter for the posts in the list of posts
        RecyclerView recyclerView = findViewById(R.id.posts);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        MyListPostAdapter adapter = new MyListPostAdapter(posts1, getApplicationContext(), database, userId);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        // we make posting, getting images and logging out available
        createPost(adapter, posts1, recyclerView);
        logOut();
        getImage();
        // we add a few functionalities to scrolling
        scrolling(recyclerView, adapter, manager, posts, posts1);

        // we get the location of the user in case the user wishes to share location
        location = new MyLocationListener(this);
    }

    private boolean isScrolling;                        // boolean telling if the user is scrolling
    int currentItems, totalItems, scrollOutItems;       // variables used for getting the position of the screen in the recyclerview
    private static AppDatabase database;                // local database instance
    private static String userId;                       // current user id
    private MyLocationListener location;                // device location

    /*
     * If back button is pressed on this activity - nothing happens. So you cant log out of the app through back button.
     */
    @Override
    public void onBackPressed() { }

    /*
     * Load more posts on scrolling
     */
    public void scrolling(RecyclerView recyclerView, MyListPostAdapter adapter, LinearLayoutManager manager, ArrayList<Post> posts, ArrayList<Post> posts1){
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            // we tell the app that the user is scrolling
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                currentItems = manager.getChildCount();
                totalItems = manager.getItemCount();
                scrollOutItems = manager.findFirstVisibleItemPosition();

                // popup a FloatingActionButton to give the user the ability to go up
                if (dy < 0 && !findViewById(R.id.go_up).isShown())
                    // show the FloatingActionButton on scoll up
                    findViewById(R.id.go_up).setVisibility(View.VISIBLE);
                else if (dy > 0 && findViewById(R.id.go_up).isShown())
                    // hide the FloatingActionButton on scroll down
                    findViewById(R.id.go_up).setVisibility(View.GONE);
                findViewById(R.id.go_up).setOnClickListener(view -> {
                    // go up if the button is pressed and hide it
                    ( (LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).scrollToPositionWithOffset(0, 0);
                    findViewById(R.id.go_up).setVisibility(View.GONE);
                });

                // if bottom of the list is reached, load 5 more posts
                if (isScrolling && (currentItems + scrollOutItems == totalItems)){
                    isScrolling = false;
                    new Handler().postDelayed(() -> {
                        for (int i = 0; i < 5; i++){
                            if (posts1.size() < posts.size()-1){
                                findViewById(R.id.progress).setVisibility(View.VISIBLE);
                                posts1.add(posts1.size(), posts.get(posts1.size() + 1));
                                adapter.notifyItemInserted(posts1.size());
                            }
                        }
                    }, 500);
                }
                else {
                    findViewById(R.id.progress).setVisibility(View.GONE);
                }
            }
        });
    }

    /*
     * Log out of the app and go back to the login screen
     */
    public void logOut(){
        // once the log out button is pressed
        final Button logOut = findViewById(R.id.logOut);
        // prompt an alert window to confirm the action
        logOut.setOnClickListener( v -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(Hoply.this);
            dialog.setTitle( "LOG OUT" )
                    .setMessage("Are you sure you want to log out?")
                    .setNegativeButton("NO", (dialoginterface, i) -> dialoginterface.cancel())
                    .setPositiveButton("YES", (dialoginterface, i) -> {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        // start login activity and finish this activity
                        startActivity(intent);
                        finish();
                    }).show();
        });
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
            AlertDialog.Builder dialog = new AlertDialog.Builder(Hoply.this);
            dialog.setTitle( "ERROR" )
                    .setMessage("Task failed due to missing connection to internet")
                    .setPositiveButton("OK", (dialoginterface, i) -> dialoginterface.cancel()).show();
            return false;
        }
    }

    /*
     * Create a new post
     */
    public void createPost(MyListPostAdapter adapter, ArrayList<Post> posts, RecyclerView recyclerView){
        // once this button is clicked
        final ImageView postButton = findViewById(R.id.postSomething);
        // we create and insert a new post into the database
        postButton.setOnClickListener( v -> {
            // this will only work if the user has not chosen an image to post
            if (!imageChoosen) {
                // take input (aka. post content) and time
                EditText id = findViewById(R.id.writePost);
                String content = id.getText().toString().trim();
                // we check to see if the user actually wrote something
                if (!content.equals("")){
                    long stamp = System.currentTimeMillis();
                    int postId = (int) (Math.random() * 1000000) + 100000; // currently, the id is a random number between 6 and 7 digits
                    // make sure the id is not taken
                    createUniqueId(postId);

                    // we check if the device is connected to the internet
                    if (checkInternetConnection()) {
                        // we create a post
                        Post post = null;
                        // if the user writes the following line, the post will contain the location of the user
                        if (content.contains("@GPS")) {
                            if (isLocationEnabled()){
                                post = new Post(postId, content.replace("@GPS", shareLocation()), userId, stamp);
                            }
                            else {
                                Snackbar.make(findViewById(R.id.hoplyActivity), "Cant share location since location is not enabled", Snackbar.LENGTH_SHORT).show();
                            }
                        } else {
                            post = new Post(postId, content, userId, stamp);
                        }
                        if (post != null){
                            // we insert the post into the local and remote database
                            database.insertPost(post);
                            RemoteDatabase.insertIntoDatabase("post", null, post, null, getApplicationContext());
                            Snackbar.make(findViewById(R.id.hoplyActivity), "Post created", Snackbar.LENGTH_SHORT).show();
                            id.setText("");
                            Post finalPost = post;
                            new Handler().postDelayed(() -> {
                                posts.add(0, finalPost);
                                adapter.notifyItemInserted(0);
                                ( (LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).scrollToPositionWithOffset(0, 0);
                            }, 500);
                        }
                        // if the post contains the word "beer", an easter egg will appear on the screen for 3 seconds
                        if (post != null && post.content.contains("beer")) {
                            GifImageView gif = findViewById(R.id.beer);
                            new CountDownTimer(3000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    gif.setBackgroundResource(R.drawable.beer);
                                    gif.setImageResource(R.drawable.beer);
                                    gif.bringToFront();
                                }
                                @Override
                                public void onFinish() {
                                    gif.setBackgroundResource(0);
                                    gif.setImageResource(0);
                                }
                            }.start();
                        }
                    }
                }
                else {
                    Snackbar.make(findViewById(R.id.hoplyActivity), "Field is empty", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*
     * Return true if location is enabled
     */
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    /*
     * Return the location of the user as a string of form @GPS[location]
     */
    private String shareLocation(){
        return "@GPS[" + location.latitude + "," + location.longitude + "]";
    }

    /*
     * Makes sure the given id is unique
     */
    private void createUniqueId(int id){
        if (database.postExists(id)){
            id = (int) (Math.random() * 1000000) + 100000; // currently, the id is a random number between 6 and 7 digits
            createUniqueId(id);
        }
    }

    private static final int IMAGE_REQUEST = 1;
    private static boolean imageChoosen = false;    // keeps track of whether or not the user has choosen to post an image

    /*
     * Lets user pick an image from the gallery
     */
    private void getImage(){
        ImageView getImage = findViewById(R.id.post_image);
        getImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Pick and image"), IMAGE_REQUEST);
        });
    }

    /*
     * The result of picking an image
     */
    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        // we check to see if all image data is in order
        if (reqCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null){
            try {
                // we get the image and save it as a Bitmap object
                final Uri imageData = data.getData();
                assert imageData != null;
                final InputStream imageStream = getContentResolver().openInputStream(imageData);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                // we show the image as a small icon in the edit-text so that the user knows that an image has been selected
                ImageView choosedImage = findViewById(R.id.choosedImage);
                choosedImage.setImageBitmap(selectedImage);
                imageChoosen = true;

                EditText writePost = findViewById(R.id.writePost);
                writePost.setOnKeyListener((v, keyCode, event) -> {
                    // if backspace is clicked in the edit-text
                    if (keyCode == KeyEvent.KEYCODE_DEL) {
                        // we revert the choices
                        choosedImage.setImageBitmap(null);
                        imageChoosen = false;
                    }
                    return false;
                });

                // once the post button is clicked
                final ImageView postButton = findViewById(R.id.postSomething);
                // we create and insert a new post into the database
                postButton.setOnClickListener( v -> {
                    // we check to see if an image has been chosen - might be redundant
                    if (imageChoosen){
                        long stamp = System.currentTimeMillis();
                        int postId = (int) (Math.random() * 1000000) + 100000;
                        createUniqueId(postId);
                        String content = writePost.getText().toString().trim();

                        // we check whether the device is connected to the internet
                        if (checkInternetConnection()) {
                            // we create a new post with the image encoded to base64 as the content
                            Post post = new Post(postId, content + "@IMG[" + encodeTobase64(selectedImage) + "]", userId, stamp);
                            // we insert the image into the local and remote database
                            database.insertPost(post);
                            RemoteDatabase.insertIntoDatabase("post", null, post, null, getApplicationContext());
                            choosedImage.setImageBitmap(null);
                            imageChoosen = false;
                            writePost.setText("");
                            Snackbar.make(findViewById(R.id.hoplyActivity), "Post created", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            Toast.makeText(this, "You haven't picked an image", Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Encode a Bitmap image to base64
     */
    private String encodeTobase64(Bitmap image) {
        Bitmap immagex = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();

        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    /*
     * Return a list of all posts in the database
     */
    private static ArrayList<Post> getAllPosts(){
        try{
            return new AsyncTask<Void, Void, ArrayList<Post>>() {
                @Override
                protected ArrayList<Post> doInBackground(Void... voids) {
                    return (ArrayList<Post>) database.postDao().getAll();
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
