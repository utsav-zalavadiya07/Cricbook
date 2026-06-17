import * as Notifications from "expo-notifications";
import { Alert } from "react-native";

export interface FCMNotification {
  id: string;
  title: string;
  body: string;
  type: "BOOKING" | "PAYMENT" | "TOURNAMENT" | "MATCH" | "GENERAL";
  data?: any;
  timestamp: number;
}

// Global listeners registry
const listeners = new Set<(notification: FCMNotification) => void>();

// Configure default notification behaviors for foreground alerts
if (typeof Notifications.setNotificationHandler !== "undefined") {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowAlert: true,
      shouldPlaySound: true,
      shouldSetBadge: true,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });
}

export function initializeFCM(onNotificationReceived: (notification: FCMNotification) => void) {
  listeners.add(onNotificationReceived);

  // Subscribe to native Expo Notifications if running in native environment
  let subscription: any;
  if (typeof Notifications.addNotificationReceivedListener !== "undefined") {
    try {
      subscription = Notifications.addNotificationReceivedListener((notification) => {
        if (!notification || !notification.request) return;
        const content = notification.request.content || {};
        const { title, body, data } = content;
        const fcmNotif: FCMNotification = {
          id: notification.request.identifier || Math.random().toString(),
          title: title || "New Notification",
          body: body || "",
          type: (data?.type as any) || "GENERAL",
          data,
          timestamp: Date.now(),
        };
        listeners.forEach((listener) => listener(fcmNotif));
      });
    } catch (e) {
      console.warn("Failed to subscribe to foreground notifications:", e);
    }
  }

  return () => {
    listeners.delete(onNotificationReceived);
    if (subscription) {
      subscription.remove();
    }
  };
}

export async function simulateFCMAlert(
  type: FCMNotification["type"],
  title: string,
  message: string,
  data?: any
): Promise<void> {
  const fcmNotif: FCMNotification = {
    id: Math.random().toString(36).substring(2, 11),
    title,
    body: message,
    type,
    data,
    timestamp: Date.now(),
  };

  // 1. Notify all in-app state listeners (updates Redux alerts real-time)
  listeners.forEach((listener) => listener(fcmNotif));

  // 2. Schedule local Expo notification for background simulation
  if (typeof Notifications.scheduleNotificationAsync !== "undefined") {
    try {
      await Notifications.scheduleNotificationAsync({
        content: {
          title,
          body: message,
          data: { type, ...data },
        },
        trigger: null, // trigger immediately
      });
    } catch (e) {
      // Fallback to React Native Alert if Expo Notifications is not fully configured in active environment
      Alert.alert(title, message);
    }
  } else {
    Alert.alert(title, message);
  }
}

export function setupNotificationHandlers(
  onNavigate: (type: FCMNotification["type"], data?: any) => void
) {
  let responseSubscription: any;

  if (typeof Notifications.addNotificationResponseReceivedListener !== "undefined") {
    try {
      responseSubscription = Notifications.addNotificationResponseReceivedListener((response) => {
        if (!response || !response.notification || !response.notification.request) return;
        const content = response.notification.request.content || {};
        const data = content.data;
        const type = (data?.type as FCMNotification["type"]) || "GENERAL";
        onNavigate(type, data);
      });
    } catch (e) {
      console.warn("Failed to subscribe to notification click deep-link actions:", e);
    }
  }

  return () => {
    if (responseSubscription) {
      responseSubscription.remove();
    }
  };
}
