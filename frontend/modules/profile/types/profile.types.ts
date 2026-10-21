export interface User {
  id: number;
  name: string;
  email: string;
  phone: string;
  city?: string;
  bio?: string;
  designation?: string;
  alternatePhone?: string;
  businessName?: string;
  businessType?: string;
  gstin?: string;
  address?: string;
  supportEmail?: string;
  profileCompletionPercentage?: number;
}

export interface UpdateProfilePayload {
  name?: string;
  phone?: string;
  city?: string;
  bio?: string;
  designation?: string;
  alternatePhone?: string;
  businessName?: string;
  businessType?: string;
  gstin?: string;
  address?: string;
  supportEmail?: string;
}

export interface PlayerProfile {
  userId: number;
  name: string;
  email: string;
  phone: string;
  photoUrl?: string;
  bio?: string;
  city?: string;
  preferredTime?: string;
  battingStyle?: string;
  bowlingStyle?: string;
  position?: string;
  experience?: string;
  matches: number;
  wins: number;
  mvp: number;
  rating: number;
  profileCompletionPercentage?: number;
}

export type BattingStyle = "RIGHT_HAND" | "LEFT_HAND" | "SWITCH_HIT";
export type BowlingStyle =
  | "RIGHT_ARM_FAST"
  | "RIGHT_ARM_MEDIUM"
  | "RIGHT_ARM_OFF_SPIN"
  | "RIGHT_ARM_LEG_SPIN"
  | "LEFT_ARM_FAST"
  | "LEFT_ARM_MEDIUM"
  | "LEFT_ARM_ORTHODOX"
  | "LEFT_ARM_CHINAMAN"
  | "NONE";
export type PlayerPosition =
  | "OPENER"
  | "TOP_ORDER"
  | "MIDDLE_ORDER"
  | "FINISHER"
  | "ALL_ROUNDER"
  | "BOWLER"
  | "WICKET_KEEPER"

export interface UpdatePlayerProfilePayload {
  photoUrl?: string;
  bio?: string;
  city?: string;
  preferredTime?: string;
  battingStyle?: BattingStyle | "";
  bowlingStyle?: BowlingStyle | "";
  position?: PlayerPosition | "";
  experience?: string;
}
