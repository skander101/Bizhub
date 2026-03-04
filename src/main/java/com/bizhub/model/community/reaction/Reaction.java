package com.bizhub.model.community.reaction;

public class Reaction {
    private int reactionId;
    private int postId;
    private int userId;
    private String type;

    public Reaction() {}
    public Reaction(int postId, int userId, String type) {
        this.postId = postId;
        this.userId = userId;
        this.type = type;
    }

    public int getReactionId() { return reactionId; }
    public void setReactionId(int reactionId) { this.reactionId = reactionId; }
    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

