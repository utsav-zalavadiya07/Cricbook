"use client";

import { useCallback, useState } from "react";
import { getMyTurfs, getTurfSlots } from "../services/turfService";
import { Turf, Slot } from "../types/turf.types";

function apiErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string } } };
  return apiError.response?.data?.message || fallback;
}

export function useMyTurfs() {
  const [turfs, setTurfs] = useState<Turf[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const fetchTurfs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getMyTurfs();
      setTurfs(res.data);
    } catch (error) {
      setError(apiErrorMessage(error, "Failed to load turfs"));
    } finally {
      setLoading(false);
    }
  }, []);

  return { turfs, loading, error, fetchTurfs };
}

export function useTurfSlots(turfId: number) {
  const [slots, setSlots] = useState<Slot[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchSlots = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getTurfSlots(turfId);
      setSlots(res.data);
    } finally {
      setLoading(false);
    }
  }, [turfId]);

  return { slots, loading, fetchSlots };
}
