"use client";
import RoomGridEditor, { SeatCell } from "@/components/rooms/RoomGridEditor";
import type { Room } from "@/app/lib/rooms";

type Props = {
  academyNumber: number;
  room: Room;
  onSaveLayout: (room: Room, layout: SeatCell[]) => Promise<void>;
  onDelete: (room: Room) => Promise<void>;
};

export default function RoomCard({ academyNumber, room, onSaveLayout, onDelete }: Props) {
  const seats = room.layout ?? [];
  const rows = room.rows ?? Math.max(...seats.map(s=>s.row), 0) + 1;
  const cols = room.cols ?? Math.max(...seats.map(s=>s.col), 0) + 1;

  return (
    <div className="relative border rounded-xl p-4">
      <button onClick={()=>onDelete(room)}
        className="absolute -top-2 -right-2 w-8 h-8 rounded-full bg-gray-800 text-white text-sm">×</button>

      <div className="text-gray-500 mb-1">Room #{room.roomNumber}</div>
      <div className="text-sm mb-3">
        좌석 수: {seats.filter(s=>!s.disabled).length}
      </div>

      <RoomGridEditor
        rows={rows} cols={cols}
        value={seats}
        onChange={(v)=>onSaveLayout(room, v)}
      />
      <div className="text-xs text-gray-500 mt-2">변경 즉시 저장</div>
    </div>
  );
}
