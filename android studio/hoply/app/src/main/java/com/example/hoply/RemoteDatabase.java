package com.example.hoply;

import android.content.Context;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/*
 * Class containing methods for using the remote database
 */
public class RemoteDatabase{
    private static AppDatabase database;    // local database instance

    /*
     * Insert a certain user/post/comment into the remote database
     */
    public static void insertIntoDatabase(String choice, User user, Post post, Comment comment, Context context){
        try {
            // initialize the url and json-object with the initial value null
            String url = null;
            JSONObject jsonObject = new JSONObject();

            // we create a new json-object with the given attributes based on which object we wish to insert into the remote database
            switch (choice) {
                case "user": {
                    // convert the long to a sql friendly zone-datetime
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(user.stamp), ZoneId.systemDefault());
                    String stamp = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                    url = "http://caracal.imada.sdu.dk/app2020/users";
                    jsonObject.put("id", user.id);
                    jsonObject.put("name", user.name);
                    jsonObject.put("stamp", stamp);
                    break;
                }
                case "post": {
                    // convert the long to a sql friendly zone-datetime
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(post.stamp), ZoneId.systemDefault());
                    String stamp = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                    url = "http://caracal.imada.sdu.dk/app2020/posts";
                    jsonObject.put("id", post.id);
                    jsonObject.put("user_id", post.userId);
                    jsonObject.put("content", post.content);
                    jsonObject.put("stamp", stamp);
                    break;
                }
                case "comment": {
                    // convert the long to a sql friendly zone-datetime
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(comment.stamp), ZoneId.systemDefault());
                    String stamp = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                    url = "http://caracal.imada.sdu.dk/app2020/comments";
                    jsonObject.put("user_id", comment.userId);
                    jsonObject.put("post_id", comment.postId);
                    jsonObject.put("content", comment.content);
                    jsonObject.put("stamp", stamp);
                    break;
                }
            }
            // we insert the object into the remote database using json-object request and POST
            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {
                // we get a response from the database
                // method throws an error because the response is null and it expects a json-object
                // does not affect the program but we should find a way for it to expect a null instead of a json-object
                @Override
                public void onResponse(JSONObject response) {

                }
                // we get an error if something went wrong and put it into the log
            }, error -> Log.e("Error: ", error.toString())) {
                // we set the headers to get authorization
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    String accesstoken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYXBwMjAyMCJ9.PZG35xIvP9vuxirBshLunzYADEpn68wPgDUqzGDd7ok";
                    headers.put("Authorization", "Bearer " + accesstoken);
                    return headers;
                }
            };
            Volley.newRequestQueue(context.getApplicationContext()).add(jsonRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * Get all objects of type choice from the remote database, insert them into the local database as long as they aren't already there
     */
    public static void getAllFromDatabase(String choice, Context context, String extra){
        try{
            database = AppDatabase.getInstance(context);

            // we initialize the url with one of these values (depending on the choice attribute)
            String url = null;
            switch (choice) {
                case "user":
                    url = "http://caracal.imada.sdu.dk/app2020/users";
                    break;
                case "post":
                    url = "http://caracal.imada.sdu.dk/app2020/posts" + extra;
                    break;
                case "comment":
                    url = "http://caracal.imada.sdu.dk/app2020/comments" + extra;
                    break;
            }
            // we request all the objects of the type "choice" from the remote database using json-array request and GET
            JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        // for each object in the remote database, we take the id, name and stamp and do something with them
                        for (int i = 0; i < response.length(); i++) {
                            switch (choice) {
                                case "user":
                                    JSONObject user = response.getJSONObject(i);
                                    // if object isn't already in the list of objects, we insert it.
                                    if (!database.userExists(user.getString("id"))) {
                                        String id = user.getString("id");
                                        String name = user.getString("name");
                                        String nameWithoutPassword = name.split("@PWD")[0];
                                        String stamp = user.getString("stamp");

                                        // we convert the zonedatetime we got from the remote database to a corresponding long (since we save stamps as long in our local database)
                                        ZonedDateTime zdt = ZonedDateTime.parse(stamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                                        long longStamp = zdt.toInstant().toEpochMilli();

                                        if (!id.contains("@IMG") && !nameWithoutPassword.contains("@IMG")){
                                            User newUser = new User(id, nameWithoutPassword, longStamp);
                                            database.insertUser(newUser);
                                        }
                                    }
                                    break;
                                case "post":
                                    JSONObject post = response.getJSONObject(i);
                                    String postContent = post.getString("content");
                                    // if object isn't already in the list of objects, we insert it. only if it does not exceed the limit
                                    if (!database.postExists(post.getInt("id"))) {
                                        if (postContent.getBytes().length < 300000) {
                                            int id = post.getInt("id");
                                            String userID = post.getString("user_id");
                                            String stamp = post.getString("stamp");

                                            // we convert the zone-date-time we got from the remote database to a corresponding long (since we save stamps as long in our local database)
                                            ZonedDateTime zdt = ZonedDateTime.parse(stamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                                            long longStamp = zdt.toInstant().toEpochMilli();

                                            Post newPost = new Post(id, postContent, userID, longStamp);
                                            database.insertPost(newPost);
                                        }
                                    }
                                    break;
                                case "comment":
                                    JSONObject comment = response.getJSONObject(i);
                                    String userID = comment.getString("user_id");
                                    int postID = comment.getInt("post_id");
                                    String content = comment.getString("content");
                                    String stamp = comment.getString("stamp");
                                    // we convert the zone-date-time we got from the remote database to a corresponding long (since we save stamps as long in our local database)
                                    ZonedDateTime zdt = ZonedDateTime.parse(stamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                                    long longStamp = zdt.toInstant().toEpochMilli();

                                    // if object isn't already in the list of objects, we insert it.
                                    if (!database.commentExists(userID, postID, longStamp) && !content.contains("@IMG")) {
                                        Comment newComment = new Comment(content, userID, postID, longStamp);
                                        database.insertComment(newComment);
                                    }
                                    break;
                            }
                        }
                    } catch(JSONException e){
                        e.printStackTrace();
                    }
                }
                // if something goes wrong, we print the error into the log
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Error", error.toString());
                }
            });
            Volley.newRequestQueue(context.getApplicationContext()).add(jsonRequest);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
     * Delete a certain user/post/comment from the remote database
     */
    public static void deleteFromDatabase(String choice, String userId, int postID, String stamp, Context context){
        try{
            // we initialize the url with one of these values (depending on the choice attribute)
            String url = null;
            switch (choice) {
                case "user":
                    url = "http://caracal.imada.sdu.dk/app2020/users?id=eq." + userId;
                    break;
                case "post":
                    url = "http://caracal.imada.sdu.dk/app2020/posts?id=eq." + postID;
                    break;
                case "comment":
                    url = "http://caracal.imada.sdu.dk/app2020/comments?user_id=eq." + userId + "&post_id=eq." + postID + "&stamp=eq." + stamp;
                    break;
            }
            // we send a string request to the remote database to delete a certain user
            StringRequest jsonRequest = new StringRequest(Request.Method.DELETE, url, new Response.Listener<String>() {
                // we put the response into the log
                @Override
                public void onResponse(String response) {
                    Log.e("Result: ", response);
                }
                // we put the error into the log
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Error: ", error.toString());
                }
                // we set the headers to get authorization
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    String accesstoken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYXBwMjAyMCJ9.PZG35xIvP9vuxirBshLunzYADEpn68wPgDUqzGDd7ok";
                    headers.put("Authorization", "Bearer " + accesstoken);
                    return headers;
                }
            };
            Volley.newRequestQueue(context.getApplicationContext()).add(jsonRequest);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}