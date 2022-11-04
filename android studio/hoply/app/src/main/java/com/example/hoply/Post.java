package com.example.hoply;

import androidx.room.*;

/*
 * Class representing posts
 */
@Entity(tableName = "posts")
public class Post {
    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE)
    @ColumnInfo(name = "userId")
    public String userId;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "stamp")
    public long stamp;

    /*
     * Constructor
     */
    public Post(int id, String content, String userId, long stamp){
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.stamp = stamp;
    }
}
