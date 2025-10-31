"use client";

import React, { useMemo, useRef, useState } from "react";
import type { VectorSeat } from "@/app/lib/rooms.vector";

const rectOverlap = (
  a: { x: number; y: number; w: number; h: number },
  b: { x: number; y: number; w: number; h: number },
  pad = 0
) =>
  a.x < b.x + b.w - pad &&
  a.x + a.w - pad > b.x &&
  a.y < b.y + b.h - pad &&
  a.y + a.h - pad > b.y;

type Props = {
  value: VectorSeat[];
  onChange: (v: VectorSeat[]) => void;
  width?: number;
  height?: number;
};

export default function VectorSeatEditor({
  value,
  onChange,
  width = 840,
  height = 520,
}: Props) {
  const svgRef = useRef<SVGSVGElement>(null);
  const [dragId, setDragId] = useState<string | null>(null);
  const [dragOff, setDragOff] = useState<{ dx: number; dy: number }>({ dx: 0, dy: 0 });
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const toPx = (v: number, axis: "x" | "y") => (axis === "x" ? v * width : v * height);
  const fromPx = (px: number, axis: "x" | "y") => (axis === "x" ? px / width : px / height);

  // 우클릭: 선택/해제
  const onSeatContext = (e: React.MouseEvent, id: string) => {
    e.preventDefault();
    setSelected((prev) => {
      const ns = new Set(prev);
      ns.has(id) ? ns.delete(id) : ns.add(id);
      return ns;
    });
  };

  // 좌클릭 드래그 시작
  const onSeatMouseDown = (e: React.MouseEvent, seat: VectorSeat) => {
    if (!svgRef.current || e.button !== 0) return;
    const box = svgRef.current.getBoundingClientRect();
    const mx = e.clientX - box.left;
    const my = e.clientY - box.top;
    setDragId(seat.id);
    setDragOff({ dx: mx - toPx(seat.x, "x"), dy: my - toPx(seat.y, "y") });
  };

  // 드래그 이동(모양 고정 + 겹침 방지 + 1% 스냅)
  const onMouseMove = (e: React.MouseEvent) => {
    if (!dragId || !svgRef.current) return;
    const box = svgRef.current.getBoundingClientRect();
    const mx = e.clientX - box.left;
    const my = e.clientY - box.top;

    const next = value.map((s) => {
      if (s.id !== dragId) return s;
      const nx = fromPx(mx - dragOff.dx, "x");
      const ny = fromPx(my - dragOff.dy, "y");
      const snap = (t: number) => Math.round(t / 0.01) * 0.01;
      return {
        ...s,
        x: Math.max(0, Math.min(1 - s.w, snap(nx))),
        y: Math.max(0, Math.min(1 - s.h, snap(ny))),
      };
    });

    const me = next.find((s) => s.id === dragId)!;
    const overlap = next.some((o) => o.id !== dragId && rectOverlap(me, o, 0.002));
    if (!overlap) onChange(next);
  };

  const onMouseUp = () => setDragId(null);

  // Delete → 통로처리 토글
  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Delete" || e.key === "Backspace") {
      if (selected.size === 0) return;
      e.preventDefault();
      const ids = new Set(selected);
      onChange(value.map((s) => (ids.has(s.id) ? { ...s, disabled: !s.disabled } : s)));
    }
  };

  // 보조 그리드
  const gridLines = useMemo(() => {
    const lines: React.ReactElement[] = [];
    for (let i = 1; i < 10; i++) {
      const x = (i / 10) * width;
      const y = (i / 10) * height;
      lines.push(<line key={`v${i}`} x1={x} y1={0} x2={x} y2={height} stroke="#eef2f7" />);
      lines.push(<line key={`h${i}`} x1={0} y1={y} x2={width} y2={y} stroke="#eef2f7" />);
    }
    return lines;
  }, [width, height]);

  return (
    <div tabIndex={0} onKeyDown={onKeyDown} className="outline-none">
      <svg
        ref={svgRef}
        width={width}
        height={height}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        style={{ background: "#f8fafc", border: "1px solid #e5e7eb", borderRadius: 12 }}
      >
        {gridLines}
        {value.map((seat) => {
          const px = toPx(seat.x, "x"), py = toPx(seat.y, "y");
          const pw = toPx(seat.w, "x"), ph = toPx(seat.h, "y");
          const isSel = selected.has(seat.id);
          const fontSize = Math.max(12, Math.min(22, pw * 0.22));

          return (
            <g
              key={seat.id}
              transform={`translate(${px},${py}) rotate(${seat.r ?? 0}, ${pw / 2}, ${ph / 2})`}
              onMouseDown={(e) => onSeatMouseDown(e, seat)}
              onContextMenu={(e) => onSeatContext(e, seat.id)}
              style={{ cursor: "move" }}
            >
              <rect
                width={pw}
                height={ph}
                rx={14}
                ry={14}
                fill={seat.disabled ? "#e5e7eb" : "#ffffff"}
                stroke={isSel ? "#16a34a" : "#94a3b8"}
                strokeWidth={isSel ? 3 : 1.25}
              />
              <text
                x={pw / 2}
                y={ph / 2}
                textAnchor="middle"
                dominantBaseline="middle"
                fontSize={fontSize}
                fill="#111827"
              >
                {seat.label}
              </text>
            </g>
          );
        })}
      </svg>
      <div className="mt-2 text-xs text-gray-600">
        좌클릭 드래그: 이동 · 우클릭: 선택/해제 · Delete: 선택 좌석 통로처리(on/off)
      </div>
    </div>
  );
}
