// src/app/teacher/TeacherSchedulePanelInline.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import api, {
  type LoginResponse,
  type CourseLite,
  type ScheduleItem,
} from "@/app/lib/api";
import WeekCalendar, { type CalendarEvent } from "@/components/ui/calendar/week-calendar";
import { roomsApi, type Room } from "@/app/lib/rooms";

/* ───────── helpers ───────── */
function normalizeDays(days: any): number[] {
  if (!days) return [];
  const mapFullWidth: Record<string, string> = { "１":"1","２":"2","３":"3","４":"4","５":"5","６":"6","７":"7" };
  const mapKorean: Record<string, number> = { "월":1,"화":2,"수":3,"목":4,"금":5,"토":6,"일":7 };
  return (Array.isArray(days) ? days : [days])
    .map((d: any) => {
      if (typeof d === "number") return d;
      if (typeof d === "string") {
        const s = mapFullWidth[d] ?? d;
        if (mapKorean[s]) return mapKorean[s];
        const n = parseInt(s, 10);
        if (n >= 1 && n <= 7) return n;
      }
      return null;
    })
    .filter(Boolean) as number[];
}

const getRoomNumber = (r: Room) =>
  Number((r as any).roomNumber ?? (r as any).number ?? (r as any).Room_Number);

const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const ymd = (d: Date) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

/** 이번 주(월~다음주 월) 범위 */
function thisWeekRange(today = new Date()) {
  const dow = today.getDay(); // 0..6 (Sun..Sat)
  const offsetToMon = dow === 0 ? -6 : 1 - dow;
  const mon = new Date(today);
  mon.setDate(today.getDate() + offsetToMon);
  mon.setHours(0, 0, 0, 0);
  const nextMon = new Date(mon);
  nextMon.setDate(mon.getDate() + 7);
  return { from: ymd(mon), to: ymd(nextMon) }; // [from, to)
}

/** JS getDay(0..6) → 1..7(Mon=1..Sun=7) */
function jsToIsoDow(jsDow: number) {
  return (jsDow === 0 ? 7 : jsDow) as 1 | 2 | 3 | 4 | 5 | 6 | 7;
}

const isHHmm = (t?: string | null) => !!t && /^\d{2}:\d{2}$/.test(t);

/* ───────── types ───────── */
type CourseDetail = CourseLite & {
  startTime?: string;
  endTime?: string;
  daysOfWeek?: number[];
  roomNumbers?: number[];
  roomNumber?: number | null;
};

/* ───────── Modal (스케줄 관리) ───────── */
type ManageModalProps = {
  open: boolean;
  onClose: () => void;
  teacherId: string;
  courses: CourseDetail[];
  selectableRoomsByClass: Map<string, number[]>;
  onCreated: () => Promise<void>;
  getDayRows: (date: string) => Promise<ScheduleItem[]>;
  onDelete: (scheduleId: string) => Promise<void>;
};

function ManageModal({
  open, onClose, teacherId, courses,
  selectableRoomsByClass, onCreated, getDayRows, onDelete
}: ManageModalProps) {

  const today = ymd(new Date());
  const [date, setDate] = useState<string>(today);
  const [classId, setClassId] = useState<string>(courses[0]?.classId ?? "");
  const [title, setTitle] = useState<string>("");
  const [startTime, setStartTime] = useState<string>(courses[0]?.startTime ?? "10:00");
  const [endTime, setEndTime] = useState<string>(courses[0]?.endTime ?? "11:00");
  const [roomNumber, setRoomNumber] = useState<number | "">(
    (() => {
      const cid = courses[0]?.classId;
      if (!cid) return "";
      const rooms = selectableRoomsByClass.get(cid) ?? [];
      return rooms[0] ?? "";
    })()
  );

  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [dayRows, setDayRows] = useState<ScheduleItem[]>([]);
  const currentRooms = useMemo(() => selectableRoomsByClass.get(classId) ?? [], [classId, selectableRoomsByClass]);

  useEffect(() => {
    if (!open) return;
    (async () => {
      try {
        const rows = await getDayRows(date);
        setDayRows(rows ?? []);
      } catch {
        setDayRows([]);
      }
    })();
  }, [open, date, getDayRows]);

  useEffect(() => {
    // 반 변경 시 기본 시간/방 초기화
    const c = courses.find(v => v.classId === classId);
    if (c?.startTime && isHHmm(c.startTime)) setStartTime(c.startTime);
    if (c?.endTime && isHHmm(c.endTime)) setEndTime(c.endTime);
    const rooms = selectableRoomsByClass.get(classId) ?? [];
    setRoomNumber(rooms[0] ?? "");
  }, [classId, courses, selectableRoomsByClass]);

  if (!open) return null;

  const createOne = async () => {
    setErr(null); setMsg(null);

    if (!classId || !date) { setErr("필수값이 비었습니다."); return; }
    const rn = typeof roomNumber === "number" ? roomNumber : null;
    if (rn == null) { setErr("강의실을 선택하세요."); return; }

    // 프런트 선검사(겹침)
    try {
      const rows = await getDayRows(date);
      const toMin = (hhmm: string) => parseInt(hhmm.slice(0,2))*60 + parseInt(hhmm.slice(3,5));
      const overlap = (s1:string,e1:string,s2:string,e2:string) => toMin(s1) < toMin(e2) && toMin(s2) < toMin(e1);

      const roomClash = rows.some(r => r.roomNumber === rn && overlap(startTime, endTime, r.startTime || "", r.endTime || ""));
      if (roomClash) { setErr("같은 강의실에서 같은 시간대에 이미 수업이 있습니다."); return; }

      const timeClash = rows.some(r => overlap(startTime, endTime, r.startTime || "", r.endTime || ""));
      if (timeClash) { setErr("해당 시간대에 이미 다른 수업이 있습니다."); return; }
    } catch { /* 서버가 최종 판단 */ }

    try {
      await api.createSchedule(teacherId, { date, classId, title: title || undefined, startTime, endTime, roomNumber: rn });
      setMsg("스케줄을 추가했습니다.");
      await onCreated();
      setDayRows(await getDayRows(date));
    } catch (e: any) {
      const m = String(e?.message ?? "");
      if (m.includes("room conflict")) setErr("같은 강의실에서 같은 시간대에 이미 수업이 있습니다.");
      else if (m.includes("time conflict")) setErr("해당 시간대에 이미 다른 수업이 있습니다.");
      else setErr(m || "스케줄 추가 실패");
    }
  };

  const deleteOne = async (scheduleId: string) => {
    setErr(null); setMsg(null);
    try {
      await onDelete(scheduleId);
      setMsg("삭제했습니다.");
      setDayRows(await getDayRows(date));
      await onCreated();
    } catch (e: any) {
      setErr(e?.message ?? "삭제 실패");
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl bg-white rounded-xl border border-gray-300 shadow-lg p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="text-lg font-semibold">스케줄 관리</div>
          <button onClick={onClose} className="px-3 py-1.5 rounded border">닫기</button>
        </div>

        <div className="grid md:grid-cols-[1fr_1fr] gap-4">
          {/* 좌: 입력 */}
          <div className="space-y-3">
            <div>
              <label className="block text-sm">날짜</label>
              <input type="date" value={date} onChange={(e)=>setDate(e.target.value)}
                     className="border rounded px-2 py-1 w-full"/>
            </div>
            <div>
              <label className="block text-sm">반(수업)</label>
              <select value={classId} onChange={(e)=>setClassId(e.target.value)}
                      className="border rounded px-2 py-1 w-full">
                {courses.map(c => <option key={c.classId} value={c.classId}>{c.className}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm">강의실</label>
              <select value={roomNumber === "" ? "" : String(roomNumber)}
                      onChange={(e)=>setRoomNumber(e.target.value ? Number(e.target.value) : "")}
                      className="border rounded px-2 py-1 w-full">
                <option value="">선택 안 함</option>
                {currentRooms.map(n => <option key={n} value={n}>Room {n}</option>)}
              </select>
              {currentRooms.length === 0 && (
                <div className="text-xs mt-1 text-gray-500">* 이 반에 연결된 강의실이 없습니다.</div>
              )}
            </div>
            <div>
              <label className="block text-sm">제목(선택)</label>
              <input value={title} onChange={(e)=>setTitle(e.target.value)}
                     placeholder="예: 주간 진도 점검"
                     className="border rounded px-2 py-1 w-full"/>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm">시작</label>
                <input type="time" value={startTime}
                       onChange={(e)=>setStartTime(e.target.value)}
                       className="border rounded px-2 py-1 w-full"/>
              </div>
              <div>
                <label className="block text-sm">끝</label>
                <input type="time" value={endTime}
                       onChange={(e)=>setEndTime(e.target.value)}
                       className="border rounded px-2 py-1 w-full"/>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button onClick={createOne} className="px-3 py-2 rounded bg-emerald-600 text-white">추가</button>
              {msg && <span className="text-emerald-700 text-sm">{msg}</span>}
              {err && <span className="text-red-600 text-sm">{err}</span>}
            </div>
          </div>

          {/* 우: 선택 날짜 스케줄 목록 */}
          <div className="space-y-2">
            <div className="font-semibold">{date} 스케줄</div>
            {dayRows.length === 0 ? (
              <div className="text-sm text-gray-500">이 날짜에는 스케줄이 없습니다.</div>
            ) : dayRows.map(ev => (
              <div key={ev.scheduleId} className="border rounded px-3 py-2 flex items-center justify-between">
                <div>
                  <div className="font-medium">
                    {ev.title || ev.classId}{typeof ev.roomNumber === "number" ? ` · R${ev.roomNumber}` : ""}
                  </div>
                  <div className="text-sm opacity-80">
                    {(ev.startTime ?? "")}{ev.endTime ? ` ~ ${ev.endTime}` : ""}
                  </div>
                </div>
                <button onClick={()=>deleteOne(ev.scheduleId!)} className="px-3 py-1.5 rounded bg-red-600 text-white">삭제</button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ───────── Main Panel ───────── */
export default function TeacherSchedulePanelInline({ user }: { user: NonNullable<LoginResponse> }) {
  const router = useRouter();
  const teacherId = user.username;
  const academyNumber = user.academyNumbers?.[0] ?? null;

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const [courses, setCourses] = useState<CourseDetail[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomFilter, setRoomFilter] = useState<string>("ALL"); // "ALL" | "<roomNumber>"
  const [weekRows, setWeekRows] = useState<ScheduleItem[]>([]);
  const [showDebug, setShowDebug] = useState(false);

  // 모달
  const [openCreate, setOpenCreate] = useState(false);
  const openModal = () => setOpenCreate(true);
  const closeModal = () => setOpenCreate(false);

  /* 반(수업) 기본 정보 */
  const loadClasses = async () => {
    try {
      const list = await api.listMyClasses(teacherId);
      console.log("[ScheduleUI] listMyClasses ok", list?.length);
      const details = await Promise.all(
        (list || []).map(async (c) => {
          try {
            const d = await api.getClassDetail(c.classId);
            const startTime = (d as any).startTime ?? (d as any).Start_Time ?? undefined;
            const endTime   = (d as any).endTime   ?? (d as any).End_Time   ?? undefined;
            const daysRaw   = (d as any).daysOfWeek ?? (d as any).Days_Of_Week ?? undefined;
            const daysOfWeek = normalizeDays(daysRaw);
            const roomNumbers = (d as any).roomNumbers ?? (d as any).Room_Numbers ?? undefined;
            const roomNumber  = (d as any).roomNumber  ?? (d as any).Room_Number  ?? undefined;
            return { ...c, startTime, endTime, daysOfWeek, roomNumbers, roomNumber } as CourseDetail;
          } catch {
            return { ...c } as CourseDetail;
          }
        })
      );
      setCourses(details);
    } catch (e: any) {
      console.error("[ScheduleUI] listMyClasses fail", e);
      setErr(e?.message ?? "시간표(반 정보)를 불러오지 못했습니다.");
    }
  };

  // 이번 주 로드 함수
  const loadThisWeek = async () => {
    setErr(null);
    const { from, to } = thisWeekRange(new Date());
    console.log("[ScheduleUI] listSchedules call", { teacherId, from, to });
    try {
      const rows = await api.listSchedules(teacherId, from, to);
      console.log("[ScheduleUI] listSchedules ok", rows?.length, rows);
      setWeekRows(rows ?? []);
    } catch (e) {
      console.error("[ScheduleUI] listSchedules fail", e);
      setErr((e as any)?.message ?? "이번 주 스케줄을 불러오지 못했습니다.");
    }
  };

  /* 최초 로드 */
  useEffect(() => {
    (async () => {
      console.log("[ScheduleUI] mount teacherId=", teacherId);
      setLoading(true);
      await Promise.all([loadClasses(), loadThisWeek()]);
      setLoading(false);
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teacherId]);

  /* 방(룸) 목록 */
  useEffect(() => {
    (async () => {
      if (!academyNumber) return;
      try {
        console.log("[ScheduleUI] rooms list call", { academyNumber });
        const list = await roomsApi.listRooms(academyNumber);
        console.log("[ScheduleUI] rooms list ok", Array.isArray(list) ? list.length : null);
        setRooms(Array.isArray(list) ? list : []);
      } catch (e) {
        console.warn("roomsApi.listRooms 실패:", e);
      }
    })();
  }, [academyNumber]);

  /* 반→선택 가능한 방 목록 (모달에서 사용) */
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

  /* 모달 콜백들 */
  const handleCreated = async () => { await loadThisWeek(); };
  const getDayRows = async (date: string) => api.getDaySchedules(teacherId, date);
  const handleDelete = async (scheduleId: string) => { await api.deleteSchedule(teacherId, scheduleId); };

  /* 주간 캘린더 이벤트 (WeekCalendar 요구 필드에 정확히 맞춤) */
  const events: CalendarEvent[] = useMemo(() => {
    const out: CalendarEvent[] = [];
    for (const s of weekRows) {
      if (roomFilter !== "ALL") {
        const rn = Number(roomFilter);
        if (s.roomNumber == null || Number(s.roomNumber) !== rn) continue;
      }
      const d = new Date(`${s.date}T00:00:00`);
      out.push({
        id: s.scheduleId || `${s.classId}-${s.date}`,
        title: (s.title && s.title.trim()) || s.classId,
        room: s.roomNumber != null ? `Room ${s.roomNumber}` : undefined,
        dayOfWeek: jsToIsoDow(d.getDay()),      // 1~7 (Mon..Sun)
        startTime: (s.startTime as any) || "00:00",
        endTime:   (s.endTime   as any) || "23:59",
        href: `/teacher/classes/${encodeURIComponent(s.classId)}`,
        color: "#dcfce7",
      });
    }
    console.log("[ScheduleUI] events size =", out.length, out);
    return out;
  }, [weekRows, roomFilter]);

  return (
    <div className="space-y-4">
      {err && <div className="border rounded p-3 text-red-600 bg-red-50">{err}</div>}

      <div className="border rounded-lg p-3">
        <div className="flex items-center justify-between mb-3">
          <div className="text-sm font-semibold">주간 캘린더</div>
          <div className="flex items-center gap-3">
            {/* 룸 필터 */}
            <div className="flex items-center gap-2">
              <label className="text-xs text-gray-600">방 필터</label>
              <select value={roomFilter} onChange={(e)=>setRoomFilter(e.target.value)}
                      className="border rounded px-2 py-1 text-sm">
                <option value="ALL">전체</option>
                {rooms.map((r) => {
                  const rn = getRoomNumber(r);
                  return <option key={String(rn)} value={String(rn)}>Room {rn}</option>;
                })}
              </select>
            </div>

            <button onClick={openModal}
                    className="px-3 py-1.5 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700">
              스케줄 관리(+)
            </button>
            <button onClick={loadThisWeek}
                    className="px-3 py-1.5 rounded border text-sm" title="새로고침">
              새로고침
            </button>
            <label className="ml-2 text-xs flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={showDebug} onChange={(e)=>setShowDebug(e.target.checked)} />
              debug
            </label>
          </div>
        </div>

        {loading ? (
          <div className="text-sm text-gray-600">로딩 중…</div>
        ) : (
          <WeekCalendar startHour={8} endHour={22} events={events} />
        )}
      </div>

      {showDebug && (
        <pre className="text-xs bg-gray-50 border rounded p-2 overflow-auto max-h-80">
          {JSON.stringify({ weekRows, events, academyNumber }, null, 2)}
        </pre>
      )}

      {/* 스케줄 관리 모달 */}
      <ManageModal
        open={openCreate}
        onClose={closeModal}
        teacherId={teacherId}
        courses={courses}
        selectableRoomsByClass={selectableRoomsByClass}
        onCreated={handleCreated}
        getDayRows={getDayRows}
        onDelete={handleDelete}
      />
    </div>
  );
}
