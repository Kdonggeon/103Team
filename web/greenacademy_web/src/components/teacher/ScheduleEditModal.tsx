"use client";

import React, { useEffect, useMemo, useState } from "react";
import api from "@/app/lib/api";
import { roomsApi } from "@/app/lib/rooms";

type EditEvent = {
  id?: string;
  date: string;           // YYYY-MM-DD
  classId: string;        // 반 ID
  title?: string;         // 제목(선택)
  startTime?: string;     // HH:mm
  endTime?: string;       // HH:mm
  roomNumber?: number | ""; // 선택 안 함이면 ""
};

export type ScheduleEditModalProps = {
  open: boolean;
  onClose: () => void;
  event: EditEvent | null;

  /** 저장(생성/덮어쓰기) */
  onSave: (patch: {
    date: string;
    classId: string;
    title: string;
    startTime: string;
    endTime: string;
    roomNumber?: number;
  }) => Promise<void> | void;

  /** 삭제 */
  onDelete: (scheduleId?: string) => Promise<void> | void;

  /** 반/강의실 로드를 위해 필요 */
  teacherId: string;
  academyNumber?: number | string | null;
};

type MyClassLite = { classId: string; className?: string };
type MyRoomLite  = { roomNumber: number; roomName?: string };

export default function ScheduleEditModal({
  open,
  onClose,
  event,
  onSave,
  onDelete,
  teacherId,
  academyNumber,
}: ScheduleEditModalProps) {
  const [classes, setClasses] = useState<MyClassLite[]>([]);
  const [rooms, setRooms] = useState<MyRoomLite[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 폼 상태
  const [date, setDate] = useState<string>(event?.date ?? "");
  const [classId, setClassId] = useState<string>(event?.classId ?? "");
  const [title, setTitle] = useState<string>(event?.title ?? "");
  const [startTime, setStartTime] = useState<string>(event?.startTime ?? "10:00");
  const [endTime, setEndTime] = useState<string>(event?.endTime ?? "11:00");
  const [roomNumber, setRoomNumber] = useState<number | "">(event?.roomNumber ?? "");

  // 이벤트가 바뀌면 폼 동기화
  useEffect(() => {
    if (!open) return;
    setDate(event?.date ?? "");
    setClassId(event?.classId ?? "");
    setTitle(event?.title ?? "");
    setStartTime(event?.startTime ?? "10:00");
    setEndTime(event?.endTime ?? "11:00");
    setRoomNumber(event?.roomNumber ?? "");
  }, [open, event]);

  // 데이터 로드 + 프리셀렉트 보장
  useEffect(() => {
    if (!open) return;
    (async () => {
      try {
        setLoading(true);
        setErr(null);

        // 1) 반 목록
        const list = await api.listMyClasses(teacherId);
        let cls: MyClassLite[] = (list ?? []).map((c: any) => ({
          classId: c.classId,
          className: c.className ?? c.classId,
        }));

        // 이벤트의 classId가 목록에 없으면 임시로 넣어서라도 보이게
        if (event?.classId && !cls.some(c => c.classId === event.classId)) {
          cls = [{ classId: event.classId, className: event.classId }, ...cls];
        }
        setClasses(cls);

        // 선택값 보장
        if (event?.classId) setClassId(event.classId);
        else if (!event?.classId && cls.length && !classId) setClassId(cls[0].classId);

        // 2) 강의실 목록
        let rms: MyRoomLite[] = [];
        if (academyNumber != null) {
          const raw = await roomsApi.listRooms(Number(academyNumber));
          rms = (Array.isArray(raw) ? raw : [])
            .map((r: any) => ({
              roomNumber: Number(r.roomNumber ?? r.Room_Number ?? r.number),
              roomName: r.roomName ?? r.name ?? undefined,
            }))
            .filter(r => Number.isFinite(r.roomNumber));
        }
        setRooms(rms);

        // 강의실 선택값 보장
        if (typeof event?.roomNumber === "number") {
          setRoomNumber(event.roomNumber);
        } else if (rms.length && roomNumber === "") {
          // 기존이 비어있으면 첫 번째로 기본 세팅(선택 안 함 유지하고 싶으면 주석)
          // setRoomNumber(rms[0].roomNumber);
        }
      } catch (e: any) {
        setErr(e?.message ?? "데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, teacherId, academyNumber]);

  const valid = useMemo(() => {
    return (
      date &&
      classId &&
      /^\d{2}:\d{2}$/.test(startTime) &&
      /^\d{2}:\d{2}$/.test(endTime) &&
      startTime < endTime
    );
  }, [date, classId, startTime, endTime]);

  const save = async () => {
    if (!valid) {
      setErr("입력 값을 다시 확인하세요. (시간 형식 HH:MM, 종료 > 시작)");
      return;
    }
    await onSave({
      date,
      classId,
      title: title?.trim() ?? "",
      startTime,
      endTime,
      roomNumber: roomNumber === "" ? undefined : Number(roomNumber),
    });
    onClose();
  };

  const remove = async () => {
    await onDelete(event?.id);
    onClose();
  };

  if (!open || !event) return null;

  return (
    <div className="fixed inset-0 z-[240] bg-black/50 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl bg-white rounded-2xl border border-gray-300 p-5 space-y-4 text-black">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-black">스케줄 수정</h2>
          <button onClick={onClose} className="px-3 py-1.5 rounded border">닫기</button>
        </div>

        <div className="text-sm">날짜: <span className="font-medium">{date}</span></div>

        {/* 반 선택 */}
        <div>
          <label className="block text-sm mb-1 text-black">반</label>
          <select
            className="border rounded px-3 py-2 w-full text-black"
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
          >
            {classes.map(c => (
              <option key={c.classId} value={c.classId} className="text-black">
                {c.className ?? c.classId}
              </option>
            ))}
          </select>
        </div>

        {/* 강의실 선택(칩) */}
        <div>
          <label className="block text-sm mb-1 text-black">강의실</label>
          <div className="flex flex-wrap gap-2 border rounded-xl p-2">
            <button
              type="button"
              onClick={() => setRoomNumber("")}
              className={`px-3 py-1.5 rounded-full border ${
                roomNumber === "" ? "bg-black text-white" : "bg-white text-black"
              }`}
            >
              선택 안 함
            </button>
            {rooms.map((r) => {
              const active = roomNumber === r.roomNumber;
              const label = r.roomName ? `${r.roomName} (#${r.roomNumber})` : `Room ${r.roomNumber}`;
              return (
                <button
                  key={r.roomNumber}
                  type="button"
                  onClick={() => setRoomNumber(r.roomNumber)}
                  className={`px-3 py-1.5 rounded-full border ${
                    active ? "bg-black text-white" : "bg-white text-black"
                  }`}
                  title={label}
                >
                  {label}
                </button>
              );
            })}
          </div>
        </div>

        {/* 제목 */}
        <div>
          <label className="block text-sm mb-1 text-black">제목</label>
          <input
            className="border rounded px-3 py-2 w-full text-black"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="예: 국어"
          />
        </div>

        {/* 시간 */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm mb-1 text-black">시작</label>
            <input
              type="time"
              className="border rounded px-3 py-2 w-full text-black"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm mb-1 text-black">끝</label>
            <input
              type="time"
              className="border rounded px-3 py-2 w-full text-black"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
            />
          </div>
        </div>

        {err && <div className="text-red-600 text-sm">{err}</div>}

        <div className="flex gap-3">
          <button
            onClick={save}
            disabled={loading || !valid}
            className="flex-1 px-4 py-2 rounded bg-emerald-600 text-white disabled:opacity-50"
          >
            저장
          </button>
          <button
            onClick={remove}
            disabled={loading}
            className="flex-1 px-4 py-2 rounded bg-red-600 text-white"
          >
            삭제
          </button>
        </div>
      </div>
    </div>
  );
}
