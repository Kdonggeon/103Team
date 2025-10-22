"use client";

import React, { useMemo } from "react";

/** 월(1)~일(7) 기준 요일 + HH:mm 타임블록 */
export type CalendarEvent = {
  id: string;
  title: string;
  dayOfWeek: 1|2|3|4|5|6|7;      // 1=월 ... 7=일
  startTime: string;              // "HH:mm"
  endTime: string;                // "HH:mm"
  room?: string;
  href?: string;                  // 클릭 시 이동
};

type WeekCalendarProps = {
  startHour: number;
  endHour: number;
  events: CalendarEvent[];
  slotMinutes?: number;   // ← 추가
};


function timeToMinutes(t: string) {
  const [h, m = "0"] = t.split(":");
  return parseInt(h, 10) * 60 + parseInt(m, 10);
}

export default function WeekCalendar({
  startHour = 8,
  endHour = 22,
  events,
  className,
}: {
  startHour?: number;   // 8
  endHour?: number;     // 22
  events: CalendarEvent[];
  className?: string;
}) {
  const hours = useMemo(() => {
    const arr: number[] = [];
    for (let h = startHour; h <= endHour; h++) arr.push(h);
    return arr;
  }, [startHour, endHour]);

  const dayCols = ["월","화","수","목","금","토","일"];

  // 블록 위치 계산
  const rangeMin = startHour * 60;
  const rangeMax = endHour * 60;

  return (
    <div className={["w-full", className].filter(Boolean).join(" ")}>
      {/* 헤더 */}
      <div className="grid" style={{ gridTemplateColumns: "80px repeat(7, 1fr)" }}>
        <div />
        {dayCols.map((d,i) => (
          <div key={i} className="py-2 text-center text-sm font-medium text-gray-700">
            {d}
          </div>
        ))}
      </div>

      {/* 바디 */}
      <div className="grid border rounded-xl overflow-hidden bg-white ring-1 ring-black/5"
           style={{ gridTemplateColumns: "80px repeat(7, 1fr)" }}>
        {/* 시간 눈금 */}
        <div className="relative">
          {hours.map((h, idx) => (
            <div key={h}
                 className="h-14 border-t first:border-t-0 text-xs text-gray-500 flex items-start justify-end pr-2 pt-1">
              {String(h).padStart(2,"0")}:00
            </div>
          ))}
        </div>

        {/* 요일 7칸 */}
        {Array.from({ length: 7 }).map((_, dayIdx) => (
          <div key={dayIdx} className="relative">
            {/* 가로 그리드 라인 */}
            {hours.map((h) => (
              <div key={h} className="h-14 border-t first:border-t-0" />
            ))}

            {/* 이벤트 블록 */}
            {events
              .filter(e => e.dayOfWeek === (dayIdx + 1))
              .map(e => {
                const s = Math.max(timeToMinutes(e.startTime), rangeMin);
                const ed = Math.min(timeToMinutes(e.endTime), rangeMax);
                const topPct = ((s - rangeMin) / (rangeMax - rangeMin)) * 100;
                const heightPct = Math.max(0, (ed - s) / (rangeMax - rangeMin)) * 100;

                const Block = (
                  <div
                    key={e.id}
                    className="absolute left-1 right-1 rounded-lg px-2 py-1 text-xs shadow-sm
                               bg-emerald-100 text-emerald-900 ring-1 ring-emerald-200 overflow-hidden"
                    style={{ top: `${topPct}%`, height: `${heightPct}%` }}
                    title={`${e.title} ${e.startTime}~${e.endTime}${e.room ? ` · ${e.room}` : ""}`}
                  >
                    <div className="font-medium truncate">{e.title}</div>
                    <div className="text-[10px] opacity-80">
                      {e.startTime}~{e.endTime}{e.room ? ` · ${e.room}` : ""}
                    </div>
                  </div>
                );
                return e.href ? (
                  <a key={e.id} href={e.href} className="absolute inset-x-0" style={{ top: `${topPct}%`, height: `${heightPct}%` }}>
                    {Block}
                  </a>
                ) : Block;
              })}
          </div>
        ))}
      </div>
    </div>
  );
}
