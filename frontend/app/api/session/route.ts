import { NextRequest, NextResponse } from "next/server";
import { ADMIN_ROLES, SESSION_COOKIE_NAMES } from "@/modules/auth/sessionPolicy";

const secureCookie = process.env.NODE_ENV === "production";
const knownRoles = new Set<string>(["USER", "OWNER", ...ADMIN_ROLES]);

type SessionPayload = {
  accessToken?: string;
  refreshToken?: string;
  role?: string;
  remember?: boolean;
};

type JwtPayload = {
  exp?: number;
  role?: string;
};

function baseCookieOptions(maxAge?: number) {
  return {
    httpOnly: true,
    sameSite: "lax" as const,
    secure: secureCookie,
    path: "/",
    priority: "high" as const,
    ...(maxAge ? { maxAge } : {}),
  };
}

function decodeJwtPayload(token: string | undefined) {
  if (!token) {
    return null;
  }

  try {
    return JSON.parse(Buffer.from(token.split(".")[1] || "", "base64url").toString("utf8")) as JwtPayload;
  } catch {
    return null;
  }
}

function secondsUntilJwtExpiry(token: string | undefined) {
  const payload = decodeJwtPayload(token);
  return payload?.exp ? payload.exp - Math.floor(Date.now() / 1000) : null;
}

function maxAgeFromJwt(token: string | undefined, fallbackSeconds: number) {
  const secondsUntilExpiry = secondsUntilJwtExpiry(token);
  return secondsUntilExpiry == null ? fallbackSeconds : Math.max(1, secondsUntilExpiry);
}

export async function POST(request: NextRequest) {
  const payload = await request.json().catch(() => null) as SessionPayload | null;
  const accessPayload = decodeJwtPayload(payload?.accessToken);
  const accessSecondsUntilExpiry = secondsUntilJwtExpiry(payload?.accessToken);

  if (!payload?.accessToken
    || !payload.role
    || !knownRoles.has(payload.role)
    || !accessPayload
    || Boolean(accessPayload.role && accessPayload.role !== payload.role)
    || accessSecondsUntilExpiry == null
    || accessSecondsUntilExpiry <= 0) {
    return NextResponse.json({ message: "Invalid session payload." }, { status: 400 });
  }

  const response = NextResponse.json({ ok: true });
  const accessMaxAge = Math.max(1, accessSecondsUntilExpiry);
  const refreshMaxAge = maxAgeFromJwt(payload.refreshToken, 24 * 60 * 60);
  const roleMaxAge = Math.max(accessMaxAge, refreshMaxAge);
  const persistent = payload.remember === true;

  response.cookies.set(SESSION_COOKIE_NAMES.accessToken, payload.accessToken, baseCookieOptions(persistent ? accessMaxAge : undefined));
  if (payload.refreshToken) {
    response.cookies.set(SESSION_COOKIE_NAMES.refreshToken, payload.refreshToken, baseCookieOptions(persistent ? refreshMaxAge : undefined));
  } else {
    response.cookies.set(SESSION_COOKIE_NAMES.refreshToken, "", { ...baseCookieOptions(), maxAge: 0 });
  }
  response.cookies.set(SESSION_COOKIE_NAMES.role, payload.role, baseCookieOptions(persistent ? roleMaxAge : undefined));

  return response;
}

export function DELETE() {
  const response = NextResponse.json({ ok: true });

  for (const name of Object.values(SESSION_COOKIE_NAMES)) {
    response.cookies.set(name, "", { ...baseCookieOptions(1), maxAge: 0 });
  }

  return response;
}
