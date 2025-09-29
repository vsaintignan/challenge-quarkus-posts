package com.example.model;

import java.util.List;

public class MergedPost {
    public int id;
    public String title;
    public String body;
    public User author;
    public int commentCount;
    public List<Comment> comments;

    public MergedPost() {}

    public MergedPost(Post p, User u, List<Comment> comments) {
        this.id = p.id;
        this.title = p.title;
        this.body = p.body;
        this.author = u;
        this.comments = comments;
        this.commentCount = comments == null ? 0 : comments.size();
    }
}
