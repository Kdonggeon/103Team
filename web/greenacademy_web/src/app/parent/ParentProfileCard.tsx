// src/app/parent/ParentProfileCard.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";

/** ===== 공통 타입 ===== */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = { role: Role; username: string; name?: string; token?: string; academyNumbers?: number[] };

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
  try {
    return JSON.parse(text) as T;
  } catch {
    return undefined as unknown as T;
  }
}

function readLogin(): LoginSession | null {
  try {
    const raw = localStorage.getItem("login");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}
function writeLogin(next: LoginSession) {
  try {
    localStorage.setItem("login", JSON.stringify(next));
  } catch {}
  try {
    window.dispatchEvent(new Event("login:updated"));
  } catch {}
}
function uniqNum(arr: number[] = []) {
  return Array.from(new Set(arr.filter((n) => Number.isFinite(n))));
}
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
      <div className="text-sm font-medium text-gray-900 mt-0.5 break-words break-all hyphens-auto">{value}</div>
    </div>
  );
}

/** ---- 프로필 수정 iframe 모달 (기존 /settings/profile 재사용) ---- */
function ProfileEditModal({
  open,
  onClose,
  onSaved,
  src = "/settings/profile",
}: {
  open: boolean;
  onClose: () => void;
  onSaved: () => void; // 저장 완료 시 데이터 재조회
  src?: string;
}) {
  useEffect(() => {
    if (!open) return;
    const handler = (e: MessageEvent) => {
      const data = e?.data;
      const ok = data === "profile:saved" || (data && typeof data === "object" && (data as any).type === "profile:saved");
      if (ok) onSaved();
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("message", handler);
    document.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("message", handler);
      document.removeEventListener("keydown", onKey);
    };
  }, [open, onClose, onSaved]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-label="내 정보 수정"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-xl ring-1 ring-black/10 w-full max-w-3xl h-[80vh] overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <h3 className="text-base font-semibold text-gray-900">내 정보 수정</h3>
          <button
            onClick={onClose}
            className="rounded-lg px-2 py-1 text-sm text-gray-700 hover:bg-gray-100"
            aria-label="닫기"
            type="button"
          >
            닫기
          </button>
        </div>
        <iframe
          title="parent-profile-edit"
          src={src}
          className="w-full h-full"
          // 저장 완료 시 /settings/profile 내부에서:
          // window.parent.postMessage('profile:saved', '*')
        />
      </div>
    </div>
  );
}

/** ===== 메인: 내 정보 & 자녀 등록 카드 ===== */
export default function ParentProfileCard() {
  // 로그인 상태
  const login = readLogin();
  const token = login?.token ?? "";
  const parentId = useMemo(() => login?.username ?? "", [login?.username]);

  // 페이지 상태
  const [loading, setLoading] = useState(false);
  const [pageErr, setPageErr] = useState<string | null>(null);

  const [parent, setParent] = useState<ParentInfo | null>(null);
  const [children, setChildren] = useState<ChildSummary[]>([]);

  // 자녀 등록/삭제 상태
  const [studentId, setStudentId] = useState("");
  const [savingChild, setSavingChild] = useState(false);
  const [childMsg, setChildMsg] = useState<string | null>(null);
  const [childErr, setChildErr] = useState<string | null>(null);

  // 프로필 수정 모달
  const [openEdit, setOpenEdit] = useState(false);
  const [refreshTick, setRefreshTick] = useState(0);

  // 파생값: 학원번호(로그인 저장 동기화용)
  const academyNumbers = useMemo(() => {
    const fromParent = (parent?.academyNumbers ?? []).filter(Number.isFinite) as number[];
    if (fromParent.length > 0) return fromParent;
    const fromKids = unionAcademies(children);
    if (fromKids.length > 0) return fromKids;
    return (login?.academyNumbers ?? []).filter(Number.isFinite) as number[];
  }, [parent, children, login]);

  // 초기 로드 + 저장 후 재조회 (부모 + 자녀 목록)
  useEffect(() => {
    if (!parentId) return;
    let aborted = false;
    (async () => {
      setLoading(true);
      setPageErr(null);
      try {
        const [p, kids] = await Promise.all([
          api<ParentInfo>(`/api/parents/${encodeURIComponent(parentId)}`, { token }),
          api<ChildSummary[]>(`/api/parents/${encodeURIComponent(parentId)}/children`, { token }),
        ]);
        if (aborted) return;
        setParent(p ?? null);
        setChildren(Array.isArray(kids) ? kids : []);
      } catch (e: any) {
        if (!aborted) setPageErr(e?.message ?? "정보를 불러오지 못했습니다.");
      } finally {
        if (!aborted) setLoading(false);
      }
    })();
    return () => {
      aborted = true;
    };
  }, [parentId, token, refreshTick]);

  // 로그인 저장 동기화(학원번호만)
  useEffect(() => {
    if (!login) return;
    const prev = (login.academyNumbers ?? []).filter(Number.isFinite);
    if (academyNumbers.length === prev.length && academyNumbers.every((n, i) => n === prev[i])) return;
    const next = { ...login, academyNumbers } as LoginSession;
    writeLogin(next);
  }, [academyNumbers]);

  /** 자녀 등록/삭제 */
  const addChild = async () => {
    const id = studentId.trim();
    if (!id) return;
    if (!parentId) {
      setChildMsg(null);
      setChildErr("로그인 정보가 없습니다.");
      return;
    }

    setSavingChild(true);
    setChildMsg(null);
    setChildErr(null);
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
    setSavingChild(true);
    setChildMsg(null);
    setChildErr(null);
    try {
      await api<void>(`/api/parents/${encodeURIComponent(parentId)}/children/${encodeURIComponent(sid)}`, {
        method: "DELETE",
        token,
      });
      const next = children.filter((c) => c.studentId !== sid);
      setChildren(next);
      setChildMsg("삭제되었습니다.");
    } catch (e: any) {
      setChildErr(e?.message ?? "삭제에 실패했습니다.");
    } finally {
      setSavingChild(false);
      setTimeout(() => setChildMsg(null), 1500);
    }
  };

  return (
    <div className="space-y-5">
      {/* 헤더 */}
      <div className="rounded-2xl bg-gradient-to-r from-emerald-50 to-white ring-1 ring-black/5 p-5 md:p-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 ring-1 ring-emerald-200 text-emerald-700 font-semibold">
            {login?.name?.[0]?.toUpperCase() ?? login?.username?.[0]?.toUpperCase() ?? "P"}
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">내 정보 & 자녀 등록</h2>
          </div>
        </div>
        <button
          onClick={() => setOpenEdit(true)}
          className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm hover:bg-emerald-700 active:scale-[0.99] transition"
          type="button"
        >
          내 정보 수정
        </button>
      </div>

      {/* 본문: 부모 요약 + 자녀 등록 */}
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
          <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {pageErr}</div>
        ) : (
          <div className="grid grid-cols-1 gap-4">
            {/* 부모 기본 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <SummaryItem label="아이디" value={parent?.parentsId ?? login?.username ?? "-"} />
              <SummaryItem label="이름" value={parent?.parentsName ?? login?.name ?? "-"} />
              <SummaryItem label="연락처" value={parent?.parentsPhoneNumber ?? "-"} />
            </div>

            {/* 자녀 등록 */}
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
                  <span className="text-sm text-red-700 bg-red-50 ring-1 ring-red-200 rounded px-2 py-1">{childErr}</span>
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
                      <div
                        key={c.studentId}
                        className="flex items-center justify-between rounded-xl bg-white ring-1 ring-gray-200 px-3 py-2 max-w-full"
                      >
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

      {/* 프로필 수정 모달: 기존 /settings/profile 그대로 사용 */}
      <ProfileEditModal
        open={openEdit}
        onClose={() => setOpenEdit(false)}
        onSaved={() => {
          setOpenEdit(false);
          setRefreshTick((t) => t + 1); // 저장 후 즉시 재조회
        }}
        src="/settings/profile"
      />
    </div>
  );
}
