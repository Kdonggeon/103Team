// src/app/lib/teachermainApi.ts
import { getSession } from "@/app/lib/session";
import { roomsVectorApi } from "@/app/lib/rooms.vector";

/* ===== API URL 구성 ===== */
const API_BASE =
  (typeof window !== "undefined" && (window as any).__API_BASE__) ||
  process.env.NEXT_PUBLIC_API_BASE ||
  "";

const BACKEND_PREFIX = process.env.NEXT_PUBLIC_BACKEND_PREFIX ?? "/backend";

function abs(path: string) {
  const p = path.startsWith("/") ? path : `/${path}`;
  const withPrefix = `${BACKEND_PREFIX}${p}`.replace(/\/{2,}/g, "/");
  return API_BASE ? `${API_BASE}${withPrefix}` : withPrefix;
}

/* ===== Types ===== */
export type TeacherClassLite = {
  classId: string;
  className: string;
  roomNumber?: number | null;
  roomNumbers?: number[] | null;
  startTime?: string | null;
  endTime?: string | null;
  schedule?: Array<{
    dow?: 1 | 2 | 3 | 4 | 5 | 6 | 7;
    startTime?: string;
    endTime?: string;
    date?: string;
  }>;
};

export type SeatBoardSeat = {
  // grid형
  seatNumber?: number | null;
  row?: number | null;
  col?: number | null;

  // vector형(0~1 정규화)
  x?: number; y?: number; w?: number; h?: number;

  disabled?: boolean | null;
  studentId?: string | null;
  studentName?: string | null;
  attendanceStatus?: string | null;
  occupiedAt?: string | null;

  // 선택: 라벨(디버깅/표시용)
  label?: string | null;
};

export type SeatBoardResponse = {
  date: string;
  layoutType?: "grid" | "vector";
  rows?: number;
  cols?: number;
  roomNumber?: number | null;
  currentClass?: { classId: string; className?: string };
  seats: SeatBoardSeat[];
  // (선택) 카운터
  presentCount?: number;
  lateCount?: number;
  absentCount?: number;
  moveOrBreakCount?: number;
  notRecordedCount?: number;
  waiting?: Array<{ studentId: string; studentName?: string | null; status?: string | null; checkedInAt?: string | null }>;
};

function friendlyHttpMessage(status: number, body?: any): string {
  const serverMsg: string | undefined =
    (body?.message as string | undefined) ??
    (body?.error as string | undefined) ??
    (body?.msg as string | undefined);

  switch (status) {
    case 400:
      return serverMsg || "요청을 처리할 수 없습니다. 입력 값을 확인해 주세요.";
    case 401:
      return "로그인이 필요합니다. 다시 로그인해 주세요.";
    case 403:
      return "접근 권한이 없습니다.";
    case 404:
      return serverMsg || "요청한 데이터를 찾을 수 없습니다.";
    case 409:
      return serverMsg || "이미 존재하거나 충돌이 발생했습니다.";
    case 500:
      return serverMsg || "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    default:
      return serverMsg || `${status} 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.`;
  }
}

/* ===== 날짜 유틸 ===== */
const KST = "Asia/Seoul";
export const todayYmd = () => {
  const now = new Date();
  return new Intl.DateTimeFormat("sv-SE", {
    timeZone: KST, year: "numeric", month: "2-digit", day: "2-digit",
  }).format(now); // YYYY-MM-DD
};

const toIsoDow = (d: Date) => (d.getDay() === 0 ? 7 : d.getDay()) as 1|2|3|4|5|6|7;

/* ===== Low-level fetch ===== */
async function _request<T>(url: string, init?: RequestInit): Promise<T> {
  const s = getSession();
  const headers: HeadersInit = {
    ...(init?.headers ?? {}),
    "Content-Type": "application/json",
    ...(s?.token ? { Authorization: `Bearer ${s.token}` } : {}),
  };

  const res = await fetch(abs(url), {
    ...init,
    headers,
    credentials: "include",
    cache: "no-store",
  });

  if (!res.ok) {
    let body: any = undefined;
    try { body = await res.json(); } catch {}
    throw new Error(friendlyHttpMessage(res.status, body));
  }

  const text = await res.text();
  return text ? (JSON.parse(text) as T) : ({} as T);
}

/* ===== Public ===== */

/** 오늘 기준 교사의 수업 */
export async function fetchTodayClasses(teacherId: string, date = todayYmd()): Promise<TeacherClassLite[]> {
  try {
    return await _request<TeacherClassLite[]>(
      `/api/teachermain/teachers/${encodeURIComponent(teacherId)}/classes/today?date=${encodeURIComponent(date)}`
    );
  } catch {
    const all = await _request<TeacherClassLite[]>(
      `/api/manage/teachers/${encodeURIComponent(teacherId)}/classes`
    );
    return filterClassesForDateClient(all, date);
  }
}

/**
 * 좌석판 조회
 * 1) 백엔드 seat-board API 시도
 * 2) 실패하면: classDetail/스케줄에서 오늘 roomNumber/academyNumber 구해
 *    rooms.vector 의 vectorLayoutV2 → SeatBoardResponse 로 변환
 */
export async function fetchSeatBoard(classId: string, date = todayYmd()): Promise<SeatBoardResponse> {
  // 1) 서버가 제공하면 그대로 사용
  try {
    return await _request<SeatBoardResponse>(
      `/api/teachermain/seat-board/${encodeURIComponent(classId)}?date=${encodeURIComponent(date)}`
    );
  } catch {
    // 2) 프론트 폴백: vectorLayoutV2 직접 읽기
    // (A) 수업 상세 → academyNumber / 오늘 roomNumber
    let detail: any = null;
    try { detail = await _request(`/api/manage/classes/${encodeURIComponent(classId)}`); } catch {}

    let academyNumber: number | null =
      detail?.academyNumber ?? detail?.Academy_Number ?? null;
    academyNumber = academyNumber != null ? Number(academyNumber) : null;

    let roomNumber: number | null =
      detail?.todayRoomNumber ?? detail?.roomNumber ?? null;
    roomNumber = roomNumber != null ? Number(roomNumber) : null;

    // (B) 여전히 없으면 오늘 스케줄에서 보정
    if (!roomNumber || !academyNumber) {
      try {
        const teacherId =
          detail?.teacherId ?? detail?.Teacher_ID ?? detail?.ownerTeacherId ?? "";
        const rows: any[] = await _request(
          `/api/manage/teachers/${encodeURIComponent(teacherId)}/schedules?from=${date}&to=${date}`
        );
        const one = (rows ?? []).find(r => String(r.classId ?? "") === classId);
        if (one?.roomNumber != null) roomNumber = Number(one.roomNumber);
        if (academyNumber == null && (one as any)?.academyNumber != null)
          academyNumber = Number((one as any).academyNumber);
      } catch {}
    }

    if (!roomNumber || !academyNumber) {
      return { date, layoutType: "vector", seats: [], roomNumber: roomNumber ?? null };
    }

    // (C) rooms.vector에서 벡터 좌석 읽기
    const roomDoc: any = await roomsVectorApi.get(roomNumber, academyNumber);
    const v2: any[] = Array.isArray(roomDoc?.vectorLayoutV2) ? roomDoc.vectorLayoutV2 : [];

    if (v2.length > 0) {
      const seats: SeatBoardSeat[] = v2.map((s: any) => ({
        x: Number(s.x ?? 0), y: Number(s.y ?? 0), w: Number(s.w ?? 0), h: Number(s.h ?? 0),
        label: String(s.label ?? s.seatNumber ?? ""),
        seatNumber:
          s.seatNumber != null ? Number(s.seatNumber)
          : isFinite(Number(s.label)) ? Number(s.label) : undefined,
        disabled: !!s.disabled,
        studentId: s.Student_ID ?? s.studentId ?? null,
        attendanceStatus: s.attendanceStatus ?? null,
      }));

      return {
        date,
        layoutType: "vector",
        roomNumber,
        currentClass: { classId, className: detail?.className ?? classId },
        seats,
      };
    }

    // (D) 예전 rows/cols/grids 지원 (없으면 빈 보드)
    const rows = Number(roomDoc?.rows ?? 0);
    const cols = Number(roomDoc?.cols ?? 0);
    const legacySeats: any[] = Array.isArray(roomDoc?.seats) ? roomDoc.seats : [];

    if (rows > 0 && cols > 0 && legacySeats.length > 0) {
      const seats: SeatBoardSeat[] = legacySeats.map((s: any) => ({
        row: Number(s.row ?? 1),
        col: Number(s.col ?? 1),
        seatNumber: Number(s.seatNumber ?? s.label ?? 0),
        disabled: !!s.disabled,
        studentId: s.studentId ?? null,
        attendanceStatus: s.attendanceStatus ?? null,
      }));
      return { date, layoutType: "grid", roomNumber, rows, cols, currentClass: { classId, className: detail?.className ?? classId }, seats };
    }

    return { date, layoutType: "vector", roomNumber, currentClass: { classId }, seats: [] };
  }
}

/* ===== Client-side filter(for fallback) ===== */
function filterClassesForDateClient(list: TeacherClassLite[], date: string): TeacherClassLite[] {
  if (!Array.isArray(list) || list.length === 0) return [];
  const d = new Date(date.replace(/-/g, "/") + " 00:00:00");
  const dow = toIsoDow(d);

  const specific = list.filter(c => (c.schedule ?? []).some(s => s.date === date));
  if (specific.length > 0) return specific;

  const weekly = list.filter(c => (c.schedule ?? []).some(s => (s.dow as number) === dow));
  if (weekly.length > 0) return weekly;

  return list.filter(c => c.startTime || c.endTime);
}
