package com.example.hoply;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Objects;

/*
 * This activity is opened each time we wish to view the comments from a post, or we wish to comment on a post ourselves
 */
public class CommentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        // we change the color of the action bar to match the style of the app
        this.getWindow().setStatusBarColor(this.getResources().getColor(R.color.yes));

        // we get an instance of the database and the current user from the previous activity
        database = AppDatabase.getInstance(getApplicationContext());
        currentUserId = getIntent().getStringExtra("currentUser");
        // we get the post details from previous activity
        postId = getIntent().getIntExtra("postId", 0);
        String postUserId = getIntent().getStringExtra("postUserId");
        String postContent = getIntent().getStringExtra("postContent");

        // we set the userId and content of the post to their respective text-views in the xml files
        TextView userId = findViewById(R.id.originalUserId);
        userId.setText(String.format("@%s", postUserId));
        TextView content = findViewById(R.id.content);
        assert postContent != null;
        content.setText(postContent);
        TextView userName = findViewById(R.id.originalUserName);
        String userNameString = database.getNameFromId(postUserId);
        userName.setText(userNameString);

        // we give the user the ability to comment on this post
        createComment();

        // we create a list of comments for the recyclerview
        ArrayList<Comment> comments = database.getAllCommentsFromPost(postId);
        // we create a new recyclerView and adapter for the comments in the list of comments
        RecyclerView recyclerView = findViewById(R.id.comments);
        MyListCommentAdapter adapter = new MyListCommentAdapter(comments, getApplicationContext(), database, ((String) ((TextView) userId)
                .getText())
                .replace("@", ""), currentUserId);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private static AppDatabase database;    // database instance
    private static String currentUserId;    // current user id
    private static int postId;              // current post id

    /*
     * If back button is pressed, finish activity
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
            AlertDialog.Builder dialog = new AlertDialog.Builder(CommentActivity.this);
            dialog.setTitle( "ERROR" )
                    .setMessage("Task failed due to missing connection to internet")
                    .setPositiveButton("OK", (dialoginterface, i) -> dialoginterface.cancel()).show();
            return false;
        }
    }

    /*
     * Create a new comment
     */
    public void createComment(){
        // once this button is clicked
        final ImageView comment = findViewById(R.id.commentSomething);
        // we create and insert a new comment into the database
        comment.setOnClickListener(v -> {
            // take input (aka. post content) and time
            EditText id = findViewById(R.id.writeComment);
            String content = id.getText().toString().trim();
            // we check to see if the user actually wrote something
            if (!content.equals("")){
                long stamp = System.currentTimeMillis();
                // check if device is connected to the internet
                if (checkInternetConnection()){
                    // create the post
                    Comment comment1 = new Comment(content, currentUserId, postId, stamp);
                    // insert the post into the local and remote database
                    database.insertComment(comment1); // insert comment into the local database
                    RemoteDatabase.insertIntoDatabase("comment", null, null, comment1, getApplicationContext()); // insert comment into remote database (currently not operational to avoid complications)
                    Snackbar.make(findViewById(R.id.commentActivity), "Comment created", Snackbar.LENGTH_SHORT).show();
                }
            }
            else {
                Snackbar.make(findViewById(R.id.commentActivity), "Field is empty", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
