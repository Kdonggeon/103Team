// src/app/student/StudentTimetablePanel.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";
import { getSession } from "@/app/lib/session";

import Panel, { PanelGrid } from "@/components/ui/Panel";
import WeekCalendar, {
  type CalendarEvent,
} from "@/components/ui/calendar/week-calendar";
import MonthCalendar, {
  type MonthEvent,
  type Holiday,
} from "@/components/ui/calendar/month-calendar";

/* ================= 공통 타입 ================= */

type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = {
  role: Role;
  username: string;
  name?: string;
  token?: string;
};

/**
 * 백엔드 /api/students/{id}/timetable (StudentClassSlotDto)와 1:1로 맞춘 슬롯 타입
 * => "어느 날짜에, 어떤 반이, 몇 시~몇 시, 어느 방에서 열리는지" 한 칸
 */
type StudentScheduleSlot = {
  classId: string;
  className: string;
  date: string; // "YYYY-MM-DD"
  dayOfWeek: 1 | 2 | 3 | 4 | 5 | 6 | 7;
  roomNumber?: number | null;
  startTime: string; // "HH:mm"
  endTime: string; // "HH:mm"
  academyNumber?: number | null;
};

const API_BASE = "/backend";

/* ================= 유틸 ================= */

async function apiGet<T>(path: string, token?: string): Promise<T> {
  const url = `${API_BASE}${path}`;
  const r = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    cache: "no-store",
  });
  if (!r.ok) {
    const t = await r.text().catch(() => "");
    throw new Error(`${r.status} ${r.statusText}${t ? ` | ${t}` : ""}`);
  }
  return r.json();
}

// js Date.getDay() → ISO 요일(1=월..7=일)
function jsToIsoDow(js: number): 1 | 2 | 3 | 4 | 5 | 6 | 7 {
  return (js === 0 ? 7 : js) as 1 | 2 | 3 | 4 | 5 | 6 | 7;
}

function ymd(d: Date): string {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

// 주간 범위 표시용: "25.11.11~25.11.17" 형태로 변환
function formatWeekRange(weekStart: string): string {
  // weekStart: "YYYY-MM-DD"
  const start = new Date(weekStart + "T00:00:00");
  const end = new Date(start);
  end.setDate(start.getDate() + 6); // 7일 범위

  const fmt = (d: Date): string => {
    const yy = String(d.getFullYear()).slice(2);
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return `${yy}.${mm}.${dd}`;
  };

  return `${fmt(start)}~${fmt(end)}`;
}

/* 🎨 파스텔 팔레트 (선생이랑 동일) */
const PALETTE = [
  "#E0F2FE",
  "#FCE7F3",
  "#FEF3C7",
  "#DCFCE7",
  "#EDE9FE",
  "#FFE4E6",
  "#F5F5F4",
  "#D1FAE5",
  "#FDE68A",
  "#E9D5FF",
];

const colorByKey = (key: string) => {
  let h = 0;
  for (let i = 0; i < key.length; i++) {
    h = (h * 31 + key.charCodeAt(i)) >>> 0;
  }
  return PALETTE[h % PALETTE.length];
};

/* 공휴일(옵션) – 선생 패널과 동일하게 사용 가능 */
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

/* ================= 월간 모달 (학생용: 읽기 전용, 슬롯 기반) ================= */

type StudentMonthModalProps = {
  open: boolean;
  onClose: () => void;
  studentId?: string;
  token?: string;
};

function StudentMonthModal({
  open,
  onClose,
  studentId,
  token,
}: StudentMonthModalProps) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedDate, setSelectedDate] = useState<string>(ymd(now));

  const [slots, setSlots] = useState<StudentScheduleSlot[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 해당 year/month 전체를 /timetable로 호출해서 슬롯을 가져온다.
  useEffect(() => {
    if (!open) return;
    if (!studentId) {
      setSlots([]);
      return;
    }

    let aborted = false;

    const fetchMonth = async () => {
      try {
        setLoading(true);
        setErr(null);

        const daysInMonth = new Date(year, month, 0).getDate();
        const startStr = `${year}-${String(month).padStart(2, "0")}-01`;

        const raw = await apiGet<StudentScheduleSlot[]>(
          `/api/students/${encodeURIComponent(
            studentId,
          )}/timetable?weekStart=${startStr}&days=${daysInMonth}`,
          token,
        );
        if (aborted) return;
        setSlots(Array.isArray(raw) ? raw : []);
      } catch (e: any) {
        if (aborted) return;
        const msg = String(e?.message ?? "");
        setErr(msg || "월간 시간표를 불러오지 못했습니다.");
        setSlots([]);
      } finally {
        if (!aborted) setLoading(false);
      }
    };

    fetchMonth();

    return () => {
      aborted = true;
    };
  }, [open, studentId, token, year, month]);

  // 슬롯 + year/month → MonthEvent[]
  const events = useMemo<MonthEvent[]>(() => {
    if (!slots.length) return [];
    return slots
      .filter((s) => {
        const [y, m] = s.date.split("-");
        return Number(y) === year && Number(m) === month;
      })
      .map((s) => {
        const key = `${s.classId}-${s.date}`;
        return {
          id: key,
          date: s.date,
          title: s.className,
          classId: s.classId,
          startTime: s.startTime,
          endTime: s.endTime,
          roomNumber:
            typeof s.roomNumber === "number" && !Number.isNaN(s.roomNumber)
              ? s.roomNumber
              : undefined,
          color: colorByKey(key),
        } satisfies MonthEvent;
      });
  }, [slots, year, month]);

  const dayEvents = useMemo(
    () => events.filter((e) => e.date === selectedDate),
    [events, selectedDate],
  );

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

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[200] bg-black/50 flex items-center justify-center p-4">
      <div className="w-full max-w-5xl max-h-[90vh] bg-white rounded-2xl border border-gray-300 shadow-2xl flex flex-col text-black">
        {/* header */}
        <div className="flex items-center justify-between px-4 h-14 border-b">
          <div className="font-semibold text-black">학생 월간 시간표</div>
          <button
            onClick={onClose}
            className="px-3 py-1.5 rounded border text-black"
          >
            닫기
          </button>
        </div>

        {/* body */}
        <div className="p-4 overflow-auto">
          {err && (
            <div className="mb-2 text-xs text-red-600 break-words">
              {err}
            </div>
          )}

          <MonthCalendar
            year={year}
            month={month}
            events={events}
            holidays={STATIC_HOLIDAYS}
            selectedDate={selectedDate}
            onPrevMonth={onPrev}
            onNextMonth={onNext}
            onDayClick={(d) => setSelectedDate(d)}
            // 이벤트 클릭은 현재 아무 동작 X (읽기 전용)
            onEventClick={undefined}
          />

          {/* 아래 선택 날짜 리스트 (읽기 전용) */}
          <div className="mt-4">
            <div className="font-semibold text-black mb-2">
              {selectedDate} 시간표
            </div>
            {loading ? (
              <div className="text-sm text-gray-700">로딩 중…</div>
            ) : dayEvents.length === 0 ? (
              <div className="text-sm text-gray-700">
                이 날짜에는 수업이 없습니다.
              </div>
            ) : (
              <div className="space-y-2">
                {dayEvents.map((ev) => (
                  <div
                    key={ev.id}
                    className="border rounded px-3 py-2 bg-white flex items-center justify-between"
                  >
                    <div>
                      <div className="font-medium text-black">
                        {ev.title}
                        {typeof ev.roomNumber === "number"
                          ? ` · Room ${ev.roomNumber}`
                          : ""}
                      </div>
                      <div className="text-sm text-gray-800">
                        {ev.startTime ?? ""}
                        {ev.endTime ? ` ~ ${ev.endTime}` : ""}
                      </div>
                    </div>
                    {/* 학생은 수정/삭제 버튼 없음 */}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ================= 메인: StudentTimetablePanel ================= */

export default function StudentTimetablePanel() {
  // 1) 훅: 항상 같은 순서로 선언
  const [login, setLogin] = useState<LoginSession | null>(null);

  const [slots, setSlots] = useState<StudentScheduleSlot[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [roomFilter, setRoomFilter] = useState<string>("ALL");
  const [openMonth, setOpenMonth] = useState(false);

  // 🔹 현재 보고 있는 주의 월요일 (YYYY-MM-DD)
  const [weekStart, setWeekStart] = useState<string>(() => {
    const now = new Date();
    const jsDay = now.getDay(); // 0=Sun..6=Sat
    const diff = jsDay === 0 ? -6 : 1 - jsDay; // 월요일로 보정
    const monday = new Date(now);
    monday.setDate(now.getDate() + diff);
    return ymd(monday);
  });

  const handlePrevWeek = () => {
    setWeekStart((prev) => {
      const d = new Date(prev + "T00:00:00");
      d.setDate(d.getDate() - 7);
      return ymd(d);
    });
  };

  const handleNextWeek = () => {
    setWeekStart((prev) => {
      const d = new Date(prev + "T00:00:00");
      d.setDate(d.getDate() + 7);
      return ymd(d);
    });
  };

  // 2) 세션 로드
  useEffect(() => {
    const s = getSession();
    if (s) {
      setLogin({
        role: s.role as Role,
        username: s.username,
        name: s.name ?? undefined,
        token: (s.token ?? "") as string,
      });
    } else {
      setLogin(null);
    }
  }, []);

  // 3) 학생 시간표 슬롯(/timetable) 로드 – weekStart 기준 7일
  useEffect(() => {
    if (!login) {
      setSlots([]);
      return;
    }
    const studentId = login.username;
    const token = login.token ?? "";

    let aborted = false;

    (async () => {
      setLoading(true);
      setErr(null);
      try {
        const raw = await apiGet<StudentScheduleSlot[]>(
          `/api/students/${encodeURIComponent(
            studentId,
          )}/timetable?weekStart=${weekStart}&days=7`,
          token,
        );
        if (aborted) return;
        const list = Array.isArray(raw) ? raw : [];
        setSlots(list);
      } catch (e: any) {
        if (aborted) return;
        const msg = String(e?.message ?? "");
        setErr(msg || "시간표를 불러오지 못했습니다.");
        setSlots([]);
      } finally {
        if (!aborted) setLoading(false);
      }
    })();

    return () => {
      aborted = true;
    };
  }, [login, weekStart]);

  // 4) 방 필터 옵션
  const roomOptions = useMemo(() => {
    const set = new Set<string>();
    for (const s of slots) {
      if (s.roomNumber != null) {
        set.add(String(s.roomNumber));
      }
    }
    return Array.from(set).sort((a, b) => Number(a) - Number(b));
  }, [slots]);

  // 5) 주간 캘린더 이벤트 (현재 weekStart 기준으로 받아온 실제 슬롯들)
  const weekEvents: CalendarEvent[] = useMemo(() => {
    const out: CalendarEvent[] = [];
    slots.forEach((s, idx) => {
      if (
        roomFilter !== "ALL" &&
        (s.roomNumber == null || String(s.roomNumber) !== roomFilter)
      ) {
        return;
      }
      const key = `${s.classId}-${s.date}`;
      out.push({
        id: `${s.classId}-${s.date}-${idx}`,
        title: s.className,
        room:
          s.roomNumber != null
            ? `Room ${s.roomNumber}`
            : undefined,
        dayOfWeek: s.dayOfWeek,
        startTime: s.startTime,
        endTime: s.endTime,
        color: colorByKey(key),
      });
    });
    return out;
  }, [slots, roomFilter]);

  // 6) 렌더링

  if (!login) {
    return (
      <div className="space-y-4">
        <Panel title="캘린더">
          <div className="text-sm text-gray-700">
            로그인 정보가 없습니다.{" "}
            <a href="/login" className="underline text-emerald-700">
              로그인
            </a>{" "}
            후 다시 시도하세요.
          </div>
        </Panel>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {err && (
        <Panel title="오류">
          <div className="text-red-600 text-sm break-words">
            {err}
          </div>
        </Panel>
      )}

      <PanelGrid>
        <Panel
          title="주간 캘린더"
          className="md:col-span-2"
          right={
            <div className="min-w-[320px] flex flex-wrap items-center gap-3 justify-end">
              {/* 방 필터 (선생 UI와 같은 스타일) */}
              <div className="flex items-center gap-2">
                <label className="text-xs text-gray-700">방</label>
                <select
                  value={roomFilter}
                  onChange={(e) => setRoomFilter(e.target.value)}
                  className="border rounded px-2 py-1 text-sm text-black"
                >
                  <option value="ALL" className="text-black">
                    전체
                  </option>
                  {roomOptions.map((rn) => (
                    <option
                      key={rn}
                      value={rn}
                      className="text-black"
                    >
                      Room {rn}
                    </option>
                  ))}
                </select>
              </div>

              {/* 주간 날짜 범위 + 주간 이동 + 월간 보기 */}
              <div className="flex items-center gap-2">
                <span className="text-xs text-gray-700 whitespace-nowrap">
                  {formatWeekRange(weekStart)}
                </span>
                <button
                  onClick={handlePrevWeek}
                  className="px-3 py-1.5 rounded border text-sm text-black hover:bg-gray-100"
                >
                  이전 주
                </button>
                <button
                  onClick={() => setOpenMonth(true)}
                  className="px-3 py-1.5 rounded bg-black text-white text-sm hover:bg-black/90"
                >
                  월간 보기
                </button>
                <button
                  onClick={handleNextWeek}
                  className="px-3 py-1.5 rounded border text-sm text-black hover:bg-gray-100"
                >
                  다음 주
                </button>
              </div>
            </div>
          }
        >
          {loading ? (
            <div className="text-sm text-gray-700">로딩 중…</div>
          ) : weekEvents.length === 0 ? (
            <div className="text-sm text-gray-700">
              표시할 수업이 없습니다.
            </div>
          ) : (
            <div className="px-4 sm:px-6 w-full">
              <WeekCalendar
                startHour={8}
                endHour={22}
                events={weekEvents}
                lineColor="rgba(0,0,0,0.18)"
                textColor="#111111"
                showNowLine
                // 학생: 클릭해도 아무 동작 X (읽기 전용)
                onEventClick={undefined}
              />
            </div>
          )}
        </Panel>
      </PanelGrid>

      {/* 월간 보기 모달 (읽기 전용, 슬롯 기반) */}
      <StudentMonthModal
        open={openMonth}
        onClose={() => setOpenMonth(false)}
        studentId={login.username}
        token={login.token}
      />
    </div>
  );
}
