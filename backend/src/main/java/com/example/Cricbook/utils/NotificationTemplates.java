package com.example.Cricbook.utils;

import java.time.LocalDate;

public class NotificationTemplates {

    public static String bookingConfirmed(LocalDate date) {
        return "Your booking for " + date + " is confirmed.";
    }

    public static String bookingCancelled() {
        return "Your booking has been cancelled.";
    }

    public static String bookingExpired() {
        return "Your booking expired due to no payment.";
    }

    public static String paymentSuccess(Long bookingId) {
        return "Your payment for booking ID " + bookingId + " is successful.";
    }

    public static String paymentFailed() {
        return "Your payment failed. Please try again.";
    }
}
