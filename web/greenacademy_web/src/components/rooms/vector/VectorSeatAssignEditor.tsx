"use client";

import React, { useMemo, useRef } from "react";
import type { VectorSeat } from "@/app/lib/rooms.vector";

/** 겹침 판단 (여기선 드래그 없음이지만, 추후 확장 대비) */
const rectOverlap = (
  a: { x: number; y: number; w: number; h: number },
  b: { x: number; y: number; w: number; h: number }
) =>
  a.x < b.x + b.w &&
  a.x + a.w > b.x &&
  a.y < b.y + b.h &&
  a.y + a.h > b.y;

type Props = {
  /** 좌석 목록 (label, x,y,w,h,disabled, r 등) */
  value: VectorSeat[]; // studentId 필드는 부모 상태가 확장해서 갖고 있어도 됨 (as any)
  /** 부모가 상태를 들고 가는 완전 제어형 */
  onChange?: (next: VectorSeat[]) => void;
  /** 좌석 클릭 시 선택 인덱스 알림 */
  onSelect?: (index: number) => void;
  /** 우클릭(컨텍스트)으로 통로/비활성 토글 요청 */
  onToggleDisabled?: (index: number) => void;

  /** 렌더 크기 */
  width?: number;
  height?: number;
  /** 좌석 텍스트 렌더 옵션 */
  showSeatLabel?: boolean;
  showStudentId?: boolean;
};

export default function VectorSeatAssignEditor({
  value,
  onChange,
  onSelect,
  onToggleDisabled,
  width = 900,
  height = 560,
  showSeatLabel = true,
  showStudentId = true,
}: Props) {
  const svgRef = useRef<SVGSVGElement>(null);

  const toPx = (v: number, axis: "x" | "y") =>
    axis === "x" ? v * width : v * height;

  const grid = useMemo(() => {
    const lines: React.ReactElement[] = [];
    for (let i = 1; i < 10; i++) {
      const x = (i / 10) * width;
      const y = (i / 10) * height;
      lines.push(<line key={`v${i}`} x1={x} y1={0} x2={x} y2={height} stroke="#eef2f7" />);
      lines.push(<line key={`h${i}`} x1={0} y1={y} x2={width} y2={y} stroke="#eef2f7" />);
    }
    return lines;
  }, [width, height]);

  const handleSeatClick = (idx: number) => {
    onSelect?.(idx);
  };

  const handleSeatContext = (e: React.MouseEvent, idx: number) => {
    e.preventDefault();
    onToggleDisabled?.(idx);
  };

  return (
    <svg
      ref={svgRef}
      width={width}
      height={height}
      style={{ background: "#ffffff", border: "1px solid #11182720", borderRadius: 12 }}
    >
      {grid}

      {value.map((seat: any, idx) => {
        const px = toPx(seat.x, "x");
        const py = toPx(seat.y, "y");
        const pw = toPx(seat.w, "x");
        const ph = toPx(seat.h, "y");

        const fontSize = Math.max(11, Math.min(20, pw * 0.22));
        const seatLabel = String(seat.label ?? "");
        const studentId: string | null = seat.studentId ?? null;

        return (
          <g
            key={seat.id ?? idx}
            transform={`translate(${px},${py}) rotate(${seat.r ?? 0}, ${pw / 2}, ${ph / 2})`}
            onClick={() => handleSeatClick(idx)}
            onContextMenu={(e) => handleSeatContext(e, idx)}
            style={{ cursor: "pointer" }}
            data-seat-index={idx}
          >
            <rect
              width={pw}
              height={ph}
              rx={14}
              ry={14}
              fill={seat.disabled ? "#e5e7eb" : "#f8fafc"}
              stroke="#11182755"
              strokeWidth={1.25}
            />
            {showSeatLabel && (
              <text
                x={pw / 2}
                y={ph / 2 - (showStudentId ? 8 : 0)}
                textAnchor="middle"
                dominantBaseline={showStudentId ? "auto" : "middle"}
                fontSize={fontSize}
                fill="#111827"
                fontWeight={600}
              >
                {seatLabel}
              </text>
            )}
            {showStudentId && (
              <text
                x={pw / 2}
                y={showSeatLabel ? ph / 2 + 14 : ph / 2}
                textAnchor="middle"
                dominantBaseline={showSeatLabel ? "auto" : "middle"}
                fontSize={Math.max(10, fontSize - 2)}
                fill={studentId ? "#111827" : "#9ca3af"}
              >
                {studentId || "미배정"}
              </text>
            )}
          </g>
        );
      })}
    </svg>
  );
}
