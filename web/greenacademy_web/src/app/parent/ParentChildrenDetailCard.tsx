// src/app/parent/ParentChildrenDetailCard.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";

/** ===== 타입 ===== */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = { role: Role; username: string; name?: string; token?: string; academyNumbers?: number[] };
type ChildSummary = { studentId: string; studentName?: string | null; academies?: number[] };
type StudentDetail = Record<string, any>;
type AcademyRequest = {
  id: string;
  academyNumber: number;
  status: "PENDING" | "APPROVED" | "REJECTED" | string;
  memo?: string;
  processedMemo?: string;
  processedBy?: string;
  createdAt?: string;
  updatedAt?: string;
};

/** ===== API 유틸 ===== */
const RAW = (process.env.NEXT_PUBLIC_API_BASE || "").trim();
const API_BASE = "/backend";


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
function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border bg-white shadow-sm ring-1 ring-black/5 overflow-visible">
      <div className="w-full flex items-center justify-between gap-3 px-5 py-4">
        <div className="flex items-center gap-2">
          <span className="inline-block h-2 w-2 rounded-full bg-emerald-500" />
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
        </div>
      </div>
      <div className="px-5 pb-5">{children}</div>
    </section>
  );
}

/** ===== 메인: 자녀 상세 정보 카드 ===== */
export default function ParentChildrenDetailCard() {
  const login = readLogin();
  const token = login?.token ?? "";
  const parentId = login?.username ?? "";

  const [loading, setLoading] = useState(false);
  const [pageErr, setPageErr] = useState<string | null>(null);

  const [children, setChildren] = useState<ChildSummary[]>([]);
  const [selectedChild, setSelectedChild] = useState<string | null>(null);

  const [childDetail, setChildDetail] = useState<StudentDetail | null>(null);
  const [childDetailLoading, setChildDetailLoading] = useState(false);
  const [childDetailErr, setChildDetailErr] = useState<string | null>(null);

  const [academyInput, setAcademyInput] = useState("");
  const [savingAcademy, setSavingAcademy] = useState(false);
  const [academyMsg, setAcademyMsg] = useState<string | null>(null);
  const [academyErr, setAcademyErr] = useState<string | null>(null);
  const [requests, setRequests] = useState<AcademyRequest[]>([]);

  // 자녀 목록 로드
  useEffect(() => {
    if (!parentId) return;
    let aborted = false;
    (async () => {
      setLoading(true); setPageErr(null);
      try {
        const kids = await api<ChildSummary[]>(`/api/parents/${encodeURIComponent(parentId)}/children`, { token });
        if (aborted) return;
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

  // 선택 자녀 상세 로드
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

  const selectedChildObj = useMemo(
    () => children.find((c) => c.studentId === selectedChild) || null,
    [children, selectedChild]
  );

  const approvedAcademies = useMemo(
    () =>
      requests
        .filter((r) => r.status === "APPROVED")
        .map((r) => Number(r.academyNumber))
        .filter(Number.isFinite),
    [requests]
  );

  const displayAcademies = useMemo(() => {
    const base = Array.isArray(selectedChildObj?.academies) ? selectedChildObj!.academies! : [];
    return Array.from(new Set([...base, ...approvedAcademies]));
  }, [selectedChildObj, approvedAcademies]);

  /** 상세 출력 빌더 — 학원번호는 제외 */
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
    return out;
  }

  /** 내 승인 요청 목록 */
  useEffect(() => {
    if (!parentId) return;
    let aborted = false;
    (async () => {
      try {
        const list = await api<AcademyRequest[]>(
          `/api/academy-requests?scope=mine&requesterId=${encodeURIComponent(parentId)}&requesterRole=parent`,
          { token }
        );
        if (!aborted) setRequests(Array.isArray(list) ? list : []);
      } catch {
        if (!aborted) setRequests([]);
      }
    })();
    return () => { aborted = true; };
  }, [parentId, token]);

  /** 학원번호 추가 → 승인 요청 */
  const addAcademy = async () => {
    if (!selectedChild) return;
    const raw = academyInput.trim();
    const num = Number(raw);
    if (!Number.isFinite(num)) { setAcademyErr("숫자만 입력하세요."); setAcademyMsg(null); return; }
    setSavingAcademy(true); setAcademyMsg(null); setAcademyErr(null);
    try {
      await api(`/api/academy-requests`, {
        method: "POST",
        body: JSON.stringify({
          academyNumber: num,
          requesterId: parentId,
          requesterRole: "parent",
          studentId: selectedChild,
          memo: `자녀 ${selectedChild} 학원 연결 요청`,
        }),
        token
      });
      setAcademyMsg("승인 요청을 등록했습니다."); setAcademyInput("");
      const list = await api<AcademyRequest[]>(
        `/api/academy-requests?scope=mine&requesterId=${encodeURIComponent(parentId)}&requesterRole=parent`,
        { token }
      );
      setRequests(Array.isArray(list) ? list : []);
    } catch (e: any) {
      setAcademyErr(e?.message ?? "승인 요청에 실패했습니다.");
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

  return (
    <div className="space-y-5">
      <Section title="자녀 상세 정보">
        {/* 요약/로딩 */}
        {loading ? (
          <Spinner label="불러오는 중…" />
        ) : pageErr ? (
          <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {pageErr}</div>
        ) : (
          <>
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

            {/* 상세 출력 */}
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

                {/* 학원번호 관리 */}
                <div className="mt-5 rounded-2xl ring-1 ring-gray-200 p-4 max-w-full overflow-visible">
                  <div className="text-sm font-semibold text-gray-900 mb-2">학원번호 관리 (승인 요청)</div>

                  <div className="mb-3">
                    <div className="text-xs font-medium text-gray-900 mb-1">현재 학원번호</div>
                    {!selectedChildObj ? (
                      <div className="text-sm text-gray-600">자녀를 먼저 선택하세요.</div>
                    ) : displayAcademies.length ? (
                      <div className="flex flex-wrap gap-2 overflow-x-auto">
                        {displayAcademies.map((n) => (
                          <span key={n} className="inline-flex items-center gap-2 rounded-full bg-gray-50 ring-1 ring-gray-200 px-3 py-1 text-sm text-gray-900">
                            #{n}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <div className="text-sm text-gray-600">등록된 학원이 없습니다.</div>
                    )}
                  </div>

                  <div className="mb-3">
                    <div className="text-xs font-medium text-gray-900 mb-1">내 승인 요청</div>
                    {requests.filter((r) => r.status !== "APPROVED").length === 0 ? (
                      <div className="text-sm text-gray-600">대기 중인 요청이 없습니다.</div>
                    ) : (
                      <div className="space-y-2">
                        {requests.filter((r) => r.status !== "APPROVED").map((r) => (
                          <div key={r.id} className="rounded-xl bg-white ring-1 ring-gray-200 px-3 py-2 flex items-center justify-between text-sm">
                            <div>
                              <div className="font-semibold text-gray-900">학원 #{r.academyNumber}</div>
                              <div className="text-xs text-gray-600">
                                {r.status === "PENDING" ? "대기" : "거절"}
                                {r.processedMemo ? ` · ${r.processedMemo}` : ""}
                              </div>
                            </div>
                            <span className={`px-2 py-0.5 rounded-full text-xs ${
                              r.status === "REJECTED"
                                ? "bg-rose-100 text-rose-700"
                                : "bg-amber-100 text-amber-700"
                            }`}>
                              {r.status === "PENDING" ? "대기" : "거절"}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

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
          </>
        )}
      </Section>
    </div>
  );
}
