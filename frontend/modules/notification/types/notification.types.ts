export interface Notification {
  id: number;
  title: string;
  message: string;
  status: "PENDING" | "SENT" | "FAILED" | "READ" | "ARCHIVED" | string;
  deliveryStatus?: "PENDING" | "SENT" | "FAILED" | string;
  read?: boolean;
  archived?: boolean;
  channel?: "EMAIL" | "PUSH" | "SMS" | "WHATSAPP" | string;
  category?: "BOOKING" | "PAYMENT" | "CANCELLATION" | "TEAM" | "MATCH" | "TOURNAMENT" | "REVIEW" | "SOCIAL" | "MARKETING" | "GENERAL" | string;
  type?: "BOOKING" | "PAYMENT" | "CANCELLATION" | "TEAM" | "MATCH" | "TOURNAMENT" | "REVIEW" | "SOCIAL" | "MARKETING" | "GENERAL" | string;
  priority?: "LOW" | "NORMAL" | "HIGH" | "CRITICAL" | string;
  campaignId?: number | null;
  deepLink?: string | null;
  retryCount?: number;
  createdAt: string;
  readAt?: string | null;
  archivedAt?: string | null;
}

export interface NotificationSummary {
  total: number;
  unread: number;
  read: number;
  archived: number;
  pending: number;
  failed: number;
  byChannel: Record<string, number>;
  byCategory: Record<string, number>;
}

export interface NotificationPreference {
  id?: number;
  channel: "EMAIL" | "PUSH" | "SMS" | "WHATSAPP" | string;
  enabled: boolean;
  frequency: "INSTANT" | "DAILY" | "WEEKLY" | string;
  disabledCategories?: string[];
  updatedAt?: string;
}

export interface NotificationAnalytics {
  total: number;
  sent: number;
  pending: number;
  failed: number;
  read: number;
  archived: number;
  views: number;
  clicks: number;
  conversions: number;
  deliveryRate: number;
  openRate: number;
  clickRate: number;
  byChannel: Record<string, number>;
  byCategory: Record<string, number>;
}

export interface NotificationAnnouncement {
  id: number;
  title: string;
  message: string;
  priority: string;
  displayLocation: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface BroadcastRequest {
  title: string;
  message: string;
  channels?: string[];
  targetRole?: string;
  targetUserIds?: number[];
  category?: string;
  priority?: string;
  deepLink?: string;
  campaignId?: number;
}

export interface BroadcastResponse {
  recipientCount: number;
  queuedCount: number;
  channels: string[];
}
