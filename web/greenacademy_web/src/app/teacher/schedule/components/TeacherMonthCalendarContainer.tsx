"use client";

import React, { useEffect, useMemo, useState } from "react";
import MonthCalendar, { type MonthEvent, type Holiday } from "@/components/ui/calendar/month-calendar";
import api, { type LoginResponse, type ScheduleItem, type CourseLite } from "@/app/lib/api";

const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const ymd = (d: Date) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

/** 해당 월의 [YYYY-MM-01, 다음달 01) **/
function monthRange(base = new Date()) {
  const first = new Date(base.getFullYear(), base.getMonth(), 1);
  const nextFirst = new Date(base.getFullYear(), base.getMonth() + 1, 1);
  first.setHours(0, 0, 0, 0);
  nextFirst.setHours(0, 0, 0, 0);
  return { from: ymd(first), to: ymd(nextFirst) };
}

/** 시간 안전 보정 → "HH:mm" */
function fixHHmm(t?: string | null) {
  if (!t) return null;
  try {
    const [hRaw, mRaw] = t.split(":");
    const h = Number(hRaw);
    const m = mRaw != null ? Number(mRaw) : 0;
    if (Number.isFinite(h)) {
      const hh = h < 10 ? `0${h}` : `${h}`;
      const mm = Number.isFinite(m) ? (m < 10 ? `0${m}` : `${m}`) : "00";
      return `${hh}:${mm}`;
    }
  } catch {}
  return t;
}

/** MonthEvent 배경색(선택) — 강의실별 구분용 간단 팔레트 */
function roomColor(room?: number) {
  if (room == null) return "#E5E7EB"; // gray-200 (미배정)
  const palette = ["#FEE2E2","#E9D5FF","#DBEAFE","#DCFCE7","#FEF9C3","#FFE4E6","#D1FAE5","#FCE7F3"];
  return palette[room % palette.length];
}

export default function TeacherMonthCalendarContainer({ user }: { user: NonNullable<LoginResponse> }) {
  const teacherId = user.username;

  const [baseDate, setBaseDate] = useState(new Date());     // 현재 보고 있는 달
  const [selectedDate, setSelectedDate] = useState<string>(); // 사용자가 클릭한 날짜
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [rows, setRows] = useState<ScheduleItem[]>([]);     // /schedules 응답
  const [courses, setCourses] = useState<CourseLite[]>([]); // 반 이름 매핑용
  const [holidays, setHolidays] = useState<Holiday[]>([]);  // (옵션) 공휴일 데이터

  // 월 변경 시 데이터 로드
  const load = async (d: Date) => {
    const { from, to } = monthRange(d);
    const [r, c] = await Promise.all([
      api.listSchedules(teacherId, from, to), // 실제 날짜별 일정
      api.listMyClasses(teacherId),          // classId → className 매핑
    ]);
    setRows(r ?? []);
    setCourses(c ?? []);
  };

  useEffect(() => {
    (async () => {
      try {
        setErr(null); setLoading(true);
        await load(baseDate);
      } catch (e: any) {
        setErr(e?.message ?? "월간 스케줄을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teacherId, baseDate]);

  // classId → className
  const nameById = useMemo(() => {
    const m = new Map<string, string>();
    for (const c of courses) m.set(c.classId, c.className);
    return m;
  }, [courses]);

  // API 데이터를 MonthEvent로 변환
  const events: MonthEvent[] = useMemo(() => {
    const out: MonthEvent[] = [];
    for (const s of rows) {
      const className = nameById.get(s.classId) || s.title || s.classId;
      const start = fixHHmm(s.startTime) ?? "??:??";
      const end   = fixHHmm(s.endTime)   ?? "??:??";
      const room  = s.roomNumber;

      out.push({
        id: s.scheduleId || `${s.classId}-${s.date}`,
        date: s.date,                       // YYYY-MM-DD
        classId: s.classId,
        title: `${className}${room != null ? ` · Room ${room}` : " · (미배정)"} · ${start}~${end}`,
        startTime: start,
        endTime: end,
        roomNumber: room,
        color: roomColor(room),
      });
    }
    return out;
  }, [rows, nameById]);

  const year = baseDate.getFullYear();
  const month = baseDate.getMonth() + 1; // 1~12

  return (
    <div className="space-y-2">
      {err && <div className="text-red-600 text-sm">{err}</div>}
      {loading ? (
        <div className="text-sm text-gray-600">로딩 중…</div>
      ) : (
        <MonthCalendar
          year={year}
          month={month}
          events={events}
          holidays={holidays}
          selectedDate={selectedDate}
          onDayClick={(d) => setSelectedDate(d)}
          onPrevMonth={() => setBaseDate(new Date(year, (month - 1) - 1, 1))}
          onNextMonth={() => setBaseDate(new Date(year, (month - 1) + 1, 1))}
        />
      )}
    </div>
  );
}
