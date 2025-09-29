"use client";

import { useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";

type Role = "student" | "parent" | "teacher" | "director";

export default function ResetPasswordPage() {
  const [role, setRole] = useState<Role>("student");
  const [username, setUsername] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [pw1, setPw1] = useState("");
  const [pw2, setPw2] = useState("");
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [ok, setOk] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    setOk(false);

    if (!username.trim() || !name.trim() || !phone.trim() || !pw1.trim() || !pw2.trim()) {
      setMsg("모든 항목을 입력해주세요.");
      return;
    }
    if (pw1 !== pw2) {
      setMsg("새 비밀번호가 일치하지 않습니다.");
      return;
    }
    if (pw1.length < 8) {
      setMsg("비밀번호는 8자 이상이어야 합니다.");
      return;
    }

    setLoading(true);
    try {
      await api.resetPassword({
        role,
        username: username.trim(),
        name: name.trim(),
        phoneNumber: phone.trim(),
        newPassword: pw1,
      });
      setOk(true);
    } catch (err: any) {
      setMsg(err?.message ?? "재설정에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-dvh flex items-center justify-center p-6">
      <form onSubmit={onSubmit} className="w-full max-w-md space-y-4 rounded-2xl p-6 shadow-lg border">
        <h1 className="text-2xl font-semibold">비밀번호 재설정</h1>

        <label className="block space-y-1">
          <span className="text-sm">역할</span>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value as Role)}
            className="w-full rounded-lg border px-3 py-2 bg-neutral-900 text-white [color-scheme:dark]"
          >
            <option value="student">학생</option>
            <option value="parent">학부모</option>
            <option value="teacher">교사</option>
            <option value="director">원장</option>
          </select>
        </label>

        <label className="block space-y-1">
          <span className="text-sm">아이디</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-full rounded-lg border px-3 py-2"
            placeholder="로그인에 쓰는 ID"
            autoComplete="username"
          />
        </label>

        <label className="block space-y-1">
          <span className="text-sm">이름</span>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-lg border px-3 py-2"
            placeholder="홍길동"
            autoComplete="name"
          />
        </label>

        <label className="block space-y-1">
          <span className="text-sm">전화번호</span>
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className="w-full rounded-lg border px-3 py-2"
            placeholder="010-1234-5678"
            inputMode="numeric"
          />
        </label>

        <label className="block space-y-1">
          <span className="text-sm">새 비밀번호</span>
          <input
            type="password"
            value={pw1}
            onChange={(e) => setPw1(e.target.value)}
            className="w-full rounded-lg border px-3 py-2"
          />
        </label>

        <label className="block space-y-1">
          <span className="text-sm">새 비밀번호 확인</span>
          <input
            type="password"
            value={pw2}
            onChange={(e) => setPw2(e.target.value)}
            className="w-full rounded-lg border px-3 py-2"
          />
        </label>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg px-4 py-2 font-medium border hover:opacity-90 disabled:opacity-50"
        >
          {loading ? "처리 중..." : "비밀번호 재설정"}
        </button>

        {msg && <p className="text-sm text-red-500">{msg}</p>}
        {ok && (
          <div className="rounded-lg border px-4 py-3">
            <p className="text-green-600">비밀번호가 재설정되었습니다.</p>
            <p className="text-sm mt-1">
              이제 <Link href="/login" className="underline">로그인</Link>해 주세요.
            </p>
          </div>
        )}

        <div className="flex items-center justify-between text-sm pt-2">
          <Link href="/login" className="underline">로그인으로</Link>
          <Link href="/find_id" className="underline">아이디 찾기</Link>
        </div>
      </form>
    </main>
  );
}
