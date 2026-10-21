package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String phone;
    private String city;
    private String bio;
    private String designation;
    private String alternatePhone;
    private String businessName;
    private String businessType;
    private String gstin;
    private String address;
    private String supportEmail;
}
