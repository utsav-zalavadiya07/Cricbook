export interface RegisterRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  role?: "USER" | "OWNER";
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface ForgotPasswordRequest {
  loginId: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface OtpVerifyRequest {
  email: string;
  otp: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  refreshExpiresIn?: number;
  sessionId?: string;
  role: "USER" | "OWNER" | "ADMIN";
}
