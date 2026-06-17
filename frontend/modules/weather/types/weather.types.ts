export type WeatherRiskLevel = "LOW" | "MEDIUM" | "HIGH" | "UNSAFE";
export type RefundType = "FULL" | "PARTIAL" | "WALLET_CREDIT" | "RESCHEDULE_ONLY";
export type TurfEnvironmentType = "INDOOR" | "SEMI_INDOOR" | "OUTDOOR";

export interface Forecast {
  city: string;
  date: string;
  hour: number;
  temperature: number;
  rainProbability: number;
  humidity: number;
  windSpeed: number;
  uvIndex: number;
  summary: string;
  riskLevel: WeatherRiskLevel;
}

export interface Playability {
  turfId: number;
  turfName: string;
  slotId: number;
  date: string;
  startTime?: string;
  endTime?: string;
  score: number;
  riskLevel: WeatherRiskLevel;
  rainProbability: number;
  temperature: number;
  reason: string;
  recommendation: string;
}

export interface WeatherPolicy {
  rainRefundAllowed: boolean;
  flexibleReschedule: boolean;
  rainThreshold: number;
  heatThresholdCelsius: number;
  refundType: RefundType;
}

export interface TurfEnvironment {
  turfId: number;
  turfName?: string;
  type: TurfEnvironmentType;
  rainProtection: boolean;
  heatProtection: boolean;
}

export interface WeatherDashboard {
  city: string;
  current: Forecast;
  forecast: Forecast[];
  recommendations: Playability[];
  highRiskBookings: number;
  weatherRefunds: number;
  insight: string;
}
