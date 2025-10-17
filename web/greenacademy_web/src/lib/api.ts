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

// ---------- (export) 저장된 JWT 읽기 ----------
export function getSavedToken(): string | null {
  try {
    if (typeof window === "undefined") return null; // SSR 보호
    const raw = localStorage.getItem("login");
    if (!raw) return null;
    const obj = JSON.parse(raw);
    const t = obj?.token;
    return typeof t === "string" && t.length > 0 ? t : null;
  } catch {
    return null;
  }
}

// ---------- (export) 세션 유틸: 읽기/저장/삭제 ----------
export function getSavedSession():
  | { token?: string; role?: string; username?: string; name?: string | null; academyNumbers?: number[] }
  | null {
  try {
    if (typeof window === "undefined") return null; // SSR 보호
    const raw = localStorage.getItem("login");
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function saveSession(data: LoginResponse, extra?: Record<string, unknown>) {
  const payload = {
    token: data.token,
    role: data.role,
    username: data.username,
    name: data.name ?? null,
    academyNumbers: Array.isArray(data.academyNumbers) ? data.academyNumbers : [],
    ...(extra ?? {}),
  };
  localStorage.setItem("login", JSON.stringify(payload));
}

export function clearSession() {
  localStorage.removeItem("login");
}

// ---------- 공통 유틸 ----------
function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}


export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as any) };

  // Authorization 자동 주입
  if (!headers["Authorization"]) {
    const token = getSavedToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  // JSON 본문이면 Content-Type 지정
  if (init.body && !isFormData(init.body) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const res = await fetch(url, { credentials: "include", ...init, headers });
  const text = await res.text();

  if (!res.ok) {
    if (res.status === 401 || res.status === 403) {
      throw new Error(`AUTH_${res.status}: ${text || "인증이 필요합니다. 다시 로그인 해주세요."}`);
    }
    throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  }

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
