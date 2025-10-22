"use client";
import { useEffect, useState } from "react";
import RoomGridEditor, { SeatCell } from "./RoomGridEditor";

// n × m 초기 그리드
function buildGrid(rows:number, cols:number): SeatCell[] {
  const a:SeatCell[] = [];
  let k = 1;
  for (let i=0;i<rows;i++) for (let j=0;j<cols;j++)
    a.push({ seatNumber: k++, row: i, col: j, disabled: false });
  return a;
}

type Props = {
  academyNumber?: number;
  // ✅ rows/cols 로 변경
  onCreate: (roomNumber:number, rows:number, cols:number, layout:SeatCell[]) => Promise<void>;
};

export default function CreateRoomForm({ academyNumber, onCreate }: Props) {
  const [roomNumber, setRoomNumber] = useState<number>(401);
  const [rows, setRows] = useState<number>(5);
  const [cols, setCols] = useState<number>(6);
  const [preview, setPreview] = useState<SeatCell[]>(buildGrid(5, 6));

  useEffect(()=>{ setPreview(buildGrid(rows, cols)); }, [rows, cols]);

  return (
    <section className="p-4 rounded-xl border">
      <h2 className="text-lg font-semibold mb-3">강의실 생성</h2>

      <div className="grid grid-cols-3 gap-3 max-w-2xl">
        <div>
          <label className="block text-sm text-black mb-1">방 번호</label>
          <input
            className="border border-black rounded p-2 w-full"
            type="number"
            value={roomNumber}
            onChange={(e)=>setRoomNumber(parseInt(e.target.value||""))}
          />
        </div>

        <div>
          <label className="block text-sm text-black mb-1">행 (rows)</label>
          <input
            className="border border-black rounded p-2 w-full"
            type="number" min={1} max={30}
            value={rows}
            onChange={(e)=>setRows(parseInt(e.target.value||"1"))}
          />
        </div>

        <div>
          <label className="block text-sm text-black mb-1">열 (cols)</label>
          <input
            className="border border-black rounded p-2 w-full"
            type="number" min={1} max={30}
            value={cols}
            onChange={(e)=>setCols(parseInt(e.target.value||"1"))}
          />
        </div>

        <div className="col-span-3 flex items-center gap-2">
          <button
            className="ml-auto px-4 py-2 rounded-2xl shadow bg-black text-white disabled:opacity-50"
            disabled={!academyNumber}
            onClick={()=> onCreate(roomNumber, rows, cols, preview)}
          >
            반 만들기
          </button>
        </div>
      </div>

      <div className="mt-4">
        <h3 className="text-sm text-black mb-2">초기 좌석 미리보기</h3>
        <RoomGridEditor rows={rows} cols={cols} value={preview} onChange={setPreview}/>
      </div>
    </section>
  );
}
