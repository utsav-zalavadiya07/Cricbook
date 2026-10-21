import api from "@/services/api";
import { SlotRequest, TurfSport } from "../types/turf.types";

const turfNotification = (title: string, message: string) => ({
  notification: {
    category: "turf" as const,
    success: { title, message },
  },
});

const slotNotification = (title: string, message: string) => ({
  notification: {
    category: "slots" as const,
    success: { title, message },
  },
});

export const createTurf = (data: FormData) =>
  api.post("/turfs", data, {
    headers: { "Content-Type": "multipart/form-data" },
    ...turfNotification("Turf Created", "Turf added successfully."),
  });

// Backend: GET /turfs/my  — needs to be added to TurfController
export const getMyTurfs = () => api.get("/turfs/my");

export const getTurfById = (id: number) => api.get(`/turfs/${id}`);

export const getActiveSports = () =>
  api.get<TurfSport[]>("/sports/active");

export const getTurfAmenities = () =>
  api.get<string[]>("/turfs/amenities");

export const updateTurf = (id: number, data: FormData) =>
  api.put(`/turfs/${id}`, data, {
    headers: { "Content-Type": "multipart/form-data" },
    ...turfNotification("Turf Updated", "Turf details updated successfully."),
  });

export const activateTurf = (id: number) =>
  api.put(`/turfs/${id}/activate`, undefined, turfNotification("Turf Activated", "Players can discover and book this turf again."));

export const deactivateTurf = (id: number) =>
  api.put(`/turfs/${id}/deactivate`, undefined, turfNotification("Turf Deactivated", "This turf is hidden from player booking flows."));

export const maintenanceTurf = (id: number) =>
  api.put(`/turfs/${id}/maintenance`, undefined, turfNotification("Maintenance Mode", "This turf is now marked for maintenance."));

export const createSlots = (turfId: number, data: SlotRequest) =>
  api.post(`/turfs/${turfId}/slots`, data, slotNotification("Schedule Saved", "This regular schedule stays active until you change it."));

export const getTurfSlots = (turfId: number) =>
  api.get(`/turfs/${turfId}/slots`);

export const getTurfAvailability = (turfId: number, date: string) =>
  api.get(`/turfs/${turfId}/availability`, { params: { date } });

export const updateSlot = (
  turfId: number,
  slotId: number,
  data: { startTime?: string; endTime?: string; price?: number; isActive?: boolean }
) => {
  const notification =
    typeof data.isActive === "boolean"
      ? data.isActive
        ? slotNotification("Slot Enabled", "This slot is available for bookings again.")
        : slotNotification("Slot Disabled", "This slot is hidden from future booking days.")
      : data.price !== undefined
        ? slotNotification("Slot Price Updated", "Slot pricing updated successfully.")
        : slotNotification("Slot Updated", "Slot details updated successfully.");

  return api.put(`/turfs/${turfId}/slots/${slotId}`, data, notification);
};

export const disableSlot = (turfId: number, slotId: number) =>
  api.post(`/turfs/${turfId}/slots/${slotId}/disable`, undefined, slotNotification("Slot Disabled", "This slot is hidden from future booking days."));

export const applySlotPricing = (
  turfId: number,
  data: { slotIds: number[]; date: string; price: number; reason?: string }
) => api.post(`/turfs/${turfId}/slots/pricing`, data, slotNotification("Pricing Updated", "Selected slot prices were updated successfully."));

export const blockSlots = (turfId: number, data: { date: string; slotIds: number[]; blocked?: boolean }) =>
  api.post(
    `/turfs/${turfId}/block`,
    data,
    data.blocked === false
      ? slotNotification("Slots Unblocked", "Selected slots are available again.")
      : slotNotification("Slots Blocked", "Selected slots are blocked for that date."),
  );

export const unblockSlots = (turfId: number, data: { date: string; slotIds: number[] }) =>
  api.post(`/turfs/${turfId}/block`, { ...data, blocked: false }, slotNotification("Slots Unblocked", "Selected slots are available again."));
