// src/components/manage/TeacherSchedulePanelInline.tsx
"use client";

import React, { useEffect, useMemo, useState, useCallback } from "react";
import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";

import api, { type LoginResponse, type ScheduleItem, type CourseLite } from "@/app/lib/api";

import Panel, { PanelGrid } from "@/components/ui/Panel";
import WeekCalendar, { type CalendarEvent } from "@/components/ui/calendar/week-calendar";
import MonthCalendar, { type MonthEvent, type Holiday } from "@/components/ui/calendar/month-calendar";
import { roomsApi, type Room } from "@/app/lib/rooms";
import ScheduleEditModal from "@/components/teacher/ScheduleEditModal";

/* ───────── helpers ───────── */
const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const ymd = (d: Date) => `${d.getFullYear()}-${d.getMonth() + 1 < 10 ? "0" : ""}${d.getMonth() + 1}-${d.getDate() < 10 ? "0" : ""}${d.getDate()}`;
function jsToIsoDow(jsDow: number) { return (jsDow === 0 ? 7 : (jsDow as 1|2|3|4|5|6|7)); }
const getRoomNumber = (r: Room) =>
  Number((r as any).roomNumber ?? (r as any).number ?? (r as any).Room_Number);

/** 이번 주 [from, to) */
function weekRange(base = new Date()) {
  const dow = base.getDay();
  const offsetToMon = dow === 0 ? -6 : 1 - dow;
  const mon = new Date(base);
  mon.setDate(base.getDate() + offsetToMon);
  mon.setHours(0, 0, 0, 0);
  const nextMon = new Date(mon);
  nextMon.setDate(mon.getDate() + 7);
  return { from: ymd(mon), to: ymd(nextMon) };
}

/** 해당 월 [from, to) */
function monthRange(base = new Date()) {
  const first = new Date(base.getFullYear(), base.getMonth(), 1, 0, 0, 0, 0);
  const nextFirst = new Date(base.getFullYear(), base.getMonth() + 1, 1, 0, 0, 0, 0);
  return { from: ymd(first), to: ymd(nextFirst) };
}

/* 🎨 파스텔 팔레트 */
const PALETTE = ["#E0F2FE","#FCE7F3","#FEF3C7","#DCFCE7","#EDE9FE","#FFE4E6","#F5F5F4","#D1FAE5","#FDE68A","#E9D5FF"];
const colorByKey = (key: string) => {
  let h = 0; for (let i=0;i<key.length;i++) h = (h*31 + key.charCodeAt(i))>>>0;
  return PALETTE[h % PALETTE.length];
};

/* ✓ user 복구(안전) */
function loadUserFromClient(): LoginResponse | null {
  if (typeof window === "undefined") return null;
  const keys = ["session","login","auth"];
  for (const k of keys) {
    const raw = localStorage.getItem(k);
    if (!raw) continue;
    try {
      const obj = JSON.parse(raw);
      if (obj && typeof obj === "object" && (obj.username || obj.role)) return obj as LoginResponse;
    } catch {}
  }
  return null;
}

/* ───────── 공휴일(옵션) ───────── */
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

/* ================== 스케줄 추가 모달 (월간에서 날짜 클릭 시) ================== */
function ScheduleAddModal({
  open, date, teacherId, academyNumber, onClose, onCreated,
}: {
  open: boolean;
  date: string | null;
  teacherId: string;
  academyNumber?: number | string | null;
  onClose: () => void;
  onCreated: () => void;
}) {
  // ✅ 전체 반 정보 그대로 들고 있음 (roomNumber / roomNumbers 사용)
  const [courses, setCourses] = useState<CourseLite[]>([]);
  const [classId, setClassId] = useState("");

  const [title, setTitle] = useState("");
  const [startTime, setStartTime] = useState("10:00");
  const [endTime, setEndTime] = useState("11:00");

  // ✅ 학원 전체 방 목록 (roomName 포함) + 선택한 반에서 허용된 방만 필터링한 리스트
  const [allRooms, setAllRooms] = useState<Array<{ roomNumber: number; roomName?: string }>>([]);
  const [myRooms, setMyRooms] = useState<Array<{ roomNumber: number; roomName?: string }>>([]);
  const [selectedRoom, setSelectedRoom] = useState<number | null>(null);

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // ▽ 첫 오픈 시: 내 반 목록 + 전체 방 정보를 가져오고, 첫 번째 반 기준으로 필터 초기화
  useEffect(() => {
    if (!open) return;
    (async () => {
      try {
        setErr(null);

        // 1) 내 반 목록
        const list = await api.listMyClasses(teacherId);
        const safeList: CourseLite[] = Array.isArray(list) ? list : [];
        setCourses(safeList);

        let initialClassId = "";
        if (safeList.length > 0) {
          initialClassId = safeList[0].classId;
          setClassId(initialClassId);
        }

        // 2) 학원 전체 방 목록 (이름 포함)
        let roomInfo: Array<{ roomNumber: number; roomName?: string }> = [];
        if (academyNumber != null) {
          const rooms = await roomsApi.listRooms(Number(academyNumber));
          roomInfo = Array.isArray(rooms)
            ? rooms
                .map((r: any) => ({
                  roomNumber: Number(r.roomNumber ?? r.Room_Number ?? r.number),
                  roomName: r.roomName ?? r.name ?? undefined,
                }))
                .filter((r) => Number.isFinite(r.roomNumber))
            : [];
        }
        setAllRooms(roomInfo);

        // 3) 첫 반 기준으로 사용 가능한 방만 필터
        const targetClassId = initialClassId || classId;
        if (targetClassId) {
          const c = safeList.find((x) => x.classId === targetClassId);
          const nums =
            c?.roomNumbers && c.roomNumbers.length > 0
              ? c.roomNumbers
              : c?.roomNumber != null
              ? [c.roomNumber]
              : [];

          const filtered = nums.map((n) => {
            const info = roomInfo.find((r) => r.roomNumber === n);
            return { roomNumber: n, roomName: info?.roomName };
          }).filter((r) => Number.isFinite(r.roomNumber));

          setMyRooms(filtered);
          setSelectedRoom(filtered.length ? filtered[0].roomNumber : null);
        } else {
          setMyRooms([]);
          setSelectedRoom(null);
        }
      } catch (e: any) {
        setErr(e?.message ?? "데이터를 불러오지 못했습니다.");
        setCourses([]);
        setAllRooms([]);
        setMyRooms([]);
        setSelectedRoom(null);
      }
    })();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, teacherId, academyNumber]);

  // ▽ 반 선택이 바뀔 때마다: 그 반에서 사용 가능한 방만 필터
  useEffect(() => {
    if (!open) return;
    if (!classId) {
      setMyRooms([]);
      setSelectedRoom(null);
      return;
    }

    const c = courses.find((x) => x.classId === classId);
    const nums =
      c?.roomNumbers && c.roomNumbers.length > 0
        ? c.roomNumbers
        : c?.roomNumber != null
        ? [c.roomNumber]
        : [];

    if (!nums.length) {
      setMyRooms([]);
      setSelectedRoom(null);
      return;
    }

    const filtered = nums
      .map((n) => {
        const info = allRooms.find((r) => r.roomNumber === n);
        return { roomNumber: n, roomName: info?.roomName };
      })
      .filter((r) => Number.isFinite(r.roomNumber));

    setMyRooms(filtered);
    setSelectedRoom((prev) => {
      if (prev && filtered.some((r) => r.roomNumber === prev)) return prev;
      return filtered.length ? filtered[0].roomNumber : null;
    });
  }, [open, classId, courses, allRooms]);

  const submit = async () => {
    if (!date || !classId) { setErr("날짜/반을 선택하세요."); return; }
    if (!/^\d{2}:\d{2}$/.test(startTime) || !/^\d{2}:\d{2}$/.test(endTime)) {
      setErr("시간은 HH:MM 형식이어야 합니다."); return;
    }
    if (endTime <= startTime) { setErr("종료 시간이 시작 시간보다 늦어야 합니다."); return; }

    try {
      setLoading(true); setErr(null);
      await api.createSchedule(teacherId, {
        date,
        classId,
        title: title || undefined,
        startTime,
        endTime,
        roomNumber: selectedRoom ?? undefined,  // ✅ 선택한 반에서 허용된 방만
      });
      onCreated();
      onClose();
    } catch (e: any) {
      setErr(e?.message ?? "스케줄 추가 실패");
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-[220] bg-black/60 flex items-center justify-center p-4">
      <div className="w-full max-w-xl bg-white rounded-2xl border border-gray-300 p-5 space-y-3 text-black">
        <div className="flex items-center justify-between mb-2">
          <h2 className="font-semibold text-black">스케줄 추가</h2>
          <button onClick={onClose} className="px-3 py-1 rounded border text-black">닫기</button>
        </div>

        <div className="text-sm text-gray-700">날짜: <span className="text-black">{date}</span></div>

        <div>
          <label className="block text-sm mb-1 text-black">반 선택</label>
          <select
            className="border rounded px-2 py-1 w-full text-black"
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
          >
            {courses.map(c => (
              <option key={c.classId} value={c.classId} className="text-black">
                {c.className}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm mb-1 text-black">강의실 선택</label>
          <div className="border rounded-xl p-2 flex flex-wrap gap-2 min-h-[40px]">
            {myRooms.length === 0 ? (
              <div className="text-sm text-gray-600">이 반에 연결된 강의실이 없습니다.</div>
            ) : myRooms.map(r => {
              const active = selectedRoom === r.roomNumber;
              return (
                <button
                  key={r.roomNumber}
                  type="button"
                  onClick={() => setSelectedRoom(r.roomNumber)}
                  className={`px-4 py-1.5 rounded-full ring-1 text-sm ${
                    active ? "bg-black text-white ring-black" : "bg-white text-black ring-gray-300 hover:bg-gray-50"
                  }`}
                  title={r.roomName ? `${r.roomName} (#${r.roomNumber})` : `Room ${r.roomNumber}`}
                >
                  {r.roomName ? `${r.roomName} (${r.roomNumber})` : `Room ${r.roomNumber}`}
                </button>
              );
            })}
          </div>
          {myRooms.length > 0 && (
            <div className="mt-1 text-xs text-gray-600">이 반에서 사용 가능한 방 {myRooms.length}개</div>
          )}
        </div>

        <div>
          <label className="block text-sm mb-1 text-black">제목(선택)</label>
          <input className="border rounded px-2 py-1 w-full text-black"
                 value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>

        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="block text-sm text-black">시작</label>
            <input type="time" className="border rounded px-2 py-1 w-full text-black"
                   value={startTime} onChange={(e) => setStartTime(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm text-black">끝</label>
            <input type="time" className="border rounded px-2 py-1 w-full text-black"
                   value={endTime} onChange={(e) => setEndTime(e.target.value)} />
          </div>
        </div>

        {err && <div className="text-red-600 text-sm">{err}</div>}
        <div className="flex gap-2 items-center">
          <button
            onClick={submit}
            disabled={loading}
            className="px-4 py-2 rounded bg-emerald-600 text-white disabled:opacity-50"
          >
            추가
          </button>
        </div>
      </div>
    </div>
  );
}

/* ==== ClassDetail 패널 모달 (이벤트 클릭 시 classId 열기) ==== */
const ClassDetailClient = dynamic(
  () => import("@/app/teacher/classes/[classId]/ClassDetailClient"),
  { ssr: false }
);

function ClassDetailPanelModal({
  open, classId, onClose,
}: {
  open: boolean;
  classId: string | null;
  onClose: () => void;
}) {
  if (!open || !classId) return null;
  return (
    <div className="fixed inset-0 z-[230] bg-black/60 flex items-center justify-center p-4">
      <div className="w-full max-w-6xl h-[90vh] bg-white rounded-2xl border border-gray-300 shadow-2xl overflow-hidden">
        <ClassDetailClient classId={classId} asPanel onClose={onClose} />
      </div>
    </div>
  );
}

/* ================== 월간 모달 ================== */

function MonthCenterModal({
  open, onClose, teacherId, academyNumber, onChanged,
}: {
  open: boolean;
  onClose: () => void;
  teacherId: string;
  academyNumber?: number | string | null;
  /** 모달에서 스케줄이 바뀌었을 때(추가/수정/삭제/닫기 후) 주간 캘린더 갱신용 콜백 */
  onChanged?: () => void;
}) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [events, setEvents] = useState<MonthEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 날짜 클릭 → 추가 모달
  const [selectedDate, setSelectedDate] = useState<string>(() => ymd(now));
  const [addOpen, setAddOpen] = useState(false);

  // ✅ 이벤트 클릭 → 수정 모달
  const [editOpen, setEditOpen] = useState(false);
  const [editEvent, setEditEvent] = useState<MonthEvent | null>(null);

  const dayEvents = useMemo(() => events.filter(e => e.date === selectedDate), [events, selectedDate]);

  const fetchMonth = useCallback(async (y = year, m = month) => {
    setLoading(true); setErr(null);
    try {
      const first = new Date(y, m - 1, 1);
      const { from, to } = monthRange(first);
      const rows: ScheduleItem[] = await api.listSchedules(teacherId, from, to);
      const mapped: MonthEvent[] = (rows ?? []).map(s => {
        const safeDate = s.date ? String(s.date).slice(0,10) : from;
        const key = s.classId || s.title || "event";
        return {
          id: s.scheduleId || `${s.classId}-${safeDate}-${s.startTime ?? ""}`,
          date: safeDate,
          title: (s.title && String(s.title).trim()) || (s.classId ?? "수업"),
          classId: s.classId,
          startTime: s.startTime ?? undefined,
          endTime: s.endTime ?? undefined,
          roomNumber: s.roomNumber ?? undefined,
          color: colorByKey(key),
        };
      });
      setEvents(mapped);
    } catch (e: any) {
      setErr(e?.message ?? "스케줄을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [teacherId, year, month]);

  useEffect(() => { if (open) void fetchMonth(); }, [open, fetchMonth]);

  const onPrev = () => setMonth(m => { if (m === 1) { setYear(y => y - 1); return 12; } return m - 1; });
  const onNext = () => setMonth(m => { if (m === 12) { setYear(y => y + 1); return 1; } return m + 1; });

  // ✅ 닫기 + 주간 새로고침
  const handleClose = () => {
    onClose();
    onChanged?.();
  };

  // ✅ 일정 삭제 함수
  const handleDelete = async (scheduleId?: string) => {
    if (!scheduleId) return;
    try {
      await api.deleteSchedule(teacherId, scheduleId);
      await fetchMonth();
      onChanged?.();     // 주간 캘린더도 조용히 갱신
    } catch (e: any) {
      alert(e?.message ?? "삭제 실패");
    }
  };

  // ✅ 수정 저장 함수 (지금은 create로 새로 만드는 구조 유지)
  const handleSave = async (patch: {
    date: string; classId: string; title: string; startTime: string; endTime: string; roomNumber?: number;
  }) => {
    await api.createSchedule(teacherId, patch);
    await fetchMonth();
    onChanged?.();
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[200] bg-black/50 flex items-center justify-center p-4">
      <div className="w-full max-w-5xl max-h-[90vh] bg-white rounded-2xl border border-gray-300 shadow-2xl flex flex-col text-black">
        {/* header */}
        <div className="flex items-center justify-between px-4 h-14 border-b">
          <div className="font-semibold text-black">월간 스케줄</div>
          <div className="flex items-center gap-2">
            {loading && <span className="text-xs text-gray-600">불러오는 중…</span>}
            <button onClick={handleClose} className="px-3 py-1.5 rounded border text-black">닫기</button>
          </div>
        </div>

        {/* body */}
        <div className="p-4 overflow-auto">
          {err && <div className="mb-2 text-red-600">{err}</div>}

          <MonthCalendar
            year={year}
            month={month}
            holidays={STATIC_HOLIDAYS}
            events={events}
            selectedDate={selectedDate}
            onDayClick={(d) => {
              setSelectedDate(d);
              setAddOpen(true); // 날짜 클릭 → 스케줄 추가
            }}
            onPrevMonth={onPrev}
            onNextMonth={onNext}
            onEventClick={(ev) => {
              setEditEvent(ev);
              setEditOpen(true);
            }}
          />

          {/* 선택 날짜 리스트 */}
          <div className="mt-4">
            <div className="font-semibold text-black mb-2">{selectedDate} 스케줄</div>
            {dayEvents.length === 0 ? (
              <div className="text-sm text-gray-700">이 날짜에는 스케줄이 없습니다.</div>
            ) : (
              <div className="space-y-2">
                {dayEvents.map(ev => (
                  <div key={ev.id} className="border rounded px-3 py-2 bg-white flex items-center justify-between">
                    <div>
                      <div className="font-medium text-black">
                        {ev.title}{typeof ev.roomNumber === "number" ? ` · R${ev.roomNumber}` : ""}
                      </div>
                      <div className="text-sm text-gray-800">
                        {ev.startTime ?? ""}{ev.endTime ? ` ~ ${ev.endTime}` : ""}
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => { setEditEvent(ev); setEditOpen(true); }}
                        className="px-3 py-1.5 rounded border text.black"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDelete(ev.id)}
                        className="px-3 py-1.5 rounded bg-red-600 text-white"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 날짜 클릭 → 스케줄 추가 모달 */}
      <ScheduleAddModal
        open={addOpen}
        date={selectedDate}
        teacherId={teacherId}
        academyNumber={academyNumber}
        onClose={() => setAddOpen(false)}
        onCreated={async () => {
          await fetchMonth();
          onChanged?.();
        }}
      />

      {/* 일정 클릭 → 스케줄 수정 모달 */}
      <ScheduleEditModal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        event={editEvent && {
          id: editEvent.id,
          date: editEvent.date,
          classId: editEvent.classId ?? "",
          title: editEvent.title ?? "",
          startTime: editEvent.startTime,
          endTime: editEvent.endTime,
          roomNumber: editEvent.roomNumber ?? "",
        }}
        onSave={handleSave}
        onDelete={(id) => (id ? handleDelete(id) : Promise.resolve())}
        teacherId={teacherId}
        academyNumber={academyNumber}
      />
    </div>
  );
}

/* ================== 메인(주간 + 월간 모달 버튼) ================== */
export default function TeacherSchedulePanelInline({ user: userProp }: { user?: LoginResponse | null }) {
  const router = useRouter();

  const [user, setUser] = useState<LoginResponse | null>(userProp ?? null);
  useEffect(() => { setUser(userProp ?? loadUserFromClient()); }, [userProp]);

  if (!user) {
    return (
      <div className="space-y-4">
        <Panel title="캘린더">
          <div className="text-sm text-gray-700">
            로그인 정보가 없습니다.{" "}
            <button onClick={() => router.push("/login")} className="underline text-emerald-700">
              로그인
            </button>{" "}
            후 다시 시도하세요.
          </div>
        </Panel>
      </div>
    );
  }

  const teacherId = user.username;
  const academyNumber = user.academyNumbers?.[0] ?? null;

  const [baseDate] = useState<Date>(new Date());
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomFilter, setRoomFilter] = useState<string>("ALL");
  const [rows, setRows] = useState<ScheduleItem[]>([]);
  const [openMonth, setOpenMonth] = useState(false);

  const [classOpen, setClassOpen] = useState(false);
  const [classIdForPanel, setClassIdForPanel] = useState<string | null>(null);

  const range = useMemo(() => weekRange(baseDate), [baseDate]);

  const loadByRange = useCallback(async () => {
    setErr(null);
    try {
      const data = await api.listSchedules(teacherId, range.from, range.to);
      setRows(data ?? []);
    } catch (e: any) {
      setErr(e?.message ?? "스케줄을 불러오지 못했습니다.");
    }
  }, [teacherId, range.from, range.to]);

  useEffect(() => {
    (async () => {
      setLoading(true);
      await loadByRange();
      setLoading(false);
    })();
  }, [loadByRange]);

  useEffect(() => {
    (async () => {
      if (!academyNumber) return;
      try {
        const list = await roomsApi.listRooms(Number(academyNumber));
        setRooms(Array.isArray(list) ? list : []);
      } catch (e) {
        console.warn("roomsApi.listRooms 실패:", e);
      }
    })();
  }, [academyNumber]);

  const weekEvents: CalendarEvent[] = useMemo(() => {
    const out: CalendarEvent[] = [];
    for (const s of rows) {
      if (roomFilter !== "ALL") {
        const rn = Number(roomFilter);
        if (s.roomNumber == null || Number(s.roomNumber) !== rn) continue;
      }
      const d = new Date(`${s.date ?? ""}T00:00:00`);
      const key = s.classId || s.title || "event";
      out.push({
        id: s.scheduleId || `${s.classId}-${s.date ?? ""}-${s.startTime ?? ""}`,
        title: (s.title && String(s.title).trim()) || s.classId,
        room: s.roomNumber != null ? `Room ${s.roomNumber}` : undefined,
        dayOfWeek: jsToIsoDow(d.getDay()),
        startTime: (s.startTime as any) || "00:00",
        endTime:   (s.endTime   as any) || "23:59",
        color: colorByKey(key),
      });
    }
    return out;
  }, [rows, roomFilter]);

  return (
    <div className="space-y-4">
      {err && (
        <Panel title="오류">
          <div className="text-red-600">{err}</div>
        </Panel>
      )}

      <PanelGrid>
        <Panel
          title="주간 캘린더"
          className="md:col-span-2"
          right={
            <div className="min-w-[320px] flex flex-wrap items-center gap-3 justify-end">
              <div className="flex items-center gap-2">
                <label className="text-xs text-gray-700">방</label>
                <select
                  value={roomFilter}
                  onChange={(e) => setRoomFilter(e.target.value)}
                  className="border rounded px-2 py-1 text-sm text-black"
                >
                  <option value="ALL" className="text-black">전체</option>
                  {rooms.map((r) => {
                    const rn = getRoomNumber(r);
                    return (
                      <option key={String(rn)} value={String(rn)} className="text-black">
                        Room {rn}
                      </option>
                    );
                  })}
                </select>
              </div>

              <button
                onClick={() => setOpenMonth(true)}
                className="px-3 py-1.5 rounded bg-black text-white text-sm hover:bg-black/90"
              >
                월간 보기
              </button>
            </div>
          }
        >
          {loading ? (
            <div className="text-sm text-gray-700">로딩 중…</div>
          ) : (
            <div className="px-4 sm:px-6 w-full">
              <WeekCalendar
                startHour={8}
                endHour={22}
                events={weekEvents}
                lineColor="rgba(0,0,0,0.18)"
                textColor="#111111"
                showNowLine
                onEventClick={(ev) => {
                  const r = rows.find(x =>
                    (x.scheduleId && ev.id === x.scheduleId) ||
                    (!x.scheduleId && ev.id === `${x.classId}-${x.date ?? ""}-${x.startTime ?? ""}`)
                  );
                  if (r?.classId) {
                    setClassIdForPanel(r.classId);
                    setClassOpen(true);
                  }
                }}
              />
            </div>
          )}
        </Panel>
      </PanelGrid>

      <MonthCenterModal
        open={openMonth}
        onClose={() => setOpenMonth(false)}
        teacherId={user.username}
        academyNumber={academyNumber}
        onChanged={loadByRange}
      />

      <ClassDetailPanelModal
        open={classOpen}
        classId={classIdForPanel}
        onClose={() => setClassOpen(false)}
      />
    </div>
  );
}
