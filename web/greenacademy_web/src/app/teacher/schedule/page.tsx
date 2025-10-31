"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import MonthCalendar, { type MonthEvent, type Holiday } from "@/components/ui/calendar/month-calendar";
import api, { type CourseLite, type LoginResponse, type ScheduleItem } from "@/app/lib/api";
import { getSession } from "@/app/lib/session";

/** 🇰🇷 고정 공휴일(예시) */
const STATIC_HOLIDAYS: Holiday[] = [
  { date: "2025-01-01", name: "신정" },
  { date: "2025-03-01", name: "삼일절" },
  { date: "2025-05-05", name: "어린이날" },
  { date: "2025-06-06", name: "현충일" },
  { date: "2025-08-15", name: "광복절" },
  { date: "2025-10-03", name: "개천절" },
  { date: "2025-10-09", name: "한글날" },
  { date: "2025-12-25", name: "성탄절" },
];

type CourseDetail = CourseLite & {
  startTime?: string | null;
  endTime?: string | null;
  daysOfWeek?: (number | string)[] | null;
  roomNumbers?: number[] | null;
  roomNumber?: number | null;
};

const ymd = (d: Date) => d.toISOString().slice(0, 10);
const toMin = (hhmm: string) => {
  const [h, m] = (hhmm || "0:0").split(":").map(Number);
  return h * 60 + m;
};
const overlap = (s1: string, e1: string, s2: string, e2: string) =>
  toMin(s1) < toMin(e2) && toMin(s2) < toMin(e1); // [s,e) 반열림

function monthRange(year: number, month: number) {
  const from = new Date(year, month - 1, 1);
  const to = new Date(year, month, 1);
  return { from: ymd(from), to: ymd(to) };
}

export default function TeacherSchedulePage() {
  const router = useRouter();

  // 세션 가드
  const me: LoginResponse | null = getSession();
  if (!me) { if (typeof window !== "undefined") location.href = "/login"; return null; }
  const teacherId = me.username;

  // 월/선택일
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedDate, setSelectedDate] = useState<string | null>(ymd(now));

  // 데이터
  const [courses, setCourses] = useState<CourseDetail[]>([]);
  const [events, setEvents] = useState<MonthEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);

  // 입력 폼
  const [form, setForm] = useState<{
    classId: string;
    date: string;
    title: string;
    startTime: string;
    endTime: string;
    roomNumber?: number | null;
  }>({
    classId: "",
    date: ymd(now),
    title: "",
    startTime: "10:00",
    endTime: "11:00",
    roomNumber: null,
  });

  // 반 → 선택 가능한 방 목록
  const selectableRoomsByClass = useMemo(() => {
    const m = new Map<string, number[]>();
    for (const c of courses) {
      const arr = Array.isArray(c.roomNumbers) && c.roomNumbers.length
        ? (c.roomNumbers as number[])
        : (typeof c.roomNumber === "number" ? [c.roomNumber] : []);
      m.set(c.classId, arr);
    }
    return m;
  }, [courses]);

  const currentRoomsForForm = useMemo(
    () => selectableRoomsByClass.get(form.classId) ?? [],
    [form.classId, selectableRoomsByClass]
  );

  // 반 목록 로드
  const loadClasses = async () => {
    setErr(null);
    try {
      const list = await api.listMyClasses(teacherId);
      const details = await Promise.all(
        (list ?? []).map(async (c) => {
          try {
            const d = await api.getClassDetail(c.classId);
            return {
              classId: c.classId,
              className: d.className ?? c.className,
              roomNumber: (d as any).roomNumber ?? (c as any).roomNumber ?? null,
              roomNumbers: (d as any).roomNumbers ?? (c as any).roomNumbers ?? undefined,
              startTime: (d as any).startTime ?? (c as any).startTime ?? null,
              endTime: (d as any).endTime ?? (c as any).endTime ?? null,
            } as CourseDetail;
          } catch {
            return { ...c } as CourseDetail;
          }
        })
      );
      setCourses(details);
      if (!form.classId && details.length) {
        const first = details[0];
        const firstRoom =
          Array.isArray(first.roomNumbers) && first.roomNumbers.length
            ? first.roomNumbers[0]
            : (typeof first.roomNumber === "number" ? first.roomNumber : null);
        setForm((f) => ({ ...f, classId: first.classId, roomNumber: firstRoom }));
      }
    } catch (e: any) {
      setErr(e?.message ?? "반 목록을 불러오지 못했습니다.");
    }
  };

  // 월간 스케줄 불러오기
  const fetchSchedules = async () => {
    setLoading(true); setErr(null); setMsg(null);
    try {
      const { from, to } = monthRange(year, month);
      const rows: ScheduleItem[] = await api.listSchedules(teacherId, from, to);
      const evs: MonthEvent[] = (rows ?? []).map((s) => ({
        id: s.scheduleId,
        classId: s.classId as any,
        date: s.date,
        title: s.title ?? s.classId,
        startTime: s.startTime as any,
        endTime: s.endTime as any,
        // MonthEvent는 roomNumber?: number (undefined 허용, null 은 X)
        roomNumber: (s.roomNumber ?? undefined) as any,
        color: "#fecaca",
      }));
      setEvents(evs);
    } catch (e: any) {
      setErr(e?.message ?? "스케줄을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadClasses(); /* eslint-disable-next-line */ }, [teacherId]);
  useEffect(() => { fetchSchedules(); /* eslint-disable-next-line */ }, [teacherId, year, month]);

  // 날짜 선택
  const onDayClick = (d: string) => { setSelectedDate(d); setForm((f) => ({ ...f, date: d })); };

  /* 생성 — 프론트 사전검사(같은 방 + 같은 시간) + 서버 메시지 처리 */
  const createOne = async () => {
  setErr(null); setMsg(null);

  if (!form.classId || !form.date) { setErr("필수값이 비었습니다."); return; }

  // 방 필수: 같은 방 중복 체크를 위해 요구
  const room = form.roomNumber ?? (() => {
    const rooms = selectableRoomsByClass.get(form.classId) ?? [];
    return rooms.length ? rooms[0] : null;
  })();
  if (room == null) {
    setErr("강의실을 선택해주세요.");
    return;
  }

  // 기준 시간: 코스 시간 → 폼 시간 폴백
  const course = courses.find(c => c.classId === form.classId);
  const s = (course?.startTime && /^\d{2}:\d{2}$/.test(course.startTime)) ? course.startTime : form.startTime;
  const e = (course?.endTime   && /^\d{2}:\d{2}$/.test(course.endTime))   ? course.endTime   : form.endTime;

  const toMin = (hhmm: string) => {
    const m = hhmm?.match(/^(\d{2}):(\d{2})$/); if (!m) return -1;
    return parseInt(m[1]) * 60 + parseInt(m[2]);
  };
  const overlap = (aS: string, aE: string, bS: string, bE: string) =>
    toMin(aS) >= 0 && toMin(aE) >= 0 && toMin(bS) >= 0 && toMin(bE) >= 0 &&
    toMin(aS) < toMin(bE) && toMin(bS) < toMin(aE);

  try {
    // 당일 스케줄 조회
    const day = await api.getDaySchedules(teacherId, form.date);

    // 1) 같은 방 + 시간 겹침
    const roomClash = day.some(ev =>
      typeof ev.roomNumber === "number" &&
      ev.roomNumber === room &&
      overlap(s, e, ev.startTime || "", ev.endTime || "")
    );
    if (roomClash) { setErr("같은 강의실에서 같은 시간대에 이미 수업이 있습니다."); return; }

    // 2) 시간만 겹침(방 무관)
    const timeClash = day.some(ev =>
      overlap(s, e, ev.startTime || "", ev.endTime || "")
    );
    if (timeClash) { setErr("해당 시간대에 이미 다른 수업이 있습니다."); return; }
  } catch (_) {
    // 조회 실패해도 최종 판단은 서버가 함 (409 처리)
  }

  try {
    await api.createSchedule(teacherId, {
      date: form.date,
      classId: form.classId,
      title: form.title || undefined,
      startTime: s,
      endTime: e,
      roomNumber: room, // 반드시 보냄
    });
    setMsg("스케줄을 추가했습니다.");
    await fetchSchedules();
  } catch (e: any) {
    const m = String(e?.message ?? "");
    if (m.includes("room conflict")) setErr("같은 강의실에서 같은 시간대에 이미 수업이 있습니다.");
    else if (m.includes("time conflict")) setErr("해당 시간대에 이미 다른 수업이 있습니다.");
    else setErr(m || "스케줄 추가 실패");
  }
};

  // 삭제
  const deleteOne = async (ev: MonthEvent) => {
    try {
      setErr(null); setMsg(null);
      if (ev.id) await api.deleteSchedule(teacherId, ev.id);
      await fetchSchedules();
      setMsg("삭제했습니다.");
    } catch (e: any) { setErr(e?.message ?? "삭제 실패"); }
  };

  // 선택 날짜 카드
  const selectedEvents = useMemo(
    () => events.filter((ev) => ev.date === (selectedDate ?? "")),
    [events, selectedDate]
  );

  return (
    <div className="min-h-screen bg-white text-gray-900">
      <div className="max-w-7xl mx-auto px-6 py-6">
        {/* 헤더 */}
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-2xl font-bold">월간 스케줄</h1>
          <button
            onClick={() => router.push("/")}
            className="px-4 py-2 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700"
            title="메인으로"
          >
            ← 메인으로
          </button>
        </div>

        <div className="grid lg:grid-cols-[1fr_360px] gap-6">
          {/* 달력 */}
          <div className="bg-white border border-black rounded-xl p-3">
            <MonthCalendar
              year={year}
              month={month}
              events={events}
              holidays={STATIC_HOLIDAYS}
              selectedDate={selectedDate ?? undefined}
              onDayClick={onDayClick}
              onPrevMonth={() => {
                const m = month - 1;
                if (m < 1) { setYear(y => y - 1); setMonth(12); } else setMonth(m);
              }}
              onNextMonth={() => {
                const m = month + 1;
                if (m > 12) { setYear(y => y + 1); setMonth(1); } else setMonth(m);
              }}
            />
          </div>

          {/* 입력 폼 + 카드 */}
          <div className="space-y-4">
            <div className="bg-white border border-black rounded-xl p-4">
              <div className="text-lg font-semibold mb-2">스케줄 입력</div>

              <div className="space-y-3">
                {/* 날짜 */}
                <div>
                  <label className="block text-sm">날짜</label>
                  <input
                    type="date"
                    value={form.date}
                    onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
                    className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
                  />
                </div>

                {/* 반 */}
                <div>
                  <label className="block text-sm">반(수업)</label>
                  <select
                    value={form.classId}
                    onChange={(e) => {
                      const cid = e.target.value;
                      const rooms = selectableRoomsByClass.get(cid) ?? [];
                      setForm((f) => ({ ...f, classId: cid, roomNumber: rooms[0] ?? null }));
                    }}
                    className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
                  >
                    {courses.map((c) => (
                      <option key={c.classId} value={c.classId}>{c.className}</option>
                    ))}
                  </select>
                </div>

                {/* 강의실 */}
                <div>
                  <label className="block text-sm">강의실</label>
                  <select
                    value={form.roomNumber ?? ""}
                    onChange={(e) => setForm((f) => ({ ...f, roomNumber: e.target.value ? Number(e.target.value) : null }))}
                    className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
                  >
                    <option value="">선택 안 함</option>
                    {currentRoomsForForm.map((n) => (
                      <option key={n} value={n}>Room {n}</option>
                    ))}
                  </select>
                  {currentRoomsForForm.length === 0 && (
                    <div className="text-xs mt-1">* 이 반에 연결된 강의실이 없습니다. 반 관리에서 강의실을 연결하세요.</div>
                  )}
                </div>

                {/* 제목(선택) */}
                <div>
                  <label className="block text-sm">제목(선택)</label>
                  <input
                    value={form.title}
                    onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                    placeholder="예: 주간 진도 점검"
                    className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
                  />
                </div>

                {/* 시간 */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm">시작</label>
                    <input
                      type="time"
                      value={form.startTime}
                      onChange={(e) => setForm((f) => ({ ...f, startTime: e.target.value }))}
                      className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
                    />
                  </div>
                  <div>
                    <label className="block text-sm">끝</label>
                    <input
                      type="time"
                      value={form.endTime}
                      onChange={(e) => setForm((f) => ({ ...f, endTime: e.target.value }))}
                      className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
                    />
                  </div>
                </div>

                <div className="flex gap-2 items-center">
                  <button onClick={createOne} className="px-3 py-2 rounded bg-emerald-600 text-white">추가</button>
                  {msg && <span className="text-emerald-700">{msg}</span>}
                  {err && <span className="text-red-600">{err}</span>}
                </div>
              </div>
            </div>

            {/* 선택 날짜 카드 */}
            <div className="bg-white border border-black rounded-xl p-4">
              <div className="text-lg font-semibold mb-2">{selectedDate} 스케줄</div>
              {loading ? (
                <div>불러오는 중…</div>
              ) : selectedEvents.length === 0 ? (
                <div>이 날짜에는 스케줄이 없습니다.</div>
              ) : (
                <div className="space-y-2">
                  {selectedEvents.map((ev) => (
                    <div key={ev.id} className="border border-black rounded px-3 py-2 flex items-center justify-between bg-white">
                      <div>
                        <div className="font-semibold">
                          {ev.title} {typeof (ev as any).roomNumber === "number" ? `· R${(ev as any).roomNumber}` : ""}
                        </div>
                        <div className="text-sm">
                          {(ev as any).startTime ?? ""}{(ev as any).endTime ? `~${(ev as any).endTime}` : ""}
                        </div>
                      </div>
                      <button onClick={() => deleteOne(ev)} className="px-3 py-1.5 rounded bg-red-600 text-white">삭제</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
