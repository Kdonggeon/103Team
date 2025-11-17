// src/app/lib/api/schedules.ts
import { getSession } from "@/app/lib/session";

export type ScheduleItem = {
  scheduleId?: string;
  teacherId?: string;
  date: string;       // YYYY-MM-DD
  classId?: string;
  title?: string;
  startTime?: string;
  endTime?: string;
  roomNumber?: number;
  memo?: string;
};

const BACKEND_PREFIX = process.env.NEXT_PUBLIC_BACKEND_PREFIX ?? "/backend";

function abs(path: string) {
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${BACKEND_PREFIX}${p}`.replace(/\/{2,}/g, "/");
}

async function http<T>(
  path: string,
  init?: RequestInit & { json?: any }
): Promise<T> {
  const { json, headers, ...rest } = init ?? {};
  const session = getSession();

  const h: HeadersInit = {
    "Content-Type": "application/json",
    ...(headers || {}),
    ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
  };

  const res = await fetch(abs(path), {
    credentials: "include",
    cache: "no-store",
    ...(rest as RequestInit),
    headers: h,
    body: json !== undefined ? JSON.stringify(json) : (rest as RequestInit)?.body,
  });

  const text = await res.text();
  const data = text
    ? (() => { try { return JSON.parse(text); } catch { return text as any; } })()
    : null;

  if (!res.ok) {
    throw new Error(
      typeof data === "string" ? data : (data?.message ?? `HTTP ${res.status}`)
    );
  }
  return data as T;
}

export async function listSchedules(
  teacherId: string,
  from: string,
  to: string
): Promise<ScheduleItem[]> {
  const q = new URLSearchParams({ from, to }).toString();
  return http(`/api/manage/teachers/${encodeURIComponent(teacherId)}/schedules?${q}`);
}

export async function createSchedule(
  teacherId: string,
  body: {
    date: string;
    classId: string;
    title?: string;
    startTime?: string;
    endTime?: string;
    roomNumber?: number;
    memo?: string;
  }
): Promise<ScheduleItem> {
  return http(`/api/manage/teachers/${encodeURIComponent(teacherId)}/schedules`, {
    method: "POST",
    json: body,
  });
}

export async function deleteSchedule(
  teacherId: string,
  scheduleId: string
): Promise<void> {
  await http(
    `/api/manage/teachers/${encodeURIComponent(teacherId)}/schedules/${encodeURIComponent(scheduleId)}`,
    { method: "DELETE" }
  );
}

export async function deleteScheduleSafe(
  teacherId: string,
  scheduleId?: string
): Promise<void> {
  if (!scheduleId) return;
  return deleteSchedule(teacherId, scheduleId);
}
