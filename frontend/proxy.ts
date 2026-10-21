import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { canRoleAccessPath, getLandingPathForRole, getRouteAccess, SESSION_COOKIE_NAMES } from "./modules/auth/sessionPolicy";

type JwtPayload = {
  exp?: number;
  role?: string;
};

function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const encodedPayload = token.split(".")[1];
    if (!encodedPayload) {
      return null;
    }
    const normalizedPayload = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, "=");
    return JSON.parse(atob(paddedPayload)) as JwtPayload;
  } catch {
    return null;
  }
}

function isPayloadExpired(payload: JwtPayload | null) {
  return !payload?.exp || payload.exp <= Math.floor(Date.now() / 1000);
}

function clearSession(response: NextResponse) {
  for (const name of Object.values(SESSION_COOKIE_NAMES)) {
    response.cookies.set(name, "", { path: "/", maxAge: 0 });
  }
}

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const access = getRouteAccess(pathname);

  if (access.public) {
    return NextResponse.next();
  }

  const accessToken = request.cookies.get(SESSION_COOKIE_NAMES.accessToken)?.value;
  const refreshToken = request.cookies.get(SESSION_COOKIE_NAMES.refreshToken)?.value;
  const cookieRole = request.cookies.get(SESSION_COOKIE_NAMES.role)?.value;
  const accessPayload = accessToken ? decodeJwtPayload(accessToken) : null;
  const validAccessToken = Boolean(accessPayload && !isPayloadExpired(accessPayload));
  const malformedAccessToken = Boolean(accessToken && !accessPayload);
  const role = accessPayload?.role || cookieRole;
  const hasSession = !malformedAccessToken && Boolean(validAccessToken || refreshToken);

  if (hasSession && canRoleAccessPath(role, pathname)) {
    return NextResponse.next();
  }

  // Authenticated user on wrong portal → redirect to their correct portal landing page.
  if (hasSession && role && !canRoleAccessPath(role, pathname)) {
    const landingPath = getLandingPathForRole(role);
    const redirectUrl = new URL(landingPath, request.url);
    return NextResponse.redirect(redirectUrl);
  }

  // Not authenticated → redirect to login.
  const loginUrl = new URL(access.loginPath, request.url);
  const response = NextResponse.redirect(loginUrl);

  if (!hasSession || malformedAccessToken) {
    clearSession(response);
  }

  return response;
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
