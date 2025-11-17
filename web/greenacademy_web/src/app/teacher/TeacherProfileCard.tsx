// C:\project\103Team-sub\web\greenacademy_web\src\app\teacher\TeacherProfileCard.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import api, { type LoginResponse, type CourseLite } from "@/app/lib/api";

/** ===== 타입/유틸 ===== */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = { role: Role; username: string; name?: string; token?: string; academyNumbers?: number[] };
type TeacherDetail = Record<string, any>;

const API_BASE = "/backend";


function readLogin(): LoginSession | null {
  try { const raw = localStorage.getItem("login"); return raw ? JSON.parse(raw) : null; } catch { return null; }
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
    d.data, d.result, d.payload, d.body, d.response,
    d.teacher, d.Teacher, d.user, d.profile, d.details, d.detail,
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
    return () => { document.body.style.overflow = original; };
  }, [active]);
}

function ProfileEditModal({
  open,
  onClose,
  src = "/settings/profile",
}: {
  open: boolean;
  onClose: () => void;
  src?: string; // 필요시 쿼리스트링 부여 가능 (/settings/profile?inModal=1 등)
}) {
  useLockBodyScroll(open);

  // ESC 닫기
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
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
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-[1px]"
        onClick={onClose}
      />
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

        {/* Body: 동일 경로를 iframe으로 로드 (세션/토큰은 동일 출처로 공유) */}
        <div className="w-full h-[calc(85vh-3rem)] bg-white">
          <iframe
            src={src}
            title="내 정보 수정"
            className="w-full h-full"
            // same-origin 이므로 로컬스토리지/쿠키 접근 및 인증 그대로 동작
            // sandbox는 주지 않음 (동일 출처 전체 기능 필요)
          />
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

  // 모달 오픈 상태
  const [openEdit, setOpenEdit] = useState(false);

  useEffect(() => {
    if (!teacherId) return;
    let aborted = false;

    // 1) 교사 상세
    (async () => {
      setLoading(true); setErr(null);
      try {
        const candidates = [
          `/api/teachers/${encodeURIComponent(teacherId)}`,
          `/api/teacher/${encodeURIComponent(teacherId)}`,
          `/api/users/${encodeURIComponent(teacherId)}`,
          `/api/teachers?teacherId=${encodeURIComponent(teacherId)}`
        ];
        let found: any = null;
        for (const p of candidates) {
          const d = await tryGetJson(p, token);
          if (d) { found = d; break; }
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
      setClassesLoading(true); setClassesErr(null);
      try {
        const list = await api.listMyClasses(teacherId);
        if (!aborted) setClasses(list || []);
      } catch (e: any) {
        if (!aborted) setClassesErr(e?.message ?? "반 목록을 불러오지 못했습니다.");
      } finally {
        if (!aborted) setClassesLoading(false);
      }
    })();

    return () => { aborted = true; };
  }, [teacherId, token]);

  /** 표기 원천 */
  const src = detail ?? {};

  // === 값 매핑(정확 키 우선) ===
  const TEACHER_NAME =
    pick(src, ["Teacher_Name","teacherName","name","displayName"]) ??
    user?.name ?? login?.name ?? teacherId;

  const TEACHER_ID =
    pick(src, ["Teacher_ID","teacherId","username","userId"]) ?? teacherId;

  const TEACHER_PHONE =
    pick(src, ["Teacher_Phone_Number","phoneNumber","phone","mobile"]) ?? "—";

  // 학원번호: 배열로 모두 표시
  const ACADEMY_NUMBERS: number[] = useMemo(() => {
    const v =
      pick(src, ["Academy_Number","Academy_Numbers","academyNumbers","academies"]) ??
      user?.academyNumbers ?? login?.academyNumbers ?? [];
    const arr = Array.isArray(v) ? v : (v == null ? [] : [v]);
    const nums = arr.map((x: any) => Number(x)).filter(Number.isFinite);
    return Array.from(new Set(nums));
  }, [src, user, login]);

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
          <button
            onClick={() => setOpenEdit(true)}
            className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm hover:bg-emerald-700 active:scale-[0.99] transition"
            type="button"
          >
            내 정보 수정
          </button>
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
                  ACADEMY_NUMBERS.length === 0 ? (
                    "—"
                  ) : (
                    <div className="flex flex-wrap gap-1.5">
                      {ACADEMY_NUMBERS.map((n) => (
                        <span
                          key={n}
                          className="inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium bg-gray-100 text-gray-800 ring-1 ring-gray-200"
                        >
                          #{n}
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
      </div>

      {/* 수정 모달: 기존 /settings/profile 페이지를 그대로 오버레이로 표시 */}
      <ProfileEditModal
        open={openEdit}
        onClose={() => setOpenEdit(false)}
        src="/settings/profile"
      />
    </>
  );
}
