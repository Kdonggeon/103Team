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

// setInterval 타입 안전
type IntervalHandle = ReturnType<typeof setInterval> | null;

export default function QnaPanel({
  academyNumber,
  role,
}: {
  academyNumber?: number;
  role?: "student" | "parent";
}) {
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

  const pollRef = useRef<IntervalHandle>(null);

  // ✅ 미확인 표시 기준 시각
  const pageEnterAtRef = useRef<string>(new Date().toISOString()); // 화면 진입 시각(fallback)
  const lastSeenRef = useRef<string | null>(null);                  // 서버 lastReadAt 우선

  // ✅ 채팅 컨테이너 ref + 자동 스크롤
  const chatBoxRef = useRef<HTMLDivElement | null>(null);
  const scrollToBottom = () => {
    const el = chatBoxRef.current;
    if (!el) return;
    // 렌더 직후 안전하게 한 프레임 뒤에 스크롤
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

    // 서버 lastReadAt 우선, 없으면 진입시각
    const serverLastRead = (q as any)?.lastReadAt;
    if (typeof serverLastRead === "string" && serverLastRead.trim().length > 0) {
      lastSeenRef.current = serverLastRead;
    } else if (!lastSeenRef.current) {
      lastSeenRef.current = pageEnterAtRef.current;
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

  // 선택된 학원으로 방 열기
  const openRoomForAcademy = async (academyNo: number) => {
    const session: any = getSavedSession();
    const rawRole = String(role ?? session?.role ?? "student").toLowerCase();
    const er: "student" | "parent" = rawRole === "parent" || rawRole === "parents" ? "parent" : "student";
    setEffectiveRole(er);

    const room: QnaId =
      er === "parent" ? await getOrCreateParentRoom(academyNo) : await getOrCreateStudentRoom(academyNo);
    const id = (room as any)?._id || (room as any)?.id;
    if (!id) throw new Error("Q&A 방을 찾거나 생성하지 못했습니다.");
    await reloadThread(String(id));

    // 폴링 재설정(답변만)
    if (pollRef.current) clearInterval(pollRef.current);
    pollRef.current = setInterval(async () => {
      try {
        const latest = (await apiGetAnswers(String(id))) as QnaAnswer[];
        if (Array.isArray(latest)) {
          setAnswers(latest);
          scrollToBottom();
        }
      } catch {
        /* ignore */
      }
    }, 5000);
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

      const list: number[] =
        Number.isFinite(academyNumber as number) ? [Number(academyNumber)] : [];
      const merged = Array.from(
        new Set([...(session?.academyNumbers || []), ...list].filter((n: any) => Number.isFinite(n)))
      ) as number[];

      if (merged.length === 0) {
        setError("접근 가능한 학원이 없습니다.");
        return;
      }
      setAcademies(merged);

      const initial = academyNumber && Number.isFinite(academyNumber)
        ? Number(academyNumber)
        : merged[0];
      setSelectedAcademy(initial);

      await openRoomForAcademy(initial);
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
    if (!canSend || !selectedAcademy) return;
    try {
      const text = input.trim();
      setInput("");
      if (!qid) return;

      if (effectiveRole === "student" || effectiveRole === "parent") {
        await postFollowupFlexible(String(qid), text, Number(selectedAcademy));
        await reloadThread(String(qid));
      } else {
        await apiPostAnswer(String(qid), text);
        const latest = (await apiGetAnswers(String(qid))) as QnaAnswer[];
        if (Array.isArray(latest)) setAnswers(latest);
        await apiMarkQuestionRead(String(qid));
      }

      // 내가 보낸 직후에도 하단 고정
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

  return (
    <div className="rounded-2xl p-0 border shadow-sm overflow-hidden relative">
      {/* 상단 바 */}
      <div className="px-5 py-4 border-b bg-gray-50 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          {/* ▶ 제목 더 진하게 */}
          <div className="text-lg font-semibold text-gray-900">Q&amp;A</div>
          {/* 학원 스피너 + 셀렉터 */}
          <div className="flex items-center gap-2 ml-3">
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
              value={selectedAcademy ?? ""}
              onChange={(e) => {
                const next = Number(e.target.value);
                if (Number.isFinite(next)) switchAcademy(next);
              }}
            >
              {academies.map((n) => (
                <option key={n} value={n}>
                  학원 #{n}
                </option>
              ))}
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
        </div>

        {/* ▶ 항상 표시: 작은 배지, 진하게 */}
        <div className="px-2 py-1 rounded-md border border-gray-300 bg-gray-100 text-[11px] font-semibold text-gray-900">
          미확인 답변: {typeof question?.unreadCount === "number" ? question.unreadCount : 0}
        </div>
      </div>

      {/* ▶ 전환 스피너 오버레이(더 진하게) */}
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
            // 교사/원장, 본인(학생/학부모) 메시지 분리
            const teacherMsgs = chatMessages.filter(x => x.side === "teacher");
            const myMsgs = chatMessages.filter(x => x.side === "student");

            const lastSeen =
              (question as any)?.lastReadAt || lastSeenRef.current || null;

            const unreadCount =
              typeof question?.unreadCount === "number" ? question.unreadCount : 0;

            // 폴백: 내 마지막 발화 시각
            const myLastMsgAt = (() => {
              const ts = myMsgs
                .map(m => (m.createdAt ? new Date(m.createdAt).getTime() : 0))
                .filter(n => Number.isFinite(n) && n > 0);
              return ts.length ? Math.max(...ts) : null;
            })();

            return chatMessages.map((m) => {
              // 1) 기준시각 이후(정상 케이스)
              const byTime =
                !!lastSeen &&
                m.side === "teacher" &&
                !!m.createdAt &&
                new Date(m.createdAt).getTime() > new Date(lastSeen).getTime();

              // 2) 폴백 A: lastSeen 없고 unreadCount만 있을 때 → 최신 unreadCount개의 교사 메시지
              const idxInTeacher = teacherMsgs.findIndex(x => x._id === m._id);
              const byCount =
                !lastSeen &&
                m.side === "teacher" &&
                unreadCount > 0 &&
                idxInTeacher >= 0 &&
                idxInTeacher >= teacherMsgs.length - unreadCount;

              // 3) 폴백 B: 둘 다 없으면 화면 진입 이후 도착분
              const byEnterTime =
                !lastSeen &&
                unreadCount === 0 &&
                m.side === "teacher" &&
                !!m.createdAt &&
                new Date(m.createdAt).getTime() > new Date(pageEnterAtRef.current).getTime();

              // 4) 폴백 C: 내 마지막 발화 이후 도착한 교사/원장 메시지
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

                  {/* 시간 줄 (이름 앞에 빨간점) */}
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

      {/* 입력창 (더 진하게) */}
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
