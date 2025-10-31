// src/app/student/StudentAttendancePanel.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";

/** 타입 */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = { role: Role; username: string; name?: string; token?: string };
type AttendanceRow = { classId: string; className: string; date: string; status: string; checkInTime?: string };

/** 유틸 */
const API_BASE = process.env.NEXT_PUBLIC_API_BASE?.trim() || "/backend";
async function apiGet<T>(path: string, token?: string): Promise<T> {
  const r = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    cache: "no-store",
  });
  if (!r.ok) { const t = await r.text().catch(()=>""); throw new Error(`${r.status} ${r.statusText}${t?` | ${t}`:""}`); }
  return r.json();
}
function readLogin(): LoginSession | null { try { const raw = localStorage.getItem("login"); return raw?JSON.parse(raw):null; } catch { return null; } }

const toYmd = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`;
function startOfMonth(d: Date) { return new Date(d.getFullYear(), d.getMonth(), 1); }
function endOfMonth(d: Date) { return new Date(d.getFullYear(), d.getMonth()+1, 0); }
function addMonths(d: Date, n: number) { return new Date(d.getFullYear(), d.getMonth()+n, 1); }
function isSameDay(a: Date, b: Date) { return a.getFullYear()===b.getFullYear() && a.getMonth()===b.getMonth() && a.getDate()===b.getDate(); }
function clampDateToMonth(d: Date, month: Date) {
  const end = endOfMonth(month).getDate();
  return new Date(month.getFullYear(), month.getMonth(), Math.min(d.getDate(), end));
}
function parseToYmd(s: string) {
  try { if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s; return toYmd(new Date(s)); } catch { return s; }
}
function statusBadgeClasses(s: string) {
  const u = (s || "").toUpperCase();
  if (u.includes("ABS")) return "bg-red-100 text-red-700 ring-red-200";
  if (u.includes("LATE")) return "bg-amber-100 text-amber-700 ring-amber-200";
  return "bg-emerald-100 text-emerald-700 ring-emerald-200";
}

/** 공통 UI */
function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-black">
      <svg className="h-4 w-4 animate-spin text-black" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-95" d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
      </svg>
      {label && <span>{label}</span>}
    </div>
  );
}

/** 미니 달력 팝오버 */
function MiniDatePicker({
  open, anchorRef, value, onCancel, onConfirm,
}: {
  open: boolean;
  anchorRef: React.RefObject<HTMLElement | null>;
  value: Date;
  onCancel: () => void;
  onConfirm: (picked: Date) => void;
}) {
  const [pos, setPos] = useState<{ top: number; left: number }>({ top: 0, left: 0 });
  const [val, setVal] = useState<string>(toYmd(value));

  useEffect(() => setVal(toYmd(value)), [value]);
  useEffect(() => {
    if (!open) return;
    const el = anchorRef.current;
    if (el) {
      const r = el.getBoundingClientRect();
      setPos({ top: r.bottom + 8 + window.scrollY, left: r.left + window.scrollX });
    } else {
      setPos({ top: 120 + window.scrollY, left: 120 + window.scrollX });
    }
  }, [open, anchorRef]);

  if (!open) return null;

  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onCancel} aria-hidden="true" />
      <div className="absolute z-50 rounded-xl bg-white shadow-lg ring-1 ring-black/5 p-3 w-[260px]"
           style={{ top: pos.top, left: pos.left }} role="dialog" aria-modal="true">
        <div className="text-sm font-medium text-gray-900 mb-2">날짜 선택</div>
        <input
          type="date"
          value={val}
          onChange={(e) => setVal(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-black"
        />
        <div className="mt-3 flex justify-end gap-2">
          <button type="button" onClick={onCancel}
                  className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50">취소</button>
          <button type="button"
                  onClick={() => { const picked = new Date(`${val}T12:00:00`); if (isNaN(picked.getTime())) return onCancel(); onConfirm(picked); }}
                  className="px-3 py-1.5 rounded-lg bg-gray-900 text-white hover:bg-black">확인</button>
        </div>
      </div>
    </>
  );
}

/** 메인 */
export default function StudentAttendancePanel() {
  const login = readLogin();
  const token = login?.token ?? "";
  const studentId = login?.username ?? "";

  const [rows, setRows] = useState<AttendanceRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [month, setMonth] = useState<Date>(() => startOfMonth(new Date()));
  const [selectedDate, setSelectedDate] = useState<Date>(() => new Date());

  // 달력 팝오버
  const [pickerOpen, setPickerOpen] = useState(false);
  const pickerBtnRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    if (!studentId) return;
    let aborted = false;
    (async () => {
      setLoading(true); setErr(null);
      try {
        const data = await apiGet<AttendanceRow[]>(`/api/students/${encodeURIComponent(studentId)}/attendance`, token);
        if (!aborted) setRows(Array.isArray(data) ? data : []);
      } catch (e: any) {
        if (!aborted) { setErr(e?.message ?? "출결 정보를 불러오지 못했습니다."); setRows([]); }
      } finally {
        if (!aborted) setLoading(false);
      }
    })();
    return () => { aborted = true; };
  }, [studentId, token]);

  const byDate = useMemo(() => {
    const m = new Map<string, AttendanceRow[]>();
    for (const r of rows) {
      const key = parseToYmd(r.date);
      m.set(key, [...(m.get(key) ?? []), r]);
    }
    return m;
  }, [rows]);

  const daysForGrid = useMemo(() => {
    const start = startOfMonth(month);
    const end = endOfMonth(month);
    const first = start.getDay();
    const total = first + end.getDate();
    const rows = Math.ceil(total / 7);
    const out: Date[] = [];
    for (let i = 0; i < first; i++) out.push(new Date(start.getFullYear(), start.getMonth(), start.getDate() - (first - i)));
    for (let d = 1; d <= end.getDate(); d++) out.push(new Date(start.getFullYear(), start.getMonth(), d));
    while (out.length < rows * 7) { const last = out[out.length - 1]; out.push(new Date(last.getFullYear(), last.getMonth(), last.getDate() + 1)); }
    return out;
  }, [month]);

  const selectedYmd = toYmd(selectedDate);
  const listForSelected = byDate.get(selectedYmd) ?? [];
  const summary = useMemo(() => {
    const u = listForSelected.map(r => (r.status||"").toUpperCase());
    return {
      present: u.filter(s=>s.includes("PRESENT")).length,
      late: u.filter(s=>s.includes("LATE")).length,
      absent: u.filter(s=>s.includes("ABS")).length,
      total: u.length,
    };
  }, [listForSelected]);

  useEffect(() => { setSelectedDate(d => clampDateToMonth(d, month)); }, [month]);

  return (
    <div className="space-y-4">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg md:text-xl font-semibold text-gray-900">출결 확인</h2>
          <p className="text-sm text-gray-700 mt-1">달력에서 날짜를 눌러 해당일의 출결을 확인하세요.</p>
        </div>
      </div>

      {/* 캘린더 */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="text-base font-semibold text-gray-900">
            {month.getFullYear()}년 {month.getMonth() + 1}월
          </div>
          <div className="flex items-center gap-2">
            <button className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50"
                    onClick={() => setMonth(addMonths(month, -1))}>← 이전달</button>
            <button ref={pickerBtnRef}
                    className="px-3 py-1.5 rounded-lg bg-gray-900 text-white hover:bg-black"
                    onClick={() => setPickerOpen(true)}>날짜 선택</button>
            <button className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50"
                    onClick={() => { const t = new Date(); setMonth(startOfMonth(t)); setSelectedDate(t); }}>오늘</button>
            <button className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50"
                    onClick={() => setMonth(addMonths(month, 1))}>다음달 →</button>
          </div>
        </div>

        {/* 요일 / 날짜 그리드 */}
        <div className="grid grid-cols-7 text-center text-xs font-medium text-gray-700">
          {["일","월","화","수","목","금","토"].map((d) => <div key={d} className="py-1">{d}</div>)}
        </div>
        <div className="grid grid-cols-7 gap-1 mt-1">
          {daysForGrid.map((d, idx) => {
            const inMonth = d.getMonth() === month.getMonth();
            const ymd = toYmd(d);
            const dayRows = byDate.get(ymd) ?? [];
            const statuses = dayRows.map(r=>(r.status||"").toUpperCase());
            const has = statuses.length > 0;
            const isSel = isSameDay(d, selectedDate);
            const cellState = statuses.find(s=>s.includes("ABS")) ? "ABSENT"
              : statuses.find(s=>s.includes("LATE")) ? "LATE"
              : statuses.find(s=>s.includes("PRESENT")) ? "PRESENT" : null;
            const ring = cellState==="ABSENT"?"ring-red-300":cellState==="LATE"?"ring-amber-300":cellState==="PRESENT"?"ring-emerald-300":"ring-gray-200";

            return (
              <button key={idx} type="button" onClick={() => setSelectedDate(d)}
                className={[
                  "h-24 rounded-xl p-2 text-left transition ring-1",
                  isSel ? "bg-gray-900 text-white ring-gray-900"
                        : inMonth ? "bg-white text-gray-900 ring-gray-200 hover:bg-gray-50"
                                  : "bg-gray-50 text-gray-400 ring-gray-200",
                  !isSel && has ? `ring-2 ${ring}` : "",
                ].join(" ")}
                title={ymd}>
                <div className="text-xs font-semibold">{d.getDate()}</div>
                <div className="mt-1 flex gap-1">
                  <span className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] ring-1
                    ${statuses.some(s=>s.includes("PRESENT"))?"bg-emerald-100 text-emerald-700 ring-emerald-200":"bg-gray-100 text-gray-400 ring-gray-200"}`}>출석</span>
                  <span className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] ring-1
                    ${statuses.some(s=>s.includes("LATE"))?"bg-amber-100 text-amber-700 ring-amber-200":"bg-gray-100 text-gray-400 ring-gray-200"}`}>지각</span>
                  <span className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] ring-1
                    ${statuses.some(s=>s.includes("ABS"))?"bg-red-100 text-red-700 ring-red-200":"bg-gray-100 text-gray-400 ring-gray-200"}`}>결석</span>
                </div>
                {has && <div className="mt-2 text-[11px] opacity-80">수업 {dayRows.length}개</div>}
              </button>
            );
          })}
        </div>
      </div>

      {/* 선택일 상세 */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="text-base font-semibold text-gray-900">선택한 날짜: {selectedYmd}</div>
          <div className="flex items-center gap-2 text-sm">
            <span className="px-2.5 py-1 rounded-full ring-1 bg-emerald-100 text-emerald-700 ring-emerald-200">출석 {summary.present}</span>
            <span className="px-2.5 py-1 rounded-full ring-1 bg-amber-100 text-amber-700 ring-amber-200">지각 {summary.late}</span>
            <span className="px-2.5 py-1 rounded-full ring-1 bg-red-100 text-red-700 ring-red-200">결석 {summary.absent}</span>
          </div>
        </div>

        {loading && <Spinner label="출결 정보를 불러오는 중" />}
        {err && <div className="text-sm text-red-600">오류: {err}</div>}
        {!loading && !err && listForSelected.length === 0 && <div className="text-sm text-gray-700">이 날짜에는 표시할 수업이 없습니다.</div>}
        {!loading && !err && listForSelected.length > 0 && (
          <div className="grid grid-cols-1 gap-2">
            {listForSelected.map((r, i) => (
              <div key={`${r.classId}-${i}`} className="rounded-xl ring-1 ring-gray-200 p-3 flex items-center justify-between">
                <div className="min-w-0">
                  <div className="text-sm font-semibold text-gray-900 truncate">{r.className}</div>
                  <div className="text-xs text-gray-600">수업ID: {r.classId}</div>
                </div>
                <span className={`px-2.5 py-1 rounded-full text-[11px] font-semibold ring-1 ${statusBadgeClasses(r.status)}`}>
                  {(r.status || "").toUpperCase()}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 날짜 선택 팝오버 */}
      <MiniDatePicker
        open={pickerOpen}
        anchorRef={pickerBtnRef as React.RefObject<HTMLElement>}
        value={selectedDate}
        onCancel={() => setPickerOpen(false)}
        onConfirm={(d) => { setSelectedDate(d); setMonth(startOfMonth(d)); setPickerOpen(false); }}
      />
    </div>
  );
}
