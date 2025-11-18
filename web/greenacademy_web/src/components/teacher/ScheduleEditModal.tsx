"use client";

import React, { useEffect, useMemo, useState } from "react";
import api from "@/app/lib/api";
import { roomsApi } from "@/app/lib/rooms";

type EditEvent = {
  id?: string;
  date: string;
  classId: string;
  title?: string;
  startTime?: string;
  endTime?: string;
  roomNumber?: number | "";
};

export type ScheduleEditModalProps = {
  open: boolean;
  onClose: () => void;
  event: EditEvent | null;
  onSave: (patch: {
    date: string;
    classId: string;
    title: string;
    startTime: string;
    endTime: string;
    roomNumber?: number;
  }) => Promise<void> | void;

  onDelete: (scheduleId?: string) => Promise<void> | void;

  teacherId: string;
  academyNumber?: number | string | null;
};

type MyClassLite = { classId: string; className?: string };
type MyRoomLite = { roomNumber: number; roomName?: string };

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

  const [date, setDate] = useState(event?.date ?? "");
  const [classId, setClassId] = useState(event?.classId ?? "");
  const [title, setTitle] = useState(event?.title ?? "");
  const [startTime, setStartTime] = useState(event?.startTime ?? "10:00");
  const [endTime, setEndTime] = useState(event?.endTime ?? "11:00");
  const [roomNumber, setRoomNumber] = useState<number | "">(event?.roomNumber ?? "");

  /* 🔄 event 변경 시 form 동기화 */
  useEffect(() => {
    if (!open) return;
    setDate(event?.date ?? "");
    setClassId(event?.classId ?? "");
    setTitle(event?.title ?? "");
    setStartTime(event?.startTime ?? "10:00");
    setEndTime(event?.endTime ?? "11:00");
    setRoomNumber(event?.roomNumber ?? "");
  }, [open, event]);

  /* 📌 데이터 전체 로드 */
  useEffect(() => {
    if (!open) return;

    (async () => {
      try {
        setLoading(true);
        setErr(null);

        /* =============================
         * 1) 반 목록 불러오기
         * ============================= */
        const list = await api.listMyClasses(teacherId);
        let cls: MyClassLite[] = (list ?? []).map((c: any) => ({
          classId: c.classId,
          className: c.className ?? c.classId,
        }));

        // 스케줄의 classId가 목록에 없으면 강제로라도 표시
        if (event?.classId && !cls.some((c) => c.classId === event.classId)) {
          cls = [{ classId: event.classId, className: event.classId }, ...cls];
        }
        setClasses(cls);

        if (!classId && cls.length > 0) {
          setClassId(cls[0].classId);
        }

        /* =============================
         * 2) 강의실 목록:
         *    → 해당 반(Class)의 roomNumbers만 표시
         * ============================= */
        if (academyNumber != null && event?.classId) {
          // 2-1) 반 상세에서 roomNumbers 가져오기
          const detail = await api.getClassDetail(event.classId);
          const usedRooms =
            Array.isArray(detail.roomNumbers) && detail.roomNumbers.length > 0
              ? detail.roomNumbers
              : detail.roomNumber != null
              ? [detail.roomNumber]
              : [];

          // 2-2) 전체 방 목록
          const rawRooms = await roomsApi.listRooms(Number(academyNumber));
          const allRooms: MyRoomLite[] = (Array.isArray(rawRooms) ? rawRooms : [])
            .map((r: any) => ({
              roomNumber: Number(r.roomNumber ?? r.Room_Number ?? r.number),
              roomName: r.roomName ?? r.name ?? undefined,
            }))
            .filter((r) => Number.isFinite(r.roomNumber));

          // 2-3) 반이 사용하는 방만 필터
          const filtered: MyRoomLite[] = usedRooms
            .map((roomNo) => {
              const info = allRooms.find((r) => r.roomNumber === roomNo);
              return { roomNumber: roomNo, roomName: info?.roomName };
            })
            .filter((r) => Number.isFinite(r.roomNumber));

          setRooms(filtered);

          // 선택값 보장
          if (
            typeof event.roomNumber === "number" &&
            filtered.some((r) => r.roomNumber === event.roomNumber)
          ) {
            setRoomNumber(event.roomNumber);
          } else if (filtered.length > 0 && roomNumber === "") {
            // 원하는 경우 자동 선택 가능
            // setRoomNumber(filtered[0].roomNumber);
          }
        } else {
          setRooms([]);
        }
      } catch (e: any) {
        console.error(e);
        setErr(e?.message ?? "데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, teacherId, academyNumber]);

  /* =============================
   * 유효성 검사
   * ============================= */
  const valid = useMemo(() => {
    return (
      date &&
      classId &&
      /^\d{2}:\d{2}$/.test(startTime) &&
      /^\d{2}:\d{2}$/.test(endTime) &&
      startTime < endTime
    );
  }, [date, classId, startTime, endTime]);

  /* =============================
   * 저장
   * ============================= */
  const save = async () => {
    if (!valid) {
      setErr("입력 값을 확인하세요. (시간 형식 / 종료>시작)");
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

  /* =============================
   * 삭제
   * ============================= */
  const remove = async () => {
    await onDelete(event?.id);
    onClose();
  };

  if (!open || !event) return null;

  return (
    <div className="fixed inset-0 z-[240] bg-black/50 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl bg-white rounded-2xl border border-gray-300 p-5 space-y-4 text-black">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">스케줄 수정</h2>
          <button onClick={onClose} className="px-3 py-1.5 rounded border">닫기</button>
        </div>

        <div className="text-sm">날짜: <span className="font-medium">{date}</span></div>

        {/* 반 선택 */}
        <div>
          <label className="block text-sm mb-1">반</label>
          <select
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
            className="border rounded px-3 py-2 w-full"
          >
            {classes.map((c) => (
              <option key={c.classId} value={c.classId}>
                {c.className ?? c.classId}
              </option>
            ))}
          </select>
        </div>

        {/* 강의실 선택 (반에서 사용하는 방만 표시) */}
        <div>
          <label className="block text-sm mb-1">강의실</label>
          <div className="flex flex-wrap gap-2 border rounded-xl p-2">
            <button
              onClick={() => setRoomNumber("")}
              className={`px-3 py-1.5 rounded-full border ${
                roomNumber === "" ? "bg-black text-white" : "bg-white"
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
                  onClick={() => setRoomNumber(r.roomNumber)}
                  className={`px-3 py-1.5 rounded-full border ${
                    active ? "bg-black text-white" : "bg-white"
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>
        </div>

        {/* 제목 */}
        <div>
          <label className="block text-sm mb-1">제목</label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="border rounded px-3 py-2 w-full"
            placeholder="예: 국어"
          />
        </div>

        {/* 시간 */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm mb-1">시작</label>
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="border rounded px-3 py-2 w-full"
            />
          </div>
          <div>
            <label className="block text-sm mb-1">끝</label>
            <input
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className="border rounded px-3 py-2 w-full"
            />
          </div>
        </div>

        {err && <div className="text-red-600 text-sm">{err}</div>}

        <div className="flex gap-3">
          <button
            onClick={save}
            disabled={loading || !valid}
            className="flex-1 px-4 py-2 rounded bg-emerald-600 text-white"
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
