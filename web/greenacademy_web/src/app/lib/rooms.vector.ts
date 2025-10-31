// src/app/lib/rooms.vector.ts
import { getSession } from "@/app/lib/session";

/* ---------- 타입 ---------- */
export type VectorSeat = {
  id: string;
  label: string;
  x: number; y: number; w: number; h: number;
  r?: number;
  disabled?: boolean;
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
const asBackend = (u: string) => (u.startsWith("/backend") ? u : `/backend${u}`);

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
    const msg = text || `${res.status} ${res.statusText}`;
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

  // hasVector / vectorSeatCount 추론
  const seats =
    row?.seats ??
    row?.vectorSeats ??
    row?.layout?.seats ??
    row?.vector?.seats ??
    [];
  const hasVector =
    !!row?.hasVector ||
    Array.isArray(seats) && seats.length > 0 ||
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
  /** 학원 전체 방 목록 (간단 DTO)
   *  - 우선순위대로 여러 엔드포인트를 시도
   *    1) /api/admin/rooms.vector-lite
   *    2) /api/admin/rooms/vector-lite
   *    3) /api/admin/rooms.vector
   *    4) /api/admin/rooms (구버전/그리드, teacher는 403일 수 있음)
   */
  async list(academyNumber: number): Promise<AdminRoomLite[]> {
    if (!academyNumber) throw new Error("academyNumber is required");

    const candidates = [
      `/api/admin/rooms.vector-lite?academyNumber=${academyNumber}`,
      `/api/admin/rooms/vector-lite?academyNumber=${academyNumber}`,
      `/api/admin/rooms.vector?academyNumber=${academyNumber}`,
      `/api/admin/rooms?academyNumber=${academyNumber}`, // fallback(구버전)
    ];

    let lastError: any = null;
    for (const path of candidates) {
      try {
        const url = asBackend(path);
        const json = await handle<any>(await fetch(url, {
          method: "GET",
          credentials: "include",
          headers: makeHeaders(),
        }));

        const arr: any[] = Array.isArray(json) ? json : (Array.isArray(json?.items) ? json.items : []);
        const out = arr
          .map(row => toLite(row, academyNumber))
          .filter((x): x is AdminRoomLite => !!x);

        if (out.length > 0 || path !== candidates[candidates.length - 1]) {
          // 결과가 있거나(성공) / 마지막 후보가 아니어도 성공으로 간주
          return out;
        }
        // 결과가 0인데 마지막 후보였다 → 그대로 반환(빈 배열)
        return out;
      } catch (e) {
        lastError = e;
        // 다음 후보 시도
      }
    }
    // 전부 실패한 경우
    throw lastError ?? new Error("Failed to load rooms");
  },

  /** 특정 방 벡터 레이아웃 조회 */
  async get(roomNumber: number, academyNumber: number): Promise<VectorLayout | null> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = asBackend(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    return handle(await fetch(url, {
      method: "GET",
      credentials: "include",
      headers: makeHeaders()
    }));
  },

  /** 저장(전체 교체) — 백엔드에서 path/query로 academy/room 주입 */
  async put(roomNumber: number, academyNumber: number, body: VectorLayout): Promise<void> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = asBackend(`/api/admin/rooms/${roomNumber}/vector-layout?academyNumber=${academyNumber}`);
    await handle(await fetch(url, {
      method: "PUT",
      credentials: "include",
      headers: makeHeaders(true),
      body: JSON.stringify(body),
    }));
  },

  /** 방 삭제 */
  async delete(roomNumber: number, academyNumber: number): Promise<void> {
    if (!academyNumber) throw new Error("academyNumber is required");
    const url = asBackend(`/api/admin/rooms/${roomNumber}?academyNumber=${academyNumber}`);
    await handle(await fetch(url, {
      method: "DELETE",
      credentials: "include",
      headers: makeHeaders()
    }));
  },
};
