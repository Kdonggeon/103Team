// src/components/manage/DirectorPeoplePanel.tsx
"use client";

import React, { useEffect, useState } from "react";
import { getSession } from "@/app/lib/session";

/* -------------------- API 래퍼 -------------------- */
async function apiGet<T>(path: string): Promise<T> {
  const token = getSession()?.token ?? null;
  const url = path.startsWith("/backend") ? path : `/backend${path}`;
  const res = await fetch(url, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  });
  const txt = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}${txt ? " | " + txt : ""}`);
  return txt ? (JSON.parse(txt) as T) : ({} as T);
}

async function apiDelete(path: string): Promise<void> {
  const token = getSession()?.token ?? null;
  const url = path.startsWith("/backend") ? path : `/backend${path}`;
  const res = await fetch(url, {
    method: "DELETE",
    credentials: "include",
    cache: "no-store",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`${res.status} ${res.statusText}${txt ? " | " + txt : ""}`);
  }
}

/* -------------------- 타입 -------------------- */
type StudentLite = { studentId: string; name?: string; school?: string; grade?: number | null };
type TeacherLite = { teacherId: string; name?: string; phone?: string; academyNumber?: number | null };
type ClassLite = {
  classId: string;
  className: string;
  dayOfWeek?: string[];
  startTime?: string;
  endTime?: string;
  roomNumber?: number | string | null;
};
type AttendanceRow = { date: string; classId: string; className: string; status: string };

/* -------------------- 유틸 -------------------- */
const dayMap: Record<string, string> = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };
const fmtSchedule = (c: Partial<ClassLite>) => {
  const days = (c.dayOfWeek ?? []).map((d) => dayMap[String(d).toUpperCase()] || d).join("·");
  const time = c.startTime && c.endTime ? `${c.startTime}-${c.endTime}` : c.startTime ? `${c.startTime}~` : "";
  const room = c.roomNumber != null ? ` · #${c.roomNumber}` : "";
  return [days, time].filter(Boolean).join(" ") + room;
};
const parseYmd = (s: string) => new Date(s.replace(/-/g, "/")).getTime() || 0;

/* -------------------- 컴포넌트 -------------------- */
export default function DirectorPeoplePanel() {
  const [tab, setTab] = useState<"students" | "teachers">("students");

  // 목록
  const [students, setStudents] = useState<StudentLite[]>([]);
  const [teachers, setTeachers] = useState<TeacherLite[]>([]);
  const [loadingList, setLoadingList] = useState(false);
  const [errList, setErrList] = useState<string | null>(null);

  // 선택
  const [selectedStudent, setSelectedStudent] = useState<StudentLite | null>(null);
  const [selectedTeacher, setSelectedTeacher] = useState<TeacherLite | null>(null);

  // 상세
  const [classes, setClasses] = useState<ClassLite[]>([]);
  const [attendance, setAttendance] = useState<AttendanceRow[]>([]);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [errDetail, setErrDetail] = useState<string | null>(null);

  const resetDetail = () => {
    setClasses([]);
    setAttendance([]);
    setErrDetail(null);
  };

  /* ---- 목록 로드 ---- */
  useEffect(() => {
    setLoadingList(true);
    setErrList(null);
    resetDetail();
    setSelectedStudent(null);
    setSelectedTeacher(null);

    (async () => {
      try {
        if (tab === "students") {
          const list = await apiGet<StudentLite[]>("/api/students");
          setStudents(list || []);
        } else {
          const list = await apiGet<TeacherLite[]>("/api/teachers");
          setTeachers(list || []);
        }
      } catch (e: any) {
        setErrList(e?.message ?? "목록 로드 실패");
      } finally {
        setLoadingList(false);
      }
    })();
  }, [tab]);

  /* ---- 학생 선택 → 수업/출결 ---- */
  const loadStudentDetail = async (s: StudentLite) => {
    setSelectedStudent(s);
    setSelectedTeacher(null);
    resetDetail();
    setLoadingDetail(true);
    setErrDetail(null);
    try {
      let cls = await apiGet<ClassLite[]>(`/api/students/${encodeURIComponent(s.studentId)}/classes`);
      // 시작시간 → 수업명
      cls = (cls || []).sort((a, b) => {
        const t = (a.startTime || "").localeCompare(b.startTime || "");
        if (t !== 0) return t;
        return (a.className || "").localeCompare(b.className || "");
      });
      setClasses(cls);
      let att = await apiGet<AttendanceRow[]>(`/api/students/${encodeURIComponent(s.studentId)}/attendance`);
      // 날짜 최신 우선
      att = (att || []).sort((a, b) => parseYmd(b.date) - parseYmd(a.date));
      setAttendance(att);
    } catch (e: any) {
      setErrDetail(e?.message ?? "상세 로드 실패");
    } finally {
      setLoadingDetail(false);
    }
  };

  /* ---- 선생 선택 → 담당 반 + (옵션) 오늘 출결 합산 ---- */
  const loadTeacherDetail = async (t: TeacherLite) => {
    setSelectedTeacher(t);
    setSelectedStudent(null);
    resetDetail();
    setLoadingDetail(true);
    setErrDetail(null);
    try {
      let cls = await apiGet<ClassLite[]>(`/api/teachers/${encodeURIComponent(t.teacherId)}/classes`);
      cls = (cls || []).sort((a, b) => {
        const t = (a.startTime || "").localeCompare(b.startTime || "");
        if (t !== 0) return t;
        return (a.className || "").localeCompare(b.className || "");
      });
      setClasses(cls);

      const today = new Date();
      const ymd = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(
        today.getDate()
      ).padStart(2, "0")}`;

      let merged: AttendanceRow[] = [];
      for (const c of cls) {
        const rows = await apiGet<AttendanceRow[]>(
          `/api/teachers/classes/${encodeURIComponent(c.classId)}/attendance?date=${encodeURIComponent(ymd)}`
        );
        if (rows?.length) merged = merged.concat(rows.map((r) => ({ ...r, className: c.className })));
      }
      setAttendance(merged.sort((a, b) => parseYmd(b.date) - parseYmd(a.date)));
    } catch (e: any) {
      setErrDetail(e?.message ?? "상세 로드 실패");
    } finally {
      setLoadingDetail(false);
    }
  };

  /* ---- 삭제 ---- */
  const deleteStudent = async (studentId: string) => {
    if (!confirm(`학생(${studentId})을(를) 삭제할까요?`)) return;
    try {
      await apiDelete(`/api/students/${encodeURIComponent(studentId)}`);
      setStudents((prev) => prev.filter((s) => s.studentId !== studentId));
      if (selectedStudent?.studentId === studentId) {
        setSelectedStudent(null);
        resetDetail();
      }
    } catch (e: any) {
      alert(e?.message ?? "삭제 실패");
    }
  };

  const deleteTeacher = async (teacherId: string) => {
    if (!confirm(`선생(${teacherId})을(를) 삭제할까요?`)) return;
    try {
      await apiDelete(`/api/teachers/${encodeURIComponent(teacherId)}`);
      setTeachers((prev) => prev.filter((t) => t.teacherId !== teacherId));
      if (selectedTeacher?.teacherId === teacherId) {
        setSelectedTeacher(null);
        resetDetail();
      }
    } catch (e: any) {
      alert(e?.message ?? "삭제 실패");
    }
  };

  return (
    <div className="space-y-6">
      {/* 상단 탭 */}
      <div className="flex items-center gap-2">
        {(["students", "teachers"] as const).map((k) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={`px-4 py-2 rounded-full font-medium ring-1 ring-black/5 transition
              ${tab === k ? "bg-[#8CF39B] text-black" : "bg-[#CFF9D6] text-black hover:bg-[#B7F2C0]"}`}
          >
            {k === "students" ? "학생 명단" : "선생 명단"}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[360px_1fr] gap-6">
        {/* 좌측 목록 */}
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="text-sm font-semibold text-black">{tab === "students" ? "학생 목록" : "선생 목록"}</div>
            {loadingList && <div className="text-xs text-black/70">불러오는 중…</div>}
          </div>
          {errList && <div className="text-sm text-red-600 mb-2">오류: {errList}</div>}

          <div className="rounded-xl overflow-hidden ring-1 ring-black/5">
            {(tab === "students" ? students : teachers).length === 0 && !loadingList ? (
              <div className="px-3 py-2 text-sm text-black/80">표시할 항목이 없습니다.</div>
            ) : (
              (tab === "students" ? students : teachers).map((p: any) => (
                <div
                  key={p.studentId ?? p.teacherId}
                  className="px-3 py-2 border-b last:border-none bg-white text-sm flex items-center justify-between gap-2"
                >
                  <div className="min-w-0">
                    <div className="font-medium text-black truncate">
                      {tab === "students" ? p.name || p.studentId : p.name || p.teacherId}
                    </div>
                    <div className="text-xs text-black/80 truncate">
                      {tab === "students"
                        ? `${p.studentId}${p.school ? ` · ${p.school}` : ""}${
                            p.grade != null ? ` · ${p.grade}학년` : ""
                          }`
                        : `${p.teacherId}${p.phone ? ` · ${p.phone}` : ""}${
                            p.academyNumber != null ? ` · #${p.academyNumber}` : ""
                          }`}
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => (tab === "students" ? loadStudentDetail(p) : loadTeacherDetail(p))}
                      className="rounded-lg px-2.5 py-1.5 text-xs ring-1 ring-gray-300 bg-gray-50 hover:bg-gray-100 text-black"
                    >
                      {tab === "students" ? "출결/일정" : "출결"}
                    </button>
                    <button
                      onClick={() => (tab === "students" ? deleteStudent(p.studentId) : deleteTeacher(p.teacherId))}
                      className="rounded-lg px-2.5 py-1.5 text-xs ring-1 ring-red-200 bg-red-50 hover:bg-red-100 text-red-700"
                    >
                      삭제
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* 우측 상세 */}
        <div className="space-y-6">
          {/* 학생: 수업 일정 */}
          {selectedStudent && (
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="text-sm font-semibold text-black">
                  {selectedStudent.name || selectedStudent.studentId} 님의 수업 일정
                </div>
                {loadingDetail && <div className="text-xs text-black/70">불러오는 중…</div>}
              </div>
              {errDetail && <div className="text-sm text-red-600 mb-2">오류: {errDetail}</div>}

              <div className="rounded-xl overflow-hidden ring-1 ring-black/5">
                {classes.length === 0 && !loadingDetail ? (
                  <div className="px-3 py-2 text-sm text-black/80">수업 일정이 없습니다.</div>
                ) : (
                  classes.map((c) => (
                    <div key={c.classId} className="px-3 py-2 border-b last:border-none bg-white">
                      <div className="font-medium text-black">{c.className}</div>
                      <div className="text-xs text-black/80">{fmtSchedule(c)}</div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* 출결 표 (공통) */}
          {(selectedStudent || selectedTeacher) && (
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="text-sm font-semibold text-black">
                  출결 내역 {selectedTeacher ? `(오늘 기준, 담당 반 합산)` : ""}
                </div>
                {loadingDetail && <div className="text-xs text-black/70">불러오는 중…</div>}
              </div>
              {errDetail && <div className="text-sm text-red-600 mb-2">오류: {errDetail}</div>}

              <div className="rounded-xl overflow-hidden ring-1 ring-black/5">
                {attendance.length === 0 && !loadingDetail ? (
                  <div className="px-3 py-2 text-sm text-black/80">출결 기록이 없습니다.</div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="min-w-full text-sm">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-3 py-2 text-left font-semibold text-black">날짜</th>
                          <th className="px-3 py-2 text-left font-semibold text-black">수업명</th>
                          <th className="px-3 py-2 text-left font-semibold text-black">상태</th>
                        </tr>
                      </thead>
                      <tbody>
                        {attendance.map((r, i) => (
                          <tr key={`${r.date}-${r.classId}-${i}`} className="border-t">
                            <td className="px-3 py-2 text-black">{r.date}</td>
                            <td className="px-3 py-2 text-black">{r.className}</td>
                            <td className="px-3 py-2">
                              <span
                                className={`px-2 py-0.5 rounded text-xs ring-1
                                  ${
                                    /ABS/i.test(r.status)
                                      ? "bg-red-50 text-red-700 ring-red-200"
                                      : /LATE/i.test(r.status)
                                      ? "bg-amber-50 text-amber-700 ring-amber-200"
                                      : "bg-emerald-50 text-emerald-700 ring-emerald-200"
                                  }`}
                              >
                                {(/ABS/i.test(r.status) && "결석") ||
                                  (/LATE/i.test(r.status) && "지각") ||
                                  "출석"}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}

          {!selectedStudent && !selectedTeacher && (
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-black">
              왼쪽 목록에서 학생 또는 선생을 선택하면 상세(수업/출결)를 보여줍니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
