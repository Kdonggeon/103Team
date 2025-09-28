// src/app/find_id/page.tsx
"use client";

import { useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";

type Role = "student" | "parent" | "teacher" | "director";

export default function FindIdPage() {
  const [role, setRole] = useState<Role>("student");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [foundId, setFoundId] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    setFoundId(null);

    if (!name.trim() || !phone.trim()) {
      setMsg("이름과 전화번호를 모두 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const { username } = await api.findId({
        role,
        name: name.trim(),
        phoneNumber: phone.trim(), // ← FindIdRequest 키와 맞춤
      });
      setFoundId(username);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "일치하는 회원을 찾을 수 없습니다.";
      setMsg(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-dvh flex items-center justify-center p-6">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-md space-y-4 rounded-2xl p-6 shadow-lg border"
      >
        <h1 className="text-2xl font-semibold">아이디 찾기</h1>

        <div className="grid gap-3">
          <label className="space-y-1">
            <span className="text-sm">역할</span>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              className="w-full rounded-lg border px-3 py-2 bg-neutral-900 text-white [color-scheme:dark]"
            >
              <option value="student">학생</option>
              <option value="parent">학부모</option>
              <option value="teacher">교사</option>
              <option value="director">원장</option> {/* 추가 */}
            </select>
            </label>


          <label className="space-y-1">
            <span className="text-sm">이름</span>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-lg border px-3 py-2"
              placeholder="홍길동"
              autoComplete="name"
            />
          </label>

          <label className="space-y-1">
            <span className="text-sm">전화번호</span>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full rounded-lg border px-3 py-2"
              placeholder="010-1234-5678"
              inputMode="numeric"
            />
          </label>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg px-4 py-2 font-medium border hover:opacity-90 disabled:opacity-50"
        >
          {loading ? "조회 중..." : "아이디 찾기"}
        </button>

        {msg && <p className="text-sm text-red-500">{msg}</p>}

        {foundId && (
          <div className="rounded-lg border px-4 py-3">
            <p>회원님의 아이디는</p>
            <p className="mt-1 text-xl font-semibold select-all">{foundId}</p>
          </div>
        )}

        <div className="flex items-center justify-between text-sm pt-2">
          <Link href="/login" className="underline">로그인으로</Link>
          <Link href="/reset_pw" className="underline">비밀번호 재설정</Link>
        </div>
      </form>
    </main>
  );
}
