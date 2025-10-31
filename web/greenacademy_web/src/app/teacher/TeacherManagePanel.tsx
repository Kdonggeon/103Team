"use client";

import { useEffect, useState } from "react";
import { api, type CourseLite, type LoginResponse } from "@/app/lib/api";
import { roomsApi, type Room } from "@/app/lib/rooms";

export default function TeacherManagePanel({ user }: { user: NonNullable<LoginResponse> }) {
  const teacherId = user.username;
  const academyNumber = user.academyNumbers?.[0] ?? 0;

  const [items, setItems] = useState<CourseLite[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [className, setClassName] = useState("");
  const [selectedRooms, setSelectedRooms] = useState<string[]>([]);
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState("");
  const [hits, setHits] = useState<any[]>([]);
  const [selectedStudents, setSelectedStudents] = useState<string[]>([]);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // ✅ 강의실 목록 불러오기
  useEffect(() => {
    (async () => {
      try {
        const list = await roomsApi.listRooms(academyNumber);
        setRooms(list || []);
      } catch (e: any) {
        setErr("강의실 목록 불러오기 실패: " + e.message);
      }
    })();
  }, [academyNumber]);

  // ✅ 반 목록 불러오기
  const load = async () => {
    try {
      setLoading(true);
      const res = await api.listMyClasses(teacherId);
      setItems(res || []);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  // ✅ 학생 검색
  const search = async () => {
    try {
      const res = await api.searchStudents(
        academyNumber,
        q,
        grade ? Number(grade) : undefined
      );
      setHits(res);
    } catch (e: any) {
      setErr(e.message);
    }
  };

  // ✅ 학생 선택/해제
  const toggleStudent = (sid: string) => {
    setSelectedStudents(prev =>
      prev.includes(sid) ? prev.filter(x => x !== sid) : [...prev, sid]
    );
  };

  // ✅ 방 선택/해제
  const toggleRoom = (roomNumber: string) => {
    setSelectedRooms(prev =>
      prev.includes(roomNumber)
        ? prev.filter(r => r !== roomNumber)
        : [...prev, roomNumber]
    );
  };

  // ✅ 반 만들기 (여러 방에 생성)
  const createClass = async () => {
    if (!className.trim()) {
      setErr("반 이름을 입력하세요.");
      return;
    }
    if (selectedRooms.length === 0) {
      setErr("하나 이상의 강의실을 선택하세요.");
      return;
    }

    try {
      setErr(null);
      setMsg(null);

      for (const rnStr of selectedRooms) {
        const rn = Number(rnStr);
        const created = await api.createClass({
          className,
          teacherId,
          academyNumber,
          roomNumber: rn,
        });

        // 선택된 학생도 자동 추가
        if (created?.classId && selectedStudents.length > 0) {
          for (const sid of selectedStudents) {
            await api.addStudentToClass(created.classId, sid);
          }
        }
      }

      setMsg("반이 성공적으로 생성되었습니다!");
      setClassName("");
      setSelectedRooms([]);
      setSelectedStudents([]);
      setHits([]);
      setQ("");
      setGrade("");
      await load();
    } catch (e: any) {
      setErr("반 생성 실패: " + e.message);
    }
  };

  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      <div className="max-w-5xl mx-auto bg-white rounded-2xl shadow-lg p-6 space-y-6">
        <h1 className="text-2xl font-bold text-black">반 관리</h1>

        {/* === 반 생성 섹션 === */}
        <div className="border border-gray-200 rounded-xl p-5 space-y-4 bg-white">
          <div className="grid sm:grid-cols-2 gap-4">
            {/* 반 이름 */}
            <div>
              <label className="block text-sm font-semibold text-black mb-1">반 이름</label>
              <input
                value={className}
                onChange={(e) => setClassName(e.target.value)}
                placeholder="예) 3학년 수학 A반"
                className="border border-gray-300 rounded px-3 py-2 w-full text-black focus:ring-emerald-500 focus:ring-2"
              />
            </div>

            {/* 강의실 선택 */}
            <div>
              <label className="block text-sm font-semibold text-black mb-1">
                강의실 선택 (여러 개)
              </label>
              <div className="flex flex-wrap gap-2">
                {rooms.map((r) => {
                  const rn = String((r as any).roomNumber ?? (r as any).Room_Number);
                  const selected = selectedRooms.includes(rn);
                  return (
                    <button
                      key={rn}
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
            <label className="block text-sm font-semibold text-black mb-1">
              학생 추가 (선택)
            </label>
            <div className="flex gap-2 mb-2">
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
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
                onClick={search}
                className="bg-emerald-600 text-white px-4 py-1 rounded hover:bg-emerald-700"
              >
                검색
              </button>
            </div>

            {/* 검색 결과 */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
              {hits.map((h) => {
                const picked = selectedStudents.includes(h.studentId);
                return (
                  <button
                    key={h.studentId}
                    onClick={() => toggleStudent(h.studentId)}
                    className={`text-left border rounded px-3 py-2 ${
                      picked
                        ? "bg-emerald-50 border-emerald-400"
                        : "bg-white border-gray-300 hover:bg-gray-50"
                    }`}
                  >
                    <div className="font-medium text-black">
                      {h.studentName ?? h.studentId}
                      {picked && (
                        <span className="ml-2 text-emerald-600 text-xs">선택됨</span>
                      )}
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
            onClick={createClass}
            className="bg-emerald-600 text-white px-5 py-2 rounded hover:bg-emerald-700"
          >
            반 만들기
          </button>

          {msg && <div className="text-emerald-600 font-medium">{msg}</div>}
          {err && <div className="text-red-600 font-medium">{err}</div>}
        </div>

        {/* === 반 목록 === */}
        <div>
          <h2 className="text-lg font-semibold text-black mb-2">내 반 목록</h2>
          {loading && <div className="text-sm text-gray-500">불러오는 중…</div>}
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {items.map((c) => (
              <a
                key={c.classId}
                href={`/teacher/classes/${encodeURIComponent(c.classId)}`}
                className="bg-white border border-gray-300 rounded-xl p-3 hover:shadow transition"
              >
                <div className="font-semibold text-black">{c.className}</div>
                <div className="text-sm text-black">Room #{c.roomNumber ?? "-"}</div>
                <div className="text-sm text-black">
                  학생 수: {c.students?.length ?? 0}
                </div>
                <div className="text-emerald-600 text-sm mt-1">학생 관리 · 시간표</div>
              </a>
            ))}
            {items.length === 0 && (
              <div className="text-gray-600 text-sm">아직 생성된 반이 없습니다.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
