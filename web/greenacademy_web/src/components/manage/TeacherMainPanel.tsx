"use client";

import React from "react";
import type { LoginResponse } from "@/app/lib/api";
import {
  fetchSeatBoard,
  fetchTodayClasses,
  type SeatBoardResponse,
  type TeacherClassLite,
  todayYmd,
} from "@/app/lib/teachermainApi";

import Panel, { PanelGrid } from "@/components/ui/Panel";

/* ---------------------- util ---------------------- */
const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const nowHM = () => {
  const d = new Date();
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
};
const toMin = (hhmm?: string | null): number | null => {
  if (!hhmm) return null;
  const [h, m] = hhmm.split(":").map((x) => parseInt(x, 10));
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  return h * 60 + m;
};

function computeClassStatus(c: TeacherClassLite) {
  const nm = toMin(nowHM());
  const sm = toMin(c.startTime);
  const em = toMin(c.endTime);
  if (sm == null || em == null || nm == null) return "진행";
  if (nm < sm) return "대기";
  if (nm >= sm && nm <= em) return "진행";
  return "종료";
}

function colorFor(att?: string | null) {
  const s = (att ?? "").toUpperCase();
  if (s.includes("WAIT") || s.includes("MOVE") || s.includes("RELOC") || s.includes("BREAK"))
    return "bg-blue-500";
  if (s.includes("PRESENT") || s.includes("출석")) return "bg-green-500";
  if (s.includes("LATE") || s.includes("지각")) return "bg-yellow-400";
  if (s.includes("ABSENT") || s.includes("결석")) return "bg-red-500";
  return "bg-gray-300";
}

/* ---------------- merge seat board (teacher) ---------------- */
function mergeSeatBoard(prev: SeatBoardResponse | null, next: SeatBoardResponse): SeatBoardResponse {
  if (!prev || !prev.seats || !next.seats) return next;

  const keyOf = (s: any, idx: number) => {
    if (s.seatNumber != null) return `seat-${s.seatNumber}`;
    if (s.row != null && s.col != null) return `rc-${s.row}-${s.col}`;
    return `idx-${idx}`;
  };

  const nextMap = new Map<string, any>();
  next.seats.forEach((s, idx) => nextMap.set(keyOf(s, idx), s));

  const mergedSeats = prev.seats.map((old, idx) => {
    const k = keyOf(old, idx);
    const newer = nextMap.get(k);
    if (!newer) return old;
    return {
      ...old,
      attendanceStatus: newer.attendanceStatus ?? old.attendanceStatus,
      studentId: newer.studentId ?? old.studentId,
      studentName: newer.studentName ?? old.studentName,
    };
  });

  return { ...prev, ...next, seats: mergedSeats };
}

/* ---------------------- Vector seat board ---------------------- */
function VectorSeatBoard({ board }: { board: SeatBoardResponse }) {
  return (
    <div className="w-full">
      <div className="relative w-full" style={{ paddingTop: "60%" }}>
        <div className="absolute inset-0 rounded-2xl bg-white ring-1 ring-black/5 shadow-sm">
          {(board.seats ?? []).map((s, idx) => {
            const left = `${Math.min(1, Math.max(0, s.x ?? 0)) * 100}%`;
            const top = `${Math.min(1, Math.max(0, s.y ?? 0)) * 100}%`;
            const width = `${Math.min(1, Math.max(0, s.w ?? 0.12)) * 100}%`;
            const height = `${Math.min(1, Math.max(0, s.h ?? 0.08)) * 100}%`;

            const label =
              s.studentName?.trim() ||
              s.studentId?.trim() ||
              (s.seatNumber ? `#${s.seatNumber}` : `${idx + 1}`);

            return (
              <div
                key={`${idx}-${s.studentId ?? ""}`}
                className={`absolute rounded-xl text-xs flex items-center justify-center shadow-sm ${colorFor(
                  s.attendanceStatus
                )} ${s.disabled ? "opacity-50" : ""}`}
                style={{ left, top, width, height }}
                title={label}
              >
                <div className="px-1 text-center leading-tight text-white">
                  <div className="font-semibold truncate">{label}</div>
                  {s.seatNumber ? (
                    <div className="opacity-80">좌석 {s.seatNumber}</div>
                  ) : (
                    <div className="opacity-50">&nbsp;</div>
                  )}
                </div>
              </div>
            );
          })}

          {(!board.seats || board.seats.length === 0) && (
            <div className="absolute inset-0 grid place-items-center text-sm text-gray-600">
              강의실 좌석 배치가 없습니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* ---------------------- Grid seat board ---------------------- */
function GridSeatBoard({ board }: { board: SeatBoardResponse }) {
  const rows = board.rows ?? 0;
  const cols = board.cols ?? 0;

  type V = { label: string; studentId?: string; att?: string | null };
  const grid: (V | null)[][] = Array.from({ length: rows }, () =>
    Array.from({ length: cols }, () => null)
  );

  for (const s of board.seats ?? []) {
    if (s?.disabled) continue;
    const r = (s.row ?? 1) - 1;
    const c = (s.col ?? 1) - 1;
    if (r >= 0 && r < rows && c >= 0 && c < cols) {
      grid[r][c] = {
        label: s.seatNumber?.toString() ?? "",
        studentId: s.studentId ?? undefined,
        att: s.attendanceStatus ?? undefined,
      };
    }
  }

  return (
    <div className="w-full overflow-auto">
      <div
        className="grid gap-2"
        style={{ gridTemplateColumns: `repeat(${Math.max(cols, 1)}, minmax(44px, 1fr))` }}
      >
        {grid.flatMap((row, ri) =>
          row.map((cell, ci) => (
            <div key={`${ri}-${ci}`} className="h-12">
              {cell ? (
                <div
                  className={`h-full rounded-xl ring-1 ring-black/5 shadow-sm flex items-center justify-center text-xs text-white ${colorFor(
                    cell.att
                  )}`}
                >
                  <div className="px-1 text-center leading-tight">
                    <div className="font-semibold">{cell.label}</div>
                    {cell.studentId && <div className="opacity-90">{cell.studentId}</div>}
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

/* ---------------------- main ---------------------- */
export default function TeacherMainPanel({ user }: { user: NonNullable<LoginResponse> }) {
  const teacherId = user.username;

  const [classes, setClasses] = React.useState<TeacherClassLite[]>([]);
  const [selected, setSelected] = React.useState<TeacherClassLite | null>(null);
  const [loadingClasses, setLoadingClasses] = React.useState(true);

  const [board, setBoard] = React.useState<SeatBoardResponse | null>(null);
  const [loadingBoard, setLoadingBoard] = React.useState(false);
  const [err, setErr] = React.useState<string | null>(null);

  /* -------- load classes (left list) -------- */
  React.useEffect(() => {
    let alive = true;
    (async () => {
      try {
        setLoadingClasses(true);
        if (!teacherId) {
          setClasses([]);
          setSelected(null);
          return;
        }
        const list = await fetchTodayClasses(teacherId);
        if (!alive) return;

        setClasses(list);

        const sorted = [...list].sort((a, b) => {
          const ord = { 진행: 0, 대기: 1, 종료: 2 } as Record<string, number>;
          return (ord[computeClassStatus(a)] ?? 9) - (ord[computeClassStatus(b)] ?? 9);
        });

        setSelected(sorted[0] ?? null);
      } catch (e: any) {
        setErr(e?.message ?? String(e));
      } finally {
        if (alive) setLoadingClasses(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [teacherId]);

  /* -------- load seat board + 3s polling -------- */
  React.useEffect(() => {
    if (!selected?.classId) {
      setBoard(null);
      return;
    }

    let alive = true;

    const load = async (showSpinner: boolean) => {
      try {
        if (showSpinner) setLoadingBoard(true);
        const data = await fetchSeatBoard(selected.classId!, todayYmd());
        if (!alive) return;

        setBoard((prev) => mergeSeatBoard(prev, data));
      } catch (e: any) {
        if (!alive) return;
        setErr(e?.message ?? String(e));
      } finally {
        if (alive && showSpinner) setLoadingBoard(false);
      }
    };

    load(true);
    const timer = setInterval(() => load(false), 3000);

    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [selected?.classId]);

  return (
    <PanelGrid>
      {/* ---------------- Left: class list ---------------- */}
      <Panel title="오늘 수업" className="h-full">
        {loadingClasses ? (
          <div className="text-sm text-gray-500">불러오는 중…</div>
        ) : classes.length === 0 ? (
          <div className="text-sm text-gray-600">오늘 일정이 없습니다.</div>
        ) : (
          <ul className="space-y-2">
            {classes.map((c) => {
              const isSel = selected?.classId === c.classId;
              const status = computeClassStatus(c);

              return (
                <li key={c.classId}>
                  <button
                    onClick={() => setSelected(c)}
                    className={`w-full text-left rounded-xl ring-1 ring-black/5 shadow-sm bg-white px-3 py-2 hover:bg-gray-50 transition ${
                      isSel ? "outline outline-2 outline-emerald-400" : ""
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="font-medium text-black">
                        {c.className}
                        {typeof c.roomNumber === "number" ? (
                          <span className="ml-2 text-gray-600">• {c.roomNumber}호</span>
                        ) : null}
                      </div>

                      <span
                        className={`text-xs px-2 py-0.5 rounded-full ${
                          status === "진행"
                            ? "bg-green-100 text-green-700"
                            : status === "대기"
                            ? "bg-blue-100 text-blue-700"
                            : "bg-gray-100 text-gray-600"
                        }`}
                      >
                        {status}
                      </span>
                    </div>

                    <div className="mt-1 text-xs text-gray-600">
                      {c.startTime ?? "??:??"} ~ {c.endTime ?? "??:??"}
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </Panel>

      {/* ---------------- Right: seat board ---------------- */}
      <Panel
        title={selected ? `${selected.className} — 좌석 현황` : "좌석 현황"}
        right={
          <span className="text-xs text-gray-600">
            {todayYmd()} {loadingBoard ? "· 처음 불러오는 중…" : "· 3초마다 자동 갱신"}
          </span>
        }
      >
        {/* Legend + summary (원장 UI와 동일) */}
        <div className="mb-3 flex flex-wrap items-center gap-3 text-xs text-gray-700">
          <Legend color="bg-green-500" label="출석" />
          <Legend color="bg-yellow-400" label="지각" />
          <Legend color="bg-red-500" label="결석" />
          <Legend color="bg-blue-500" label="대기/이동/휴식" />
          <Legend color="bg-gray-300" label="미기록" />
          {board && (
            <span className="ml-auto">
              출석 {board.presentCount ?? 0} · 지각 {board.lateCount ?? 0} · 결석{" "}
              {board.absentCount ?? 0} · 이동/휴식 {board.moveOrBreakCount ?? 0} · 미기록{" "}
              {board.notRecordedCount ?? 0}
            </span>
          )}
        </div>

        {!selected?.classId ? (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
            왼쪽에서 수업을 선택하면 좌석판이 표시됩니다.
          </div>
        ) : !board && loadingBoard ? (
          <div className="text-sm text-gray-500">좌석판 불러오는 중…</div>
        ) : !board ? (
          <div className="text-sm text-gray-600">좌석 데이터가 없습니다.</div>
        ) : board.layoutType === "vector" ? (
          <VectorSeatBoard board={board} />
        ) : (
          <GridSeatBoard board={board} />
        )}

        {err && <div className="mt-3 text-xs text-red-600">{err}</div>}
      </Panel>
    </PanelGrid>
  );
}

/* ---------------------- small component ---------------------- */
function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className={`inline-block w-3 h-3 rounded-sm ${color}`} />
      {label}
    </span>
  );
}
