// src/components/manage/DirectorPeoplePanel.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";
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
const PAGE_SIZE = 20;

const dayMap: Record<string, string> = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };
const fmtSchedule = (c: Partial<ClassLite>) => {
  const days = (c.dayOfWeek ?? []).map((d) => dayMap[String(d).toUpperCase()] || d).join("·");
  const time = c.startTime && c.endTime ? `${c.startTime}-${c.endTime}` : c.startTime ? `${c.startTime}~` : "";
  const room = c.roomNumber != null ? ` · #${c.roomNumber}` : "";
  return [days, time].filter(Boolean).join(" ") + room;
};
const parseYmd = (s: string) => new Date(s.replace(/-/g, "/")).getTime() || 0;

function toStr(v: unknown) {
  if (v === null || v === undefined) return "";
  return String(v);
}

/* -------------------- 내부 Pagination 컴포넌트 -------------------- */
function Pager({
  page,
  total,
  onChange,
}: {
  page: number; // 1-base
  total: number; // total pages
  onChange: (p: number) => void;
}) {
  if (total <= 1) return null;

  const pages: number[] = [];
  const push = (n: number) => (n >= 1 && n <= total ? pages.push(n) : null);

  push(1);
  if (page > 4) pages.push(-1);
  for (let p = page - 2; p <= page + 2; p++) push(p);
  if (page < total - 3) pages.push(-2);
  if (total > 1) push(total);

  return (
    <div className="flex items-center justify-between gap-2 px-3 py-2 bg-white">
      <div className="text-xs text-black">
        페이지 <span className="font-semibold text-black">{page}</span> / {total}
      </div>

      <div className="flex items-center gap-1">
        <button
          onClick={() => onChange(1)}
          disabled={page === 1}
          className="px-2 py-1 text-xs rounded ring-1 ring-black/20 disabled:opacity-40 text-black"
        >
          « 처음
        </button>
        <button
          onClick={() => onChange(Math.max(1, page - 1))}
          disabled={page === 1}
          className="px-2 py-1 text-xs rounded ring-1 ring-black/20 disabled:opacity-40 text-black"
        >
          ‹ 이전
        </button>

        <div className="flex items-center gap-1">
          {pages.map((p, i) =>
            p > 0 ? (
              <button
                key={`${p}-${i}`}
                onClick={() => onChange(p)}
                className={`px-2.5 py-1 text-xs rounded ring-1 transition text-black ${
                  p === page
                    ? "bg-[#8CF39B] ring-black/20"
                    : "bg-white ring-black/20 hover:bg-gray-50"
                }`}
              >
                {p}
              </button>
            ) : (
              <span key={`${p}-${i}`} className="px-1 text-xs text-black">
                …
              </span>
            )
          )}
        </div>

        <button
          onClick={() => onChange(Math.min(total, page + 1))}
          disabled={page === total}
          className="px-2 py-1 text-xs rounded ring-1 ring-black/20 disabled:opacity-40 text-black"
        >
          다음 ›
        </button>
        <button
          onClick={() => onChange(total)}
          disabled={page === total}
          className="px-2 py-1 text-xs rounded ring-1 ring-black/20 disabled:opacity-40 text-black"
        >
          끝 »
        </button>
      </div>
    </div>
  );
}

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

  // 검색 & 페이지
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1); // 1-base

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
    setQuery("");
    setPage(1);

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

  // 검색어 변경 시 페이지 1로
  useEffect(() => {
    setPage(1);
  }, [query, tab]);

  /* ---- 학생 선택 → 수업/출결 ---- */
  const loadStudentDetail = async (s: StudentLite) => {
    setSelectedStudent(s);
    setSelectedTeacher(null);
    resetDetail();
    setLoadingDetail(true);
    setErrDetail(null);
    try {
      let cls = await apiGet<ClassLite[]>(`/api/students/${encodeURIComponent(s.studentId)}/classes`);
      cls = (cls || []).sort((a, b) => {
        const t = (a.startTime || "").localeCompare(b.startTime || "");
        if (t !== 0) return t;
        return (a.className || "").localeCompare(b.className || "");
      });
      setClasses(cls);

      let att = await apiGet<AttendanceRow[]>(`/api/students/${encodeURIComponent(s.studentId)}/attendance`);
      att = (att || []).sort((a, b) => parseYmd(b.date) - parseYmd(a.date));
      setAttendance(att);
    } catch (e: any) {
      setErrDetail(e?.message ?? "상세 로드 실패");
    } finally {
      setLoadingDetail(false);
    }
  };

  /* ---- 선생 선택 → 담당 반 목록 (출결 X) ---- */
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
    } catch (e: any) {
      alert(e?.message ?? "삭제 실패");
    }
  };

  const deleteTeacher = async (teacherId: string) => {
    if (!confirm(`선생(${teacherId})을(를) 삭제할까요?`)) return;
    try {
      await apiDelete(`/api/teachers/${encodeURIComponent(teacherId)}`);
      setTeachers((prev) => prev.filter((t) => t.teacherId !== teacherId));
    } catch (e: any) {
      alert(e?.message ?? "삭제 실패");
    }
  };

  /* ---- 검색 필터링 & 페이징 계산 ---- */
  const { pageItems, totalPages, totalCount } = useMemo(() => {
    const q = query.trim().toLowerCase();
    const list = tab === "students" ? students : teachers;

    const filtered = !q
      ? list
      : list.filter((item: any) => {
          if (tab === "students") {
            const s = item as StudentLite;
            const hay =
              `${toStr(s.studentId)} ${toStr(s.name)} ${toStr(s.school)} ${toStr(s.grade)}`.toLowerCase();
            return hay.includes(q);
          } else {
            const t = item as TeacherLite;
            const hay =
              `${toStr(t.teacherId)} ${toStr(t.name)} ${toStr(t.phone)} ${toStr(t.academyNumber)}`.toLowerCase();
            return hay.includes(q);
          }
        });

    const totalCount = filtered.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE));
    const safePage = Math.min(Math.max(1, page), totalPages);
    const start = (safePage - 1) * PAGE_SIZE;
    const pageItems = filtered.slice(start, start + PAGE_SIZE);

    const selectedGone =
      (selectedStudent && tab === "students" && !filtered.some((s: any) => s.studentId === selectedStudent.studentId)) ||
      (selectedTeacher && tab === "teachers" && !filtered.some((t: any) => t.teacherId === selectedTeacher.teacherId));
    if (selectedGone) {
      setSelectedStudent(null);
      setSelectedTeacher(null);
      resetDetail();
    }

    if (page !== safePage) {
      queueMicrotask(() => setPage(safePage));
    }

    return { pageItems, totalPages, totalCount };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, students, teachers, query, page, selectedStudent, selectedTeacher]);

  useEffect(() => {
    const totalPagesAfter = totalPages;
    if (page > totalPagesAfter) setPage(totalPagesAfter);
  }, [totalPages, page]);

  /* ---- 렌더 ---- */
  return (
    <div className="space-y-6">
      {/* 상단 탭 */}
      <div className="flex items-center gap-2">
        {(["students", "teachers"] as const).map((k) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={`px-4 py-2 rounded-full font-medium ring-1 ring-black/20 transition
              ${tab === k ? "bg-[#8CF39B] text-black" : "bg-[#CFF9D6] text-black hover:bg-[#B7F2C0]"}`}
          >
            {k === "students" ? "학생 명단" : "선생 명단"}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[360px_1fr] gap-6">
        {/* 좌측 목록 */}
        <div className="rounded-2xl bg-white ring-1 ring-black/20 shadow-sm">
          {/* 헤더 + 검색 */}
          <div className="p-4 border-b border-black/20">
            <div className="flex items-center justify-between mb-2">
              <div className="text-sm font-semibold text-black">
                {tab === "students" ? "학생 목록" : "선생 목록"}
              </div>
              {loadingList && <div className="text-xs text-black">불러오는 중…</div>}
            </div>
            {errList && <div className="text-sm text-red-600 mb-2">오류: {errList}</div>}

            <div className="flex items-center gap-2">
              <div className="relative flex-1">
                <input
                  value={query}
                  placeholder={tab === "students" ? "이름/ID/학교/학년 검색…" : "이름/ID/전화/학원번호 검색…"}
                  onChange={(e) => setQuery(e.target.value)}
                  className="w-full rounded-xl px-3 py-2 text-sm ring-1 ring-black/20 bg-white focus:outline-none focus:ring-2 focus:ring-emerald-400 text-black placeholder-black"
                />
                {query && (
                  <button
                    onClick={() => setQuery("")}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-xs px-2 py-0.5 rounded bg-gray-100 ring-1 ring-black/20 text-black"
                  >
                    초기화
                  </button>
                )}
              </div>
              <div className="text-xs text-black min-w-[90px] text-right">
                총 <span className="font-semibold text-black">{totalCount}</span>명
              </div>
            </div>
          </div>

          {/* 목록 + 페이지네이션 */}
          <div className="rounded-xl overflow-hidden">
            {pageItems.length === 0 && !loadingList ? (
              <div className="px-3 py-2 text-sm text-black">표시할 항목이 없습니다.</div>
            ) : (
              <>
                <div>
                  {pageItems.map((p: any) => (
                    <div
                      key={p.studentId ?? p.teacherId}
                      className="px-3 py-2 border-b last:border-none border-black/10 bg-white text-sm flex items-center justify-between gap-2"
                    >
                      <div className="min-w-0">
                        <div className="font-medium text-black truncate">
                          {tab === "students" ? p.name || p.studentId : p.name || p.teacherId}
                        </div>
                        <div className="text-xs text-black truncate">
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
                          className="rounded-lg px-2.5 py-1.5 text-xs ring-1 ring-black/20 bg-gray-50 hover:bg-gray-100 text-black"
                        >
                          {tab === "students" ? "출결/일정" : "담당 반"}
                        </button>
                        <button
                          onClick={() => (tab === "students" ? deleteStudent(p.studentId) : deleteTeacher(p.teacherId))}
                          className="rounded-lg px-2.5 py-1.5 text-xs ring-1 ring-red-300 bg-red-50 hover:bg-red-100 text-red-700"
                        >
                          삭제
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                <Pager page={page} total={totalPages} onChange={setPage} />
              </>
            )}
          </div>
        </div>

        {/* 우측 상세 */}
        <div className="space-y-6">
          {/* 학생: 수업 일정 */}
          {selectedStudent && (
            <div className="rounded-2xl bg-white ring-1 ring-black/20 shadow-sm p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="text-sm font-semibold text-black">
                  {selectedStudent.name || selectedStudent.studentId} 님의 수업 일정
                </div>
                {loadingDetail && <div className="text-xs text-black">불러오는 중…</div>}
              </div>
              {errDetail && <div className="text-sm text-red-600 mb-2">오류: {errDetail}</div>}

              <div className="rounded-xl overflow-hidden ring-1 ring-black/20">
                {classes.length === 0 && !loadingDetail ? (
                  <div className="px-3 py-2 text-sm text-black">수업 일정이 없습니다.</div>
                ) : (
                  classes.map((c) => (
                    <div key={c.classId} className="px-3 py-2 border-b last:border-none border-black/10 bg-white">
                      <div className="font-medium text-black">{c.className}</div>
                      <div className="text-xs text-black">{fmtSchedule(c)}</div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* 선생: 담당 반 목록 */}
          {selectedTeacher && (
            <div className="rounded-2xl bg-white ring-1 ring-black/20 shadow-sm p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="text-sm font-semibold text-black">
                  {selectedTeacher.name || selectedTeacher.teacherId} 선생님의 담당 반
                </div>
                {loadingDetail && <div className="text-xs text-black">불러오는 중…</div>}
              </div>
              {errDetail && <div className="text-sm text-red-600 mb-2">오류: {errDetail}</div>}

              <div className="rounded-xl overflow-hidden ring-1 ring-black/20">
                {classes.length === 0 && !loadingDetail ? (
                  <div className="px-3 py-2 text-sm text-black">담당 반이 없습니다.</div>
                ) : (
                  classes.map((c) => (
                    <div key={c.classId} className="px-3 py-2 border-b last:border-none border-black/10 bg-white">
                      <div className="font-medium text-black">{c.className}</div>
                      <div className="text-xs text-black">{fmtSchedule(c)}</div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* 출결 표: 학생일 때만 */}
          {selectedStudent && (
            <div className="rounded-2xl bg-white ring-1 ring-black/20 shadow-sm p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="text-sm font-semibold text-black">출결 내역</div>
                {loadingDetail && <div className="text-xs text-black">불러오는 중…</div>}
              </div>
              {errDetail && <div className="text-sm text-red-600 mb-2">오류: {errDetail}</div>}

              <div className="rounded-xl overflow-hidden ring-1 ring-black/20">
                {attendance.length === 0 && !loadingDetail ? (
                  <div className="px-3 py-2 text-sm text-black">출결 기록이 없습니다.</div>
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
                          <tr key={`${r.date}-${r.classId}-${i}`} className="border-t border-black/10">
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
            <div className="rounded-2xl bg-white ring-1 ring-black/20 shadow-sm p-6 text-sm text-black">
              왼쪽 목록에서 학생 또는 선생을 선택하면 상세(수업/출결)를 보여줍니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
