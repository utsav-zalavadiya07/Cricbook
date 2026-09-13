import { Notification } from "../types/notification.types";

export const isNotificationRead = (notification: Notification) =>
  Boolean(notification.read) || notification.status === "READ";

export const isNotificationArchived = (notification: Notification) =>
  Boolean(notification.archived) || notification.status === "ARCHIVED";

