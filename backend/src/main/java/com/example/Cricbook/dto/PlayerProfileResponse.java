package com.example.Cricbook.dto;

import com.example.Cricbook.enums.BattingStyle;
import com.example.Cricbook.enums.BowlingStyle;
import com.example.Cricbook.enums.PlayerPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProfileResponse {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String photoUrl;
    private String bio;
    private String city;
    private String preferredTime;
    private BattingStyle battingStyle;
    private BowlingStyle bowlingStyle;
    private PlayerPosition position;
    private String experience;
    private Integer matches;
    private Integer wins;
    private Integer mvp;
    private Double rating;
    private Integer profileCompletionPercentage;
}
