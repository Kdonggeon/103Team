// C:\project\103Team-sub\web\greenacademy_web\src\app\teacher\QRGeneratorPanel.tsx
"use client";

import { useEffect, useMemo, useState } from "react";
import type { LoginResponse } from "@/app/lib/api";
import { roomsApi, type Room } from "@/app/lib/rooms";

/* =============================================
   상수: PNG/미리보기는 고정, 인쇄(mm)만 변경 가능
============================================= */
const QR_PNG_PX = 320;     // QR 원본 PNG 해상도(고정)
const PREVIEW_PX = 120;    // 화면 미리보기 크기(고정)

/* =============================================
   유틸
============================================= */

// 숫자 3자리 패딩(강의실 번호 표기용: 1 → "001")
const pad3 = (n: number) => String(n).padStart(3, "0");

// 모바일 파서가 바로 읽을 수 있는 쿼리스트링 페이로드 생성
// 형식: v=1&type=seat&academyNumber=103&room=403&seat=12
function buildQrPayloadQS(academyNumber: number, roomNumber: number, seatNumber: number): string {
  const qs = new URLSearchParams({
    v: "1",
    type: "seat",
    academyNumber: String(academyNumber),
    room: String(roomNumber),
    seat: String(seatNumber), // 1..N
  });
  return qs.toString();
}

// 외부 QR PNG 생성 서비스 (프로토타입용) — PNG 해상도는 고정
function makeQrUrlFromQS(payloadQS: string): string {
  return `https://api.qrserver.com/v1/create-qr-code/?size=${QR_PNG_PX}x${QR_PNG_PX}&data=${encodeURIComponent(payloadQS)}`;
}

// 새 창으로 인쇄 — 인쇄할 때의 물리 크기(mm)만 반영
function printQrCards(
  items: Array<{ roomNumber: number; seatNumber: number; url: string }>,
  academyNumber: number | undefined,
  printSizeMm: number
) {
  if (!items.length) return;
  const uniqueRooms = Array.from(new Set(items.map((i) => i.roomNumber))).sort((a, b) => a - b);
  const title =
    uniqueRooms.length === 1
      ? `좌석 QR - Academy ${academyNumber ?? "-"} / Room ${uniqueRooms[0]}`
      : `좌석 QR - Academy ${academyNumber ?? "-"} / Room 전체(${uniqueRooms.length}개)`;

  const safeSize = Math.max(10, Math.min(120, Math.round(printSizeMm))); // 10~120mm 가드
  const html = `<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<title>${title}</title>
<style>
  @page { size: A4; margin: 10mm; }
  body { font-family: system-ui, -apple-system, Segoe UI, Roboto, "Noto Sans KR", Helvetica, Arial, sans-serif; }
  .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
  .card { border: 1px solid #ddd; border-radius: 10px; padding: 10px; text-align: center; }
  .title { font-weight: 700; margin: 4px 0 8px; }
  /* 인쇄 물리 크기(mm) 고정 */
  img { width: ${safeSize}mm; height: ${safeSize}mm; object-fit: contain; }
  .meta { font-size: 12px; color: #555; margin-top: 6px; }
</style>
</head>
<body>
  <h3>${title}</h3>
  <div class="grid">
    ${items
      .map(
        (it) => `
      <div class="card">
        <div class="title">${pad3(it.roomNumber)} 강의실 ${it.seatNumber}번 좌석</div>
        <img src="${it.url}" />
        <div class="meta">Printed ${new Date().toLocaleString()}</div>
      </div>`
      )
      .join("")}
  </div>
  <script>window.onload = () => setTimeout(() => window.print(), 200);</script>
</body>
</html>`;
  const w = window.open("", "_blank");
  if (!w) return;
  w.document.open();
  w.document.write(html);
  w.document.close();
}

/* =============================================
   좌석 추출 로직
   - 1순위: layout(row/col 있는 정규 스키마)
   - 2순위: vectorLayout 길이(좌석 수) 기반 생성
   - 3순위: rows*cols 기반 생성
============================================= */

type SeatCell = { seatNumber: number; row?: number; col?: number; disabled?: boolean };

const fromGridLayout = (v: any[] | undefined, rows?: number, cols?: number): SeatCell[] => {
  const arr = (v || []).map((s: any, i: number) => ({
    seatNumber: Number(s?.seatNumber ?? i + 1),
    row: (Number(s?.row) || 1) - 1,
    col: (Number(s?.col) || 1) - 1,
    disabled: !!s?.disabled,
  })) as SeatCell[];

  if (arr.length === 0 && rows && cols) {
    let k = 1;
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        arr.push({ seatNumber: k++, row: r, col: c, disabled: false });
      }
    }
  }
  arr.sort((a, b) => a.seatNumber - b.seatNumber);
  return arr;
};

const fromVectorLayout = (vectorLayout: any[] | undefined): SeatCell[] => {
  const n = Array.isArray(vectorLayout) ? vectorLayout.length : 0;
  if (!n) return [];
  return Array.from({ length: n }, (_, i) => ({ seatNumber: i + 1, disabled: false }));
};

function seatsFromRoom(room: any): SeatCell[] {
  // 1) layout(row/col)
  const grid = fromGridLayout(room?.layout, room?.rows ?? room?.Rows, room?.cols ?? room?.Cols);
  if (grid.length > 0) return grid;

  // 2) vectorLayout 길이 기반
  const vec = fromVectorLayout(room?.vectorLayout);
  if (vec.length > 0) return vec;

  // 3) rows*cols만 있을 때
  return fromGridLayout([], room?.rows ?? room?.Rows, room?.cols ?? room?.Cols);
}
type Props = { user: NonNullable<LoginResponse> };

type SeatItem = { roomNumber: number; seatNumber: number; disabled?: boolean };

export default function QRGeneratorPanel({ user }: Props) {
  // 학원 선택(문자/숫자 혼합 대비 정규화)
  const academyOptions = (user.academyNumbers || [])
    .map((n: any) => Number(n))
    .filter((n) => Number.isFinite(n)) as number[];

  const [academyNumber, setAcademyNumber] = useState<number | undefined>(academyOptions[0]);

  // 룸 목록 / 현재 룸 (0 = 전체)
  const [rooms, setRooms] = useState<Room[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [roomsErr, setRoomsErr] = useState<string | null>(null);
  const [roomNumber, setRoomNumber] = useState<number | undefined>(undefined);

  const isAllRooms = roomNumber === 0;
  const currentRoom = useMemo(
    () =>
      typeof roomNumber === "number" && roomNumber > 0
        ? rooms.find((r: any) => Number(r?.roomNumber) === Number(roomNumber))
        : undefined,
    [rooms, roomNumber]
  );

  // 좌석 목록(현재 선택된 모드에 따라 단일/전체)
  const allSeats: SeatItem[] = useMemo(() => {
    if (!rooms.length) return [];
    if (isAllRooms) {
      // 전체: 모든 방의 좌석을 합쳐서
      const out: SeatItem[] = [];
      for (const r of rooms) {
        const seats = seatsFromRoom(r);
        for (const s of seats) {
          out.push({ roomNumber: Number(r.roomNumber), seatNumber: s.seatNumber, disabled: s.disabled });
        }
      }
      // 방번호, 좌석번호 기준 정렬
      out.sort((a, b) => (a.roomNumber - b.roomNumber) || (a.seatNumber - b.seatNumber));
      return out;
    }
    // 단일 방
    if (!currentRoom) return [];
    const seats = seatsFromRoom(currentRoom);
    return seats.map((s) => ({
      roomNumber: Number(currentRoom.roomNumber),
      seatNumber: s.seatNumber,
      disabled: s.disabled,
    }));
  }, [rooms, currentRoom, isAllRooms]);

  // 선택 상태: "room-seat" 키로 관리
  const keyOf = (roomNumber: number, seatNumber: number) => `${roomNumber}-${seatNumber}`;
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const toggleSeat = (k: string) =>
    setSelectedKeys((prev) => (prev.includes(k) ? prev.filter((x) => x !== k) : [...prev, k]));
  const selectAll = () => setSelectedKeys(allSeats.map((s) => keyOf(s.roomNumber, s.seatNumber)));
  const clearAll = () => setSelectedKeys([]);

  // 생성된 좌석별 QR 결과
  const [qrList, setQrList] = useState<Array<{ roomNumber: number; seatNumber: number; url: string }>>([]);

  // 인쇄 크기(mm)만 사용자가 바꿀 수 있음
  const [printSizeMm, setPrintSizeMm] = useState<number>(30);

  // 룸 불러오기
  useEffect(() => {
    const load = async () => {
      if (!academyNumber) {
        setRooms([]);
        setRoomNumber(undefined);
        return;
      }
      setLoadingRooms(true);
      setRoomsErr(null);
      try {
        const list = await roomsApi.listRooms(academyNumber);
        const sorted = (list || []).slice().sort((a: any, b: any) => Number(a.roomNumber) - Number(b.roomNumber));
        setRooms(sorted);
        // 최초 진입 시 첫 룸 자동 선택(전체가 기본이길 원하면 0으로 바꿔도 됨)
        setRoomNumber((prev) => (prev !== undefined ? prev : (sorted[0]?.roomNumber as number | undefined)));
      } catch (e: any) {
        setRoomsErr(e?.message || "강의실 목록을 불러오지 못했습니다.");
      } finally {
        setLoadingRooms(false);
      }
    };
    load();
  }, [academyNumber]);

  // 룸/학원 변경 시 선택/결과 초기화
  useEffect(() => {
    setSelectedKeys([]);
    setQrList([]);
  }, [roomNumber, academyNumber]);

  // 좌석 QR 일괄 생성 (PNG 해상도 고정)
  const generateSeatQrs = () => {
    if (!academyNumber) return alert("학원번호를 선택하세요.");
    if (roomNumber === undefined) return alert("강의실을 선택하세요."); // 0(전체)은 허용
    if (selectedKeys.length === 0) return alert("좌석을 하나 이상 선택하세요.");

    // lookup map
    const map = new Map(allSeats.map((s) => [keyOf(s.roomNumber, s.seatNumber), s]));

    const out = selectedKeys
      .map((k) => map.get(k))
      .filter(Boolean)
      .map((s) => {
        const qs = buildQrPayloadQS(academyNumber, s!.roomNumber, s!.seatNumber);
        return { roomNumber: s!.roomNumber, seatNumber: s!.seatNumber, url: makeQrUrlFromQS(qs) };
      })
      // 방→좌석 정렬
      .sort((a, b) => (a.roomNumber - b.roomNumber) || (a.seatNumber - b.seatNumber));

    setQrList(out);
  };

  // 단일 인쇄 — 인쇄(mm)만 반영
  const printOne = (item: { roomNumber: number; seatNumber: number; url: string }) =>
    printQrCards([item], academyNumber, printSizeMm);

  // 모두 인쇄 — 인쇄(mm)만 반영
  const printAll = () => {
    if (!qrList.length) return alert("먼저 QR 이미지를 생성하세요.");
    printQrCards(qrList, academyNumber, printSizeMm);
  };

  /* =============================================
     UI
  ============================================= */

  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex flex-wrap items-center gap-3">
        <h2 className="text-lg font-semibold text-gray-900">좌석 QR 생성</h2>

        {/* 학원 선택 */}
        <div className="ml-auto flex items-center gap-2">
          <label className="text-sm text-black">학원번호</label>
          <select
            className="border border-black rounded px-2 py-1 text-sm text-black bg-white"
            value={academyNumber ?? ""}
            onChange={(e) => {
              const v = Number(e.target.value);
              setAcademyNumber(Number.isFinite(v) ? v : undefined);
            }}
          >
            {(academyOptions.length ? academyOptions : ["" as any]).map((n) =>
              n ? (
                <option key={n} value={n}>
                  {n}
                </option>
              ) : (
                <option key="none" value="">
                  선택
                </option>
              )
            )}
          </select>
        </div>

        {/* 룸 선택 스피너 (0 = 전체) */}
        <div className="flex items-center gap-2">
          <label className="text-sm text-black">강의실</label>
          <select
            className="border border-black rounded px-2 py-1 text-sm text-black bg-white"
            value={roomNumber ?? ""}
            onChange={(e) => {
              const v = Number(e.target.value);
              setRoomNumber(Number.isFinite(v) ? v : undefined); // 0도 유지(전체)
            }}
            disabled={loadingRooms || !rooms.length}
          >
            {/* 전체 옵션 */}
            <option value={0}>전체</option>
            {rooms.length === 0 ? (
              <option value="">없음</option>
            ) : (
              rooms.map((r: any) => (
                <option key={`${r.academyNumber}-${r.roomNumber}`} value={Number(r.roomNumber)}>
                  {Number(r.roomNumber)}
                </option>
              ))
            )}
          </select>
        </div>

        {/* 인쇄 크기(mm)만 조절 */}
        <div className="flex items-center gap-2">
          <label className="text-sm text-black">인쇄(mm)</label>
          <select
            className="border border-black rounded px-2 py-1 text-sm text-black bg-white"
            value={printSizeMm}
            onChange={(e) => setPrintSizeMm(Number(e.target.value))}
          >
            {[25, 30, 35, 40, 50].map((mm) => (
              <option key={mm} value={mm}>
                {mm}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 로딩/오류 */}
      {loadingRooms && <div className="text-sm text-gray-600">강의실 목록 불러오는 중…</div>}
      {roomsErr && <div className="text-sm text-red-600">오류: {roomsErr}</div>}

      {/* 좌석 체크박스 리스트 */}
      <div className="rounded-xl border ring-1 ring-black/5 bg-white overflow-hidden">
        <div className="px-4 py-3 flex items-center gap-2 bg-gray-50">
          <div className="font-semibold text-sm text-gray-900">좌석 선택</div>
          <div className="ml-auto flex items-center gap-2">
            <button
              onClick={selectAll}
              className="px-3 py-1.5 rounded bg-gray-800 text-white text-sm hover:bg-gray-700"
              disabled={!allSeats.length}
            >
              전체 선택
            </button>
            <button
              onClick={clearAll}
              className="px-3 py-1.5 rounded bg-gray-100 text-gray-900 text-sm hover:bg-gray-200"
            >
              선택 해제
            </button>
            <button
              onClick={generateSeatQrs}
              className="px-3 py-1.5 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700"
              disabled={!selectedKeys.length}
            >
              선택 좌석 QR 생성
            </button>
          </div>
        </div>

        <div className="p-4">
          {(!isAllRooms && !currentRoom) ? (
            <div className="text-sm text-gray-600">강의실을 선택하세요.</div>
          ) : allSeats.length === 0 ? (
            <div className="text-sm text-gray-600">
              이 강의실에 좌석 레이아웃이 없습니다. (벡터 스키마 사용 시 <code>vectorLayout</code> 길이를 좌석 수로 사용합니다)
            </div>
          ) : (
            <div className="grid gap-2 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
              {allSeats.map((s) => {
                const k = keyOf(s.roomNumber, s.seatNumber);
                const checked = selectedKeys.includes(k);
                const disabled = !!s.disabled;
                // ✅ 단일/전체 모두 같은 표기 형식
                const label = `${pad3(s.roomNumber)} 강의실 ${s.seatNumber}번 좌석`;
                return (
                  <label
                    key={k}
                    className={`flex items-center gap-2 px-3 py-2 rounded border ${
                      checked ? "bg-emerald-50 border-emerald-200" : "bg-white hover:bg-gray-50"
                    } ${disabled ? "opacity-50" : ""}`}
                    title={disabled ? "비활성 좌석" : label}
                  >
                    <input
                      type="checkbox"
                      className="accent-emerald-600"
                      checked={checked}
                      disabled={disabled}
                      onChange={() => toggleSeat(k)}
                    />
                    <span className="text-sm text-gray-900">{label}</span>
                  </label>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* 생성된 QR 미리보기 + 다운로드/인쇄 */}
      <div className="rounded-xl border ring-1 ring-black/5 bg-white overflow-hidden">
        <div className="px-4 py-3 flex items-center gap-2 bg-gray-50">
          <div className="font-semibold text-sm text-gray-900">QR 미리보기</div>
          <div className="ml-auto flex items-center gap-2">
            <button
              onClick={printAll}
              className="px-3 py-1.5 rounded bg-black text-white text-sm hover:bg-gray-900"
              disabled={!qrList.length}
              title={`지금 설정된 인쇄 크기: ${printSizeMm}mm`}
            >
              모두 인쇄
            </button>
          </div>
        </div>

        <div className="p-4">
          {!qrList.length ? (
            <div className="text-sm text-gray-600">
              좌석을 선택하고 “선택 좌석 QR 생성”을 눌러주세요.
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {qrList.map((it) => (
                <div key={`${it.roomNumber}-${it.seatNumber}`} className="rounded-xl border ring-1 ring-black/5 p-3 text-center">
                  <div className="text-sm font-semibold text-gray-900 mb-2">
                    {pad3(it.roomNumber)} 강의실 {it.seatNumber}번 좌석
                  </div>
                  <img
                    src={it.url}
                    alt={`${it.roomNumber}강의실 ${it.seatNumber}번 좌석 QR`}
                    /* 미리보기는 고정 px */
                    style={{ width: `${PREVIEW_PX}px`, height: `${PREVIEW_PX}px` }}
                    className="mx-auto rounded-lg ring-1 ring-black/5"
                  />
                  <div className="mt-3 flex items-center justify-center gap-2">
                    <a
                      href={it.url}
                      download={`qr_seat_${academyNumber}_${it.roomNumber}_${it.seatNumber}.png`}
                      className="inline-block px-3 py-1.5 rounded bg-gray-800 text-white text-sm hover:bg-gray-700"
                    >
                      PNG 다운로드
                    </a>
                    <button
                      onClick={() => printOne(it)}
                      className="inline-block px-3 py-1.5 rounded bg-gray-100 text-gray-900 text-sm hover:bg-gray-200"
                      title={`인쇄 크기: ${printSizeMm}mm`}
                    >
                      인쇄
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
