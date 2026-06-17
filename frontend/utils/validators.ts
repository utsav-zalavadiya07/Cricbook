export const validators = {
  email: (value: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) ? "" : "Enter a valid email address.",

  password: (value: string) =>
    value.length >= 8 ? "" : "Password must be at least 8 characters.",

  name: (value: string) =>
    value.trim().length >= 2 ? "" : "Enter at least 2 characters.",

  phone: (value: string) =>
    /^[0-9]{10}$/.test(value) ? "" : "Enter a 10-digit mobile number.",

  otp: (value: string) =>
    /^[0-9]{6}$/.test(value) ? "" : "Enter the 6-digit OTP.",
};
