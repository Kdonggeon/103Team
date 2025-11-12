// src/components/manage/TeacherMainPanel.tsx
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

/* ===== 유틸 ===== */
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
  if (s.includes("WAIT") || s.includes("MOVE") || s.includes("RELOC") || s.includes("BREAK")) return "bg-blue-500";
  if (s.includes("PRESENT") || s.includes("출석")) return "bg-green-500";
  if (s.includes("LATE") || s.includes("지각")) return "bg-yellow-400";
  if (s.includes("ABSENT") || s.includes("결석")) return "bg-red-500";
  return "bg-gray-300";
}

/* ====== Vector 보드 렌더 ====== */
function VectorSeatBoard({ board }: { board: SeatBoardResponse }) {
  return (
    <div className="w-full">
      <div className="relative w-full" style={{ paddingTop: "60%" }}>
        <div className="absolute inset-0 rounded-2xl ring-1 ring-black/10 bg-white">
          {(board.seats ?? []).map((s, idx) => {
            const left = `${Math.max(0, Math.min(1, s.x ?? 0)) * 100}%`;
            const top = `${Math.max(0, Math.min(1, s.y ?? 0)) * 100}%`;
            const width = `${Math.max(0, Math.min(1, s.w ?? 0.12)) * 100}%`;
            const height = `${Math.max(0, Math.min(1, s.h ?? 0.08)) * 100}%`;
            const label =
              s.studentName?.trim() ||
              s.studentId?.trim() ||
              (s.seatNumber ? `#${s.seatNumber}` : `${idx + 1}`);

            return (
              <div
                key={`${idx}-${s.studentId ?? ""}`}
                className={`absolute rounded-xl shadow-sm text-xs flex items-center justify-center ${colorFor(
                  s.attendanceStatus
                )} ${s.disabled ? "opacity-50" : ""}`}
                style={{ left, top, width, height }}
                title={label}
              >
                <div className="px-2 text-center leading-tight text-white">
                  <div className="font-semibold truncate">{label}</div>
                  {s.seatNumber ? <div className="opacity-90">좌석 {s.seatNumber}</div> : <div className="opacity-60">&nbsp;</div>}
                </div>
              </div>
            );
          })}
          {(!board.seats || board.seats.length === 0) && (
            <div className="absolute inset-0 grid place-items-center text-sm text-gray-600">
              강의실 좌석 배치가 없습니다. 원장 화면에서 먼저 저장하세요.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* ====== Grid 보드 렌더 ====== */
function GridSeatBoard({ board }: { board: SeatBoardResponse }) {
  const rows = Math.max(0, board.rows ?? 0);
  const cols = Math.max(0, board.cols ?? 0);
  type SeatCellView = { label: string; studentId?: string; att?: string | null; disabled?: boolean | null };
  const grid: (SeatCellView | null)[][] = Array.from({ length: rows }, () =>
    Array.from({ length: cols }, () => null)
  );

  for (const s of board.seats ?? []) {
    if (s?.disabled) continue;
    const r = (s.row ?? 1) - 1;
    const c = (s.col ?? 1) - 1;
    if (r >= 0 && r < rows && c >= 0 && c < cols) {
      grid[r][c] = { label: s.seatNumber?.toString() ?? "", studentId: s.studentId ?? undefined, att: s.attendanceStatus ?? undefined };
    }
  }

  return (
    <div className="w-full overflow-auto">
      <div className="grid gap-2" style={{ gridTemplateColumns: `repeat(${Math.max(cols, 1)}, minmax(44px, 1fr))` }}>
        {grid.flatMap((row, ri) =>
          row.map((cell, ci) => (
            <div key={`${ri}-${ci}`} className="h-12">
              {cell ? (
                <div className={`h-full rounded-xl ring-1 ring-black/5 shadow-sm flex items-center justify-center text-xs text-white ${colorFor(cell.att)}`}>
                  <div className="px-2 text-center leading-tight">
                    <div className="font-medium">{cell.label}</div>
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

export default function TeacherMainPanel({ user }: { user: NonNullable<LoginResponse> }) {
  const teacherId: string | undefined = user.username || undefined;

  const [classes, setClasses] = React.useState<TeacherClassLite[]>([]);
  const [loadingClasses, setLoadingClasses] = React.useState(true);

  const [selected, setSelected] = React.useState<TeacherClassLite | null>(null);
  const [board, setBoard] = React.useState<SeatBoardResponse | null>(null);
  const [loadingBoard, setLoadingBoard] = React.useState(false);
  const [err, setErr] = React.useState<string | null>(null);

  // 오늘 수업 로드
  React.useEffect(() => {
    let alive = true;
    (async () => {
      try {
        setLoadingClasses(true);
        if (!teacherId) {
          setClasses([]); setSelected(null); return;
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
        setLoadingClasses(false);
      }
    })();
    return () => { alive = false; };
  }, [teacherId]);

  // 좌석판 로드 + 폴링
  React.useEffect(() => {
    if (!selected?.classId) { setBoard(null); return; }
    let alive = true;
    const load = async () => {
      try {
        setLoadingBoard(true);
        const data = await fetchSeatBoard(selected.classId, todayYmd());
        if (!alive) return;
        setBoard(data);
      } catch (e: any) {
        if (!alive) return;
        setErr(e?.message ?? String(e));
      } finally {
        if (alive) setLoadingBoard(false);
      }
    };
    load();
    const timer = setInterval(load, 3000); // 3초
    return () => { alive = false; clearInterval(timer); };
  }, [selected?.classId]);

  return (
    <PanelGrid>
      {/* 좌측: 오늘 수업 목록 */}
      <Panel title="오늘 수업" className="h-full">
        {loadingClasses ? (
          <div className="text-sm text-gray-500">불러오는 중…</div>
        ) : classes.length === 0 ? (
          <div className="text-sm text-gray-600">오늘 일정이 없습니다.</div>
        ) : (
          <ul className="space-y-2">
            {classes.map((c) => {
              const status = computeClassStatus(c);
              const isSel = selected?.classId === c.classId;
              return (
                <li key={c.classId}>
                  <button
                    onClick={() => setSelected(c)}
                    className={`w-full text-left rounded-xl px-3 py-2 ring-1 ring-black/5 shadow-sm bg-white hover:bg-gray-50 transition ${isSel ? "outline outline-2 outline-green-400" : ""}`}
                  >
                    <div className="flex items-center justify-between">
                      {/* ✅ 항상 검은색으로 고정 */}
                      <div className="font-medium text-black">{c.className}</div>

                      <span className={`text-xs px-2 py-0.5 rounded-full ${
                        status === "진행" ? "bg-green-100 text-green-700"
                        : status === "대기" ? "bg-blue-100 text-blue-700"
                        : "bg-gray-100 text-gray-600"
                      }`}>
                        {status}
                      </span>
                    </div>
                    <div className="mt-1 text-xs text-gray-600">
                      {c.startTime ?? "??:??"} ~ {c.endTime ?? "??:??"}
                      {typeof c.roomNumber === "number" && <span className="ml-2">• 강의실 {c.roomNumber}</span>}
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </Panel>

      {/* 우측: 좌석판 */}
      <Panel
        title={selected ? `${selected.className} — 좌석 현황` : "좌석 현황"}
        className="min-h-[420px]"
        right={<span className="text-xs text-gray-600">{board?.date ?? todayYmd()}</span>}
      >
        {/* Legend */}
        <div className="mb-3 flex flex-wrap items-center gap-4 text-xs text-gray-600">
          <Legend color="bg-green-500" label="출석" />
          <Legend color="bg-yellow-400" label="지각" />
          <Legend color="bg-red-500" label="결석" />
          <Legend color="bg-blue-500" label="대기/이동/휴식" />
          <Legend color="bg-gray-300" label="미기록" />
          {board && (
            <span className="ml-auto">
              출석 {board.presentCount ?? 0} · 지각 {board.lateCount ?? 0} · 결석 {board.absentCount ?? 0} · 이동/휴식 {board.moveOrBreakCount ?? 0} · 미기록 {board.notRecordedCount ?? 0}
            </span>
          )}
        </div>

        {!selected?.classId ? (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
            왼쪽에서 수업을 선택하면 좌석판이 표시됩니다.
          </div>
        ) : loadingBoard ? (
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

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className={`inline-block w-3 h-3 rounded-sm ${color}`} />
      {label}
    </span>
  );
}
