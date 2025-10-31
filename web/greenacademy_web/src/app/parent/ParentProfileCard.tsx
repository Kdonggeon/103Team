// src/app/parent/ParentProfileCard.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";

/** ===== 공통 타입 ===== */
type Role = "parent" | "student" | "teacher" | "director";

type LoginSession = {
  role: Role;
  username: string;
  name?: string;
  token?: string;
  academyNumbers?: number[];
};

type ParentInfo = {
  parentsId: string;
  parentsName?: string | null;
  parentsPhoneNumber?: string | null;
  academyNumbers?: number[];
};

type ChildSummary = {
  studentId: string;
  studentName?: string | null;
  academies?: number[];
};

/** 자녀 상세(백엔드 응답 그대로 받음) */
type StudentDetail = Record<string, any>;

/** ===== API 유틸 ===== */
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "/backend";

async function api<T>(path: string, opts?: RequestInit & { token?: string }): Promise<T> {
  const { token, ...rest } = opts || {};
  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(rest.headers || {}),
    },
    cache: "no-store",
  });
  if (!res.ok) {
    const errText = await res.text().catch(() => "");
    throw new Error(`${res.status} ${res.statusText}${errText ? ` | ${errText}` : ""}`);
  }
  const text = await res.text().catch(() => "");
  if (!text) return undefined as unknown as T;
  try { return JSON.parse(text) as T; } catch { return undefined as unknown as T; }
}

function readLogin(): LoginSession | null {
  try {
    const raw = localStorage.getItem("login");
    return raw ? JSON.parse(raw) : null;
  } catch { return null; }
}
function writeLogin(next: LoginSession) {
  try { localStorage.setItem("login", JSON.stringify(next)); } catch {}
  try { window.dispatchEvent(new Event("login:updated")); } catch {}
}

function uniqNum(arr: number[] = []) { return Array.from(new Set(arr.filter((n) => Number.isFinite(n)))); }
function unionAcademies(children: ChildSummary[]) {
  return uniqNum(children.flatMap((c) => c.academies ?? []));
}

/** ===== 공통 UI ===== */
function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-black">
      <svg className="h-4 w-4 animate-spin text-black" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-90" d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
      </svg>
      {label && <span>{label}</span>}
    </div>
  );
}

function SummaryItem({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-200 px-4 py-3 max-w-full">
      <div className="text-[11px] text-gray-600">{label}</div>
      <div className="text-sm font-medium text-gray-900 mt-0.5 break-words break-all hyphens-auto">
        {value}
      </div>
    </div>
  );
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-200 px-4 py-3 max-w-full overflow-visible">
      <div className="text-[11px] text-gray-600">{label}</div>
      <div className="text-sm font-medium text-gray-900 mt-0.5 break-words break-all hyphens-auto min-w-0">
        {value}
      </div>
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <div className="flex items-center gap-2 rounded-xl bg-gray-50 ring-1 ring-gray-200 px-4 py-3 text-sm text-gray-700">
      <svg width="16" height="16" viewBox="0 0 24 24" className="opacity-60" aria-hidden="true">
        <path fill="currentColor" d="M12 2a10 10 0 1 0 10 10A10.011 10.011 0 0 0 12 2m1 15h-2v-2h2zm0-4h-2V7h2z"/>
      </svg>
      {text}
    </div>
  );
}

/** 접이식 섹션(아코디언) — max-height로 부드럽게 열고 닫기 */
function CollapsibleSection({
  title,
  children,
  defaultOpen = false,
}: {
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const bodyRef = useRef<HTMLDivElement | null>(null);

  // 열고 닫을 때 maxHeight 갱신
  useEffect(() => {
    const el = bodyRef.current;
    if (!el) return;
    const set = () => {
      if (open) {
        el.style.maxHeight = el.scrollHeight + "px";
        el.style.opacity = "1";
      } else {
        el.style.maxHeight = "0px";
        el.style.opacity = "0";
      }
    };
    set();

    // 컨텐츠 높이가 변해도 자연스럽게 따라가도록 ResizeObserver
    let ro: ResizeObserver | undefined;
    if (typeof ResizeObserver !== "undefined") {
      ro = new ResizeObserver(() => {
        if (open) el.style.maxHeight = el.scrollHeight + "px";
      });
      ro.observe(el);
    }
    return () => ro?.disconnect();
  }, [open, children]);

  return (
    <section className="rounded-2xl border bg-white shadow-sm ring-1 ring-black/5 overflow-visible">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="w-full flex items-center justify-between gap-3 px-5 py-4"
        aria-expanded={open}
        aria-controls={title}
      >
        <div className="flex items-center gap-2">
          <span className="inline-block h-2 w-2 rounded-full bg-emerald-500" />
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
        </div>
        <svg
          className={`h-5 w-5 text-gray-500 transition-transform ${open ? "rotate-180" : ""}`}
          viewBox="0 0 24 24"
          fill="none"
          aria-hidden="true"
        >
          <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      <div
        id={title}
        ref={bodyRef}
        className="px-5 pb-5 overflow-hidden transition-all duration-300 ease-in-out"
        style={{ maxHeight: defaultOpen ? undefined : 0, opacity: defaultOpen ? 1 : 0 }}
      >
        {children}
      </div>
    </section>
  );
}

/** ===== 통합 프로필 카드 ===== */
export default function ParentProfileCard() {
  const router = useRouter();

  // 로그인 상태
  const login = readLogin();
  const token = login?.token ?? "";
  const parentId = useMemo(() => login?.username ?? "", [login?.username]);

  // 페이지 상태
  const [loading, setLoading] = useState(false);
  const [pageErr, setPageErr] = useState<string | null>(null);

  const [parent, setParent] = useState<ParentInfo | null>(null);
  const [children, setChildren] = useState<ChildSummary[]>([]);

  // 자녀 등록/삭제 상태(요약 카드에 통합)
  const [studentId, setStudentId] = useState("");
  const [savingChild, setSavingChild] = useState(false);
  const [childMsg, setChildMsg] = useState<string | null>(null);
  const [childErr, setChildErr] = useState<string | null>(null);

  // 학원 등록 상태(자녀 상세 섹션에서만 사용)
  const [selectedChild, setSelectedChild] = useState<string | null>(null);
  const [academyInput, setAcademyInput] = useState("");
  const [savingAcademy, setSavingAcademy] = useState(false);
  const [academyMsg, setAcademyMsg] = useState<string | null>(null);
  const [academyErr, setAcademyErr] = useState<string | null>(null);

  // 자녀 상세 보기
  const [childDetail, setChildDetail] = useState<StudentDetail | null>(null);
  const [childDetailLoading, setChildDetailLoading] = useState(false);
  const [childDetailErr, setChildDetailErr] = useState<string | null>(null);

  // 파생값: 학원번호(로그인 저장 동기화용)
  const academyNumbers = useMemo(() => {
    const fromParent = (parent?.academyNumbers ?? []).filter(Number.isFinite);
    if (fromParent.length > 0) return fromParent as number[];
    const fromKids = unionAcademies(children);
    if (fromKids.length > 0) return fromKids;
    return (login?.academyNumbers ?? []).filter(Number.isFinite) as number[];
  }, [parent, children, login]);

  // 초기 로드
  useEffect(() => {
    if (!parentId) return;
    let aborted = false;
    (async () => {
      setLoading(true); setPageErr(null);
      try {
        const [p, kids] = await Promise.all([
          api<ParentInfo>(`/api/parents/${encodeURIComponent(parentId)}`, { token }),
          api<ChildSummary[]>(`/api/parents/${encodeURIComponent(parentId)}/children`, { token }),
        ]);
        if (aborted) return;
        setParent(p ?? null);
        setChildren(Array.isArray(kids) ? kids : []);
        setSelectedChild(kids?.[0]?.studentId ?? null);
      } catch (e: any) {
        if (!aborted) setPageErr(e?.message ?? "정보를 불러오지 못했습니다.");
      } finally {
        if (!aborted) setLoading(false);
      }
    })();
    return () => { aborted = true; };
  }, [parentId, token]);

  // 자녀 목록 갱신 시 자동 선택
  useEffect(() => {
    if (!selectedChild && children.length > 0) {
      setSelectedChild(children[0].studentId);
    }
  }, [children, selectedChild]);

  // 로그인 저장 동기화(학원번호만)
  useEffect(() => {
    if (!login) return;
    if (academyNumbers.length === (login.academyNumbers?.length ?? 0)) return;
    const next = { ...login, academyNumbers } as LoginSession;
    writeLogin(next);
  }, [academyNumbers]);

  /** ===== 자녀 등록/삭제(요약 카드) ===== */
  const addChild = async () => {
    const id = studentId.trim();
    if (!id) return;
    if (!parentId) { setChildMsg(null); setChildErr("로그인 정보가 없습니다."); return; }

    setSavingChild(true); setChildMsg(null); setChildErr(null);
    try {
      await api<void>(`/api/parents/${encodeURIComponent(parentId)}/children`, {
        method: "POST",
        body: JSON.stringify({ studentId: id }),
        token,
      });
      setStudentId("");
      const kids = await api<ChildSummary[]>(`/api/parents/${encodeURIComponent(parentId)}/children`, { token });
      setChildren(Array.isArray(kids) ? kids : []);
      setChildMsg("등록되었습니다.");
    } catch (e: any) {
      const m = typeof e?.message === "string" ? e.message : "";
      if (m.startsWith("409")) setChildErr("이미 등록된 자녀입니다.");
      else if (m.startsWith("400")) setChildErr("studentId 또는 studentIds가 필요합니다.");
      else setChildErr(e?.message ?? "등록에 실패했습니다.");
    } finally {
      setSavingChild(false);
      setTimeout(() => setChildMsg(null), 1500);
    }
  };

  const removeChild = async (sid: string) => {
    if (!sid) return;
    if (!confirm(`자녀(${sid}) 연결을 해제할까요?`)) return;
    setSavingChild(true); setChildMsg(null); setChildErr(null);
    try {
      await api<void>(`/api/parents/${encodeURIComponent(parentId)}/children/${encodeURIComponent(sid)}`, {
        method: "DELETE", token
      });
      const next = children.filter((c) => c.studentId !== sid);
      setChildren(next);
      setChildMsg("삭제되었습니다.");
      if (selectedChild === sid) {
        setSelectedChild(next[0]?.studentId ?? null);
        setChildDetail(null);
      }
    } catch (e: any) {
      setChildErr(e?.message ?? "삭제에 실패했습니다.");
    } finally {
      setSavingChild(false);
      setTimeout(() => setChildMsg(null), 1500);
    }
  };

  /** ===== 선택 자녀 상세(불러오기) ===== */
  useEffect(() => {
    if (!selectedChild) { setChildDetail(null); setChildDetailErr(null); return; }
    let aborted = false;
    (async () => {
      setChildDetailLoading(true);
      setChildDetailErr(null);
      setChildDetail(null);
      try {
        const raw = await api<StudentDetail>(`/api/students/${encodeURIComponent(selectedChild)}`, { token });
        if (aborted) return;
        setChildDetail(raw && typeof raw === "object" ? raw : null);
      } catch (e: any) {
        if (aborted) return;
        const msg = typeof e?.message === "string" ? e.message : "";
        if (msg.startsWith("404")) setChildDetailErr("DB에 존재하지 않는 학생입니다.");
        else setChildDetailErr(e?.message ?? "자녀 상세 정보를 불러오지 못했습니다.");
        setChildDetail(null);
      } finally {
        if (!aborted) setChildDetailLoading(false);
      }
    })();
    return () => { aborted = true; };
  }, [selectedChild, token]);

  /** ===== 학원 등록/삭제(자녀 상세 섹션) ===== */
  const addAcademy = async () => {
    if (!selectedChild) return;
    const raw = academyInput.trim();
    const num = Number(raw);
    if (!Number.isFinite(num)) { setAcademyErr("숫자만 입력하세요."); setAcademyMsg(null); return; }
    setSavingAcademy(true); setAcademyMsg(null); setAcademyErr(null);
    try {
      await api(`/api/parents/${encodeURIComponent(parentId)}/children/${encodeURIComponent(selectedChild)}/academies/${encodeURIComponent(num)}`, {
        method: "POST", token
      });
      const kids = await api<ChildSummary[]>(`/api/parents/${encodeURIComponent(parentId)}/children`, { token });
      setChildren(Array.isArray(kids) ? kids : []);
      setAcademyMsg("추가되었습니다."); setAcademyInput("");
    } catch (e: any) {
      setAcademyErr(e?.message ?? "추가에 실패했습니다.");
    } finally {
      setSavingAcademy(false);
      setTimeout(() => setAcademyMsg(null), 1500);
    }
  };

  const removeAcademy = async (n: number) => {
    if (!selectedChild) return;
    setSavingAcademy(true); setAcademyMsg(null); setAcademyErr(null);
    try {
      await api(`/api/parents/${encodeURIComponent(parentId)}/children/${encodeURIComponent(selectedChild)}/academies`, {
        method: "PATCH",
        body: JSON.stringify({ remove: [n] }),
        token,
      });
      const kids = await api<ChildSummary[]>(`/api/parents/${encodeURIComponent(parentId)}/children`, { token });
      setChildren(Array.isArray(kids) ? kids : []);
      setAcademyMsg("삭제되었습니다.");
      setTimeout(() => setAcademyMsg(null), 1500);
    } catch (e: any) {
      setAcademyErr(e?.message ?? "삭제에 실패했습니다.");
    } finally {
      setSavingAcademy(false);
    }
  };

  const selectedChildObj = useMemo(
    () => children.find((c) => c.studentId === selectedChild) || null,
    [children, selectedChild]
  );

  /** ===== 상세 렌더링(화이트리스트 + 한글 라벨 고정) — 학원번호는 제외 ===== */
  function isEmpty(v: any) {
    if (v === null || v === undefined) return true;
    if (typeof v === "string" && v.trim() === "") return true;
    if (Array.isArray(v) && v.length === 0) return true;
    if (typeof v === "object" && !Array.isArray(v) && Object.keys(v).length === 0) return true;
    return false;
  }
  const pick = (obj: StudentDetail, candidates: string[]) => {
    for (const key of candidates) {
      if (key in obj && !isEmpty(obj[key])) return obj[key];
    }
    return undefined;
  };
  function buildSelectedEntries(detail: StudentDetail) {
    if (!detail) return [];
    const out: { label: string; value: string }[] = [];
    const push = (label: string, raw: any) => {
      if (raw === undefined) return;
      const value = typeof raw === "object" ? JSON.stringify(raw) : String(raw);
      if (!isEmpty(value)) out.push({ label, value });
    };
    push("이름",         pick(detail, ["Student_Name", "studentName", "student_name", "name"]));
    push("주소",         pick(detail, ["Student_Address", "studentAddress", "student_address", "address"]));
    push("핸드폰 번호",   pick(detail, ["Student_Phone_Number", "studentPhoneNumber", "student_phone_number"]));
    push("학교",         pick(detail, ["School", "school", "schoolName", "school_name"]));
    push("학년",         pick(detail, ["Grade", "grade"]));
    push("성별",         pick(detail, ["Gender", "gender"]));
    // ⛔️ 학원번호(보기)는 여기서 제외 — 아래 ‘학원번호 관리’에서만 표시/삭제
    return out;
  }

  /** ===== 레이아웃 ===== */
  return (
    <div className="space-y-5">
      {/* 헤더 */}
      <div className="rounded-2xl bg-gradient-to-r from-emerald-50 to-white ring-1 ring-black/5 p-5 md:p-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 ring-1 ring-emerald-200 text-emerald-700 font-semibold">
            {login?.name?.[0]?.toUpperCase() ?? login?.username?.[0]?.toUpperCase() ?? "P"}
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">내 정보 & 자녀 관리</h2>
          </div>
        </div>
        <button
          onClick={() => router.push("/settings/profile")}
          className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm hover:bg-emerald-700 active:scale-[0.99] transition"
          type="button"
        >
          내 정보 수정
        </button>
      </div>

      {/* 요약 카드 (+ 자녀 등록 통합) */}
      <section className="rounded-2xl border bg-white p-5 md:p-6 shadow-sm ring-1 ring-black/5 max-w-full overflow-visible">
        {loading ? (
          <>
            <Spinner label="불러오는 중…" />
            <div className="animate-pulse mt-4 space-y-3">
              <div className="h-4 w-24 rounded bg-gray-200" />
              <div className="h-5 w-48 rounded bg-gray-200" />
              <div className="h-4 w-24 rounded bg-gray-200 mt-3" />
              <div className="h-5 w-40 rounded bg-gray-200" />
            </div>
          </>
        ) : pageErr ? (
          <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">
            오류: {pageErr}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4">
            {/* 부모 기본 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <SummaryItem label="아이디" value={parent?.parentsId ?? login?.username ?? "-"} />
              <SummaryItem label="이름" value={parent?.parentsName ?? login?.name ?? "-"} />
              <SummaryItem label="연락처" value={parent?.parentsPhoneNumber ?? "-"} />
            </div>

            {/* 자녀 등록(통합) */}
            <div className="rounded-2xl ring-1 ring-gray-200 p-4 max-w-full overflow-visible">
              <div className="text-sm font-semibold text-gray-900 mb-2">자녀 등록</div>
              <div className="flex flex-wrap items-end gap-3">
                <div className="w-full sm:w-auto sm:max-w-xs">
                  <label className="block text-xs text-gray-600">학생 아이디</label>
                  <input
                    value={studentId}
                    onChange={(e) => setStudentId(e.target.value)}
                    placeholder="예) 12345"
                    className="mt-1 w-full sm:w-64 rounded-xl border border-black/30 px-3 py-2 text-sm text-black placeholder-black/50 ring-1 ring-black/20 focus:outline-none focus:ring-2 focus:ring-black"
                  />
                </div>
                <button
                  onClick={addChild}
                  type="button"
                  className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm disabled:opacity-50 hover:bg-emerald-700 active:scale-[0.99] transition"
                  disabled={!studentId || savingChild}
                >
                  {savingChild ? "처리 중…" : "등록"}
                </button>
                {childErr && (
                  <span className="text-sm text-red-700 bg-red-50 ring-1 ring-red-200 rounded px-2 py-1">
                    {childErr}
                  </span>
                )}
                {childMsg && (
                  <span className="text-sm text-emerald-700 bg-emerald-50 ring-1 ring-emerald-200 rounded px-2 py-1">
                    {childMsg}
                  </span>
                )}
              </div>

              {/* 등록된 자녀 리스트 */}
              <div className="mt-4">
                <div className="text-xs text-gray-600 mb-1">등록된 자녀</div>
                {children.length === 0 ? (
                  <div className="text-sm text-gray-600">아직 등록된 자녀가 없습니다.</div>
                ) : (
                  <div className="grid gap-2">
                    {children.map((c) => (
                      <div key={c.studentId} className="flex items-center justify-between rounded-xl bg-white ring-1 ring-gray-200 px-3 py-2 max-w-full">
                        <div className="min-w-0 pr-2">
                          <div className="text-sm font-medium text-gray-900 truncate">{c.studentName ?? c.studentId}</div>
                          <div className="text-[11px] text-gray-600 truncate">
                            {c.studentId}
                            {c.academies?.length ? ` · 학원: ${c.academies.map((n) => `#${n}`).join(", ")}` : ""}
                          </div>
                        </div>
                        <button
                          onClick={() => removeChild(c.studentId)}
                          type="button"
                          className="text-sm font-medium text-red-600 hover:underline disabled:opacity-50 shrink-0"
                          disabled={savingChild}
                        >
                          삭제
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </section>

      {/* 자녀 상세 정보 (+ 학원 관리 ‘보기’ 단일화) */}
      <CollapsibleSection title="자녀 상세 정보">
        {/* 자녀 선택 */}
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <div className="text-xs text-gray-600">
            {children.length > 0 ? `등록된 자녀: ${children.length}명` : "등록된 자녀가 없습니다."}
          </div>
          <div className="w-full sm:w-auto sm:max-w-xs">
            <label className="block text-xs text-gray-600">대상 자녀</label>
            <select
              className="mt-1 w-full sm:w-56 rounded-xl border border-black/30 px-3 py-2 text-sm text-black ring-1 ring-black/20 focus:outline-none focus:ring-2 focus:ring-black disabled:opacity-60"
              value={selectedChild ?? ""}
              onChange={(e) => setSelectedChild(e.target.value || null)}
              disabled={children.length === 0}
            >
              <option value="">{children.length === 0 ? "선택할 자녀가 없습니다" : "선택하세요"}</option>
              {children.map((c) => (
                <option key={c.studentId} value={c.studentId}>
                  {c.studentName ?? c.studentId} ({c.studentId})
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* 상세/스피너 */}
        {!selectedChild ? (
          <Empty text="상단에서 자녀를 선택하세요." />
        ) : childDetailLoading ? (
          <div className="space-y-3">
            <Spinner label="자녀 정보를 불러오는 중…" />
            <div className="animate-pulse grid grid-cols-1 gap-3">
              <div className="h-5 rounded bg-gray-200" />
              <div className="h-5 rounded bg-gray-200" />
              <div className="h-5 rounded bg-gray-200" />
              <div className="h-5 rounded bg-gray-200" />
            </div>
          </div>
        ) : !childDetail ? (
          <div className="space-y-2">
            {childDetailErr && (
              <div className="rounded-lg bg-amber-50 text-amber-800 text-sm px-3 py-2 ring-1 ring-amber-200">
                {childDetailErr}
              </div>
            )}
            <Empty text="표시할 학생 정보가 없습니다." />
          </div>
        ) : (
          <>
            {/* 선택 자녀 상세 필드 — 학원번호는 제외됨 */}
            {(() => {
              const entries = buildSelectedEntries(childDetail);
              return entries.length === 0 ? (
                <Empty text="표시할 학생 정보가 없습니다." />
              ) : (
                <div className="grid grid-cols-1 gap-3">
                  {entries.map((e, i) => (
                    <Info key={i} label={e.label} value={e.value} />
                  ))}
                </div>
              );
            })()}

            {/* 학원번호 관리(여기서만 ‘보기’ + 삭제/추가) */}
            <div className="mt-5 rounded-2xl ring-1 ring-gray-200 p-4 max-w-full overflow-visible">
              <div className="text-sm font-semibold text-gray-900 mb-2">학원번호 관리</div>

              {/* 현재 학원번호: 한 곳만 보여줌 + 삭제 버튼 */}
              <div className="mb-3">
                <div className="text-xs font-medium text-gray-900 mb-1">현재 학원번호</div>
                {!selectedChildObj ? (
                  <div className="text-sm text-gray-600">자녀를 먼저 선택하세요.</div>
                ) : selectedChildObj.academies?.length ? (
                  <div className="flex flex-wrap gap-2 overflow-x-auto">
                    {selectedChildObj.academies!.map((n) => (
                      <span key={n} className="inline-flex items-center gap-2 rounded-full bg-gray-50 ring-1 ring-gray-200 px-3 py-1 text-sm text-gray-900">
                        #{n}
                        <button
                          title="삭제"
                          onClick={() => removeAcademy(n)}
                          type="button"
                          className="text-[12px] text-red-600 hover:underline disabled:opacity-50"
                          disabled={savingAcademy}
                        >
                          삭제
                        </button>
                      </span>
                    ))}
                  </div>
                ) : (
                  <div className="text-sm text-gray-600">등록된 학원이 없습니다.</div>
                )}
              </div>

              {/* 추가 입력(원하면 유지) */}
              <div className="flex flex-wrap items-end gap-3">
                <div className="w-full sm:w-auto sm:max-w-[180px]">
                  <label className="block text-xs text-gray-600">학원번호 추가</label>
                  <input
                    value={academyInput}
                    onChange={(e) => setAcademyInput(e.target.value)}
                    placeholder="예) 103"
                    className="mt-1 w-full rounded-xl border border-black/30 px-3 py-2 text-sm text-black placeholder-black/50 ring-1 ring-black/20 focus:outline-none focus:ring-2 focus:ring-black"
                    inputMode="numeric"
                  />
                </div>
                <button
                  onClick={addAcademy}
                  type="button"
                  className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm disabled:opacity-50 hover:bg-emerald-700 active:scale-[0.99] transition"
                  disabled={!academyInput || savingAcademy || !selectedChild}
                >
                  {savingAcademy ? "추가 중…" : "추가"}
                </button>
                {academyErr && (
                  <span className="text-sm text-red-700 bg-red-50 ring-1 ring-red-200 rounded px-2 py-1">{academyErr}</span>
                )}
                {academyMsg && (
                  <span className="text-sm text-emerald-700 bg-emerald-50 ring-1 ring-emerald-200 rounded px-2 py-1">{academyMsg}</span>
                )}
              </div>
            </div>
          </>
        )}
      </CollapsibleSection>
    </div>
  );
}
