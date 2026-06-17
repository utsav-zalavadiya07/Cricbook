import api from "@/services/api";
import {
  RegisterRequest,
  LoginRequest,
  OtpVerifyRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from "../types/auth.types";

export const registerUser = (data: RegisterRequest) =>
  api.post("/auth/register", data);

export const loginUser = (data: LoginRequest) =>
  api.post("/auth/login", data);

export const requestPasswordReset = (data: ForgotPasswordRequest) =>
  api.post("/auth/forgot-password", data);

export const resetPassword = (data: ResetPasswordRequest) =>
  api.post("/auth/reset-password", data);

export const verifyOtp = (data: OtpVerifyRequest) =>
  api.post("/auth/verify-otp", data);

export const resendOtp = (email: string) =>
  api.post("/auth/resend-otp", { email });
