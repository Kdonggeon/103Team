// src/app/qna/QnaPanel.tsx
"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { getSavedSession } from "@/lib/api";
import {
  getOrCreateStudentRoom,
  getOrCreateParentRoom,
  getQuestion as apiGetQuestion,
  getAnswers as apiGetAnswers,
  postAnswer as apiPostAnswer,
  markQuestionRead as apiMarkQuestionRead,
  getFollowupsFlexible,
  postFollowupFlexible,
} from "@/lib/qna";
import type { QnaQuestion as QType, QnaAnswer as AType, QnaId } from "@/lib/qna";

type IdLike = { id?: string; _id?: string };
type QnaQuestion = IdLike & QType;
type QnaAnswer = IdLike & AType;

type QnaPanelProps = {
  academyNumber: number;
  role: "student" | "parent";
  /** 선택: 특정 질문을 강제로 열 때 전달 */
  questionId?: string;
};

// setInterval 타입 안전
type IntervalHandle = ReturnType<typeof setInterval> | null;

// 🔹 API BASE (학부모 자녀 이름 조회용)
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "";

// 🔹 공용 GET 유틸
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

export default function QnaPanel({
  academyNumber,
  role,
  questionId,
}: QnaPanelProps) {
  const router = useRouter();

  // --- 상태 ---
  const [loading, setLoading] = useState(true);
  const [switching, setSwitching] = useState(false);
  const [question, setQuestion] = useState<QnaQuestion | null>(null);
  const [answers, setAnswers] = useState<QnaAnswer[]>([]);
  const [followupQs, setFollowupQs] = useState<any[]>([]);
  const [input, setInput] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [effectiveRole, setEffectiveRole] = useState<"student" | "parent">("student");
  const [displayName, setDisplayName] = useState<string>("student");
  const [academies, setAcademies] = useState<number[]>([]);
  const [selectedAcademy, setSelectedAcademy] = useState<number | null>(null);

  // 🔹 학부모 전용: 부모/자녀 이름 표기용
  const [parentChildLabel, setParentChildLabel] = useState<string | null>(null);

  const pollRef = useRef<IntervalHandle>(null);

  // ✅ 미확인 표시 기준 시각
  const pageEnterAtRef = useRef<string>(new Date().toISOString()); // 화면 진입 시각(fallback)
  const lastSeenRef = useRef<string | null>(null);                  // 서버 lastReadAt 우선

  // ✅ 채팅 컨테이너 ref + 자동 스크롤
  const chatBoxRef = useRef<HTMLDivElement | null>(null);
  const scrollToBottom = () => {
    const el = chatBoxRef.current;
    if (!el) return;
    requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight;
    });
  };

  const qid = question?._id || question?.id;
  const canSend = useMemo(() => input.trim().length > 0 && !!qid, [input, qid]);

  // 공통 새로고침 (질문/답변/팔로우업 재조회)
  const reloadThread = async (rootId: string) => {
    const q = (await apiGetQuestion(rootId)) as QnaQuestion;
    setQuestion(q);

    // ✅ 질문의 학원번호로 스피너/학원 동기화 (최근 QnA 바로가기 대응)
    const qAcad = (q as any)?.academyNumber;
    if (Number.isFinite(qAcad)) {
      setSelectedAcademy((prev) => (prev === qAcad ? prev : qAcad));
      setAcademies((prev) => (prev.includes(qAcad) ? prev : [...prev, qAcad]));
    }

    // 서버 lastReadAt 우선, 없으면 null(다른 휴리스틱 가동)
    const serverLastRead = (q as any)?.lastReadAt;
    if (typeof serverLastRead === "string" && serverLastRead.trim().length > 0) {
      lastSeenRef.current = serverLastRead;  // 서버 값이 있으면 사용
    } else {
      lastSeenRef.current = null;            // 없으면 null로 둠
    }

    const a = (await apiGetAnswers(rootId)) as QnaAnswer[];
    setAnswers(Array.isArray(a) ? a : []);

    const rawFollowups: any[] = Array.isArray((q as any)?.followups) ? (q as any).followups : [];
    const hasInlineContent = rawFollowups.some(
      (f) => typeof f?.content === "string" && f.content.trim().length > 0
    );
    const resolvedFollowups = hasInlineContent ? rawFollowups : await getFollowupsFlexible(String(rootId));
    setFollowupQs(resolvedFollowups);

    // 읽음 처리(서버만), 현재 화면의 lastSeenRef는 덮어쓰지 않음
    try {
      await apiMarkQuestionRead(rootId);
    } catch {
      /* ignore */
    }

    // 새 스레드 로드 후 하단으로
    scrollToBottom();
  };

  // ✅ 폴링 시작/갱신
  const startPolling = (rootId: string) => {
    if (pollRef.current) clearInterval(pollRef.current);
    pollRef.current = setInterval(async () => {
      try {
        const latest = (await apiGetAnswers(String(rootId))) as QnaAnswer[];
        if (Array.isArray(latest)) {
          setAnswers(latest);
          scrollToBottom();
        }
      } catch {
        /* ignore */
      }
    }, 5000);
  };

  // 선택된 학원으로 방 열기(질문방 생성/조회 후 로드)
  const openRoomForAcademy = async (academyNo: number) => {
    // 폴링 먼저 정리(전환 타이밍 꼬임 방지)
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }

    const session: any = getSavedSession();
    const rawRole = String(role ?? session?.role ?? "student").toLowerCase();
    const er: "student" | "parent" = rawRole === "parent" || rawRole === "parents" ? "parent" : "student";
    setEffectiveRole(er);

    const room: QnaId =
      er === "parent" ? await getOrCreateParentRoom(academyNo) : await getOrCreateStudentRoom(academyNo);
    const id = (room as any)?._id || (room as any)?.id;
    if (!id) throw new Error("Q&A 방을 찾거나 생성하지 못했습니다.");

    await reloadThread(String(id));
    startPolling(String(id));
  };

  // 🔹 학부모: 부모/자녀 이름 라벨 세팅
  async function initParentChildLabel(session: any) {
    try {
      const parentName =
        (session?.name && String(session.name)) ||
        (session?.username && String(session.username)) ||
        "parent";

      const childId = session?.childStudentId || null;
      if (!childId) {
        setParentChildLabel(`${parentName}(학부모)`);
        return;
      }
      const profile = await apiGet<any>(`${API_BASE}/api/students/${encodeURIComponent(childId)}`, session?.token);
      const childName =
        (typeof profile?.name === "string" && profile.name) ||
        (typeof profile?.studentName === "string" && profile.studentName) ||
        childId;

      setParentChildLabel(`${parentName}(학부모) · ${childName}(자녀)`);
    } catch {
      // 실패해도 부모 이름만이라도 표기
      const s = getSavedSession();
      const parentName =
        (s?.name && String(s.name)) ||
        (s?.username && String(s.username)) ||
        "parent";
      setParentChildLabel(`${parentName}(학부모)`);
    }
  }

  // ====== 🔴 “최근 QnA 바로가기” 기준: 미확인(unread) 우선, 없으면 최신 ======
  type RoomSummary = {
    academyNumber: number;
    id: string;
    unreadCount: number;
    updatedAt: number; // ms (updatedAt || createdAt)
  };

  const fetchRoomSummary = async (academyNo: number): Promise<RoomSummary | null> => {
    try {
      const session: any = getSavedSession();
      const rawRole = String(role ?? session?.role ?? "student").toLowerCase();
      const er: "student" | "parent" = rawRole === "parent" || rawRole === "parents" ? "parent" : "student";
      const room: any =
        er === "parent"
          ? await getOrCreateParentRoom(academyNo)
          : await getOrCreateStudentRoom(academyNo);

      const id: string | undefined = room?._id || room?.id;
      if (!id) return null;

      const t =
        (typeof room?.updatedAt === "string" && +new Date(room.updatedAt)) ||
        (typeof room?.createdAt === "string" && +new Date(room.createdAt)) ||
        0;

      const unread = typeof room?.unreadCount === "number" ? room.unreadCount : 0;

      return {
        academyNumber: academyNo,
        id,
        unreadCount: unread,
        updatedAt: t,
      };
    } catch {
      return null;
    }
  };

  const pickBestRoom = (items: RoomSummary[]): RoomSummary | null => {
    if (!items.length) return null;
    const unread = items.filter(i => (i.unreadCount ?? 0) > 0);
    const base = unread.length ? unread : items;
    return [...base].sort((a, b) => (b.updatedAt - a.updatedAt))[0];
  };

  // 최초 부팅
  async function bootstrap() {
    try {
      setLoading(true);
      setError(null);

      const session: any = getSavedSession();
      if (!session?.token) {
        router.push("/login?next=/qna");
        return;
      }

      setDisplayName(
        (session?.name && String(session.name)) ||
          (session?.username && String(session.username)) ||
          "student"
      );

      // 🔹 학부모 라벨 준비
      const rawRole = String(role ?? session?.role ?? "student").toLowerCase();
      const er: "student" | "parent" = rawRole === "parent" || rawRole === "parents" ? "parent" : "student";
      setEffectiveRole(er);
      if (er === "parent") {
        initParentChildLabel(session);
      } else {
        setParentChildLabel(null);
      }

      // 사용자 보유 학원 + props로 받은 academyNumber를 병합(문자/숫자 혼용 대비)
      const extraList: number[] = Number.isFinite(academyNumber as number) ? [Number(academyNumber)] : [];
      const merged = Array.from(
        new Set([...(session?.academyNumbers || []), ...extraList]
          .map((n: any) => Number(n))
          .filter((n: number) => Number.isFinite(n)))
      ) as number[];
      setAcademies(merged);

      // ✅ questionId가 주어진 경우: 해당 스레드 즉시 오픈(권한 오류면 '내 방'으로 폴백)
      if (questionId) {
        try {
          await reloadThread(String(questionId));
          startPolling(String(questionId));
          return;
        } catch (e: any) {
          const msg = String(e?.message ?? "");
          if (msg.startsWith("AUTH_401") || msg.startsWith("AUTH_403")) {
            const raw = String(role ?? session?.role ?? "student").toLowerCase();
            const erole: "student" | "parent" = raw === "parent" || raw === "parents" ? "parent" : "student";
            const acad = Number.isFinite(academyNumber as number)
              ? Number(academyNumber)
              : (merged[0] ?? 0);
            const room: any =
              erole === "parent"
                ? await getOrCreateParentRoom(acad)
                : await getOrCreateStudentRoom(acad);
            const id = String(room?._id || room?.id || "");
            if (id) {
              await reloadThread(id);
              startPolling(id);
              return;
            }
          }
          // 그 외 오류는 아래 공통 흐름으로
        }
      }

      // ✅ “최근 QnA 바로가기” 기준: 미확인 우선 → 없으면 최신
      if (merged.length === 0) {
        setError("접근 가능한 학원이 없습니다.");
        return;
      }

      // 각 학원별 요약을 병렬 수집
      const settled = await Promise.allSettled(merged.map(n => fetchRoomSummary(n)));
      const summaries: RoomSummary[] = settled
        .map(x => (x.status === "fulfilled" ? x.value : null))
        .filter(Boolean) as RoomSummary[];

      // 최적 타겟 선정
      const best = pickBestRoom(summaries);
      const initialAcademy = best?.academyNumber ?? merged[0];
      setSelectedAcademy(initialAcademy);

      // 선정된 학원의 방 열기
      await openRoomForAcademy(initialAcademy);
    } catch (e: any) {
      const msg = String(e?.message ?? "");
      if (msg.startsWith("AUTH_401") || msg.startsWith("AUTH_403")) {
        router.push("/login?next=/qna");
        return;
      }
      setError(e?.message ?? "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    bootstrap();
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ✅ 최근 QnA 바로가기 등으로 questionId가 바뀔 때도 스레드/학원 동기화 (권한 오류 시 내 방 폴백)
  useEffect(() => {
    if (!questionId) return;
    (async () => {
      try {
        await reloadThread(String(questionId));
        startPolling(String(questionId));
      } catch (e: any) {
        const msg = String(e?.message ?? "");
        if (msg.startsWith("AUTH_401") || msg.startsWith("AUTH_403")) {
          const session: any = getSavedSession();
          const raw = String(role ?? session?.role ?? "student").toLowerCase();
          const erole: "student" | "parent" = raw === "parent" || raw === "parents" ? "parent" : "student";
          const acad = Number.isFinite(academyNumber as number)
            ? Number(academyNumber)
            : (Array.isArray(session?.academyNumbers) && session.academyNumbers.length
                ? Number(session.academyNumbers[0])
                : 0);
          const room: any =
            erole === "parent"
              ? await getOrCreateParentRoom(acad)
              : await getOrCreateStudentRoom(acad);
          const id = String(room?._id || room?.id || "");
          if (id) {
            await reloadThread(id);
            startPolling(id);
          }
        } else {
          setError(e?.message ?? "스레드를 불러오지 못했습니다.");
        }
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [questionId]);

  // 학원 전환 핸들러
  const switchAcademy = async (next: number) => {
    if (!Number.isFinite(next) || next === selectedAcademy) return;
    try {
      setSwitching(true);
      setError(null);
      setSelectedAcademy(next);
      await openRoomForAcademy(next);
    } catch (e: any) {
      setError(e?.message ?? "학원 전환 중 오류가 발생했습니다.");
    } finally {
      setSwitching(false);
    }
  };

  // ===== 채팅 메시지 구성: 학생(왼쪽) / 선생·원장(오른쪽) =====
  type ChatMsg = {
    _id?: string;
    side: "student" | "teacher";
    text: string;
    createdAt?: string;
    meta?: string;
  };

  const chatMessages: ChatMsg[] = (() => {
    const msgs: ChatMsg[] = [];
    const qany = question as any;

    // (1) 메인 질문
    const mainText = typeof qany?.content === "string" ? qany.content.trim() : "";
    if (mainText) {
      msgs.push({
        _id: qany?._id || qany?.id || "question",
        side: "student",
        text: mainText,
        createdAt: qany?.createdAt,
        meta: displayName,
      });
    }

    // (2) 후속 질문
    for (const fq of followupQs) {
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
          meta: displayName,
        });
      }
    }

    // (3) 답변
    for (const a of answers) {
      const text =
        (typeof a?.content === "string" && a.content.trim()) ||
        (typeof (a as any)?.message === "string" && (a as any).message.trim()) ||
        "";
      if (text) {
        const roleLower = String(a?.authorRole ?? "").toLowerCase();
        const label =
          a?.teacherName ? `by ${a.teacherName}` : roleLower === "director" ? "director" : "teacher";
        msgs.push({
          _id: (a as any)?._id || (a as any)?.id,
          side: "teacher",
          text,
          createdAt: a?.createdAt,
          meta: label,
        });
      }
    }

    // (4) 시간순 정렬
    msgs.sort((m1, m2) => {
      const t1 = m1.createdAt ? +new Date(m1.createdAt) : 0;
      const t2 = m2.createdAt ? +new Date(m2.createdAt) : 0;
      return t1 - t2;
    });

    return msgs;
  })();

  // ✅ 전송
  async function handleSend() {
    if (!canSend) return;
    try {
      const text = input.trim();
      setInput("");
      if (!qid) return;

      if (effectiveRole === "student" || effectiveRole === "parent") {
        // 학원번호는 백엔드가 필요 시만 사용하므로 optional로 전달
        await postFollowupFlexible(String(qid), text, selectedAcademy ?? undefined);
        await reloadThread(String(qid));
      } else {
        await apiPostAnswer(String(qid), text);
        const latest = (await apiGetAnswers(String(qid))) as QnaAnswer[];
        if (Array.isArray(latest)) setAnswers(latest);
        await apiMarkQuestionRead(String(qid));
      }

      scrollToBottom();
    } catch (e: any) {
      const msg = String(e?.message ?? "");
      if (msg.startsWith("AUTH_401") || msg.startsWith("AUTH_403")) {
        router.push("/login?next=/qna");
        return;
      }
      setError(e?.message ?? "전송에 실패했습니다.");
    }
  }

  // ✅ 질문/답변/후속질문이 바뀔 때마다 자동으로 하단으로
  useEffect(() => {
    scrollToBottom();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [question?._id, answers.length, followupQs.length]);

  // --- 렌더 ---
  if (loading)
    return <div className="rounded-2xl p-6 border shadow-sm text-sm text-gray-500">불러오는 중…</div>;
  if (error)
    return <div className="rounded-2xl p-6 border shadow-sm text-sm text-red-600">오류: {error}</div>;

  const idx = selectedAcademy ? academies.indexOf(selectedAcademy) : -1;
  const hasPrev = idx > 0;
  const hasNext = idx >= 0 && idx < academies.length - 1;

  // 🔹 헤더 왼쪽 정보 영역
  const leftInfo = (() => {
    const academyTag =
      typeof selectedAcademy === "number"
        ? `학원 #${selectedAcademy}`
        : (typeof (question as any)?.academyNumber === "number"
            ? `학원 #${(question as any).academyNumber}`
            : "");

    if (effectiveRole === "parent") {
      return [academyTag, parentChildLabel].filter(Boolean).join(" · ");
    }
    // student
    return academyTag;
  })();

  return (
    <div className="rounded-2xl p-0 border shadow-sm overflow-hidden relative">
      {/* 상단 바 */}
      <div className="px-5 py-4 border-b bg-gray-50 flex items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <div className="text-lg font-semibold text-gray-900 shrink-0">Q&amp;A</div>

          {/* 학원 스피너 + 셀렉터 */}
          <div className="flex items-center gap-2">
            <button
              className={`h-8 w-8 rounded-full border flex items-center justify-center text-base
    ${
      hasPrev
        ? "bg-white text-black border-gray-400 hover:bg-gray-100 hover:border-gray-700 shadow-sm"
        : "bg-gray-100 text-gray-400 border-gray-300 cursor-not-allowed"
    }`}
              disabled={!hasPrev}
              onClick={() => hasPrev && switchAcademy(academies[idx - 1])}
              aria-label="이전 학원"
              title="이전 학원"
            >
              ‹
            </button>
            <select
              className="h-8 rounded-lg border px-2 text-sm bg-white text-gray-900"
              value={selectedAcademy != null ? String(selectedAcademy) : ""}
              onChange={(e) => {
                const next = parseInt(e.target.value, 10);
                if (Number.isFinite(next)) switchAcademy(next);
              }}
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
            <button
              className={`h-8 w-8 rounded-full border flex items-center justify-center text-base
    ${
      hasNext
        ? "bg-white text-black border-gray-400 hover:bg-gray-100 hover:border-gray-700 shadow-sm"
        : "bg-gray-100 text-gray-400 border-gray-300 cursor-not-allowed"
    }`}
              disabled={!hasNext}
              onClick={() => hasNext && switchAcademy(academies[idx + 1])}
              aria-label="다음 학원"
              title="다음 학원"
            >
              ›
            </button>
          </div>

          {/* 역할별 추가 정보 뱃지 */}
          {leftInfo && (
            <span className="ml-1 inline-flex max-w-[50vw] items-center truncate gap-2 rounded-md px-2 py-1 text-xs font-semibold bg-white border border-gray-300 text-gray-900">
              {leftInfo}
            </span>
          )}
        </div>

        <div className="px-2 py-1 rounded-md border border-gray-300 bg-gray-100 text-[11px] font-semibold text-gray-900">
          미확인 답변: {typeof question?.unreadCount === "number" ? question.unreadCount : 0}
        </div>
      </div>

      {/* 전환 스피너 오버레이 */}
      {switching && (
        <div className="absolute inset-0 bg-white/60 backdrop-blur-[1px] flex items-center justify-center z-10">
          <div className="flex items-center gap-2 text-sm text-black">
            <svg className="animate-spin h-5 w-5 text-black" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8v4A4 4 0 008 12H4z"
              />
            </svg>
            학원 변경 중…
          </div>
        </div>
      )}

      {/* 채팅 */}
      <div
        ref={chatBoxRef}
        className="h-[420px] overflow-y-auto px-5 py-4 space-y-3"
      >
        {chatMessages.length === 0 ? (
          <div className="text-sm text-gray-500">표시할 메시지가 없습니다.</div>
        ) : (
          (() => {
            const teacherMsgs = chatMessages.filter(x => x.side === "teacher");
            const myMsgs = chatMessages.filter(x => x.side === "student");

            const lastSeen =
              (question as any)?.lastReadAt || lastSeenRef.current || null;

            const unreadCount =
              typeof question?.unreadCount === "number" ? question.unreadCount : 0;

            const myLastMsgAt = (() => {
              const ts = myMsgs
                .map(m => (m.createdAt ? new Date(m.createdAt).getTime() : 0))
                .filter(n => Number.isFinite(n) && n > 0);
              return ts.length ? Math.max(...ts) : null;
            })();

            return chatMessages.map((m) => {
              const byTime =
                !!lastSeen &&
                m.side === "teacher" &&
                !!m.createdAt &&
                new Date(m.createdAt).getTime() > new Date(lastSeen).getTime();

              const idxInTeacher = teacherMsgs.findIndex(x => x._id === m._id);
              const byCount =
                !lastSeen &&
                m.side === "teacher" &&
                unreadCount > 0 &&
                idxInTeacher >= 0 &&
                idxInTeacher >= teacherMsgs.length - unreadCount;

              const byEnterTime =
                !lastSeen &&
                unreadCount === 0 &&
                m.side === "teacher" &&
                !!m.createdAt &&
                new Date(m.createdAt).getTime() > new Date(pageEnterAtRef.current).getTime();

              const byMyLast =
                !lastSeen &&
                unreadCount === 0 &&
                myLastMsgAt !== null &&
                m.side === "teacher" &&
                !!m.createdAt &&
                new Date(m.createdAt).getTime() > myLastMsgAt;

              const isUnread = byTime || byCount || byEnterTime || byMyLast;

              return (
                <div
                  key={m._id}
                  className={`max-w-[80%] rounded-2xl px-4 py-2 text-sm whitespace-pre-wrap ${
                    m.side === "teacher"
                      ? "ml-auto bg-blue-50 border border-blue-200"
                      : "mr-auto bg-white border"
                  }`}
                >
                  <div className="text-gray-800">{m.text}</div>
                  <div className="text-[11px] text-gray-400 mt-1 flex items-center gap-1">
                    {isUnread && (
                      <span
                        className="inline-block mr-2 h-2.5 w-2.5 rounded-full bg-red-500 align-middle"
                        aria-label="미확인 메시지"
                        title="미확인 메시지"
                      />
                    )}
                    <span>{m.meta}</span>
                    <span>·</span>
                    <span>{m.createdAt ? new Date(m.createdAt).toLocaleString() : ""}</span>
                  </div>
                </div>
              );
            });
          })()
        )}
      </div>

      {/* 입력창 */}
      <div className="p-4 border-t bg-white">
        <div className="flex gap-2">
          <input
            className="flex-1 rounded-xl border border-gray-400 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-gray-900 text-black placeholder-black/70"
            placeholder="메시지를 입력하세요…"
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
              canSend ? "bg-black text-white" : "bg-gray-200 text-gray-600 cursor-not-allowed"
            }`}
            onClick={handleSend}
            disabled={!canSend}
          >
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
