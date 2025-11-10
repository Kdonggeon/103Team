"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import MonthCalendar, { type MonthEvent, type Holiday } from "@/components/ui/calendar/month-calendar";
import api, { type ScheduleItem } from "@/app/lib/api";
import ScheduleEditModal from "@/components/teacher/ScheduleEditModal";

/** 🇰🇷 고정 공휴일 */
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

/** YYYY-MM-DD 유틸 */
const ymd = (d: Date) => {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
};

/** 월 범위 계산 */
function monthRange(year: number, month: number) {
  const from = new Date(year, month - 1, 1);
  const to = new Date(year, month, 1);
  return { from: ymd(from), to: ymd(to) };
}

export type MonthScheduleModalProps = {
  open: boolean;
  onClose: () => void;
  teacherId: string;
  academyNumber?: number | string | null;
};

export default function MonthScheduleModal({
  open,
  onClose,
  teacherId,
  academyNumber,
}: MonthScheduleModalProps) {
  if (!open) return null;

  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedDate, setSelectedDate] = useState<string>(ymd(now));

  const [events, setEvents] = useState<MonthEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);

  // 편집 모달
  const [editOpen, setEditOpen] = useState(false);
  const [editEv, setEditEv] = useState<MonthEvent | null>(null);

  /** 월간 스케줄 로드 */
  const fetchSchedules = useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const { from, to } = monthRange(year, month);
      const rows: ScheduleItem[] = await api.listSchedules(teacherId, from, to);

      const evs: MonthEvent[] = (rows ?? []).map((s) => ({
        id: s.scheduleId || `${s.classId}-${s.date}-${s.startTime ?? ""}`,
        date: s.date,
        title: s.title ?? s.classId ?? "(제목 없음)",
        classId: s.classId,
        startTime: s.startTime ?? undefined,
        endTime: s.endTime ?? undefined,
        roomNumber: typeof s.roomNumber === "number" ? s.roomNumber : undefined,
        color: "#dcfce7",
      }));

      setEvents(evs);
    } catch (e: any) {
      setErr(e?.message ?? "스케줄을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [teacherId, year, month]);

  /** 일정 삭제 */
  const handleDelete = async (scheduleId?: string) => {
    if (!scheduleId) return;
    try {
      await api.deleteSchedule(teacherId, scheduleId);
      await fetchSchedules();
      setMsg("삭제했습니다.");
    } catch (e: any) {
      setErr(e?.message ?? "삭제 실패");
    }
  };

  /** 일정 저장 */
  const handleSave = async (patch: {
    date: string;
    classId: string;
    title: string;
    startTime: string;
    endTime: string;
    roomNumber?: number;
  }) => {
    try {
      await api.createSchedule(teacherId, patch);
      await fetchSchedules();
      setMsg("저장했습니다.");
    } catch (e: any) {
      setErr(e?.message ?? "저장 실패");
    }
  };

  useEffect(() => {
    if (open) fetchSchedules();
  }, [open, fetchSchedules]);

  const onPrev = () =>
    setMonth((m) => {
      if (m === 1) {
        setYear((y) => y - 1);
        return 12;
      }
      return m - 1;
    });
  const onNext = () =>
    setMonth((m) => {
      if (m === 12) {
        setYear((y) => y + 1);
        return 1;
      }
      return m + 1;
    });

  const selectedEvents = useMemo(
    () => events.filter((ev) => ev.date === selectedDate),
    [events, selectedDate]
  );

  return (
    <div className="fixed inset-0 z-[200] bg-black/50 flex items-center justify-center p-4">
      <div className="w-full max-w-6xl max-h-[90vh] overflow-hidden rounded-2xl bg-white border border-gray-300 shadow-2xl flex flex-col">
        {/* header */}
        <div className="flex items-center justify-between px-5 h-12 border-b">
          <div className="font-semibold text-gray-900">월간 스케줄</div>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-600">
              {loading ? "불러오는 중…" : ""}
              {err ? <span className="text-red-600 ml-2">{err}</span> : null}
              {msg ? <span className="text-emerald-700 ml-2">{msg}</span> : null}
            </span>
            <button onClick={onClose} className="px-3 py-1.5 rounded border hover:bg-gray-50">
              닫기
            </button>
          </div>
        </div>

        {/* body */}
        <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-4 p-4 overflow-auto">
          {/* 달력 */}
          <div className="bg-white border border-gray-200 rounded-xl p-3">
            <MonthCalendar
              year={year}
              month={month}
              events={events}
              holidays={STATIC_HOLIDAYS}
              selectedDate={selectedDate}
              onDayClick={(d) => setSelectedDate(d)}
              onPrevMonth={onPrev}
              onNextMonth={onNext}
              onEventClick={(ev) => {
                setEditEv(ev);
                setEditOpen(true);
              }}
            />
          </div>

          {/* 오른쪽 패널 */}
          <div className="space-y-3">
            <div className="text-lg font-semibold text-gray-900">{selectedDate} 스케줄</div>
            {selectedEvents.length === 0 ? (
              <div className="text-sm text-gray-600">이 날짜에는 스케줄이 없습니다.</div>
            ) : (
              <div className="space-y-2">
                {selectedEvents.map((ev) => (
                  <div
                    key={ev.id}
                    className="border border-gray-300 rounded px-3 py-2 flex items-center justify-between bg-white"
                  >
                    <div className="min-w-0">
                      <div className="font-medium truncate text-black">
                        {ev.title}
                        {typeof ev.roomNumber === "number" ? ` · R${ev.roomNumber}` : ""}
                      </div>
                      <div className="text-sm text-gray-700">
                        {ev.startTime ?? ""} {ev.endTime ? `~ ${ev.endTime}` : ""}
                      </div>
                    </div>
                    <button
                      onClick={() => {
                        setEditEv(ev);
                        setEditOpen(true);
                      }}
                      className="px-3 py-1.5 rounded border text-sm hover:bg-gray-50"
                    >
                      수정
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 스케줄 수정 모달 */}
      <ScheduleEditModal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        event={
          editEv && {
            id: editEv.id,
            date: editEv.date,
            classId: editEv.classId ?? "",
            title: editEv.title ?? "",
            startTime: editEv.startTime,
            endTime: editEv.endTime,
            roomNumber: editEv.roomNumber ?? "",
          }
        }
        onSave={handleSave}
        onDelete={handleDelete}
        teacherId={teacherId}               // ✅ 자동 반/강의실 로드용
        academyNumber={academyNumber}       // ✅ 자동 반/강의실 로드용
      />
    </div>
  );
}
