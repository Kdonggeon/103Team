"use client";

import React, { useEffect, useState } from "react";
import Panel from "@/components/ui/Panel";
import VectorSeatEditor from "@/components/rooms/vector/VectorSeatEditor";
import { generateAutoLayout } from "@/components/rooms/vector/generate";
import {
  roomsVectorApi,
  type VectorLayout,
  type AdminRoomLite,
} from "@/app/lib/rooms.vector";
import type { LoginResponse } from "@/app/lib/api";

/** 숫자만 받는 입력 */
function NumericText({
  label, value, onChange, className = "w-28", min = 1, placeholder,
}: {
  label: string;
  value: number;
  onChange: (n: number) => void;
  className?: string;
  min?: number;
  placeholder?: string;
}) {
  const [text, setText] = useState(String(value ?? ""));
  useEffect(() => setText(String(value ?? "")), [value]);

  const commit = () => {
    const only = text.replace(/[^\d]/g, "");
    if (!only) { setText(String(value ?? "")); return; }
    const n = Math.max(min, parseInt(only, 10));
    onChange(n);
    setText(String(n));
  };

  return (
    <div>
      <label className="block text-sm text-gray-900">{label}</label>
      <input
        type="text" inputMode="numeric" pattern="[0-9]*"
        value={text} placeholder={placeholder}
        onChange={(e) => setText(e.target.value)}
        onBlur={commit}
        onKeyDown={(e) => e.key === "Enter" && e.currentTarget.blur()}
        className={`border border-black text-gray-900 bg-white rounded px-2 py-1 ${className} appearance-none placeholder:text-gray-900`}
      />
    </div>
  );
}

export default function DirectorRoomsPanel({ user }: { user: NonNullable<LoginResponse> }) {
  const academyOptions = Array.isArray(user.academyNumbers) ? user.academyNumbers : [];
  const [academyNumber, setAcademyNumber] = useState<number | undefined>(academyOptions[0]);

  const [roomNumber, setRoomNumber] = useState<number>(401);
  const [seatCount, setSeatCount] = useState<number>(30);

  const [layout, setLayout] = useState<VectorLayout | null>(null);

  const [list, setList] = useState<AdminRoomLite[]>([]);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const extractVectorLayout = (raw: any) => {
    if (!raw || typeof raw !== "object") return {};
    const candidate =
      raw.seats?.length ? raw :
      raw.layout?.seats?.length ? raw.layout :
      raw.data?.seats?.length ? raw.data :
      raw.vector?.seats?.length ? raw.vector :
      raw.vectorSeats?.length ? { seats: raw.vectorSeats } : {};
    const seats = candidate?.seats && Array.isArray(candidate.seats) ? candidate.seats : undefined;
    const canvasW = candidate.canvasW ?? raw.canvasW ?? raw.layout?.canvasW ?? raw.data?.canvasW;
    const canvasH = candidate.canvasH ?? raw.canvasH ?? raw.layout?.canvasH ?? raw.data?.canvasH;
    const version = candidate.version ?? raw.version ?? raw.layout?.version ?? raw.data?.version ?? 1;
    return { seats, canvasW, canvasH, version };
  };

  const loadList = async () => {
    if (!academyNumber) return;
    try {
      const rows = await roomsVectorApi.list(academyNumber);
      setList(rows ?? []);
      setErr(null);
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const loadOne = async (rn = roomNumber) => {
    if (!academyNumber) return;
    setLoading(true); setErr(null); setMsg(null);
    try {
      const v = await roomsVectorApi.get(rn, academyNumber);
      const parsed: any = extractVectorLayout(v);
      if (parsed.seats && parsed.seats.length > 0) {
        setLayout({
          version: parsed.version ?? 1,
          canvasW: parsed.canvasW ?? 1,
          canvasH: parsed.canvasH ?? 1,
          seats: parsed.seats,
        });
      } else {
        setLayout(generateAutoLayout(seatCount));
        setMsg("저장된 좌석이 없어 초기 배치를 생성했습니다.");
      }
    } catch {
      setLayout(generateAutoLayout(seatCount));
      setMsg("불러오기 실패로 초기 배치를 생성했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ 저장 후 항상 강의실 목록 새로고침 되도록 정리
  const save = async () => {
    if (!academyNumber || !layout) return;
    setErr(null); setMsg(null);
    try {
      try {
        // 기본 포맷
        await roomsVectorApi.put(roomNumber, academyNumber, layout);
        setMsg("저장 완료");
      } catch (e1: any) {
        // 호환 포맷 (백엔드가 layout 래핑 버전만 받는 경우)
        await (roomsVectorApi as any).put(roomNumber, academyNumber, { layout });
        setMsg("저장 완료(호환 포맷)");
      }
      // ✅ 저장이 성공한 경우에만 목록 새로고침
      await loadList();
    } catch (e2: any) {
      setErr(e2?.message ?? "저장 실패");
    }
  };

  const remove = async (targetRoom?: number) => {
    if (!academyNumber) return;
    const rn = targetRoom ?? roomNumber;
    if (!confirm(`Room ${rn} 삭제할까요?`)) return;
    setErr(null); setMsg(null);
    try {
      await roomsVectorApi.delete(rn, academyNumber);
      if (rn === roomNumber) setLayout(generateAutoLayout(seatCount));
      await loadList();
      setMsg("삭제 완료");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const regenerate = () => {
    setLayout(generateAutoLayout(seatCount));
    setMsg("초기 배치를 새로 생성했습니다.");
  };

  const pickRoom = async (rn: number) => {
    setRoomNumber(rn);
    await loadOne(rn);
  };

  useEffect(() => { loadList(); }, [academyNumber]);
  useEffect(() => { loadOne(roomNumber); }, [academyNumber, roomNumber]);

  return (
    <Panel
      title="강의실 관리"
      right={
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-900">학원번호</label>
          <select
            className="border border-black rounded px-2 py-1 text-gray-900 bg-white"
            value={academyNumber ?? ""}
            onChange={(e) => setAcademyNumber(parseInt(e.target.value))}
          >
            {academyOptions.map(n => <option key={n} value={n} className="text-gray-900">{n}</option>)}
          </select>
        </div>
      }
    >
      <div className="space-y-3">
        {/* 툴바 */}
        <div className="flex flex-wrap items-end gap-3">
          <NumericText label="방 번호" value={roomNumber} onChange={setRoomNumber} placeholder="예: 401" />
          <NumericText label="좌석 개수" value={seatCount} onChange={setSeatCount} placeholder="예: 30" />
          <div className="ml-auto flex gap-2">
            <button onClick={regenerate} className="bg-gray-900 text-white px-4 py-2 rounded">배치 생성</button>
            <button onClick={save} className="bg-emerald-600 text-white px-4 py-2 rounded">저장</button>
            <button onClick={() => remove()} className="bg-red-600 text-white px-4 py-2 rounded">삭제</button>
          </div>
        </div>

        {/* 메시지 */}
        {loading && <div className="text-sm text-gray-900">불러오는 중…</div>}
        {err && <div className="text-sm text-red-600">오류: {err}</div>}
        {msg && <div className="text-sm text-emerald-600">{msg}</div>}

        {/* 좌석 에디터 */}
        {layout && (
          <div className="border border-black rounded-xl p-3 bg-white">
            <VectorSeatEditor
              value={layout.seats}
              onChange={(seats) => setLayout(prev => (prev ? { ...prev, seats } : null))}
            />
          </div>
        )}

        {/* 강의실 목록 */}
        <div className="rounded-xl border border-black bg-white">
          <div className="px-3 py-2 font-semibold flex items-center justify-between text-gray-900">
            <span>강의실 목록</span>
            <button
              onClick={loadList}
              className="text-sm px-3 py-1.5 rounded border border-black hover:bg-gray-100"
            >
              새로고침
            </button>
          </div>

          {list.length === 0 ? (
            <div className="px-3 pb-3 text-sm text-gray-900">등록된 강의실이 없습니다.</div>
          ) : (
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-2 p-3">
              {list.map(r => {
                const active = r.roomNumber === roomNumber;
                return (
                  <div
                    key={`${r.academyNumber}-${r.roomNumber}`}
                    className={`border border-black rounded-lg p-3 ${active ? "ring-2 ring-emerald-400" : ""}`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="font-semibold text-gray-900">Room {r.roomNumber}</div>
                      <div className="text-xs text-gray-900">
                        {r.hasVector ? `좌석 ${r.vectorSeatCount}개` : "미설정"}
                      </div>
                    </div>
                    <div className="mt-2 flex gap-2">
                      <button
                        className="px-3 py-1.5 rounded bg-gray-900 text-white text-sm"
                        onClick={() => pickRoom(r.roomNumber)}
                      >
                        불러오기
                      </button>
                      <button
                        className="px-3 py-1.5 rounded bg-red-600 text-white text-sm"
                        onClick={() => remove(r.roomNumber)}
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </Panel>
  );
}
