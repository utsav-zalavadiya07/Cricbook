import api from "@/services/api";
import { BroadcastRequest } from "../types/notification.types";

export const getNotifications = (params?: {
  folder?: string;
  search?: string;
  channel?: string;
  type?: string;
  priority?: string;
  page?: number;
  size?: number;
}) => api.get("/notifications", { params });

export const getNotificationSummary = () =>
  api.get("/notifications/summary");

export const getNotificationAnalytics = () =>
  api.get("/notifications/analytics");

export const getActiveAnnouncements = () =>
  api.get("/notifications/announcements");

export const broadcastNotification = (payload: BroadcastRequest) =>
  api.post("/notifications/broadcast", payload);

export const previewTemplate = (payload: {
  templateName?: string;
  subject?: string;
  body?: string;
  channel?: string;
  variables?: Record<string, string>;
}) => api.post("/notifications/templates/preview", payload);

export const getNotificationEventCatalog = () =>
  api.get("/notifications/events/catalog");

export const markAsRead = (id: number) =>
  api.patch(`/notifications/${id}/read`);

export const markManyAsRead = (ids: number[]) =>
  api.patch("/notifications/bulk/read", { ids });

export const markAllAsRead = () =>
  api.patch("/notifications/read-all");

export const archiveNotification = (id: number) =>
  api.patch(`/notifications/${id}/archive`);

export const archiveManyNotifications = (ids: number[]) =>
  api.patch("/notifications/bulk/archive", { ids });

export const deleteNotification = (id: number) =>
  api.delete(`/notifications/${id}`);

export const deleteManyNotifications = (ids: number[]) =>
  api.delete("/notifications/bulk", { data: { ids } });

export const getPreferences = () =>
  api.get("/notifications/preferences");

export const updatePreference = (
  channel: string,
  enabled: boolean,
  frequency: string = "INSTANT",
  category?: string,
  categoryEnabled?: boolean
) =>
  api.put("/notifications/preferences", { channel, enabled, frequency, category, categoryEnabled });

export const trackMetric = (id: number, action: "view" | "click") =>
  api.patch(`/notifications/${id}/track?action=${action}`);

export const triggerEvent = (event: string, payload: Record<string, unknown> = {}) =>
  api.post("/notifications/trigger-event", { event, ...payload });

export const getRecommendations = () =>
  api.get("/recommendations");
