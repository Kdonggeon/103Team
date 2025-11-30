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

// 학생 시간표와 동일한 슬롯 타입
type StudentScheduleSlot = {
  classId: string;
  className: string;
  date: string;            // "YYYY-MM-DD"
  dayOfWeek: 1 | 2 | 3 | 4 | 5 | 6 | 7; // ISO 요일(1=월..7=일) - 여기서는 date 기준으로만 사용
  roomNumber?: number | null;
  startTime: string;       // "HH:mm"
  endTime: string;         // "HH:mm"
  academyNumber?: number | null;
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

function readLogin(): LoginSession | null {
  try {
    const raw = localStorage.getItem("login");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

// 날짜/시간 헬퍼
function ymd(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(
    2,
    "0",
  )}`;
}
function dotYmd(d: Date) {
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(
    2,
    "0",
  )}`;
}
function addDays(d: Date, n: number) {
  const o = new Date(d);
  o.setDate(o.getDate() + n);
  return o;
}
function startOfWeekMonday(d: Date) {
  const day = d.getDay(); // 0=Sun..6=Sat
  const diff = day === 0 ? -6 : 1 - day; // 월요일 시작
  const out = new Date(d);
  out.setDate(d.getDate() + diff);
  out.setHours(0, 0, 0, 0);
  return out;
}
function minutesFromHHMM(s?: string) {
  if (!s) return 0;
  const m = /^(\d{1,2}):(\d{2})$/.exec(String(s).trim());
  if (!m) return 0;
  const h = Number(m[1]),
    mm = Number(m[2]);
  return h * 60 + mm;
}
function hhmmFromMinutes(m: number) {
  const h = Math.floor(m / 60),
    mm = m % 60;
  return `${String(h).padStart(2, "0")}:${String(mm).padStart(2, "0")}`;
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
  let hash = 0;
  for (let i = 0; i < id.length; i++) hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
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

  React.useEffect(() => {
    setVal(ymd(value));
  }, [value]);

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

/** ===== 메인: 자녀 시간표(슬롯 기반 /timetable, 월~일, 07~22) ===== */
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

  // 시간표 슬롯 (/timetable 결과)
  const [slots, setSlots] = useState<StudentScheduleSlot[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 보기 날짜 (주간 기준점)
  const [viewDate, setViewDate] = useState<Date>(() => new Date());

  // 캘린더 팝오버
  const [pickerOpen, setPickerOpen] = useState(false);
  const pickerBtnRef = useRef<HTMLButtonElement | null>(null);

  // ----- 자녀 목록 로딩 -----
  useEffect(() => {
    if (!parentId) return;
    let aborted = false;
    (async () => {
      setKidsLoading(true);
      setKidsErr(null);
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
    return () => {
      aborted = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [parentId]);

  // ----- 주간 기준(월요일) 및 문자열 -----
  const weekStartDate = useMemo(() => startOfWeekMonday(viewDate), [viewDate]);
  const weekStartStr = useMemo(() => ymd(weekStartDate), [weekStartDate]);
  const weekDates = useMemo(() => {
    const out: Date[] = [];
    for (let i = 0; i < 7; i++) out.push(addDays(weekStartDate, i));
    return out;
  }, [weekStartDate]);
  const weekRangeText = `${dotYmd(weekDates[0])} ~ ${dotYmd(weekDates[6])}`;

  // ----- 선택 자녀 + 주간 기준에 맞춰 /timetable 호출 -----
  useEffect(() => {
    if (!selectedChild) {
      setSlots([]);
      setErr(null);
      return;
    }
    let aborted = false;
    (async () => {
      setLoading(true);
      setErr(null);
      try {
        // 학생 시간표와 동일한 엔드포인트 사용
        const raw = await apiGet<StudentScheduleSlot[]>(
          `/api/students/${encodeURIComponent(
            selectedChild,
          )}/timetable?weekStart=${weekStartStr}&days=7`,
          token,
        );
        if (aborted) return;
        setSlots(Array.isArray(raw) ? raw : []);
      } catch (e: any) {
        if (!aborted) {
          setErr(e?.message ?? "시간표를 불러오지 못했습니다.");
          setSlots([]);
        }
      } finally {
        if (!aborted) setLoading(false);
      }
    })();
    return () => {
      aborted = true;
    };
  }, [selectedChild, token, weekStartStr]);

  /** ===== 화면 구성 데이터 ===== */

  // 시간 축: 07:00 ~ 22:00 고정
  const MIN_START = 7 * 60;
  const MAX_END = 22 * 60;
  const hours = useMemo(() => {
    const out: number[] = [];
    for (let t = MIN_START; t <= MAX_END; t += 60) out.push(t);
    return out;
  }, []);
  const PX_PER_MIN = 1;
  const GRID_HEIGHT = (MAX_END - MIN_START) * PX_PER_MIN;

  type SlotWithLayout = StudentScheduleSlot & { top: number; height: number };

  // 날짜별 슬롯 매핑 (해당 주 범위에 포함되는 것만)
  const byDate = useMemo(() => {
    const m = new Map<string, SlotWithLayout[]>();
    const weekEndDate = addDays(weekStartDate, 6);

    for (const s of slots) {
      if (!s.date) continue;
      const d = new Date(`${s.date}T00:00:00`);
      if (d < weekStartDate || d > weekEndDate) continue;

      const st = minutesFromHHMM(s.startTime);
      const et = minutesFromHHMM(s.endTime);
      const clampedStart = Math.max(st, MIN_START);
      const clampedEnd = Math.min(et || MIN_START + 60, MAX_END);
      const top = (clampedStart - MIN_START) * PX_PER_MIN;
      const height = Math.max(24, (clampedEnd - clampedStart) * PX_PER_MIN);

      const key = s.date;
      if (!m.has(key)) m.set(key, []);
      m.get(key)!.push({ ...s, top, height });
    }

    // 각 날짜별 슬롯을 시작시간 기준 정렬
    for (const [key, list] of m) {
      list.sort((a, b) => minutesFromHHMM(a.startTime) - minutesFromHHMM(b.startTime));
      m.set(key, list);
    }

    return m;
  }, [slots, weekStartDate]);

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
              setViewDate(picked); // 해당 날짜가 포함된 주로 자동 반영
            }}
          />
        </div>
      </div>

      {/* 자녀 선택 (기존 스피너/드롭다운 유지) */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="block text-xs text-gray-700">대상 자녀</label>
            {kidsLoading ? (
              <div className="mt-1">
                <Spinner label="자녀 목록 불러오는 중" />
              </div>
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

      {/* 시간표 그리드 (월~일 / 07~22) - 슬롯 기반 */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm">
        {/* 헤더 행: 시간축 빈칸 + 요일/날짜 (월~일) */}
        <div
          className="grid border-b border-gray-200"
          style={{ gridTemplateColumns: `64px repeat(7, minmax(0,1fr))` }}
        >
          <div className="h-12" />
          {weekDates.map((date, i) => (
            <div key={`head-${i}`} className="h-12 flex flex-col items-center justify-center">
              <div className="text-sm font-semibold text-gray-900">
                {["월", "화", "수", "목", "금", "토", "일"][i]}
              </div>
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
              const top = (t - MIN_START) * PX_PER_MIN;
              return (
                <div key={`time-${i}`} className="absolute left-0 right-0" style={{ top }}>
                  <div className="text-[11px] text-gray-700 -translate-y-2">{hhmmFromMinutes(t)}</div>
                </div>
              );
            })}
          </div>

          {/* 각 요일 칼럼: weekDates 기준으로 날짜별로 매핑 */}
          {weekDates.map((date, idx) => {
            const dateKey = ymd(date);
            const list = byDate.get(dateKey) ?? [];
            return (
              <div
                key={`col-${dateKey}`}
                className="relative border-l border-gray-200"
                style={{ height: GRID_HEIGHT }}
              >
                {/* 시간 가이드 라인 */}
                {hours.map((t, i) => {
                  const top = (t - MIN_START) * PX_PER_MIN;
                  return (
                    <div
                      key={`line-${dateKey}-${i}`}
                      className="absolute left-0 right-0 border-t border-gray-100"
                      style={{ top }}
                    />
                  );
                })}

                {/* 수업 블록 (슬롯) */}
                {list.map((c) => (
                  <div
                    key={`${c.classId}-${c.date}-${c.startTime}-${c.endTime}`}
                    className={`absolute left-1 right-1 rounded-lg ring-1 p-2 text-xs font-medium shadow-sm ${colorFor(
                      c.classId,
                    )}`}
                    style={{ top: c.top, height: c.height, minHeight: 24 }}
                    title={`${c.className} | ${c.startTime ?? "??:??"} ~ ${c.endTime ?? "??:??"}`}
                  >
                    <div className="truncate">{c.className}</div>
                    <div className="text-[11px] opacity-80">
                      {c.startTime ?? "??:??"}~{c.endTime ?? "??:??"}
                      {typeof c.roomNumber === "number" ? ` · ${c.roomNumber}실` : ""}
                    </div>
                  </div>
                ))}
              </div>
            );
          })}
        </div>

        {/* 로딩/오류/빈 상태 */}
        <div className="p-3">
          {loading && <Spinner label="시간표 불러오는 중" />}
          {err && <div className="text-sm text-red-600">오류: {err}</div>}
          {!loading && !err && slots.length === 0 && (
            <div className="text-sm text-gray-700">표시할 수업이 없습니다.</div>
          )}
        </div>
      </div>
    </div>
  );
}
