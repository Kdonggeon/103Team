import type { VectorLayout, VectorSeat } from "@/app/lib/rooms.vector";

/** 좌석 개수만으로 가로형 카드 비율을 유지해 자동 배치 */
export function generateAutoLayout(count: number): VectorLayout {
  const W = 1, H = 1;
  const PAD = 0.04;         // 바깥 여백
  const GUT = 0.02;         // 좌석 간격
  const CARD_W = 1.0;
  const CARD_H = 0.62;      // 가로가 더 긴 카드(비율)
  const ASPECT = CARD_H / CARD_W;

  const MIN_W = 0.08;       // 카드 최소 폭
  const MAX_W = 0.20;       // 카드 최대 폭

  // √N 기반 열 수 추정 → 행 계산
  let cols = Math.max(1, Math.round(Math.sqrt(count)));
  let rows = Math.ceil(count / cols);

  const innerW = W - PAD * 2;
  const innerH = H - PAD * 2;

  // 셀 크기
  const cellW = (innerW - GUT * (cols - 1)) / cols;
  const cellH = (innerH - GUT * (rows - 1)) / rows;

  // 카드 폭 후보: 세로 셀의 높이를 비율로 환산한 폭과 가로 셀 폭 중 작은 값
  const fitW = Math.min(cellW, cellH / ASPECT);
  const boxW = Math.min(MAX_W, Math.max(MIN_W, fitW * 0.9));
  const boxH = boxW * ASPECT;

  // 중앙 정렬
  const gridW = cols * boxW + (cols - 1) * GUT;
  const gridH = rows * boxH + (rows - 1) * GUT;
  const startX = (W - gridW) / 2;
  const startY = (H - gridH) / 2;

  const seats: VectorSeat[] = [];
  for (let i = 0; i < count; i++) {
    const r = Math.floor(i / cols);
    const c = i % cols;
    seats.push({
      id: (globalThis.crypto ?? (require("crypto") as any).webcrypto).randomUUID(),
      label: String(i + 1),
      x: startX + c * (boxW + GUT),
      y: startY + r * (boxH + GUT),
      w: boxW,
      h: boxH,  // ← 비율 고정
    });
  }

  return { version: 1, canvasW: W, canvasH: H, seats };
}
