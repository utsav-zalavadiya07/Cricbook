export const SESSION_COOKIE_NAMES = {
  accessToken: "cricbook_access",
  refreshToken: "cricbook_refresh",
  role: "cricbook_role",
} as const;

export const ADMIN_ROLES = [
  "ADMIN",
  "SUPER_ADMIN",
  "PLATFORM_ADMIN",
  "FINANCE_ADMIN",
  "SUPPORT_ADMIN",
  "CONTENT_ADMIN",
  "OPERATIONS_ADMIN",
] as const;

const ownerRoles = ["OWNER", ...ADMIN_ROLES] as const;

const authenticatedPrefixes = [
  "/assistant",
  "/booking",
  "/favorites",
  "/feed",
  "/inbox",
  "/live-scoring",
  "/matches",
  "/membership",
  "/notifications",
  "/payment",
  "/personalized",
  "/profile",
  "/progress",
  "/rebook",
  "/referrals",
  "/rewards",
  "/settings",
  "/smart",
  "/social",
  "/split-payments",
  "/subscription",
  "/teams",
  "/tournaments",
  "/trust",
  "/wallet",
  "/weather",
] as const;

export type RouteAccess =
  | { public: true; loginPath: "/auth/login" | "/admin/login"; roles?: never }
  | { public: false; loginPath: "/auth/login" | "/admin/login"; roles?: readonly string[] };

function matchesPrefix(pathname: string, prefix: string) {
  return pathname === prefix || pathname.startsWith(`${prefix}/`);
}

function isOwnerTurfManagementPath(pathname: string) {
  return pathname === "/turf/create"
    || pathname === "/turf/my-turfs"
    || /^\/turf\/[^/]+\/(edit|slots)(\/.*)?$/.test(pathname);
}

export function normalizePathname(pathname: string) {
  const path = (pathname || "/").split(/[?#]/)[0] || "/";

  if (path === "/") {
    return "/";
  }

  return path.endsWith("/") ? path.slice(0, -1) : path;
}

export function getRouteAccess(pathname: string): RouteAccess {
  const path = normalizePathname(pathname);

  if (path === "/admin/login") {
    return { public: true, loginPath: "/admin/login" };
  }

  if (matchesPrefix(path, "/admin")) {
    return { public: false, loginPath: "/admin/login", roles: ADMIN_ROLES };
  }

  if (matchesPrefix(path, "/auth")) {
    return { public: true, loginPath: "/auth/login" };
  }

  if (matchesPrefix(path, "/owner") || isOwnerTurfManagementPath(path)) {
    return { public: false, loginPath: "/auth/login", roles: ownerRoles };
  }

  if (authenticatedPrefixes.some((prefix) => matchesPrefix(path, prefix))) {
    return { public: false, loginPath: "/auth/login" };
  }

  return { public: true, loginPath: "/auth/login" };
}

export function canRoleAccessPath(role: string | null | undefined, pathname: string) {
  const access = getRouteAccess(pathname);
  if (access.public) {
    return true;
  }

  if (!role) {
    return false;
  }

  return access.roles ? access.roles.includes(role) : true;
}

export function getLandingPathForRole(role: string | null | undefined) {
  if (!role) {
    return "/search";
  }

  if (role === "OWNER" || ADMIN_ROLES.includes(role as typeof ADMIN_ROLES[number])) {
    return role === "OWNER" ? "/owner/dashboard" : "/admin";
  }

  return "/search";
}

export function isSafeNextPath(value: string | null | undefined) {
  return Boolean(value && value.startsWith("/") && !value.startsWith("//") && !value.includes("://"));
}

export function safeNextPath(value: string | null | undefined, fallback: string) {
  return isSafeNextPath(value) ? value as string : fallback;
}
