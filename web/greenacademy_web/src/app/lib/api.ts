// src/app/lib/api.ts

/** ---------- 공통 타입 ---------- */
export type Role = "student" | "teacher" | "parent" | "director";

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

/** 좌석/강의실 (필요 시) */
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

/** 반(코스) 요약/수정 */
export interface CourseLite {
  classId: string;
  className: string;
  roomNumber?: number;
  students?: string[];
}
export interface CreateClassReq {
  className: string;
  teacherId: string;
  academyNumber: number;
  roomNumber?: number;
}
export interface PatchClassReq {
  className?: string;
  roomNumber?: number;
  academyNumber?: number;
}

/** 학생 검색 결과 */
export type StudentHit = {
  studentId: string;
  studentName?: string | null;
  grade?: number | null;
  academyNumber?: number | null;
};

/** 출결(대시보드에서 쓸 수 있도록 노출) */
export type StudentAttendanceRow = {
  classId: string;
  className: string;
  date: string;     // "yyyy-MM-dd" or ISO
  status: string;   // "PRESENT" | "LATE" | "ABSENT" | ...
};

/** ---------- 내부 유틸 ---------- */

// 프록시 접두사(Next.js rewrites 필요: /backend → http://localhost:9090)
const BACKEND_PREFIX = "/backend";

// 교사 관리 컨트롤러 베이스 경로
// 백엔드 @RequestMapping("/api/manage/teachers") 와 반드시 일치!
const TEACHER_PREFIX = "/api/manage/teachers";

/** FormData 여부 판별 */
function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

/** LocalStorage 에서 JWT 토큰 꺼내오기 (session 우선, legacy login 허용) */
function getTokenFromLocalStorage(): string | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem("session") ?? localStorage.getItem("login");
    if (!raw) return null;
    const parsed = JSON.parse(raw) as LoginResponse;
    return parsed?.token ?? null;
  } catch {
    return null;
  }
}

/** 공통 요청 래퍼: /backend 프록시 + Authorization 자동 주입 */
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as any) };

  if (init.body && !isFormData(init.body) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const token = getTokenFromLocalStorage();
  if (token && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  // 절대 URL(https://...) 이 아닌 이상 /backend 접두사 붙임
  const isAbsolute = /^https?:\/\//i.test(path);
  const url = isAbsolute
    ? path
    : path.startsWith("/backend")
    ? path
    : `${BACKEND_PREFIX}${path.startsWith("/") ? "" : "/"}${path}`;

  const res = await fetch(url, {
    credentials: "include",
    ...init,
    headers,
  });

  const text = await res.text();
  if (!res.ok) {
    // 백엔드 에러 메시지가 본문에 포함되어 있으면 함께 노출
    throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  }
  // 빈 본문 처리
  return text ? (JSON.parse(text) as T) : ({} as T);
}

/** 날짜 유틸 (원하면 가져다 쓰세요) */
export const todayISO = () => new Date().toISOString().slice(0, 10);

/** ---------- 실제 API ---------- */
export const api = {
  /* ====== 인증 ====== */
  login: (body: LoginRequest) =>
    request<LoginResponse>("/api/login", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  /* ====== 교사: 반 관리 ====== */
  listMyClasses: (teacherId: string) =>
    request<CourseLite[]>(`${TEACHER_PREFIX}/${encodeURIComponent(teacherId)}/classes`),

  createClass: (body: CreateClassReq) =>
    request<CourseLite>(`${TEACHER_PREFIX}/classes`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  getClassDetail: (classId: string) =>
    request<CourseLite>(`${TEACHER_PREFIX}/classes/${encodeURIComponent(classId)}`),

  patchClass: (classId: string, body: PatchClassReq) =>
    request<void>(`${TEACHER_PREFIX}/classes/${encodeURIComponent(classId)}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    }),

  addStudentToClass: (classId: string, studentId: string) =>
    request<void>(
      `${TEACHER_PREFIX}/classes/${encodeURIComponent(classId)}/students?studentId=${encodeURIComponent(
        studentId
      )}`,
      { method: "POST" }
    ),

  removeStudentFromClass: (classId: string, studentId: string) =>
    request<void>(
      `${TEACHER_PREFIX}/classes/${encodeURIComponent(classId)}/students/${encodeURIComponent(
        studentId
      )}`,
      { method: "DELETE" }
    ),

  searchStudents: (academyNumber: number, q: string, grade?: number) => {
    const params = new URLSearchParams({
      academyNumber: String(academyNumber),
      q: q ?? "",
    });
    if (typeof grade === "number") params.set("grade", String(grade));
    return request<StudentHit[]>(`${TEACHER_PREFIX}/students/search?${params.toString()}`);
  },

  /* ====== (선택) 좌석 보드/강의실 관리 필요 시 ====== */
  getSeatBoard: (classId: string, date: string) =>
    request<SeatBoardResponse>(
      `/api/teachers/classes/${encodeURIComponent(classId)}/seatboard?date=${encodeURIComponent(
        date
      )}`
    ),

  listRooms: (academyNumber: number) =>
    request<any[]>(`/api/admin/rooms?academyNumber=${academyNumber}`),

  getRoom: (academyNumber: number, roomNumber: number) =>
    request<any>(`/api/admin/rooms/${roomNumber}?academyNumber=${academyNumber}`),

  saveRoomLayout: (roomNumber: number, body: RoomLayoutBody) =>
    request<any>(`/api/admin/rooms/${roomNumber}/layout`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  /* ====== (선택) 출결 조회: 대시보드에서 쓰고 싶으면 ====== */
  studentAttendance: (studentId: string) =>
    request<StudentAttendanceRow[]>(`/api/students/${encodeURIComponent(studentId)}/attendance`),

  parentAttendance: (studentId: string) =>
    request<StudentAttendanceRow[]>(`/api/parents/${encodeURIComponent(studentId)}/attendance`),
};

export default api;
