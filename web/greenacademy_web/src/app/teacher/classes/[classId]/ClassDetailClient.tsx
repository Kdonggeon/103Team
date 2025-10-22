"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { api, type CourseLite, type StudentHit } from "@/app/lib/api";
import { getSession } from "@/app/lib/session";

const DAYS = [
  { n: 1, label: "월" }, { n: 2, label: "화" }, { n: 3, label: "수" },
  { n: 4, label: "목" }, { n: 5, label: "금" }, { n: 6, label: "토" }, { n: 7, label: "일" },
];

type CourseDetail = CourseLite & {
  startTime?: string;
  endTime?: string;
  daysOfWeek?: number[];
};

function normalizeDays(v: any): number[] {
  if (!v) return [];
  const fw: Record<string,string> = { "１":"1","２":"2","３":"3","４":"4","５":"5","６":"6","７":"7" };
  const ko: Record<string,number> = { "월":1,"화":2,"수":3,"목":4,"금":5,"토":6,"일":7 };
  return (Array.isArray(v) ? v : [v])
    .map(d=>{
      if (typeof d === "number") return d;
      if (typeof d === "string") {
        const s = fw[d] ?? d;
        if (ko[s] != null) return ko[s];
        const n = parseInt(s,10);
        if (n>=1 && n<=7) return n;
      }
      return null;
    })
    .filter(Boolean) as number[];
}

export default function ClassDetailClient({ classId }: { classId: string }) {
  const me = getSession();
  if (!me) {
    if (typeof window !== "undefined") location.href = "/login";
    return null;
  }
  const academyNumber = me.academyNumbers?.[0] ?? null;

  // === 반 기본 정보 ===
  const [data, setData] = useState<CourseDetail | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // === 반 정보 수정 필드 ===
  const [newName, setNewName] = useState("");
  const [newRoom, setNewRoom] = useState<string>("");

  // === 시간표(요일/시간) ===
  const [startTime, setStart] = useState("10:00");
  const [endTime, setEnd] = useState("11:00");
  const [days, setDays] = useState<number[]>([]);
  const toggleDay = (d: number) =>
    setDays(prev => prev.includes(d) ? prev.filter(x=>x!==d) : [...prev, d].sort((a,b)=>a-b));

  const canSaveSchedule = useMemo(
    () => days.length>0 && /^\d{2}:\d{2}$/.test(startTime) && /^\d{2}:\d{2}$/.test(endTime),
    [days, startTime, endTime]
  );

  // === 학생 검색/추가 ===
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState<string>("");
  const [hits, setHits] = useState<StudentHit[]>([]);
  const [searching, setSearching] = useState(false);

  const load = async () => {
    setErr(null); setMsg(null); setLoading(true);
    try {
      const c = await api.getClassDetail(classId);
      const d = c as CourseDetail;
      setData(d);

      setNewName(d.className || "");
      setNewRoom(d.roomNumber != null ? String(d.roomNumber) : "");

      const st = (d as any).startTime ?? (d as any).Start_Time;
      const et = (d as any).endTime   ?? (d as any).End_Time;
      const dw = (d as any).daysOfWeek ?? (d as any).Days_Of_Week;

      setStart(typeof st === "string" ? st : "10:00");
      setEnd(typeof et === "string" ? et : "11:00");
      setDays(normalizeDays(dw));
    } catch (e:any) { setErr(e.message); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [classId]);

  // 반 정보 저장
  const saveInfo = async () => {
    try {
      setErr(null); setMsg(null);
      await api.patchClass(classId, {
        className: newName || undefined,
        roomNumber: newRoom ? Number(newRoom) : undefined,
      });
      await load();
      setMsg("반 정보가 저장되었습니다.");
    } catch (e:any) { setErr(e.message); }
  };

  // 시간표 저장
  const [savingSchedule, setSavingSchedule] = useState(false);
  const saveSchedule = async () => {
    try {
      setSavingSchedule(true); setErr(null); setMsg(null);
      await api.patchClass(classId, { startTime, endTime, daysOfWeek: days } as any);
      await load();
      setMsg("시간표가 저장되었습니다.");
    } catch (e:any) { setErr(e.message ?? "시간표 저장 실패"); }
    finally { setSavingSchedule(false); }
  };

  // 학생 검색/추가/삭제
  const search = async () => {
    if (!academyNumber) { setErr("학원번호 없음"); return; }
    try {
      setSearching(true); setErr(null);
      const res = await api.searchStudents(academyNumber, q, grade ? Number(grade) : undefined);
      setHits(res);
    } catch (e:any) { setErr(e.message); }
    finally { setSearching(false); }
  };

  const addStudent = async (sid: string) => {
    try {
      setErr(null); setMsg(null);
      await api.addStudentToClass(classId, sid);
      await load();
      setMsg("학생이 추가되었습니다.");
    } catch (e:any) { setErr(e.message); }
  };

  const removeStudent = async (sid: string) => {
    try {
      setErr(null); setMsg(null);
      await api.removeStudentFromClass(classId, sid);
      await load();
      setMsg("학생이 제거되었습니다.");
    } catch (e:any) { setErr(e.message); }
  };

  if (loading) return <div className="p-6">불러오는 중…</div>;
  if (err) return <div className="p-6 text-red-600">{err}</div>;
  if (!data) return <div className="p-6">데이터가 없습니다.</div>;

  return (
    <div className="max-w-7xl mx-auto px-6 py-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-end gap-3">
          <h1 className="text-2xl font-bold">{data.className}</h1>
          <span className="text-gray-600">({data.classId})</span>
        </div>
        <Link href="/" className="text-sm text-emerald-700 hover:underline">← 대시보드로</Link>
      </div>

      {/* 시간표 편집 */}
      <div className="bg-white border rounded-2xl p-5 space-y-3">
        <div className="text-lg font-semibold">요일/시간 설정</div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex gap-2">
            {DAYS.map((d) => (
              <label
                key={d.n}
                className={`px-3 py-1 rounded-full border cursor-pointer ${
                  days.includes(d.n) ? "bg-emerald-100 border-emerald-300" : "bg-white"
                }`}
              >
                <input className="hidden" type="checkbox" checked={days.includes(d.n)} onChange={() => toggleDay(d.n)} />
                {d.label}
              </label>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <input type="time" value={startTime} onChange={(e) => setStart(e.target.value)} className="border rounded px-2 py-1" />
            <span className="text-gray-500">~</span>
            <input type="time" value={endTime} onChange={(e) => setEnd(e.target.value)} className="border rounded px-2 py-1" />
          </div>

          <button onClick={saveSchedule} disabled={!canSaveSchedule || savingSchedule}
                  className="px-4 py-2 rounded bg-emerald-600 text-white disabled:opacity-50">
            {savingSchedule ? "저장 중…" : "저장"}
          </button>
        </div>
        <p className="text-xs text-gray-500">
          {/* 저장 후 <b>교사 &gt; 시간표</b> 페이지의 주간 캘린더에 반 블록이 표시됩니다. */}
        </p>
      </div>

      {/* 반 기본 정보 수정 */}
      <div className="bg-white border rounded-2xl p-5 space-y-3">
        <div className="text-lg font-semibold">반 정보</div>
        <div className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm text-gray-600">반 이름</label>
            <input value={newName} onChange={e=>setNewName(e.target.value)} className="border rounded px-2 py-1 w-full" />
          </div>
          <div>
            <label className="block text-sm text-gray-600">방 번호</label>
            <input value={newRoom} onChange={e=>setNewRoom(e.target.value)} className="border rounded px-2 py-1 w-full" />
          </div>
        </div>
        <button onClick={saveInfo} className="bg-emerald-600 text-white px-3 py-2 rounded">반 정보 저장</button>
        {msg && <span className="text-emerald-600 ml-2">{msg}</span>}
      </div>

      {/* 현재 학생 목록 */}
      <div className="bg-white border rounded-2xl p-5">
        <div className="text-lg font-semibold mb-2">등록 학생 ({data.students?.length ?? 0})</div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
          {(data.students || []).map(sid => (
            <div key={sid} className="border rounded px-3 py-2 flex items-center justify-between">
              <span>{sid}</span>
              <button onClick={()=>removeStudent(sid)} className="text-red-600 text-sm hover:underline">삭제</button>
            </div>
          ))}
          {(data.students || []).length === 0 && <div className="text-gray-500">아직 학생이 없습니다.</div>}
        </div>
      </div>

      {/* 학생 검색/추가 */}
      <div className="bg-white border rounded-2xl p-5 space-y-3">
        <div className="text-lg font-semibold">학생 검색</div>
        <div className="flex flex-wrap gap-2">
          <input value={q} onChange={e=>setQ(e.target.value)} placeholder="이름 검색" className="border rounded px-2 py-1" />
          <input value={grade} onChange={e=>setGrade(e.target.value)} placeholder="학년(선택)" className="border rounded px-2 py-1 w-32" />
          <button onClick={search} className="bg-gray-800 text-white px-3 py-1 rounded">검색</button>
          {searching && <span className="text-gray-500 text-sm">검색중…</span>}
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
          {hits.map(h => (
            <div key={h.studentId} className="border rounded px-3 py-2 flex items-center justify-between">
              <div>
                <div className="font-medium">{h.studentName ?? h.studentId}</div>
                <div className="text-xs text-gray-600">ID: {h.studentId} · 학년: {h.grade ?? "-"}</div>
              </div>
              <button onClick={()=>addStudent(h.studentId)} className="text-emerald-600 text-sm hover:underline">추가</button>
            </div>
          ))}
          {hits.length === 0 && <div className="text-gray-500">검색 결과 없음</div>}
        </div>
      </div>
    </div>
  );
}
