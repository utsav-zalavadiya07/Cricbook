const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

function backendOrigin() {
  try {
    return new URL(API_BASE_URL).origin;
  } catch {
    return "http://localhost:8080";
  }
}

export const getImageUrl = (path: string | undefined | null) => {
  const value = path?.trim();
  if (!value) return "";
  if (/^(https?:|data:|blob:)/i.test(value)) return value;

  return value.startsWith("/") ? `${backendOrigin()}${value}` : `${backendOrigin()}/${value}`;
};
