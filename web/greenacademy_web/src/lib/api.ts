// src/lib/api.ts

// ---------- 공통 타입 ----------
export type Role = "student" | "teacher" | "parent" | "director";

export interface LoginRequest {
  username: string;
  password: string;
  fcmToken?: string; // 웹은 보통 안 보냄
}

export interface LoginResponse {
  status: "success";
  role: Role;
  username: string;
  name: string | null;
  token: string | null;
  phone?: string | null;
  address?: string | null;   // 학생만
  school?: string | null;    // 학생만
  grade?: number;            // 학생만
  gender?: string | null;    // 학생만
  academyNumbers?: number[]; // 모든 역할 가능 (List<Integer>)
  parentsNumber?: number;    // 학부모만 (setter로 추가됨)
  childStudentId?: string;   // 학부모만 (첫 자녀)
}

// ---------- 환경설정 ----------
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:9090";

// ---------- 공통 유틸 ----------
function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as any) };

  if (init.body && !isFormData(init.body) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const res = await fetch(url, { credentials: "include", ...init, headers });
  const text = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  return text ? (JSON.parse(text) as T) : ({} as T);
}

function normalizePhone(phone: string) {
  return phone.replace(/[^0-9]/g, "");
}

// ---------- 아이디 찾기 ----------
type IdFindableRole = Extract<Role, "student" | "parent" | "teacher" | "director">;

export interface FindIdRequest {
  role: IdFindableRole;
  name: string;
  phoneNumber: string;
}
export interface FindIdResponse {
  username: string;
}

// ---------- 비밀번호 재설정 ----------
export interface ResetPasswordRequest {
  role: Role;
  username: string;     // 로그인 ID (Student_ID / Parents_ID / Teacher_ID / Director.username)
  name: string;         // 본인 확인용
  phoneNumber: string;  // 본인 확인용
  newPassword: string;  // 새 비밀번호
}

export interface ResetPasswordResponse {
  status: "ok";
}

export const api = {
  // 로그인
  login: (body: LoginRequest) =>
    request<LoginResponse>("/api/login", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  // 아이디 찾기
  findId: ({ role, name, phoneNumber }: FindIdRequest) => {
    const path = `/api/${role}s/find_id`;
    return request<FindIdResponse>(path, {
      method: "POST",
      body: JSON.stringify({ name, phoneNumber: normalizePhone(phoneNumber) }),
    });
  },

  // 비밀번호 재설정 (단일 엔드포인트 사용)
  resetPassword: (body: ResetPasswordRequest) =>
    request<ResetPasswordResponse>("/api/reset-password", {
      method: "POST",
      body: JSON.stringify({
        ...body,
        phoneNumber: normalizePhone(body.phoneNumber),
      }),
    }),
};
