// src/app/lib/rooms.vector.ts
import { getSession } from "@/app/lib/session";

/* ---------- 타입 ---------- */
export type VectorSeat = {
  id: string;
  label: string;
  x: number; y: number; w: number; h: number;
  r?: number;
  disabled?: boolean;
  /** ✅ 좌석 배정된 학생 */
  studentId?: string | null;
  /** (선택) 서버에서 내려줄 수 있음 */
  occupiedAt?: string | null;
};

export type VectorLayout = {
  version: number;
  canvasW: number;
  canvasH: number;
  seats: VectorSeat[];
};

export type AdminRoomLite = {
  academyNumber: number;
  roomNumber: number;
  hasVector: boolean;
  vectorSeatCount: number;
};

/* ---------- 유틸 ---------- */
/**
 * API_BASE: 백엔드가 다른 오리진(예: 9090)일 때 절대 URL 앞부분
 * BACKEND_PREFIX: 프록시를 /backend 로 태워줄 때만 "/backend", 아니면 ""(빈값)
 *
 * 예시1) 프록시 없이 CORS로 직통:
 *   NEXT_PUBLIC_API_BASE=http://localhost:9090
 *   NEXT_PUBLIC_BACKEND_PREFIX=
 *
 * 예시2) 프록시로 같은 오리진에서 /backend → 9090:
 *   NEXT_PUBLIC_API_BASE=
 *   NEXT_PUBLIC_BACKEND_PREFIX=/backend
 */
const API_BASE =
  (typeof window !== "undefined" && (window as any).__API_BASE__) ||
  process.env.NEXT_PUBLIC_API_BASE ||
  "";

const BACKEND_PREFIX = process.env.NEXT_PUBLIC_BACKEND_PREFIX ?? "/backend";

/** 항상 절대/정규화된 URL을 만든다 */
function absUrl(path: string) {
  const p = path.startsWith("/") ? path : `/${path}`;
  const withPrefix = `${BACKEND_PREFIX}${p}`.replace(/\/{2,}/g, "/");
  return API_BASE ? `${API_BASE}${withPrefix}` : withPrefix;
}

function makeHeaders(withJson = false): Headers {
  const h = new Headers();
  if (withJson) h.set("Content-Type", "application/json");
  const token = getSession()?.token;
  if (token) h.set("Authorization", `Bearer ${token}`);
  return h;
}

async function handle<T = any>(res: Response): Promise<T> {
  const text = await res.text();
  if (!res.ok) {
    let msg = text || `${res.status} ${res.statusText}`;
    try { const j = JSON.parse(text); msg = j?.message ?? msg; } catch {}
    throw new Error(msg);
  }
  return (text ? JSON.parse(text) : {}) as T;
}

// 다양한 필드명을 roomNumber로 통일
const pickRoomNumber = (o: any): number | undefined => {
  const rn = o?.roomNumber ?? o?.Room_Number ?? o?.number ?? o?.room_no ?? o?.RoomNo;
  const n = typeof rn === "string" ? parseInt(rn, 10) : Number(rn);
  return Number.isFinite(n) ? n : undefined;
};

// AdminRoomLite로 통일 맵핑
const toLite = (row: any, academyNumber: number): AdminRoomLite | null => {
  const roomNumber = pickRoomNumber(row);
  if (!Number.isFinite(roomNumber)) return null;

  const seats =
    row?.vectorLayout ??
    row?.seats ??
    row?.vectorSeats ??
    row?.layout?.seats ??
    [];

  const hasVector =
    !!row?.hasVector ||
    (Array.isArray(seats) && seats.length > 0) ||
    !!row?.vectorSeatCount;

  const vectorSeatCount =
    Number(row?.vectorSeatCount) ||
    (Array.isArray(seats) ? seats.length : 0);

  return {
    academyNumber,
    roomNumber: roomNumber as number,
    hasVector: !!hasVector,
    vectorSeatCount: Number.isFinite(vectorSeatCount) ? vectorSeatCount : 0,
  };
};

/* ---------- API (vector + listRooms 통합) ---------- */
export const roomsVectorApi = {
  /** 학원 전체 방 목록 (간단 DTO) */
  async list(academyNumber: number): Promise<AdminRoomLite[]> {
    if (!academyNumber) throw new Error("academyNumber is required");

    const candidates = [
      `/api/admin/rooms.vector-lite?academyNumber=${academyNumber}`,
      `/api/admin/rooms/vector-lite?academyNumber=${academyNumber}`,
      `/api/admin/rooms.vector?academyNumber=${academyNumber}`,
      `/api/admin/rooms?academyNumber=${academyNumber}`, // fallback
    ];

    let lastError: any = null;
    for (const path of candidates) {
      try {
        const url = absUrl(path);
        const json = await handle<any>(await fetch(url, {
          method: "GET",
          credentials: "include",
          headers: makeHeaders(),
        }));
        const arr: any[] = Array.isArray(json) ? json : (Array.isArray(json?.items) ? json.items : []);
        const out = arr.map(row => toLite(row, academyNumber)).filter((x): x is AdminRoomLite => !!x);
        return out; // 비어있어도 그대로 반환(엔드포인트가 존재)
      } catch (e) { lastError = e; }
    }
    throw lastError ?? new Error("Failed to load rooms");
  },

  /** (구호환용) rooms.ts의 listRooms 대체 */
  async listRooms(academyNumber: number): Promise<AdminRoomLite[]> {
    return this.list(academyNumber);
  },

  /** 특정 방 벡터 레이아웃 조회 */
  async get(roomNumber: number, academyNumber: number): Promise<VectorLayout | null> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = absUrl(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    return handle(await fetch(url, {
      method: "GET",
      credentials: "include",
      headers: makeHeaders(),
    }));
  },

  /** 저장(전체 교체) — studentId 포함해서 보냄 */
  async put(roomNumber: number, academyNumber: number, body: VectorLayout): Promise<void> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = absUrl(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    await handle(await fetch(url, {
      method: "PUT",
      credentials: "include",
      headers: makeHeaders(true),
      body: JSON.stringify(body),
    }));
  },

  /** 부분 수정이 필요하면 사용 */
  async patch(roomNumber: number, academyNumber: number, body: Partial<VectorLayout>): Promise<void> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = absUrl(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    await handle(await fetch(url, {
      method: "PATCH",
      credentials: "include",
      headers: makeHeaders(true),
      body: JSON.stringify(body),
    }));
  },

  /** 방 삭제 */
  async delete(roomNumber: number, academyNumber: number): Promise<void> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = absUrl(`/api/admin/rooms/${roomNumber}?academyNumber=${academyNumber}`);
    await handle(await fetch(url, {
      method: "DELETE",
      credentials: "include",
      headers: makeHeaders(),
    }));
  },
};

export type Room = never; // 구파일 대체용(불필요)
