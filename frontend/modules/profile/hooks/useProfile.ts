import { useEffect, useState, useCallback } from "react";
import { useAuthStore } from "@/store/authStore";
import { getAdminAnalytics, getAdminReviews, type AdminExecutiveAnalytics } from "@/modules/admin/services/adminService";
import type { Booking } from "@/modules/booking/types/booking.types";
import { getMyProfileMedia } from "@/modules/media/services/mediaService";
import { getOwnerBookings, getOwnerDashboard, getOwnerReviews, getOwnerTurfs } from "@/modules/owner/services/ownerService";
import type { Review } from "@/modules/review/types/review.types";
import type { Turf } from "@/modules/turf/types/turf.types";
import type { User } from "../types/profile.types";
import {
  getProfile,
  getBookings,
  getReviews,
} from "../services/profileService";

type ProfileUser = User & {
  photoUrl?: string;
  roles?: string[];
};

type OwnerDashboardStats = {
  todayBookings?: number;
  monthlyRevenue?: number;
  occupancyPercent?: number;
  cancellationPercent?: number;
  activeTurfs?: number;
  inactiveTurfs?: number;
  maintenanceTurfs?: number;
};

export default function useProfile() {
  const token = useAuthStore((s) => s.token);
  const storeRole = useAuthStore((s) => s.role);
  const [user, setUser] = useState<ProfileUser | null>(null);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [adminAnalytics, setAdminAnalytics] = useState<AdminExecutiveAnalytics | undefined>(undefined);
  const [ownerDashboard, setOwnerDashboard] = useState<OwnerDashboardStats | undefined>(undefined);
  const [ownerTurfs, setOwnerTurfs] = useState<Turf[]>([]);
  const [profileRole, setProfileRole] = useState<string | null>(storeRole);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    if (!token) {
      setUser(null);
      setBookings([]);
      setReviews([]);
      setAdminAnalytics(undefined);
      setOwnerDashboard(undefined);
      setOwnerTurfs([]);
      setProfileRole(null);
      setError("Not authenticated");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const [u, mediaRes] = await Promise.allSettled([getProfile(), getMyProfileMedia()]);
      if (u.status !== "fulfilled") {
        throw u.reason;
      }
      const nextUser = {
        ...u.value.data,
        photoUrl: mediaRes.status === "fulfilled" ? mediaRes.value.data.imageUrl : u.value.data?.photoUrl,
      };
      setUser(nextUser);
      const roles = Array.isArray(u.value.data?.roles) ? u.value.data.roles : [];
      const role = storeRole || (roles.includes("ROLE_ADMIN") ? "ADMIN" : roles.includes("ROLE_OWNER") ? "OWNER" : "USER");
      setProfileRole(role);

      if (role === "ADMIN") {
        const [analyticsRes, reviewsRes] = await Promise.allSettled([getAdminAnalytics(), getAdminReviews()]);
        setAdminAnalytics(analyticsRes.status === "fulfilled" ? analyticsRes.value.data : undefined);
        setReviews(reviewsRes.status === "fulfilled" ? reviewsRes.value.data : []);
        setBookings([]);
        setOwnerDashboard(undefined);
        setOwnerTurfs([]);
      } else if (role === "OWNER") {
        const [dashboardRes, turfsRes, bookingsRes, reviewsRes] = await Promise.allSettled([
          getOwnerDashboard(),
          getOwnerTurfs(),
          getOwnerBookings(),
          getOwnerReviews(),
        ]);
        setOwnerDashboard(dashboardRes.status === "fulfilled" ? dashboardRes.value.data : undefined);
        setOwnerTurfs(turfsRes.status === "fulfilled" ? turfsRes.value.data : []);
        setBookings(bookingsRes.status === "fulfilled" ? bookingsRes.value.data : []);
        setReviews(reviewsRes.status === "fulfilled" ? reviewsRes.value.data : []);
        setAdminAnalytics(undefined);
      } else {
        const [bookingsRes, reviewsRes] = await Promise.allSettled([getBookings(), getReviews()]);
        setBookings(bookingsRes.status === "fulfilled" ? bookingsRes.value.data : []);
        setReviews(reviewsRes.status === "fulfilled" ? reviewsRes.value.data : []);
        setAdminAnalytics(undefined);
        setOwnerDashboard(undefined);
        setOwnerTurfs([]);
      }
    } catch (error) {
      console.error("Failed to load profile data", error);
      setUser(null);
      setError("Session expired. Please login again.");
    } finally {
      setLoading(false);
    }
  }, [storeRole, token]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadData(), 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  return { user, bookings, reviews, adminAnalytics, ownerDashboard, ownerTurfs, profileRole, error, loading, refresh: loadData };
}
