// src/app/student/StudentProfileCard.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

/** ===== 타입/유틸 ===== */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = { role: Role; username: string; name?: string; token?: string; academyNumbers?: number[] };
type StudentDetail = Record<string, any>;

const API_BASE = process.env.NEXT_PUBLIC_API_BASE?.trim() || "/backend";

async function apiGet<T>(path: string, token?: string): Promise<T> {
  const r = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    cache: "no-store",
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}
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
function pick(obj: StudentDetail, keys: string[]) {
  for (const k of keys) if (k in obj && !isEmpty(obj[k])) return obj[k];
  return undefined;
}
function formatAcademies(v: any) {
  const arr = Array.isArray(v) ? v : [v];
  const nums = arr.map((x) => Number(x)).filter(Number.isFinite);
  return nums.length ? nums.map((n) => `#${n}`).join(", ") : (isEmpty(v) ? "" : String(v));
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

/** ===== 메인 ===== */
export default function StudentProfileCard() {
  const router = useRouter();
  const login = readLogin();
  const token = login?.token ?? "";
  const studentId = login?.username ?? "";

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [detail, setDetail] = useState<StudentDetail | null>(null);

  useEffect(() => {
    if (!studentId) return;
    let aborted = false;
    (async () => {
      setLoading(true); setErr(null);
      try {
        const d = await apiGet<StudentDetail>(`/api/students/${encodeURIComponent(studentId)}`, token);
        if (!aborted) setDetail(d ?? null);
      } catch (e: any) {
        if (!aborted) setErr(e?.message ?? "정보를 불러오지 못했습니다.");
      } finally {
        if (!aborted) setLoading(false);
      }
    })();
    return () => { aborted = true; };
  }, [studentId, token]);

  const academies: number[] = useMemo(() => {
    const v = detail?.Academy_Numbers ?? detail?.academyNumbers ?? detail?.academies ?? login?.academyNumbers ?? [];
    const arr = Array.isArray(v) ? v : [v];
    return Array.from(new Set(arr.map((x) => Number(x)).filter(Number.isFinite)));
  }, [detail, login]);

  const entries = useMemo(() => {
    if (!detail) return [];
    const out: { label: string; value: string }[] = [];
    const push = (label: string, raw: any, fmt?: (v:any)=>string) => {
      if (raw === undefined) return;
      const value = fmt ? fmt(raw) : (typeof raw === "object" ? JSON.stringify(raw) : String(raw));
      if (!isEmpty(value)) out.push({ label, value });
    };
    push("이름", pick(detail, ["Student_Name","studentName","student_name","name"]));
    push("주소", pick(detail, ["Student_Address","studentAddress","student_address","address"]));
    push("핸드폰 번호", pick(detail, ["Student_Phone_Number","studentPhoneNumber","student_phone_number","phone"]));
    push("학교", pick(detail, ["School","school","schoolName","school_name"]));
    push("학년", pick(detail, ["Grade","grade","year"]));
    push("성별", pick(detail, ["Gender","gender","sex"]));
    // 학원번호는 '보기만' — 관리(추가/삭제)는 다른 화면에서
    push("학원번호", academies, formatAcademies);
    return out;
  }, [detail, academies]);

  return (
    <div className="space-y-5">
      {/* 헤더 */}
      <div className="rounded-2xl bg-gradient-to-r from-emerald-50 to-white ring-1 ring-black/5 p-5 md:p-6 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 ring-1 ring-emerald-200 text-emerald-700 font-semibold">
            {login?.name?.[0]?.toUpperCase() ?? login?.username?.[0]?.toUpperCase() ?? "S"}
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">내 정보</h2>
            <div className="text-sm text-gray-600">아이디: {studentId}</div>
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

      {/* 본문 */}
      <section className="rounded-2xl border bg-white p-5 md:p-6 shadow-sm ring-1 ring-black/5">
        {loading ? (
          <Spinner label="불러오는 중…" />
        ) : err ? (
          <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {err}</div>
        ) : !detail ? (
          <div className="text-sm text-gray-700">표시할 학생 정보가 없습니다.</div>
        ) : entries.length === 0 ? (
          <div className="text-sm text-gray-700">표시할 학생 정보가 없습니다.</div>
        ) : (
          <div className="grid grid-cols-1 gap-3">{entries.map((e, i) => <Info key={i} label={e.label} value={e.value} />)}</div>
        )}
      </section>
    </div>
  );
}
