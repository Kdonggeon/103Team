// src/app/lib/rooms.ts
import { ApiError } from "@/app/lib/api";

/** 백엔드 응답의 다양한 키 이름을 모두 수용하는 Room 타입 */
export type Room = {
  roomNumber?: number;     // 선호
  number?: number;         // 백엔드가 이렇게 줄 수도 있음
  Room_Number?: number;    // Mongo에 이렇게 있을 수도 있음

  name?: string;
  roomName?: string;
  Room_Name?: string;

  seats?: Array<{
    seatNumber: number;
    row?: number;
    col?: number;
    disabled?: boolean;
  }>;
};

function getBaseUrl() {
  if (typeof window !== "undefined" && (window as any)?.__API_BASE__) {
    return (window as any).__API_BASE__;
  }
  return process.env.NEXT_PUBLIC_API_BASE || "http://localhost:9090";
}

function getToken(): string | null {
  try {
    const raw =
      localStorage.getItem("session") ??
      localStorage.getItem("login") ??
      localStorage.getItem("auth") ??
      null;
    if (!raw) return null;
    let t = raw;
    try {
      const parsed = JSON.parse(raw) as { token?: string };
      t = (parsed?.token ?? raw) as string;
    } catch { /* raw is token */ }
    t = String(t).trim();
    if (!t || t === "null" || t === "undefined") return null;
    if (t.toLowerCase().startsWith("bearer ")) t = t.slice(7).trim();
    return t;
  } catch { return null; }
}

async function coreFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const base = getBaseUrl();
  const url = /^https?:\/\//i.test(path) ? path : `${base}${path.startsWith("/") ? "" : "/"}${path}`;
  const headers: Record<string, string> = { ...(init.headers as any) };

  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;
  if (init.body && !(init.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  console.log("[roomsApi] fetch", url);
  const res = await fetch(url, { credentials: "include", ...init, headers });
  const text = await res.text();

  if (!res.ok) {
    let body: any;
    try { body = text ? JSON.parse(text) : undefined; } catch {}
    throw new ApiError(res.status, body?.message || `${res.status} ${res.statusText}`, body);
  }
  return text ? (JSON.parse(text) as T) : ({} as T);
}

/** 다양한 키에서 방 번호를 뽑아 정수로 통일 */
export function normalizeRoomNumber(room: Room): number | undefined {
  const v = (room.roomNumber ?? room.number ?? room.Room_Number) as any;
  const n = typeof v === "string" ? Number(v) : v;
  return typeof n === "number" && Number.isFinite(n) ? n : undefined;
}

export const roomsApi = {
  /** 관리자 방 목록 가져오기: /api/admin/rooms?academyNumber=103 */
  async listRooms(academyNumber: number): Promise<Room[]> {
    if (academyNumber == null || Number.isNaN(Number(academyNumber))) {
      console.warn("[roomsApi] invalid academyNumber:", academyNumber);
      return [];
    }
    const qs = `academyNumber=${encodeURIComponent(String(academyNumber))}`;
    const data = await coreFetch<any[]>(`/api/admin/rooms?${qs}`);
    const arr = Array.isArray(data) ? data : [];
    return arr.map((x) => {
      const rn = normalizeRoomNumber(x as Room);
      return rn ? { ...(x as Room), roomNumber: rn } : (x as Room);
    });
  },
};

export default roomsApi;
