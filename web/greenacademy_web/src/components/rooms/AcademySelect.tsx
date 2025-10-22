"use client";
type Props = {
  value?: number;
  options: number[];                 // login.academyNumbers
  onChange: (v: number) => void;
};
export default function AcademySelect({ value, options, onChange }: Props) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-sm text-gray-600">학원번호</span>
      <select
        className="border rounded p-2"
        value={value ?? ""}
        onChange={(e) => onChange(parseInt(e.target.value))}
      >
        <option value="" disabled>선택</option>
        {options?.map((n) => (
          <option key={n} value={n}>{n}</option>
        ))}
      </select>
    </div>
  );
}
