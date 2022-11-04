package com.example.hoply;

import androidx.room.*;
import androidx.annotation.NonNull;

/*
 * Class representing comments
 */
@Entity(tableName = "comments", primaryKeys = {"userId", "postId", "stamp"})
public class Comment {
    @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE)
    @NonNull
    @ColumnInfo(name = "userId")
    public String userId;

    @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "postId", onDelete = ForeignKey.CASCADE)
    @ColumnInfo(name = "postId")
    public int postId;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "stamp")
    public long stamp;

    /*
     * Constructor
     */
    public Comment(String content, @NonNull String userId, int postId, long stamp){
        this.userId = userId;
        this.postId = postId;
        this.content = content;
        this.stamp = stamp;
    }
}
