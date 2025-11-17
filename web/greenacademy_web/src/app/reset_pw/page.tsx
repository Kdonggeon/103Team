"use client";

import React, { useMemo, useState } from "react";
import { useRouter } from "next/navigation";

const API_BASE = "/backend";


type Role = "student" | "parent" | "teacher" | "director";

/** 비밀번호 규칙: 8~64자, 영문/숫자/특수문자 각각 1개 이상, 공백 금지 */
const PW_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#$%^&*()_\-+=\[{\]};:'",.<>/?\\|`]).{8,64}$/;

const pwChecks = (pw: string) => ({
  len: pw.length >= 8 && pw.length <= 64,
  hasLetter: /[A-Za-z]/.test(pw),
  hasDigit: /\d/.test(pw),
  hasSpecial: /[~!@#$%^&*()_\-+=\[{\]};:'",.<>/?\\|`]/.test(pw),
  noSpace: !/\s/.test(pw),
});

export default function ResetPasswordPage() {
  const router = useRouter();

  // 입력 상태
  const [role, setRole] = useState<Role>("student");
  const [id, setId] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState(""); // 하이픈 허용, 전송 시 숫자만
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");

  // 보기/숨김
  const [showPw1, setShowPw1] = useState(false);
  const [showPw2, setShowPw2] = useState(false);

  // UI 상태
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  // 검증
  const rules = pwChecks(pw);
  const passRule = PW_REGEX.test(pw) && rules.noSpace;
  const match = pw.length > 0 && pw === pw2;

  const disabled = useMemo(() => {
    if (!id.trim() || !name.trim() || !phone.trim()) return true;
    if (!passRule || !match) return true;
    return false;
  }, [id, name, phone, passRule, match]);

  const normalizePhone = (v: string) => v.replace(/\D/g, "");

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (disabled) return;

    setLoading(true);
    setErr(null);
    setMsg(null);

    try {
      const res = await fetch(`${API_BASE}/api/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        cache: "no-store",
        body: JSON.stringify({
          role,
          id: id.trim(),         // studentId / parentsId / teacherId / director.username
          name: name.trim(),
          phone: normalizePhone(phone),
          newPassword: pw,
        }),
      });

      const text = await res.text();
      let data: any = {};
      try { data = text ? JSON.parse(text) : {}; } catch { data = { message: text }; }

      if (!res.ok) throw new Error(data?.message || "비밀번호 재설정 실패");

      setMsg("비밀번호가 변경되었습니다. 로그인 페이지로 이동합니다.");
      setTimeout(() => router.replace("/login"), 1200);
    } catch (e: any) {
      setErr(e?.message || "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-[100svh] bg-gray-50 grid place-items-center px-4">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-gray-100 p-8 space-y-6"
        aria-describedby={err ? "reset-error" : undefined}
      >
        {/* 헤더 */}
        <div className="text-center">
          <h1 className="text-2xl font-extrabold text-emerald-600">비밀번호 재설정</h1>
          <p className="mt-2 text-sm text-gray-900">
            본인 확인 정보가 일치하면 새 비밀번호로 변경됩니다.
          </p>
        </div>

        {/* 역할 선택 */}
        <div className="grid grid-cols-2 gap-2">
          {(["student","parent","teacher","director"] as Role[]).map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRole(r)}
              className={`h-10 rounded-xl ring-1 ring-black/5 font-semibold text-sm ${
                role === r ? "bg-emerald-500 text-white" : "bg-gray-100 text-gray-900 hover:bg-gray-200"
              }`}
              aria-pressed={role === r}
            >
              {r === "student" ? "학생" : r === "parent" ? "학부모" : r === "teacher" ? "교사" : "원장"}
            </button>
          ))}
        </div>

        {/* 아이디 / 이름 / 연락처 */}
        <label className="block">
          <span className="block text-sm text-gray-900 mb-1">
            아이디 ({role === "student" ? "학생ID" : role === "parent" ? "학부모ID" : role === "teacher" ? "교사ID" : "원장ID"})
          </span>
          <input
            className="w-full h-11 rounded-xl border border-gray-300 px-3 text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
            value={id}
            onChange={(e) => setId(e.target.value)}
            autoComplete="username"
            required
          />
        </label>

        <label className="block">
          <span className="block text-sm text-gray-900 mb-1">이름</span>
          <input
            className="w-full h-11 rounded-xl border border-gray-300 px-3 text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </label>

        <label className="block">
          <span className="block text-sm text-gray-900 mb-1">연락처</span>
          <input
            className="w-full h-11 rounded-xl border border-gray-300 px-3 text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="010-0000-0000"
            inputMode="tel"
            required
          />
        </label>

        {/* 새 비밀번호 / 확인 */}
        <PwField
          label="새 비밀번호"
          value={pw}
          onChange={setPw}
          show={showPw1}
          onToggleShow={() => setShowPw1((v) => !v)}
          autoComplete="new-password"
        />
        <PwField
          label="새 비밀번호 확인"
          value={pw2}
          onChange={setPw2}
          show={showPw2}
          onToggleShow={() => setShowPw2((v) => !v)}
          autoComplete="new-password"
        />

        {/* 규칙 안내 */}
        <ul className="mt-1 space-y-1 text-sm">
          <Rule ok={rules.len}>8~64자</Rule>
          <Rule ok={rules.hasLetter}>영문 포함</Rule>
          <Rule ok={rules.hasDigit}>숫자 포함</Rule>
          <Rule ok={rules.hasSpecial}>특수문자 포함</Rule>
          <Rule ok={rules.noSpace}>공백 없음</Rule>
          <Rule ok={pw.length > 0 && match}>새 비밀번호와 확인이 일치</Rule>
        </ul>

        {/* 메시지 */}
        {msg && (
          <div className="text-sm text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-2 rounded-lg">
            {msg}
          </div>
        )}
        {err && (
          <div id="reset-error" className="text-sm text-red-600 bg-red-50 border border-red-200 px-3 py-2 rounded-lg">
            {err}
          </div>
        )}

        {/* 제출 */}
        <button
          disabled={loading || disabled}
          className="w-full h-11 rounded-xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600 disabled:opacity-50"
        >
          {loading ? "처리 중..." : "비밀번호 재설정"}
        </button>

        <button
          type="button"
          onClick={() => router.replace("/login")}
          className="w-full h-10 rounded-xl mt-2 ring-1 ring-gray-300 text-gray-900 hover:bg-gray-50"
        >
          로그인으로 돌아가기
        </button>
      </form>
    </main>
  );
}

/* ------- 재사용 컴포넌트 ------- */

function PwField({
  label,
  value,
  onChange,
  show,
  onToggleShow,
  autoComplete,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  show: boolean;
  onToggleShow: () => void;
  autoComplete: "new-password";
}) {
  return (
    <label className="block">
      <span className="block text-sm text-gray-900 mb-1">{label}</span>
      <div className="flex h-11 rounded-xl border border-gray-300 overflow-hidden focus-within:ring-2 focus-within:ring-emerald-300 focus-within:border-emerald-300">
        <input
          className="flex-1 px-3 outline-none border-0 bg-white text-gray-900 placeholder-gray-400"
          type={show ? "text" : "password"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          autoComplete={autoComplete}
        />
        <button
          type="button"
          onClick={onToggleShow}
          className={`px-3 text-xs font-semibold border-l ${
            show
              ? "bg-emerald-500 text-white border-emerald-500 hover:bg-emerald-600"
              : "bg-gray-100 text-gray-900 border-gray-300 hover:bg-gray-200"
          }`}
        >
          {show ? "숨김" : "보기"}
        </button>
      </div>
    </label>
  );
}

function Rule({ ok, children }: { ok: boolean; children: React.ReactNode }) {
  return (
    <li className={`flex items-center gap-2 ${ok ? "text-emerald-700" : "text-gray-900"}`}>
      <span className={`inline-block w-2 h-2 rounded-full ${ok ? "bg-emerald-500" : "bg-gray-400"}`} />
      {children}
    </li>
  );
}
