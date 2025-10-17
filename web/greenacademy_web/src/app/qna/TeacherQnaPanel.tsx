// src/app/qna/TeacherQnaPanel.tsx
"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { getSavedSession } from "@/lib/api";
import {
  listQuestions,
  getQuestion as apiGetQuestion,
  getAnswers as apiGetAnswers,
  postAnswer as apiPostAnswer,
  markQuestionRead as apiMarkQuestionRead,
  getFollowupsFlexible,
} from "@/lib/qna";

type IdLike = { id?: string; _id?: string };

type QnaQuestion = IdLike & {
  title?: string;
  content?: string;
  authorRole?: string;
  createdAt?: string;
  unreadCount?: number;   // 서버가 내려주는 미확인 개수(선택)
  followups?: any[];
  academyNumber?: number;
  lastReadAt?: string;    // 서버가 내려주는 마지막 읽음 시각(선택)
};

type QnaAnswer = IdLike & {
  content?: string;
  authorRole?: string; // "teacher" | "director"
  teacherName?: string;
  createdAt?: string;
};

type IntervalHandle = ReturnType<typeof setInterval> | null;

// 역할 정규화
function normalizeRole(raw?: unknown) {
  const s = String(raw ?? "").toLowerCase();
  if (s.includes("teacher")) return "teacher";
  if (s.includes("director")) return "director";
  if (s.includes("parent")) return "parent";
  return "student";
}

export default function TeacherQnaPanel() {
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [switching, setSwitching] = useState(false);

  const [academies, setAcademies] = useState<number[]>([]);
  const [selectedAcademy, setSelectedAcademy] = useState<number | null>(null);

  const [questions, setQuestions] = useState<QnaQuestion[]>([]);
  const [currentId, setCurrentId] = useState<string | null>(null);

  const [question, setQuestion] = useState<QnaQuestion | null>(null);
  const [answers, setAnswers] = useState<QnaAnswer[]>([]);
  const [input, setInput] = useState("");
  const [error, setError] = useState<string | null>(null);

  const pollRef = useRef<IntervalHandle>(null);

  // ✅ 미확인 표시 기준 시각
  // - 서버 lastReadAt이 있으면 우선 사용
  // - 없으면 화면 진입 시각을 fallback
  const pageEnterAtRef = useRef<string>(new Date().toISOString());
  const lastSeenRef = useRef<string | null>(null);

  // ✅ 채팅 컨테이너 ref + 자동 스크롤 함수
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

  // 스레드 새로고침 (질문/답변/후속질문 모두)
  const reloadThread = async (rootId: string) => {
    const [q, a, fu] = await Promise.all([
      apiGetQuestion(rootId),
      apiGetAnswers(rootId),
      getFollowupsFlexible(rootId),
    ]);
    const qCast = q as QnaQuestion;

    // ★ lastSeen 기준 설정
    const serverLastRead = (qCast as any)?.lastReadAt;
    if (typeof serverLastRead === "string" && serverLastRead.trim().length > 0) {
      lastSeenRef.current = serverLastRead;
    } else if (!lastSeenRef.current) {
      lastSeenRef.current = pageEnterAtRef.current;
    }

    setQuestion({ ...qCast, followups: Array.isArray(fu) ? fu : [] });
    setAnswers(Array.isArray(a) ? (a as QnaAnswer[]) : []);

    // 읽음 처리(서버만). 현재 화면 기준 lastSeenRef는 덮어쓰지 않음 → 이번 렌더에서 빨간점 표시 가능
    try {
      await apiMarkQuestionRead(rootId);
    } catch {
      /* ignore */
    }

    // 새 스레드 로딩 후 하단으로
    scrollToBottom();
  };

  // 질문 열기
  const openQuestion = async (id: string) => {
    await reloadThread(id);

    // 폴링(답변 + 후속질문)
    if (pollRef.current) clearInterval(pollRef.current);
    pollRef.current = setInterval(async () => {
      try {
        const [latestAns, latestFu] = await Promise.all([
          apiGetAnswers(id),
          getFollowupsFlexible(id),
        ]);
        let changed = false;
        if (Array.isArray(latestAns)) {
          setAnswers(latestAns as QnaAnswer[]);
          changed = true;
        }
        setQuestion((prev) => {
          if (!prev) return prev;
          changed = true;
          return { ...prev, followups: Array.isArray(latestFu) ? latestFu : [] };
        });
        if (changed) scrollToBottom();
      } catch {
        /* ignore */
      }
    }, 5000);
  };

  // 부팅
  const bootstrap = async () => {
    try {
      setLoading(true);
      setError(null);

      const session: any = getSavedSession?.();
      if (!session?.token) {
        router.push("/login?next=/qna");
        return;
      }

      // 역할 체크
      const role = normalizeRole(session?.role);
      if (!(role === "teacher" || role === "director")) {
        setError("교사/원장만 접근할 수 있습니다.");
        return;
      }

      // 학원 목록
      const acads = (session?.academyNumbers || []).filter((n: any) => Number.isFinite(n)) as number[];
      if (acads.length === 0) {
        setError("소속된 학원이 없습니다.");
        return;
      }
      setAcademies(acads);
      const initialAcademy = acads[0];
      setSelectedAcademy(initialAcademy);

      // 질문 목록
      const qs = await listQuestions(initialAcademy);
      const arr = Array.isArray(qs) ? (qs as QnaQuestion[]) : [];
      setQuestions(arr);

      if (arr.length > 0) {
        const firstId = (arr[0]._id || arr[0].id) as string | undefined;
        if (firstId) {
          setCurrentId(firstId);
          await openQuestion(firstId);
        }
      } else {
        setQuestion(null);
        setAnswers([]);
        setError("표시할 질문이 없습니다. (학원을 변경해 보세요)");
      }
    } catch (e: any) {
      const msg = String(e?.message ?? "");
      if (/^AUTH_(401|403)/.test(msg) || /^(401|403)\b/.test(msg) || /Unauthorized|Forbidden/i.test(msg)) {
        router.push("/login?next=/qna");
        return;
      }
      setError(e?.message ?? "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    bootstrap();
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 학원 전환
  const onChangeAcademy = async (academyNo: number) => {
    if (!Number.isFinite(academyNo) || academyNo === selectedAcademy) return;
    try {
      setSwitching(true);
      setError(null);
      setSelectedAcademy(academyNo);

      const qs = await listQuestions(academyNo);
      const arr = Array.isArray(qs) ? (qs as QnaQuestion[]) : [];
      setQuestions(arr);

      const firstId = (arr[0]?._id || arr[0]?.id) as string | undefined;
      setCurrentId(firstId ?? null);

      if (firstId) {
        await openQuestion(firstId);
      } else {
        setQuestion(null);
        setAnswers([]);
        setError("표시할 질문이 없습니다.");
      }
    } catch (e: any) {
      setError(e?.message ?? "학원 전환 중 오류가 발생했습니다.");
    } finally {
      setSwitching(false);
    }
  };

  // 전송(교사/원장은 답변 전송)
  const handleSend = async () => {
    if (!canSend || !qid) return;
    try {
      const text = input.trim();
      setInput("");

      await apiPostAnswer(String(qid), text);

      // 답변/질문 동시 갱신
      const [q, latestAns, latestFu] = await Promise.all([
        apiGetQuestion(String(qid)),
        apiGetAnswers(String(qid)),
        getFollowupsFlexible(String(qid)),
      ]);
      setQuestion({ ...(q as QnaQuestion), followups: Array.isArray(latestFu) ? latestFu : [] });
      setAnswers(Array.isArray(latestAns) ? (latestAns as QnaAnswer[]) : []);

      await apiMarkQuestionRead(String(qid));

      // 내가 보낸 직후에도 하단 고정
      scrollToBottom();
    } catch (e: any) {
      const msg = String(e?.message ?? "");
      if (/^AUTH_(401|403)/.test(msg) || /^(401|403)\b/.test(msg) || /Unauthorized|Forbidden/i.test(msg)) {
        router.push("/login?next=/qna");
        return;
      }
      setError(e?.message ?? "전송에 실패했습니다.");
    }
  };

  // 채팅 메시지(학생/학부모 = 왼쪽, 교사/원장 = 오른쪽)
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

    // 메인 질문(학생/학부모)
    const mainText = typeof qany?.content === "string" ? qany.content.trim() : "";
    if (mainText) {
      msgs.push({
        _id: qany?._id || qany?.id || "question",
        side: "student",
        text: mainText,
        createdAt: qany?.createdAt,
        meta: "student",
      });
    }

    // 후속 질문(학생/학부모)
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
          meta: "student",
        });
      }
    }

    // 교사/원장 답변
    for (const a of answers) {
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

    // 시간순 정렬
    msgs.sort((m1, m2) => {
      const t1 = m1.createdAt ? +new Date(m1.createdAt) : 0;
      const t2 = m2.createdAt ? +new Date(m2.createdAt) : 0;
      return t1 - t2;
    });
    return msgs;
  })();

  // ✅ 데이터가 바뀔 때마다 하단으로 스크롤 보장
  useEffect(() => {
    scrollToBottom();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [question?._id, answers.length, (question as any)?.followups?.length]);

  // --- 렌더 ---
  if (loading) return <div className="rounded-2xl p-6 border shadow-sm text-sm text-gray-500">불러오는 중…</div>;
  if (error) return <div className="rounded-2xl p-6 border shadow-sm text-sm text-red-600">오류: {error}</div>;

  return (
    <div className="rounded-2xl p-0 border shadow-sm overflow-hidden relative">
      {/* 상단 바 */}
      <div className="px-5 py-4 border-b bg-gray-50 flex items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          {/* ▶ 타이틀 더 진하게 */}
          <div className="text-lg font-semibold text-gray-900">Q&amp;A (교사/원장)</div>

          {/* 학원 선택 */}
          <select
            className="h-8 rounded-lg border px-2 text-sm bg-white text-gray-900"
            value={selectedAcademy ?? ""}
            onChange={(e) => onChangeAcademy(Number(e.target.value))}
          >
            {academies.map((n) => (
              <option key={n} value={n}>
                학원 #{n}
              </option>
            ))}
          </select>
        </div>

        {/* ▶ 항상 표시, 진하게, 작은 배지로 감싸기 */}
        <div className="px-2 py-1 rounded-md border border-gray-300 bg-gray-100 text-[11px] font-semibold text-gray-900">
          미확인 메시지: {typeof question?.unreadCount === "number" ? question.unreadCount : 0}
        </div>
      </div>

      {/* ▶ 전환 스피너 : 검정색으로 또렷하게 */}
      {switching && (
        <div className="absolute inset-0 bg-white/60 backdrop-blur-[1px] flex items-center justify-center z-10">
          <div className="flex items-center gap-2 text-sm text-black">
            <svg className="animate-spin h-5 w-5 text-black" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4A4 4 0 008 12H4z" />
            </svg>
            전환 중…
          </div>
        </div>
      )}

      {/* 본문: 왼쪽(질문 목록) / 오른쪽(대화) */}
      <div className="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-4 p-4">
        {/* 왼쪽: 질문 목록 */}
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
          <div className="text-sm font-semibold text-gray-900 mb-3">질문 목록</div>

          {questions.length === 0 ? (
            <div className="text-sm text-gray-600">표시할 질문이 없습니다.</div>
          ) : (
            <ul className="divide-y">
              {questions.map((q) => {
                const id = (q as any)?.id || (q as any)?._id;
                return (
                  <li key={id}>
                    <button
                      onClick={async () => {
                        if (!id) return;
                        setCurrentId(String(id));
                        await openQuestion(String(id));
                      }}
                      className={`w-full text-left px-2 py-2 text-sm ${currentId === id ? "bg-gray-50" : ""}`}
                    >
                      <div className="font-medium text-gray-900 truncate">
                        {q.title ?? "(제목 없음)"}
                      </div>
                      <div className="text-xs text-gray-600 flex items-center gap-1">
                        <span>#{q.academyNumber ?? selectedAcademy}</span>
                        <span>·</span>
                        <span>{q.createdAt ?? ""}</span>
                        {typeof q.unreadCount === "number" && q.unreadCount > 0 && (
                          <span className="ml-1 inline-block rounded-full bg-emerald-100 text-emerald-700 px-1.5 py-[1px] text-[10px]">
                            +{q.unreadCount}
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

        {/* 오른쪽: 채팅 */}
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm overflow-hidden">
          <div
            ref={chatBoxRef}
            className="h-[420px] overflow-y-auto px-5 py-4 space-y-3"
          >
            {chatMessages.length === 0 ? (
              <div className="text-sm text-gray-500">표시할 메시지가 없습니다.</div>
            ) : (
              (() => {
                // 학생/학부모 메시지, 교사/원장 메시지 분리
                const studentMsgs = chatMessages.filter(x => x.side === "student");
                const myMsgs = chatMessages.filter(x => x.side === "teacher");

                const lastSeen =
                  (question as any)?.lastReadAt || lastSeenRef.current || null;

                const unreadCount =
                  typeof question?.unreadCount === "number" ? question.unreadCount : 0;

                // 내가 마지막으로 보낸(교사/원장) 메시지 시각
                const myLastMsgAt = (() => {
                  const ts = myMsgs
                    .map(m => (m.createdAt ? new Date(m.createdAt).getTime() : 0))
                    .filter(n => Number.isFinite(n) && n > 0);
                  return ts.length ? Math.max(...ts) : null;
                })();

                return chatMessages.map((m) => {
                  // 1) lastSeen 이후 도착한 학생/학부모 메시지
                  const byTime =
                    !!lastSeen &&
                    m.side === "student" &&
                    !!m.createdAt &&
                    new Date(m.createdAt).getTime() > new Date(lastSeen).getTime();

                  // 2) lastSeen 없고 unreadCount만 있을 때 → 최신 unreadCount개의 학생 메시지
                  const idxInStudent = studentMsgs.findIndex(x => x._id === m._id);
                  const byCount =
                    !lastSeen &&
                    m.side === "student" &&
                    unreadCount > 0 &&
                    idxInStudent >= 0 &&
                    idxInStudent >= studentMsgs.length - unreadCount;

                  // 3) 둘 다 없으면 화면 진입 이후 도착한 학생 메시지
                  const byEnterTime =
                    !lastSeen &&
                    unreadCount === 0 &&
                    m.side === "student" &&
                    !!m.createdAt &&
                    new Date(m.createdAt).getTime() > new Date(pageEnterAtRef.current).getTime();

                  // 4) 나의 마지막 답변 이후에 온 학생 메시지
                  const byMyLast =
                    !lastSeen &&
                    unreadCount === 0 &&
                    myLastMsgAt !== null &&
                    m.side === "student" &&
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
                        {isUnread && m.side === "student" && (
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

          {/* ▶ 입력창: 텍스트/플레이스홀더/테두리 더 진하게 */}
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
      </div>
    </div>
  );
}
