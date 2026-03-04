package com.bizhub.model.investissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NegotiationMessage {

    private Integer messageId;
    private Integer negotiationId;
    private Integer senderId;
    private String message;
    private String messageType; // text, offer, counter_offer, ai_suggestion
    private BigDecimal proposedAmount;
    private String sentiment;
    private LocalDateTime createdAt;

    private String senderName;
    private String senderAvatarUrl;

    public NegotiationMessage() {}

    public NegotiationMessage(Integer negotiationId, Integer senderId, String message, String messageType) {
        this.negotiationId = negotiationId;
        this.senderId = senderId;
        this.message = message;
        this.messageType = messageType;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }

    public Integer getNegotiationId() { return negotiationId; }
    public void setNegotiationId(Integer negotiationId) { this.negotiationId = negotiationId; }

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public BigDecimal getProposedAmount() { return proposedAmount; }
    public void setProposedAmount(BigDecimal proposedAmount) { this.proposedAmount = proposedAmount; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderAvatarUrl() { return senderAvatarUrl; }
    public void setSenderAvatarUrl(String senderAvatarUrl) { this.senderAvatarUrl = senderAvatarUrl; }
}

