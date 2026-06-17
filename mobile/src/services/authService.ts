import AsyncStorage from "@react-native-async-storage/async-storage";
import * as LocalAuthentication from "expo-local-authentication";
import { apiRequest, clearStoredSession, isJwtExpired, refreshMobileSession } from "../api/client";
import { LoginResponse, Session } from "../types/models";

const SESSION_KEYS = ["token", "refreshToken", "role", "user"] as const;

export async function login(loginId: string, password: string) {
  const response = await apiRequest<LoginResponse>("/auth/login", {
    method: "POST",
    body: { username: loginId.trim(), password },
  });
  const token = response.accessToken || response.token;
  if (!token) {
    throw new Error("Token missing from login response");
  }
  await AsyncStorage.setItem("token", token);
  if (response.refreshToken) await AsyncStorage.setItem("refreshToken", response.refreshToken);
  if (response.role) await AsyncStorage.setItem("role", response.role);
  if (response.user) await AsyncStorage.setItem("user", JSON.stringify(response.user));
  return {
    token,
    refreshToken: response.refreshToken,
    role: response.role,
    user: response.user,
  } satisfies Session;
}

export async function logout() {
  const refreshToken = await AsyncStorage.getItem("refreshToken");
  await apiRequest("/auth/logout", {
    method: "POST",
    body: refreshToken ? { refreshToken } : undefined,
    skipAuthRefresh: true,
  }).catch(() => undefined);
  await AsyncStorage.multiRemove(["biometricEnabled"]);
  await clearStoredSession();
}

export async function getStoredSession(): Promise<Session | null> {
  const [token, refreshToken, role, user] = await AsyncStorage.multiGet(SESSION_KEYS).then((entries) => entries.map((entry) => entry[1]));
  if (!token) return null;
  if (isJwtExpired(token)) {
    const refreshedToken = refreshToken ? await refreshMobileSession() : null;
    if (!refreshedToken) {
      return null;
    }

    const [nextToken, nextRefreshToken, nextRole, nextUser] = await AsyncStorage.multiGet(SESSION_KEYS).then((entries) => entries.map((entry) => entry[1]));
    if (!nextToken) {
      return null;
    }
    return {
      token: nextToken,
      refreshToken: nextRefreshToken || undefined,
      role: nextRole || undefined,
      user: nextUser ? JSON.parse(nextUser) : undefined,
    };
  }

  return {
    token,
    refreshToken: refreshToken || undefined,
    role: role || undefined,
    user: user ? JSON.parse(user) : undefined,
  };
}

export async function enableBiometricLogin() {
  const compatible = await LocalAuthentication.hasHardwareAsync();
  const enrolled = await LocalAuthentication.isEnrolledAsync();
  if (!compatible || !enrolled) {
    throw new Error("Biometric login is not available");
  }
  await AsyncStorage.setItem("biometricEnabled", "true");
}

export async function unlockWithBiometrics() {
  const enabled = await AsyncStorage.getItem("biometricEnabled");
  if (enabled !== "true") return false;
  const result = await LocalAuthentication.authenticateAsync({ promptMessage: "Unlock Cricbook" });
  return result.success;
}

export async function isBiometricEnabled() {
  return (await AsyncStorage.getItem("biometricEnabled")) === "true";
}
