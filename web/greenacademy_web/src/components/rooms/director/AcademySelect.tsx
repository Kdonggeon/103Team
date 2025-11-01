"use client";

import React from "react";

export default function AcademySelect({
  value,
  options,
  onChange,
}: {
  value: number | undefined;
  options: number[];
  onChange: (n: number | undefined) => void;
}) {
  return (
    <div className="flex items-center gap-2">
      <label className="text-sm text-gray-900">학원번호</label>
      <select
        className="border border-black rounded px-2 py-1 text-gray-900 bg-white"
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value ? parseInt(e.target.value, 10) : undefined)}
      >
        {options.map((n) => (
          <option key={n} value={n} className="text-gray-900">
            {n}
          </option>
        ))}
      </select>
    </div>
  );
}
