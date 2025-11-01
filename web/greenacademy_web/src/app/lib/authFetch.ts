// src/app/lib/authFetch.ts
import { getSession, clearSession } from "@/app/lib/session";

export async function authFetch(input: string, init: RequestInit = {}) {
  const token = getSession()?.token;
  if (!token) {
    // 여기서 바로 막아주면 "Authorization present? NO" 상황을 예방
    throw new Error("로그인이 만료되었습니다. 다시 로그인해 주세요.");
  }

  const headers = new Headers(init.headers || {});
  headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  return fetch(input, {
    ...init,
    headers,
    credentials: "include",
    cache: "no-store",
  });
}
