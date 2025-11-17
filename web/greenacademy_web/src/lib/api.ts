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

// ---------- 환경설정 ----------
// NoticePanel 과 동일한 기본값으로 통일
// - 클라이언트: /backend → next.config.mjs에서 백엔드로 프록시
// - 서버(SSR): localhost:9090 로 직접 호출
const RAW_BASE = process.env.NEXT_PUBLIC_API_BASE;
const API_BASE =
  RAW_BASE ??
  (typeof window === "undefined" ? "http://localhost:9090" : "/backend");

// ---------- 세션/토큰 유틸 ----------
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
function resolveUrl(path: string): string {
  // 절대 URL이면 그대로 사용
  if (/^https?:\/\//i.test(path)) return path;

  // 이미 /backend/* 로 시작하면 그대로 사용 (rewrite 대상)
  if (path.startsWith("/backend/")) {
    return path;
  }

  // API_BASE가 지정된 경우 (env 또는 기본값)
  if (API_BASE) {
    const base = API_BASE.replace(/\/+$/, ""); // 끝 슬래시 제거
    const p = path.startsWith("/") ? path : `/${path}`;
    return `${base}${p}`;
  }

  // 이 지점까지 올 일은 거의 없음 (fallback)
  return path;
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

  // 2) URL 조립
  const url = resolveUrl(path);

  // 3) 최종 init
  const finalInit: RequestInit = {
    ...init,
    headers,
    credentials: init.credentials ?? "include",
    cache: init.cache ?? "no-store",
  };

  // 4) 요청
  let res: Response;
  try {
    res = await fetch(url, finalInit);
  } catch (e: any) {
    // 네트워크 오류
    const msg = typeof e?.message === "string" ? e.message : "네트워크 오류가 발생했습니다.";
    throw new Error(`NETWORK_ERROR: ${msg}`);
  }

  // 5) 응답 처리
  const status = res.status;

  // 204/205 or 빈 본문
  if (status === 204 || status === 205) {
    return {} as T;
  }

  const rawText = await res.text();

  if (!res.ok) {
    if (status === 401 || status === 403) {
      // 라우팅단에서 이 메시지를 감지해 로그인 페이지로 보낼 수 있도록 AUTH_* 형태로 throw
      throw new Error(`AUTH_${status}: ${rawText || "인증이 필요합니다. 다시 로그인 해주세요."}`);
    }
    // 서버 메시지 보존
    throw new Error(`${status} ${res.statusText}${rawText ? " | " + rawText : ""}`);
  }

  if (!rawText) return {} as T;

  // Content-Type 확인 후 JSON 파싱 시도 (방어적)
  const ct = res.headers.get("Content-Type") || "";
  if (ct.toLowerCase().includes("application/json")) {
    try {
      return JSON.parse(rawText) as T;
    } catch {
      // JSON 표시인데 파싱 실패 시에도 안전하게 텍스트 반환
      return (rawText as unknown) as T;
    }
  }

  // JSON 이외 컨텐츠는 원문 텍스트 반환(필요 시 호출부에서 처리)
  return rawText as unknown as T;
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
