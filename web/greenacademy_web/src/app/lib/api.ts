import { getSession } from "@/app/lib/session";

export type Role = "student" | "teacher" | "parent" | "director";

/* ---- 로그인 ---- */
export interface LoginRequest {
  username: string;
  password: string;
  fcmToken?: string;
}
export interface LoginResponse {
  status: "success";
  role: Role;
  username: string;
  name: string | null;
  token: string | null;
  phone?: string | null;
  address?: string | null;
  school?: string | null;
  grade?: number;
  gender?: string | null;
  academyNumbers?: number[];
  parentsNumber?: number;
  childStudentId?: string;
}

/* ---- 좌석/강의실 ---- */
export type SeatStatus = {
  seatNumber: number;
  row?: number;
  col?: number;
  disabled?: boolean;
  studentId?: string;
  attendanceStatus?: string;
};
export type SeatBoardResponse = {
  currentClass?: { classId?: string; className?: string };
  rows: number;
  cols: number;
  seats: SeatStatus[];
};
export type SeatCell = { seatNumber: number; row: number; col: number; disabled?: boolean };
export interface RoomLayoutBody {
  academyNumber: number;
  rows: number;
  cols: number;
  layout: SeatCell[];
}

/* ---- 반 요약/상세 ---- */
export interface CourseLite {
  classId: string;
  className: string;
  roomNumber?: number;
  /** ✅ 여러 강의실 지원 (선택) */
  roomNumbers?: number[];
  students?: string[];
}
export interface CourseDetail extends CourseLite {
  startTime?: string | null;
  endTime?: string | null;
  daysOfWeek?: (number | string)[] | null; // 1~7
  schedule?: string | null;
  extraDates?: string[];      // "YYYY-MM-DD"
  cancelledDates?: string[];  // "YYYY-MM-DD"
}

/* ---- 생성/수정 바디 ---- */
export interface CreateClassReq {
  className: string;
  teacherId: string;
  academyNumber: number;
  /** ✅ 기존 단일 방 (호환) */
  roomNumber?: number;
  /** ✅ 다중 방(선택) — 백엔드가 받으면 저장, 아니면 무시됨 */
  roomNumbers?: number[];

  startTime?: string | null; // "HH:mm"
  endTime?: string | null;   // "HH:mm"
  daysOfWeek?: (number | string)[] | null; // 1~7
  schedule?: string | null;
}
export interface PatchClassReq {
  className?: string;
  /** ✅ 단일 방 (호환) */
  roomNumber?: number;
  /** ✅ 다중 방(선택) */
  roomNumbers?: number[];

  academyNumber?: number;
  startTime?: string | null;
  endTime?: string | null;
  daysOfWeek?: (number | string)[] | null;
  schedule?: string | null;
  extraDates?: string[];
  cancelledDates?: string[];
}

/* ---- 스케줄(시간표) ---- */
export interface ScheduleItem {
  scheduleId: string;
  teacherId: string;
  date: string; // "YYYY-MM-DD"
  classId: string;
  title?: string;
  startTime: string; // "HH:mm"
  endTime: string;   // "HH:mm"
  roomNumber?: number;
  memo?: string;
}
export interface CreateScheduleReq {
  date: string;
  classId: string;
  title?: string;
  startTime: string;
  endTime: string;
  roomNumber?: number;
  memo?: string;
}
export interface UpdateScheduleReq {
  date?: string;
  classId?: string;
  title?: string;
  startTime?: string;
  endTime?: string;
  roomNumber?: number;
  memo?: string;
}

/* ---- 검색/출결 ---- */
export type StudentHit = {
  studentId: string;
  studentName?: string | null;
  grade?: number | null;
  academyNumber?: number | null;
};
export type StudentAttendanceRow = {
  classId: string;
  className: string;
  date: string;
  status: string;
};

/* ---- 공통 오류 ---- */
class ApiError extends Error {
  status: number;
  body?: any;
  constructor(status: number, message: string, body?: any) {
    super(message);
    this.status = status;
    this.body = body;
  }
}
export { ApiError };

/* =============================================================================
 * 내부 유틸
 * ========================================================================== */

/** 백엔드 베이스 URL: .env에 NEXT_PUBLIC_API_BASE 없으면 localhost:9090 */
const BASE_URL =
  (typeof window !== "undefined" && (window as any)?.__API_BASE__) ||
  process.env.NEXT_PUBLIC_API_BASE ||
  "http://localhost:9090";

/** 교사 API 공통 프리픽스 */
const TEACHER_PREFIX = "/api/manage/teachers";

function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

function getTokenFromLocalStorage(): string | null {
  if (typeof window === "undefined") return null;
  try {
    const raw =
      localStorage.getItem("session") ??
      localStorage.getItem("login") ??
      localStorage.getItem("auth") ??
      null;
    if (!raw) return null;

    let token: string | null = null;
    try {
      const parsed = JSON.parse(raw) as Partial<LoginResponse> | { token?: string } | undefined;
      token = (parsed?.token as string | undefined) ?? null;
    } catch {
      token = raw;
    }
    if (!token) return null;

    let s = String(token).trim();
    if (!s || s.toLowerCase() === "null" || s.toLowerCase() === "undefined") return null;
    if (s.toLowerCase().startsWith("bearer ")) s = s.slice(7).trim();

    // (선택) JWT exp 검사
    try {
      const [, payloadB64] = s.split(".");
      if (payloadB64) {
        const payload = JSON.parse(
          atob(payloadB64.replace(/-/g, "+").replace(/_/g, "/"))
        ) as { exp?: number };
        if (payload?.exp && Date.now() / 1000 >= payload.exp) return null;
      }
    } catch { /* ignore */ }
    return s;
  } catch {
    return null;
  }
}

function resolveUrl(path: string): string {
  const isAbsolute = /^https?:\/\//i.test(path);
  if (isAbsolute) return path;
  return `${BASE_URL}${path.startsWith("/") ? "" : "/"}${path}`;
}

function getAuthToken(): string | null {
  try {
    const s = getSession();
    let t = s?.token ?? null;
    if (t && typeof t === "string") {
      t = t.trim();
      if (!t || t.toLowerCase() === "null" || t.toLowerCase() === "undefined") t = null;
      if (t && t.toLowerCase().startsWith("bearer ")) t = t.slice(7).trim();
      if (t) return t;
    }
  } catch { /* ignore */ }

  // fallback: localStorage
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem("session") ??
                localStorage.getItem("login") ??
                localStorage.getItem("auth") ?? null;
    if (!raw) return null;
    let s = raw;
    try {
      const parsed = JSON.parse(raw) as { token?: string };
      s = (parsed?.token ?? raw) as string;
    } catch { /* raw is token */ }
    s = String(s).trim();
    if (!s || s.toLowerCase() === "null" || s.toLowerCase() === "undefined") return null;
    if (s.toLowerCase().startsWith("bearer ")) s = s.slice(7).trim();
    return s;
  } catch { return null; }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as any) };

  if (init.body && !(init.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const token = getAuthToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const url = resolveUrl(path);
  console.log("[API] fetch", url, init?.method ?? "GET");
  const res = await fetch(url, { credentials: "include", ...init, headers });
  const text = await res.text();
  console.log("[API] response", res.status, res.statusText, url);

  if (!res.ok) {
    let body: any = undefined;
    try { body = text ? JSON.parse(text) : undefined; } catch {}
    throw new ApiError(res.status, body?.message || `${res.status} ${res.statusText}`, body);
  }
  return text ? (JSON.parse(text) as T) : ({} as T);
}

export const todayISO = () => new Date().toISOString().slice(0, 10);

/* =============================================================================
 * 실제 API
 * ========================================================================== */
export const api = {
  /* 로그인 */
  login: (body: LoginRequest) =>
    request<LoginResponse>("/api/login", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  /* ✅ 아이디 찾기 (find_id 페이지에서 사용) */
  findId: (body: { role: Role; name: string; phoneNumber: string }) =>
    request<{ username: string }>("/api/find-id", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  /* 반 관리 */
  listMyClasses: (teacherId: string) =>
    request<CourseLite[]>(
      `${TEACHER_PREFIX}/${encodeURIComponent(teacherId)}/classes`
    ),

  createClass: (body: CreateClassReq) =>
    request<CourseLite>(`${TEACHER_PREFIX}/classes`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  getClassDetail: (classId: string) =>
    request<CourseDetail>(
      `${TEACHER_PREFIX}/classes/${encodeURIComponent(classId)}`
    ),

  patchClass: (classId: string, body: PatchClassReq) =>
    request<void>(
      `${TEACHER_PREFIX}/classes/${encodeURIComponent(classId)}`,
      { method: "PATCH", body: JSON.stringify(body) }
    ),

  addStudentToClass: (classId: string, studentId: string) =>
    request<void>(
      `${TEACHER_PREFIX}/classes/${encodeURIComponent(
        classId
      )}/students?studentId=${encodeURIComponent(studentId)}`,
      { method: "POST" }
    ),

  removeStudentFromClass: (classId: string, studentId: string) =>
    request<void>(
      `${TEACHER_PREFIX}/classes/${encodeURIComponent(
        classId
      )}/students/${encodeURIComponent(studentId)}`,
      { method: "DELETE" }
    ),

  searchStudents: (academyNumber: number, q: string, grade?: number) => {
    const params = new URLSearchParams({
      academyNumber: String(academyNumber),
      q: q ?? "",
    });
    if (typeof grade === "number") params.set("grade", String(grade));
    return request<StudentHit[]>(
      `${TEACHER_PREFIX}/students/search?${params.toString()}`
    );
  },

  /* ---- 스케줄 API ---- */
  getDaySchedules: (teacherId: string, date: string) =>
    request<ScheduleItem[]>(
      `${TEACHER_PREFIX}/${encodeURIComponent(
        teacherId
      )}/schedules/day?date=${encodeURIComponent(date)}`
    ),

  listSchedules: (teacherId: string, from: string, to: string) =>
    request<ScheduleItem[]>(
      `${TEACHER_PREFIX}/${encodeURIComponent(
        teacherId
      )}/schedules?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    ),

  createSchedule: (teacherId: string, body: CreateScheduleReq) =>
    request<ScheduleItem>(
      `${TEACHER_PREFIX}/${encodeURIComponent(teacherId)}/schedules`,
      { method: "POST", body: JSON.stringify(body) }
    ),

  updateSchedule: (teacherId: string, scheduleId: string, body: UpdateScheduleReq) =>
    request<void>(
      `${TEACHER_PREFIX}/${encodeURIComponent(
        teacherId
      )}/schedules/${encodeURIComponent(scheduleId)}`,
      { method: "PATCH", body: JSON.stringify(body) }
    ),

  deleteSchedule: (teacherId: string, scheduleId: string) =>
    request<void>(
      `${TEACHER_PREFIX}/${encodeURIComponent(
        teacherId
      )}/schedules/${encodeURIComponent(scheduleId)}`
    ),

  deleteScheduleByClassDate: (teacherId: string, classId: string, date: string) =>
    request<void>(
      `${TEACHER_PREFIX}/${encodeURIComponent(teacherId)}/schedules/${encodeURIComponent(`${classId}_${date}`)}`,
    ),

  /* ---- 좌석/강의실 ---- */
  getSeatBoard: (classId: string, date: string) =>
    request<SeatBoardResponse>(
      `${TEACHER_PREFIX}/classes/${encodeURIComponent(
        classId
      )}/seatboard?date=${encodeURIComponent(date)}`
    ),

  // (관리용) 방 목록/상세/레이아웃 저장
  listRooms: (academyNumber: number) =>
    request<any[]>(`/api/admin/rooms?academyNumber=${academyNumber}`),

  getRoom: (academyNumber: number, roomNumber: number) =>
    request<any>(`/api/admin/rooms/${roomNumber}?academyNumber=${academyNumber}`),

  saveRoomLayout: (roomNumber: number, body: RoomLayoutBody) =>
    request<any>(`/api/admin/rooms/${roomNumber}/layout`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
};

export default api;
