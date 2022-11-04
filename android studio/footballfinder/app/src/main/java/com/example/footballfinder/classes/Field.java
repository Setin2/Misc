package com.example.footballfinder.classes;

import android.content.Context;

import androidx.core.util.Consumer;

import com.android.volley.VolleyError;
import com.example.footballfinder.utilities.httpHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/*
 * Class representing fields
 */
public class Field {
    public int id;
    public String description;
    public String location;
    public double lat;
    public double lon;
    public String type;
    public String owner;
    public boolean event_today;

    /*
     * Constructor
     */
    public Field(int id, String description, String location, double lat, double lon, String type, String owner, boolean event_today){
        this.id = id;
        this.description = description;
        this.location = location;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
        this.owner = owner;
        this.event_today = event_today;
    }
    public Field(int id, String description, String location, double lat, double lon, String type, String owner){
        this(id, description, location, lat, lon, type, owner, false);
    }

    public static void getAllFields(Context con, Consumer<ArrayList<Field>> consumer, Consumer<VolleyError> errorHandler){
        httpHelper http = new httpHelper(con);
        ArrayList<Field> fields = new ArrayList<>();
        JSONObject body = new JSONObject();
        try{
            body.put("case", "getfields");
        }catch(Exception e){

        }

        http.postRequest(body, data -> {
            try{
                JSONArray fields_data = (JSONArray) data.get("data");
                for(int i = 0; i < fields_data.length(); i++){
                    JSONObject f = (JSONObject) fields_data.get(i);
                    Field field = getFieldFromJSON(f);
                    fields.add(field);
                }
                consumer.accept(fields);
            }catch (Exception e){
            }
            http.stopQueue();
        }, err -> {
            errorHandler.accept(err);
            http.stopQueue();
        });
    }

    public static Field getFieldFromJSON(JSONObject field_json) throws Exception{
        Field field = new Field(
                field_json.getInt("id"),
                field_json.getString("description"),
                field_json.getString("location"),
                field_json.getDouble("lat"),
                field_json.getDouble("lon"),
                field_json.getString("type"),
                field_json.getString("owner"),
                field_json.getBoolean("event_today"));

        return field;
    }

}