"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { getSession } from "@/app/lib/session";
import { api, type CourseLite, type ScheduleItem } from "@/app/lib/api";
import Panel from "@/components/ui/Panel";
import MonthCalendar, { krFixedSolarHolidays } from "@/components/ui/calendar/month-calendar";
import DayTimeline, { type DayEvent } from "@/components/ui/calendar/day-timeline";
import { deriveDayFromClasses, deriveMonthCountFromClasses, toYmd, normalizeTime } from "@/app/lib/schedule-derive";

import { useRouter } from "next/navigation";
import { ApiError } from "@/app/lib/api"; // ← ApiError export 필요(앞서 안내한 api.ts 수정)


type CourseDetail = CourseLite & {
  startTime?: string | null;
  endTime?: string | null;
  daysOfWeek?: (number | string)[] | null;
  schedule?: string | null;
  extraDates?: string[];
  cancelledDates?: string[];
};

export default function TeacherSchedulePage() {
  const me = getSession();
  const teacherId = me?.username ?? "";

  // 월
  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);

  // ✅ 다중 선택 날짜
  const [selectedYmds, setSelectedYmds] = useState<string[]>([toYmd(today)]);

  // 내 반 / 월·일 스케줄
  const [courses, setCourses] = useState<CourseLite[]>([]);
  const [monthly, setMonthly] = useState<ScheduleItem[]>([]);
  const [daily, setDaily] = useState<ScheduleItem[]>([]);
  const [countByDate, setCountByDate] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 입력 폼
  const [classId, setClassId] = useState("");
  const [title, setTitle] = useState("");
  const [startTime, setStartTime] = useState("10:00");
  const [endTime, setEndTime] = useState("11:00");
  const [roomNumber, setRoomNumber] = useState<string>("");

  const [saveEnabled, setSaveEnabled] = useState(true);
  const [saving, setSaving] = useState(false);
  const formRef = useRef<HTMLDivElement | null>(null);

  // 내 반 로드
  useEffect(() => {
    (async () => {
      if (!teacherId) return;
      try {
        const list = await api.listMyClasses(teacherId);
        setCourses(list || []);
      } catch (e: any) {
        setErr(e.message ?? "내 반을 불러오지 못했습니다.");
      }
    })();
  }, [teacherId]);

  // 월 스케줄 로드
    useEffect(() => {
    (async () => {
      if (!teacherId) return;
      setLoading(true);
      setErr(null);

      const from = `${year}-${String(month).padStart(2, "0")}-01`;
      const to = toYmd(new Date(year, month, 1)); // 다음달 1일 (exclusive)

      try {
        let items: ScheduleItem[] | null = null;
        let apiOk = false;

        // ✅ API 호출이 "성공했는지"만 본다 (빈 배열이어도 성공이면 활성화)
        try {
          items = await api.listSchedules(teacherId, from, to); // ← 오타 수정!
          apiOk = true;
        } catch (e) {
          apiOk = false;
          // console.warn("listSchedules failed:", e);
        }

        if (apiOk) {
          setSaveEnabled(true);              // ← 성공이면 활성화 (데이터 0건이어도)
          setMonthly(items ?? []);
          const map: Record<string, number> = {};
          for (const s of (items ?? [])) map[s.date] = (map[s.date] ?? 0) + 1;
          setCountByDate(map);
        } else {
          // ⬇ 폴백 파생 모드 (API 실패시에만)
          const lite = await api.listMyClasses(teacherId);
          const details: CourseDetail[] = await Promise.all(
            (lite || []).map(async (c) => {
              const d = await api.getClassDetail(c.classId);
              return { ...c, ...d };
            })
          );
          const map = deriveMonthCountFromClasses(year, month, details as any);
          setMonthly([]);
          setCountByDate(map);
          setSaveEnabled(false);            // ← 실패일 때만 비활성화
        }
      } catch (e: any) {
        setErr(e.message ?? "월 스케줄을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [teacherId, year, month]);


  // 선택된 마지막 날짜의 일 타임라인만 보여주기
  const focusYmd = selectedYmds[selectedYmds.length - 1];
  useEffect(() => {
    (async () => {
      if (!teacherId || !focusYmd) return;
      try {
        let items: ScheduleItem[] = [];
        try {
          items = await api.getDaySchedules(teacherId, focusYmd);
        } catch {}
        if (items && items.length) {
          setDaily(items);
        } else {
          const lite = await api.listMyClasses(teacherId);
          const details: CourseDetail[] = await Promise.all(
            (lite || []).map(async (c) => {
              const d = await api.getClassDetail(c.classId);
              return { ...c, ...d };
            })
          );
          const derived = deriveDayFromClasses(focusYmd, details as any);
          setDaily(
            derived.map((v) => ({
              scheduleId: v.id,
              teacherId,
              date: v.date,
              classId: v.classId,
              title: undefined,
              startTime: v.startTime,
              endTime: v.endTime,
              roomNumber: v.roomNumber,
            }))
          );
        }
      } catch (e: any) {
        setErr(e.message ?? "일 스케줄을 불러오지 못했습니다.");
      }
    })();
  }, [teacherId, focusYmd]);

  // 타임라인 이벤트
  const dayEvents: DayEvent[] = useMemo(
    () =>
      daily.map((d) => ({
        id: d.scheduleId,
        title: courses.find((c) => c.classId === d.classId)?.className ?? d.title ?? "수업",
        startTime: normalizeTime(d.startTime),
        endTime: normalizeTime(d.endTime),
        room: d.roomNumber != null ? `Room ${d.roomNumber}` : undefined,
        href: `/teacher/classes/${encodeURIComponent(d.classId)}`,
      })),
    [daily, courses]
  );

  // 날짜 토글
  const toggleDate = (ymd: string) => {
    setSelectedYmds((prev) => {
      const has = prev.includes(ymd);
      const next = has ? prev.filter((d) => d !== ymd) : [...prev, ymd];
      // 폼 스크롤
      formRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      // 반이 하나뿐이면 자동 선택
      if (!classId && courses.length === 1) setClassId(courses[0].classId);
      return next.sort(); // 보기 좋게 정렬
    });
  };
  const router = useRouter();

  // 저장 (선택된 모든 날짜에 대해 생성)
  // 저장 (선택된 모든 날짜에 대해 생성)
const save = async () => {
  if (!saveEnabled) {
    alert("현재는 ‘반 시간표(요일/시간/추가/휴강)’ 파생 모드라 저장 API가 비활성화되어 있을 수 있습니다.");
    return;
  }
  if (!teacherId) return;
  if (!classId) return alert("반을 선택하세요.");
  if (selectedYmds.length === 0) return alert("날짜를 선택하세요.");

  try {
    setSaving(true);
    setErr(null);

    // 요청 묶음
    const jobs = selectedYmds.map((date) =>
      api.createSchedule(teacherId, {
        date,
        classId,
        title: title || undefined,
        startTime,
        endTime,
        roomNumber: roomNumber ? Number(roomNumber) : undefined,
      })
    );

    const results = await Promise.allSettled(jobs);

    // ✅ 개별 실패 사유 수집(상태코드/메시지)
    const fails: Array<{ i: number; reason: string }> = [];
    results.forEach((r, i) => {
      if (r.status === "rejected") {
        const e = r.reason;
        if (e instanceof ApiError) {
          if (e.status === 401) {
            // 토큰 만료/미로그인
            alert("로그인이 만료되었습니다. 다시 로그인해주세요.");
            router.replace("/login");
            return;
          }
          if (e.status === 403) {
            fails.push({ i, reason: "403 권한/토큰 문제" });
            return;
          }
          fails.push({ i, reason: `${e.status} ${e.message}` });
        } else {
          fails.push({ i, reason: e?.message ?? "알 수 없는 오류" });
        }
      }
    });
    if (fails.length && fails.length === results.length) {
      // 전부 실패한 경우 즉시 안내
      alert(
        `저장 실패: ${fails.length}건\n` +
        fails.slice(0, 3).map(f => `- ${selectedYmds[f.i]}: ${f.reason}`).join("\n") +
        (fails.length > 3 ? `\n...외 ${fails.length - 3}건` : "")
      );
    } else {
      const ok = results.length - fails.length;
      alert(
        `저장 완료: ${ok}건 성공` +
        (fails.length
          ? `, ${fails.length}건 실패\n` +
            fails.slice(0, 3).map(f => `- ${selectedYmds[f.i]}: ${f.reason}`).join("\n") +
            (fails.length > 3 ? `\n...외 ${fails.length - 3}건` : "")
          : "")
      );
    }

    // ✅ 월/일 재조회 (listSchedules는 [from, to)라서 to=다음달 1일 OK)
    const from = `${year}-${String(month).padStart(2, "0")}-01`;
    const to = toYmd(new Date(year, month, 1)); // 다음달 1일
    const monthItems = await api.listSchedules(teacherId, from, to);
    setMonthly(monthItems);
    const map: Record<string, number> = {};
    for (const s of monthItems) map[s.date] = (map[s.date] ?? 0) + 1;
    setCountByDate(map);

    if (focusYmd) {
      const dayItems = await api.getDaySchedules(teacherId, focusYmd);
      setDaily(dayItems);
    }

    // 연속 등록 편의
    setTitle("");
  } catch (e: any) {
    // ✅ 추가 방어: 단건 try/catch로 떨어진 오류
    if (e instanceof ApiError) {
      if (e.status === 401) {
        alert("로그인이 만료되었습니다. 다시 로그인해주세요.");
        router.replace("/login");
        return;
      }
      if (e.status === 403) {
        alert("저장 실패(403): 권한/토큰 문제가 있어요. 로그인 토큰이 만료됐거나 권한이 부족할 수 있습니다.");
        setErr(e.message);
        return;
      }
      setErr(e.message || "저장 실패");
      alert(`저장 실패: ${e.message}`);
      return;
    }
    setErr(e?.message ?? "저장 실패");
    alert("알 수 없는 오류가 발생했습니다.");
  } finally {
    setSaving(false);
  }
};

  // 월 이동
  const decMonth = () => {
    const d = new Date(year, month - 2, 1);
    setYear(d.getFullYear());
    setMonth(d.getMonth() + 1);
  };
  const incMonth = () => {
    const d = new Date(year, month, 1);
    setYear(d.getFullYear());
    setMonth(d.getMonth() + 1);
  };

  return (
    <div className="max-w-7xl mx-auto p-6 space-y-4">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-xl font-bold">스케줄 관리</h1>
          <p className="text-sm text-slate-600">달력 셀을 클릭하면 선택/해제 됩니다. 좌측에서 시간/반 설정 후 저장하세요.</p>
        </div>
        <Link href="/" className="text-sm text-emerald-700 hover:underline">← 대시보드</Link>
      </div>

      {err && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 text-rose-700 px-4 py-2 text-sm">{err}</div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[360px_1fr] gap-6">
        {/* 좌측: 입력/타임라인 */}
        <div className="space-y-4">
          <Panel title="스케줄 입력">
            <div id="schedule-form" ref={formRef} className="space-y-3">
              <div className="text-sm text-slate-700">
                선택한 날짜:{" "}
                <b>{selectedYmds.length ? selectedYmds.join(", ") : "없음"}</b>
                {selectedYmds.length > 0 && (
                  <button
                    className="ml-2 text-xs px-2 py-0.5 rounded bg-slate-100 hover:bg-slate-200"
                    onClick={() => setSelectedYmds([])}
                    type="button"
                  >
                    모두 해제
                  </button>
                )}
              </div>

              {!saveEnabled && (
                <div className="text-xs text-amber-700 bg-amber-50 ring-1 ring-amber-200 rounded px-3 py-2">
                  현재는 <b>반의 요일/시간/추가/휴강</b>을 기반으로 달력에 표시하는 모드입니다.
                  별도 <b>스케줄 저장 API</b>가 비활성화되어 저장 버튼이 동작하지 않을 수 있습니다.
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <label className="text-sm">
                  <div className="text-slate-600 mb-1">반 선택</div>
                  <select
                    value={classId}
                    onChange={(e) => setClassId(e.target.value)}
                    className="w-full border rounded px-2 py-1"
                    disabled={!saveEnabled}
                  >
                    <option value="">선택하세요</option>
                    {courses.map((c) => (
                      <option key={c.classId} value={c.classId}>
                        {c.className}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="text-sm">
                  <div className="text-slate-600 mb-1">수업 제목(선택)</div>
                  <input
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="w-full border rounded px-2 py-1"
                    disabled={!saveEnabled}
                  />
                </label>

                <label className="text-sm">
                  <div className="text-slate-600 mb-1">시작</div>
                  <input
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className="w-full border rounded px-2 py-1"
                    disabled={!saveEnabled}
                  />
                </label>

                <label className="text-sm">
                  <div className="text-slate-600 mb-1">종료</div>
                  <input
                    type="time"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className="w-full border rounded px-2 py-1"
                    disabled={!saveEnabled}
                  />
                </label>

                <label className="text-sm">
                  <div className="text-slate-600 mb-1">방 번호(선택)</div>
                  <input
                    value={roomNumber}
                    onChange={(e) => setRoomNumber(e.target.value)}
                    className="w-full border rounded px-2 py-1"
                    disabled={!saveEnabled}
                  />
                </label>
              </div>

              <button
                onClick={save}
                disabled={!saveEnabled || loading || saving}
                className="px-4 py-2 rounded bg-emerald-600 text-white font-medium disabled:opacity-60"
              >
                {saving ? "저장 중…" : "선택된 날짜 모두 저장"}
              </button>
            </div>
          </Panel>

          <Panel title={`일정(${focusYmd || "-"})`}>
            <DayTimeline startHour={8} endHour={22} events={dayEvents} />
          </Panel>
        </div>

        {/* 우측: 월간 캘린더 */}
        <Panel
          title={
            <div className="flex items-center gap-3">
              <button onClick={decMonth} className="px-2 py-1 rounded bg-slate-100 hover:bg-slate-200">◀</button>
              <span className="text-lg font-semibold">{year}년 {month}월</span>
              <button onClick={incMonth} className="px-2 py-1 rounded bg-slate-100 hover:bg-slate-200">▶</button>
            </div>
          }
          right={loading ? <span className="text-xs text-slate-500">불러오는 중…</span> : null}
        >
          <MonthCalendar
            year={year}
            month={month}
            holidays={krFixedSolarHolidays(year)}
            eventCountByDate={countByDate}
            selectedYmds={selectedYmds}     // ✅ 다중선택 표시
            onToggle={toggleDate}           // ✅ 클릭 시 토글
            showWeekendColors
          />
          console.log("[save schedule] user", s?.role, s?.username, "token?", !!s?.token);
        </Panel>
      </div>
    </div>
    
  );
}
