"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";

// ====== 타입 ======
type TeacherClassLite = {
  classId: string;
  className: string;
  roomNumber?: number;
  startTime?: string;
  endTime?: string;
};

type SeatBoardResponse = {
  currentClass?: { classId: string; className: string };
  date: string;
  layoutType?: "grid" | "vector";
  rows?: number;
  cols?: number;
  seats: Array<{
    seatNumber?: number | null;
    row?: number | null;
    col?: number | null;
    disabled?: boolean;
    studentId?: string | null;
    studentName?: string | null;        // ✅ 이름 우선 표시
    attendanceStatus?: string | null;   // "출석" | "지각" | "결석" | "이동" | "미기록" | ...
    occupiedAt?: string | null;
  }>;
  waiting?: Array<{
    studentId: string;
    studentName?: string | null;
    status?: string | null;      // LOBBY / MOVING / BREAK ...
    checkedInAt?: string | null; // ISO-8601
  }>;
  // (선택) 카운트 필드가 오면 사용
  presentCount?: number;
  lateCount?: number;
  absentCount?: number;
  moveOrBreakCount?: number;
  notRecordedCount?: number;
};

// ====== API 래퍼 ======
async function fetchTodayClasses(teacherId: string, ymd?: string): Promise<TeacherClassLite[]> {
  const url = ymd
    ? `/api/teachermain/teachers/${encodeURIComponent(teacherId)}/classes/today?date=${ymd}`
    : `/api/teachermain/teachers/${encodeURIComponent(teacherId)}/classes/today`;
  const r = await fetch(url, { cache: "no-store" });
  if (!r.ok) throw new Error(`today classes ${r.status}`);
  return r.json();
}

async function fetchSeatBoard(classId: string, ymd?: string): Promise<SeatBoardResponse> {
  const url = ymd
    ? `/api/teachermain/seat-board/${encodeURIComponent(classId)}?date=${ymd}`
    : `/api/teachermain/seat-board/${encodeURIComponent(classId)}`;
  const r = await fetch(url, { cache: "no-store" });
  if (!r.ok) throw new Error(`seat board ${r.status}`);
  return r.json();
}

// 날짜 유틸
const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const todayYmd = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
};

// 상태 → 뱃지 스타일
function statusClass(s?: string | null) {
  switch (s) {
    case "출석":
      return "bg-emerald-100 text-emerald-700 border-emerald-300";
    case "지각":
      return "bg-yellow-100 text-yellow-700 border-yellow-300";
    case "결석":
      return "bg-rose-100 text-rose-700 border-rose-300";
    case "이동":
    case "휴식":
      return "bg-blue-100 text-blue-700 border-blue-300";
    case "미기록":
    default:
      return "bg-gray-100 text-gray-700 border-gray-300";
  }
}

type Props = {
  user: { username: string }; // LoginResponse 에서 username = teacherId
};

export default function TeacherMainPanel({ user }: Props) {
  const teacherId = user.username;
  const [ymd, setYmd] = useState<string>(todayYmd());

  // 왼쪽: 오늘 수업
  const [classes, setClasses] = useState<TeacherClassLite[]>([]);
  const [loadingLeft, setLoadingLeft] = useState(false);
  const [leftErr, setLeftErr] = useState<string | null>(null);

  // 선택 클래스
  const [selected, setSelected] = useState<TeacherClassLite | null>(null);

  // 오른쪽: 좌석 현황
  const [board, setBoard] = useState<SeatBoardResponse | null>(null);
  const [loadingRight, setLoadingRight] = useState(false);
  const [rightErr, setRightErr] = useState<string | null>(null);

  // 폴링(실시간)
  const pollRef = useRef<number | null>(null);

  // 왼쪽 로드
  useEffect(() => {
    (async () => {
      setLoadingLeft(true);
      setLeftErr(null);
      try {
        const list = await fetchTodayClasses(teacherId, ymd);
        setClasses(list);
        // 자동 선택(첫 항목)
        if (list.length > 0) setSelected((prev) => prev?.classId ? prev : list[0]);
        else {
          setSelected(null);
          setBoard(null);
        }
      } catch (e: any) {
        setLeftErr(e.message || "오늘 수업 불러오기 실패");
      } finally {
        setLoadingLeft(false);
      }
    })();
  }, [teacherId, ymd]);

  // 오른쪽 로드(선택/폴링)
  useEffect(() => {
    if (!selected) return;

    const load = async () => {
      setLoadingRight(true);
      setRightErr(null);
      try {
        const res = await fetchSeatBoard(selected.classId, ymd);
        setBoard(res);
      } catch (e: any) {
        setRightErr(e.message || "좌석 현황 불러오기 실패");
      } finally {
        setLoadingRight(false);
      }
    };

    // 즉시 1회
    load();

    // 5초 폴링
    if (pollRef.current) window.clearInterval(pollRef.current);
    pollRef.current = window.setInterval(load, 5000);

    return () => {
      if (pollRef.current) window.clearInterval(pollRef.current);
    };
  }, [selected?.classId, ymd]);

  // 좌석 정렬 & 레이아웃
  const seats = useMemo(() => {
    const items = board?.seats ?? [];
    // seatNumber → 오름차순, 없으면 뒤로
    const sorted = [...items].sort((a, b) => {
      const na = a.seatNumber ?? 99999;
      const nb = b.seatNumber ?? 99999;
      return na - nb;
    });
    return sorted;
  }, [board]);

  const gridCols = useMemo(() => {
    if (!board) return 10; // 폴백
    if (board.layoutType === "grid" && board.cols && board.cols > 0) return board.cols;
    // vector라도 표시를 위해 seatNumber 기준으로 자동 10열
    return 10;
  }, [board]);

  // 통계 (백엔드에서 오면 사용, 없으면 계산)
  const counts = useMemo(() => {
    if (!board?.seats) return { present: 0, late: 0, absent: 0, move: 0, none: 0 };
    if (
      typeof board.presentCount === "number" ||
      typeof board.lateCount === "number" ||
      typeof board.absentCount === "number" ||
      typeof board.moveOrBreakCount === "number" ||
      typeof board.notRecordedCount === "number"
    ) {
      return {
        present: board.presentCount ?? 0,
        late: board.lateCount ?? 0,
        absent: board.absentCount ?? 0,
        move: board.moveOrBreakCount ?? 0,
        none: board.notRecordedCount ?? 0,
      };
    }
    // 프론트 계산
    let present = 0, late = 0, absent = 0, move = 0, none = 0;
    for (const s of board.seats) {
      switch (s.attendanceStatus) {
        case "출석": present++; break;
        case "지각": late++; break;
        case "결석": absent++; break;
        case "이동":
        case "휴식": move++; break;
        default: none++; break;
      }
    }
    return { present, late, absent, move, none };
  }, [board]);

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center gap-3">
        <h1 className="text-xl font-bold text-black">오늘 수업 / 좌석 현황</h1>
        <input
          type="date"
          value={ymd}
          onChange={(e) => setYmd(e.target.value)}
          className="border border-gray-300 rounded px-2 py-1 text-black"
        />
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        {/* ========== 왼쪽: 오늘 수업 ========== */}
        <section className="bg-white rounded-2xl border border-gray-200 p-4">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-lg font-semibold text-black">오늘 수업</h2>
            {loadingLeft && <span className="text-sm text-gray-500">불러오는 중…</span>}
          </div>

          {leftErr && (
            <div className="mb-3 text-sm text-rose-600 bg-rose-50 border border-rose-200 rounded px-3 py-2">
              {leftErr}
            </div>
          )}

          <div className="space-y-2">
            {classes.map((c) => {
              const active = selected?.classId === c.classId;
              return (
                <button
                  key={c.classId}
                  onClick={() => setSelected(c)}
                  className={`w-full text-left rounded-xl border px-4 py-3 transition ${
                    active
                      ? "border-emerald-400 bg-emerald-50"
                      : "border-gray-200 bg-white hover:bg-gray-50"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="font-semibold text-black">{c.className}</div>
                    <span className="text-xs rounded-full px-2 py-0.5 border border-emerald-300 text-emerald-700 bg-emerald-50">
                      진행
                    </span>
                  </div>
                  <div className="text-sm text-black mt-1">
                    {(c.startTime && c.endTime) ? `${c.startTime} ~ ${c.endTime}` : "??:?? ~ ??:??"}
                    <span className="mx-2">·</span>
                    {c.roomNumber ? `강의실 ${c.roomNumber}` : "강의실 ?"}
                  </div>
                </button>
              );
            })}
            {!loadingLeft && classes.length === 0 && (
              <div className="text-gray-500">오늘 수업이 없습니다.</div>
            )}
          </div>
        </section>

        {/* ========== 오른쪽: 좌석 현황 ========== */}
        <section className="bg-white rounded-2xl border border-gray-200 p-4">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-lg font-semibold text-black">
              {board?.currentClass?.className ?? (selected?.className ?? "좌석 현황")}
              <span className="ml-2 text-sm text-gray-500">{board?.date ?? ymd}</span>
            </h2>
            {loadingRight && <span className="text-sm text-gray-500">새로고침 중…</span>}
          </div>

          {/* 범례 */}
          <div className="flex flex-wrap gap-3 mb-4 items-center text-sm">
            <Badge label="출석" className="bg-emerald-100 text-emerald-700 border-emerald-300" />
            <Badge label="지각" className="bg-yellow-100 text-yellow-700 border-yellow-300" />
            <Badge label="결석" className="bg-rose-100 text-rose-700 border-rose-300" />
            <Badge label="대기/이동/휴식" className="bg-blue-100 text-blue-700 border-blue-300" />
            <Badge label="미기록" className="bg-gray-100 text-gray-700 border-gray-300" />
            <span className="ml-auto text-xs text-gray-500">
              출석 {counts.present} · 지각 {counts.late} · 결석 {counts.absent} · 이동/휴식 {counts.move} · 미기록 {counts.none}
            </span>
          </div>

          {rightErr && (
            <div className="mb-3 text-sm text-rose-600 bg-rose-50 border border-rose-200 rounded px-3 py-2">
              {rightErr}
            </div>
          )}

          {/* 좌석 그리드 */}
          <div
            className="grid gap-2"
            style={{ gridTemplateColumns: `repeat(${gridCols}, minmax(0, 1fr))` }}
          >
            {(seats ?? []).map((s, idx) => {
              const label =
                s.studentName?.trim() ||
                s.studentId?.trim() ||
                (s.seatNumber ? `#${s.seatNumber}` : `Seat ${idx + 1}`);
              return (
                <div
                  key={`${s.seatNumber ?? idx}-${s.studentId ?? ""}`}
                  className={`rounded-xl border px-2 py-2 text-xs text-center truncate ${
                    s.disabled ? "opacity-50" : ""
                  } ${statusClass(s.attendanceStatus)}`}
                  title={
                    s.studentName
                      ? `${s.studentName} (${s.studentId ?? ""})`
                      : label
                  }
                >
                  <div className="font-semibold">{label}</div>
                  {s.seatNumber ? (
                    <div className="opacity-70">좌석 {s.seatNumber}</div>
                  ) : (
                    <div className="opacity-70">&nbsp;</div>
                  )}
                </div>
              );
            })}
            {!loadingRight && (!seats || seats.length === 0) && (
              <div className="text-gray-500">좌석 데이터가 없습니다.</div>
            )}
          </div>

          {/* 웨이팅룸 */}
          <div className="mt-5">
            <div className="text-sm font-semibold text-black mb-2">대기/이동/휴식 명단</div>
            <div className="flex flex-wrap gap-2">
              {(board?.waiting ?? []).map((w) => (
                <span
                  key={w.studentId}
                  className="text-xs rounded-full border px-2 py-1 bg-blue-50 text-blue-700 border-blue-200"
                  title={w.checkedInAt ?? ""}
                >
                  {w.studentName ?? w.studentId} {w.status ? `· ${w.status}` : ""}
                </span>
              ))}
              {(board?.waiting?.length ?? 0) === 0 && (
                <span className="text-xs text-gray-500">현재 대기/이동/휴식 학생 없음</span>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

// 공통 뱃지
function Badge({ label, className = "" }: { label: string; className?: string }) {
  return (
    <span className={`rounded-full border px-2 py-0.5 ${className}`}>{label}</span>
  );
}
