// src/app/qna/TeacherQnaPanel.tsx
"use client";

import { useEffect, useMemo, useRef, useState, startTransition } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { getSavedSession } from "@/lib/api";
import {
  listQuestions,
  getQuestion as apiGetQuestion,
  getAnswers as apiGetAnswers,
  postAnswer as apiPostAnswer,
  getFollowupsFlexible,
  markQuestionRead as apiMarkQuestionRead,
} from "@/lib/qna";

/* ======================= 타입 ======================= */
type IdLike = { id?: string; _id?: string };

type QnaQuestion = IdLike & {
  title?: string;
  content?: string;
  authorRole?: string;
  createdAt?: string;
  updatedAt?: string;
  lastAnswerAt?: string;
  unreadCount?: number;
  followups?: any[];
  academyNumber?: number;
  lastReadAt?: string;

  student?: { id?: string; _id?: string; name?: string; studentName?: string; displayName?: string };
  parent?: { id?: string; _id?: string; name?: string; parentName?: string; childStudentName?: string; displayName?: string };
  child?: { id?: string; _id?: string; name?: string; studentName?: string };
  room?: {
    studentId?: string;
    parentId?: string;
    studentName?: string;
    parentName?: string;
    childStudentName?: string;
    student?: { id?: string; _id?: string; name?: string; studentName?: string };
    parent?: { id?: string; _id?: string; name?: string; parentName?: string };
  };
  studentId?: string;
  parentId?: string;
  childStudentId?: string;
  studentName?: string;
  parentName?: string;
  childStudentName?: string;
};

type QnaAnswer = IdLike & {
  content?: string;
  author?: string;
  authorRole?: string; // "teacher" | "director"
  teacherName?: string;
  createdAt?: string;
};

const API_BASE = "/backend";

/* ======================= 상수/스토리지 ======================= */
const CLOCK_SKEW_MS = 5000;
const READ_AT_KEY_BASE = "qna:readAt";
const MIN_SPIN_MS = 200;

function readKeyForUser() {
  const user = getSavedSession?.()?.username ?? "anon";
  return `${READ_AT_KEY_BASE}:${user}`;
}
function loadJSON<T>(key: string, fallback: T): T {
  try {
    if (typeof window === "undefined") return fallback;
    const raw = sessionStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}
function saveJSON<T>(key: string, value: T) {
  try {
    if (typeof window === "undefined") return;
    sessionStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* ignore */
  }
}
function loadReadAt(): Record<string, string | undefined> {
  return loadJSON(readKeyForUser(), {});
}
function persistReadAt(map: Record<string, string | undefined>) {
  saveJSON(readKeyForUser(), map);
}

/* ======================= 유틸 ======================= */
function normalizeRole(raw?: unknown) {
  const s = String(raw ?? "").toLowerCase();
  if (s.includes("teacher")) return "teacher";
  if (s.includes("director")) return "director";
  if (s.includes("parent")) return "parent";
  return "student";
}
async function apiGet<T>(url: string, token?: string): Promise<T> {
  const r = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    cache: "no-store",
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}
function getId(x: any): string | null {
  if (!x) return null;
  const id = x._id ?? x.id;
  return id ? String(id) : null;
}
function safeStr(x: any): string | undefined {
  if (typeof x === "string" && x.trim().length) return x.trim();
  return undefined;
}
function extractInlineNames(q: any) {
  const title = safeStr(q?.title) || "";
  const studentFromTitle =
    (title.match(/학생\s+(.+?)\s*채팅방/i)?.[1] ?? undefined) ||
    (title.match(/자녀\s+(.+?)\s*채팅방/i)?.[1] ?? undefined);
  const parentFromTitle =
    (title.match(/보호자\s+(.+?)\s*채팅방/i)?.[1] ?? undefined) ||
    (title.match(/학부모\s+(.+?)\s*채팅방/i)?.[1] ?? undefined);

  const studentName =
    safeStr(q?.studentName) ||
    safeStr(q?.student?.name) ||
    safeStr(q?.student?.studentName) ||
    safeStr(q?.childStudentName) ||
    safeStr(studentFromTitle) ||
    undefined;

  const parentName =
    safeStr(q?.parentName) ||
    safeStr(q?.parent?.name) ||
    safeStr(q?.parent?.parentName) ||
    safeStr(parentFromTitle) ||
    undefined;

  const childName =
    safeStr(q?.childStudentName) ||
    safeStr(q?.studentName) ||
    safeStr(q?.student?.name) ||
    safeStr(q?.student?.studentName) ||
    safeStr(studentFromTitle) ||
    undefined;

  const studentId =
    safeStr(q?.studentId) ||
    safeStr(q?.childStudentId) ||
    getId(q?.student) ||
    undefined;

  const parentId = safeStr(q?.parentId) || getId(q?.parent) || undefined;

  const academyNumber: number | undefined =
    typeof q?.academyNumber === "number" ? q.academyNumber : undefined;

  return { studentName, parentName, childName, studentId, parentId, academyNumber };
}
function latestActivityTs(q: QnaQuestion): number {
  const lastAnswerTs = q.lastAnswerAt ? new Date(q.lastAnswerAt).getTime() : 0;
  const updatedTs = q.updatedAt ? new Date(q.updatedAt).getTime() : 0;
  const createdTs = q.createdAt ? new Date(q.createdAt).getTime() : 0;
  return Math.max(lastAnswerTs, updatedTs, createdTs);
}
function getSortTime(q: QnaQuestion): number {
  return latestActivityTs(q);
}

/** YYYY-MM-DD HH:mm:ss 로 통일된 시간 포맷 */
function formatYmdHms(s?: string) {
  if (!s) return "";
  const d = new Date(s);
  if (isNaN(d.getTime())) return "";
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

/* ======================= 컴포넌트 ======================= */
type TeacherQnaPanelProps = {
  questionId?: string;
  /** “최근 QnA 바로가기”에서만 true로 넘겨 자동 진입 */
  recentShortcut?: boolean;
};

export default function TeacherQnaPanel({ questionId, recentShortcut = false }: TeacherQnaPanelProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname();

  // 🔒 변하지 않는 prop 스냅샷 (deps 길이 흔들림 방지)
  const recentShortcutRef = useRef(recentShortcut);
  // StrictMode 2회 실행 가드
  const bootedRef = useRef(false);
  const depsAnchor = useRef(0);

  const qidFromQuery = searchParams?.get("questionId") || undefined;
  const academyFromQuery = (() => {
    const v = searchParams?.get("academy");
    const n = v ? Number(v) : NaN;
    return Number.isFinite(n) ? n : undefined;
  })();

  const [switching, setSwitching] = useState(false);
  const [academies, setAcademies] = useState<number[]>([]);
  const [selectedAcademy, setSelectedAcademy] = useState<number | null>(null);

  const [questions, setQuestions] = useState<QnaQuestion[]>([]);
  const [currentId, setCurrentId] = useState<string | null>(null);

  const [question, setQuestion] = useState<QnaQuestion | null>(null);
  const [answers, setAnswers] = useState<QnaAnswer[]>([]);
  const [input, setInput] = useState("");
  const [error, setError] = useState<string | null>(null);

  const [qSearch, setQSearch] = useState("");
  const [onlyUnread, setOnlyUnread] = useState(false);
  const [unreadTotal, setUnreadTotal] = useState(0);

  // 🔑 내가 마지막으로 본 시각(로컬)
  const [readAtById, setReadAtById] = useState<Record<string, string | undefined>>(
    () => loadReadAt()
  );

  const currentIdRef = useRef<string | null>(null);
  useEffect(() => {
    currentIdRef.current = currentId;
  }, [currentId]);

  // 🔴 말풍선 기준 시각 + 페이지 진입시각
  const prevReadAtRef = useRef<string | null>(null);
  const pageEnterAtRef = useRef<string>(new Date().toISOString());

  // ✅ “나갈 때 읽음 처리”를 위한 이전 방 추적자
  const prevOpenedIdRef = useRef<string | null>(null);

  /* ---------- 안전한 쿼리 변경 ---------- */
  const setQuerySafe = (next: Record<string, string | undefined>) => {
    const curr = new URLSearchParams(searchParams.toString());
    Object.entries(next).forEach(([k, v]) => {
      if (v == null || v === "") curr.delete(k);
      else curr.set(k, v);
    });
    const nextStr = curr.toString();
    const prevStr = searchParams.toString();
    if (nextStr === prevStr) return; // 동일 → 내비게이션 불필요

    const url = `${pathname}${nextStr ? `?${nextStr}` : ""}`;
    // 스크롤 이동 방지, 리렌더 최소화
    router.replace(url, { scroll: false });
  };

  const setReadNow = (id: string) => {
    const nowIso = new Date().toISOString();
    setReadAtById((prev) => {
      const next = { ...prev, [id]: nowIso };
      persistReadAt(next);
      return next;
    });
  };

  // ✅ 목록 미확인 카운트(표시 기준)
  const calcDisplayUnread = (q: QnaQuestion): number => {
    const id = (q as any)?._id || (q as any)?.id;
    if (!id) return 0;

    const latest = latestActivityTs(q);
    const readAtIso = readAtById[id];
    const serverCount = typeof q.unreadCount === "number" ? q.unreadCount : undefined;

    if (readAtIso) {
      const readTs = new Date(readAtIso).getTime();
      if (latest <= readTs + CLOCK_SKEW_MS) return 0;
      return serverCount ?? 0;
    }
    return serverCount ?? 0;
  };

  // 목록 필터
  const filteredQuestions = useMemo(() => {
    const keyword = qSearch.trim().toLowerCase();
    const base = questions.filter((q) => {
      const displayU = calcDisplayUnread(q);
      return onlyUnread ? displayU > 0 : true;
    });

    if (!keyword) return base;

    return base.filter((q) => {
      const title = String(q?.title ?? "").toLowerCase();
      const { studentName, parentName, childName } = extractInlineNames(q);
      const s = (studentName ?? "").toLowerCase();
      const p = (parentName ?? "").toLowerCase();
      const c = (childName ?? "").toLowerCase();
      return (
        title.includes(keyword) ||
        (s && s.includes(keyword)) ||
        (p && p.includes(keyword)) ||
        (c && c.includes(keyword))
      );
    });
  }, [questions, qSearch, onlyUnread, readAtById]);

  // 상단 합계는 표시 기준으로 합산
  useEffect(() => {
    const sum = questions.reduce((acc, q) => acc + calcDisplayUnread(q), 0);
    setUnreadTotal(sum);
  }, [questions, readAtById]);

  /* ========== 데이터 로드 ========== */
  async function loadAllAcademyQuestions(acads: number[]) {
    const buckets = await Promise.all(
      acads.map((n) =>
        listQuestions(n)
          .then((res) => ({ academy: n, items: Array.isArray(res) ? (res as QnaQuestion[]) : [] }))
          .catch(() => ({ academy: n, items: [] }))
      )
    );
    return buckets;
  }

  // 리스트 갱신
  async function refreshGlobalAndList(currentAcad: number | null, allAcads: number[]) {
    if (!allAcads.length) {
      setUnreadTotal(0);
      setQuestions([]);
      return;
    }

    const buckets = await loadAllAcademyQuestions(allAcads);

    // 현재 학원 리스트만 교체(정렬)
    if (Number.isFinite(currentAcad as number)) {
      const listRaw = buckets.find((b) => b.academy === currentAcad)?.items ?? [];
      const list = [...listRaw].sort((a, b) => getSortTime(b) - getSortTime(a));
      setQuestions(list);
    }

    // 합계 갱신
    const total = buckets.flatMap((b) => b.items).reduce((acc, q) => acc + calcDisplayUnread(q), 0);
    setUnreadTotal(total);
  }

  // 이름/헤더 보강
  const [headerStudentName, setHeaderStudentName] = useState<string | null>(null);
  const [headerParentName, setHeaderParentName] = useState<string | null>(null);
  const [headerAcademy, setHeaderAcademy] = useState<number | null>(null);

  async function hydrateHeaderNames(q: QnaQuestion) {
    try {
      if (!q) return;

      const {
        studentName: sInline,
        parentName: pInline,
        childName: cInline,
        studentId,
        parentId,
        academyNumber,
      } = extractInlineNames(q);

      setHeaderAcademy(
        typeof academyNumber === "number"
          ? academyNumber
          : typeof selectedAcademy === "number"
          ? selectedAcademy
          : null
      );

      let sName: string | null = sInline ?? cInline ?? null;
      let pName: string | null = pInline ?? null;
      setHeaderStudentName(sName ?? null);
      setHeaderParentName(pName ?? null);

      const session: any = getSavedSession?.();
      const token = session?.token;
      if (!token || !API_BASE) return;

      const roomId = (q as any)?._id || (q as any)?.id || null;

      if (!sName && studentId) {
        try {
          const student = await apiGet<any>(
            `${API_BASE}/api/students/${encodeURIComponent(studentId)}`,
            token
          );
          if (currentIdRef.current !== roomId) return;
          sName =
            safeStr(student?.name) ||
            safeStr(student?.studentName) ||
            safeStr(student?.displayName) ||
            String(studentId);
          setHeaderStudentName(sName);
        } catch {}
      }

      if (!pName && parentId) {
        try {
          const parent = await apiGet<any>(
            `${API_BASE}/api/parents/${encodeURIComponent(parentId)}`,
            token
          );
          if (currentIdRef.current !== roomId) return;
          pName =
            safeStr(parent?.name) ||
            safeStr(parent?.parentName) ||
            safeStr(parent?.displayName) ||
            String(parentId);
          setHeaderParentName(pName);
          if (!sName) {
            sName =
              safeStr(parent?.childStudentName) ||
              safeStr(parent?.childName) ||
              safeStr(parent?.studentName) ||
              null;
            setHeaderStudentName(sName);
          }
        } catch {}
      }
    } catch {}
  }

  // 스레드 새로고침
  const reloadThread = async (rootId: string) => {
    const [q, a, fu] = await Promise.all([
      apiGetQuestion(rootId),
      apiGetAnswers(rootId),
      getFollowupsFlexible(rootId),
    ]);

    const qCast = q as QnaQuestion;

    // 학원 동기화 + 해당 학원 목록 교체
    const qAcad = (qCast as any)?.academyNumber;
    if (Number.isFinite(qAcad)) {
      setSelectedAcademy((prev) => (prev === qAcad ? prev : qAcad));
      setHeaderAcademy(qAcad);

      listQuestions(qAcad)
        .then((list) => {
          const arr = Array.isArray(list) ? (list as QnaQuestion[]) : [];
          setQuestions([...arr].sort((a, b) => getSortTime(b) - getSortTime(a)));
        })
        .catch(() => {});
    }

    setQuestion({ ...qCast, followups: Array.isArray(fu) ? fu : [] });
    setAnswers(Array.isArray(a) ? (a as QnaAnswer[]) : []);

    if (!prevReadAtRef.current) {
      prevReadAtRef.current = qCast?.lastReadAt ?? null;
    }

    await hydrateHeaderNames(qCast);

    requestAnimationFrame(() => {
      const el = document.querySelector<HTMLDivElement>("#chatbox");
      if (el) el.scrollTop = el.scrollHeight;
    });
  };

  // 학생/학부모(상대방) 최신 메시지 시각 계산
  async function latestCounterpartTs(roomId: string, q?: QnaQuestion): Promise<number> {
    try {
      const fu = await getFollowupsFlexible(roomId);
      const studentParentTs = (Array.isArray(fu) ? fu : [])
        .filter((x: any) => {
          const r = String(x?.authorRole ?? "").toLowerCase();
          return r.includes("student") || r.includes("parent");
        })
        .map((x: any) => (x?.createdAt ? +new Date(x.createdAt) : 0))
        .filter((n: number) => Number.isFinite(n));

      const fromFollowups = studentParentTs.length ? Math.max(...studentParentTs) : 0;
      const fromQuestion = q?.createdAt ? +new Date(q.createdAt) : 0;
      return Math.max(fromFollowups, fromQuestion);
    } catch {
      return q?.createdAt ? +new Date(q.createdAt) : 0;
    }
  }

  // ✅ “최근 QnA 바로가기” 선택 규칙(상대방 최신 기준)
  async function pickBestRoomForShortcut(acads: number[]): Promise<string | null> {
    const buckets = await loadAllAcademyQuestions(acads);
    const all = buckets.flatMap((b) => b.items);

    const withId = all
      .map((q) => ({ q, id: (q as any)?._id || (q as any)?.id }))
      .filter((x) => !!x.id) as { q: QnaQuestion; id: string }[];

    // 1) 미확인 후보 우선
    const unread = withId.filter(({ q }) => calcDisplayUnread(q) > 0);
    const LIMIT = 50;

    if (unread.length) {
      const pool = unread.slice(0, LIMIT);
      const tsArr = await Promise.all(
        pool.map(async ({ id, q }) => ({ id, ts: await latestCounterpartTs(id, q) }))
      );
      const best = tsArr.reduce<{ id: string | null; ts: number }>(
        (acc, cur) => (cur.ts > acc.ts ? cur : acc),
        { id: null, ts: -1 }
      );
      if (best.id) return best.id;
    }

    // 2) 전체 후보
    const poolAll = withId.slice(0, LIMIT);
    const tsArrAll = await Promise.all(
      poolAll.map(async ({ id, q }) => ({ id, ts: await latestCounterpartTs(id, q) }))
    );
    const bestAll = tsArrAll.reduce<{ id: string | null; ts: number }>(
      (acc, cur) => (cur.ts > acc.ts ? cur : acc),
      { id: null, ts: -1 }
    );
    return bestAll.id;
  }

  // 질문 열기(입장시 읽음 처리 ❌) + URL 반영 + 스피너
  const openQuestion = async (id: string) => {
    const started = Date.now();
    setSwitching(true);
    try {
      prevReadAtRef.current = readAtById[id] ?? null;
      setCurrentId(id);
      // recent 쿼리도 확실히 제거
      setQuerySafe({ academy: String(selectedAcademy ?? ""), questionId: id, recent: undefined });

      await reloadThread(id);
    } finally {
      const remain = MIN_SPIN_MS - (Date.now() - started);
      if (remain > 0) await new Promise((r) => setTimeout(r, remain));
      setSwitching(false);
    }
  };

  // ✅ “나갈 때” 읽음 처리 (이전 방)
  useEffect(() => {
    const prev = prevOpenedIdRef.current;
    if (prev && prev !== currentId) {
      setReadNow(prev);
      apiMarkQuestionRead(prev).catch(() => {});
    }
    prevOpenedIdRef.current = currentId || null;
  }, [currentId]);

  // 언마운트 시 마지막 방 처리
  useEffect(() => {
    return () => {
      const last = prevOpenedIdRef.current;
      if (last) {
        setReadNow(last);
        apiMarkQuestionRead(last).catch(() => {});
      }
    };
  }, []);

  // 부팅 (deps 고정, StrictMode 가드)
  useEffect(() => {
    if (bootedRef.current) return; // ✅ 개발모드 2회 실행 방지
    bootedRef.current = true;

    (async () => {
      try {
        setError(null);

        const session: any = getSavedSession?.();
        if (!session?.token) {
          router.push("/login?next=/qna");
          return;
        }

        const role = normalizeRole(session?.role);
        if (!(role === "teacher" || role === "director")) {
          setError("교사/원장만 접근할 수 있습니다.");
          return;
        }

        const acads = (session?.academyNumbers || [])
          .map((n: any) => Number(n))
          .filter((n: number) => Number.isFinite(n)) as number[];

        setAcademies(acads);

        // 초기 학원: URL ?academy 우선, 없으면 첫 번째
        const initialAcademy = academyFromQuery ?? (acads.length ? acads[0] : null);
        setSelectedAcademy(initialAcademy);

        if (acads.length === 0) {
          setError("소속된 학원이 없습니다.");
          return;
        }

        // 전체 요약 + 현재 학원 리스트 세팅
        await refreshGlobalAndList(initialAcademy, acads);

        // ① 자동 오픈 조건: (a) prop 제공 or (b) 최근바로가기 모드에서만 URL의 questionId 인정
        const effectiveQid = questionId ?? (recentShortcutRef.current ? qidFromQuery : undefined);
        if (effectiveQid) {
          await openQuestion(String(effectiveQid));
          return;
        }

        // ② 최근 QnA 바로가기 모드: bestId 선택
        if (recentShortcutRef.current) {
          const bestIdFromShortcut = await pickBestRoomForShortcut(acads);
          if (bestIdFromShortcut) {
            await openQuestion(bestIdFromShortcut);
            return;
          }
        }

        // ③ 일반 진입: 현재 학원의 첫 번째 방 자동 오픈 시도 → 없으면 전체 중 best 선택
        let opened = false;

        if (initialAcademy != null) {
          const listForInitial = await listQuestions(initialAcademy);
          const arr = Array.isArray(listForInitial) ? (listForInitial as QnaQuestion[]) : [];
          const sorted = [...arr].sort((a, b) => getSortTime(b) - getSortTime(a));
          setQuestions(sorted);

          if (sorted.length > 0) {
            const firstId = (sorted[0] as any)?.id || (sorted[0] as any)?._id;
            if (firstId) {
              await openQuestion(String(firstId));
              opened = true;
            }
          }
        }

        if (!opened) {
          const bestId = await pickBestRoomForShortcut(acads);
          if (bestId) {
            await openQuestion(bestId);
            opened = true;
          }
        }

        if (!opened) {
          // 정말 열 수 있는 방이 아무 것도 없을 때만 안내 문구 유지
          setCurrentId(null);
          setQuestion(null);
          setAnswers([]);
          setQuerySafe({ academy: String(initialAcademy ?? ""), questionId: undefined, recent: undefined });
        }
      } catch (e: any) {
        const msg = String(e?.message ?? "");
        if (/^AUTH_(401|403)/.test(msg) || /^(401|403)\b/.test(msg) || /Unauthorized|Forbidden/i.test(msg)) {
          router.push("/login?next=/qna");
          return;
        }
        setError(e?.message ?? "오류가 발생했습니다.");
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // StrictMode에서도 동작하도록 내부 IIFE

  // 외부에서 questionId/쿼리 바뀌면 해당 스레드 열기 (deps 길이 고정)
  useEffect(() => {
    if (!qidFromQuery) return;
    if (currentIdRef.current === qidFromQuery) return; // 동일 방이면 noop
    startTransition(() => {
      openQuestion(String(qidFromQuery));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [qidFromQuery, depsAnchor.current]); // ← 항상 길이 2 유지

  // 학원 파라미터 변경 → 상태 동기화
  useEffect(() => {
    if (!academyFromQuery) return;
    setSelectedAcademy((prev) => (prev === academyFromQuery ? prev : academyFromQuery));
  }, [academyFromQuery]);

  /* ===== 상대 이름(label) 헬퍼: 학생/학부모 이름 표시 ===== */
  const counterpartName = (authorRole?: string) => {
    const role = String(authorRole ?? "").toLowerCase();
    const inline = question ? extractInlineNames(question) : ({} as any);

    const studentNm =
      headerStudentName ||
      inline?.studentName ||
      inline?.childName ||
      "학생";

    const parentNm =
      headerParentName ||
      inline?.parentName ||
      "학부모";

    if (role.includes("parent")) return parentNm;
    if (role.includes("student")) return studentNm;

    // 역할 정보가 없으면 학생 이름으로 우선 표시
    return studentNm;
  };

  // 채팅 메시지 구성(렌더 내에서 계산)
  type ChatMsg = { _id?: string; side: "student" | "teacher"; text: string; createdAt?: string; meta?: string };

  // 헤더(상단 바의 뱃지에만 사용)
  const leftInfoParts: string[] = [];
  if (typeof headerAcademy === "number") leftInfoParts.push(`학원 #${headerAcademy}`);
  if (headerParentName) leftInfoParts.push(`${headerParentName}(학부모)`);
  if (headerStudentName) leftInfoParts.push(headerParentName ? `${headerStudentName}(자녀)` : `${headerStudentName}`);
  const leftInfo = leftInfoParts.join(" · ");

  return (
    <div className="rounded-2xl p-0 border shadow-sm overflow-hidden relative">
      {/* 상단 바 */}
      <div className="px-5 py-4 border-b bg-gray-50 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 min-w-0">
          <div className="text-lg font-semibold text-gray-900 shrink-0">Q&amp;A (교사/원장)</div>

          {/* 학원 선택(컨트롤드 + URL 동기화) */}
          <select
            className="h-8 rounded-lg border px-2 text-sm bg-white text-gray-900"
            value={selectedAcademy != null ? String(selectedAcademy) : ""}
            onChange={(e) => onChangeAcademy(e.target.value)}
          >
            {academies.length === 0 ? (
              <option value="">학원 없음</option>
            ) : (
              academies.map((n) => (
                <option key={n} value={String(n)}>
                  학원 #{n}
                </option>
              ))
            )}
          </select>

          {/* 방 정보 뱃지: 방 선택 후에만 */}
          {leftInfo && currentId && (
            <span className="ml-1 inline-flex items-center truncate gap-2 rounded-md px-2 py-1 text-xs font-semibold bg-white border border-gray-300 text-gray-900">
              {leftInfo}
            </span>
          )}
        </div>

        {/* 전역 미확인 합계 */}
        <div className="px-2 py-1 rounded-md border border-gray-300 bg-gray-100 text-[11px] font-semibold text-gray-900">
          미확인 메시지: {unreadTotal}
        </div>
      </div>

      {/* 전환 스피너 */}
      {switching && (
        <div className="absolute inset-0 bg-white/60 backdrop-blur-[1px] flex items-center justify-center z-20">
          <div className="flex items-center gap-2 text-sm text-black">
            <svg className="animate-spin h-5 w-5 text-black" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4A4 4 0 008 12H4z" />
            </svg>
            전환 중…
          </div>
        </div>
      )}

      {/* 본문 */}
      <div className="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-4 p-4">
        {/* 왼쪽: 질문 목록 */}
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
          <div className="text-sm font-semibold text-gray-900 mb-3">질문 목록</div>

          {/* 검색/토글 */}
          <div className="mb-3 flex items-center gap-2">
            <input
              value={qSearch}
              onChange={(e) => setQSearch(e.target.value)}
              className="flex-1 h-9 rounded-lg border px-3 text-sm bg-white text-gray-900 placeholder-gray-400"
              placeholder="이름(학생/학부모/자녀) 또는 제목으로 검색…"
            />
            <button
              onClick={() => setOnlyUnread((v) => !v)}
              className={`h-9 px-3 rounded-lg text-xs font-semibold ring-1 transition ${
                onlyUnread ? "bg-emerald-100 text-emerald-800 ring-emerald-200" : "bg-gray-50 text-gray-800 ring-gray-200"
              }`}
              title="미확인 채팅만 보기"
            >
              미확인 채팅
            </button>
          </div>

          {filteredQuestions.length === 0 ? (
            <div className="text-sm text-gray-600">
              {qSearch.trim().length || onlyUnread ? "검색/필터 결과가 없습니다." : "표시할 질문이 없습니다."}
            </div>
          ) : (
            <ul className="divide-y">
              {filteredQuestions.map((q) => {
                const id = (q as any)?.id || (q as any)?._id;
                const timeText = formatYmdHms(q.updatedAt || q.lastAnswerAt || q.createdAt);
                const displayU = calcDisplayUnread(q);

                return (
                  <li key={id}>
                    <button
                      onClick={async () => {
                        if (!id) return;
                        await openQuestion(String(id));
                      }}
                      className={`w-full text-left px-2 py-2 text-sm ${currentId === id ? "bg-gray-50" : ""}`}
                    >
                      <div className="font-medium text-gray-900 truncate">{q.title ?? "(제목 없음)"}</div>
                      <div className="text-xs text-gray-600 flex items-center gap-1">
                        <span>#{q.academyNumber ?? selectedAcademy}</span>
                        <span>·</span>
                        <span>{timeText}</span>
                        {displayU > 0 && (
                          <span className="ml-1 inline-block rounded-full bg-emerald-100 text-emerald-700 px-1.5 py-[1px] text-[10px]">
                            +{displayU}
                          </span>
                        )}
                      </div>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* 오른쪽: 채팅/안내 */}
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm overflow-hidden">
          {!currentId ? (
            <div className="h-[420px] flex items-center justify-center text-sm text-gray-500">
              채팅방을 선택해 주세요
            </div>
          ) : (
            <>
              <div id="chatbox" className="h-[420px] overflow-y-auto px-5 py-3 space-y-3">
                {(() => {
                  const msgs: ChatMsg[] = [];
                  const qany = question as any;
                  const mainText = typeof qany?.content === "string" ? qany.content.trim() : "";
                  if (mainText) {
                    msgs.push({
                      _id: qany?._id || qany?.id || "question",
                      side: "student",
                      text: mainText,
                      createdAt: qany?.createdAt,
                      meta: counterpartName(qany?.authorRole),
                    });
                  }
                  const followups: any[] = Array.isArray(qany?.followups) ? qany.followups : [];
                  for (const fq of followups) {
                    const text =
                      (typeof fq?.content === "string" && fq.content.trim()) ||
                      (typeof fq?.message === "string" && fq.message.trim()) ||
                      (typeof fq?.body === "string" && fq.body.trim()) ||
                      "";
                    if (text) {
                      msgs.push({
                        _id: fq?._id || fq?.id,
                        side: "student",
                        text,
                        createdAt: fq?.createdAt,
                        meta: counterpartName(fq?.authorRole),
                      });
                    }
                  }
                  for (const a of answers || []) {
                    const text =
                      (typeof a?.content === "string" && a.content.trim()) ||
                      (typeof (a as any)?.message === "string" && (a as any).message.trim()) ||
                      "";
                    if (text) {
                      const label =
                        a?.teacherName
                          ? `by ${a.teacherName}`
                          : String(a?.authorRole ?? "").toLowerCase() === "director"
                          ? "director"
                          : "teacher";
                      msgs.push({
                        _id: (a as any)?._id || (a as any)?.id,
                        side: "teacher",
                        text,
                        createdAt: a?.createdAt,
                        meta: label,
                      });
                    }
                  }

                  msgs.sort((m1, m2) => {
                    const t1 = m1.createdAt ? +new Date(m1.createdAt) : 0;
                    const t2 = m2.createdAt ? +new Date(m2.createdAt) : 0;
                    return t1 - t2;
                  });

                  if (msgs.length === 0) {
                    return <div className="text-sm text-gray-500">표시할 메시지가 없습니다.</div>;
                  }

                  // 🔴 빨간점 계산
                  const lastReadIso = prevReadAtRef.current;
                  const readTsForBubble = lastReadIso ? new Date(lastReadIso).getTime() : 0;
                  const unreadCount = typeof question?.unreadCount === "number" ? question.unreadCount : 0;
                  const studentMsgs = msgs.filter((m) => m.side === "student");

                  return msgs.map((m) => {
                    const createdTs = m.createdAt ? new Date(m.createdAt).getTime() : 0;

                    let showRedDot = false;
                    if (m.side === "student") {
                      const byTime = !!lastReadIso && createdTs > readTsForBubble + CLOCK_SKEW_MS;

                      const idxInStudent = studentMsgs.findIndex((x) => x._id === m._id);
                      const byCount =
                        !lastReadIso &&
                        unreadCount > 0 &&
                        idxInStudent >= 0 &&
                        idxInStudent >= studentMsgs.length - unreadCount;

                      const byEnterTime =
                        !lastReadIso &&
                        unreadCount === 0 &&
                        createdTs > new Date(pageEnterAtRef.current).getTime();

                      showRedDot = byTime || byCount || byEnterTime;
                    }

                    return (
                      <div
                        key={m._id}
                        className={`relative max-w-[80%] rounded-2xl px-4 py-2 text-sm whitespace-pre-wrap ${
                          m.side === "teacher" ? "ml-auto bg-blue-50 border border-blue-200" : "mr-auto bg-white border"
                        }`}
                      >
                        {showRedDot && (
                          <span className="absolute -top-1 -right-1 inline-block h-2 w-2 rounded-full bg-red-500 ring-2 ring-white" />
                        )}

                        <div className="text-gray-800">{m.text}</div>
                        <div className="text-[11px] text-gray-400 mt-1 flex items-center gap-1">
                          <span>{m.meta}</span>
                          <span>·</span>
                          <span>{m.createdAt ? formatYmdHms(m.createdAt) : ""}</span>
                        </div>
                      </div>
                    );
                  });
                })()}
              </div>

              {/* 입력창 */}
              <div className="p-4 border-t bg-white">
                <div className="flex gap-2">
                  <input
                    className="flex-1 rounded-xl border border-gray-400 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-gray-900 text-black placeholder-black/70"
                    placeholder="답변을 입력하세요…"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !e.shiftKey) {
                        e.preventDefault();
                        handleSend();
                      }
                    }}
                  />
                  <button
                    className={`rounded-xl px-4 py-2 text-sm font-semibold ${
                      input.trim().length > 0 && (question as any)?._id
                        ? "bg-black text-white"
                        : "bg-gray-200 text-gray-600 cursor-not-allowed"
                    }`}
                    onClick={handleSend}
                    disabled={!(input.trim().length > 0 && (question as any)?._id)}
                  >
                    전송
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* 에러 표시 */}
      {error && (
        <div className="px-4 pb-4 text-sm text-red-600">
          {error}
        </div>
      )}
    </div>
  );

  // 학원 변경 시: 그 학원의 첫 번째 방 자동 오픈
  async function onChangeAcademy(academyNoRaw: number | string) {
    const academyNo = typeof academyNoRaw === "string" ? parseInt(academyNoRaw, 10) : academyNoRaw;
    if (!Number.isFinite(academyNo) || academyNo === selectedAcademy) return;

    const started = Date.now();
    setSwitching(true);
    try {
      setError(null);
      setSelectedAcademy(academyNo);

      // URL 동기화(+ questionId/recent 제거)
      setQuerySafe({ academy: String(academyNo), questionId: undefined, recent: undefined });

      // 해당 학원 리스트만 로드
      const listRaw = await listQuestions(academyNo);
      const list = Array.isArray(listRaw) ? (listRaw as QnaQuestion[]) : [];
      const sorted = [...list].sort((a, b) => getSortTime(b) - getSortTime(a));
      setQuestions(sorted);

      if (sorted.length > 0) {
        const firstIdRaw = (sorted[0] as any)?.id || (sorted[0] as any)?._id;
        if (firstIdRaw) {
          const idStr = String(firstIdRaw);
          prevReadAtRef.current = readAtById[idStr] ?? null;
          setCurrentId(idStr);
          setQuerySafe({ academy: String(academyNo), questionId: idStr, recent: undefined });
          await reloadThread(idStr);
        }
      } else {
        // 질문 없으면 오른쪽 비우고 안내 유지
        setCurrentId(null);
        setQuestion(null);
        setAnswers([]);
        prevReadAtRef.current = null;
        prevOpenedIdRef.current = null;
      }
    } catch (e: any) {
      setError(e?.message ?? "학원 전환 중 오류가 발생했습니다.");
    } finally {
      const remain = MIN_SPIN_MS - (Date.now() - started);
      if (remain > 0) await new Promise((r) => setTimeout(r, remain));
      setSwitching(false);
    }
  }

  // 전송
  async function handleSend() {
    const qid = (question as any)?._id || (question as any)?.id;
    if (!(input.trim().length > 0 && qid)) return;
    try {
      const text = input.trim();
      setInput("");

      await apiPostAnswer(String(qid), text);

      const [q, latestAns, latestFu] = await Promise.all([
        apiGetQuestion(String(qid)),
        apiGetAnswers(String(qid)),
        getFollowupsFlexible(String(qid)),
      ]);
      setQuestion({ ...(q as QnaQuestion), followups: Array.isArray(latestFu) ? latestFu : [] });
      setAnswers(Array.isArray(latestAns) ? (latestAns as QnaAnswer[]) : []);

      await refreshGlobalAndList(selectedAcademy, academies);

      requestAnimationFrame(() => {
        const el = document.querySelector<HTMLDivElement>("#chatbox");
        if (el) el.scrollTop = el.scrollHeight;
      });
    } catch (e: any) {
      const msg = String(e?.message ?? "");
      if (/^AUTH_(401|403)/.test(msg) || /^(401|403)\b/.test(msg) || /Unauthorized|Forbidden/i.test(msg)) {
        router.push("/login?next=/qna");
        return;
      }
      setError(e?.message ?? "전송에 실패했습니다.");
    }
  }
}
