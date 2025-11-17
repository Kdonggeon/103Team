// src/app/lib/api/attendance.ts
import { getSession } from "@/app/lib/session";

export type AttendanceRow = { studentId?: string; status?: string };

// 공통 prefix (다른 파일들과 통일)
const BACKEND_PREFIX = process.env.NEXT_PUBLIC_BACKEND_PREFIX ?? "/backend";

function abs(path: string) {
  const p = path.startsWith("/") ? path : `/${path}`;
  // /backend + /api/... → 중복 슬래시 제거
  return `${BACKEND_PREFIX}${p}`.replace(/\/{2,}/g, "/");
}

async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers = new Headers(init?.headers || {});
  if (session?.token) headers.set("Authorization", `Bearer ${session.token}`);
  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const res = await fetch(abs(path), {
    credentials: "include",
    cache: "no-store",
    ...init,
    headers,
  });

  const text = await res.text();
  const data = text
    ? (() => {
        try { return JSON.parse(text); } catch { return text as any; }
      })()
    : null;

  if (!res.ok) {
    throw new Error(
      typeof data === "string" ? data : (data?.message ?? `HTTP ${res.status}`)
    );
  }
  return data as T;
}

export async function getClassAttendance(
  classId: string,
  date?: string
): Promise<AttendanceRow[]> {
  const qs = date ? `?date=${encodeURIComponent(date)}` : "";
  return http(`/api/teachers/classes/${encodeURIComponent(classId)}/attendance${qs}`);
}
