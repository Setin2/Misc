package com.example.hoply;

import android.content.Context;
import android.os.AsyncTask;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.ArrayList;

/*
 * This class represents the local database
 */
@Database(entities = {User.class, Post.class, Comment.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase{
    // database daos
    public abstract UserDao userDao();
    public abstract PostDao postDao();
    public abstract CommentDao commentDao();

    private static final String databaseName = "database";  // name
    private static AppDatabase instance;                    // instance

    /*
     * Return an instance of the database
     */
    static synchronized AppDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context, AppDatabase.class, databaseName)
                    .build();
        }
        return instance;
    }

    /*
     * Return true if commenter has made over 100 comments on the original poster's posts (aka if the commentor is a top fan)
     * We will use this to assign the top fan badge
     */
    static boolean isTopFan(String commenterId, String originalPosterId){
        try{
            return new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    int numberOfComments = instance.commentDao().topFan(commenterId, originalPosterId);
                    return numberOfComments >= 100;
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Return the name of the user with the given id
     */
    static String getNameFromId(String userId){
        try{
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    return instance.userDao().getNameFromId(userId);
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Return the number of comments on a certain post
     */
    static int numberOfCommentsOnThisPost(int postId){
        try{
            return new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... voids) {
                    return instance.commentDao().numberOfCommentsOnThisPost(postId);
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    /*
     * Return a list of all comments that belong to the post with the given id
     */
    static ArrayList<Comment> getAllCommentsFromPost(int id){
        try{
            return new AsyncTask<Void, Void, ArrayList<Comment>>() {
                @Override
                protected ArrayList<Comment> doInBackground(Void... voids) {
                    return (ArrayList<Comment>) instance.commentDao().getAllCommentsFromPost(id);
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Insert an user in the database
     */
    static void insertUser(final User user){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids){
                instance.userDao().createUser(user);
                return null;
            }
        }.execute();
    }

    /*
     * Insert a post in the database
     */
    static void insertPost(final Post post){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids){
                instance.postDao().createPost(post);
                return null;
            }
        }.execute();
    }

    /*
     * Insert a comment in the database
     */
    static void insertComment(final Comment comment){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids){
                instance.commentDao().createComment(comment);
                return null;
            }
        }.execute();
    }

    /*
     * Return true if a user with the given id exists
     */
    static boolean userExists(final String userID){
        try{
            return new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    return instance.userDao().userExists(userID);
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Returns true if a post with the given id exists
     */
    static boolean postExists(final int id){
        try{
            return new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    return instance.postDao().postExists(id);
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Returns true if a comment with the given attributes exists
     */
    static boolean commentExists(final String userId, final int postId, final long stamp){
        try{
            return new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    return instance.commentDao().commentExists(userId, postId, stamp);
                }
            }.execute().get();
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Delete an user from the database
     */
    static void deleteUser(User user){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids){
                instance.userDao().deleteUser(user);
                return null;
            }
        }.execute();
    }

    /*
     * Delete a post from the database
     */
    static void deletePost(Post post){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids){
                instance.postDao().deletePost(post);
                return null;
            }
        }.execute();
    }

    /*
     * Delete a comment from the database
     */
    static void deleteComment(Comment comment){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids){
                instance.commentDao().deleteComment(comment);
                return null;
            }
        }.execute();
    }
}
