// src/app/hooks/useRoomsVector.ts
import { useState } from "react";
import { roomsVectorApi, type VectorLayout, type VectorSeat } from "@/app/lib/rooms.vector";
import { generateAutoLayout } from "@/components/rooms/vector/generate";

export function useRoomsVector(params: {
  academyNumber?: number;
  roomNumber: number;
  seatCount: number;
}) {
  const { academyNumber, roomNumber, seatCount } = params;

  const [layout, setLayout] = useState<VectorLayout | null>(null);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const guard = () => {
    if (!academyNumber) { setErr("학원번호를 선택하세요."); return false; }
    return true;
  };

  const load = async () => {
    if (!guard()) return;
    setLoading(true); setErr(null); setMsg(null);
    try {
      const v = await roomsVectorApi.get(roomNumber, academyNumber!);
      setLayout(v && v.seats?.length ? v : generateAutoLayout(seatCount));
    } catch (e:any) {
      // 없으면 자동 생성
      setLayout(generateAutoLayout(seatCount));
    } finally { setLoading(false); }
  };

  const save = async () => {
    if (!guard() || !layout) return;
    setErr(null); setMsg(null);
    try {
      await roomsVectorApi.put(roomNumber, academyNumber!, layout);
      setMsg("저장 완료");
    } catch (e:any) { setErr(e.message); }
  };

  const remove = async () => {
    if (!guard()) return;
    if (!confirm(`Room #${roomNumber} 삭제할까요?`)) return;
    setErr(null); setMsg(null);
    try {
      await roomsVectorApi.delete(roomNumber, academyNumber!);
      setLayout(generateAutoLayout(seatCount));
      setMsg("삭제 완료");
    } catch (e:any) { setErr(e.message); }
  };

  const regenerate = () => {
    setLayout(generateAutoLayout(seatCount));
    setMsg("초기 배치를 새로 생성했습니다.");
  };

  const setSeats = (seats: VectorSeat[]) =>
    setLayout(prev => (prev ? { ...prev, seats } : prev));

  return { layout, loading, msg, err, load, save, remove, regenerate, setSeats };
}
