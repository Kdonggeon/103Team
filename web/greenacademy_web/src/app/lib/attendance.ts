// src/app/lib/api/attendance.ts
export type AttendanceRow = { studentId?: string; status?: string };

async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, { credentials: "include", ...(init || {}) });
  const text = await res.text();
  const data = text ? (() => { try { return JSON.parse(text); } catch { return text as any; } })() : null;
  if (!res.ok) throw new Error(typeof data === "string" ? data : (data?.message ?? `HTTP ${res.status}`));
  return data as T;
}

export async function getClassAttendance(classId: string, date?: string): Promise<AttendanceRow[]> {
  const qs = date ? `?date=${encodeURIComponent(date)}` : "";
  return http(`/api/teachers/classes/${encodeURIComponent(classId)}/attendance${qs}`);
}
