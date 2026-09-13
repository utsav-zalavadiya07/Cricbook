"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import api from "@/services/api";
import type { ReactNode } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useConfirmDialog } from "@/components/ui/ConfirmDialog";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getOwnerTurfs } from "@/modules/owner/services/ownerService";
import {
  applySlotPricing,
  blockSlots,
  createSlots,
  disableSlot,
  getTurfAvailability,
  getTurfSlots,
  unblockSlots,
  updateSlot,
} from "@/modules/turf/services/turfService";
import { Slot, Turf } from "@/modules/turf/types/turf.types";
import { useToast } from "@/modules/ui/ToastProvider";

type ViewKey = "calendar" | "disable" | "price" | "holiday" | "generate";
type CalendarMode = "day" | "week" | "month";
type SlotStatusFilter = "all" | "active" | "disabled";
type SlotTimeBand = "all" | "morning" | "afternoon" | "evening" | "night";
type CalendarSlotState = "available" | "booked" | "blocked" | "expired" | "locked";

type AvailabilitySlot = {
  id?: number;
  slotId: number;
  startTime: string;
  endTime: string;
  price: number;
  date: string;
  available?: boolean;
  blocked?: boolean;
  isAvailable?: boolean;
  isBlocked?: boolean;
  locked?: boolean;
  status?: string;
};

const views: { key: ViewKey; label: string }[] = [
  { key: "calendar", label: "Calendar View" },
  { key: "disable", label: "Disable Slot" },
  { key: "price", label: "Edit Slot Price" },
  { key: "holiday", label: "Special Day Pricing" },
  { key: "generate", label: "Bulk Generate" },
];

const inputClass =
  "theme-input w-full rounded-xl px-3 py-2 text-sm outline-none transition focus:border-sky-400 focus:ring-2 focus:ring-sky-500/10";

const today = () => new Date().toLocaleDateString("en-CA");
const hourRowHeight = 76;
const timelineHeaderHeight = 34;

const getApiMessage = (error: unknown, fallback: string) => {
  if (
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    typeof (error as { response?: { data?: { message?: unknown } } }).response?.data?.message === "string"
  ) {
    return (error as { response: { data: { message: string } } }).response.data.message;
  }
  return fallback;
};

const parseTimeToMinutes = (time: string) => {
  const [hours = 0, minutes = 0] = time.split(":").map(Number);
  return hours * 60 + minutes;
};

const getEndMinutes = (startTime: string, endTime: string) => {
  const start = parseTimeToMinutes(startTime);
  const end = parseTimeToMinutes(endTime);
  return end <= start ? end + 24 * 60 : end;
};

const formatTimeLabel = (time: string) => {
  const [hours = "00", minutes = "00"] = time.split(":");
  return `${hours.padStart(2, "0")}:${minutes.padStart(2, "0")}`;
};

const formatHourLabel = (minutes: number) => {
  const hour = Math.floor(minutes / 60);
  return `${String(hour).padStart(2, "0")}:00`;
};

const formatDateTitle = (value: string) => {
  const parsed = new Date(`${value}T00:00:00`);
  return new Intl.DateTimeFormat("en-IN", {
    weekday: "long",
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(parsed);
};

const toDateKey = (dateValue: Date) => dateValue.toLocaleDateString("en-CA");

const addDays = (value: string, days: number) => {
  const dateValue = new Date(`${value}T00:00:00`);
  dateValue.setDate(dateValue.getDate() + days);
  return toDateKey(dateValue);
};

const getWeekDates = (value: string) => {
  const dateValue = new Date(`${value}T00:00:00`);
  const day = dateValue.getDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  const monday = addDays(value, mondayOffset);
  return Array.from({ length: 7 }, (_, index) => addDays(monday, index));
};

const getMonthDates = (value: string) => {
  const dateValue = new Date(`${value}T00:00:00`);
  const first = new Date(dateValue.getFullYear(), dateValue.getMonth(), 1);
  const last = new Date(dateValue.getFullYear(), dateValue.getMonth() + 1, 0);
  return Array.from({ length: last.getDate() }, (_, index) => toDateKey(new Date(first.getFullYear(), first.getMonth(), index + 1)));
};

const getCalendarDates = (value: string, mode: CalendarMode) => {
  if (mode === "week") return getWeekDates(value);
  if (mode === "month") return getMonthDates(value);
  return [value];
};

const formatShortDate = (value: string) => {
  const parsed = new Date(`${value}T00:00:00`);
  return new Intl.DateTimeFormat("en-IN", { weekday: "short", day: "2-digit", month: "short" }).format(parsed);
};

const slotTimeBands: Array<{ key: Exclude<SlotTimeBand, "all">; label: string; helper: string }> = [
  { key: "night", label: "Night", helper: "10 PM - 6 AM" },
  { key: "morning", label: "Morning", helper: "6 AM - 12 PM" },
  { key: "afternoon", label: "Afternoon", helper: "12 PM - 5 PM" },
  { key: "evening", label: "Evening", helper: "5 PM - 10 PM" },
];

const getSlotTimeBand = (slot: Slot): Exclude<SlotTimeBand, "all"> => {
  const minutes = parseTimeToMinutes(slot.startTime);
  if (minutes < 6 * 60 || minutes >= 22 * 60) return "night";
  if (minutes < 12 * 60) return "morning";
  if (minutes < 17 * 60) return "afternoon";
  if (minutes < 22 * 60) return "evening";
  return "night";
};

const slotsOverlap = (first: Slot, second: Slot) =>
  parseTimeToMinutes(first.startTime) < getEndMinutes(second.startTime, second.endTime) &&
  getEndMinutes(first.startTime, first.endTime) > parseTimeToMinutes(second.startTime);

const findActiveSlotConflict = (slot: Slot, allSlots: Slot[]) =>
  allSlots.find((candidate) => candidate.id !== slot.id && candidate.isActive && slotsOverlap(slot, candidate));

const getScheduleBounds = (items: AvailabilitySlot[]) => {
  if (items.length === 0) {
    return { start: 6 * 60, end: 22 * 60 };
  }

  const starts = items.map((item) => parseTimeToMinutes(item.startTime));
  const ends = items.map((item) => getEndMinutes(item.startTime, item.endTime));
  const start = Math.max(0, Math.floor(Math.min(...starts) / 60) * 60);
  const end = Math.min(24 * 60, Math.ceil(Math.max(...ends) / 60) * 60);
  return { start, end: Math.max(end, start + 60) };
};

const calendarSlotPalette: Record<
  CalendarSlotState,
  {
    label: string;
    metricText: string;
    blockClass: string;
    badgeClass: string;
    accentClass: string;
    summaryChipClass: string;
    selectedClass: string;
    selectedMetaClass: string;
  }
> = {
  available: {
    label: "Available",
    metricText: "slot-status-text-available",
    blockClass: "slot-status-available",
    badgeClass: "slot-status-badge-available",
    accentClass: "slot-status-accent-available",
    summaryChipClass: "slot-status-summary-available",
    selectedClass: "slot-status-selected-available",
    selectedMetaClass: "slot-status-text-available",
  },
  booked: {
    label: "Booked",
    metricText: "slot-status-text-booked",
    blockClass: "slot-status-booked",
    badgeClass: "slot-status-badge-booked",
    accentClass: "slot-status-accent-booked",
    summaryChipClass: "slot-status-summary-booked",
    selectedClass: "slot-status-selected-booked",
    selectedMetaClass: "slot-status-text-booked",
  },
  blocked: {
    label: "Blocked",
    metricText: "slot-status-text-blocked",
    blockClass: "slot-status-blocked",
    badgeClass: "slot-status-badge-blocked",
    accentClass: "slot-status-accent-blocked",
    summaryChipClass: "slot-status-summary-blocked",
    selectedClass: "slot-status-selected-blocked",
    selectedMetaClass: "slot-status-text-blocked",
  },
  expired: {
    label: "Past",
    metricText: "slot-status-text-expired",
    blockClass: "slot-status-expired",
    badgeClass: "slot-status-badge-expired",
    accentClass: "slot-status-accent-expired",
    summaryChipClass: "slot-status-summary-expired",
    selectedClass: "slot-status-selected-expired",
    selectedMetaClass: "slot-status-text-expired",
  },
  locked: {
    label: "Held",
    metricText: "slot-status-text-locked",
    blockClass: "slot-status-locked",
    badgeClass: "slot-status-badge-locked",
    accentClass: "slot-status-accent-locked",
    summaryChipClass: "slot-status-summary-locked",
    selectedClass: "slot-status-selected-locked",
    selectedMetaClass: "slot-status-text-locked",
  },
};

const getAvailabilityState = (item: AvailabilitySlot): CalendarSlotState => {
  const status = item.status?.trim().toUpperCase();
  if (status === "BLOCKED" || (item.blocked ?? item.isBlocked)) return "blocked";
  if (status === "LOCKED" || item.locked) return "locked";
  if (status === "BOOKED") return "booked";
  if (status === "EXPIRED" || status === "CLOSED" || status === "MAINTENANCE" || status === "UNAVAILABLE") return "expired";
  if (status === "AVAILABLE" || (item.available ?? item.isAvailable)) return "available";
  return "expired";
};

export default function OwnerSlotsPage() {
  const router = useRouter();
  const confirm = useConfirmDialog();
  const { showError, showWarning } = useToast();
  const searchParams = useSearchParams();
  const requestedTurfId = Number(searchParams.get("turfId")) || null;
  const [turfs, setTurfs] = useState<Turf[]>([]);
  const [selectedTurfId, setSelectedTurfId] = useState<number | null>(null);
  const [activeView, setActiveView] = useState<ViewKey>("calendar");
  const [calendarMode, setCalendarMode] = useState<CalendarMode>("day");
  const [slots, setSlots] = useState<Slot[]>([]);
  const [availability, setAvailability] = useState<AvailabilitySlot[]>([]);
  const [rangeAvailability, setRangeAvailability] = useState<Record<string, AvailabilitySlot[]>>({});
  const [selectedSlotIds, setSelectedSlotIds] = useState<number[]>([]);
  const [loadingTurfs, setLoadingTurfs] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [saving, setSaving] = useState(false);
  const [date, setDate] = useState(today());
  const [disableFilter, setDisableFilter] = useState<SlotStatusFilter>("all");
  const [disableTimeBand, setDisableTimeBand] = useState<SlotTimeBand>("all");
  const [disableSearch, setDisableSearch] = useState("");
  const [generateForm, setGenerateForm] = useState({
    startTime: "06:00",
    endTime: "22:00",
    slotDuration: 60,
    price: 500,
    replaceExisting: false,
    fullDay: false,
  });
  const [holidayForm, setHolidayForm] = useState({
    date: today(),
    price: 750,
    reason: "Holiday pricing",
  });
  const [enableAutoHoliday, setEnableAutoHoliday] = useState(false);
  const [pricingStrategy, setPricingStrategy] = useState("PERCENTAGE"); // PERCENTAGE, FIXED, MULTIPLIER
  const [pricingValue, setPricingValue] = useState(50);
  const [applyToSlotsMode, setApplyToSlotsMode] = useState("ALL");
  const [upcomingHolidays, setUpcomingHolidays] = useState<Array<{ id?: number; holidayName: string; holidayDate: string; holidayType: string }>>([]);
  const [excludedHolidays, setExcludedHolidays] = useState<string[]>([]);
  const [automationSaving, setAutomationSaving] = useState(false);

  const selectableTurfs = useMemo(
    () => turfs.filter((turf): turf is Turf & { id: number } => Number.isFinite(turf.id)),
    [turfs]
  );

  const selectedTurf = useMemo(
    () => turfs.find((turf) => turf.id === selectedTurfId),
    [selectedTurfId, turfs]
  );

  const selectedSlots = useMemo(
    () => slots.filter((slot) => selectedSlotIds.includes(slot.id)),
    [selectedSlotIds, slots]
  );

  const selectedAvailability = useMemo(
    () => availability.filter((item) => selectedSlotIds.includes(item.slotId)),
    [availability, selectedSlotIds]
  );

  const selectedAvailableCount = selectedAvailability.filter((item) => getAvailabilityState(item) === "available").length;
  const selectedBlockedCount = selectedAvailability.filter((item) => getAvailabilityState(item) === "blocked").length;

  const loadSlotData = useCallback(async (turfId: number, targetDate: string, mode: CalendarMode) => {
    if (!turfId) return;
    setLoadingSlots(true);
    try {
      const calendarDates = getCalendarDates(targetDate, mode);
      const [slotRes, availabilityResults] = await Promise.all([
        getTurfSlots(turfId),
        Promise.all(
          calendarDates.map((calendarDate) =>
            getTurfAvailability(turfId, calendarDate).then((res) => [calendarDate, res.data || []] as const)
          )
        ),
      ]);
      const nextRangeAvailability = Object.fromEntries(availabilityResults);
      setSlots(slotRes.data || []);
      setRangeAvailability(nextRangeAvailability);
      setAvailability(nextRangeAvailability[targetDate] || []);
      setSelectedSlotIds((current) =>
        current.filter((slotId) => (slotRes.data || []).some((slot: Slot) => slot.id === slotId))
      );
    } catch {
      setSlots([]);
      setAvailability([]);
      setRangeAvailability({});
    } finally {
      setLoadingSlots(false);
    }
  }, []);

  useEffect(() => {
    getOwnerTurfs()
      .then((res) => {
        const nextTurfs: Turf[] = res.data || [];
        setTurfs(nextTurfs);
        setSelectedTurfId((current) => {
          if (current) return current;
          if (requestedTurfId && nextTurfs.some((turf) => turf.id === requestedTurfId)) {
            return requestedTurfId;
          }
          return nextTurfs.find((turf) => turf.id)?.id ?? null;
        });
      })
      .catch(() => undefined)
      .finally(() => setLoadingTurfs(false));
  }, [requestedTurfId]);

  const loadHolidayAutomationData = useCallback(async (turfId: number) => {
    if (!turfId) return;
    try {
      const [ruleRes, holidaysRes] = await Promise.all([
        api.get(`/turfs/${turfId}/holiday-rule`),
        api.get(`/turfs/${turfId}/upcoming-holidays`),
      ]);
      const rule = ruleRes.data;
      if (rule) {
        setEnableAutoHoliday(rule.autoApply ?? false);
        setPricingStrategy(rule.pricingMode ?? "PERCENTAGE");
        setPricingValue(rule.value ?? 0);
        setApplyToSlotsMode(rule.applyTo ?? "ALL");
        setExcludedHolidays(rule.excludedHolidays ?? []);
        if (rule.applyTo === "SELECTED") {
          setSelectedSlotIds(rule.slotIds ?? []);
        }
      }
      setUpcomingHolidays(holidaysRes.data || []);
    } catch (err) {
      console.error("Error loading holiday automation data", err);
    }
  }, []);

  useEffect(() => {
    if (!selectedTurfId) return;
    const timer = window.setTimeout(() => {
      void loadSlotData(selectedTurfId, date, calendarMode);
      void loadHolidayAutomationData(selectedTurfId);
    }, 0);
    return () => window.clearTimeout(timer);
  }, [calendarMode, date, loadSlotData, loadHolidayAutomationData, selectedTurfId]);

  const setView = (view: ViewKey) => {
    setActiveView(view);
    setSelectedSlotIds([]);
  };

  const handleTurfChange = (value: string | number) => {
    const nextTurfId = Number(value);
    if (!Number.isFinite(nextTurfId)) return;
    setSelectedTurfId(nextTurfId);
    setSelectedSlotIds([]);
    router.replace(`/owner/slots?turfId=${nextTurfId}`, { scroll: false });
  };

  const toggleSelectedSlot = (slotId: number) => {
    setSelectedSlotIds((current) =>
      current.includes(slotId) ? current.filter((id) => id !== slotId) : [...current, slotId]
    );
  };

  const calculatedSlotCount = () => {
    const [startHour, startMinute] = (generateForm.fullDay ? "00:00" : generateForm.startTime).split(":").map(Number);
    const [endHour, endMinute] = (generateForm.fullDay ? "00:00" : generateForm.endTime).split(":").map(Number);
    const startTotal = startHour * 60 + startMinute;
    const endTotal = endHour * 60 + endMinute;
    const totalMinutes = endTotal <= startTotal ? endTotal + 24 * 60 - startTotal : endTotal - startTotal;
    return totalMinutes > 0 ? Math.floor(totalMinutes / generateForm.slotDuration) : 0;
  };

  const runAction = async (action: () => Promise<void>, onSuccess?: () => void) => {
    if (!selectedTurfId) return;
    setSaving(true);
    try {
      await action();
      await loadSlotData(selectedTurfId, date, calendarMode);
      onSuccess?.();
    } catch (e: unknown) {
      if (!(typeof e === "object" && e !== null && "response" in e)) {
        showError("Action failed", getApiMessage(e, "Please try again."));
      }
    } finally {
      setSaving(false);
    }
  };

  const handleGenerate = async () => {
    const count = calculatedSlotCount();
    if (!selectedTurfId || count <= 0) {
      showWarning("Invalid slot configuration", "Choose a valid start time, end time and duration.");
      return;
    }

    if (generateForm.replaceExisting) {
      const confirmed = await confirm({
        title: "Replace slot configuration?",
        description: (
          <span>
            This will archive the current regular slots for {selectedTurf?.name || "this turf"} and create {count} new slots.
            Future active bookings must be cleared before replacement.
          </span>
        ),
        confirmLabel: "Replace slots",
        cancelLabel: "Keep current slots",
        tone: "danger",
      });
      if (!confirmed) return;
    }

    void runAction(() =>
      createSlots(selectedTurfId, {
        ...generateForm,
        startTime: generateForm.fullDay ? "00:00" : generateForm.startTime,
        endTime: generateForm.fullDay ? "00:00" : generateForm.endTime,
      }).then(() => undefined)
    );
  };

  const handleToggleSlot = async (slot: Slot) => {
    if (!selectedTurfId) return;
    if (!slot.isActive) {
      const conflictingSlot = findActiveSlotConflict(slot, slots);
      if (conflictingSlot) {
        const message = `${formatTimeLabel(slot.startTime)} - ${formatTimeLabel(slot.endTime)} already overlaps active slot ${formatTimeLabel(conflictingSlot.startTime)} - ${formatTimeLabel(conflictingSlot.endTime)}. Disable the conflicting slot first or edit the timing.`;
        showWarning("Slot conflict", message);
        return;
      }
    }

    if (slot.isActive) {
      const confirmed = await confirm({
        title: "Disable this regular slot?",
        description: (
          <span>
            {formatTimeLabel(slot.startTime)} - {formatTimeLabel(slot.endTime)} will be hidden from every future booking day for {selectedTurf?.name || "this turf"}.
            Use Calendar View if you only need to block one date.
          </span>
        ),
        confirmLabel: "Disable slot",
        cancelLabel: "Keep active",
        tone: "danger",
      });
      if (!confirmed) return;
    }

    void runAction(() =>
      slot.isActive
        ? disableSlot(selectedTurfId, slot.id).then(() => undefined)
        : updateSlot(selectedTurfId, slot.id, { isActive: true }).then(() => undefined)
    );
  };

  const handlePriceSave = (slot: Slot, price: number) => {
    if (!selectedTurfId || !Number.isFinite(price) || price <= 0) {
      showWarning("Invalid price", "Enter a valid slot price.");
      return;
    }
    void runAction(() => updateSlot(selectedTurfId, slot.id, { price }).then(() => undefined));
  };

  const handleHolidayPricing = () => {
    if (!selectedTurfId || selectedSlotIds.length === 0) {
      showWarning("No slots selected", "Select at least one slot for holiday pricing.");
      return;
    }
    void runAction(() =>
      applySlotPricing(selectedTurfId, {
        slotIds: selectedSlotIds,
        date: holidayForm.date,
        price: holidayForm.price,
        reason: holidayForm.reason,
      }).then(() => undefined)
    );
  };

  const handleSaveHolidayAutomation = async () => {
    if (!selectedTurfId) return;
    setAutomationSaving(true);
    try {
      const payload = {
        autoApply: enableAutoHoliday,
        pricingMode: pricingStrategy,
        value: pricingValue,
        applyTo: applyToSlotsMode,
        excludedHolidays: excludedHolidays,
        slotIds: applyToSlotsMode === "SELECTED" ? selectedSlotIds : [],
        isActive: true,
      };
      await api.post(`/turfs/${selectedTurfId}/holiday-rule`, payload);
      await loadHolidayAutomationData(selectedTurfId);
    } catch (err) {
      console.error("Error saving holiday pricing rule", err);
    } finally {
      setAutomationSaving(false);
    }
  };

  const handleBlockSelected = async () => {
    const openSlotIds = selectedAvailability
      .filter((item) => getAvailabilityState(item) === "available")
      .map((item) => item.slotId);

    if (!selectedTurfId || openSlotIds.length === 0) {
      showWarning("No available slots selected", "Select at least one available slot to block on this date.");
      return;
    }

    const confirmed = await confirm({
      title: "Block selected slots?",
      description: (
        <span>
          {openSlotIds.length} slot{openSlotIds.length === 1 ? "" : "s"} on {formatDateTitle(date)} will be unavailable for player bookings.
          Existing bookings are protected by the system.
        </span>
      ),
      confirmLabel: "Block slots",
      cancelLabel: "Keep available",
      tone: "danger",
    });
    if (!confirmed) return;

    void runAction(
      () => blockSlots(selectedTurfId, { date, slotIds: openSlotIds, blocked: true }).then(() => undefined),
      () => setSelectedSlotIds([])
    );
  };

  const handleUnblockSelected = async () => {
    const blockedSlotIds = selectedAvailability
      .filter((item) => getAvailabilityState(item) === "blocked")
      .map((item) => item.slotId);

    if (!selectedTurfId || blockedSlotIds.length === 0) {
      showWarning("No blocked slots selected", "Select at least one blocked slot to unblock.");
      return;
    }

    const confirmed = await confirm({
      title: "Unblock selected slots?",
      description: (
        <span>
          {blockedSlotIds.length} slot{blockedSlotIds.length === 1 ? "" : "s"} on {formatDateTitle(date)} will become available again if there are no bookings or turf restrictions.
        </span>
      ),
      confirmLabel: "Unblock slots",
      cancelLabel: "Keep blocked",
      tone: "owner",
    });
    if (!confirmed) return;

    void runAction(
      () => unblockSlots(selectedTurfId, { date, slotIds: blockedSlotIds }).then(() => undefined),
      () => setSelectedSlotIds([])
    );
  };

  const handleCalendarDateChange = (nextDate: string) => {
    setDate(nextDate);
    setAvailability(rangeAvailability[nextDate] || []);
    setSelectedSlotIds([]);
  };

  const renderSlotPicker = (emptyLabel = "No slots generated yet.") => (
    <div className="grid gap-2 grid-cols-1 sm:grid-cols-2 md:grid-cols-3">
      {slots.length === 0 && (
        <p className="theme-card theme-muted col-span-full rounded-xl border-dashed p-6 text-center text-sm">
          {emptyLabel}
        </p>
      )}
      {slots.map((slot) => {
        const selected = selectedSlotIds.includes(slot.id);
        return (
          <button
            key={slot.id}
            type="button"
            onClick={() => toggleSelectedSlot(slot.id)}
            className={`flex items-center justify-between rounded-xl border p-3 text-left transition duration-150 ${
              selected ? "border-sky-400 bg-sky-500/10" : "theme-card hover:border-sky-500/40"
            }`}
          >
            <span>
              <span className="theme-title block text-sm font-semibold">
                {slot.startTime} - {slot.endTime}
              </span>
              <span className="theme-muted text-xs">
                Rs {slot.price} | {slot.isActive ? "Active" : "Disabled"}
              </span>
            </span>
            <span className={`h-4 w-4 shrink-0 rounded border flex items-center justify-center transition-all ${
              selected ? "border-sky-400 bg-sky-500 text-white" : "theme-border bg-black/10"
            }`}>
              {selected && (
                <svg className="w-2.5 h-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="3">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              )}
            </span>
          </button>
        );
      })}
    </div>
  );

  const renderTabToolbar = ({
    title,
    subtitle,
    controls,
  }: {
    title: string;
    subtitle: string;
    controls?: ReactNode;
  }) => (
    <div className="theme-panel-soft border-b p-4 sm:p-5">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <h2 className="theme-title text-lg font-bold">{title}</h2>
          <p className="theme-muted mt-1 text-sm">{subtitle}</p>
        </div>
        {controls && (
          <div className="grid w-full gap-3 sm:grid-cols-2 xl:w-auto xl:grid-flow-col xl:auto-cols-[220px] xl:items-end">
            {controls}
          </div>
        )}
      </div>
    </div>
  );

  const renderLoadingSlots = () => (
    <p className="theme-muted p-10 text-center text-sm">Loading slots for {selectedTurf?.name || "selected turf"}...</p>
  );

  const renderActiveView = () => {
    if (!selectedTurfId) {
      return (
        <section className="theme-panel overflow-hidden rounded-2xl">
          {renderTabToolbar({
            title: views.find((view) => view.key === activeView)?.label || "Slot Manager",
            subtitle: loadingTurfs ? "Loading your turfs..." : "Add a turf first, then manage slots from this page.",
          })}
          <p className="theme-muted p-10 text-center text-sm">
            {loadingTurfs ? "Loading your turfs..." : "No turfs found. Add a turf first from My Turfs."}
          </p>
        </section>
      );
    }

    if (activeView === "generate") {
      const count = calculatedSlotCount();
      return (
        <section className="theme-panel overflow-hidden rounded-2xl">
          {renderTabToolbar({
            title: "Bulk Generate",
            subtitle: "Create the base slot template for the selected turf.",
          })}
          <div className="p-4 sm:p-5 space-y-6">
            <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-4">
              <Field label="Opening Time">
                <input
                  type="time"
                  className={inputClass}
                  value={generateForm.fullDay ? "00:00" : generateForm.startTime}
                  disabled={generateForm.fullDay}
                  onChange={(e) => setGenerateForm({ ...generateForm, startTime: e.target.value })}
                />
              </Field>
              <Field label="Closing Time">
                <input
                  type="time"
                  className={inputClass}
                  value={generateForm.fullDay ? "00:00" : generateForm.endTime}
                  disabled={generateForm.fullDay}
                  onChange={(e) => setGenerateForm({ ...generateForm, endTime: e.target.value })}
                />
              </Field>
              <Field label="Duration">
                <select className={inputClass} value={generateForm.slotDuration} onChange={(e) => setGenerateForm({ ...generateForm, slotDuration: Number(e.target.value) })}>
                  <option value={30}>30 minutes</option>
                  <option value={60}>60 minutes</option>
                  <option value={90}>90 minutes</option>
                  <option value={120}>120 minutes</option>
                </select>
              </Field>
              <Field label="Price">
                <input type="number" min={1} className={inputClass} value={generateForm.price} onChange={(e) => setGenerateForm({ ...generateForm, price: Number(e.target.value) })} />
              </Field>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <label className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition duration-150 ${
                generateForm.fullDay ? "border-sky-500/40 bg-sky-500/10" : "theme-card hover:border-sky-500/40"
              }`}>
                <input
                  type="checkbox"
                  className="mt-1 h-4 w-4 shrink-0 rounded border-slate-400 accent-sky-500"
                  checked={generateForm.fullDay}
                  onChange={(e) =>
                    setGenerateForm({
                      ...generateForm,
                      fullDay: e.target.checked,
                      startTime: e.target.checked ? "00:00" : generateForm.startTime,
                      endTime: e.target.checked ? "00:00" : generateForm.endTime,
                    })
                  }
                />
                <span>
                  <span className="theme-title block text-sm font-bold">24-hour schedule</span>
                  <span className="theme-muted mt-1 block text-xs leading-5">
                    Generate slots from midnight to midnight for turfs that operate all day.
                  </span>
                </span>
              </label>

              <label className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition duration-150 ${
                generateForm.replaceExisting ? "border-amber-500/40 bg-amber-500/10" : "theme-card hover:border-sky-500/40"
              }`}>
                <input
                  type="checkbox"
                  className="mt-1 h-4 w-4 shrink-0 rounded border-slate-400 accent-sky-500"
                  checked={generateForm.replaceExisting}
                  onChange={(e) => setGenerateForm({ ...generateForm, replaceExisting: e.target.checked })}
                />
                <span>
                  <span className="theme-title block text-sm font-bold">Replace existing slots</span>
                  <span className="theme-muted mt-1 block text-xs leading-5">
                    Archive current regular slots and create new ones. Useful when changing turf timetable hours.
                  </span>
                </span>
              </label>
            </div>

            <div className="flex flex-col gap-3 rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-emerald-700 dark:text-emerald-100 font-medium">
                {count} slots will be {generateForm.replaceExisting ? "used as the new regular configuration" : "generated"} for {selectedTurf?.name}.
              </p>
              <button disabled={saving || count === 0} onClick={() => void handleGenerate()} className="rounded-xl bg-emerald-500 hover:bg-emerald-400 px-5 py-2.5 text-sm font-bold text-white transition disabled:opacity-50">
                {generateForm.replaceExisting ? "Replace Slots" : "Generate Slots"}
              </button>
            </div>
          </div>
        </section>
      );
    }

    if (activeView === "price") {
      return (
        <section className="theme-panel overflow-hidden rounded-2xl">
          {renderTabToolbar({
            title: "Edit Slot Price",
            subtitle: "Update regular prices for generated slots.",
          })}
          <div className="grid gap-3 p-4 sm:p-5">
            {loadingSlots && renderLoadingSlots()}
            {!loadingSlots && slots.length === 0 && <p className="theme-card theme-muted rounded-xl border-dashed p-6 text-center text-sm">Generate slots first.</p>}
            {!loadingSlots && slots.map((slot) => (
              <PriceRow key={`${slot.id}-${slot.price}`} slot={slot} saving={saving} onSave={(price) => handlePriceSave(slot, price)} />
            ))}
          </div>
        </section>
      );
    }

    if (activeView === "disable") {
      const sortedSlots = [...slots].sort((a, b) => parseTimeToMinutes(a.startTime) - parseTimeToMinutes(b.startTime));
      const activeSlots = sortedSlots.filter((slot) => slot.isActive);
      const disabledSlots = sortedSlots.filter((slot) => !slot.isActive);
      const slotsMatchingStatus = sortedSlots.filter((slot) => {
        if (disableFilter === "active") return slot.isActive;
        if (disableFilter === "disabled") return !slot.isActive;
        return true;
      });
      const normalizedSearch = disableSearch.trim().toLowerCase();
      const visibleSlots = slotsMatchingStatus.filter((slot) => {
        const formattedStart = formatTimeLabel(slot.startTime);
        const formattedEnd = formatTimeLabel(slot.endTime);
        const haystack = [
          formattedStart,
          formattedEnd,
          `${formattedStart} - ${formattedEnd}`,
          slot.startTime,
          slot.endTime,
          slot.isActive ? "active" : "disabled",
          String(slot.price),
        ].join(" ").toLowerCase();
        const matchesSearch = !normalizedSearch || haystack.includes(normalizedSearch);
        const matchesBand = disableTimeBand === "all" || getSlotTimeBand(slot) === disableTimeBand;
        return matchesSearch && matchesBand;
      });
      const filterOptions: Array<{ key: SlotStatusFilter; label: string; count: number }> = [
        { key: "all", label: "All", count: sortedSlots.length },
        { key: "active", label: "Active", count: activeSlots.length },
        { key: "disabled", label: "Disabled", count: disabledSlots.length },
      ];
      const timeFilterOptions: Array<{ key: SlotTimeBand; label: string; count: number }> = [
        { key: "all", label: "All day", count: slotsMatchingStatus.length },
        ...slotTimeBands.map((band) => ({
          key: band.key,
          label: band.label,
          count: slotsMatchingStatus.filter((slot) => getSlotTimeBand(slot) === band.key).length,
        })),
      ];
      const groupedVisibleSlots = slotTimeBands
        .map((band) => ({
          ...band,
          slots: visibleSlots.filter((slot) => getSlotTimeBand(slot) === band.key),
        }))
        .filter((group) => group.slots.length > 0);

      return (
        <section className="theme-panel overflow-hidden rounded-2xl">
          {renderTabToolbar({
            title: "Disable Slot",
            subtitle: "Control recurring slot availability. Use Calendar View for date-only blocks.",
          })}
          <div className="space-y-5 p-4 sm:p-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-xl border p-3 theme-panel-soft">
              <div className="flex items-center gap-5">
                <div className="flex items-center gap-2">
                  <span className="flex h-2 w-2 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.8)]"></span>
                  <span className="text-sm font-bold text-emerald-400">{activeSlots.length} Active Slots</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="flex h-2 w-2 rounded-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.8)]"></span>
                  <span className="text-sm font-bold text-red-400">{disabledSlots.length} Disabled Slots</span>
                </div>
              </div>
              <p className="theme-muted text-xs">
                <span className="font-bold text-slate-300 dark:text-slate-200">Global impact:</span> Disabled slots are hidden from future booking days.
              </p>
            </div>

            <div className="space-y-4 rounded-2xl border border-sky-500/15 bg-sky-500/10 p-3">
              <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
                <div>
                  <p className="theme-title text-sm font-bold">Regular availability switches</p>
                  <p className="theme-muted mt-1 text-xs">Toggle a slot off only when the time should not repeat in your regular schedule.</p>
                </div>
                <div className="theme-input inline-flex rounded-xl p-1">
                  {filterOptions.map((option) => (
                    <button
                      key={option.key}
                      type="button"
                      onClick={() => setDisableFilter(option.key)}
                      className={`rounded-lg px-3 py-2 text-xs font-bold transition duration-150 ${
                        disableFilter === option.key
                          ? "bg-sky-500 text-white"
                          : "theme-muted hover:bg-sky-500/10 hover:text-sky-500"
                      }`}
                    >
                      {option.label} <span className="opacity-75">({option.count})</span>
                    </button>
                  ))}
                </div>
              </div>
              <div className="grid gap-3 xl:grid-cols-[minmax(260px,1fr)_minmax(0,2fr)]">
                <div className="relative">
                  <input
                    className={`${inputClass} pr-20`}
                    value={disableSearch}
                    onChange={(event) => setDisableSearch(event.target.value)}
                    placeholder="Find slot by time, e.g. 18:00"
                  />
                  {disableSearch && (
                    <button
                      type="button"
                      onClick={() => setDisableSearch("")}
                      className="theme-muted absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold hover:text-sky-500"
                    >
                      Clear
                    </button>
                  )}
                </div>
                <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
                  {timeFilterOptions.map((option) => (
                    <button
                      key={option.key}
                      type="button"
                      onClick={() => setDisableTimeBand(option.key)}
                      className={`shrink-0 rounded-xl border px-3 py-2 text-xs font-black transition duration-150 ${
                        disableTimeBand === option.key
                          ? "border-sky-400 bg-sky-500 text-white"
                          : "theme-card theme-muted hover:border-sky-500/40 hover:text-sky-500"
                      }`}
                    >
                      {option.label} <span className="opacity-75">({option.count})</span>
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {loadingSlots && renderLoadingSlots()}
            {!loadingSlots && slots.length === 0 && (
              <p className="theme-card theme-muted rounded-xl border-dashed p-6 text-center text-sm">Generate slots first.</p>
            )}
            {!loadingSlots && sortedSlots.length > 0 && visibleSlots.length === 0 && (
              <p className="theme-card theme-muted rounded-xl border-dashed p-6 text-center text-sm">No slots match this search or filter.</p>
            )}
            {!loadingSlots && visibleSlots.length > 0 && (
              <div className="space-y-4">
                <p className="theme-muted text-xs font-semibold">
                  Showing {visibleSlots.length} of {sortedSlots.length} regular slots.
                </p>
                {groupedVisibleSlots.map((group) => (
                  <section key={group.key} className="theme-panel-soft rounded-2xl border p-3">
                    <div className="mb-3 flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <p className="theme-title text-sm font-black">{group.label}</p>
                        <p className="theme-muted text-xs">{group.helper}</p>
                      </div>
                      <span className="theme-card theme-muted rounded-full px-3 py-1 text-xs font-bold border border-white/5">
                        {group.slots.length} slots
                      </span>
                    </div>
                    <div className="grid gap-2 xl:grid-cols-2">
                      {group.slots.map((slot) => {
                        const active = slot.isActive;
                        const conflictingSlot = active ? undefined : findActiveSlotConflict(slot, sortedSlots);
                        return (
                          <article
                            key={slot.id}
                            className={`flex flex-col gap-3 rounded-xl border px-4 py-3 transition duration-150 sm:flex-row sm:items-center sm:justify-between ${
                              conflictingSlot
                                ? "border-amber-500/30 bg-amber-500/5"
                                : active
                                  ? "theme-card border-emerald-500/20"
                                  : "border-red-500/30 bg-red-500/5"
                            }`}
                          >
                            <div className="min-w-0">
                              <div className="flex flex-wrap items-center gap-2">
                                <p className="theme-title text-base font-black">
                                  {formatTimeLabel(slot.startTime)} - {formatTimeLabel(slot.endTime)}
                                </p>
                                <span
                                  className={`rounded-full px-2.5 py-1 text-[11px] font-black uppercase tracking-wide border ${
                                    active
                                      ? "bg-emerald-500/15 text-emerald-400 border-emerald-500/20"
                                      : "bg-red-500/15 text-red-400 border-red-500/20"
                                  }`}
                                >
                                  {active ? "Active" : "Disabled"}
                                </span>
                                {conflictingSlot && (
                                  <span className="rounded-full bg-amber-500/15 px-2.5 py-1 text-[11px] font-black uppercase tracking-wide text-amber-400 border border-amber-500/20 animate-pulse">
                                    Conflict
                                  </span>
                                )}
                              </div>
                              <p className="theme-muted mt-1 text-xs font-semibold">Rs {slot.price} regular price</p>
                              {conflictingSlot && (
                                <p className="mt-1 text-xs font-bold text-amber-400">
                                  Overlaps active {formatTimeLabel(conflictingSlot.startTime)} - {formatTimeLabel(conflictingSlot.endTime)}
                                </p>
                              )}
                            </div>
                            <button
                              type="button"
                              disabled={saving || Boolean(conflictingSlot)}
                              onClick={() => void handleToggleSlot(slot)}
                              aria-pressed={active}
                              className={`flex min-w-[164px] items-center justify-between rounded-xl border px-3 py-2 text-left transition disabled:opacity-50 ${
                                conflictingSlot
                                  ? "border-amber-500/25 bg-amber-500/10"
                                  : active
                                    ? "border-emerald-500/25 bg-emerald-500/10 hover:border-red-400/40 hover:bg-red-500/10"
                                    : "border-red-500/25 bg-red-500/10 hover:border-emerald-400/40 hover:bg-emerald-500/10"
                              }`}
                            >
                              <span className="theme-title text-xs font-black">{conflictingSlot ? "Resolve first" : active ? "Disable" : "Enable"}</span>
                              <span className={`relative h-6 w-11 rounded-full transition ${active ? "bg-emerald-500" : "bg-red-500/70"}`}>
                                <span className={`absolute top-1 h-4 w-4 rounded-full bg-white shadow transition ${active ? "right-1" : "left-1"}`} />
                              </span>
                            </button>
                          </article>
                        );
                      })}
                    </div>
                  </section>
                ))}
              </div>
            )}
          </div>
        </section>
      );
    }

    if (activeView === "holiday") {
      return (
        <section className="space-y-6">
          {/* Card 1: Holiday Pricing Automation */}
          <div className="theme-panel overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-lg">
            <div className="theme-panel-soft border-b p-4 sm:p-5">
              <h2 className="theme-title text-lg font-bold bg-gradient-to-r from-sky-400 to-indigo-300 bg-clip-text text-transparent">Holiday Pricing Automation</h2>
              <p className="theme-muted mt-1 text-sm">Configure dynamic rules that automatically adjust turf rates on calendar holidays.</p>
            </div>

            <div className="p-4 sm:p-5 space-y-6">
              {/* Toggle Rule */}
              <label className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition duration-200 ${
                enableAutoHoliday ? "border-sky-500/40 bg-sky-500/10" : "theme-card hover:border-sky-500/40"
              }`}>
                <input
                  type="checkbox"
                  className="mt-1 h-4 w-4 shrink-0 rounded border-slate-400 accent-sky-500"
                  checked={enableAutoHoliday}
                  onChange={(e) => setEnableAutoHoliday(e.target.checked)}
                />
                <span>
                  <span className="theme-title block text-sm font-bold">Auto Apply Holiday Pricing</span>
                  <span className="theme-muted mt-1 block text-xs leading-5">
                    Automatically detect upcoming national, state, and festival holidays and adjust prices.
                  </span>
                </span>
              </label>

              {enableAutoHoliday && (
                <div className="grid gap-6 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-white/10">
                  <div className="space-y-5">
                    <div className="grid grid-cols-2 gap-4">
                      {/* Location */}
                      <div>
                        <span className="theme-muted block text-xs font-semibold uppercase tracking-wider">Location</span>
                        <p className="theme-title text-sm font-bold mt-1">
                          {selectedTurf?.city || "Ahmedabad"}, {selectedTurf?.state || "Gujarat"}
                        </p>
                      </div>

                      {/* Source */}
                      <div>
                        <span className="theme-muted block text-xs font-semibold uppercase tracking-wider">Calendar Database</span>
                        <p className="theme-title text-sm font-bold mt-1">
                          {selectedTurf?.holidaySource || "India + Gujarat Calendar"}
                        </p>
                      </div>
                    </div>

                    {/* Pricing Strategy */}
                    <div className="space-y-2">
                      <span className="theme-muted block text-xs font-semibold uppercase tracking-wider">Pricing Strategy</span>
                      <div className="flex flex-wrap gap-4">
                        {(["PERCENTAGE", "FIXED", "MULTIPLIER"] as const).map((strat) => (
                          <label key={strat} className="flex items-center gap-2 text-sm cursor-pointer select-none">
                            <input
                              type="radio"
                              name="strategy"
                              value={strat}
                              checked={pricingStrategy === strat}
                              onChange={() => setPricingStrategy(strat)}
                              className="accent-sky-500"
                            />
                            <span className="capitalize">{strat.toLowerCase().replace("_", " ")}</span>
                          </label>
                        ))}
                      </div>
                    </div>

                    {/* Pricing Value Input */}
                    <Field label={
                      pricingStrategy === "PERCENTAGE" ? "Percentage Increase (%)" :
                      pricingStrategy === "MULTIPLIER" ? "Multiplier Value (e.g. 1.5)" :
                      "Fixed Price (Rs)"
                    }>
                      <input
                        type="number"
                        min={0}
                        step={pricingStrategy === "MULTIPLIER" ? 0.1 : 1}
                        className={inputClass}
                        value={pricingValue}
                        onChange={(e) => setPricingValue(Number(e.target.value))}
                      />
                    </Field>

                    {/* Apply To */}
                    <div className="space-y-2">
                      <span className="theme-muted block text-xs font-semibold uppercase tracking-wider">Apply To</span>
                      <div className="flex gap-4">
                        <label className="flex items-center gap-2 text-sm cursor-pointer select-none">
                          <input
                            type="radio"
                            name="applyTo"
                            value="ALL"
                            checked={applyToSlotsMode === "ALL"}
                            onChange={() => setApplyToSlotsMode("ALL")}
                            className="accent-sky-500"
                          />
                          All Slots
                        </label>
                        <label className="flex items-center gap-2 text-sm cursor-pointer select-none">
                          <input
                            type="radio"
                            name="applyTo"
                            value="SELECTED"
                            checked={applyToSlotsMode === "SELECTED"}
                            onChange={() => setApplyToSlotsMode("SELECTED")}
                            className="accent-sky-500"
                          />
                          Selected Slots
                        </label>
                      </div>
                    </div>

                    {/* Slot Picker if Selected Slots is checked */}
                    {applyToSlotsMode === "SELECTED" && (
                      <div className="space-y-2">
                        <span className="theme-muted block text-xs font-semibold uppercase tracking-wider">Select Target Slots</span>
                        <div className="max-h-60 overflow-y-auto border border-white/10 rounded-xl p-3 bg-black/20">
                          {loadingSlots ? renderLoadingSlots() : renderSlotPicker("Generate slots first.")}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Upcoming Holidays list */}
                  <div className="space-y-4 pt-5 md:pt-0 md:pl-6">
                    <span className="theme-muted block text-xs font-semibold uppercase tracking-wider">Upcoming Calendar Holidays</span>
                    <div className="space-y-2 max-h-[380px] overflow-y-auto pr-1">
                      {upcomingHolidays.length === 0 ? (
                        <p className="text-xs text-slate-500">No upcoming holidays found.</p>
                      ) : (
                        upcomingHolidays.map((holiday) => {
                          const isExcluded = excludedHolidays.includes(holiday.holidayName);
                          return (
                            <label
                              key={holiday.holidayName}
                              className={`flex items-center justify-between rounded-xl border p-3 cursor-pointer transition ${
                                !isExcluded ? "border-sky-500/20 bg-sky-500/5 hover:bg-sky-500/10" : "border-white/5 bg-white/5 opacity-60 hover:opacity-100"
                              }`}
                            >
                              <div className="flex items-center gap-3">
                                <input
                                  type="checkbox"
                                  checked={!isExcluded}
                                  onChange={() => {
                                    if (isExcluded) {
                                      setExcludedHolidays(excludedHolidays.filter(name => name !== holiday.holidayName));
                                    } else {
                                      setExcludedHolidays([...excludedHolidays, holiday.holidayName]);
                                    }
                                  }}
                                  className="h-4 w-4 rounded accent-sky-500"
                                />
                                <div>
                                  <p className="text-sm font-bold text-white">{holiday.holidayName}</p>
                                  <p className="text-xs text-slate-400">{new Date(holiday.holidayDate).toLocaleDateString("en-IN", { day: 'numeric', month: 'short', year: 'numeric' })}</p>
                                </div>
                              </div>
                              <span className="rounded-full bg-slate-800 px-2 py-0.5 text-[10px] text-slate-300 font-bold uppercase tracking-wider border border-white/5">
                                {holiday.holidayType}
                              </span>
                            </label>
                          );
                        })
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* Save Rule Action */}
              <div className="flex justify-end pt-3 border-t border-white/10">
                <button
                  type="button"
                  disabled={automationSaving}
                  onClick={handleSaveHolidayAutomation}
                  className="rounded-xl bg-gradient-to-r from-sky-500 to-indigo-500 px-6 py-2.5 text-sm font-black text-white transition duration-200 hover:from-sky-400 hover:to-indigo-400 shadow-md shadow-sky-500/10"
                >
                  {automationSaving ? "Saving..." : "Save Rule"}
                </button>
              </div>
            </div>
          </div>

          {/* Card 2: Manual Holiday Pricing */}
          <div className="theme-panel overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-lg">
            <div className="theme-panel-soft border-b p-4 sm:p-5">
              <h2 className="theme-title text-lg font-bold">Manual Holiday Pricing</h2>
              <p className="theme-muted mt-1 text-sm">Apply date-specific prices manually for customized/unlisted events.</p>
            </div>

            <div className="p-4 sm:p-5 space-y-4">
              <div className="grid gap-4 md:grid-cols-3">
                <Field label="Date">
                  <input type="date" className={inputClass} value={holidayForm.date} onChange={(e) => setHolidayForm({ ...holidayForm, date: e.target.value })} />
                </Field>
                <Field label="Holiday Price">
                  <input type="number" min={1} className={inputClass} value={holidayForm.price} onChange={(e) => setHolidayForm({ ...holidayForm, price: Number(e.target.value) })} />
                </Field>
                <Field label="Reason / Event Name">
                  <input className={inputClass} value={holidayForm.reason} onChange={(e) => setHolidayForm({ ...holidayForm, reason: e.target.value })} />
                </Field>
              </div>

              <div className="grid gap-4 pt-2 lg:grid-cols-[1fr_280px]">
                <div className="space-y-2">
                  <span className="theme-muted block text-xs font-semibold uppercase tracking-wider">Select Slots to Pricing Override</span>
                  <div className="max-h-60 overflow-y-auto border border-white/10 rounded-xl p-3 bg-black/20">
                    {loadingSlots ? renderLoadingSlots() : renderSlotPicker("Generate slots first.")}
                  </div>
                </div>
                <div className="theme-panel-soft rounded-xl p-4 border border-white/10 bg-black/10 flex flex-col justify-between">
                  <div>
                    <p className="theme-title text-sm font-semibold">{selectedSlots.length} slots selected</p>
                    <p className="theme-muted mt-1 text-xs leading-4">Apply a manual override price for this specific date.</p>
                  </div>
                  <button disabled={saving || selectedSlotIds.length === 0} onClick={handleHolidayPricing} className="mt-4 w-full rounded-xl bg-sky-500 px-4 py-2.5 text-sm font-bold text-white transition duration-200 hover:bg-sky-400 disabled:opacity-50">
                    Apply Holiday Price
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>
      );
    }

    const sortedAvailability = [...availability].sort(
      (a, b) => parseTimeToMinutes(a.startTime) - parseTimeToMinutes(b.startTime)
    );
    const bounds = getScheduleBounds(sortedAvailability);
    const hours = Array.from(
      { length: Math.ceil((bounds.end - bounds.start) / 60) + 1 },
      (_, index) => bounds.start + index * 60
    );
    const scheduleHeight = ((bounds.end - bounds.start) / 60) * hourRowHeight;
    const availableCount = sortedAvailability.filter((item) => getAvailabilityState(item) === "available").length;
    const blockedCount = sortedAvailability.filter((item) => getAvailabilityState(item) === "blocked").length;
    const bookedCount = sortedAvailability.filter((item) => getAvailabilityState(item) === "booked").length;
    const lockedCount = sortedAvailability.filter((item) => getAvailabilityState(item) === "locked").length;
    const expiredCount = sortedAvailability.filter((item) => getAvailabilityState(item) === "expired").length;

    const renderCalendarToolbar = () => (
      <div className="theme-panel-soft border-b p-4 sm:p-5">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div>
            <h2 className="theme-title text-lg font-bold">Calendar View</h2>
            <p className="theme-muted mt-1 text-sm">{formatDateTitle(date)} schedule</p>
          </div>
          <div className="grid gap-3 lg:grid-cols-[180px_160px_auto] lg:items-center">
            <Field label="View Mode">
              <div className="theme-input grid grid-cols-3 rounded-xl p-1">
                {(["day", "week", "month"] as CalendarMode[]).map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => {
                      setCalendarMode(mode);
                      setSelectedSlotIds([]);
                    }}
                    className={`rounded-lg px-2 py-1 text-xs font-bold capitalize transition duration-155 ${
                      calendarMode === mode ? "bg-sky-500 text-white" : "theme-muted hover:bg-sky-500/10 hover:text-sky-500"
                    }`}
                  >
                    {mode}
                  </button>
                ))}
              </div>
            </Field>
            <Field label="Date">
              <input type="date" className={inputClass} value={date} onChange={(e) => handleCalendarDateChange(e.target.value)} />
            </Field>

            {/* Compact metrics bar */}
            <div className="flex flex-wrap items-center gap-4 px-4 py-2.5 bg-slate-900/40 rounded-xl border border-white/5 self-end">
              <ScheduleMetricCompact label="Available" value={availableCount} dotClass="bg-emerald-500" />
              <ScheduleMetricCompact label="Booked" value={bookedCount} dotClass="bg-blue-500" />
              <ScheduleMetricCompact label="Blocked" value={blockedCount} dotClass="bg-rose-500" />
              <ScheduleMetricCompact label="Held" value={lockedCount} dotClass="bg-amber-500" />
              <ScheduleMetricCompact label="Past" value={expiredCount} dotClass="bg-slate-500" />
            </div>
          </div>
        </div>
      </div>
    );

    if (loadingSlots) {
      return (
        <section className="theme-panel overflow-hidden rounded-2xl">
          {renderCalendarToolbar()}
          {renderLoadingSlots()}
        </section>
      );
    }

    if (calendarMode === "week") {
      const weekDates = getWeekDates(date);
      return (
        <section className="theme-panel overflow-hidden rounded-2xl">
          {renderCalendarToolbar()}
          <div className="grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-7">
            {weekDates.map((day) => (
              <CalendarSummaryDay
                key={day}
                date={day}
                items={rangeAvailability[day] || []}
                selected={day === date}
                onOpen={() => {
                  handleCalendarDateChange(day);
                  setCalendarMode("day");
                }}
              />
            ))}
          </div>
        </section>
      );
    }

    if (calendarMode === "month") {
      const monthDates = getMonthDates(date);
      return (
        <section className="theme-panel overflow-hidden rounded-2xl">
          {renderCalendarToolbar()}
          <div className="grid gap-3 p-4 sm:grid-cols-2 md:grid-cols-4 xl:grid-cols-7">
            {monthDates.map((day) => (
              <CalendarSummaryDay
                key={day}
                date={day}
                items={rangeAvailability[day] || []}
                selected={day === date}
                compact
                onOpen={() => {
                  handleCalendarDateChange(day);
                  setCalendarMode("day");
                }}
              />
            ))}
          </div>
        </section>
      );
    }

    return (
      <section className="theme-panel overflow-hidden rounded-2xl">
        {renderCalendarToolbar()}

        {sortedAvailability.length === 0 ? (
          <p className="theme-card theme-muted m-4 rounded-xl border-dashed p-10 text-center text-sm">
            No availability found for this date. Generate slots first.
          </p>
        ) : (
          <div className="grid gap-0 xl:grid-cols-[minmax(0,1fr)_300px]">
            <div className="overflow-x-auto">
              <div className="min-w-[720px] p-4">
                <div className="grid grid-cols-[72px_minmax(0,1fr)]">
                  <div className="theme-border border-r pr-3 text-right text-xs theme-soft" style={{ paddingTop: timelineHeaderHeight }}>
                    {hours.map((hour, index) => (
                      <div key={hour} style={{ height: index === hours.length - 1 ? 0 : hourRowHeight }}>
                        {formatHourLabel(hour)}
                      </div>
                    ))}
                  </div>

                  <div className="theme-panel-soft relative rounded-r-2xl border-y border-r" style={{ height: scheduleHeight + timelineHeaderHeight }}>
                    {hours.slice(0, -1).map((hour) => (
                      <div
                        key={hour}
                        className="theme-border absolute left-0 right-0 border-t"
                        style={{ top: timelineHeaderHeight + ((hour - bounds.start) / 60) * hourRowHeight }}
                      />
                    ))}
                    <div className="theme-soft absolute left-5 top-3 text-[11px] font-black uppercase tracking-[0.18em]">
                      Slots
                    </div>

                    {sortedAvailability.map((item) => {
                      const start = parseTimeToMinutes(item.startTime);
                      const end = getEndMinutes(item.startTime, item.endTime);
                      const rawHeight = ((end - start) / 60) * hourRowHeight;
                      const verticalGap = rawHeight >= 56 ? 6 : 3;
                      const top = timelineHeaderHeight + ((start - bounds.start) / 60) * hourRowHeight + verticalGap;
                      const height = Math.max(rawHeight - verticalGap * 2, 32);
                      const selected = selectedSlotIds.includes(item.slotId);
                      const state = getAvailabilityState(item);
                      const palette = calendarSlotPalette[state];

                      return (
                        <button
                          key={`${item.slotId}-${item.date}`}
                          type="button"
                          onClick={() => toggleSelectedSlot(item.slotId)}
                          aria-pressed={selected}
                          aria-label={`${selected ? "Deselect" : "Select"} ${palette.label.toLowerCase()} slot ${formatTimeLabel(item.startTime)} to ${formatTimeLabel(item.endTime)}`}
                          className={`slot-calendar-item absolute left-5 right-5 overflow-hidden rounded-2xl border text-left transition-all duration-150 hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-300 focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--theme-panel-soft)] ${
                            selected ? "slot-calendar-item--selected" : ""
                          } ${palette.blockClass}`}
                          style={{ top, height }}
                        >
                          <span className={`absolute bottom-2 left-2 top-2 w-1 rounded-full ${palette.accentClass}`} aria-hidden="true" />
                          <div className="flex h-full min-h-0 items-center justify-between gap-4 py-2.5 pl-7 pr-4">
                            <div className="min-w-0">
                              <p className="truncate text-sm font-bold">
                                {formatTimeLabel(item.startTime)} - {formatTimeLabel(item.endTime)}
                              </p>
                              <p className="mt-1 text-xs font-medium opacity-70">Rs {item.price}</p>
                            </div>
                            <div className="flex shrink-0 flex-col items-end gap-1.5">
                              <span className={`rounded-full px-3 py-1 text-xs font-black ${palette.badgeClass}`}>
                                {palette.label}
                              </span>
                            </div>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>

            <aside className="theme-panel-soft border-t p-4 xl:border-l xl:border-t-0 bg-slate-950/10">
              <p className="theme-title text-sm font-semibold">Selected slots</p>
              <p className="theme-muted mt-1 text-xs leading-4">Select timeline blocks, then block available slots or unblock blocked slots.</p>
              <div className="mt-4 grid grid-cols-2 gap-2">
                <button
                  type="button"
                  disabled={saving || selectedAvailableCount === 0}
                  onClick={handleBlockSelected}
                  className="rounded-xl bg-red-600 px-3 py-2.5 text-sm font-bold text-white transition-colors hover:bg-red-500 disabled:opacity-45 shadow-sm"
                >
                  Block
                </button>
                <button
                  type="button"
                  disabled={saving || selectedBlockedCount === 0}
                  onClick={handleUnblockSelected}
                  className="rounded-xl bg-emerald-500 px-3 py-2.5 text-sm font-bold text-emerald-950 transition-colors hover:bg-emerald-400 disabled:opacity-45 shadow-sm"
                >
                  Unblock
                </button>
              </div>
              <div className="mt-4 space-y-2 max-h-[350px] overflow-y-auto pr-1">
                {selectedSlotIds.length === 0 && (
                  <p className="theme-card theme-muted rounded-xl border-dashed p-4 text-sm text-center">Nothing selected.</p>
                )}
                {sortedAvailability
                  .filter((item) => selectedSlotIds.includes(item.slotId))
                  .map((item) => {
                    const state = getAvailabilityState(item);
                    const palette = calendarSlotPalette[state];
                    return (
                      <button
                        key={`selected-${item.slotId}`}
                        type="button"
                        onClick={() => toggleSelectedSlot(item.slotId)}
                        className={`w-full rounded-xl border p-3 text-left text-sm transition-colors ${palette.selectedClass}`}
                      >
                        <span className="block font-semibold">{formatTimeLabel(item.startTime)} - {formatTimeLabel(item.endTime)}</span>
                        <span className={`text-xs ${palette.selectedMetaClass}`}>
                          Rs {item.price} | {palette.label}
                        </span>
                      </button>
                    );
                  })}
              </div>
            </aside>
          </div>
        )}
      </section>
    );
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Global Page Header & Turf Selector */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-white/10 pb-6">
          <div>
            <h1 className="theme-title text-3xl font-black tracking-tight">Slot Manager</h1>
            <p className="theme-muted mt-1 text-sm">Manage slot templates, daily schedules, dynamic pricing, and holiday rules.</p>
          </div>
          <div className="w-full sm:w-72 shrink-0">
            <label className="block">
              <span className="theme-muted mb-1.5 block text-xs font-bold uppercase tracking-wider">Select Turf</span>
              <select
                className={`${inputClass} truncate max-w-full pr-10`}
                value={selectedTurfId ?? ""}
                disabled={loadingTurfs || selectableTurfs.length === 0}
                onChange={(e) => handleTurfChange(e.target.value)}
              >
                {loadingTurfs && <option value="">Loading turfs...</option>}
                {!loadingTurfs && selectableTurfs.length === 0 && <option value="">No turfs available</option>}
                {selectableTurfs.map((turf) => (
                  <option key={turf.id} value={turf.id}>
                    {turf.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>

        {/* Segmented Tab Bar */}
        <div className="flex overflow-x-auto border-b border-white/10 pb-px gap-8 no-scrollbar">
          {views.map((item) => {
            const active = activeView === item.key;
            return (
              <button
                key={item.key}
                type="button"
                onClick={() => setView(item.key)}
                className={`pb-4 text-sm font-semibold relative transition duration-200 shrink-0 ${
                  active ? "text-sky-400 font-bold" : "theme-muted hover:text-sky-500/80"
                }`}
              >
                {item.label}
                {active && (
                  <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-sky-400 to-indigo-400 rounded-full animate-in fade-in zoom-in-50 duration-200" />
                )}
              </button>
            );
          })}
        </div>

        {/* Render Selected View */}
        {renderActiveView()}
      </div>
    </DashboardLayout>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="theme-muted mb-1.5 block text-xs font-bold uppercase tracking-wider">{label}</span>
      {children}
    </label>
  );
}

function ScheduleMetricCompact({ label, value, dotClass }: { label: string; value: number; dotClass: string }) {
  return (
    <div className="flex items-center gap-1.5 px-1 shrink-0">
      <span className={`h-2.5 w-2.5 rounded-full ${dotClass}`} aria-hidden="true" />
      <span className="text-sm font-bold text-white leading-none">{value}</span>
      <span className="theme-muted text-[11px] font-semibold leading-none">{label}</span>
    </div>
  );
}

function SlotStatusCard({ label, value, tone }: { label: string; value: number; tone: "emerald" | "red" }) {
  const toneClass = {
    emerald: "border-emerald-500/20 bg-emerald-500/10 text-emerald-400",
    red: "border-red-500/20 bg-red-500/10 text-red-400",
  }[tone];

  return (
    <div className={`rounded-2xl border p-4 ${toneClass} shadow-sm`}>
      <p className="text-[10px] font-black uppercase tracking-[0.22em] opacity-75">{label}</p>
      <p className="mt-2.5 text-3xl font-black">{value}</p>
    </div>
  );
}

function CalendarSummaryDay({
  date,
  items,
  selected,
  compact = false,
  onOpen,
}: {
  date: string;
  items: AvailabilitySlot[];
  selected: boolean;
  compact?: boolean;
  onOpen: () => void;
}) {
  const availableCount = items.filter((item) => getAvailabilityState(item) === "available").length;
  const blockedCount = items.filter((item) => getAvailabilityState(item) === "blocked").length;
  const bookedCount = items.filter((item) => getAvailabilityState(item) === "booked").length;
  const lockedCount = items.filter((item) => getAvailabilityState(item) === "locked").length;
  const expiredCount = items.filter((item) => getAvailabilityState(item) === "expired").length;
  const firstSlot = [...items].sort((a, b) => parseTimeToMinutes(a.startTime) - parseTimeToMinutes(b.startTime))[0];

  return (
    <button
      type="button"
      onClick={onOpen}
      className={`rounded-xl border p-4 text-left transition duration-150 hover:border-sky-400 ${
        selected ? "border-sky-400 bg-sky-500/10" : "theme-card"
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="theme-title text-sm font-bold">{formatShortDate(date)}</p>
          <p className="theme-muted mt-1 text-xs">{items.length} slots</p>
        </div>
        {blockedCount > 0 && <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${calendarSlotPalette.blocked.badgeClass}`}>{blockedCount} blocked</span>}
        {expiredCount > 0 && blockedCount === 0 && <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${calendarSlotPalette.expired.badgeClass}`}>{expiredCount} past</span>}
      </div>
      <div className={`mt-4 grid gap-2 ${compact ? "grid-cols-2" : "grid-cols-1"}`}>
        <span className={`rounded-lg px-2 py-1 text-xs font-bold text-center ${calendarSlotPalette.available.summaryChipClass}`}>{availableCount} available</span>
        <span className={`rounded-lg px-2 py-1 text-xs font-bold text-center ${calendarSlotPalette.booked.summaryChipClass}`}>{bookedCount} booked</span>
        <span className={`rounded-lg px-2 py-1 text-xs font-bold text-center ${calendarSlotPalette.blocked.summaryChipClass}`}>{blockedCount} blocked</span>
        {lockedCount > 0 && <span className={`rounded-lg px-2 py-1 text-xs font-bold text-center ${calendarSlotPalette.locked.summaryChipClass}`}>{lockedCount} held</span>}
        {expiredCount > 0 && <span className={`rounded-lg px-2 py-1 text-xs font-bold text-center ${calendarSlotPalette.expired.summaryChipClass}`}>{expiredCount} past</span>}
      </div>
      {!compact && firstSlot && (
        <p className="theme-muted mt-4 truncate text-xs">
          Starts {formatTimeLabel(firstSlot.startTime)} | Rs {firstSlot.price}
        </p>
      )}
    </button>
  );
}

function PriceRow({ slot, saving, onSave }: { slot: Slot; saving: boolean; onSave: (price: number) => void }) {
  const [price, setPrice] = useState(slot.price);

  return (
    <div className="theme-card grid gap-3 rounded-xl p-4 sm:grid-cols-[1fr_140px_110px] sm:items-center">
      <div>
        <p className="theme-title text-sm font-semibold">{slot.startTime} - {slot.endTime}</p>
        <p className="theme-muted text-xs">{slot.isActive ? "Active" : "Disabled"}</p>
      </div>
      <input type="number" min={1} className={inputClass} value={price} onChange={(e) => setPrice(Number(e.target.value))} />
      <button disabled={saving} onClick={() => onSave(price)} className="rounded-xl bg-sky-500 px-4 py-2 text-sm font-bold text-white disabled:opacity-50">
        Save
      </button>
    </div>
  );
}
