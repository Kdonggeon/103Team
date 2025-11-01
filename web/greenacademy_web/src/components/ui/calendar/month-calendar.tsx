"use client";

import React, { useMemo } from "react";

export type MonthEvent = {
  id: string;
  date: string;        // "YYYY-MM-DD"
  title: string;
  classId?: string;
  startTime?: string;
  endTime?: string;
  roomNumber?: number;
  color?: string;      // optional chip bg color
};

export type Holiday = { date: string; name: string };

export type MonthCalendarProps = {
  year: number;
  month: number; // 1~12
  events: MonthEvent[];
  holidays?: Holiday[];
  selectedDate?: string;
  onDayClick?: (date: string) => void;
  onPrevMonth?: () => void;
  onNextMonth?: () => void;
};

// ✅ 로컬 기준으로 안전하게 YYYY-MM-DD 만들기
const ymd = (d: Date) => {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
};

const same = (a?: string, b?: string) => !!a && !!b && a === b;

export default function MonthCalendar({
  year,
  month,
  events,
  holidays = [],
  selectedDate,
  onDayClick,
  onPrevMonth,
  onNextMonth,
}: MonthCalendarProps) {
  const first = new Date(year, month - 1, 1); // ✅ 로컬 생성자 사용
  const firstDow = first.getDay(); // 0~6
  const last = new Date(year, month, 0);
  const daysInMonth = last.getDate();

  // ✅ 로컬 기준으로 6주 그리드(42칸) 구성
  const cells = useMemo(() => {
    const arr: { date: string; inMonth: boolean; dow: number }[] = [];
    // 전월 채우기
    for (let i = 0; i < firstDow; i++) {
      const d = new Date(year, month - 1, -(firstDow - i - 1));
      arr.push({ date: ymd(d), inMonth: false, dow: d.getDay() });
    }
    // 이번달
    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(year, month - 1, day);
      arr.push({ date: ymd(d), inMonth: true, dow: d.getDay() });
    }
    // 다음달 채우기 (총 42칸)
    while (arr.length < 42) {
      const lastDate = new Date(arr[arr.length - 1].date + "T00:00:00"); // ✅ 로컬기준
      lastDate.setDate(lastDate.getDate() + 1);
      arr.push({ date: ymd(lastDate), inMonth: false, dow: lastDate.getDay() });
    }
    return arr;
  }, [year, month, daysInMonth, firstDow]);

  const evByDate = useMemo(() => {
    const m = new Map<string, MonthEvent[]>();
    for (const e of events) {
      const a = m.get(e.date) ?? [];
      a.push(e);
      m.set(e.date, a);
    }
    return m;
  }, [events]);

  const holidaySet = useMemo(() => new Set(holidays.map((h) => h.date)), [holidays]);
  const today = ymd(new Date());

  return (
    <div className="w-full">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-3">
        <button
          type="button"
          onClick={onPrevMonth}
          className="px-2 py-1 rounded border border-black bg-white hover:bg-gray-50"
        >
          ◀
        </button>
        <div className="text-lg font-semibold">{year}년 {month}월</div>
        <button
          type="button"
          onClick={onNextMonth}
          className="px-2 py-1 rounded border border-black bg-white hover:bg-gray-50"
        >
          ▶
        </button>
      </div>

      {/* 요일 */}
      <div className="grid grid-cols-7 text-center text-sm font-semibold mb-1">
        {["일","월","화","수","목","금","토"].map((w, i) => (
          <div key={w} className={i===0 ? "text-red-600" : i===6 ? "text-blue-600" : "text-gray-900"}>
            {w}
          </div>
        ))}
      </div>

      {/* 캘린더 */}
      <div className="grid grid-cols-7 gap-2">
        {cells.map((c) => {
          const d = new Date(c.date + "T00:00:00");
          const n = d.getDate();
          const isHoliday = holidaySet.has(c.date) || c.dow === 0;
          const isSat = c.dow === 6;
          const numberColor = isHoliday ? "text-red-600" : isSat ? "text-blue-600" : "text-gray-900";
          const isToday = same(c.date, today);
          const isSel = same(c.date, selectedDate);

          const evs = evByDate.get(c.date) ?? [];

          return (
            <button
              key={c.date}
              type="button"
              onClick={() => onDayClick?.(c.date)}
              className={[
                "relative min-h-[96px] bg-white rounded-lg p-2 text-left",
                "border border-gray-400 hover:bg-emerald-50 transition",
                isSel ? "ring-2 ring-red-500" : isToday ? "ring-2 ring-emerald-500" : "",
              ].join(" ")}
            >
              <div className={`absolute left-2 top-1 text-xs ${numberColor}`}>{n}</div>
              <div className="mt-5 space-y-1">
                {evs.map((ev) => (
                  <div
                    key={ev.id}
                    className="w-full rounded-md border border-black px-2 py-1 text-xs font-medium truncate"
                    style={{ background: ev.color ?? "#fee2e2" }}
                    title={ev.title}
                  >
                    {ev.title}
                  </div>
                ))}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
