import AsyncStorage from "@react-native-async-storage/async-storage";
import { apiRequest } from "../api/client";
import { Booking, PublicMatch, Turf, UserProfile } from "../types/models";

const CACHE_KEYS = {
  PROFILE: "cricbook_cache_profile",
  BOOKINGS: "cricbook_cache_bookings",
  FAVORITES: "cricbook_cache_favorites",
  HISTORY: "cricbook_cache_history",
  SYNC_QUEUE: "cricbook_cache_sync_queue",
  NETWORK_STATUS: "cricbook_network_status",
};

export interface SyncAction {
  id: string;
  type: string;
  path: string;
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: any;
  timestamp: number;
}

export async function cacheProfile(profile: UserProfile): Promise<void> {
  await AsyncStorage.setItem(CACHE_KEYS.PROFILE, JSON.stringify(profile));
}

export async function getCachedProfile(): Promise<UserProfile | null> {
  const data = await AsyncStorage.getItem(CACHE_KEYS.PROFILE);
  return data ? JSON.parse(data) : null;
}

export async function cacheBookings(bookings: Booking[]): Promise<void> {
  await AsyncStorage.setItem(CACHE_KEYS.BOOKINGS, JSON.stringify(bookings));
}

export async function getCachedBookings(): Promise<Booking[]> {
  const data = await AsyncStorage.getItem(CACHE_KEYS.BOOKINGS);
  return data ? JSON.parse(data) : [];
}

export async function cacheFavorites(favorites: Turf[]): Promise<void> {
  await AsyncStorage.setItem(CACHE_KEYS.FAVORITES, JSON.stringify(favorites));
}

export async function getCachedFavorites(): Promise<Turf[]> {
  const data = await AsyncStorage.getItem(CACHE_KEYS.FAVORITES);
  return data ? JSON.parse(data) : [];
}

export async function cacheHistory(history: PublicMatch[]): Promise<void> {
  await AsyncStorage.setItem(CACHE_KEYS.HISTORY, JSON.stringify(history));
}

export async function getCachedHistory(): Promise<PublicMatch[]> {
  const data = await AsyncStorage.getItem(CACHE_KEYS.HISTORY);
  return data ? JSON.parse(data) : [];
}

export async function enqueueSyncAction(
  type: string,
  path: string,
  method: SyncAction["method"],
  body?: any
): Promise<void> {
  const queue = await getSyncQueue();
  const newAction: SyncAction = {
    id: Math.random().toString(36).substring(2, 11),
    type,
    path,
    method,
    body,
    timestamp: Date.now(),
  };
  queue.push(newAction);
  await AsyncStorage.setItem(CACHE_KEYS.SYNC_QUEUE, JSON.stringify(queue));
}

export async function getSyncQueue(): Promise<SyncAction[]> {
  const data = await AsyncStorage.getItem(CACHE_KEYS.SYNC_QUEUE);
  return data ? JSON.parse(data) : [];
}

export async function dequeueSyncAction(id: string): Promise<void> {
  const queue = await getSyncQueue();
  const nextQueue = queue.filter((act) => act.id !== id);
  await AsyncStorage.setItem(CACHE_KEYS.SYNC_QUEUE, JSON.stringify(nextQueue));
}

export async function setOfflineMode(offline: boolean): Promise<void> {
  await AsyncStorage.setItem(CACHE_KEYS.NETWORK_STATUS, offline ? "offline" : "online");
}

export async function isOfflineMode(): Promise<boolean> {
  const status = await AsyncStorage.getItem(CACHE_KEYS.NETWORK_STATUS);
  return status === "offline";
}

export async function checkConnectivity(): Promise<boolean> {
  if (await isOfflineMode()) return false;
  try {
    const response = await fetch("http://192.168.29.175:8080/api/v1/platform/health", {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    return response.ok;
  } catch {
    return false;
  }
}

let isSyncing = false;
let offlineSyncInterval: ReturnType<typeof setInterval> | null = null;

export async function syncOfflineQueue(force = false): Promise<{ success: boolean; syncedCount: number }> {
  if (isSyncing) return { success: false, syncedCount: 0 };
  isSyncing = true;

  try {
    const isConnected = force || (await checkConnectivity());
    if (!isConnected) {
      isSyncing = false;
      return { success: false, syncedCount: 0 };
    }

    const queue = await getSyncQueue();
    if (queue.length === 0) {
      isSyncing = false;
      return { success: true, syncedCount: 0 };
    }

    let syncedCount = 0;
    for (const action of queue) {
      try {
        await apiRequest(action.path, {
          method: action.method,
          body: action.body,
        });
        await dequeueSyncAction(action.id);
        syncedCount++;
      } catch (err: any) {
        // If the error is a network connectivity failure, stop further execution
        if (err.message && (err.message.includes("Network") || err.message.includes("fetch"))) {
          break;
        }
        // For standard 400 Bad Request or business logic failures, log and discard from queue to avoid blockages
        console.warn(`Sync failed for action ${action.id} (${action.type}):`, err);
        await dequeueSyncAction(action.id);
      }
    }

    isSyncing = false;
    return { success: true, syncedCount };
  } catch (err) {
    isSyncing = false;
    return { success: false, syncedCount: 0 };
  }
}

export function startOfflineSync(intervalMs = 60000): void {
  if (offlineSyncInterval || typeof setInterval === "undefined") {
    return;
  }

  offlineSyncInterval = setInterval(async () => {
    try {
      const isConnected = await checkConnectivity();
      if (isConnected) {
        await syncOfflineQueue(true);
      }
    } catch {
      // Quiet fail in background
    }
  }, intervalMs);
}

export function stopOfflineSync(): void {
  if (!offlineSyncInterval || typeof clearInterval === "undefined") {
    return;
  }

  clearInterval(offlineSyncInterval);
  offlineSyncInterval = null;
}
