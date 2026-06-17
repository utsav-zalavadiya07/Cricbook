type PersistSessionPayload = {
  accessToken: string;
  refreshToken?: string | null;
  role: string;
  remember?: boolean;
};

export async function persistSessionCookies(payload: PersistSessionPayload) {
  if (typeof window === "undefined") {
    return;
  }

  const response = await fetch("/api/session", {
    method: "POST",
    credentials: "same-origin",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Could not establish secure browser session.");
  }
}

export async function clearSessionCookies() {
  if (typeof window === "undefined") {
    return;
  }

  await fetch("/api/session", {
    method: "DELETE",
    credentials: "same-origin",
  }).catch(() => undefined);
}
