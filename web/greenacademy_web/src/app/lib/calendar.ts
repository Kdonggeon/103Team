
const BASE = "/backend";

export type MonthItem = {
  date: string;
  items: { classId: string; className: string; startTime: string; endTime: string; roomNumber?: number }[];
};

function authHeaders(): Record<string, string> {
  const h: Record<string, string> = {};
  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("session") ?? localStorage.getItem("login");
      if (raw) {
        const token = (JSON.parse(raw) as { token?: string | null })?.token ?? null;
        if (token) h.Authorization = `Bearer ${token}`;
      }
    } catch {
      /* ignore */
    }
  }
  return h;
}

/** 월간 수업 일정 */
export async function fetchMonth(yyyymm: string): Promise<MonthItem[]> {
  const res = await fetch(`${BASE}/api/calendar/month?yyyymm=${yyyymm}`, {
    headers: { ...authHeaders() },   // ✅ 이제 타입 에러 없음
    credentials: "include",
  });
  if (!res.ok) throw new Error(`month ${res.status}`);
  return res.json();
}

/** 날짜 토글 */
export async function toggleDate(classId: string, date: string): Promise<void> {
  const res = await fetch(`${BASE}/api/calendar/toggle`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() }, // ✅ ok
    credentials: "include",
    body: JSON.stringify({ classId, date }),
  });
  if (!res.ok) throw new Error(`toggle ${res.status}`);
}
