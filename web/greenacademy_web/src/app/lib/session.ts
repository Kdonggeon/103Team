"use client";
import type { LoginResponse } from "./api";

export function saveSession(resp: LoginResponse) {
  // token이 null일 수도 있으니 문자열로 보정
  const token = (resp.token ?? "").toString();
  const data = { ...resp, token };
  localStorage.setItem("session", JSON.stringify(data));
}

export function clearSession() {
  localStorage.removeItem("session");
  localStorage.removeItem("login");
  localStorage.removeItem("auth");
}

export function getSession(): LoginResponse | null {
  try {
    const raw = localStorage.getItem("session") ?? localStorage.getItem("login");
    return raw ? (JSON.parse(raw) as LoginResponse) : null;
  } catch {
    return null;
  }
}
export { saveSession as setSession };
