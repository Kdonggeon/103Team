"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { api, type CourseLite, type LoginResponse } from "@/app/lib/api";
import { roomsApi, type Room } from "@/app/lib/rooms";

/** 안전한 roomNumber 추출 (백엔드 혼재 대응) */
function getRoomNumber(r: Room | Record<string, unknown>): number {
  const n = (r as any).roomNumber ?? (r as any).Room_Number;
  return typeof n === "number" ? n : Number(n);
}

type Props = { user: NonNullable<LoginResponse> };

export default function TeacherManagePanel({ user }: Props) {
  const teacherId = user.username;
  const academyNumber = user.academyNumbers?.[0] ?? 0;

  /** ---------- 상태 ---------- */
  const [rooms, setRooms] = useState<Room[]>([]);
  const [classes, setClasses] = useState<CourseLite[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [loadingClasses, setLoadingClasses] = useState(false);

  // UI 입력
  const [className, setClassName] = useState("");
  const [selectedRooms, setSelectedRooms] = useState<number[]>([]);
  const [query, setQuery] = useState("");
  const [grade, setGrade] = useState<string>("");

  // 검색 결과/선택 학생
  const [hits, setHits] = useState<{ studentId: string; studentName?: string; grade?: number }[]>([]);
  const [selectedStudents, setSelectedStudents] = useState<string[]>([]);

  // 메시지
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  /** ---------- 데이터 로드 ---------- */
  const loadRooms = useCallback(async () => {
    setLoadingRooms(true);
    setErr(null);
    try {
      const list = await roomsApi.listRooms(academyNumber);
      setRooms(list || []);
    } catch (e: any) {
      setErr("강의실 목록 불러오기 실패: " + e.message);
    } finally {
      setLoadingRooms(false);
    }
  }, [academyNumber]);

  const loadClasses = useCallback(async () => {
    setLoadingClasses(true);
    setErr(null);
    try {
      const list = await api.listMyClasses(teacherId);
      setClasses(list || []);
    } catch (e: any) {
      setErr("반 목록 불러오기 실패: " + e.message);
    } finally {
      setLoadingClasses(false);
    }
  }, [teacherId]);

  useEffect(() => { loadRooms(); }, [loadRooms]);
  useEffect(() => { loadClasses(); }, [loadClasses]);

  /** ---------- 메모 ---------- */
  const roomOptions = useMemo(
    () => rooms.map(r => getRoomNumber(r)).filter(n => !Number.isNaN(n)),
    [rooms]
  );

  /** ---------- 핸들러 ---------- */
  const toggleRoom = useCallback((rn: number) => {
    setSelectedRooms(prev => prev.includes(rn) ? prev.filter(x => x !== rn) : [...prev, rn]);
  }, []);

  const toggleStudent = useCallback((sid: string) => {
    setSelectedStudents(prev => prev.includes(sid) ? prev.filter(x => x !== sid) : [...prev, sid]);
  }, []);

  const clearMsg = useCallback(() => { setMsg(null); setErr(null); }, []);

  const search = useCallback(async () => {
    clearMsg();
    try {
      const res = await api.searchStudents(academyNumber, query.trim(), grade ? Number(grade) : undefined);
      setHits((res || []).map((h: any) => ({
        studentId: h.studentId,
        studentName: h.studentName,
        grade: h.grade,
      })));
    } catch (e: any) {
      setErr("학생 검색 실패: " + e.message);
    }
  }, [academyNumber, query, grade, clearMsg]);

  const createClass = useCallback(async () => {
    clearMsg();

    if (!className.trim()) { setErr("반 이름을 입력하세요."); return; }
    if (selectedRooms.length === 0) { setErr("하나 이상의 강의실을 선택하세요."); return; }

    try {
      for (const rn of selectedRooms) {
        const created = await api.createClass({
          className: className.trim(),
          teacherId,
          academyNumber,
          roomNumber: rn,
        });

        if (created?.classId && selectedStudents.length > 0) {
          await Promise.all(selectedStudents.map(sid => api.addStudentToClass(created.classId!, sid)));
        }
      }

      setMsg("반이 성공적으로 생성되었습니다!");
      // 입력 초기화
      setClassName("");
      setSelectedRooms([]);
      setSelectedStudents([]);
      setHits([]);
      setQuery("");
      setGrade("");

      await loadClasses();
    } catch (e: any) {
      setErr("반 생성 실패: " + e.message);
    }
  }, [academyNumber, className, selectedRooms, selectedStudents, teacherId, loadClasses, clearMsg]);

  /** ---------- UI ---------- */
  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      <div className="max-w-5xl mx-auto bg-white rounded-2xl shadow-lg p-6 space-y-6">
        <h1 className="text-2xl font-bold text-black">강의실 관리</h1>

        {/* 메시지 영역 */}
        {(msg || err) && (
          <div className={`rounded-lg px-4 py-3 ${err ? "bg-red-50 text-red-700" : "bg-emerald-50 text-emerald-700"}`}>
            {err ?? msg}
          </div>
        )}

        {/* 반 생성 */}
        <section className="border border-gray-200 rounded-xl p-5 space-y-4 bg-white">
          <div className="grid sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-black mb-1">반 이름</label>
              <input
                value={className}
                onChange={(e) => setClassName(e.target.value)}
                placeholder="예) 3학년 수학 A반"
                className="border border-gray-300 rounded px-3 py-2 w-full text-black focus:ring-emerald-500 focus:ring-2"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-black mb-1">강의실 선택 (여러 개)</label>
              <div className="flex flex-wrap gap-2">
                {loadingRooms && <span className="text-sm text-gray-500">방 불러오는 중…</span>}
                {!loadingRooms && roomOptions.map(rn => {
                  const selected = selectedRooms.includes(rn);
                  return (
                    <button
                      key={rn}
                      type="button"
                      onClick={() => toggleRoom(rn)}
                      className={`px-4 py-1 rounded-full border transition ${
                        selected
                          ? "bg-emerald-100 border-emerald-400 text-emerald-700"
                          : "bg-white border-gray-400 text-black hover:bg-gray-50"
                      }`}
                    >
                      Room {rn}
                    </button>
                  );
                })}
              </div>
              <div className="text-xs text-black mt-1">방 {rooms.length}개</div>
            </div>
          </div>

          {/* 학생 검색 */}
          <div>
            <label className="block text-sm font-semibold text-black mb-1">학생 추가 (선택)</label>
            <div className="flex gap-2 mb-2">
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="이름 검색"
                className="border border-gray-300 rounded px-3 py-1 text-black"
              />
              <input
                value={grade}
                onChange={(e) => setGrade(e.target.value)}
                placeholder="학년(선택)"
                className="border border-gray-300 rounded px-3 py-1 text-black w-28"
              />
              <button
                type="button"
                onClick={search}
                className="bg-emerald-600 text-white px-4 py-1 rounded hover:bg-emerald-700"
              >
                검색
              </button>
            </div>

            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
              {hits.map(h => {
                const picked = selectedStudents.includes(h.studentId);
                return (
                  <button
                    key={h.studentId}
                    type="button"
                    onClick={() => toggleStudent(h.studentId)}
                    className={`text-left border rounded px-3 py-2 ${
                      picked ? "bg-emerald-50 border-emerald-400" : "bg-white border-gray-300 hover:bg-gray-50"
                    }`}
                  >
                    <div className="font-medium text-black">
                      {h.studentName ?? h.studentId}
                      {picked && <span className="ml-2 text-emerald-600 text-xs">선택됨</span>}
                    </div>
                    <div className="text-xs text-black">
                      ID: {h.studentId} · 학년: {h.grade ?? "-"}
                    </div>
                  </button>
                );
              })}
              {hits.length === 0 && (
                <div className="text-gray-500">검색 결과 없음</div>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={createClass}
            className="bg-emerald-600 text-white px-5 py-2 rounded hover:bg-emerald-700"
          >
            반 만들기
          </button>
        </section>

        {/* 내 반 목록 */}
        <section>
          <h2 className="text-lg font-semibold text-black mb-2">내 반 목록</h2>
          {loadingClasses && <div className="text-sm text-gray-500">불러오는 중…</div>}
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {classes.map((c) => (
              <a
                key={c.classId}
                href={`/teacher/classes/${encodeURIComponent(c.classId)}`}
                className="bg-white border border-gray-300 rounded-xl p-3 hover:shadow transition"
              >
                <div className="font-semibold text-black">{c.className}</div>
                <div className="text-sm text-black">Room #{c.roomNumber ?? "-"}</div>
                <div className="text-sm text-black">학생 수: {c.students?.length ?? 0}</div>
                <div className="text-emerald-600 text-sm mt-1">학생 관리 · 시간표</div>
              </a>
            ))}
            {!loadingClasses && classes.length === 0 && (
              <div className="text-gray-600 text-sm">아직 생성된 반이 없습니다.</div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
