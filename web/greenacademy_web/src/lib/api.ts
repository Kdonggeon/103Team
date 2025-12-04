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
  parentsNumber?: number;    // 학부모만
  childStudentId?: string;   // 학부모만 (첫 자녀)
}

const RAW_BASE = process.env.NEXT_PUBLIC_API_BASE;
const PRIMARY_BASE = (RAW_BASE || "").trim() || "/backend";
const LOCAL_BASE = "http://localhost:9090";

// ---------- ??/?? ?? ----------
type SavedSession = {
  token?: string | null;
  role?: Role | string;
  username?: string;
  name?: string | null;
  academyNumbers?: number[];
  parentsNumber?: number;
  childStudentId?: string;
};

const STORAGE_KEY = "login";

export function getSavedToken(): string | null {
  try {
    if (typeof window === "undefined") return null; // SSR 보호
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const obj = JSON.parse(raw) as SavedSession;
    const t = obj?.token;
    return typeof t === "string" && t.length > 0 ? t : null;
  } catch {
    return null;
  }
}

export function getSavedSession(): SavedSession | null {
  try {
    if (typeof window === "undefined") return null; // SSR 보호
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as SavedSession;
  } catch {
    return null;
  }
}

export function saveSession(data: LoginResponse, extra?: Record<string, unknown>) {
  const payload: SavedSession = {
    token: data.token,
    role: data.role,
    username: data.username,
    name: data.name ?? null,
    academyNumbers: Array.isArray(data.academyNumbers) ? data.academyNumbers : [],
    parentsNumber: typeof data.parentsNumber === "number" ? data.parentsNumber : undefined,
    childStudentId: data.childStudentId ?? undefined,
    ...(extra ?? {}),
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

export function updateSession(patch: Partial<SavedSession>) {
  const cur = getSavedSession() ?? {};
  const next = { ...cur, ...patch };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY);
}

// ---------- 공통 유틸 ----------
function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

// ✅ NoticePanel / next.config.mjs와 맞춘 URL 조립 함수
function resolveUrl(path: string, base: string): string {
  // 절대 URL이면 그대로 사용
  if (/^https?:\/\//i.test(path)) return path;

  // 이미 /backend/* 로 시작하면 그대로 사용 (rewrite 대상)
  if (path.startsWith("/backend/")) {
    return path;
  }

  const b = base.replace(/\/+$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${b}${p}`;
}

/**
 * 공용 fetch 래퍼
 * - 외부(init)에서 준 옵션/헤더를 **보존**
 * - Authorization/Accept/Content-Type은 **없는 경우에만** 채움
 * - 기본값: credentials=include, cache=no-store
 * - 401/403은 AUTH_코드로 throw (라우팅단에서 로그인 유도)
 */
export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  // 1) 헤더 병합 (Headers로 대소문자 안전)
  const headers = new Headers(init.headers || {});

  // Accept 기본값
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json, text/plain;q=0.9, */*;q=0.8");
  }

  // Authorization 자동 주입 (없을 때만)
  if (!headers.has("Authorization")) {
    const token = getSavedToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  // JSON 본문이면 Content-Type 지정 (FormData 제외, 이미 있으면 유지)
  if (init.body && !isFormData(init.body) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const bases = Array.from(new Set([PRIMARY_BASE, LOCAL_BASE]));

  let lastErr: unknown;
  for (const base of bases) {
    const url = resolveUrl(path, base);

    // 3) 최종 init
    const finalInit: RequestInit = {
      ...init,
      headers,
      credentials: init.credentials ?? "include",
      cache: init.cache ?? "no-store",
    };

    try {
      const res = await fetch(url, finalInit);
      const status = res.status;

      // 204/205 or 빈 본문
      if (status === 204 || status === 205) {
        return {} as T;
      }

      const rawText = await res.text();

      if (!res.ok) {
        if (status === 401 || status === 403) {
          throw new Error(`AUTH_${status}: ${rawText || "인증이 필요합니다. 다시 로그인 해주세요."}`);
        }
        throw new Error(`${status} ${res.statusText}${rawText ? " | " + rawText : ""}`);
      }

      if (!rawText) return {} as T;

      const ct = res.headers.get("Content-Type") || "";
      if (ct.toLowerCase().includes("application/json")) {
        try {
          return JSON.parse(rawText) as T;
        } catch {
          return (rawText as unknown) as T;
        }
      }
      return rawText as unknown as T;
    } catch (e) {
      lastErr = e;
      // 다음 base로 fallback
    }
  }

  if (lastErr instanceof Error) throw lastErr;
  throw new Error("요청에 실패했습니다.");
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

// ---------- API 컬렉션 ----------
export const api = {
  // 로그인
  login: (body: LoginRequest) =>
    request<LoginResponse>("/backend/api/login", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  // 아이디 찾기
  findId: ({ role, name, phoneNumber }: FindIdRequest) => {
    const path = `/backend/api/${role}s/find_id`;
    return request<FindIdResponse>(path, {
      method: "POST",
      body: JSON.stringify({ name, phoneNumber: normalizePhone(phoneNumber) }),
    });
  },

  // 비밀번호 재설정 (단일 엔드포인트 사용)
  resetPassword: (body: ResetPasswordRequest) =>
    request<ResetPasswordResponse>("/backend/api/reset-password", {
      method: "POST",
      body: JSON.stringify({
        ...body,
        phoneNumber: normalizePhone(body.phoneNumber),
      }),
    }),
};
