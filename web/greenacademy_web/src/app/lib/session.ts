// src/app/lib/session.ts
import type { LoginResponse, Role } from "@/app/lib/api";

export type Session = LoginResponse;

// session 우선, 없으면 legacy login 키도 읽기
export function getSession(): Session | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem("session") ?? localStorage.getItem("login");
  return raw ? (JSON.parse(raw) as Session) : null;
}

export function setSession(s: Session) {
  if (typeof window === "undefined") return;
  const json = JSON.stringify(s);
  // 표준 키
  localStorage.setItem("session", json);
  // 레거시 키도 함께 저장(기존 코드 호환)
  localStorage.setItem("login", json);
}

export function clearSession() {
  if (typeof window === "undefined") return;
  localStorage.removeItem("session");
  localStorage.removeItem("login"); // 레거시도 같이 정리
}

export function requireRole(roles: Role[]): boolean {
  const s = getSession();
  return !!s && roles.includes(s.role);
}

export function firstAcademyNo(): number | null {
  const s = getSession();
  return s?.academyNumbers?.[0] ?? null;
}
