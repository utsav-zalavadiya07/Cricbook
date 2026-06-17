package com.example.Cricbook.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    private Long turfId;
    private Integer rating;
    private String title;
    private String comment;
}
