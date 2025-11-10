// src/app/lib/api/schedules.ts
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

async function http<T>(url: string, init?: RequestInit & { json?: any }): Promise<T> {
  const { json, headers, ...rest } = init ?? {};
  const res = await fetch(url, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(headers || {}) },
    ...(rest as RequestInit),
    body: json !== undefined ? JSON.stringify(json) : (rest as RequestInit)?.body,
  });
  const text = await res.text();
  const data = text ? (() => { try { return JSON.parse(text); } catch { return text as any; } })() : null;
  if (!res.ok) throw new Error(typeof data === "string" ? data : (data?.message ?? `HTTP ${res.status}`));
  return data as T;
}

export async function listSchedules(teacherId: string, from: string, to: string): Promise<ScheduleItem[]> {
  const q = new URLSearchParams({ from, to }).toString();
  return http(`/api/manage/teachers/${encodeURIComponent(teacherId)}/schedules?${q}`);
}

export async function createSchedule(teacherId: string, body: {
  date: string;
  classId: string;
  title?: string;
  startTime?: string;
  endTime?: string;
  roomNumber?: number;
  memo?: string;
}): Promise<ScheduleItem> {
  return http(`/api/manage/teachers/${encodeURIComponent(teacherId)}/schedules`, {
    method: "POST",
    json: body,
  });
}

export async function deleteSchedule(teacherId: string, scheduleId: string): Promise<void> {
  await http(`/api/manage/teachers/${encodeURIComponent(teacherId)}/schedules/${encodeURIComponent(scheduleId)}`, {
    method: "DELETE",
  });
}

/** 필요하면 undefined 안전 버전도 함께 제공 */
export async function deleteScheduleSafe(teacherId: string, scheduleId?: string): Promise<void> {
  if (!scheduleId) return;
  return deleteSchedule(teacherId, scheduleId);
}
