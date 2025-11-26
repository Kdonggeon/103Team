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
    <main className="min-h-[100svh] bg-gray-50 grid place-items-center px-4">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-gray-100 p-8 space-y-6"
        aria-describedby={msg ? "find-id-error" : undefined}
      >
        <div className="text-center">
          <h1 className="text-2xl font-extrabold text-emerald-600">아이디 찾기</h1>
          <p className="mt-2 text-sm text-gray-900">이름과 연락처를 입력하면 등록된 아이디를 알려드립니다.</p>
        </div>

        <div className="grid gap-4">
          <div className="grid grid-cols-2 gap-2">
            {(["student", "parent", "teacher", "director"] as Role[]).map((r) => (
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

          <label className="space-y-1">
            <span className="text-sm text-gray-900">이름</span>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full h-11 rounded-xl border border-gray-300 px-3 text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
              placeholder="홍길동"
              autoComplete="name"
            />
          </label>

          <label className="space-y-1">
            <span className="text-sm text-gray-900">전화번호</span>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full h-11 rounded-xl border border-gray-300 px-3 text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
              placeholder="010-1234-5678"
              inputMode="numeric"
            />
          </label>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full h-11 rounded-xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "조회 중..." : "아이디 찾기"}
        </button>

        {msg && (
          <div
            id="find-id-error"
            className="text-sm text-red-600 bg-red-50 border border-red-200 p-3 rounded-lg"
            aria-live="polite"
          >
            {msg}
          </div>
        )}

        {foundId && (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-gray-900">
            <p>회원님의 아이디는</p>
            <p className="mt-1 text-xl font-semibold select-all text-emerald-700">{foundId}</p>
          </div>
        )}

        <div className="flex items-center justify-between text-sm pt-2 text-gray-600">
          <Link href="/login" className="hover:underline">
            로그인으로
          </Link>
          <Link href="/reset_pw" className="hover:underline">
            비밀번호 재설정
          </Link>
        </div>
      </form>
    </main>
  );
}
