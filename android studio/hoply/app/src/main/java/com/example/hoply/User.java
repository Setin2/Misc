package com.example.hoply;

import androidx.room.*;
import androidx.annotation.NonNull;

/*
 * Class representing users
 */
@Entity(tableName = "users")
public class User {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "stamp")
    public long stamp;

    /*
     * Constructor
     */
    public User(@NonNull String id, String name, long stamp){
        this.id = id;
        this.name = name;
        this.stamp = stamp;
    }
}