// src/app/lib/rooms.ts
import { ApiError } from "@/app/lib/api";

/** 좌석 한 칸(그리드) 정보 */
export type SeatCellLike = {
  seatNumber: number;
  row?: number;
  col?: number;
  disabled?: boolean;
};

/** 백엔드 응답의 다양한 키 이름을 모두 수용하는 Room 타입 */
export type Room = {
  // 번호 계열
  roomNumber?: number;     // 선호
  number?: number;         // 백엔드가 이렇게 줄 수도 있음
  Room_Number?: number;    // Mongo에 이렇게 있을 수도 있음

  // 이름 계열
  name?: string;
  roomName?: string;
  Room_Name?: string;

  // 기존 좌석 정보 (백엔드에서 이렇게 줄 수 있음)
  seats?: SeatCellLike[];

  // 👉 RoomCard / RoomGridEditor 에서 사용하는 좌석 레이아웃 필드
  //    (벡터 좌석판용)
  layout?: SeatCellLike[];
  rows?: number;
  cols?: number;
};

/**
 * 베이스 URL
 * - 실제 운영 기준:
 *   - NEXT_PUBLIC_API_BASE가 있으면 그걸 사용
 *   - 없으면 /backend (Next rewrite → 백엔드로 프록시)
 * - 더 이상 localhost:9090 하드코딩 없음
 */
const API_BASE = (process.env.NEXT_PUBLIC_API_BASE ?? "/backend").replace(/\/$/, "");

/** 백엔드 토큰 가져오기 (로컬스토리지 여러 키 방어적으로 탐색) */
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
    } catch {
      // raw 자체가 토큰인 경우
    }

    t = String(t).trim();
    if (!t || t === "null" || t === "undefined") return null;
    if (t.toLowerCase().startsWith("bearer ")) t = t.slice(7).trim();
    return t;
  } catch {
    return null;
  }
}

async function coreFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const p = path.startsWith("/") ? path : `/${path}`;
  const url = /^https?:\/\//i.test(path) ? path : `${API_BASE}${p}`;

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
    try {
      body = text ? JSON.parse(text) : undefined;
    } catch {}
    throw new ApiError(
      res.status,
      body?.message || `${res.status} ${res.statusText}`,
      body
    );
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
      const room: Room = { ...(x as Room) };
      if (rn) room.roomNumber = rn;
      return room;
    });
  },
};

export default roomsApi;
