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

type StudentClassPattern = {
  classId: string;
  className: string;
  roomNumber?: number | string;
  // 1~7 (1=월 ... 7=일)
  daysOfWeek: (1 | 2 | 3 | 4 | 5 | 6 | 7)[];
  startTime: string; // "HH:mm"
  endTime: string; // "HH:mm"
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

/* ============ 학생 반 JSON → 패턴(StudentClassPattern) 변환 ============ */

/**
 * /api/students/{id}/classes 응답 1개(raw)를
 * "요일/시간 패턴" 여러 개로 풀어냄.
 *
 * 지원하는 필드(둘 중 하나 있으면 처리):
 *  - schedule: [{ dow, startTime, endTime, roomNumber? }, ...]
 *  - Days_Of_Week / daysOfWeek + Start_Time / End_Time
 */
function extractPatternsFromClass(raw: any): StudentClassPattern[] {
  if (!raw) return [];

  const classId =
    raw.Class_ID ?? raw.classId ?? raw.id ?? raw._id ?? undefined;
  const className =
    raw.Class_Name ??
    raw.className ??
    raw.Title ??
    raw.name ??
    raw.title ??
    undefined;

  if (!classId || !className) return [];

  const baseRoom =
    raw.roomNumber ??
    raw.Room_Number ??
    raw.Room ??
    (Array.isArray(raw.roomNumbers) && raw.roomNumbers.length
      ? raw.roomNumbers[0]
      : undefined);

  const patterns: StudentClassPattern[] = [];

  // 1) schedule 배열이 있는 경우 (선생 CourseLite.schedule과 유사)
  if (Array.isArray(raw.schedule) && raw.schedule.length > 0) {
    for (const s of raw.schedule) {
      const dow = s?.dow ?? s?.dayOfWeek;
      if (!dow) continue;
      const iso =
        dow === 0
          ? 7
          : (Number(dow) as 1 | 2 | 3 | 4 | 5 | 6 | 7);

      const start =
        s.startTime ??
        raw.Start_Time ??
        raw.startTime ??
        "00:00";
      const end =
        s.endTime ??
        raw.End_Time ??
        raw.endTime ??
        "23:59";

      const room =
        s.roomNumber ??
        baseRoom;

      patterns.push({
        classId: String(classId),
        className: String(className),
        roomNumber: room,
        daysOfWeek: [iso],
        startTime: String(start),
        endTime: String(end),
      });
    }
    return patterns;
  }

  // 2) Days_Of_Week 기반 패턴
  let dows: number[] = [];
  if (Array.isArray(raw.Days_Of_Week)) {
    dows = raw.Days_Of_Week.map((n: any) => Number(n));
  } else if (Array.isArray(raw.daysOfWeek)) {
    dows = raw.daysOfWeek.map((n: any) => Number(n));
  } else if (typeof raw.Days_Of_Week === "string") {
    dows = raw.Days_Of_Week.split(",")
 .map((s: string) => Number(s.trim()))
    .filter((n: number) => n >= 1 && n <= 7);
  } else if (raw.dow != null) {
    dows = [Number(raw.dow)];
  }

  // 3) date만 있는 경우 → 그 날짜의 요일로 1개 패턴
  if (!dows.length && raw.date) {
    const d = new Date(String(raw.date).slice(0, 10) + "T00:00:00");
    dows = [jsToIsoDow(d.getDay())];
  }

  if (!dows.length) return [];

  const start = raw.Start_Time ?? raw.startTime ?? "00:00";
  const end = raw.End_Time ?? raw.endTime ?? "23:59";

  const uniqDows = Array.from(
    new Set(
      dows
        .map((n) => Number(n))
        .filter((n) => n >= 1 && n <= 7)
    )
  ) as (1 | 2 | 3 | 4 | 5 | 6 | 7)[];

  if (!uniqDows.length) return [];

  patterns.push({
    classId: String(classId),
    className: String(className),
    roomNumber: baseRoom,
    daysOfWeek: uniqDows,
    startTime: String(start),
    endTime: String(end),
  });

  return patterns;
}

/* ================= 월간 모달 (학생용: 읽기 전용) ================= */

function StudentMonthModal({
  open,
  onClose,
  patterns,
}: {
  open: boolean;
  onClose: () => void;
  patterns: StudentClassPattern[];
}) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedDate, setSelectedDate] = useState<string>(ymd(now));

  // 패턴 + year/month → MonthEvent[]
  const events = useMemo<MonthEvent[]>(() => {
    const first = new Date(year, month - 1, 1);
    const last = new Date(year, month, 0);
    const out: MonthEvent[] = [];

    for (let day = 1; day <= last.getDate(); day++) {
      const d = new Date(year, month - 1, day);
      const isoDow = jsToIsoDow(d.getDay());
      const dateStr = ymd(d);

      for (const p of patterns) {
        if (!p.daysOfWeek.includes(isoDow)) continue;
        const key = `${p.classId}-${isoDow}`;
        out.push({
          id: `${key}-${dateStr}`,
          date: dateStr,
          title: p.className,
          classId: p.classId,
          startTime: p.startTime,
          endTime: p.endTime,
          roomNumber:
            p.roomNumber != null && !Number.isNaN(Number(p.roomNumber))
              ? Number(p.roomNumber)
              : undefined,
          color: colorByKey(key),
        });
      }
    }
    return out;
  }, [patterns, year, month]);

  const dayEvents = useMemo(
    () => events.filter((e) => e.date === selectedDate),
    [events, selectedDate]
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
            {dayEvents.length === 0 ? (
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

  const [patterns, setPatterns] = useState<StudentClassPattern[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [roomFilter, setRoomFilter] = useState<string>("ALL");
  const [openMonth, setOpenMonth] = useState(false);

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

  // 3) 학생 클래스 목록 → 패턴으로 변환
  useEffect(() => {
    if (!login) {
      setPatterns([]);
      return;
    }
    const studentId = login.username;
    const token = login.token ?? "";

    let aborted = false;

    (async () => {
      setLoading(true);
      setErr(null);
      try {
        const raw = await apiGet<any[]>(
          `/api/students/${encodeURIComponent(studentId)}/classes`,
          token
        );
        if (aborted) return;
        const list = Array.isArray(raw) ? raw : [];
        const pats = list.flatMap(extractPatternsFromClass);
        setPatterns(pats);
      } catch (e: any) {
        if (aborted) return;
        const msg = String(e?.message ?? "");
        setErr(msg || "시간표를 불러오지 못했습니다.");
        setPatterns([]);
      } finally {
        if (!aborted) setLoading(false);
      }
    })();

    return () => {
      aborted = true;
    };
  }, [login]);

  // 4) 방 필터 옵션
  const roomOptions = useMemo(() => {
    const set = new Set<string>();
    for (const p of patterns) {
      if (p.roomNumber != null) {
        set.add(String(p.roomNumber));
      }
    }
    return Array.from(set).sort((a, b) => Number(a) - Number(b));
  }, [patterns]);

  // 5) 주간 캘린더 이벤트 (요일 기반, 날짜 상관 없음)
  const weekEvents: CalendarEvent[] = useMemo(() => {
    const out: CalendarEvent[] = [];
    patterns.forEach((p, idx) => {
      p.daysOfWeek.forEach((dow) => {
        if (roomFilter !== "ALL") {
          if (
            p.roomNumber == null ||
            String(p.roomNumber) !== roomFilter
          ) {
            return;
          }
        }
        const key = `${p.classId}-${dow}`;
        out.push({
          id: `${p.classId}-${dow}-${idx}`,
          title: p.className,
          room:
            p.roomNumber != null
              ? `Room ${p.roomNumber}`
              : undefined,
          dayOfWeek: dow,
          startTime: p.startTime,
          endTime: p.endTime,
          color: colorByKey(key),
        });
      });
    });
    return out;
  }, [patterns, roomFilter]);

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
                  onChange={(e) =>
                    setRoomFilter(e.target.value)
                  }
                  className="border rounded px-2 py-1 text-sm text-black"
                >
                  <option
                    value="ALL"
                    className="text-black"
                  >
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

      {/* 월간 보기 모달 (읽기 전용) */}
      <StudentMonthModal
        open={openMonth}
        onClose={() => setOpenMonth(false)}
        patterns={patterns}
      />
    </div>
  );
}
