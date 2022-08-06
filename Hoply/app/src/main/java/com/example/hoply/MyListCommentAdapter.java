package com.example.hoply;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * This class represents the adapter for the recyclerView containing comments
 */
public class MyListCommentAdapter extends RecyclerView.Adapter<MyListCommentAdapter.ViewHolder>{
    private ArrayList<Comment> comment;     // list of all comments for the given post
    private Context context;
    private AppDatabase appDatabase;        // instance of local database
    private String posterId;                // id of the original poster
    private String currentUser;             // current user id

    /*
     * Constructor
     */
    MyListCommentAdapter(ArrayList<Comment> comment, Context context, AppDatabase appDatabase, String posterId, String currentUser) {
        this.comment = comment;
        this.context = context;
        this.posterId = posterId;
        this.appDatabase = appDatabase;
        this.currentUser = currentUser;
    }

    /*
     * Create new view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.list_comment, parent, false);
        return new ViewHolder(listItem);
    }

    /*
     * Replace the content of a view
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Comment comment = this.comment.get(position);
        // we set the user-id, name, content etc to their respective views
        holder.userId.setText("@" + comment.userId);
        holder.userName.setText(appDatabase.getNameFromId(comment.userId));
        holder.content.setText(comment.content);
        // we check how long ago the comment has been made
        long postedXTimeAgo = System.currentTimeMillis() - comment.stamp;
        // posted less than a day ago
        if (postedXTimeAgo < 86400000){
            int timeInDays = (int) TimeUnit.MILLISECONDS.toHours(postedXTimeAgo);
            holder.commentStamp.setText(timeInDays + "h ago");
        }
        // posted between a day and a week ago
        else if (postedXTimeAgo < 604800000){
            int timeInDays = (int) TimeUnit.MILLISECONDS.toDays(postedXTimeAgo);
            holder.commentStamp.setText(timeInDays + "d ago");
        }
        // posted at least a week ago
        else{
            int timeInDays = (int) TimeUnit.MILLISECONDS.toDays(postedXTimeAgo) / 7;
            holder.commentStamp.setText(timeInDays + "w ago");
        }
        if (appDatabase.isTopFan(comment.userId, posterId)){
            // if commenter is top fan of the poster we set the visibility of the top fan badge to visible
            holder.topFan.setVisibility(View.VISIBLE);
        }
        // if this comment is made by the current user, we set the visibility of the delete button to visible
        if (comment.userId.equals((currentUser))){
            holder.delete.setVisibility(View.VISIBLE);
            holder.edit.setVisibility(View.VISIBLE);
        }
        AtomicBoolean firstTap = new AtomicBoolean(true);
        //once the delete button is pressed prompt an alert dialog
        holder.delete.setOnClickListener(v -> {
            // if this is the first time the delete button is pressed, we prompt the user to confirm his action by pressing again
            if (firstTap.get()){
                Toast.makeText(context, "Press twice to confirm", Toast.LENGTH_SHORT).show();
                firstTap.set(false);
            }
            // if this is not the first tap
            else {
                    // we delete the comment from the local database
                    appDatabase.deleteComment(comment);
                    // we convert the stamp of the comment to string
                    ZonedDateTime zdt2 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(comment.stamp), ZoneId.systemDefault());
                    // we replace the + in the timestamp with %2B since the sign is encoded in html
                    String stamp2 = zdt2.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+", "%2B");
                    // delete the comment from the remote database
                    RemoteDatabase.deleteFromDatabase("comment", comment.userId, comment.postId, stamp2, context);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show();
                }
        });
        holder.edit.setOnClickListener(v -> {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            View popupView = inflater.inflate(R.layout.pupup_edit, null);
            final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            popupWindow.showAtLocation(holder.edit, Gravity.CENTER, 0, 0);
            ((EditText) popupView.findViewById(R.id.editWindow)).setText(comment.content);

            ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(comment.stamp), ZoneId.systemDefault());
            // we replace the + in the timestamp with %2B since the sign is encoded in html
            String stamp = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+", "%2B");

            final Button send = popupView.findViewById(R.id.send);
            send.setOnClickListener(vv -> {
                if (checkInternetConnection()){
                    // we delete the old comment from the remote database
                    RemoteDatabase.deleteFromDatabase("comment", comment.userId, comment.postId, stamp, context);
                    Toast.makeText(context, "Please close the window to confirm", Toast.LENGTH_SHORT).show();

                    // if the user dismisses the popup window we post the new comment into the both databases
                    // this way we ensure that we don't delete the comment without replacing it with something else
                    popupWindow.setOnDismissListener(() -> {
                        Comment editComment = new Comment(((EditText) popupView.findViewById(R.id.editWindow)).getText().toString(), comment.userId, comment.postId, comment.stamp); // create new post
                        appDatabase.insertComment(editComment); // insert post into the local database
                        RemoteDatabase.insertIntoDatabase("comment", null, null, editComment, context);
                        Toast.makeText(context, "Post edited succesfully", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    /*
     * Return true if internet connection is enabled
     */
    public boolean checkInternetConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // we return true if connection is available
        assert connectivityManager != null;
        if (Objects.requireNonNull(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)).getState() == NetworkInfo.State.CONNECTED ||
                Objects.requireNonNull(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        // else we alert the user that connection to internet is missing
        else {
            Toast.makeText(context, "Something went wrong due to missing internet connection", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /*
     * Set max number of posts to be displayed
     */
    @Override
    public int getItemCount() {
        return comment.size();
    }

    /*
     * Provide reference to the view for each item
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView userId;
        public TextView userName;
        public TextView content;
        TextView commentStamp;
        ImageView topFan;
        public Button delete;
        public Button edit;
        RelativeLayout relativeLayout;
        ViewHolder(View itemView) {
            super(itemView);
            this.userId = itemView.findViewById(R.id.userId);
            this.userName = itemView.findViewById(R.id.userName);
            this.content = itemView.findViewById(R.id.content);
            this.commentStamp = itemView.findViewById(R.id.commentStamp);
            this.topFan = itemView.findViewById(R.id.topFan);
            this.delete = itemView.findViewById(R.id.delete);
            this.edit = itemView.findViewById(R.id.edit);
            relativeLayout = itemView.findViewById(R.id.relativeLayout);
        }
    }
}