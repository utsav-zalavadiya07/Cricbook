package com.example.Cricbook.dto;

import com.example.Cricbook.enums.BattingStyle;
import com.example.Cricbook.enums.BowlingStyle;
import com.example.Cricbook.enums.PlayerPosition;
import lombok.Data;

@Data
public class UpdatePlayerProfileRequest {
    private String photoUrl;
    private String bio;
    private String city;
    private String preferredTime;
    private BattingStyle battingStyle;
    private BowlingStyle bowlingStyle;
    private PlayerPosition position;
    private String experience;
}
