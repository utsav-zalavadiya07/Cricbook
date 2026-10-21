"use client";

import { useCallback, useEffect, useState } from "react";
import { Button, Card, Field, Input, Select } from "@/components/ui";
import { applySlotPricing, createSlots, disableSlot, getTurfById, getTurfSlots, updateSlot } from "../services/turfService";
import { Slot } from "../types/turf.types";
import { useToast } from "@/modules/ui/ToastProvider";

interface SlotManagerProps {
  turfId: number;
  onSuccess?: () => void;
}

export default function SlotManager({ turfId, onSuccess }: SlotManagerProps) {
  const { showWarning } = useToast();
  const [form, setForm] = useState({
    startTime: "06:00",
    endTime: "22:00",
    slotDuration: 60,
    price: 0,
  });
  const [loading, setLoading] = useState(false);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [pricing, setPricing] = useState({ date: "", price: 750, reason: "Holiday pricing" });

  const loadSlots = useCallback(async () => {
    try {
      const [slotRes, turfRes] = await Promise.all([getTurfSlots(turfId), getTurfById(turfId)]);
      setSlots(slotRes.data);
      setForm((current) => ({ ...current, price: Number(turfRes.data.pricePerHour) }));
    } catch {
      setSlots([]);
    }
  }, [turfId]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadSlots();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [loadSlots]);

  const set = (field: string) => (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setForm((prev) => ({
      ...prev,
      [field]: event.target.type === "number" ? Number(event.target.value) : event.target.value,
    }));
  };

  const calcSlotCount = () => {
    try {
      const [sh, sm] = form.startTime.split(":").map(Number);
      const [eh, em] = form.endTime.split(":").map(Number);
      const totalMins = eh * 60 + em - (sh * 60 + sm);
      return totalMins > 0 ? Math.floor(totalMins / form.slotDuration) : 0;
    } catch {
      return 0;
    }
  };

  const handleGenerate = async () => {
    if (calcSlotCount() === 0 || !Number.isFinite(form.price) || form.price <= 0) {
      showWarning("Invalid slot configuration", "Choose valid opening hours, duration and a positive slot price.");
      return;
    }
    setLoading(true);
    try {
      await createSlots(turfId, form);
      await loadSlots();
      onSuccess?.();
    } catch {
      return;
    } finally {
      setLoading(false);
    }
  };

  const handleToggleSlot = async (slot: Slot) => {
    try {
      if (slot.isActive) {
        await disableSlot(turfId, slot.id);
      } else {
        await updateSlot(turfId, slot.id, { isActive: true });
      }
      await loadSlots();
    } catch {
      return;
    }
  };

  const handlePriceChange = async (slot: Slot, price: number) => {
    try {
      await updateSlot(turfId, slot.id, { price });
      await loadSlots();
    } catch {
      return;
    }
  };

  const handleHolidayPricing = async () => {
    if (!pricing.date || slots.length === 0) {
      showWarning("No pricing target", "Choose a date after generating slots.");
      return;
    }
    try {
      await applySlotPricing(turfId, {
        slotIds: slots.map((slot) => slot.id),
        date: pricing.date,
        price: pricing.price,
        reason: pricing.reason,
      });
    } catch {
      return;
    }
  };

  const slotCount = calcSlotCount();

  return (
    <Card className="space-y-5 p-6">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500/20">
          <span className="text-xs font-black text-emerald-400">SL</span>
        </div>
        <div>
          <h3 className="theme-title font-semibold">Manage Turf Slots</h3>
          <p className="theme-soft text-xs">Generate, price and enable slots for this turf</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <Field label="Opening Time">
          <Input type="time" value={form.startTime} onChange={set("startTime")} />
        </Field>
        <Field label="Closing Time">
          <Input type="time" value={form.endTime} onChange={set("endTime")} />
        </Field>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <Field label="Duration (minutes)">
          <Select
            value={form.slotDuration}
            onChange={set("slotDuration")}
            options={[
              { label: "30 min", value: 30 },
              { label: "60 min", value: 60 },
              { label: "90 min", value: 90 },
              { label: "120 min", value: 120 },
            ]}
          />
        </Field>
        <Field label="Default slot price (₹)">
          <Input type="number" placeholder="Turf listing price" value={form.price || ""} onChange={set("price")} />
        </Field>
      </div>

      {slotCount > 0 && (
        <div className="flex items-center justify-between rounded-xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3">
          <span className="theme-muted text-sm">Slots to be generated</span>
          <span className="text-lg font-bold text-emerald-400">{slotCount}</span>
        </div>
      )}

      <Button onClick={handleGenerate} disabled={loading || slotCount === 0 || form.price <= 0} className="w-full py-3">
        {loading ? "Generating..." : `Generate ${slotCount} Slots`}
      </Button>

      <div className="theme-border border-t pt-5">
        <h4 className="theme-title font-semibold">Calendar Pricing</h4>
        <p className="theme-soft mt-1 text-xs">Apply holiday/weekend pricing to all current slots for a date.</p>
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <Input type="date" value={pricing.date} onChange={(event) => setPricing({ ...pricing, date: event.target.value })} />
          <Input type="number" value={pricing.price} onChange={(event) => setPricing({ ...pricing, price: Number(event.target.value) })} />
          <Button tone="owner" onClick={handleHolidayPricing} className="py-3">
            Apply Pricing
          </Button>
        </div>
      </div>

      <div className="theme-border border-t pt-5">
        <h4 className="theme-title font-semibold">Existing Slots</h4>
        <div className="mt-4 grid gap-3">
          {slots.length === 0 && <p className="theme-muted rounded-xl border border-dashed p-4 text-center text-sm">No slots generated yet.</p>}
          {slots.map((slot) => (
            <div key={slot.id} className="theme-card grid grid-cols-1 gap-3 rounded-xl p-3 sm:grid-cols-[1fr_120px_120px]">
              <div>
                <p className="theme-title text-sm font-semibold">
                  {slot.startTime} - {slot.endTime}
                </p>
                <p className="theme-soft text-xs">{slot.isActive ? "Active" : "Disabled"}</p>
              </div>
              <Input type="number" defaultValue={slot.price} onBlur={(event) => handlePriceChange(slot, Number(event.target.value))} />
              <button
                onClick={() => void handleToggleSlot(slot)}
                className={`rounded-xl px-4 py-2 text-sm font-semibold ${slot.isActive ? "border border-red-500/30 bg-red-500/10 text-red-300" : "border border-emerald-500/30 bg-emerald-500/10 text-emerald-300"}`}
              >
                {slot.isActive ? "Disable" : "Enable"}
              </button>
            </div>
          ))}
        </div>
      </div>
    </Card>
  );
}
