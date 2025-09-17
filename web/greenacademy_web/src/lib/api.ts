// src/lib/api.ts (업데이트 버전)

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

function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as any) };
  if (init.body && !isFormData(init.body) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  const res = await fetch(path, { credentials: "include", ...init, headers });
  const text = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  return text ? (JSON.parse(text) as T) : ({} as T);
}

export const api = {
  login: (body: LoginRequest) =>
    request<LoginResponse>("/api/login", { method: "POST", body: JSON.stringify(body) }),
};
