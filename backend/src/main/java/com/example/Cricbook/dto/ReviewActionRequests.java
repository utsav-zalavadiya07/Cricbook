package com.example.Cricbook.dto;

import lombok.Getter;
import lombok.Setter;

public class ReviewActionRequests {
    @Getter
    @Setter
    public static class OwnerReplyRequest {
        private String reply;
    }

    @Getter
    @Setter
    public static class VoteRequest {
        private Boolean helpful;
    }

    @Getter
    @Setter
    public static class ReportRequest {
        private String reason;
    }

    @Getter
    @Setter
    public static class ModerationRequest {
        private String action;
        private String reason;
    }
}
