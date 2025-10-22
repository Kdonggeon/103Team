"use client";
import Link from "next/link";

export type DayEvent = {
  id: string;
  title: string;
  startTime: string; // "HH:mm"
  endTime: string;   // "HH:mm"
  room?: string;
  href?: string;     // 클릭 이동(선택)
};

type Props = {
  startHour?: number;           // 기본 8
  endHour?: number;             // 기본 22
  events: DayEvent[];
  pxPerMinute?: number;         // 기본 0.8 → 30분=24px
  showNowLine?: boolean;        // 현재 시각 라인 표시
  nowHHmm?: string;             // 테스트용 현재시각 고정
};

export default function DayTimeline({
  startHour = 8,
  endHour = 22,
  events,
  pxPerMinute = 0.8,
  showNowLine = false,
  nowHHmm,
}: Props) {
  const labelWidth = 64; // 좌측 시간 라벨 폭
  const totalMinutes = (endHour - startHour) * 60;
  const height = Math.max(1, totalMinutes * pxPerMinute);

  // 시간 눈금
  const hours: number[] = [];
  for (let h = startHour; h <= endHour; h++) hours.push(h);

  // 이벤트 레이아웃(겹침 처리)
  const laid = layoutEvents(events, startHour, pxPerMinute);

  // 현재시간 라인
  const nowTop = showNowLine
    ? pos(startHour, nowHHmm ?? toHHmm(new Date()), pxPerMinute)
    : null;

  return (
    <div
      className="relative rounded-xl ring-1 ring-black/5 bg-white overflow-hidden"
      style={{ height }}
    >
      {/* 배경 그리드 */}
      <div className="absolute inset-0" style={{ marginLeft: labelWidth }}>
        {/* 1시간 라인 */}
        {hours.map((h, i) => {
          const top = Math.max(0, (h - startHour) * 60 * pxPerMinute);
          return (
            <div
              key={`h-${i}`}
              className="absolute left-0 right-0 border-t border-gray-200"
              style={{ top }}
            />
          );
        })}
        {/* 30분 보조선 */}
        {hours.slice(0, -1).map((h, i) => {
          const top = Math.max(0, ((h - startHour) * 60 + 30) * pxPerMinute);
          return (
            <div
              key={`m-${i}`}
              className="absolute left-0 right-0 border-t border-dashed border-gray-200/70"
              style={{ top }}
            />
          );
        })}
      </div>

      {/* 좌측 시간 라벨 */}
      <div className="absolute top-0 left-0 h-full" style={{ width: labelWidth }}>
        {hours.map((h, i) => {
          const top = Math.max(0, (h - startHour) * 60 * pxPerMinute);
          return (
            <div
              key={`label-${i}`}
              className="absolute right-2 text-[11px] text-gray-600 select-none"
              style={{ top: Math.max(0, top - 8) }}
            >
              {String(h).padStart(2, "0")}:00
            </div>
          );
        })}
      </div>

      {/* 이벤트 레이어 */}
      <div className="absolute inset-0" style={{ left: labelWidth }}>
        {laid.map((e) => {
          const content = (
            <div className="text-xs">
              <div className="font-medium truncate">{e.title}</div>
              {e.room && <div className="opacity-75 truncate">{e.room}</div>}
              <div className="opacity-70">
                {e.startTime}~{e.endTime}
              </div>
            </div>
          );

          return e.href ? (
            <Link
              key={e.id}
              href={e.href}
              className="absolute rounded-lg bg-emerald-100/85 ring-1 ring-emerald-300 hover:ring-emerald-400 transition-shadow"
              style={{
                top: e.top,
                height: e.height,
                left: e.leftPct + "%",
                width: e.widthPct + "%",
              }}
              title={`${e.title} ${e.startTime}~${e.endTime}`}
            >
              {content}
            </Link>
          ) : (
            <div
              key={e.id}
              className="absolute rounded-lg bg-emerald-100/85 ring-1 ring-emerald-300"
              style={{
                top: e.top,
                height: e.height,
                left: e.leftPct + "%",
                width: e.widthPct + "%",
              }}
              title={`${e.title} ${e.startTime}~${e.endTime}`}
            >
              {content}
            </div>
          );
        })}
      </div>

      {/* 현재 시간 라인 */}
      {showNowLine && nowTop !== null && nowTop >= 0 && nowTop <= height && (
        <div className="absolute left-0 right-0" style={{ top: nowTop }}>
          <div className="absolute" style={{ left: labelWidth }}>
            <div className="h-px bg-rose-500" />
            <div className="w-2 h-2 -mt-[3px] rounded-full bg-rose-500" />
          </div>
        </div>
      )}
    </div>
  );
}

/* ================= helpers ================= */

function toHHmm(d: Date) {
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${hh}:${mm}`;
}

function pos(startHour: number, hhmm: string, pxPerMinute: number) {
  const [hh, mm = "0"] = hhmm.split(":");
  const minutesFromStart =
    (parseInt(hh, 10) - startHour) * 60 + parseInt(mm, 10);
  return minutesFromStart * pxPerMinute;
}

function minutes(hhmm: string) {
  const [h, m = "0"] = hhmm.split(":");
  return parseInt(h, 10) * 60 + parseInt(m, 10);
}

type Laid = {
  id: string;
  title: string;
  startTime: string;
  endTime: string;
  room?: string;
  href?: string;      // ★ 추가: 링크 이동용
  top: number;
  height: number;
  leftPct: number;    // 0~100
  widthPct: number;   // 0~100
};

/** 겹침 처리(클러스터 단위 열 분할) */
function layoutEvents(
  events: DayEvent[],
  startHour: number,
  pxPerMinute: number
): Laid[] {
  const es = [...events]
    .map((e) => ({
      ...e,
      s: minutes(e.startTime),
      e: minutes(e.endTime),
    }))
    .sort((a, b) => a.s - b.s || a.e - b.e);

  // 클러스터 묶기
  const clusters: typeof es[] = [];
  let cur: typeof es = [];
  let curEnd = -1;
  for (const ev of es) {
    if (cur.length === 0 || ev.s < curEnd) {
      cur.push(ev);
      curEnd = Math.max(curEnd, ev.e);
    } else {
      clusters.push(cur);
      cur = [ev];
      curEnd = ev.e;
    }
  }
  if (cur.length) clusters.push(cur);

  const laid: Laid[] = [];

  for (const cluster of clusters) {
    // 열 배치
    const cols: { end: number; items: typeof es }[] = [];
    for (const ev of cluster) {
      let placed = false;
      for (const col of cols) {
        if (ev.s >= col.end) {
          col.items.push(ev);
          col.end = Math.max(col.end, ev.e);
          placed = true;
          break;
        }
      }
      if (!placed) cols.push({ end: ev.e, items: [ev] });
    }
    const colCount = Math.max(1, cols.length);

    // 좌표 계산
    for (let i = 0; i < colCount; i++) {
      for (const ev of cols[i].items) {
        const top = pos(startHour, ev.startTime, pxPerMinute);
        const h = Math.max((ev.e - ev.s) * pxPerMinute, 26); // 최소 높이
        const leftPct = (i / colCount) * 100;
        const widthPct = (1 / colCount) * 100 - 2; // 칸 간 여백

        laid.push({
          id: ev.id,
          title: ev.title,
          startTime: ev.startTime,
          endTime: ev.endTime,
          room: ev.room,
          href: ev.href,              
          top,
          height: h,
          leftPct,
          widthPct,
        });
      }
    }
  }
  return laid;
}
