"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import dayjs from "dayjs";
import MonthCalendar from "@/components/ui/calendar/month-calendar";
import { fetchMonth, toggleDate } from "@/app/lib/calendar";
import api, { type CourseLite, type CreateClassReq } from "@/app/lib/api";

const DAYS = [
  { n: 1, label: "월" }, { n: 2, label: "화" }, { n: 3, label: "수" },
  { n: 4, label: "목" }, { n: 5, label: "금" }, { n: 6, label: "토" }, { n: 7, label: "일" },
];



export default function TeacherManagePanel({
  teacherId,
  defaultAcademy,
}: {
  teacherId: string;
  defaultAcademy: number | null;
}) {
  // -------- 내 반 목록 --------
  const [classes, setClasses] = useState<CourseLite[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  

  const loadClasses = async () => {
    if (!teacherId) return;
    setErr(null);
    try {
      const list = await api.listMyClasses(teacherId);
      setClasses(list || []);
    } catch (e: any) {
      setErr(e.message ?? "반 목록을 불러오지 못했습니다.");
    }
  };
  useEffect(() => { loadClasses(); /* eslint-disable-next-line */ }, [teacherId]);

  // -------- 반 생성 폼 --------
  const [name, setName] = useState("");
  const [room, setRoom] = useState<string>("");
  const [startTime, setStart] = useState("10:00");
  const [endTime, setEnd] = useState("12:00");
  const [days, setDays] = useState<number[]>([1, 3, 5]);
  const [schedule, setSchedule] = useState("월수금 10:00~12:00");

  const toggleDay = (n: number) =>
    setDays((prev) => (prev.includes(n) ? prev.filter((x) => x !== n) : [...prev, n].sort((a, b) => a - b)));

  const canCreate = name.trim().length > 0 && defaultAcademy != null && /^\d{2}:\d{2}$/.test(startTime) && /^\d{2}:\d{2}$/.test(endTime) && days.length > 0;

  const createClass = async () => {
    if (!canCreate) return;
    setLoading(true); setErr(null);
    try {
      const body: CreateClassReq = {
        className: name.trim(),
        teacherId,
        academyNumber: defaultAcademy as number,
        roomNumber: room ? Number(room) : undefined,
        startTime, endTime, daysOfWeek: days, schedule,
      } as any;
      await api.createClass(body);
      setName(""); setRoom(""); setSchedule("월수금 10:00~12:00"); setDays([1,3,5]); setStart("10:00"); setEnd("12:00");
      await loadClasses();
    } catch (e: any) {
      setErr(e.message ?? "반 생성 실패");
    } finally { setLoading(false); }
  };

  // -------- 월간 미리보기 --------
  const [yyyymm, setYYYMM] = useState(dayjs().format("YYYYMM"));
  const [monthRows, setMonthRows] = useState<{ date: string; items: { classId: string; className: string }[] }[]>([]);
  const [pickedDates, setPickedDates] = useState<string[]>([]);
  useEffect(() => { fetchMonth(yyyymm).then(setMonthRows).catch((e)=>setErr(String(e))); }, [yyyymm]);

  const countMap = useMemo(() => {
    const m: Record<string, number> = {};
    monthRows.forEach((r) => (m[r.date] = r.items.length));
    return m;
  }, [monthRows]);

  const year = Number(yyyymm.slice(0, 4));
  const month = Number(yyyymm.slice(4));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">관리 패널</h1>
        <div className="text-sm text-gray-500">교사: <b>{teacherId}</b> · 학원번호: <b>{defaultAcademy ?? "-"}</b></div>
      </div>

      {err && <div className="border border-red-200 bg-red-50 text-red-700 px-4 py-2 rounded">{err}</div>}

      {/* ====== 반 생성 ====== */}
      <section className="rounded-xl border bg-white p-4 space-y-3">
        <div className="font-semibold">반 생성</div>
        <div className="grid sm:grid-cols-2 gap-3">
          <label className="text-sm">
            <div className="text-gray-600 mb-1">반 이름</div>
            <input value={name} onChange={(e)=>setName(e.target.value)} className="w-full border rounded px-2 py-1" />
          </label>
          <label className="text-sm">
            <div className="text-gray-600 mb-1">강의실(선택)</div>
            <input value={room} onChange={(e)=>setRoom(e.target.value)} className="w-full border rounded px-2 py-1" />
          </label>

          <label className="text-sm">
            <div className="text-gray-600 mb-1">시작</div>
            <input type="time" value={startTime} onChange={(e)=>setStart(e.target.value)} className="w-full border rounded px-2 py-1" />
          </label>
          <label className="text-sm">
            <div className="text-gray-600 mb-1">종료</div>
            <input type="time" value={endTime} onChange={(e)=>setEnd(e.target.value)} className="w-full border rounded px-2 py-1" />
          </label>

          <div className="sm:col-span-2">
            <div className="text-sm text-gray-600 mb-1">요일</div>
            <div className="flex flex-wrap gap-2">
              {DAYS.map((d) => (
                <button
                  key={d.n}
                  type="button"
                  onClick={()=>toggleDay(d.n)}
                  className={`px-3 py-1 rounded-full border ${days.includes(d.n) ? "bg-emerald-100 border-emerald-300" : "bg-white"}`}
                >
                  {d.label}
                </button>
              ))}
            </div>
          </div>

          <label className="text-sm sm:col-span-2">
            <div className="text-gray-600 mb-1">표시 메모(선택)</div>
            <input value={schedule} onChange={(e)=>setSchedule(e.target.value)} className="w-full border rounded px-2 py-1" placeholder="월수금 10:00~12:00" />
          </label>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={createClass}
            disabled={!canCreate || loading}
            className="px-4 py-2 rounded bg-emerald-600 text-white disabled:opacity-50"
          >
            {loading ? "생성 중…" : "반 생성"}
          </button>
          <span className="text-xs text-gray-500">학생 추가/수정은 생성 후 반 상세에서 진행합니다.</span>
        </div>
      </section>

      {/* ====== 내 반 목록 ====== */}
      <section className="rounded-xl border bg-white p-4 space-y-3">
        <div className="font-semibold">내 반 목록</div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {classes.map((c)=>(
            <div key={c.classId} className="border rounded-lg p-3">
              <div className="font-medium">{c.className}</div>
              <div className="text-xs text-gray-500 mt-1">ID: {c.classId}</div>
              {c.roomNumber != null && <div className="text-xs text-gray-600 mt-1">강의실: {c.roomNumber}</div>}
              <div className="flex gap-3 mt-2 text-sm">
                <Link className="text-blue-600 underline" href={`/teacher/classes/${c.classId}`}>상세/학생관리</Link>
                <Link className="text-blue-600 underline" href={`/seats/${c.classId}`}>좌석</Link>
              </div>
            </div>
          ))}
          {classes.length === 0 && <div className="text-gray-500">아직 생성된 반이 없습니다.</div>}
        </div>
      </section>

      {/* ====== 월간 미리보기 + 날짜 토글 ====== */}
      <section className="rounded-xl border bg-white p-4 space-y-3">
        <div className="flex items-center gap-2">
          <div className="font-semibold">월간 캘린더</div>
          <button onClick={()=>setYYYMM(dayjs(yyyymm+"01").subtract(1,"month").format("YYYYMM"))} className="px-2 py-1 rounded bg-gray-100">◀</button>
          <div className="text-sm">{dayjs(yyyymm+"01").format("YYYY.MM")}</div>
          <button onClick={()=>setYYYMM(dayjs(yyyymm+"01").add(1,"month").format("YYYYMM"))} className="px-2 py-1 rounded bg-gray-100">▶</button>
        </div>
        <MonthCalendar
          year={year}
          month={month}
          selectedYmds={pickedDates}
          eventCountByDate={countMap}
          onToggle={async (ymd) => {
            try {
              // 토글: 이미 있으면 제거, 없으면 추가
              setPickedDates(prev =>
                prev.includes(ymd) ? prev.filter(d => d !== ymd) : [...prev, ymd]
              );

              // 예시: 해당 날짜의 첫 반으로 토글 호출 (현재 로직 유지)
              const first = monthRows.find((r) => r.date === ymd)?.items?.[0];
              if (!first) return;

              await toggleDate(first.classId, ymd);
              const next = await fetchMonth(yyyymm);
              setMonthRows(next);
            } catch (e: any) {
              console.error(e);
              alert(e?.message ?? "날짜 토글 중 오류가 발생했습니다.");
            }
          }}
        />
      </section>
    </div>
  );
}
