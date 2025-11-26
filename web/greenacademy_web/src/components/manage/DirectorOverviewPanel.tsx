// src/components/manage/DirectorOverviewPanel.tsx
"use client";

import React from "react";
import type { LoginResponse } from "@/app/lib/api";
import Panel, { PanelGrid } from "@/components/ui/Panel";
import SeatBoardCard from "@/components/manage/SeatBoardCard";
import {
  fetchDirectorOverview,
  type DirectorOverviewResponse,
  type DirectorRoomStatus,
} from "@/app/lib/directorApi";
import { fetchSeatBoard, todayYmd, type SeatBoardResponse } from "@/app/lib/teachermainApi";

const isVectorRoom = (r: DirectorRoomStatus | null) =>
  !!r &&
  (r.layoutType === "vector" || (r.seats ?? []).some((s) => s?.x != null || s?.w != null || s?.h != null));

type Selection =
  | { type: "waiting" }
  | { type: "room"; roomNumber: number };

// API가 여러 학원 데이터를 동시에 줄 경우, 현재 선택된 학원 번호로 한번 더 필터
function filterOverviewByAcademy(data: DirectorOverviewResponse, academy: number): DirectorOverviewResponse {
  const roomList = (data?.rooms ?? []).filter((r) => {
    const num = (r as any).academyNumber ?? (r as any).academy ?? null;
    return num == null || Number(num) === Number(academy);
  });
  const waitingList = (data?.waiting ?? []).filter((w) => {
    const nums = (w as any).academyNumbers ?? (w as any).academyNumber ?? null;
    if (Array.isArray(nums)) return nums.some((n: any) => Number(n) === Number(academy));
    if (nums == null) return true;
    return Number(nums) === Number(academy);
  });
  return { ...data, rooms: roomList, waiting: waitingList };
}

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
    const newer = nextMap.get(keyOf(old, idx));
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

export default function DirectorOverviewPanel({ user }: { user: NonNullable<LoginResponse> }) {
  const academies =
    Array.isArray(user.academyNumbers) && user.academyNumbers.length > 0
      ? user.academyNumbers
      : [0];
  const [academy, setAcademy] = React.useState<number>(academies[0] ?? 0);

  const [ymd, setYmd] = React.useState<string>(todayYmd());
  const [data, setData] = React.useState<DirectorOverviewResponse | null>(null);
  const [sel, setSel] = React.useState<Selection | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [err, setErr] = React.useState<string | null>(null);

  const [board, setBoard] = React.useState<SeatBoardResponse | null>(null);
  const [loadingBoard, setLoadingBoard] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);

  // 개요 로드
  const load = React.useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const res = await fetchDirectorOverview(academy, ymd);
      const filtered = filterOverviewByAcademy(res, academy);
      setData(filtered);
      setSel((prev) => {
        if (prev) return prev;
        if ((filtered.waiting?.length ?? 0) > 0) return { type: "waiting" };
        const first = filtered.rooms[0]?.roomNumber;
        return first ? { type: "room", roomNumber: first } : null;
      });
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }, [academy, ymd]);

  React.useEffect(() => {
    load();
    const t = setInterval(load, 15000);
    return () => clearInterval(t);
  }, [load]);

  // 학원 변경 시 리셋
  React.useEffect(() => {
    setSel(null);
    setBoard(null);
    setModalOpen(false);
  }, [academy]);

  // 우측 좌석판 로드 + 3초 폴링
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

    if (isVectorRoom(room) || !room.classId) {
      setBoard(null);
      return;
    }

    let alive = true;

    const loadBoard = async (showSpinner: boolean) => {
      try {
        if (showSpinner) setLoadingBoard(true);
        const b = await fetchSeatBoard(room.classId!, ymd);
        if (!alive) return;
        setBoard((prev) => mergeSeatBoard(prev, b));
      } catch {
        if (!alive) return;
        setBoard(null);
      } finally {
        if (alive && showSpinner) setLoadingBoard(false);
      }
    };

    loadBoard(true);
    const timer = setInterval(() => loadBoard(false), 3000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [data, sel, ymd]);

  // 합계
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

  // board 없을 때 room seats로 fallback
  const boardToShow: SeatBoardResponse | null = React.useMemo(() => {
    if (board) return board;
    if (!selectedRoom) return null;

    if (isVectorRoom(selectedRoom)) {
      return {
        date: data?.date ?? ymd,
        layoutType: "vector",
        seats: (selectedRoom as any).seats ?? [],
        presentCount: selectedRoom.presentCount ?? undefined,
        lateCount: selectedRoom.lateCount ?? undefined,
        absentCount: selectedRoom.absentCount ?? undefined,
        moveOrBreakCount: selectedRoom.moveOrBreakCount ?? undefined,
        notRecordedCount: selectedRoom.notRecordedCount ?? undefined,
      };
    }

    if (Array.isArray((selectedRoom as any).seats) && (selectedRoom as any).seats.length > 0) {
      return {
        date: data?.date ?? ymd,
        layoutType: "grid",
        seats: (selectedRoom as any).seats,
        rows: selectedRoom.rows ?? undefined,
        cols: selectedRoom.cols ?? undefined,
        presentCount: selectedRoom.presentCount ?? undefined,
        lateCount: selectedRoom.lateCount ?? undefined,
        absentCount: selectedRoom.absentCount ?? undefined,
        moveOrBreakCount: selectedRoom.moveOrBreakCount ?? undefined,
        notRecordedCount: selectedRoom.notRecordedCount ?? undefined,
      };
    }

    return null;
  }, [board, selectedRoom, data?.date, ymd]);

  // ESC 모달 닫기
  React.useEffect(() => {
    if (!modalOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setModalOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [modalOpen]);

  return (
    <PanelGrid>
      {/* 좌측: 강의실 리스트 */}
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
            {academies.length > 1 && (
              <div className="flex gap-2">
                {academies.map((a) => (
                  <button
                    key={a}
                    onClick={() => setAcademy(a)}
                    className={`px-2.5 py-1 rounded-full border text-xs ${
                      academy === a
                        ? "bg-emerald-100 border-emerald-300 text-emerald-700"
                        : "bg-white border-gray-300 text-gray-700"
                    }`}
                  >
                    학원 {a}
                  </button>
                ))}
              </div>
            )}
            {loading && <span className="text-xs text-gray-500">새로고침 중…</span>}
          </div>
        }
      >
        {err && <div className="mb-2 text-xs text-rose-600">{err}</div>}

        <ul className="space-y-2">
          {/* 대기실 */}
          <li>
            <button
              onClick={() => setSel({ type: "waiting" })}
              className={`w-full text-left rounded-xl px-3 py-2 ring-1 ring-black/5 shadow-sm bg-white hover:bg-gray-50 transition ${
                sel?.type === "waiting" ? "outline outline-2 outline-emerald-400" : ""
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="font-medium text-black">대기/이동/휴식 (학원 {academy})</div>
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
                        {r.className ? <span className="ml-2 text-gray-600">· {r.className}</span> : null}
                      </div>
                      <span className="text-xs text-gray-500">
                        출석 {r.presentCount ?? 0} · 지각 {r.lateCount ?? 0} · 결석 {r.absentCount ?? 0}
                      </span>
                    </div>
                  </button>
                </li>
              );
            })
          )}
        </ul>
      </Panel>

      {/* 우측: 좌석 현황 */}
      <Panel
        title={
          sel?.type === "waiting"
            ? "대기/이동/휴식 명단"
            : selectedRoom
            ? `강의실 ${selectedRoom.roomNumber} — 좌석 현황`
            : "좌석 현황"
        }
        right={
          <span className="text-xs text-gray-600">
            {data?.date ?? ymd}
            {selectedRoom && boardToShow ? " · 3초마다 좌석 자동 갱신" : ""}
          </span>
        }
      >
        <div className="mb-3 flex flex-wrap items-center gap-3 text-xs">
          <Badge label="출석" className="bg-emerald-100 text-emerald-700 border-emerald-300" />
          <Badge label="지각" className="bg-yellow-100 text-yellow-700 border-yellow-300" />
          <Badge label="결석" className="bg-rose-100 text-rose-700 border-rose-300" />
          <Badge label="대기/이동/휴식" className="bg-blue-100 text-blue-700 border-blue-300" />
          <Badge label="미기록" className="bg-gray-100 text-gray-700 border-gray-300" />
          <span className="ml-auto text-gray-600">
            전체 합계 — 출석 {totals.present} · 지각 {totals.late} · 결석 {totals.absent} · 이동/휴식 {totals.move} ·
            미기록 {totals.none}
          </span>
        </div>

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
        ) : loadingBoard && !boardToShow ? (
          <div className="text-sm text-gray-500">좌석판 불러오는 중…</div>
        ) : boardToShow ? (
          <>
            {!modalOpen && (
              <SeatBoardCard
                title={selectedRoom.className ?? `강의실 ${selectedRoom.roomNumber}`}
                date={data?.date ?? ymd}
                board={boardToShow}
                onExpand={() => setModalOpen(true)}
              />
            )}
            {modalOpen && (
              <div
                className="fixed inset-0 z-[300] bg-black/50 flex items-center justify-center p-4"
                onClick={() => setModalOpen(false)}
              >
                <div
                  className="w-full max-w-6xl max-h-[90vh] overflow-auto bg-white rounded-2xl border border-gray-300 shadow-2xl p-4"
                  onClick={(e) => e.stopPropagation()}
                >
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold text-black">좌석 현황 확대</h3>
                    <button
                      onClick={() => setModalOpen(false)}
                      className="px-3 py-1 rounded border text-sm text-gray-800 hover:bg-gray-100"
                    >
                      닫기
                    </button>
                  </div>
                  <SeatBoardCard
                    title={selectedRoom.className ?? `강의실 ${selectedRoom.roomNumber}`}
                    date={data?.date ?? ymd}
                    board={boardToShow}
                  />
                </div>
              </div>
            )}
          </>
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

function Badge({ label, className = "" }: { label: string; className?: string }) {
  return <span className={`rounded-full border px-2 py-0.5 ${className}`}>{label}</span>;
}
