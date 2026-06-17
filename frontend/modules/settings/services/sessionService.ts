import api from "@/services/api";

export type UserSession = {
  sessionId: string;
  deviceName: string;
  browser: string;
  os: string;
  ipAddress: string;
  loginTime: string;
  lastActivity: string;
  expiresAt: string;
  current: boolean;
  rememberMe: boolean;
};

export type SecuritySessionsResponse = {
  activeCount: number;
  sessions: UserSession[];
};

export const getSecuritySessions = () => api.get<SecuritySessionsResponse>("/security/sessions");

export const revokeSecuritySession = (sessionId: string) =>
  api.delete(`/security/sessions/${sessionId}`, {
    notification: {
      category: "security",
      success: {
        title: "Session Revoked",
        message: "The selected device has been signed out.",
      },
    },
  });

export const logoutAllSecuritySessions = () =>
  api.post("/auth/logout-all", undefined, {
    notification: {
      category: "security",
      success: {
        title: "Devices Signed Out",
        message: "All other active sessions have been revoked.",
      },
    },
  });
