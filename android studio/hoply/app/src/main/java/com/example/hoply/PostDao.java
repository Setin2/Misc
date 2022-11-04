package com.example.hoply;

import androidx.room.*;
import java.util.List;

@Dao
public interface PostDao {
    // return a list of all posts
    @Query("SELECT * FROM posts ORDER BY stamp DESC LIMIT 200")
    List<Post> getAll();

    // return true if the given post id can be found in the database
    @Query("SELECT EXISTS(SELECT * FROM posts WHERE id = :postId)")
    boolean postExists(int postId);

    // delete a post from the database
    @Delete
    void deletePost(Post post);

    // insert a new post into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE) //  might need to choose another onConflictStrategy later
    void createPost(Post post);
}
