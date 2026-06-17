"use client";

import { useEffect, useState } from "react";
import { getActiveSports, getTurfAmenities } from "../services/turfService";
import type { SelectOption } from "@/components/ui/Select";
import type { TurfSport } from "../types/turf.types";
import {
  buildAmenityOptions,
  mergeSelectOptions,
  normalizeSportOptions,
  TURF_AMENITY_OPTIONS,
  TURF_SPORT_OPTIONS,
} from "../constants/turfOptions";

type TurfTaxonomyState = {
  sportOptions: SelectOption[];
  amenityOptions: SelectOption[];
  loading: boolean;
};

const EMPTY_VALUES: string[] = [];

export function useTurfTaxonomy(extraAmenities: string[] = EMPTY_VALUES, extraSports: string[] = EMPTY_VALUES) {
  const [state, setState] = useState<TurfTaxonomyState>({
    sportOptions: mergeSelectOptions(TURF_SPORT_OPTIONS, extraSports),
    amenityOptions: mergeSelectOptions(TURF_AMENITY_OPTIONS, extraAmenities),
    loading: true,
  });

  useEffect(() => {
    let active = true;

    Promise.allSettled([getActiveSports(), getTurfAmenities()])
      .then(([sportsResult, amenitiesResult]) => {
        if (!active) return;

        const sportOptions =
          sportsResult.status === "fulfilled"
            ? normalizeSportOptions((sportsResult.value.data || []) as TurfSport[])
            : TURF_SPORT_OPTIONS;

        const amenityOptions =
          amenitiesResult.status === "fulfilled"
            ? buildAmenityOptions(amenitiesResult.value.data || [])
            : TURF_AMENITY_OPTIONS;

        setState({
          sportOptions: mergeSelectOptions(sportOptions.length ? sportOptions : TURF_SPORT_OPTIONS, extraSports),
          amenityOptions: mergeSelectOptions(amenityOptions.length ? amenityOptions : TURF_AMENITY_OPTIONS, extraAmenities),
          loading: false,
        });
      })
      .catch(() => {
        if (!active) return;
        setState({
          sportOptions: mergeSelectOptions(TURF_SPORT_OPTIONS, extraSports),
          amenityOptions: mergeSelectOptions(TURF_AMENITY_OPTIONS, extraAmenities),
          loading: false,
        });
      });

    return () => {
      active = false;
    };
  }, [extraAmenities, extraSports]);

  return state;
}
