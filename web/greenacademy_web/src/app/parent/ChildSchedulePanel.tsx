"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";

/** ===== 타입 ===== */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = {
  role: Role;
  username: string;
  name?: string;
  token?: string;
  childStudentId?: string | null;
};
type ChildSummary = {
  studentId: string;
  studentName?: string | null;
  academies?: number[];
};
type ClassInfo = {
  classId: string;
  className: string;
  teacherId?: string;
  roomNumber?: string | number;
  startTime?: string; // "09:00"
  endTime?: string;   // "10:00"
  daysOfWeek: number[]; // 0(Sun) ~ 6(Sat)
};

/** ===== 유틸 ===== */
const API_BASE = process.env.NEXT_PUBLIC_API_BASE?.trim() || "/backend";

async function apiGet<T>(path: string, token?: string): Promise<T> {
  const r = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    cache: "no-store",
  });
  if (!r.ok) {
    const t = await r.text().catch(() => "");
    throw new Error(`${r.status} ${r.statusText}${t ? ` | ${t}` : ""}`);
  }
  return r.json();
}
async function apiGetWithFallback<T>(primary: string, token?: string, fallback?: string) {
  try { return await apiGet<T>(primary, token); }
  catch (e) { if (!fallback) throw e; return await apiGet<T>(fallback, token); }
}
function readLogin(): LoginSession | null {
  try { const raw = localStorage.getItem("login"); return raw ? JSON.parse(raw) : null; }
  catch { return null; }
}

// 날짜/시간 헬퍼
function ymd(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`;
}
function dotYmd(d: Date) {
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,"0")}.${String(d.getDate()).padStart(2,"0")}`;
}
function addDays(d: Date, n: number) { const o = new Date(d); o.setDate(o.getDate()+n); return o; }
function startOfWeekMonday(d: Date) {
  const day = d.getDay(); // 0=Sun..6=Sat
  const diff = (day === 0 ? -6 : 1 - day); // 월요일 시작
  const out = new Date(d); out.setDate(d.getDate()+diff); out.setHours(0,0,0,0); return out;
}
function minutesFromHHMM(s?: string) {
  if (!s) return 0;
  const m = /^(\d{1,2}):(\d{2})$/.exec(String(s).trim());
  if (!m) return 0;
  const h = Number(m[1]), mm = Number(m[2]);
  return h*60 + mm;
}
function hhmmFromMinutes(m: number) {
  const h = Math.floor(m/60), mm = m % 60;
  return `${String(h).padStart(2,"0")}:${String(mm).padStart(2,"0")}`;
}

// 요일 정규화 (1~7도 허용, 7=일=0)
function dayToNum(v: any): number | null {
  const s = String(v ?? "").trim().toUpperCase();
  const n = Number(s);
  if (Number.isFinite(n)) {
    if (n >= 0 && n <= 6) return n;           // 0~6 직접
    if (n >= 1 && n <= 7) return n === 7 ? 0 : n; // 1~7 → 7은 0(일요일)
  }
  if (["SUN","일"].some(k=>s.includes(k))) return 0;
  if (["MON","월"].some(k=>s.includes(k))) return 1;
  if (["TUE","화"].some(k=>s.includes(k))) return 2;
  if (["WED","수"].some(k=>s.includes(k))) return 3;
  if (["THU","목"].some(k=>s.includes(k))) return 4;
  if (["FRI","금"].some(k=>s.includes(k))) return 5;
  if (["SAT","토"].some(k=>s.includes(k))) return 6;
  return null;
}
function normalizeDays(any: any): number[] {
  // 7칸 불리언/0/1 배열도 인식
  if (
    Array.isArray(any) &&
    any.length === 7 &&
    any.every(v => v === true || v === false || v === 0 || v === 1 || v === "0" || v === "1")
  ) {
    return any.map((v,i)=>(v===true||v===1||v==="1")?i:null).filter((n): n is number => n!==null);
  }
  const arr = Array.isArray(any) ? any : [any];
  return arr.map(dayToNum).filter((n): n is number => typeof n === "number");
}
function normalizeClass(raw: any): ClassInfo | null {
  const classId   = raw?.Class_ID ?? raw?.classId ?? raw?.ClassId ?? raw?.id ?? raw?.Class_No ?? raw?.class_no;
  const className = raw?.Class_Name ?? raw?.className ?? raw?.name ?? raw?.Title ?? raw?.title;
  if (!classId || !className) return null;
  const days = normalizeDays(raw?.Days_Of_Week ?? raw?.daysOfWeek ?? raw?.days ?? raw?.Days ?? []);
  return {
    classId: String(classId),
    className: String(className),
    teacherId: raw?.Teacher_ID ?? raw?.teacherId ?? raw?.teacher ?? undefined,
    roomNumber: raw?.roomNumber ?? raw?.Room ?? undefined,
    startTime: raw?.Start_Time ?? raw?.startTime ?? undefined,
    endTime: raw?.End_Time ?? raw?.endTime ?? undefined,
    daysOfWeek: days,
  };
}

// 색상(해시 고정)
const COLORS = [
  "bg-emerald-200/70 text-emerald-950 ring-emerald-300",
  "bg-sky-200/70 text-sky-950 ring-sky-300",
  "bg-amber-200/70 text-amber-950 ring-amber-300",
  "bg-violet-200/70 text-violet-950 ring-violet-300",
  "bg-rose-200/70 text-rose-950 ring-rose-300",
  "bg-lime-200/70 text-lime-950 ring-lime-300",
  "bg-cyan-200/70 text-cyan-950 ring-cyan-300",
  "bg-fuchsia-200/70 text-fuchsia-950 ring-fuchsia-300",
];
function colorFor(id: string) {
  let hash = 0; for (let i=0;i<id.length;i++) hash = (hash*31 + id.charCodeAt(i)) >>> 0;
  return COLORS[hash % COLORS.length];
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

/** ===== 출결과 같은 스타일의 네이티브 date 팝오버 ===== */
function MiniDatePicker({
  open,
  anchorRef,
  value,
  onCancel,
  onConfirm,
}: {
  open: boolean;
  anchorRef: React.RefObject<HTMLElement | null>;
  value: Date;
  onCancel: () => void;
  onConfirm: (picked: Date) => void;
}) {
  const [pos, setPos] = React.useState<{ top: number; left: number }>({ top: 0, left: 0 });
  const [val, setVal] = React.useState<string>(() => ymd(value));

  React.useEffect(() => { setVal(ymd(value)); }, [value]);

  React.useEffect(() => {
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
      <div
        className="absolute z-50 rounded-xl bg-white shadow-lg ring-1 ring-black/5 p-3 w-[260px]"
        style={{ top: pos.top, left: pos.left }}
        role="dialog"
        aria-modal="true"
      >
        <div className="text-sm font-medium text-gray-900 mb-2">날짜 선택</div>
        <input
          type="date"
          value={val}
          onChange={(e) => setVal(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-black"
        />
        <div className="mt-3 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50 active:scale-[0.99]"
          >
            취소
          </button>
          <button
            type="button"
            onClick={() => {
              const picked = new Date(`${val}T12:00:00`);
              if (isNaN(picked.getTime())) return onCancel();
              onConfirm(picked);
            }}
            className="px-3 py-1.5 rounded-lg bg-gray-900 text-white hover:bg-black active:scale-[0.99]"
          >
            확인
          </button>
        </div>
      </div>
    </>
  );
}

/** ===== 메인: 자녀 시간표(주간, 월~일, 07~22 고정) ===== */
export default function StudentTimetablePanel() {
  const login = readLogin();
  const token = login?.token ?? "";
  const parentId = login?.username ?? "";

  // 자녀 목록
  const [kids, setKids] = useState<ChildSummary[]>([]);
  const [kidsLoading, setKidsLoading] = useState(false);
  const [kidsErr, setKidsErr] = useState<string | null>(null);

  // 선택 자녀
  const [selectedChild, setSelectedChild] = useState<string | null>(login?.childStudentId ?? null);

  // 수업(시간표)
  const [classes, setClasses] = useState<ClassInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 보기 날짜 (주간 기준점)
  const [viewDate, setViewDate] = useState<Date>(() => new Date());

  // 캘린더 팝오버
  const [pickerOpen, setPickerOpen] = useState(false);
  const pickerBtnRef = useRef<HTMLButtonElement | null>(null);

  // 자녀 목록
  useEffect(() => {
    if (!parentId) return;
    let aborted = false;
    (async () => {
      setKidsLoading(true); setKidsErr(null);
      try {
        const list = await apiGet<ChildSummary[]>(`/api/parents/${encodeURIComponent(parentId)}/children`, token);
        if (aborted) return;
        setKids(Array.isArray(list) ? list : []);
        if (!selectedChild) setSelectedChild(list?.[0]?.studentId ?? null);
      } catch (e: any) {
        if (!aborted) setKidsErr(e?.message ?? "자녀 목록을 불러오지 못했습니다.");
      } finally {
        if (!aborted) setKidsLoading(false);
      }
    })();
    return () => { aborted = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [parentId]);

  // 선택 자녀 시간표
  useEffect(() => {
    if (!selectedChild) { setClasses([]); setErr(null); return; }
    let aborted = false;
    (async () => {
      setLoading(true); setErr(null);
      try {
        const raw = await apiGetWithFallback<any[]>(
          `/api/students/${encodeURIComponent(selectedChild)}/classes`,
          token,
          `/api/classes?studentId=${encodeURIComponent(selectedChild)}`
        );
        if (aborted) return;
        const list = (Array.isArray(raw) ? raw : [])
          .map(normalizeClass)
          .filter((x): x is ClassInfo => !!x);
        setClasses(list);
      } catch (e: any) {
        if (!aborted) { setErr(e?.message ?? "시간표를 불러오지 못했습니다."); setClasses([]); }
      } finally {
        if (!aborted) setLoading(false);
      }
    })();
    return () => { aborted = true; };
  }, [selectedChild, token]);

  /** ===== 화면 구성 데이터 ===== */
  // 고정: 월~일 (Monday-first)
  const dayOrder = [1,2,3,4,5,6,0] as const; // Mon..Sun

  // 주간 날짜들 (월~일)
  const weekStart = useMemo(() => startOfWeekMonday(viewDate), [viewDate]);
  const weekDates = useMemo(() => dayOrder.map((_,i)=> addDays(weekStart, i)), [weekStart]);
  const weekRangeText = `${dotYmd(weekDates[0])} ~ ${dotYmd(weekDates[6])}`;

  // 시간 축: 07:00 ~ 22:00 고정
  const MIN_START = 7 * 60;
  const MAX_END   = 22 * 60;
  const hours = useMemo(() => {
    const out: number[] = [];
    for (let t = MIN_START; t <= MAX_END; t += 60) out.push(t);
    return out;
  }, []);
  const PX_PER_MIN = 1;
  const GRID_HEIGHT = (MAX_END - MIN_START) * PX_PER_MIN;

  // 요일별 수업 매핑 + 높이/위치 계산
  const byDay = useMemo(() => {
    const map = new Map<number, Array<ClassInfo & { top: number; height: number }>>();
    dayOrder.forEach(d => map.set(d, []));
    for (const c of classes) {
      const st = minutesFromHHMM(c.startTime);
      const et = minutesFromHHMM(c.endTime);
      const clampedStart = Math.max(st, MIN_START);
      const clampedEnd = Math.min(et || MIN_START + 60, MAX_END);
      const top = (clampedStart - MIN_START) * PX_PER_MIN;
      const height = Math.max(24, (clampedEnd - clampedStart) * PX_PER_MIN);
      c.daysOfWeek.forEach(d => {
        if (!map.has(d)) map.set(d, []);
        map.get(d)!.push({ ...c, top, height });
      });
    }
    for (const d of map.keys()) {
      map.set(d, map.get(d)!.sort((a,b)=> minutesFromHHMM(a.startTime) - minutesFromHHMM(b.startTime)));
    }
    return map;
  }, [classes]);

  /** ===== 렌더 ===== */
  return (
    <div className="space-y-4">
      {/* 헤더 */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg md:text-xl font-semibold text-gray-900">자녀 시간표</h2>
          <p className="text-sm text-gray-700 mt-1">
            보고 있는 날짜: <b>{ymd(viewDate)}</b> · 주간: <b>{weekRangeText}</b>
          </p>
        </div>

        {/* 날짜 컨트롤 (출결과 같은 팝오버 캘린더) */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50 active:scale-[0.99]"
            onClick={() => setViewDate(addDays(viewDate, -7))}
            aria-label="이전 주"
          >
            ← 이전 주
          </button>
          <button
            type="button"
            className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50 active:scale-[0.99]"
            onClick={() => setViewDate(new Date())}
          >
            오늘
          </button>
          <button
            type="button"
            className="px-3 py-1.5 rounded-lg ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50 active:scale-[0.99]"
            onClick={() => setViewDate(addDays(viewDate, 7))}
            aria-label="다음 주"
          >
            다음 주 →
          </button>

          <button
            ref={pickerBtnRef}
            type="button"
            className="px-3 py-1.5 rounded-lg bg-gray-900 text-white hover:bg-black active:scale-[0.99]"
            onClick={() => setPickerOpen(true)}
          >
            날짜 선택
          </button>

          <MiniDatePicker
            open={pickerOpen}
            anchorRef={pickerBtnRef as unknown as React.RefObject<HTMLElement | null>}
            value={viewDate}
            onCancel={() => setPickerOpen(false)}
            onConfirm={(picked) => {
              setPickerOpen(false);
              setViewDate(picked); // 해당 날짜 주간으로 자동 반영
            }}
          />
        </div>
      </div>

      {/* 자녀 선택 */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="block text-xs text-gray-700">대상 자녀</label>
            {kidsLoading ? (
              <div className="mt-1"><Spinner label="자녀 목록 불러오는 중" /></div>
            ) : (
              <select
                className="mt-1 w-56 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-black"
                value={selectedChild ?? ""}
                onChange={(e) => setSelectedChild(e.target.value || null)}
              >
                <option value="">{kids.length === 0 ? "선택할 자녀가 없습니다" : "선택하세요"}</option>
                {kids.map((c) => (
                  <option key={c.studentId} value={c.studentId}>
                    {c.studentName ?? c.studentId} ({c.studentId})
                  </option>
                ))}
              </select>
            )}
          </div>
          {kidsErr && <span className="text-sm text-red-600">{kidsErr}</span>}
        </div>
      </div>

      {/* 시간표 그리드 (월~일 / 07~22) */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm">
        {/* 헤더 행: 시간축 빈칸 + 요일/날짜 (월~일) */}
        <div
          className="grid border-b border-gray-200"
          style={{ gridTemplateColumns: `64px repeat(7, minmax(0,1fr))` }}
        >
          <div className="h-12" />
          {weekDates.map((date, i) => (
            <div key={`head-${i}`} className="h-12 flex flex-col items-center justify-center">
              <div className="text-sm font-semibold text-gray-900">{["월","화","수","목","금","토","일"][i]}</div>
              <div className="text-[11px] text-gray-600 -mt-0.5">{ymd(date)}</div>
            </div>
          ))}
        </div>

        {/* 본문: 시간(좌) + 요일 칼럼들(우) */}
        <div
          className="grid"
          style={{ gridTemplateColumns: `64px repeat(7, minmax(0,1fr))`, columnGap: `8px` }}
        >
          {/* 왼쪽 시간 축 */}
          <div className="relative" style={{ height: GRID_HEIGHT }}>
            {hours.map((t, i) => {
              const top = (t - MIN_START) * 1;
              return (
                <div key={`time-${i}`} className="absolute left-0 right-0" style={{ top }}>
                  <div className="text-[11px] text-gray-700 -translate-y-2">{hhmmFromMinutes(t)}</div>
                </div>
              );
            })}
          </div>

          {/* 각 요일 칼럼 */}
          {[1,2,3,4,5,6,0].map((d) => (
            <div key={`col-${d}`} className="relative border-l border-gray-200" style={{ height: GRID_HEIGHT }}>
              {/* 시간 가이드 라인 */}
              {hours.map((t, i) => {
                const top = (t - MIN_START) * 1;
                return (
                  <div key={`line-${d}-${i}`} className="absolute left-0 right-0 border-t border-gray-100" style={{ top }} />
                );
              })}

              {/* 수업 블록 */}
              {byDay.get(d)?.map((c) => (
                <div
                  key={`${c.classId}-${c.startTime}-${c.endTime}`}
                  className={`absolute left-1 right-1 rounded-lg ring-1 p-2 text-xs font-medium shadow-sm ${colorFor(c.classId)}`}
                  style={{ top: c.top, height: c.height, minHeight: 24 }}
                  title={`${c.className} | ${c.startTime ?? "??:??"} ~ ${c.endTime ?? "??:??"}`}
                >
                  <div className="truncate">{c.className}</div>
                  <div className="text-[11px] opacity-80">
                    {c.startTime ?? "??:??"}~{c.endTime ?? "??:??"}
                    {typeof c.roomNumber !== "undefined" ? ` · ${c.roomNumber}실` : ""}
                    {c.teacherId ? ` · ${c.teacherId}` : ""}
                  </div>
                </div>
              ))}
            </div>
          ))}
        </div>

        {/* 로딩/오류/빈 상태 */}
        <div className="p-3">
          {loading && <Spinner label="시간표 불러오는 중" />}
          {err && <div className="text-sm text-red-600">오류: {err}</div>}
          {!loading && !err && classes.length === 0 && (
            <div className="text-sm text-gray-700">표시할 수업이 없습니다.</div>
          )}
        </div>
      </div>
    </div>
  );
}
