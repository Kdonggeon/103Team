"use client";
import { useEffect, useState } from "react";
import { api, type CourseLite } from "@/app/lib/api";

export default function TeacherManagePanel({
  teacherId,
  defaultAcademy,
}: {
  teacherId: string;
  defaultAcademy: number | null;
}) {
  const [items, setItems] = useState<CourseLite[]>([]);
  const [className, setClassName] = useState("");
  const [roomNumber, setRoomNumber] = useState<string>("");

  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!teacherId) {
      setErr("로그인이 필요합니다.");
      setLoading(false);
      return;
    }
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      const res = await api.listMyClasses(teacherId);
      setItems(res || []);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teacherId]);

  const create = async () => {
    if (!defaultAcademy) {
      setErr("학원번호가 없습니다.");
      return;
    }
    if (!className.trim()) {
      setErr("반 이름을 입력하세요.");
      return;
    }
    try {
      setErr(null);
      setMsg(null);
      await api.createClass({
        className: className.trim(),
        teacherId,
        academyNumber: defaultAcademy,
        roomNumber: roomNumber ? Number(roomNumber) : undefined,
      });
      setClassName("");
      setRoomNumber("");
      await load();
      setMsg("반이 생성되었습니다.");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
    <div className="p-0 space-y-4">
      <div className="text-xl font-bold">내 반 관리</div>

      <div className="flex flex-wrap gap-3 items-end bg-white border rounded p-3">
        <div>
          <label className="block text-sm text-gray-600">반 이름</label>
          <input
            value={className}
            onChange={(e) => setClassName(e.target.value)}
            className="border rounded px-2 py-1"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-600">방 번호(선택)</label>
          <input
            value={roomNumber}
            onChange={(e) => setRoomNumber(e.target.value)}
            className="border rounded px-2 py-1 w-28"
          />
        </div>
        <button onClick={create} className="bg-emerald-600 text-white px-3 py-2 rounded">
          반 만들기
        </button>
        {msg && <span className="text-emerald-600">{msg}</span>}
        {err && <span className="text-red-600">{err}</span>}
      </div>

      {loading && <div>불러오는 중…</div>}

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((c) => (
          <a
            key={c.classId}
            href={`/teacher/classes/${encodeURIComponent(c.classId)}`}
            className="bg-white border rounded p-3 hover:shadow"
          >
            <div className="font-semibold">{c.className}</div>
            <div className="text-sm text-gray-600">Room #{c.roomNumber ?? "-"}</div>
            <div className="text-sm text-gray-600">학생 수: {c.students?.length ?? 0}</div>
          </a>
        ))}
        {!loading && items.length === 0 && (
          <div className="text-gray-500">생성된 반이 없습니다. 위에서 새로 만들어 주세요.</div>
        )}
      </div>
    </div>
  );
}
