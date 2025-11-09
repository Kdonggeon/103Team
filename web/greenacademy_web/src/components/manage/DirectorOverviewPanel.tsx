// src/components/manage/DirectorOverviewPanel.tsx
"use client";

import React from "react";
import type { LoginResponse } from "@/app/lib/api";
import Panel, { PanelGrid } from "@/components/ui/Panel";
import {
  fetchDirectorOverview,
  type DirectorOverviewResponse,
  type DirectorRoomStatus,
} from "@/app/lib/directorApi";

// ✅ Teacher 화면에서 쓰던 API 재사용 (좌석판 폴백 용)
import { fetchSeatBoard, todayYmd, type SeatBoardResponse } from "@/app/lib/teachermainApi";

/* ====================== 유틸 ====================== */
const isVectorRoom = (r: DirectorRoomStatus | null) =>
  !!r &&
  (r.layoutType === "vector" ||
    (r.seats ?? []).some((s) => s?.x != null || s?.w != null || s?.h != null));

type Selection =
  | { type: "waiting" }
  | { type: "room"; roomNumber: number };

/* ====================== 메인 ====================== */
export default function DirectorOverviewPanel({ user }: { user: NonNullable<LoginResponse> }) {
  const academyNumber =
    Array.isArray(user.academyNumbers) && user.academyNumbers.length > 0
      ? user.academyNumbers[0]
      : 0;

  const [ymd, setYmd] = React.useState<string>(todayYmd());
  const [data, setData] = React.useState<DirectorOverviewResponse | null>(null);
  const [sel, setSel] = React.useState<Selection | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [err, setErr] = React.useState<string | null>(null);

  // 우측 좌석판(교사 좌석 API 재사용; vector가 아닌 경우 폴백)
  const [board, setBoard] = React.useState<SeatBoardResponse | null>(null);
  const [loadingBoard, setLoadingBoard] = React.useState(false);

  const load = React.useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const res = await fetchDirectorOverview(academyNumber, ymd);
      setData(res);
      // 초기 선택: 대기실 → 없으면 첫 방
      setSel((prev) => {
        if (prev) return prev;
        if ((res.waiting?.length ?? 0) > 0) return { type: "waiting" };
        const first = res.rooms[0]?.roomNumber;
        return first ? { type: "room", roomNumber: first } : null;
      });
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }, [academyNumber, ymd]);

  React.useEffect(() => {
    load();
    const t = setInterval(load, 15000);
    return () => clearInterval(t);
  }, [load]);

  // 좌측에서 방을 선택했을 때 좌석판 로드 (vector가 아니고 classId가 있으면)
  React.useEffect(() => {
    if (!data || !sel || sel.type !== "room") {
      setBoard(null);
      return;
    }
    const room = data.rooms.find((r) => r.roomNumber === sel.roomNumber) || null;
    if (!room) {
      setBoard(null);
      return;
    }

    // vector는 우측에서 room.seats를 바로 그림
    if (isVectorRoom(room)) {
      setBoard(null);
      return;
    }

    // grid인데 classId가 없으면 폴백 불가
    if (!room.classId) {
      setBoard(null);
      return;
    }

    let alive = true;
    (async () => {
      try {
        setLoadingBoard(true);
        const b = await fetchSeatBoard(room.classId!, ymd);
        if (alive) setBoard(b);
      } catch {
        if (alive) setBoard(null);
      } finally {
        if (alive) setLoadingBoard(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [data, sel, ymd]);

  // 합계 (상단 요약)
  const totals = React.useMemo(
    () =>
      (data?.rooms ?? []).reduce(
        (acc, r) => {
          acc.present += r.presentCount ?? 0;
          acc.late += r.lateCount ?? 0;
          acc.absent += r.absentCount ?? 0;
          acc.move += r.moveOrBreakCount ?? 0;
          acc.none += r.notRecordedCount ?? 0;
          return acc;
        },
        { present: 0, late: 0, absent: 0, move: 0, none: 0 }
      ),
    [data]
  );

  const selectedRoom =
    sel?.type === "room"
      ? data?.rooms.find((r) => r.roomNumber === sel.roomNumber) ?? null
      : null;

  return (
    <PanelGrid>
      {/* 좌측: 강의실 + 대기실 항목 */}
      <Panel
        title="강의실"
        className="h-full"
        right={
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={ymd}
              onChange={(e) => setYmd(e.target.value)}
              className="border border-gray-300 rounded px-2 py-1 text-black"
            />
            {loading && <span className="text-xs text-gray-500">새로고침 중…</span>}
          </div>
        }
      >
        {err && <div className="mb-2 text-xs text-rose-600">{err}</div>}

        <ul className="space-y-2">
          {/* 대기실 항목 */}
          <li>
            <button
              onClick={() => setSel({ type: "waiting" })}
              className={`w-full text-left rounded-xl px-3 py-2 ring-1 ring-black/5 shadow-sm bg-white hover:bg-gray-50 transition ${
                sel?.type === "waiting" ? "outline outline-2 outline-emerald-400" : ""
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="font-medium text-black">대기/이동/휴식 (학원 전체)</div>
                <span className="text-xs text-gray-500">{data?.waiting?.length ?? 0}명</span>
              </div>
            </button>
          </li>

          {/* 강의실 목록 */}
          {!data || data.rooms.length === 0 ? (
            <div className="text-sm text-gray-600">표시할 강의실이 없습니다.</div>
          ) : (
            data.rooms.map((r) => {
              const isSel = sel?.type === "room" && sel.roomNumber === r.roomNumber;
              return (
                <li key={r.roomNumber}>
                  <button
                    onClick={() => setSel({ type: "room", roomNumber: r.roomNumber })}
                    className={`w-full text-left rounded-xl px-3 py-2 ring-1 ring-black/5 shadow-sm bg-white hover:bg-gray-50 transition ${
                      isSel ? "outline outline-2 outline-emerald-400" : ""
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="font-medium text-black">
                        강의실 {r.roomNumber}
                        {r.className ? (
                          <span className="ml-2 text-gray-600">· {r.className}</span>
                        ) : null}
                      </div>
                      <span className="text-xs text-gray-500">
                        출석 {r.presentCount ?? 0} · 지각 {r.lateCount ?? 0} · 결석{" "}
                        {r.absentCount ?? 0}
                      </span>
                    </div>
                  </button>
                </li>
              );
            })
          )}
        </ul>
      </Panel>

      {/* 우측: 좌석 현황 / 대기실 목록 */}
      <Panel
        title={
          sel?.type === "waiting"
            ? "대기/이동/휴식 명단"
            : selectedRoom
            ? `강의실 ${selectedRoom.roomNumber} — 좌석 현황`
            : "좌석 현황"
        }
        right={<span className="text-xs text-gray-600">{data?.date ?? ymd}</span>}
      >
        {/* 상단 합계 */}
        <div className="mb-3 flex flex-wrap items-center gap-3 text-xs">
          <Badge label="출석" className="bg-emerald-100 text-emerald-700 border-emerald-300" />
          <Badge label="지각" className="bg-yellow-100 text-yellow-700 border-yellow-300" />
          <Badge label="결석" className="bg-rose-100 text-rose-700 border-rose-300" />
          <Badge label="대기/이동/휴식" className="bg-blue-100 text-blue-700 border-blue-300" />
          <Badge label="미기록" className="bg-gray-100 text-gray-700 border-gray-300" />
          <span className="ml-auto text-gray-600">
            전체 합계 — 출석 {totals.present} · 지각 {totals.late} · 결석 {totals.absent} ·
            이동/휴식 {totals.move} · 미기록 {totals.none}
          </span>
        </div>

        {/* 본문 */}
        {sel?.type === "waiting" ? (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
            <div className="flex flex-wrap gap-2">
              {(data?.waiting ?? []).map((w) => (
                <span
                  key={w.studentId}
                  className="text-xs rounded-full border px-2 py-1 bg-blue-50 text-blue-700 border-blue-200"
                  title={w.checkedInAt ?? ""}
                >
                  {(w.studentName ?? w.studentId) + (w.status ? ` · ${w.status}` : "")}
                </span>
              ))}
              {(data?.waiting?.length ?? 0) === 0 && (
                <span className="text-xs text-gray-500">현재 대기/이동/휴식 학생 없음</span>
              )}
            </div>
          </div>
        ) : !selectedRoom ? (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
            왼쪽에서 강의실을 선택하세요.
          </div>
        ) : isVectorRoom(selectedRoom) ? (
          <CanvasVectorBoard room={selectedRoom} />
        ) : loadingBoard ? (
          <div className="text-sm text-gray-500">좌석판 불러오는 중…</div>
        ) : board ? (
          board.layoutType === "vector" ? (
            <CanvasVectorBoard
              room={{
                roomNumber: selectedRoom.roomNumber,
                classId: selectedRoom.classId,
                className: selectedRoom.className,
                seats: board.seats ?? [],
                layoutType: "vector",
                canvasW: (board as any).canvasW ?? 1000,
                canvasH: (board as any).canvasH ?? 600,
                presentCount: board.presentCount,
                lateCount: board.lateCount,
                absentCount: board.absentCount,
                moveOrBreakCount: board.moveOrBreakCount,
                notRecordedCount: board.notRecordedCount,
              }}
            />
          ) : (
            <CanvasGridBoard
              room={{
                roomNumber: selectedRoom.roomNumber,
                classId: selectedRoom.classId,
                className: selectedRoom.className,
                seats: board.seats ?? [],
                layoutType: "grid",
                rows: board.rows,
                cols: board.cols,
                presentCount: board.presentCount,
                lateCount: board.lateCount,
                absentCount: board.absentCount,
                moveOrBreakCount: board.moveOrBreakCount,
                notRecordedCount: board.notRecordedCount,
              }}
            />
          )
        ) : (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
            오늘 이 방에서 표시할 좌석판이 없습니다.
            {selectedRoom.classId ? "" : " (classId 없음)"}
          </div>
        )}
      </Panel>
    </PanelGrid>
  );
}

/* ====================== 보드 렌더러 ====================== */

function statusClass(att?: string | null): string {
  const s = (att ?? "").toUpperCase();
  if (s.includes("PRESENT") || s.includes("출석"))
    return "bg-emerald-100 text-emerald-700 border-emerald-300";
  if (s.includes("LATE") || s.includes("지각"))
    return "bg-yellow-100 text-yellow-700 border-yellow-300";
  if (s.includes("ABSENT") || s.includes("결석"))
    return "bg-rose-100 text-rose-700 border-rose-300";
  if (s.includes("MOVE") || s.includes("BREAK") || s.includes("WAIT") || s.includes("RELOC"))
    return "bg-blue-100 text-blue-700 border-blue-300";
  return "bg-gray-100 text-gray-700 border-gray-300";
}

// Vector 좌석판: 0~1 정규화 값 사용
function CanvasVectorBoard({ room }: { room: any }) {
  const cw = room.canvasW && room.canvasW > 0 ? room.canvasW : 1000;
  const ch = room.canvasH && room.canvasH > 0 ? room.canvasH : 600;
  const ratio = (ch / cw) * 100;

  return (
    <div className="w-full">
      <div
        className="relative w-full border rounded-2xl bg-white ring-1 ring-black/5 shadow-sm"
        style={{ paddingTop: `${ratio}%` }}
      >
        {(room.seats ?? []).map((s: any, idx: number) => {
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
              title={label}
            >
              <span className="px-1">{label}</span>
            </div>
          );
        })}
        {(!room.seats || room.seats.length === 0) && (
          <div className="absolute inset-0 grid place-items-center text-sm text-gray-600">
            좌석 배치가 없습니다.
          </div>
        )}
      </div>
    </div>
  );
}

// Grid 좌석판: rows/cols + row/col 좌표 사용
function CanvasGridBoard({ room }: { room: any }) {
  const rows = Math.max(0, room.rows ?? 0);
  const cols = Math.max(0, room.cols ?? 0);
  type Cell = { seatNumber?: number; studentId?: string; studentName?: string; att?: string | null } | null;
  const grid: Cell[][] = Array.from({ length: rows }, () => Array.from({ length: cols }, () => null));

  for (const s of room.seats ?? []) {
    if (s?.disabled) continue;
    const r = (s.row ?? 1) - 1;
    const c = (s.col ?? 1) - 1;
    if (r >= 0 && r < rows && c >= 0 && c < cols) {
      grid[r][c] = {
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
        style={{ gridTemplateColumns: `repeat(${Math.max(cols, 1)}, minmax(44px, 1fr))` }}
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

/* ====================== 작은 컴포넌트 ====================== */
function Badge({ label, className = "" }: { label: string; className?: string }) {
  return <span className={`rounded-full border px-2 py-0.5 ${className}`}>{label}</span>;
}
