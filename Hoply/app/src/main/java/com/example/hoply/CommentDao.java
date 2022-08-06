package com.example.hoply;

import androidx.room.*;
import java.util.List;

@Dao
public interface CommentDao {
    // delete a comment from the database
    @Delete
    void deleteComment(Comment comment);

    // return all comments that belong to the post with this id
    @Query("SELECT * FROM comments WHERE postId = :postId")
    List<Comment> getAllCommentsFromPost(int postId);

    // return true if the given comment can be found in the database
    @Query("SELECT EXISTS(SELECT * FROM comments WHERE userId = :userId AND postId = :postId AND stamp = :stamp)")
    boolean commentExists(String userId, int postId, long stamp);

    // return the number of all comments that have been made by this commentor and belong to a post made by this poster
    @Query("SELECT COUNT(*) FROM comments WHERE userId = :commentorId AND postId IN (SELECT id FROM posts WHERE userId = :posterId)")
    int topFan(String commentorId, String posterId);

    // return the number of comments with the given post-id
    @Query("SELECT COUNT(*) FROM comments WHERE postId =:postId")
    int numberOfCommentsOnThisPost(int postId);

    // insert new comment into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE) //  might need to choose another onConflictStrategy later
    void createComment(Comment comment);
}
