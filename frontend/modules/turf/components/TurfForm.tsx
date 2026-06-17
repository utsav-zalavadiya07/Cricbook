"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import ImageUploader from "./ImageUploader";
import { createTurf, updateTurf } from "../services/turfService";
import { Turf } from "../types/turf.types";
import { Button, Field, Input, MultiSelect, Select, Textarea } from "@/components/ui";
import {
  mergeSelectOptions,
  TURF_BOX_TYPE_OPTIONS,
  TURF_PITCH_TYPE_OPTIONS,
} from "../constants/turfOptions";
import { useTurfTaxonomy } from "../hooks/useTurfTaxonomy";

interface TurfFormProps {
  initialData?: Turf;
  turfId?: number;
  onSuccess?: () => void;
  onCancel?: () => void;
}

function initialFormState(initialData?: Turf) {
  const existingSports = initialData?.sports?.length
    ? initialData.sports
    : initialData?.sport
      ? [initialData.sport]
      : ["CRICKET"];

  return {
    name: initialData?.name || "",
    description: initialData?.description || "",
    location: initialData?.location || "",
    city: initialData?.city || "",
    capacity: initialData?.capacity ? String(initialData.capacity) : "12",
    sports: [String(existingSports[0] || "CRICKET").toUpperCase()],
    amenities: initialData?.amenities?.map((item) => typeof item === "string" ? item : item.amenity) || [],
    pricePerHour: initialData?.pricePerHour ? String(initialData.pricePerHour) : "",
    pitchType: initialData?.pitchType || "TURF",
    boxType: initialData?.boxType || "SIX_V_SIX",
    latitude: initialData?.latitude ? String(initialData.latitude) : "",
    longitude: initialData?.longitude ? String(initialData.longitude) : "",
  };
}

function optionalNumber(value: string) {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  return Number(trimmed);
}

function buildTurfPayload(form: ReturnType<typeof initialFormState>, retainedImages: string[]) {
  const sport = form.sports[0] || "";
  return {
    name: form.name.trim(),
    description: form.description.trim() || undefined,
    location: form.location.trim(),
    city: form.city.trim(),
    capacity: Number(form.capacity),
    pricePerHour: Number(form.pricePerHour),
    pitchType: form.pitchType,
    boxType: form.boxType,
    sport,
    sportCode: sport,
    sports: form.sports,
    amenities: form.amenities,
    images: retainedImages,
    latitude: optionalNumber(form.latitude),
    longitude: optionalNumber(form.longitude),
  };
}

export default function TurfForm({ initialData, turfId, onSuccess, onCancel }: TurfFormProps = {}) {
  const router = useRouter();
  const [images, setImages] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState(() => initialFormState(initialData));
  const [retainedImages, setRetainedImages] = useState<string[]>(() => initialData?.images || []);
  const { sportOptions, amenityOptions } = useTurfTaxonomy(form.amenities, form.sports);
  const visibleSportOptions = mergeSelectOptions(sportOptions, form.sports);
  const visibleAmenityOptions = mergeSelectOptions(amenityOptions, form.amenities);

  const set = (field: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const setSport = (e: React.ChangeEvent<HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, sports: [e.target.value] }));

  const setAmenities = (amenities: string[]) =>
    setForm((prev) => ({ ...prev, amenities }));

  const handleSubmit = async () => {
    const payload = buildTurfPayload(form, retainedImages);
    if (!payload.name || !payload.city || !payload.pricePerHour || !payload.location || !payload.capacity) {
      setError("Please fill in all required fields.");
      return;
    }
    if (!Number.isInteger(payload.capacity) || payload.capacity < 1 || payload.capacity > 200) {
      setError("Capacity must be between 1 and 200 players.");
      return;
    }
    if (!Number.isFinite(payload.pricePerHour) || payload.pricePerHour <= 0) {
      setError("Price per hour must be greater than zero.");
      return;
    }
    if (payload.amenities.length > 20) {
      setError("You can add up to 20 amenities.");
      return;
    }
    if (payload.sports.length === 0) {
      setError("Please select at least one sport supported by this turf.");
      return;
    }
    if (payload.sports.length > 1) {
      setError("A turf can be mapped to one primary sport.");
      return;
    }
    if (payload.latitude !== undefined && (!Number.isFinite(payload.latitude) || payload.latitude < -90 || payload.latitude > 90)) {
      setError("Latitude must be between -90 and 90.");
      return;
    }
    if (payload.longitude !== undefined && (!Number.isFinite(payload.longitude) || payload.longitude < -180 || payload.longitude > 180)) {
      setError("Longitude must be between -180 and 180.");
      return;
    }
    if (retainedImages.length + images.length > 20) {
      setError("A turf can have up to 20 images.");
      return;
    }

    setLoading(true);
    setError("");

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(payload)], { type: "application/json" }));
    images.forEach((img) => formData.append("images", img));

    try {
      if (turfId) {
        await updateTurf(turfId, formData);
      } else {
        await createTurf(formData);
      }
      if (onSuccess) {
        onSuccess();
      } else {
        router.push("/turf/my-turfs");
      }
    } catch (e: unknown) {
      let errorMessage = turfId ? "Failed to update turf." : "Failed to create turf.";
      const apiError = e as { response?: { data?: { message?: string; error?: string; details?: Record<string, unknown> } | Record<string, unknown> } };
      const data = apiError.response?.data;
      if (data) {
        if ("message" in data && typeof data.message === "string") {
          errorMessage = data.message;
        } else if ("error" in data && typeof data.error === "string") {
          errorMessage = data.error;
        } else if ("details" in data && data.details && typeof data.details === "object") {
          const errors = Object.entries(data.details)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join(", ");
          if (errors) errorMessage = errors;
        } else if (typeof data === "object") {
          const errors = Object.entries(data)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join(", ");
          if (errors) errorMessage = errors;
        }
      }
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-5">
      {error && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-xl">
          {error}
        </div>
      )}

      {/* Row 1 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Field label="Turf Name *">
          <Input placeholder="e.g. Green Pitch Arena" value={form.name} onChange={set("name")} />
        </Field>
        <Field label="City *">
          <Input placeholder="e.g. Mumbai" value={form.city} onChange={set("city")} />
        </Field>
      </div>

      {/* Location */}
      <Field label="Address / Location *">
        <Input placeholder="Full address" value={form.location} onChange={set("location")} />
      </Field>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(8rem,0.65fr)_minmax(12rem,1fr)_minmax(22rem,1.8fr)]">
        <Field label="Capacity *" hint="players">
          <Input type="number" min={1} max={200} placeholder="12" value={form.capacity} onChange={set("capacity")} />
        </Field>
        <Field label="Sport *" hint="Used by player search sport filters.">
          <Select
            value={form.sports[0] || ""}
            onChange={setSport}
            options={visibleSportOptions}
            accent="owner"
          />
        </Field>
        <div className="block min-w-0">
          <span className="theme-muted mb-1.5 block text-sm font-semibold">Amenities</span>
          <MultiSelect
            value={form.amenities}
            onChange={setAmenities}
            options={visibleAmenityOptions}
            placeholder="Select amenities"
            accent="owner"
            maxSelected={20}
          />
          <span className="theme-soft mt-1 block text-xs">These values match the player portal amenities filters.</span>
        </div>
      </div>

      {/* Description */}
      <Field label="Description">
        <Textarea rows={3} placeholder="Describe your turf..." value={form.description} onChange={set("description")} />
      </Field>

      {/* Row 2 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Field label="Price / Hour (Rs) *">
          <Input type="number" placeholder="500" value={form.pricePerHour} onChange={set("pricePerHour")} />
        </Field>
        <Field label="Pitch Type">
          <Select
            value={form.pitchType}
            onChange={set("pitchType")}
            options={TURF_PITCH_TYPE_OPTIONS}
            accent="owner"
          />
        </Field>
        <Field label="Box Type">
          <Select
            onChange={set("boxType")}
            value={form.boxType}
            options={TURF_BOX_TYPE_OPTIONS}
            accent="owner"
          />
        </Field>
      </div>

      {/* Coordinates (optional) */}
      <div className="grid grid-cols-2 gap-4">
        <Field label="Latitude" hint="optional">
          <Input placeholder="19.0760" type="number" value={form.latitude} onChange={set("latitude")} />
        </Field>
        <Field label="Longitude" hint="optional">
          <Input placeholder="72.8777" type="number" value={form.longitude} onChange={set("longitude")} />
        </Field>
      </div>

      {/* Image Upload */}
      <ImageUploader
        onChange={setImages}
        maxFiles={20}
        existingImages={retainedImages}
        onExistingChange={setRetainedImages}
      />

      {/* Submit */}
      <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
        <Button
          onClick={handleSubmit}
          disabled={loading}
          className="w-full py-3"
        >
          {loading ? (
            <span className="flex items-center justify-center gap-2">
              <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
              </svg>
              {turfId ? "Updating Turf..." : "Creating Turf..."}
            </span>
          ) : turfId ? "Update Turf" : "Create Turf"}
        </Button>
        {onCancel && (
          <Button
            tone="secondary"
            onClick={onCancel}
            disabled={loading}
            className="px-5 py-3"
          >
            Cancel
          </Button>
        )}
      </div>
    </div>
  );
}
