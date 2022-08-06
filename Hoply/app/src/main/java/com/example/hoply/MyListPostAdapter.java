package com.example.hoply;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
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
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * This class represents the adapter for the recyclerView containing posts
 */
public class MyListPostAdapter extends RecyclerView.Adapter<MyListPostAdapter.ViewHolder>{
    private ArrayList<Post> post;   // list containing al the posts in the database
    private Context context;
    private String currentUser;     // current user id
    private AppDatabase database;   // local database instance

    /*
     * Constructor
     */
    MyListPostAdapter(ArrayList<Post> post, Context context, AppDatabase database, String currentUser) {
        this.post = post;
        this.database = database;
        this.context = context;
        this.currentUser = currentUser;
    }

    /*
     * Create new view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.list_post, parent, false);
        return new ViewHolder(listItem);
    }

    /*
     * Replace the content of a view
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Post post = this.post.get(position);
        // we set the userId, userName, content and number of comments to this post
        holder.userId.setText("@" + post.userId);
        holder.userName.setText(database.getNameFromId(post.userId));
        // we check whether or not this post contains an image
        if (post.content.contains(("@IMG"))){
            // if it does, we will take the content from before the encoded image
            holder.content.setText((post.content).split("@IMG")[0]);
            // and set the image to the text between @IMG[ and ]
            String image = null;
            try {
                image = StringUtils.substringBetween(post.content, "@IMG[", "]");
                byte[] decodedString = Base64.decode(image.getBytes(), Base64.NO_WRAP);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.image.setImageBitmap(decodedByte);
            } catch (Exception ignored){
            }
        }
        else {
            holder.content.setText(post.content);
        }
        holder.numberOfComments.setText(Integer.toString(database.numberOfCommentsOnThisPost(post.id)));
        // we convert the stamp so that we show the user how long ago the post was made
        long postedXTimeAgo = System.currentTimeMillis() - post.stamp;
        // posted less than a day ago
        if (postedXTimeAgo < 86400000){
            int timeInDays = (int) TimeUnit.MILLISECONDS.toHours(postedXTimeAgo);
            holder.postStamp.setText(timeInDays + "h ago");
        }
        // posted between a day and a week ago
        else if (postedXTimeAgo < 604800000){
            int timeInDays = (int) TimeUnit.MILLISECONDS.toDays(postedXTimeAgo);
            holder.postStamp.setText(timeInDays + "d ago");
        }
        // posted at least a week ago
        else{
            int timeInDays = (int) TimeUnit.MILLISECONDS.toDays(postedXTimeAgo) / 7;
            holder.postStamp.setText(timeInDays + "w ago");
        }
        // once the comment button is pressed, start a new activity showing the post and its comments, and allow the user to comment on the post
        holder.button.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("postId", post.id);                 // pass this post to the next activity
            intent.putExtra("postUserId", post.userId);         // pass this post to the next activity
            intent.putExtra("currentUser", currentUser);        // pass the current user to the next activity
            if (post.content.contains(("@IMG"))){
                intent.putExtra("postContent", post.content.split("@IMG")[0]);       // pass this post to the next activity
            }
            else {
                intent.putExtra("postContent", post.content);       // pass this post to the next activity
            }
            context.startActivity(intent);
        });
        // if this post is made by the current user, we set the visibility of the delete and edit buttons to visible
        if (post.userId.equals((currentUser))){
            holder.delete.setVisibility(View.VISIBLE);
            holder.edit.setVisibility(View.VISIBLE);
        }
        // we use booleans to see how many times the user has tapped the delete button
        AtomicBoolean firstTap = new AtomicBoolean(true);
        //once the delete button is pressed prompt an alert dialog
        holder.delete.setOnClickListener(v -> {
            // if this is the first time the delete button is pressed, we prompt the user to confirm his action by pressing again
            if (firstTap.get()){
                firstTap.set(false);
                Toast.makeText(context, "Press twice to confirm", Toast.LENGTH_SHORT).show();
            }
            // if this is not the first tap
            else {
                // we delete the post, only if there are no comments on this post
                if (database.numberOfCommentsOnThisPost(post.id) == 0){
                    database.deletePost(post);
                    RemoteDatabase.deleteFromDatabase("post", null, post.id, null, context);
                    this.post.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                    Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(context, "You cant delete a post with comments", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // once the edit button is pressed we open a popup window
        holder.edit.setOnClickListener(v -> {
            // we only open the window if the post is not an image
            if (post.content.contains("@IMG")){
                Toast.makeText(context, "You cant edit an image", Toast.LENGTH_SHORT).show();
            }
            else {
                // create the popup-window and display the old content of the post
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                assert inflater != null;
                View popupView = inflater.inflate(R.layout.pupup_edit, null);
                final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                popupWindow.showAtLocation(holder.edit, Gravity.CENTER, 0, 0);
                ((EditText) popupView.findViewById(R.id.editWindow)).setText(post.content);

                // if the send button is clicked, we change the old post with the new post
                final Button send = popupView.findViewById(R.id.send);
                send.setOnClickListener(vv -> {
                    // we check if the user has internet connection
                    if (checkInternetConnection()){
                        // we let the user edit the post only if the post has no comments
                        if (database.numberOfCommentsOnThisPost(post.id) == 0){
                            // we delete the old post from the remote database
                            RemoteDatabase.deleteFromDatabase("post", null, post.id, null, context);
                            Toast.makeText(context, "Please close the window to confirm", Toast.LENGTH_SHORT).show();

                            // if the user dismisses the popup window we post the new post into the both databases
                            // this way we ensure that we don't delete the post without replacing it with something else
                            popupWindow.setOnDismissListener(() -> {
                                    Post editPost = new Post(post.id, ((EditText) popupView.findViewById(R.id.editWindow)).getText().toString(), post.userId, post.stamp); // create new post
                                    database.insertPost(editPost); // insert post into the local database
                                    RemoteDatabase.insertIntoDatabase("post", null, editPost, null, context); // insert post into remote database (currently not operational to avoid complications)
                                    Toast.makeText(context, "Post edited successfully", Toast.LENGTH_SHORT).show();
                            });
                        }
                        else {
                            Toast.makeText(context, "You cant edit a post with comments", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
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
        return post.size();
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
        public TextView userId;
        public TextView userName;
        TextView postStamp;
        public TextView content;
        public TextView numberOfComments;
        public ImageView image;
        public Button button;
        public Button delete;
        public Button edit;
        RelativeLayout relativeLayout;
        ViewHolder(View itemView) {
            super(itemView);
            this.image = itemView.findViewById(R.id.image);
            this.userId = itemView.findViewById(R.id.userId);
            this.userName = itemView.findViewById(R.id.userName);
            this.content = itemView.findViewById(R.id.content);
            this.numberOfComments = itemView.findViewById(R.id.numberOfComments);
            this.postStamp = itemView.findViewById(R.id.postStamp);
            this.button = itemView.findViewById(R.id.button);
            this.edit = itemView.findViewById(R.id.edit);
            this.delete = itemView.findViewById(R.id.delete);
            relativeLayout = itemView.findViewById(R.id.relativeLayout);
        }
    }
}