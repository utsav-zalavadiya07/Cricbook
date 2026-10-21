import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { SESSION_COOKIE_NAMES, getLandingPathForRole } from "@/modules/auth/sessionPolicy";

function decodeJwtPayload(token: string) {
  try {
    const encodedPayload = token.split(".")[1];
    if (!encodedPayload) {
      return null;
    }
    const normalizedPayload = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, "=");
    return JSON.parse(Buffer.from(paddedPayload, "base64").toString("utf8")) as { exp?: number; role?: string };
  } catch {
    return null;
  }
}

export default async function Home() {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(SESSION_COOKIE_NAMES.accessToken)?.value ?? "";
  const roleCookie = cookieStore.get(SESSION_COOKIE_NAMES.role)?.value ?? "";
  const payload = accessToken ? decodeJwtPayload(accessToken) : null;
  const expired = !payload?.exp || payload.exp <= Math.floor(Date.now() / 1000);
  const role = payload?.role || roleCookie || null;

  if (accessToken && !expired && role) {
    redirect(getLandingPathForRole(role));
  }

  redirect("/auth/login");
}
