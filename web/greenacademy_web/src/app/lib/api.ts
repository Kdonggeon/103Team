export type Role = "student" | "teacher" | "parent" | "director";

export interface LoginRequest { username: string; password: string; fcmToken?: string; }
export interface LoginResponse {
  status: "success"; role: Role; username: string; name: string | null; token: string | null;
  phone?: string | null; address?: string | null; school?: string | null;
  grade?: number; gender?: string | null; academyNumbers?: number[];
  parentsNumber?: number; childStudentId?: string;
}

function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as any) };
  if (init.body && !isFormData(init.body) && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const res = await fetch(path, { credentials: "include", ...init, headers });
  const text = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  return text ? (JSON.parse(text) as T) : ({} as T);
}

export const api = {
  login: (body: LoginRequest) => request<LoginResponse>("/api/login", { method: "POST", body: JSON.stringify(body) }),
};
