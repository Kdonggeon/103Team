// src/components/manage/SeatBoardCard.tsx
"use client";

import React from "react";
import { type SeatBoardResponse, type SeatBoardSeat } from "@/app/lib/teachermainApi";

type Props = {
  title?: string;
  date?: string;
  board: SeatBoardResponse | null;
  loading?: boolean;
  error?: string | null;
  onExpand?: () => void;
};

/* 상태별 색상 */
function statusClass(att?: string | null): string {
  const s = (att ?? "").toUpperCase();
  if (s.includes("PRESENT") || s.includes("출석"))
    return "bg-emerald-100 text-emerald-700 border-emerald-300";
  if (s.includes("LATE") || s.includes("지각"))
    return "bg-yellow-100 text-yellow-700 border-yellow-300";
  if (s.includes("ABSENT") || s.includes("결석"))
    return "bg-rose-100 text-rose-700 border-rose-300";
  if (s.includes("MOVE") || s.includes("BREAK") || s.includes("WAIT") || s.includes("RELOC") || s.includes("LOBBY"))
    return "bg-blue-100 text-blue-700 border-blue-300";
  return "bg-gray-100 text-gray-700 border-gray-300";
}

const STATUS_STYLES = {
  present: {
    bg: "bg-emerald-50",
    border: "border-emerald-400",
    text: "text-emerald-700",
  },
  late: {
    bg: "bg-amber-50",
    border: "border-amber-400",
    text: "text-amber-700",
  },
  absent: {
    bg: "bg-rose-50",
    border: "border-rose-400",
    text: "text-rose-700",
  },
  move: {
    bg: "bg-blue-50",
    border: "border-blue-400",
    text: "text-blue-700",
  },
  none: {
    bg: "bg-gray-50",
    border: "border-gray-300",
    text: "text-gray-700",
  },
} as const;

function Badge({ label, variant }: { label: string; variant: keyof typeof STATUS_STYLES }) {
  const v = STATUS_STYLES[variant];
  return (
    <span
      className={`inline-flex items-center rounded-full border px-3 py-1 text-[11px] font-semibold ${v.bg} ${v.border} ${v.text}`}
    >
      {label}
    </span>
  );
}

function computeCounts(board: SeatBoardResponse | null) {
  if (
    board &&
    (board.presentCount != null ||
      board.lateCount != null ||
      board.absentCount != null ||
      board.moveOrBreakCount != null ||
      board.notRecordedCount != null)
  ) {
    return {
      present: board.presentCount ?? 0,
      late: board.lateCount ?? 0,
      absent: board.absentCount ?? 0,
      move: board.moveOrBreakCount ?? 0,
      none: board.notRecordedCount ?? 0,
    };
  }

  let present = 0;
  let late = 0;
  let absent = 0;
  let move = 0;
  let none = 0;
  for (const s of board?.seats ?? []) {
    const st = (s.attendanceStatus ?? "").toUpperCase();
    if (st.includes("PRESENT") || st.includes("출석")) present++;
    else if (st.includes("LATE") || st.includes("지각")) late++;
    else if (st.includes("ABSENT") || st.includes("결석")) absent++;
    else if (st.includes("MOVE") || st.includes("BREAK") || st.includes("WAIT")) move++;
    else none++;
  }
  return { present, late, absent, move, none };
}

function CanvasVectorBoard({ seats }: { seats: SeatBoardSeat[] }) {
  // 좌표/크기가 없을 때 기본값 사용
  const cw = 1000;
  const ch = 600;
  const ratio = (ch / cw) * 100;

  return (
    <div className="w-full">
      <div
        className="relative w-full border rounded-2xl bg-white ring-1 ring-black/5 shadow-sm"
        style={{ paddingTop: `${ratio}%` }}
      >
        {(seats ?? []).map((s, idx) => {
          const left = `${Math.max(0, Math.min(1, s.x ?? 0)) * 100}%`;
          const top = `${Math.max(0, Math.min(1, s.y ?? 0)) * 100}%`;
          const width = `${Math.max(0, Math.min(1, s.w ?? 0.12)) * 100}%`;
          const height = `${Math.max(0, Math.min(1, s.h ?? 0.08)) * 100}%`;
          const label = s.studentName || s.studentId || s.seatNumber || `Seat ${idx + 1}`;
          return (
            <div
              key={idx}
              className={`absolute rounded-xl border flex items-center justify-center text-xs font-semibold truncate ${statusClass(
                s.attendanceStatus
              )} ${s.disabled ? "opacity-50" : ""}`}
              style={{ left, top, width, height }}
              title={label?.toString()}
            >
              <span className="px-1">{label}</span>
            </div>
          );
        })}
        {(!seats || seats.length === 0) && (
          <div className="absolute inset-0 grid place-items-center text-sm text-gray-600">
            좌석 배치가 없습니다.
          </div>
        )}
      </div>
    </div>
  );
}

function CanvasGridBoard({ seats, rows, cols }: { seats: SeatBoardSeat[]; rows?: number; cols?: number }) {
  const r = Math.max(0, rows ?? 0);
  const c = Math.max(0, cols ?? 0);
  type Cell = { seatNumber?: number; studentId?: string; studentName?: string; att?: string | null } | null;
  const grid: Cell[][] = Array.from({ length: r || 1 }, () => Array.from({ length: c || 1 }, () => null));

  for (const s of seats ?? []) {
    if (s?.disabled) continue;
    const ri = (s.row ?? 1) - 1;
    const ci = (s.col ?? 1) - 1;
    if (ri >= 0 && ri < grid.length && ci >= 0 && ci < grid[0].length) {
      grid[ri][ci] = {
        seatNumber: s.seatNumber ?? undefined,
        studentId: s.studentId ?? undefined,
        studentName: s.studentName ?? undefined,
        att: s.attendanceStatus ?? undefined,
      };
    }
  }

  return (
    <div className="w-full overflow-auto">
      <div
        className="grid gap-2"
        style={{ gridTemplateColumns: `repeat(${Math.max(c || 1, 1)}, minmax(44px, 1fr))` }}
      >
        {grid.flatMap((row, ri) =>
          row.map((cell, ci) => (
            <div key={`${ri}-${ci}`} className="h-12">
              {cell ? (
                <div
                  className={`h-full rounded-xl ring-1 ring-black/5 shadow-sm flex items-center justify-center text-xs ${statusClass(
                    cell.att
                  )}`}
                  title={cell.studentName ?? cell.studentId ?? ""}
                >
                  <div className="px-2 text-center leading-tight">
                    <div className="font-medium">{cell.seatNumber ?? ""}</div>
                    {(cell.studentName || cell.studentId) && (
                      <div className="opacity-80 truncate">
                        {cell.studentName ?? cell.studentId}
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="h-full rounded-xl bg-transparent" />
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default function SeatBoardCard({ title, date, board, loading, error, onExpand }: Props) {
  const counts = computeCounts(board);
  const isVector =
    board?.layoutType === "vector" ||
    (board?.seats ?? []).some((s) => s?.x != null || s?.w != null || s?.h != null);

  const clickable = typeof onExpand === "function";

  const handleKey = (e: React.KeyboardEvent) => {
    if (!clickable) return;
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onExpand?.();
    }
  };

  return (
    <section
      className={`bg-white rounded-2xl border border-gray-200 p-4 ${clickable ? "cursor-pointer" : ""}`}
      onClick={clickable ? onExpand : undefined}
      onKeyDown={handleKey}
      role={clickable ? "button" : undefined}
      tabIndex={clickable ? 0 : undefined}
    >
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-lg font-semibold text-black">
          {title ?? "좌석 현황"}
          {date ? <span className="ml-2 text-sm text-gray-500">{date}</span> : null}
        </h2>
        {loading && <span className="text-sm text-gray-500">불러오는 중…</span>}
      </div>

      <div
        className={`flex flex-col gap-1.5 mb-3 ${
          clickable ? "cursor-pointer select-none" : ""
        }`}
        onClick={clickable ? onExpand : undefined}
        role={clickable ? "button" : undefined}
        tabIndex={clickable ? 0 : undefined}
      >
        <div className="flex flex-wrap gap-2.5 items-center text-sm">
          <Badge label="출석" variant="present" />
          <Badge label="지각" variant="late" />
          <Badge label="결석" variant="absent" />
          <Badge label="대기/이동/휴식" variant="move" />
          <Badge label="미기록" variant="none" />
        </div>
        <div className="text-[11px] text-gray-700">
          전체 합계 — 출석 {counts.present} · 지각 {counts.late} · 결석 {counts.absent} · 이동/휴식 {counts.move} · 미기록 {counts.none}
        </div>
      </div>

      {error && (
        <div className="mb-3 text-sm text-rose-600 bg-rose-50 border border-rose-200 rounded px-3 py-2">
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-sm text-gray-700">로딩 중…</div>
      ) : !board ? (
        <div className="text-gray-600">좌석 데이터가 없습니다.</div>
      ) : isVector ? (
        <CanvasVectorBoard seats={board.seats ?? []} />
      ) : (
        <CanvasGridBoard seats={board.seats ?? []} rows={board.rows} cols={board.cols} />
      )}

      <div className="mt-5">
        <div className="text-sm font-semibold text-black mb-2">대기/이동/외출 명단</div>
        <div className="flex flex-wrap gap-2">
          {(board?.waiting ?? []).map((w) => (
            <span
              key={w.studentId}
              className="text-xs rounded-full border px-2 py-1 bg-blue-50 text-blue-700 border-blue-200"
              title={w.checkedInAt ?? ""}
            >
              {(w.studentName ?? w.studentId) + (w.status ? ` · ${w.status}` : "")}
            </span>
          ))}
          {(board?.waiting?.length ?? 0) === 0 && (
            <span className="text-xs text-gray-500">현재 대기/이동/외출 학생 없음</span>
          )}
        </div>
      </div>
    </section>
  );
}
