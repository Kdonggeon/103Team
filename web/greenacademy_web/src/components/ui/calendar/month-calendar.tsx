// src/components/ui/calendar/month-calendar.tsx
"use client";
import { useMemo } from "react";

/** 한국 양력 고정 공휴일(해마다 동일) */
export function krFixedSolarHolidays(year: number): Record<string, string> {
  const make = (m: number, d: number, name: string) =>
    ({ [`${year}-${String(m).padStart(2,"0")}-${String(d).padStart(2,"0")}`]: name });
  return {
    ...make(1,1,"신정"), ...make(3,1,"삼일절"), ...make(5,5,"어린이날"),
    ...make(6,6,"현충일"), ...make(8,15,"광복절"),
    ...make(10,3,"개천절"), ...make(10,9,"한글날"),
    ...make(12,25,"성탄절"),
  };
}

export default function MonthCalendar({
  year, month,
  selectedYmds = [],             // ✅ 다중선택
  onToggle,                      // ✅ 셀 클릭 시 토글
  eventCountByDate,
  holidays,
  showWeekendColors = true,
  className,
}: {
  year: number;
  month: number; // 1~12
  selectedYmds?: string[];
  onToggle?: (ymd: string) => void;
  eventCountByDate?: Record<string, number>;
  holidays?: Record<string, string>;
  showWeekendColors?: boolean;
  className?: string;
}) {
  const { weeks } = useMemo(() => buildCalendar(year, month), [year, month]);
  const holidaySet = holidays ?? {};
  const selectedSet = useMemo(() => new Set(selectedYmds), [selectedYmds]);

  return (
    <div className={["w-full", className].filter(Boolean).join(" ")}>
      {/* 요일 헤더 */}
      <div className="grid grid-cols-7 text-center text-sm text-slate-700 px-2">
        {["월","화","수","목","금","토","일"].map((d, i) => (
          <div
            key={d}
            className={[
              "py-2 font-semibold",
              showWeekendColors && i === 5 ? "text-blue-600"
              : showWeekendColors && i === 6 ? "text-rose-600"
              : "text-slate-700",
            ].join(" ")}
          >
            {d}
          </div>
        ))}
      </div>

      {/* 날짜 그리드 */}
      <div className="grid grid-rows-6 gap-px bg-slate-200 rounded-2xl overflow-hidden ring-1 ring-black/5">
        {weeks.map((week, wi) => (
          <div key={wi} className="grid grid-cols-7 gap-px bg-slate-200">
            {week.map((cell, di) => {
              const ymd = cell.ymd;
              const isCurrentMonth = cell.inMonth;
              const isSelected = selectedSet.has(ymd);
              const cnt = eventCountByDate?.[ymd] ?? 0;

              const isHoliday = !!holidaySet[ymd];
              const weekendClass =
                showWeekendColors && (di === 5 || di === 6)
                  ? (di === 6 ? "text-rose-600" : "text-blue-600")
                  : "";
              const colorClass = isHoliday ? "text-rose-600" : weekendClass;
              const isToday = ymd === toYmd(new Date());

              return (
                <button
                  key={di}
                  type="button"
                  onClick={() => onToggle?.(ymd)}
                  title={ymd}
                  className={[
                    "relative text-left bg-white p-2 h-[96px] focus:outline-none",
                    isCurrentMonth ? "text-slate-900" : "text-slate-400",
                    isSelected ? "ring-2 ring-emerald-500 z-[1]" : "hover:bg-emerald-50",
                  ].join(" ")}
                >
                  <div className="flex items-start justify-between">
                    <span className={["text-xs leading-none font-medium", colorClass].join(" ")}>{cell.day}</span>
                  </div>

                  {isHoliday && (
                    <div className="mt-1 inline-flex items-center px-1.5 py-0.5 rounded bg-rose-50 text-[10px] text-rose-600 ring-1 ring-rose-100">
                      {holidaySet[ymd]}
                    </div>
                  )}
                  {cnt > 0 && (
                    <div className="absolute bottom-1 right-1 text-[10px] px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                      {cnt}개
                    </div>
                  )}
                  {isToday && !isSelected && (
                    <div className="absolute inset-0 pointer-events-none outline outline-1 outline-emerald-300 rounded-md" />
                  )}
                </button>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}

/** 6주 셀 계산 (월요일 시작) */
function buildCalendar(year: number, month1to12: number) {
  const first = new Date(year, month1to12 - 1, 1);
  const jsDow = first.getDay();           // 0=Sun
  const offset = (jsDow + 6) % 7;         // Monday-first
  const end = new Date(year, month1to12, 0).getDate();
  const prevEnd = new Date(year, month1to12 - 1, 0).getDate();

  const cells: { ymd: string; inMonth: boolean; day: number }[] = [];
  for (let i=offset; i>0; i--) {
    const d = new Date(year, month1to12 - 2, prevEnd - i + 1);
    cells.push({ ymd: toYmd(d), inMonth: false, day: d.getDate() });
  }
  for (let day=1; day<=end; day++) {
    const d = new Date(year, month1to12 - 1, day);
    cells.push({ ymd: toYmd(d), inMonth: true, day });
  }
  const remain = 42 - cells.length;
  for (let day=1; day<=remain; day++) {
    const d = new Date(year, month1to12, day);
    cells.push({ ymd: toYmd(d), inMonth: false, day });
  }

  const weeks: typeof cells[] = [];
  for (let i=0; i<6; i++) weeks.push(cells.slice(i*7, i*7 + 7));
  return { weeks };
}

function toYmd(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2,"0");
  const dd = String(d.getDate()).padStart(2,"0");
  return `${y}-${m}-${dd}`;
}
