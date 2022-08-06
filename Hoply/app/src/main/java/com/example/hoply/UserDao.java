package com.example.hoply;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {
    // return true if the given user id can be found in the database
    @Query("SELECT EXISTS(SELECT * FROM users WHERE id = :userId)")
    boolean userExists(String userId);

    // delete a user from the database
    @Delete
    void deleteUser(User user);

    // return the name that matches the given id
    @Query("SELECT name FROM users WHERE id = :userId")
    String getNameFromId(String userId);

    // insert user into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE) //  might need to choose another onConflictStrategy later
    void createUser(User user);
}
