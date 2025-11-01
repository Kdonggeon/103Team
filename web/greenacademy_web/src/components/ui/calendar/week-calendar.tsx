// src/components/ui/calendar/week-calendar.tsx
"use client";

import React, { useMemo } from "react";

/* =========================================================
 * 타입 정의
 * ======================================================= */
export type CalendarEvent = {
  id: string;
  title: string;
  dayOfWeek: 1 | 2 | 3 | 4 | 5 | 6 | 7; // Mon..Sun
  startTime: string; // "HH:mm" or "H:mm"
  endTime: string;   // "HH:mm" or "H:mm"
  href?: string;
  room?: string;     // e.g., "Room 403"
  color?: string;    // 카드 배경색
};

<<<<<<< HEAD


function timeToMinutes(t: string) {
  const [h, m = "0"] = t.split(":");
  return parseInt(h, 10) * 60 + parseInt(m, 10);
=======
export type WeekCalendarProps = {
  startHour?: number;   // default 8
  endHour?: number;     // default 22
  events: CalendarEvent[];
  onEventClick?: (ev: CalendarEvent) => void;

  /** 스타일 옵션(요청: 회색 → 검정 라인) */
  lineColor?: string;   // grid 라인 색(기본: rgba(0,0,0,0.18))
  textColor?: string;   // 시각/요일 텍스트 색(기본: #111)
  showNowLine?: boolean;
};

/* =========================================================
 * 유틸
 * ======================================================= */
const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));

/** "H:mm" or "HH:mm" -> minutes */
function toMin(hhmm: string) {
  const m = hhmm?.match(/^(\d{1,2}):(\d{2})$/);
  if (!m) return NaN;
  const hh = parseInt(m[1], 10);
  const mm = parseInt(m[2], 10);
  return hh * 60 + mm;
>>>>>>> main-develop/web/feature9
}

/** overlap [s,e) */
function overlap(aS: number, aE: number, bS: number, bE: number) {
  return aS < bE && bS < aE;
}

/* =========================================================
 * 컴포넌트
 * ======================================================= */
export default function WeekCalendar({
  startHour = 8,
  endHour = 22,
  events,
  onEventClick,
  lineColor = "rgba(0,0,0,0.18)",   // 🔧 검은 라인(연함)
  textColor = "#111111",            // 🔧 텍스트는 짙은 검정
  showNowLine = true,
}: WeekCalendarProps) {

  const hours = useMemo(() => {
    const out: number[] = [];
    for (let h = startHour; h <= endHour; h++) out.push(h);
    return out;
  }, [startHour, endHour]);

  const totalMinutes = Math.max(60, (endHour - startHour) * 60);

  // 요일별 레이아웃
  const perDay = useMemo(() => {
    const map = new Map<number, (CalendarEvent & {
      topPct: number; heightPct: number;
      lane: number; laneCount: number;
      _s: number; _e: number;
    })[]>();
    for (let d = 1; d <= 7; d++) map.set(d, []);

    for (let d = 1; d <= 7; d++) {
      const list = events
        .filter(e => e.dayOfWeek === d)
        .map(e => {
          const s = toMin(e.startTime);
          const eMin = toMin(e.endTime);
          const startOffset = (isFinite(s) ? s : 0) - startHour * 60;
          const endOffset = (isFinite(eMin) ? eMin : 0) - startHour * 60;

          // 최소 높이 20분
          const rawHeight = Math.max(20, endOffset - startOffset);

          const top = Math.max(0, startOffset) / totalMinutes;
          const height = Math.max(0, rawHeight) / totalMinutes;

          return {
            ...e,
            _s: isFinite(s) ? s : 0,
            _e: isFinite(eMin) ? eMin : 0,
            topPct: Math.max(0, Math.min(1, top)) * 100,
            heightPct: Math.max(0, Math.min(1, height)) * 100,
          };
        })
        .sort((a, b) => (a._s - b._s) || (a._e - b._e) || a.title.localeCompare(b.title));

      // 겹침 그룹 → lane 배정
      type LItem = typeof list[number] & { lane: number; laneCount: number };
      const laid: LItem[] = [];
      let i = 0;
      while (i < list.length) {
        const group: typeof list = [list[i]];
        let gEnd = list[i]._e;
        let j = i + 1;
        while (j < list.length) {
          if (overlap(list[j]._s, list[j]._e, list[i]._s, gEnd)) {
            gEnd = Math.max(gEnd, list[j]._e);
            group.push(list[j]);
            j++;
          } else break;
        }
        const lanes: LItem[][] = [];
        for (const ev of group) {
          let placed = false;
          for (let li = 0; li < lanes.length; li++) {
            const lane = lanes[li];
            const last = lane[lane.length - 1];
            if (!overlap(ev._s, ev._e, last._s, last._e)) {
              lane.push(ev as any);
              (ev as any).lane = li;
              placed = true;
              break;
            }
          }
          if (!placed) {
            (ev as any).lane = lanes.length;
            lanes.push([ev as any]);
          }
        }
        const laneCount = lanes.length;
        for (const ev of group) {
          laid.push({ ...(ev as any), laneCount });
        }
        i = j;
      }
      map.set(d, laid);
    }
    return map;
  }, [events, startHour, totalMinutes]);

  const gridStyle: React.CSSProperties = {
    gridTemplateColumns: "64px repeat(7, minmax(0,1fr))",
  };

  // 🔴 현재 시각 라인 위치(선택)
  const now = new Date();
  const nowMin = (now.getHours() * 60 + now.getMinutes()) - startHour * 60;
  const nowPct = Math.max(0, Math.min(1, nowMin / totalMinutes)) * 100;

  return (
    <div className="w-full select-none">
      {/* 헤더: 요일 */}
      <div className="grid" style={gridStyle}>
        <div />
        {["월","화","수","목","금","토","일"].map((w, i) => (
          <div
            key={w}
            className="py-2 text-center text-sm font-bold"
            style={{ color: i === 5 ? "#2563eb" : i === 6 ? "#dc2626" : textColor }}
          >
            {w}
          </div>
        ))}
      </div>

      {/* 본문 */}
      <div className="grid" style={gridStyle}>
        {/* 좌측 시간축 */}
        <div className="relative bg-white">
          {hours.map((h) => (
            <div key={h} className="h-16 relative" style={{ borderBottom: `1px dashed ${lineColor}` }}>
              <div className="absolute right-1 -translate-y-1/2 top-8 text-[11px]" style={{ color: textColor }}>
                {pad2(h)}:00
              </div>
            </div>
          ))}
        </div>

        {/* 7일 */}
        {Array.from({ length: 7 }, (_, idx) => idx + 1).map((dow) => {
          const dayEvents = perDay.get(dow) ?? [];
          return (
            <div key={dow} className="relative bg-white" style={{ borderLeft: `1px solid ${lineColor}` }}>
              {/* 시간 그리드 라인 */}
              {hours.map((h) => (
                <div key={h} className="h-16" style={{ borderBottom: `1px dashed ${lineColor}` }} />
              ))}

              {/* 현재시각 라인 */}
              {showNowLine && dow === ((now.getDay() === 0 ? 7 : now.getDay()) as 1|2|3|4|5|6|7) && (
                <div
                  className="absolute left-0 right-0"
                  style={{
                    top: `${nowPct}%`,
                    height: 2,
                    background: "rgba(220,38,38,0.9)",
                  }}
                />
              )}

              {/* 이벤트들 */}
              <div className="absolute inset-0 px-1">
                {dayEvents.map((ev) => {
                  // lanes 간 간격: 4px(2px 여백)
                  const top = `${ev.topPct}%`;
                  const height = `${ev.heightPct}%`;
                  const left = `calc(${(ev.lane / Math.max(1, ev.laneCount)) * 100}% + 2px)`;
                  const width = `calc(${100 / Math.max(1, ev.laneCount)}% - 4px)`;
                  const bg = ev.color ?? "#dcfce7";

                  const Card: React.FC = () => (
                    <div
                      className="absolute overflow-hidden"
                      style={{ top, height, left, width }}
                      onClick={() => onEventClick?.(ev)}
                    >
                      <div
                        className="h-full w-full"
                        style={{
                          background: bg,
                          border: "1px solid rgba(16,185,129,0.6)",
                          borderRadius: 12,               // 🔧 사각형 더 예쁘게
                          boxShadow: "0 2px 6px rgba(0,0,0,0.12)",
                          color: "#0f172a",
                          padding: "6px 8px",
                          display: "flex",
                          flexDirection: "column",
                          gap: 2,
                        }}
                        title={`${ev.title} (${ev.startTime}~${ev.endTime})${ev.room ? " · " + ev.room : ""}`}
                      >
                        <div className="font-semibold truncate" style={{ fontSize: 12 }}>
                          {ev.title}{ev.room ? ` · ${ev.room}` : ""}
                        </div>
                        <div className="opacity-80" style={{ fontSize: 11 }}>
                          {ev.startTime}~{ev.endTime}
                        </div>
                      </div>
                    </div>
                  );

                  return ev.href ? (
                    <a key={ev.id} href={ev.href} onClick={(e) => e.stopPropagation()}>
                      <Card />
                    </a>
                  ) : (
                    <Card key={ev.id} />
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
