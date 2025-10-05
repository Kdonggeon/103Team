"use client";

import { useEffect, useState } from "react";
import { api, type CourseLite, type StudentHit } from "@/app/lib/api";
import { getSession } from "@/app/lib/session";

export default function ClassDetailPage({
  params,
}: {
  params: { classId: string };
}) {
  const classId = params.classId;
  const me = getSession();
  if (!me) {
    if (typeof window !== "undefined") location.href = "/login";
    return null;
  }
  const academyNumber = me.academyNumbers?.[0] ?? null;

  const [data, setData] = useState<CourseLite | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // 수정 폼
  const [newName, setNewName] = useState("");
  const [newRoom, setNewRoom] = useState<string>("");

  // 검색
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState<string>("");
  const [hits, setHits] = useState<StudentHit[]>([]);
  const [searching, setSearching] = useState(false);

  const load = async () => {
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      const c = await api.getClassDetail(classId);
      setData(c);
      setNewName(c.className || "");
      setNewRoom(c.roomNumber != null ? String(c.roomNumber) : "");
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId]);

  const saveInfo = async () => {
    try {
      setErr(null);
      setMsg(null);
      await api.patchClass(classId, {
        className: newName || undefined,
        roomNumber: newRoom ? Number(newRoom) : undefined,
      });
      await load();
      setMsg("반 정보가 저장되었습니다.");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const search = async () => {
    if (!academyNumber) {
      setErr("학원번호 없음");
      return;
    }
    try {
      setSearching(true);
      setErr(null);
      const res = await api.searchStudents(
        academyNumber,
        q,
        grade ? Number(grade) : undefined
      );
      setHits(res);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setSearching(false);
    }
  };

  const addStudent = async (sid: string) => {
    try {
      setErr(null);
      setMsg(null);
      await api.addStudentToClass(classId, sid);
      await load();
      setMsg("학생이 추가되었습니다.");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const removeStudent = async (sid: string) => {
    try {
      setErr(null);
      setMsg(null);
      await api.removeStudentFromClass(classId, sid);
      await load();
      setMsg("학생이 제거되었습니다.");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  if (loading) return <div className="p-6">불러오는 중…</div>;
  if (err) return <div className="p-6 text-red-600">{err}</div>;
  if (!data) return <div className="p-6">데이터가 없습니다.</div>;

  return (
    <div className="max-w-7xl mx-auto px-6 py-6 space-y-6">
      <div className="flex items-end gap-3">
        <h1 className="text-xl font-bold">{data.className}</h1>
        <span className="text-gray-600">({data.classId})</span>
      </div>

      {/* 반 정보 수정 */}
      <div className="bg-white border rounded p-4 space-y-3">
        <div className="font-semibold">반 정보</div>
        <div className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm text-gray-600">반 이름</label>
            <input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="border rounded px-2 py-1 w-full"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600">방 번호</label>
            <input
              value={newRoom}
              onChange={(e) => setNewRoom(e.target.value)}
              className="border rounded px-2 py-1 w-full"
            />
          </div>
        </div>
        <button onClick={saveInfo} className="bg-emerald-600 text-white px-3 py-2 rounded">
          저장
        </button>
        {msg && <span className="text-emerald-600 ml-2">{msg}</span>}
      </div>

      {/* 현재 학생 목록 */}
      <div className="bg-white border rounded p-4">
        <div className="font-semibold mb-2">등록 학생 ({data.students?.length ?? 0})</div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
          {(data.students || []).map((sid) => (
            <div key={sid} className="border rounded px-3 py-2 flex items-center justify-between">
              <span>{sid}</span>
              <button
                onClick={() => removeStudent(sid)}
                className="text-red-600 text-sm hover:underline"
              >
                삭제
              </button>
            </div>
          ))}
          {(data.students || []).length === 0 && (
            <div className="text-gray-500">아직 학생이 없습니다.</div>
          )}
        </div>
      </div>

      {/* 학생 검색/추가 */}
      <div className="bg-white border rounded p-4 space-y-3">
        <div className="font-semibold">학생 검색</div>
        <div className="flex flex-wrap gap-2">
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="이름 검색"
            className="border rounded px-2 py-1"
          />
          <input
            value={grade}
            onChange={(e) => setGrade(e.target.value)}
            placeholder="학년(선택)"
            className="border rounded px-2 py-1 w-32"
          />
          <button onClick={search} className="bg-gray-800 text-white px-3 py-1 rounded">
            검색
          </button>
          {searching && <span className="text-gray-500">검색중…</span>}
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
          {hits.map((h) => (
            <div key={h.studentId} className="border rounded px-3 py-2 flex items-center justify-between">
              <div>
                <div className="font-medium">{h.studentName ?? h.studentId}</div>
                <div className="text-xs text-gray-600">
                  ID: {h.studentId} · 학년: {h.grade ?? "-"}
                </div>
              </div>
              <button
                onClick={() => addStudent(h.studentId)}
                className="text-emerald-600 text-sm hover:underline"
              >
                추가
              </button>
            </div>
          ))}
          {hits.length === 0 && <div className="text-gray-500">검색 결과 없음</div>}
        </div>
      </div>
    </div>
  );
}
