package com.example.footballfinder.classes;

import android.content.Context;

import androidx.core.util.Consumer;

import com.android.volley.VolleyError;
import com.example.footballfinder.utilities.httpHelper;

import org.json.JSONObject;

/*
 * Class representing users
 */
public class User {
    public int id;
    public String username;
    public String password;
    private static User loggedInAsUser;
    /*
     * Constructor
     */
    public User(int id, String name, String password){
        this.id = id;
        this.username = name;
        this.password = password;
    }

    public static User getCurrentUser(){
        if(loggedInAsUser == null){
            return new User(-1, "", "");
        }
        return loggedInAsUser;
    }

    public static void userLogin(Context con, String username, String password, Consumer<User> consumer, Consumer<VolleyError> errorHandler){
        httpHelper http = new httpHelper(con);
        JSONObject body = new JSONObject();
        try{
            body.put("case", "userlogin");
            body.put("username", username);
            body.put("password", password);
        }catch(Exception e){
        }

        http.postRequest(body, data -> {
            try{
                JSONObject user_data = (JSONObject) data.get("data");
                user_data = (JSONObject) user_data.get("user");

                User user = getUserFromJSON(user_data);

                loggedInAsUser = user;

                consumer.accept(user);
            }catch (Exception e){
                consumer.accept(null);
            }
            http.stopQueue();
        }, err -> {
            errorHandler.accept(err);
            http.stopQueue();
        });

    }

    public static void createUser(Context con, String username, String password, Consumer<User> consumer, Consumer<VolleyError> errorHandler){
        httpHelper http = new httpHelper(con);
        JSONObject body = new JSONObject();
        try{
            body.put("case", "newuser");
            body.put("username", username);
            body.put("password", password);
        }catch(Exception e){
        }

        http.postRequest(body, data -> {
            try{
                JSONObject user_data = (JSONObject) data.get("data");
                user_data = (JSONObject) user_data.get("user");

                User user = getUserFromJSON(user_data);

                consumer.accept(user);
            }catch (Exception e){
                consumer.accept(null);
            }
            http.stopQueue();
        }, err -> {
            errorHandler.accept(err);
            http.stopQueue();

        });

    }

    public static User getUserFromJSON(JSONObject user_json) throws  Exception{
        User user = new User(
                user_json.getInt("id"),
                user_json.getString("username"),
                user_json.getString("password"));
        return user;
    }

}