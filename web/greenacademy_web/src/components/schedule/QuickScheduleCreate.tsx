"use client";

import React, { useEffect, useMemo, useState } from "react";
import api, {
  type CourseLite,
  type CreateScheduleReq,
  type LoginResponse,
} from "@/app/lib/api";
import { roomsApi, type Room } from "@/app/lib/rooms";

// roomNumber 안전 추출
const getRoomNumber = (r: Room) =>
  Number((r as any).roomNumber ?? (r as any).number ?? (r as any).Room_Number);

// 날짜 유틸(오늘)
const todayISO = () => new Date().toISOString().slice(0, 10);

type Props = {
  user: NonNullable<LoginResponse>;
  open: boolean;
  onClose: () => void;
  onCreated: () => void; // 생성 후 상위에서 캘린더 재로드
};

export default function QuickScheduleCreate({
  user,
  open,
  onClose,
  onCreated,
}: Props) {
  const teacherId = user.username;
  const academyNumber = user.academyNumbers?.[0] ?? null;

  // 목록들
  const [classes, setClasses] = useState<CourseLite[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);

  // 폼 상태
  const [classId, setClassId] = useState<string>("");
  const [date, setDate] = useState<string>(todayISO());
  const [startTime, setStartTime] = useState<string>("10:00");
  const [endTime, setEndTime] = useState<string>("11:00");
  const [title, setTitle] = useState<string>("");
  const [roomNumber, setRoomNumber] = useState<number | "">("");

  // 상태
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);

  // 현재 선택된 반의 “관리에서 저장한 방들(roomNumbers)”를 가져오기
  const [classRooms, setClassRooms] = useState<number[]>([]);

  useEffect(() => {
    if (!open) return;
    (async () => {
      setErr(null);
      try {
        const list = await api.listMyClasses(teacherId);
        setClasses(list ?? []);
      } catch (e: any) {
        setErr(e?.message ?? "반 목록 불러오기 실패");
      }
    })();
  }, [open, teacherId]);

  useEffect(() => {
    (async () => {
      if (!academyNumber || !open) return;
      try {
        const r = await roomsApi.listRooms(academyNumber);
        setRooms(Array.isArray(r) ? r : []);
      } catch {
        // 보조 데이터라 콘솔만
        console.warn("roomsApi.listRooms 실패");
      }
    })();
  }, [academyNumber, open]);

  // 반 선택 시 해당 반의 roomNumbers 우선 사용, 없으면 전체 rooms
  useEffect(() => {
    (async () => {
      setClassRooms([]);
      setRoomNumber("");
      if (!classId) return;
      try {
        const detail = await api.getClassDetail(classId);
        const savedRooms: number[] = (detail as any).roomNumbers ?? [];
        const single = (detail as any).roomNumber;
        const merged = Array.from(
          new Set([...(Array.isArray(savedRooms) ? savedRooms : []), ...(single != null ? [Number(single)] : [])])
        ).filter((n) => Number.isFinite(n));
        setClassRooms(merged as number[]);
        // 반 이름을 기본 title로 제안
        setTitle((detail as any).className ?? "");
      } catch (e: any) {
        setErr(e?.message ?? "반 상세 불러오기 실패");
      }
    })();
  }, [classId]);

  const roomOptions = useMemo(() => {
    // 1순위: 반에 저장된 roomNumbers
    if (classRooms.length > 0) return classRooms.sort((a, b) => a - b);
    // 2순위: 관리의 방 목록
    const all = rooms.map(getRoomNumber).filter((n) => Number.isFinite(n));
    return Array.from(new Set(all)).sort((a, b) => a - b);
  }, [classRooms, rooms]);

  const submit = async () => {
    if (!teacherId || !classId || !date || !startTime || !endTime) {
      setErr("필수 항목(반/날짜/시간)이 비어 있습니다.");
      return;
    }
    setLoading(true);
    setErr(null);
    setMsg(null);
    try {
      const body: CreateScheduleReq = {
        date,
        classId,
        title: title?.trim() || undefined,
        startTime,
        endTime,
        roomNumber: roomNumber === "" ? undefined : Number(roomNumber),
      };
      await api.createSchedule(teacherId, body);
      setMsg("스케줄이 등록되었습니다.");
      onCreated?.();
      // 폼 초기화 (원하면 유지도 가능)
      setClassId("");
      setRoomNumber("");
      setTitle("");
      setStartTime("10:00");
      setEndTime("11:00");
    } catch (e: any) {
      setErr(e?.message ?? "스케줄 저장 실패");
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[999] flex items-center justify-center bg-black/40">
      <div className="w-[720px] max-w-[92vw] rounded-2xl bg-white border border-black shadow-xl p-5 space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-gray-900">빠른 스케줄 등록</h3>
          <button
            onClick={onClose}
            className="px-3 py-1.5 rounded border border-black text-gray-900 hover:bg-gray-100"
          >
            닫기
          </button>
        </div>

        {err && <div className="text-sm text-red-600">{err}</div>}
        {msg && <div className="text-sm text-emerald-700">{msg}</div>}

        {/* 폼 */}
        <div className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm text-gray-900">반 선택</label>
            <select
              value={classId}
              onChange={(e) => setClassId(e.target.value)}
              className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
            >
              <option value="">선택…</option>
              {classes.map((c) => (
                <option key={c.classId} value={c.classId}>
                  {c.className} ({c.classId})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm text-gray-900">날짜</label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-900">시작</label>
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-900">종료</label>
            <input
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="block text-sm text-gray-900">제목(선택)</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
              placeholder="예) 수학(심화) 2반"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="block text-sm text-gray-900">방 선택 (관리에서 저장한 목록 우선)</label>
            <select
              value={roomNumber === "" ? "" : String(roomNumber)}
              onChange={(e) => setRoomNumber(e.target.value === "" ? "" : Number(e.target.value))}
              className="border border-black rounded px-2 py-1 w-full bg-white text-gray-900"
            >
              <option value="">미배정</option>
              {roomOptions.map((rn) => (
                <option key={rn} value={String(rn)}>
                  Room {rn}
                </option>
              ))}
            </select>
            <p className="text-xs text-gray-600 mt-1">
              * 반에 저장된 방이 있으면 그 목록만 표시되고, 없으면 학원 전체 방 목록이 표시됩니다.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={submit}
            disabled={loading || !classId || !date || !startTime || !endTime}
            className="px-4 py-2 rounded bg-emerald-600 text-white disabled:opacity-50"
          >
            {loading ? "저장 중…" : "저장"}
          </button>
          <button onClick={onClose} className="px-4 py-2 rounded border border-black text-gray-900">
            취소
          </button>
        </div>
      </div>
    </div>
  );
}
