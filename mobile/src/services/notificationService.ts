import * as Notifications from "expo-notifications";
import { apiRequest } from "../api/client";

export async function registerForPushNotifications() {
  const permission = await Notifications.requestPermissionsAsync();
  if (!permission.granted) {
    throw new Error("Notification permission denied");
  }
  return "local-notifications-enabled";
}

export function getNotifications() {
  return apiRequest<unknown[]>("/notifications");
}
