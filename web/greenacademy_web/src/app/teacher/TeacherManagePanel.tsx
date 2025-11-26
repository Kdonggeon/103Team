"use client";

import React, { useEffect, useRef, useState } from "react";
import SeatBoardCard from "@/components/manage/SeatBoardCard";

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
    studentName?: string | null;
    attendanceStatus?: string | null;
    occupiedAt?: string | null;
  }>;
  waiting?: Array<{
    studentId: string;
    studentName?: string | null;
    status?: string | null;
    checkedInAt?: string | null;
  }>;
  presentCount?: number;
  lateCount?: number;
  absentCount?: number;
  moveOrBreakCount?: number;
  notRecordedCount?: number;
};

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

const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const todayYmd = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
};

type Props = {
  user: { username: string };
};

export default function TeacherMainPanel({ user }: Props) {
  const teacherId = user.username;
  const [ymd, setYmd] = useState<string>(todayYmd());

  const [classes, setClasses] = useState<TeacherClassLite[]>([]);
  const [loadingLeft, setLoadingLeft] = useState(false);
  const [leftErr, setLeftErr] = useState<string | null>(null);

  const [selected, setSelected] = useState<TeacherClassLite | null>(null);

  const [board, setBoard] = useState<SeatBoardResponse | null>(null);
  const [loadingRight, setLoadingRight] = useState(false);
  const [rightErr, setRightErr] = useState<string | null>(null);

  const pollRef = useRef<number | null>(null);

  useEffect(() => {
    (async () => {
      setLoadingLeft(true);
      setLeftErr(null);
      try {
        const list = await fetchTodayClasses(teacherId, ymd);
        setClasses(list);
        if (list.length > 0) {
          setSelected((prev) => (prev?.classId ? prev : list[0]));
        } else {
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

    load();

    if (pollRef.current) window.clearInterval(pollRef.current);
    pollRef.current = window.setInterval(load, 5000);

    return () => {
      if (pollRef.current) window.clearInterval(pollRef.current);
    };
  }, [selected?.classId, ymd]);

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
        {/* 왼쪽: 오늘 수업 */}
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
                    active ? "border-emerald-400 bg-emerald-50" : "border-gray-200 bg-white hover:bg-gray-50"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="font-semibold text-black">{c.className}</div>
                    <span className="text-xs rounded-full px-2 py-0.5 border border-emerald-300 text-emerald-700 bg-emerald-50">
                      진행
                    </span>
                  </div>
                  <div className="text-sm text-black mt-1">
                    {c.startTime && c.endTime ? `${c.startTime} ~ ${c.endTime}` : "--:-- ~ --:--"}
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

        {/* 오른쪽: 좌석 현황 */}
        <SeatBoardCard
          title={board?.currentClass?.className ?? selected?.className ?? "좌석 현황"}
          date={board?.date ?? ymd}
          board={board}
          loading={loadingRight}
          error={rightErr}
        />
      </div>
    </div>
  );
}
