"use client";

import React, { useEffect, useState } from "react";

/** 숫자만 받되, 편집 중엔 문자열로 두는 가벼운 입력 */
function NumericField({
  label,
  value,
  onChange,
  placeholder,
  min = 1,
  className = "w-28",
}: {
  label: string;
  value: number;
  onChange: (n: number) => void;
  placeholder?: string;
  min?: number;
  className?: string;
}) {
  const [text, setText] = useState<string>(String(value ?? ""));

  useEffect(() => {
    setText(String(value ?? ""));
  }, [value]);

  const onlyDigits = (s: string) => s.replace(/[^0-9]/g, "");

  const commit = () => {
    const digits = onlyDigits(text);
    if (digits === "") {
      setText(String(value ?? ""));
      return;
    }
    const n = Math.max(min, parseInt(digits, 10));
    if (!Number.isNaN(n)) {
      onChange(n);
      setText(String(n));
    }
  };

  return (
    <div>
      <label className="block text-sm text-gray-900">{label}</label>
      <input
        type="text"
        inputMode="numeric"
        pattern="[0-9]*"
        value={text}
        placeholder={placeholder}
        onChange={(e) => setText(onlyDigits(e.target.value))}
        onBlur={commit}
        onKeyDown={(e) => {
          if (e.key === "Enter") (e.target as HTMLInputElement).blur();
        }}
        className={`border border-black text-gray-900 placeholder:text-gray-900 bg-white rounded px-2 py-1 ${className} appearance-none`}
      />
    </div>
  );
}

export default function DirectorToolbar({
  roomNumber,
  onRoomChange,
  seatCount,
  onSeatCountChange,
  onGenerate,
  onSave,
  onDelete,
  disabled = false,
}: {
  roomNumber: number;
  onRoomChange: (n: number) => void;
  seatCount: number;
  onSeatCountChange: (n: number) => void;
  onGenerate: () => void;
  onSave: () => Promise<void> | void;
  onDelete: () => Promise<void> | void;
  disabled?: boolean;
}) {
  return (
    <div className="flex flex-wrap items-end gap-3">
      <NumericField
        label="방 번호"
        value={roomNumber}
        onChange={onRoomChange}
        placeholder="101"
      />
      <NumericField
        label="좌석 개수"
        value={seatCount}
        onChange={onSeatCountChange}
        placeholder="30"
      />

      <div className="ml-auto flex gap-2">
        <button
          onClick={onGenerate}
          disabled={disabled}
          className="bg-gray-900 text-white px-4 py-2 rounded disabled:opacity-40"
        >
          초기 배치 생성
        </button>
        <button
          onClick={() => void onSave()}
          disabled={disabled}
          className="bg-emerald-600 text-white px-4 py-2 rounded disabled:opacity-40"
        >
          저장
        </button>
        <button
          onClick={() => void onDelete()}
          disabled={disabled}
          className="bg-red-600 text-white px-4 py-2 rounded disabled:opacity-40"
        >
          삭제
        </button>
      </div>
    </div>
  );
}
