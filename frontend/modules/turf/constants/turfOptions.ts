import type { SelectOption } from "@/components/ui/Select";
import type { TurfSport } from "../types/turf.types";

export type TurfOption = SelectOption & {
  labelKey?: string;
};

export const TURF_SPORT_OPTIONS: TurfOption[] = [
  { value: "CRICKET", label: "Box Cricket", labelKey: "search.sport.cricket" },
  { value: "FOOTBALL", label: "Football", labelKey: "search.sport.football" },
  { value: "BADMINTON", label: "Badminton", labelKey: "search.sport.badminton" },
  { value: "TENNIS", label: "Tennis", labelKey: "search.sport.tennis" },
  { value: "PICKLEBALL", label: "Pickleball" },
  { value: "BASKETBALL", label: "Basketball", labelKey: "search.sport.basketball" },
];

export const TURF_AMENITY_OPTIONS: TurfOption[] = [
  { value: "Indoor", label: "Indoor" },
  { value: "Outdoor", label: "Outdoor" },
  { value: "Parking", label: "Parking" },
  { value: "Washroom", label: "Washroom" },
  { value: "Floodlights", label: "Floodlights" },
  { value: "Drinking Water", label: "Drinking water" },
  { value: "Cafeteria", label: "Cafeteria" },
  { value: "Changing Room", label: "Changing room" },
  { value: "Scoreboard", label: "Scoreboard" },
];

export const TURF_BOX_TYPE_OPTIONS = [
  { value: "FIVE_V_FIVE", label: "5v5" },
  { value: "SIX_V_SIX", label: "6v6" },
  { value: "SEVEN_V_SEVEN", label: "7v7" },
  { value: "ELEVEN_V_ELEVEN", label: "11v11" },
];

export const TURF_PITCH_TYPE_OPTIONS = [
  { value: "TURF", label: "Turf" },
  { value: "CONCRETE", label: "Concrete" },
  { value: "MATTING", label: "Matting" },
];

export function toTitleLabel(value: string) {
  return value
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function normalizeSportOptions(sports: TurfSport[]): TurfOption[] {
  if (!sports.length) {
    return TURF_SPORT_OPTIONS;
  }

  return sports
    .filter((sport) => sport.active !== false)
    .map((sport) => {
      const code = String(sport.code || sport.name).toUpperCase();
      return {
        value: code,
        label: sport.name || toTitleLabel(code),
      };
    });
}

export function buildAmenityOptions(amenities: string[]): TurfOption[] {
  const dynamicOptions = amenities
    .map((amenity) => String(amenity || "").trim())
    .filter(Boolean)
    .map((amenity) => ({ value: amenity, label: toTitleLabel(amenity) }));

  return mergeSelectOptions(TURF_AMENITY_OPTIONS, dynamicOptions);
}

export function mergeSelectOptions(baseOptions: TurfOption[], extras: Array<TurfOption | string>): TurfOption[] {
  const byValue = new Map<string, TurfOption>();

  const addOption = (option: TurfOption | string) => {
    const value = typeof option === "string" ? option.trim() : String(option.value).trim();
    if (!value) return;

    const key = value.toUpperCase();
    if (byValue.has(key)) return;

    byValue.set(key, {
      value,
      label: typeof option === "string" ? toTitleLabel(value) : option.label || toTitleLabel(value),
      ...(typeof option === "string" ? {} : { disabled: option.disabled, labelKey: option.labelKey }),
    });
  };

  baseOptions.forEach(addOption);
  extras.forEach(addOption);

  return Array.from(byValue.values());
}

export function optionLabel(options: Array<{ value: string | number; label: string }>, value?: string) {
  if (!value) return "";
  const normalizedValue = String(value).toUpperCase();
  return options.find((option) => String(option.value).toUpperCase() === normalizedValue)?.label || toTitleLabel(value);
}
