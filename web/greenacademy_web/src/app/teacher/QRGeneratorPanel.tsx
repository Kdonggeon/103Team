// C:\project\103Team-sub\web\greenacademy_web\src\app\teacher\QRGeneratorPanel.tsx
"use client";

import { useState } from "react";
import type { LoginResponse } from "@/app/lib/api";

type Props = { user: NonNullable<LoginResponse> };

// 안전한 nonce 생성기(클라이언트에서 자동 생성, 화면엔 표시하지 않음)
function genNonce(len = 16): string {
  try {
    if (typeof crypto !== "undefined" && crypto.getRandomValues) {
      const b = new Uint8Array(len);
      crypto.getRandomValues(b);
      const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
      let s = "";
      for (const v of b) s += alphabet[v % alphabet.length];
      return s;
    }
  } catch {
    /* ignore and fallback */
  }
  // fallback(덜 안전): 개발/레거시 환경 대비
  return Array.from({ length: len }, () => Math.floor(Math.random() * 36).toString(36)).join("");
}

export default function QRGeneratorPanel({ user }: Props) {
  // ✅ 문자열/숫자 혼합 가능성 대비: 숫자 배열로 정규화
  const academyOptions = (user.academyNumbers || [])
    .map((n: any) => Number(n))
    .filter((n) => Number.isFinite(n)) as number[];

  const [academyNumber, setAcademyNumber] = useState<number | undefined>(academyOptions[0]);

  // 학원 입구
  const [entranceUrl, setEntranceUrl] = useState<string>("");

  // 강의실 입구
  const [roomNumber, setRoomNumber] = useState<number | undefined>(undefined);
  const [roomUrl, setRoomUrl] = useState<string>("");

  // 좌석
  const [seatRoom, setSeatRoom] = useState<number | undefined>(undefined);
  const [seatNumber, setSeatNumber] = useState<number | undefined>(undefined);
  const [seatUrl, setSeatUrl] = useState<string>("");

  const makeQrUrl = (payload: object) => {
    const json = JSON.stringify(payload);
    const data = encodeURIComponent(json);
    return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${data}`;
  };
  const now = () => Date.now();

  const genEntrance = () => {
    if (!academyNumber) return alert("학원번호를 선택하세요.");
    const payload = { v: 1, type: "academy", academyNumber, issuedAt: now(), nonce: genNonce() };
    setEntranceUrl(makeQrUrl(payload));
  };

  const genRoom = () => {
    if (!academyNumber) return alert("학원번호를 선택하세요.");
    if (!roomNumber) return alert("강의실 번호를 입력하세요.");
    const payload = {
      v: 1,
      type: "room",
      academyNumber,
      roomNumber,
      issuedAt: now(),
      nonce: genNonce(),
    };
    setRoomUrl(makeQrUrl(payload));
  };

  const genSeat = () => {
    if (!academyNumber) return alert("학원번호를 선택하세요.");
    if (!seatRoom) return alert("강의실 번호를 입력하세요.");
    if (!seatNumber) return alert("좌석 번호를 입력하세요.");
    const payload = {
      v: 1,
      type: "seat",
      academyNumber,
      roomNumber: seatRoom,
      seatNumber,
      issuedAt: now(),
      nonce: genNonce(),
    };
    setSeatUrl(makeQrUrl(payload));
  };

  const Download = ({ url, name }: { url: string; name: string }) =>
    url ? (
      <a
        href={url}
        download={name}
        className="inline-block px-3 py-1.5 rounded bg-black text-white text-sm hover:bg-gray-800"
      >
        PNG 다운로드
      </a>
    ) : null;

  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 space-y-6">
      <div className="flex items-center gap-3">
        <h2 className="text-lg font-semibold text-gray-900">QR 생성</h2>
        <div className="ml-auto flex items-center gap-2">
          <label className="text-sm text-gray-700">학원번호</label>
          <select
            className="border rounded px-2 py-1 text-sm"
            value={academyNumber ?? ""}
            onChange={(e) => setAcademyNumber(Number(e.target.value))}
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
      </div>

      {/* 학원 입구 */}
      <details className="rounded-xl border ring-1 ring-black/5 bg-white overflow-hidden" open>
        <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-gray-900 bg-gray-50">
          학원 입구 QR 생성
        </summary>
        <div className="p-4 space-y-3">
          <div className="flex flex-wrap items-end gap-3">
            <button onClick={genEntrance} className="ml-auto px-4 py-2 rounded bg-emerald-600 text-white">
              생성
            </button>
          </div>
          {entranceUrl && (
            <div className="flex items-start gap-4">
              <img
                src={entranceUrl}
                alt="Academy Entrance QR"
                className="w-40 h-40 rounded-lg ring-1 ring-black/5"
              />
              <div className="space-y-2">
                <div className="text-sm text-gray-700">
                  스캔: <code className="text-xs">type=academy</code>
                </div>
                <Download url={entranceUrl} name={`qr_academy_${academyNumber}.png`} />
              </div>
            </div>
          )}
        </div>
      </details>

      {/* 강의실 입구 */}
      <details className="rounded-xl border ring-1 ring-black/5 bg-white overflow-hidden">
        <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-gray-900 bg-gray-50">
          강의실 입구 QR 생성
        </summary>
        <div className="p-4 space-y-3">
          <div className="flex flex-wrap items-end gap-3">
            <div>
              <label className="block text-sm text-gray-600">강의실 번호</label>
              <input
                type="number"
                value={roomNumber ?? ""}
                onChange={(e) => setRoomNumber(parseInt(e.target.value || "0"))}
                className="border rounded px-2 py-1 w-36"
              />
            </div>
            <button onClick={genRoom} className="ml-auto px-4 py-2 rounded bg-emerald-600 text-white">
              생성
            </button>
          </div>
          {roomUrl && (
            <div className="flex items-start gap-4">
              <img src={roomUrl} alt="Room Entrance QR" className="w-40 h-40 rounded-lg ring-1 ring-black/5" />
              <div className="space-y-2">
                <div className="text-sm text-gray-700">
                  스캔: <code className="text-xs">type=room → waiting_room 등록</code>
                </div>
                <Download url={roomUrl} name={`qr_room_${academyNumber}_${roomNumber}.png`} />
              </div>
            </div>
          )}
        </div>
      </details>

      {/* 좌석 */}
      <details className="rounded-xl border ring-1 ring-black/5 bg-white overflow-hidden">
        <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-gray-900 bg-gray-50">
          좌석 QR 생성
        </summary>
        <div className="p-4 space-y-3">
          <div className="flex flex-wrap items-end gap-3">
            <div>
              <label className="block text-sm text-gray-600">강의실 번호</label>
              <input
                type="number"
                value={seatRoom ?? ""}
                onChange={(e) => setSeatRoom(parseInt(e.target.value || "0"))}
                className="border rounded px-2 py-1 w-36"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-600">좌석 번호</label>
              <input
                type="number"
                value={seatNumber ?? ""}
                onChange={(e) => setSeatNumber(parseInt(e.target.value || "0"))}
                className="border rounded px-2 py-1 w-36"
              />
            </div>
            <button onClick={genSeat} className="ml-auto px-4 py-2 rounded bg-emerald-600 text-white">
              생성
            </button>
          </div>
          {seatUrl && (
            <div className="flex items-start gap-4">
              <img src={seatUrl} alt="Seat QR" className="w-40 h-40 rounded-lg ring-1 ring-black/5" />
              <div className="space-y-2">
                <div className="text-sm text-gray-700">
                  스캔: <code className="text-xs">type=seat → seat 배치 & waiting_room 제거</code>
                </div>
                <Download
                  url={seatUrl}
                  name={`qr_seat_${academyNumber}_${seatRoom}_${seatNumber}.png`}
                />
              </div>
            </div>
          )}
        </div>
      </details>

    </div>
  );
}
