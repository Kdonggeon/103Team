"use client";

import React from "react";
import VectorSeatEditor from "@/components/rooms/vector/VectorSeatEditor";
import type { VectorSeat } from "@/app/lib/rooms.vector";

export default function VectorEditorCard({
  seats,
  onChange,
}: {
  seats: VectorSeat[];
  onChange: (v: VectorSeat[]) => void;
}) {
  return (
    <div className="border rounded-xl p-3 bg-white">
      <VectorSeatEditor value={seats} onChange={onChange} />
      <div className="mt-2 text-xs text-gray-600">
        좌클릭 드래그: 이동 · 우클릭: 선택/해제 · Delete: 선택 좌석 통로처리(on/off)
      </div>
    </div>
  );
}
