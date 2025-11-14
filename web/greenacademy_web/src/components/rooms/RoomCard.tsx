"use client";

import RoomGridEditor, { type SeatCell } from "@/components/rooms/RoomGridEditor";
import type { Room } from "@/app/lib/rooms";

type Props = {
  academyNumber: number;
  room: Room;
  onSaveLayout: (room: Room, layout: SeatCell[]) => Promise<void>;
  onDelete: (room: Room) => Promise<void>;
};

export default function RoomCard({ academyNumber, room, onSaveLayout, onDelete }: Props) {
  // 1) layout이 있으면 그걸 우선 사용
  const fromLayout = (room.layout ?? []) as SeatCell[];

  // 2) layout이 없으면 seats를 SeatCell로 변환해서 사용
  const fromSeats: SeatCell[] = (room.seats ?? []).map((s, idx) => ({
    seatNumber: s.seatNumber ?? idx + 1,
    row: s.row ?? 0,
    col: s.col ?? 0,
    disabled: s.disabled ?? false,
  }));

  const seats: SeatCell[] = fromLayout.length > 0 ? fromLayout : fromSeats;

  // 좌표에서 rows/cols 자동 계산 (좌석 없으면 기본값 5x6)
  const rows =
    room.rows ??
    (seats.length > 0 ? Math.max(...seats.map((s) => s.row), 0) + 1 : 5);

  const cols =
    room.cols ??
    (seats.length > 0 ? Math.max(...seats.map((s) => s.col), 0) + 1 : 6);

  return (
    <div className="relative border rounded-xl p-4">
      <button
        onClick={() => onDelete(room)}
        className="absolute -top-2 -right-2 w-8 h-8 rounded-full bg-gray-800 text-white text-sm"
      >
        ×
      </button>

      <div className="text-gray-500 mb-1">Room #{room.roomNumber}</div>
      <div className="text-sm mb-3">
        좌석 수: {seats.filter((s) => !s.disabled).length}
      </div>

      <RoomGridEditor
        rows={rows}
        cols={cols}
        value={seats}
        onChange={(v) => onSaveLayout(room, v)}
      />

      <div className="text-xs text-gray-500 mt-2">변경 즉시 저장</div>
    </div>
  );
}
