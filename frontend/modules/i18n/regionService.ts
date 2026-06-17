import { getExpansionConfigs } from "@/modules/global/services/globalService";
import { LocaleCode, normalizeLocale } from "./messages";

export type RegionOption = {
  city: string;
  locale: LocaleCode;
  currency: string;
  timeZone: string;
};

export const defaultRegions: RegionOption[] = [
  { city: "Ahmedabad", locale: "gu", currency: "INR", timeZone: "Asia/Kolkata" },
  { city: "Mumbai", locale: "hi", currency: "INR", timeZone: "Asia/Kolkata" },
  { city: "Dubai", locale: "en", currency: "AED", timeZone: "Asia/Dubai" },
  { city: "London", locale: "en", currency: "GBP", timeZone: "Europe/London" },
  { city: "Singapore", locale: "en", currency: "SGD", timeZone: "Asia/Singapore" },
];

export async function loadRegionOptions() {
  try {
    const response = await getExpansionConfigs();
    const configs = response.data as Array<{ city: string; locale: string; currency: string; timeZone: string }>;
    if (!configs.length) {
      return defaultRegions;
    }

    return configs.map((config) => ({
      city: config.city,
      locale: normalizeLocale(config.locale) as LocaleCode,
      currency: config.currency,
      timeZone: config.timeZone,
    }));
  } catch {
    return defaultRegions;
  }
}
