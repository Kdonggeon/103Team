// src/app/lib/rooms.vector.ts
import { getSession } from "@/app/lib/session";


/* ---------- 타입 ---------- */
export type VectorSeat = {
  id?: string;
  _id?: string;
  label: string;
  x: number; y: number; w: number; h: number;
  r?: number;
  disabled?: boolean;
  studentId?: string | null;
  occupiedAt?: string | null;
};

export type VectorLayout = {
  version: number;
  canvasW: number;
  canvasH: number;
  seats: VectorSeat[];
};

export type VectorLayoutV2Payload = {
  vectorVersion: number;
  vectorCanvasW: number;
  vectorCanvasH: number;
  vectorLayoutV2: Array<{
    _id: string;
    label: string;
    x: number; y: number; w: number; h: number;
    disabled: boolean;
    Student_ID: string | null;
  }>;
};

export type AdminRoomLite = {
  academyNumber: number;
  roomNumber: number;
  hasVector: boolean;
  vectorSeatCount: number;
};

/* ---------- 유틸 ---------- */
const API_BASE =
  (typeof window !== "undefined" && (window as any).__API_BASE__) ||
  process.env.NEXT_PUBLIC_API_BASE ||
  "";

const BACKEND_PREFIX = process.env.NEXT_PUBLIC_BACKEND_PREFIX ?? "/backend";

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

const pickRoomNumber = (o: any): number | undefined => {
  const rn =
    o?.roomNumber ?? o?.Room_Number ?? o?.number ?? o?.room_no ?? o?.RoomNo;
  const n = typeof rn === "string" ? parseInt(rn, 10) : Number(rn);
  return Number.isFinite(n) ? n : undefined;
};

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

/* ---------- API ---------- */
export const roomsVectorApi = {
  /** ✅ 방 목록 */
  async list(academyNumber: number): Promise<AdminRoomLite[]> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const candidates = [
      `/api/admin/rooms.vector-lite?academyNumber=${academyNumber}`,
      `/api/admin/rooms/vector-lite?academyNumber=${academyNumber}`,
      `/api/admin/rooms.vector?academyNumber=${academyNumber}`,
      `/api/admin/rooms?academyNumber=${academyNumber}`,
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
        const arr: any[] = Array.isArray(json)
          ? json
          : (Array.isArray(json?.items) ? json.items : []);
        const out = arr
          .map(row => toLite(row, academyNumber))
          .filter((x): x is AdminRoomLite => !!x);
        return out;
      } catch (e) { lastError = e; }
    }
    throw lastError ?? new Error("Failed to load rooms");
  },

  async listRooms(academyNumber: number): Promise<AdminRoomLite[]> {
    return this.list(academyNumber);
  },

  /** ✅ 레이아웃 조회 (V1/V2 둘 다 지원) */
  async get(
    roomNumber: number,
    academyNumber: number
  ): Promise<any> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = absUrl(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    const res = await fetch(url, {
      method: "GET",
      credentials: "include",
      headers: makeHeaders(),
    });
    // 💡 여기서 타입 제한하지 말고 그대로 반환 (normalizeLayout에서 처리)
    return handle<any>(res);
  },

  /** ✅ 저장 (V1/V2 모두 허용) */
  async put(
    roomNumber: number,
    academyNumber: number,
    body: VectorLayout | VectorLayoutV2Payload
  ): Promise<void> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = absUrl(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    await handle(await fetch(url, {
      method: "PUT",
      credentials: "include",
      headers: makeHeaders(true),
      body: JSON.stringify(body),
    }));
  },

  /** 부분 수정 */
  async patch(
    roomNumber: number,
    academyNumber: number,
    body: Partial<VectorLayout> | Partial<VectorLayoutV2Payload>
  ): Promise<void> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = absUrl(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    await handle(await fetch(url, {
      method: "PATCH",
      credentials: "include",
      headers: makeHeaders(true),
      body: JSON.stringify(body),
    }));
  },

  /** 삭제 */
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

export type Room = never;
