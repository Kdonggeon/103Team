// C:\project\103Team-sub\web\greenacademy_web\src\app\teacher\TeacherProfileCard.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import api, { type LoginResponse, type CourseLite } from "@/app/lib/api";

/** ===== 타입/유틸 ===== */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = { role: Role; username: string; name?: string; token?: string; academyNumbers?: number[] };
type TeacherDetail = Record<string, any>;
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

const API_BASE = "/backend";

function readLogin(): LoginSession | null {
  try {
    const raw = localStorage.getItem("login");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}
function isEmpty(v: any) {
  if (v === null || v === undefined) return true;
  if (typeof v === "string" && v.trim() === "") return true;
  if (Array.isArray(v) && v.length === 0) return true;
  if (typeof v === "object" && !Array.isArray(v) && Object.keys(v).length === 0) return true;
  return false;
}
/** null/undefined 안전 */
function pick(obj: TeacherDetail | null | undefined, keys: string[]) {
  if (!obj) return undefined;
  for (const k of keys) if (k in obj && !isEmpty((obj as any)[k])) return (obj as any)[k];
  return undefined;
}
/** 다양한 응답 래핑/배열 케이스를 흡수해 실제 레코드를 추출 */
function firstObject(...cands: any[]) {
  for (const c of cands) {
    if (c && typeof c === "object" && !Array.isArray(c) && Object.keys(c).length > 0) return c;
  }
  return null;
}
function unwrapDetail(d: any) {
  if (!d) return null;
  if (Array.isArray(d)) return firstObject(d[0], d.find((x) => x && typeof x === "object"));
  return firstObject(
    d,
    d.data,
    d.result,
    d.payload,
    d.body,
    d.response,
    d.teacher,
    d.Teacher,
    d.user,
    d.profile,
    d.details,
    d.detail,
    Array.isArray(d.content) ? d.content[0] : d.content
  );
}
/** 404/405는 폴백(세션만 표시), 그 외 상태만 에러로 처리 */
async function tryGetJson(path: string, token?: string) {
  const r = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    cache: "no-store",
    credentials: "include",
  });
  if (r.ok) return r.json();
  if (r.status === 404 || r.status === 405) return null; // 폴백
  throw new Error(`${r.status} ${r.statusText}`);
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
    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-200 px-4 py-3">
      <div className="text-[11px] text-gray-600">{label}</div>
      <div className="text-sm font-medium text-gray-900 mt-0.5 break-words">{value}</div>
    </div>
  );
}

/** ===== 모달(다이얼로그) — /settings/profile을 동일하게 iframe으로 오버레이 ===== */
function useLockBodyScroll(active: boolean) {
  useEffect(() => {
    if (!active) return;
    const original = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = original;
    };
  }, [active]);
}

function ProfileEditModal({
  open,
  onClose,
  src = "/settings/profile",
}: {
  open: boolean;
  onClose: () => void;
  src?: string;
}) {
  useLockBodyScroll(open);

  // ESC 닫기
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  // 포털 대상
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!mounted) return null;
  if (!open) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[1000] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-label="내 정보 수정"
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-[1px]" onClick={onClose} />
      {/* Dialog panel */}
      <div className="relative w-full max-w-[980px] h-[85vh] rounded-2xl bg-white shadow-2xl ring-1 ring-black/10 overflow-hidden">
        {/* Header */}
        <div className="h-12 shrink-0 flex items-center justify-between px-4 border-b">
          <div className="text-sm font-semibold text-gray-900">내 정보 수정</div>
          <button
            type="button"
            onClick={onClose}
            className="inline-flex items-center justify-center w-8 h-8 rounded-lg hover:bg-gray-100"
            aria-label="닫기"
            title="닫기 (Esc)"
          >
            <svg viewBox="0 0 24 24" className="w-5 h-5 text-gray-700" fill="none" stroke="currentColor" strokeWidth="2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 6l12 12M18 6l-12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="w-full h-[calc(85vh-3rem)] bg-white">
          <iframe src={src} title="내 정보 수정" className="w-full h-full" />
        </div>
      </div>
    </div>,
    document.body
  );
}

/** ===== 메인(학생 디자인 유지 + 한글 라벨 + 다중 학원번호 + 반 정보) ===== */
export default function TeacherProfileCard({ user }: { user?: LoginResponse }) {
  const login = readLogin();

  // props 우선, 없으면 로컬스토리지 로그인 사용
  const token = login?.token ?? "";
  const teacherId = user?.username ?? login?.username ?? "";

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [detail, setDetail] = useState<TeacherDetail | null>(null);

  // 담당 반
  const [classes, setClasses] = useState<CourseLite[]>([]);
  const [classesErr, setClassesErr] = useState<string | null>(null);
  const [classesLoading, setClassesLoading] = useState(false);

  // 모달/소속 해제 모드
  const [openEdit, setOpenEdit] = useState(false);
  const [detachMode, setDetachMode] = useState(false);
  const [academyInput, setAcademyInput] = useState("");
  const [requests, setRequests] = useState<AcademyRequest[]>([]);
  const [reqMsg, setReqMsg] = useState<string | null>(null);
  const [reqErr, setReqErr] = useState<string | null>(null);
  const [savingReq, setSavingReq] = useState(false);

  useEffect(() => {
    if (!teacherId) return;
    let aborted = false;

    // 1) 교사 상세
    (async () => {
      setLoading(true);
      setErr(null);
      try {
        const candidates = [
          `/api/teachers/${encodeURIComponent(teacherId)}`,
          `/api/teacher/${encodeURIComponent(teacherId)}`,
          `/api/users/${encodeURIComponent(teacherId)}`,
          `/api/teachers?teacherId=${encodeURIComponent(teacherId)}`,
        ];
        let found: any = null;
        for (const p of candidates) {
          const d = await tryGetJson(p, token);
          if (d) {
            found = d;
            break;
          }
        }
        const unwrapped = unwrapDetail(found) ?? {};
        if (!aborted) setDetail(unwrapped);
      } catch (e: any) {
        if (!aborted) setErr(e?.message ?? "정보를 불러오지 못했습니다.");
      } finally {
        if (!aborted) setLoading(false);
      }
    })();

    // 2) 담당 반 목록
    (async () => {
      setClassesLoading(true);
      setClassesErr(null);
      try {
        const list = await api.listMyClasses(teacherId);
        if (!aborted) setClasses(list || []);
      } catch (e: any) {
        if (!aborted) setClassesErr(e?.message ?? "반 목록을 불러오지 못했습니다.");
      } finally {
        if (!aborted) setClassesLoading(false);
      }
    })();

    return () => {
      aborted = true;
    };
  }, [teacherId, token]);

  // 승인 요청 목록
  useEffect(() => {
    if (!teacherId) return;
    let aborted = false;
    (async () => {
      try {
        const list = await tryGetJson(`/api/academy-requests?scope=mine&requesterId=${encodeURIComponent(teacherId)}&requesterRole=teacher`, token);
        if (!aborted) setRequests(Array.isArray(list) ? list : []);
      } catch {
        if (!aborted) setRequests([]);
      }
    })();
    return () => { aborted = true; };
  }, [teacherId, token]);

  const submitRequest = async () => {
    const raw = academyInput.trim();
    const num = Number(raw);
    if (!Number.isFinite(num)) { setReqErr("숫자만 입력하세요."); setReqMsg(null); return; }
    setSavingReq(true); setReqErr(null); setReqMsg(null);
    try {
      await fetch(`${API_BASE}/api/academy-requests`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify({
          academyNumber: num,
          requesterId: teacherId,
          requesterRole: "teacher",
          memo: "교사 학원 연결 요청",
        }),
      }).then(async (r) => {
        if (!r.ok) {
          const t = await r.text().catch(() => "");
          throw new Error(`${r.status} ${r.statusText}${t ? " | " + t : ""}`);
        }
      });
      setReqMsg("승인 요청을 등록했습니다."); setAcademyInput("");
      const list = await tryGetJson(`/api/academy-requests?scope=mine&requesterId=${encodeURIComponent(teacherId)}&requesterRole=teacher`, token);
      setRequests(Array.isArray(list) ? list : []);
    } catch (e: any) {
      setReqErr(e?.message ?? "승인 요청에 실패했습니다.");
    } finally {
      setSavingReq(false);
      setTimeout(() => setReqMsg(null), 1500);
    }
  };

  /** 표기 원천 */
  const src = detail ?? {};

  // === 값 매핑(정확 키 우선) ===
  const TEACHER_NAME =
    pick(src, ["Teacher_Name", "teacherName", "name", "displayName"]) ?? user?.name ?? login?.name ?? teacherId;

  const TEACHER_ID = pick(src, ["Teacher_ID", "teacherId", "username", "userId"]) ?? teacherId;

  const TEACHER_PHONE = pick(src, ["Teacher_Phone_Number", "phoneNumber", "phone", "mobile"]) ?? "—";

  // 학원번호: 배열로 모두 표시
  const ACADEMY_NUMBERS: number[] = useMemo(() => {
    const v =
      pick(src, ["Academy_Number", "Academy_Numbers", "academyNumbers", "academies"]) ??
      user?.academyNumbers ??
      login?.academyNumbers ??
      [];
    const arr = Array.isArray(v) ? v : v == null ? [] : [v];
    const nums = arr.map((x: any) => Number(x)).filter(Number.isFinite);
    return Array.from(new Set(nums));
  }, [src, user, login]);

  const APPROVED_ACADEMIES = useMemo(
    () =>
      requests
        .filter((r) => r.status === "APPROVED")
        .map((r) => Number(r.academyNumber))
        .filter(Number.isFinite),
    [requests]
  );

  const DISPLAY_ACADEMY_NUMBERS = useMemo(
    () => Array.from(new Set([...ACADEMY_NUMBERS, ...APPROVED_ACADEMIES])),
    [ACADEMY_NUMBERS, APPROVED_ACADEMIES]
  );

  /** 🔹 소속 해제 호출 */
  const handleDetach = async (academyNumber: number) => {
    if (!teacherId || !academyNumber) return;
    const ok = window.confirm(`학원 #${academyNumber} 소속을 해제할까요?\n(담당 반 설정 등에도 영향이 있을 수 있습니다.)`);
    if (!ok) return;

    try {
      const url = `${API_BASE}/api/teachers/${encodeURIComponent(
        teacherId
      )}/academies/detach?academyNumber=${encodeURIComponent(academyNumber)}`;

      // 실제 어떤 URL로 나가는지 확인용 로그
      console.log("DETACH PATCH →", url);

      const res = await fetch(url, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      const text = await res.text();

      if (!res.ok) {
        alert(`소속 해제에 실패했습니다.\n${text || `${res.status} ${res.statusText}`}`);
        return;
      }

      // 1) 백엔드에서 Teacher 전체를 돌려주면 그대로 detail 갱신
      let updated: any = {};
      try {
        updated = text ? JSON.parse(text) : {};
      } catch {
        updated = {};
      }
      if (updated && typeof updated === "object" && Object.keys(updated).length > 0) {
        setDetail(updated);
      } else {
        // 2) 혹시 모를 경우 로컬 detail에서만 academyNumbers 제거
        setDetail((prev) => {
          if (!prev) return prev;
          const next: any = { ...prev };
          const raw = next.academyNumbers ?? next.Academy_Numbers ?? next.academies ?? [];
          const arr = (Array.isArray(raw) ? raw : [raw])
            .map((x: any) => Number(x))
            .filter((n: number) => Number.isFinite(n) && n !== academyNumber);
          if ("academyNumbers" in next) next.academyNumbers = arr;
          if ("Academy_Numbers" in next) next.Academy_Numbers = arr;
          if ("academies" in next) next.academies = arr;
          return next;
        });
      }

      // 3) 세션(login)에도 반영해서 왼쪽 사이드바 학원번호도 맞춰줌
      try {
        const raw = localStorage.getItem("login");
        if (raw) {
          const s = JSON.parse(raw);
          const acas: number[] = Array.isArray(s.academyNumbers)
            ? s.academyNumbers.filter((n: number) => n !== academyNumber)
            : [];
          localStorage.setItem("login", JSON.stringify({ ...s, academyNumbers: acas }));
        }
      } catch {
        // 세션 갱신 실패해도 치명적이진 않으니 무시
      }

      alert(`학원 #${academyNumber} 소속이 해제되었습니다.`);

      // 4) 요청대로 전체 새로고침
      window.location.reload();
    } catch (e: any) {
      alert(`소속 해제에 실패했습니다.\n${e?.message ?? String(e)}`);
    }
  };

  return (
    <>
      <div className="space-y-5">
        {/* 헤더 — 학생 카드와 동일 톤 */}
        <div className="rounded-2xl bg-gradient-to-r from-emerald-50 to-white ring-1 ring-black/5 p-5 md:p-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 ring-1 ring-emerald-200 text-emerald-700 font-semibold">
              {String(TEACHER_NAME || TEACHER_ID).trim().charAt(0).toUpperCase() || "T"}
            </div>
            <div>
              <h2 className="text-xl font-semibold text-gray-900">내 정보</h2>
              <div className="text-sm text-gray-600">아이디: {String(TEACHER_ID)}</div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setDetachMode((p) => !p)}
              className={`px-3 py-2 rounded-xl text-sm font-medium border ${
                detachMode
                  ? "border-red-500 text-red-600 bg-red-50"
                  : "border-gray-300 text-gray-800 bg-white hover:bg-gray-50"
              }`}
            >
              {detachMode ? "소속 해제 취소" : "소속 해제"}
            </button>

            <button
              onClick={() => setOpenEdit(true)}
              className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm hover:bg-emerald-700 active:scale-[0.99] transition"
              type="button"
            >
              내 정보 수정
            </button>
          </div>
        </div>

        {/* 본문 — 한글 라벨 + 다중 학원번호 */}
        <section className="rounded-2xl border bg-white p-5 md:p-6 shadow-sm ring-1 ring-black/5">
          {loading ? (
            <Spinner label="불러오는 중…" />
          ) : err ? (
            <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {err}</div>
          ) : (
            <div className="grid grid-cols-1 gap-3">
              <Info label="이름" value={String(TEACHER_NAME)} />
              <Info label="아이디" value={String(TEACHER_ID)} />
              <Info label="핸드폰번호" value={String(TEACHER_PHONE)} />
              <Info
                label="학원번호"
                value={
                  DISPLAY_ACADEMY_NUMBERS.length === 0 ? (
                    "—"
                  ) : (
                    <div className="flex flex-wrap gap-1.5">
                      {DISPLAY_ACADEMY_NUMBERS.map((n) => (
                        <span
                          key={n}
                          className="inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium bg-gray-100 text-gray-800 ring-1 ring-gray-200"
                        >
                          <span>#{n}</span>
                          {detachMode && (
                            <button
                              type="button"
                              onClick={() => handleDetach(n)}
                              className="ml-1 inline-flex items-center justify-center w-4 h-4 rounded-full hover:bg-red-100"
                              aria-label={`학원 #${n} 소속 해제`}
                            >
                              <span className="text-[10px] leading-none text-red-600">✕</span>
                            </button>
                          )}
                        </span>
                      ))}
                    </div>
                  )
                }
              />
            </div>
          )}
        </section>

        {/* 담당 반 섹션 — 한글 라벨: 반 ID / 반 이름 */}
        <section className="rounded-2xl border bg-white p-5 md:p-6 shadow-sm ring-1 ring-black/5">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-base font-semibold text-gray-900">담당 반</h3>
            {classesLoading && <Spinner />}
          </div>

          {classesErr ? (
            <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {classesErr}</div>
          ) : classes.length === 0 ? (
            <div className="text-sm text-gray-700">표시할 반이 없습니다.</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {classes.map((c) => (
                <div key={c.classId} className="rounded-xl bg-gray-50 ring-1 ring-gray-200 px-4 py-3 space-y-1.5">
                  <div className="text-[11px] text-gray-600">반 ID</div>
                  <div className="text-sm font-medium text-gray-900 break-words">{c.classId}</div>
                  <div className="text-[11px] text-gray-600 mt-2">반 이름</div>
                  <div className="text-sm font-medium text-gray-900 break-words">{c.className}</div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* 학원 연결 요청 섹션 */}
        <section className="rounded-2xl border bg-white p-5 md:p-6 shadow-sm ring-1 ring-black/5">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-base font-semibold text-gray-900">학원 연결 요청</h3>
          </div>
          <div className="flex flex-wrap items-end gap-3">
            <div className="w-full sm:w-auto sm:max-w-[180px]">
              <label className="block text-xs text-gray-600">학원번호</label>
              <input
                value={academyInput}
                onChange={(e) => setAcademyInput(e.target.value)}
                placeholder="예) 103"
                className="mt-1 w-full rounded-xl border border-black/30 px-3 py-2 text-sm text-black placeholder-black/50 ring-1 ring-black/20 focus:outline-none focus:ring-2 focus:ring-black"
                inputMode="numeric"
              />
            </div>
            <button
              type="button"
              onClick={submitRequest}
              className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm disabled:opacity-50 hover:bg-emerald-700 active:scale-[0.99] transition"
              disabled={!academyInput || savingReq}
            >
              {savingReq ? "요청 중…" : "요청"}
            </button>
            {reqErr && <span className="text-sm text-red-700 bg-red-50 ring-1 ring-red-200 rounded px-2 py-1">{reqErr}</span>}
            {reqMsg && <span className="text-sm text-emerald-700 bg-emerald-50 ring-1 ring-emerald-200 rounded px-2 py-1">{reqMsg}</span>}
          </div>

          <div className="mt-3">
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
        </section>
      </div>

      {/* 수정 모달 */}
      <ProfileEditModal open={openEdit} onClose={() => setOpenEdit(false)} src="/settings/profile" />
    </>
  );
}
