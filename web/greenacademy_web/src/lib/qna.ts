// /src/lib/qna.ts
import { request, getSavedToken, getSavedSession } from "@/lib/api";

export type QnaId = { id?: string; _id?: string };

export type QnaQuestion = QnaId & {
  unreadCount?: number;
  content?: string;
  title?: string;
  authorRole?: string;
  createdAt?: string;
  followups?: any[]; // 백엔드가 요약/ID만 넣어주는 경우 있음
  // 선택: 백엔드가 내려줄 수 있는 필드들(있으면 UI에 도움)
  academyNumber?: number;
  room?: boolean;
  teacherNames?: string[];
  /** 현재 사용자 기준 마지막 읽음 시각(백엔드가 주면 우선, 없으면 프런트 로컬 보강) */
  lastReadAt?: string;
  lastStudentMsgAt?: string;
  lastParentMsgAt?: string;
  lastFollowupAt?: string;

  student?: { id?: string; _id?: string; name?: string; studentName?: string; displayName?: string };
  parent?: { id?: string; _id?: string; name?: string; parentName?: string; childStudentName?: string; displayName?: string };
};

export type QnaAnswer = QnaId & {
  content?: string;
  authorRole?: string; // "teacher" | "director" 등
  teacherName?: string;
  createdAt?: string;
};

// ======================================================
// 🔸 Authorization/credentials/cache를 확실히 주입하는 래퍼
// ======================================================
async function requestAuthLocal<T = any>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getSavedToken?.();

  // 기존 헤더 + Authorization 안전 머지
  const headers = new Headers(init.headers || {});
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (!headers.has("Content-Type") && init.body && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  // 기본 옵션 주입
  const nextInit: RequestInit = {
    ...init,
    headers,
    credentials: init.credentials ?? "include",
    cache: init.cache ?? "no-store",
  };

  return request<T>(path, nextInit);
}

// ======================================================
/* 🔸 프런트 로컬 lastReadAt 보강 유틸 (사용자/문서별 키) */
// ======================================================
function lrKey(questionId: string): string {
  try {
    const session = getSavedSession?.();
    const user = (session?.username || "anon").trim();
    return `qna:lastRead:${user}:${questionId}`;
  } catch {
    return `qna:lastRead:anon:${questionId}`;
  }
}

function getLocalLastRead(questionId: string): string | null {
  try {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(lrKey(questionId));
  } catch {
    return null;
  }
}

function setLocalLastRead(questionId: string, iso: string) {
  try {
    if (typeof window === "undefined") return;
    localStorage.setItem(lrKey(questionId), iso);
  } catch {
    /* ignore */
  }
}

// ======================================================
// ✅ 학생 / 학부모 / 공통 API
// ======================================================

export function getCounterpartyLatestAt(q: QnaQuestion): number {
  const t = (s?: string) => (s ? new Date(s).getTime() : 0);
  return Math.max(t(q.lastStudentMsgAt), t(q.lastParentMsgAt), t(q.lastFollowupAt));
}

// 학생 방 (학생/교사/원장 접근 가능; 학생은 자기 방)
export async function getOrCreateStudentRoom(academyNumber: number): Promise<QnaId> {
  return requestAuthLocal(`/api/questions/room?academyNumber=${academyNumber}`, { method: "GET" });
}

// 학부모 방 (우선 통합 경로 시도 → 실패 시 전용 경로)
export async function getOrCreateParentRoom(academyNumber: number): Promise<QnaId> {
  try {
    return await requestAuthLocal(`/api/questions/room?academyNumber=${academyNumber}`, { method: "GET" });
  } catch {
    return requestAuthLocal(`/api/questions/room/parent?academyNumber=${academyNumber}`, { method: "GET" });
  }
}

// 역할 자동 판별(getOrCreate)
export async function getOrCreateRoomAuto(academyNumber: number): Promise<QnaId> {
  const sess = getSavedSession?.();
  const role = String(sess?.role ?? "").toLowerCase();
  if (role.includes("parent")) return getOrCreateParentRoom(academyNumber);
  return getOrCreateStudentRoom(academyNumber);
}

// 질문 단건 (★ lastReadAt 로컬 보강)
export async function getQuestion(questionId: string): Promise<QnaQuestion> {
  const q = await requestAuthLocal<QnaQuestion>(`/api/questions/${questionId}`, { method: "GET" });
  const id = String(q?.id ?? q?._id ?? questionId);
  const local = getLocalLastRead(id);
  // 백엔드 값 우선, 없으면 로컬 값 주입
  return { ...q, lastReadAt: q.lastReadAt ?? local ?? undefined };
}

// 답변 목록
export async function getAnswers(questionId: string): Promise<QnaAnswer[]> {
  return requestAuthLocal(`/api/questions/${questionId}/answers`, { method: "GET" });
}

// 읽음 처리 (★ 서버 갱신 + 로컬 lastReadAt 동기화)
export async function markQuestionRead(questionId: string): Promise<void> {
  await requestAuthLocal(`/api/questions/${questionId}/read`, { method: "PUT" });
  const now = new Date().toISOString();
  setLocalLastRead(String(questionId), now);
}

// 교사/원장 답변 작성
export async function postAnswer(questionId: string, content: string): Promise<QnaAnswer> {
  return requestAuthLocal(`/api/questions/${questionId}/answers`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ content }),
  });
}

/**
 * ✅ 레거시/모바일 데이터 폴백용 후속질문 조회
 * 1) /api/questions/{id}/followups
 * 2) /api/questions?parentId={id}
 */
export async function getFollowupsFlexible(rootId: string): Promise<any[]> {
  try {
    const a = await requestAuthLocal<any[]>(`/api/questions/${rootId}/followups`, { method: "GET" });
    if (Array.isArray(a)) return a;
  } catch {}
  try {
    const b = await requestAuthLocal<any[]>(
      `/api/questions?parentId=${encodeURIComponent(rootId)}`,
      { method: "GET" }
    );
    if (Array.isArray(b)) return b;
  } catch {}
  return [];
}

export async function postFollowupFlexible(
  parentQuestionId: string,
  content: string,
  academyNumber?: number
): Promise<any> {
  try {
    return await requestAuthLocal(`/api/questions/${parentQuestionId}/followups`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content }),
    });
  } catch {}
  return requestAuthLocal(`/api/questions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ parentId: parentQuestionId, content, academyNumber }),
  });
}

// ======================================================
// ✅ 최근 QnA 바로가기(API + 라우팅 헬퍼)
// ======================================================

export type QnaRecent = {
  questionId: string;
  answerId?: string | null;
  source: "ANSWER" | "QUESTION";
};

/** 서버에서 역할/학원 무관 '최신 QnA 1건' 선정 */
export async function getRecentQna(): Promise<QnaRecent | null> {
  try {
    const data = await requestAuthLocal<QnaRecent>(`/api/qna/recent`, { method: "GET" });
    if (data && data.questionId) return data;
    return null;
  } catch {
    return null;
  }
}

export async function openRecentQna(router: { push: (href: string) => void }) {
  const recent = await getRecentQna();
  if (recent?.questionId) {
    router.push(`/family-portal?tab=qna&qnaId=${encodeURIComponent(recent.questionId)}`);
  } else {
    alert("최근 QnA가 없습니다.");
  }
}

// ======================================================
// ✅ 교사 / 원장 전용 API
// ======================================================

/** 교사/원장: 학원별 학생 목록 조회 (여러 엔드포인트를 순차 시도) */
export async function getStudentsForAcademy(
  academyNumber: number
): Promise<Array<{ id: string; name?: string }>> {
  const tryPaths = [
    `/api/teachers/students?academyNumber=${academyNumber}`, // 우선 권장
    `/api/students?academyNumber=${academyNumber}`, // 대안 1
    `/api/classes?academyNumber=${academyNumber}`, // 대안 2 (수업에서 학생 집계)
  ];

  for (const p of tryPaths) {
    try {
      const res = await requestAuthLocal<any>(p, { method: "GET" });
      if (!res) continue;

      // 1) 배열 [{id,name}] 형태
      if (Array.isArray(res) && res.length && (res[0]?.id || res[0]?._id)) {
        return res.map((x: any) => ({ id: String(x.id ?? x._id), name: x.name ?? x.studentName }));
      }

      // 2) {students:[{id,name}]} 형태
      if (Array.isArray(res?.students)) {
        return res.students.map((x: any) => ({ id: String(x.id ?? x._id), name: x.name ?? x.studentName }));
      }

      // 3) classes에서 학생 집계
      if (Array.isArray(res?.content) || Array.isArray(res?.items)) {
        const arr = (res.content ?? res.items) as any[];
        const bag: Record<string, { id: string; name?: string }> = {};
        for (const c of arr) {
          const sts = c?.students ?? c?.studentIds ?? [];
          for (const s of sts) {
            const id = String(s?.id ?? s?._id ?? s);
            if (!bag[id]) bag[id] = { id, name: s?.name ?? s?.studentName };
          }
        }
        return Object.values(bag);
      }
    } catch {
      // 다음 경로 시도
    }
  }
  return [];
}

/** 교사/원장: 학원 + 학생ID 기준 방 생성/조회 (교사 답변 전용, 필요 시만 사용) */
export async function getOrCreateTeacherRoom(
  academyNumber: number,
  studentId: string
): Promise<QnaId> {
  if (!academyNumber || !studentId) throw new Error("학원 또는 학생 선택이 필요합니다.");
  const res = await requestAuthLocal(
    `/api/questions/room?academyNumber=${academyNumber}&studentId=${encodeURIComponent(studentId)}`,
    { method: "GET" }
  );
  if (!res || !(res.id || res._id)) throw new Error("질문 방을 찾거나 생성하지 못했습니다.");
  return res;
}

/** ✅ 교사/원장: 학원별 질문 목록 조회 (unreadCount 누락 시 0으로 보강) */
export async function listQuestions(academyNumber?: number): Promise<QnaQuestion[]> {
  const qs = academyNumber ? `?academyNumber=${academyNumber}` : "";
  const raw = await requestAuthLocal<QnaQuestion[]>(`/api/questions${qs}`, { method: "GET" });
  return Array.isArray(raw) ? raw.map((x) => ({ ...x, unreadCount: x?.unreadCount ?? 0 })) : [];
}

/** (선택) 교사/원장: ID(학생/학부모)를 넣으면 해당 대상 방 조회/생성 */
export async function getOrCreateTeacherRoomById(
  academyNumber: number,
  targetId: string
): Promise<QnaId> {
  return requestAuthLocal(
    `/api/questions/room/by-id?academyNumber=${academyNumber}&id=${encodeURIComponent(targetId)}`,
    { method: "GET" }
  );
}

export type QnaListItem = {
  id: string;
  unreadCount?: number | null;
  createdAt?: string | null;   // ISO
  updatedAt?: string | null;   // 서버가 주면 사용 (없으면 createdAt 사용)
};

// 상대방 최신 시각 → 없으면 updatedAt/createdAt
function scoreForRecent(i: QnaListItem & Partial<QnaQuestion>): number {
  const base = Math.max(
    i.updatedAt ? new Date(i.updatedAt).getTime() : 0,
    i.createdAt ? new Date(i.createdAt).getTime() : 0
  );
  try {
    const cp = getCounterpartyLatestAt(i as QnaQuestion); // lastStudentMsgAt/lastParentMsgAt/lastFollowupAt
    return Math.max(cp || 0, base);
  } catch {
    return base;
  }
}

export function pickLatestTarget(
  items: Array<QnaListItem & Partial<QnaQuestion>>
): (QnaListItem & Partial<QnaQuestion>) | undefined {
  if (!items?.length) return undefined;
  const unread = items.filter((i) => (i.unreadCount ?? 0) > 0);
  const pool = unread.length ? unread : items;
  return [...pool].sort((a, b) => scoreForRecent(b) - scoreForRecent(a))[0];
}

// (선택) 목록 정렬 헬퍼
export function sortByCounterpartyLatest<T extends Partial<QnaQuestion>>(arr: T[]): T[] {
  return [...arr].sort((a, b) => scoreForRecent(b as any) - scoreForRecent(a as any));
}
