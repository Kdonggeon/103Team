"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { routeByRole } from "@/lib/routeByRole";

export default function LoginPage() {
  const router = useRouter();

  const [id, setId] = useState("");
  const [pw, setPw] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !pw) {
      setMsg("아이디와 비밀번호를 입력해주세요.");
      return;
    }
    setLoading(true);
    setMsg(null);
    try {
      const res = await api.login({ username: id, password: pw });
      localStorage.setItem("login", JSON.stringify(res));
      router.replace(routeByRole(res.role));
    } catch (err: any) {
      setMsg(err.message || "로그인 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-[100svh] flex items-center justify-center bg-gradient-to-b from-emerald-50 to-white px-4">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm rounded-2xl bg-white shadow-md p-6 space-y-5"
      >
        {/* 헤더 - 모바일 느낌 */}
        <div className="text-center space-y-1">
          <h1 className="text-2xl font-extrabold tracking-tight">그린 아카데미</h1>
          <p className="text-gray-500 text-sm">로그인</p>
        </div>

        {/* 아이디 */}
        <div className="space-y-1">
          <label className="text-sm text-gray-600">아이디</label>
          <input
            className="w-full rounded-xl border border-gray-200 p-3 outline-none focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
            placeholder="아이디를 입력하세요"
            value={id}
            onChange={(e) => setId(e.target.value)}
            autoFocus
            autoComplete="username"
          />
        </div>

        {/* 비밀번호 + 보기 토글 */}
        <div className="space-y-1">
          <label className="text-sm text-gray-600">비밀번호</label>
          <div className="flex items-center gap-2">
            <input
              className="w-full rounded-xl border border-gray-200 p-3 outline-none focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
              placeholder="비밀번호를 입력하세요"
              type={showPw ? "text" : "password"}
              value={pw}
              onChange={(e) => setPw(e.target.value)}
              autoComplete="current-password"
            />
            <button
              type="button"
              onClick={() => setShowPw((v) => !v)}
              className="shrink-0 text-xs px-3 py-2 rounded-lg border border-gray-200 hover:bg-gray-50"
            >
              {showPw ? "숨김" : "보기"}
            </button>
          </div>
        </div>

        {/* 에러/안내 메시지 */}
        {msg && (
          <div
            className="text-sm text-red-600 bg-red-50 border border-red-200 p-2.5 rounded-lg"
            aria-live="polite"
          >
            {msg}
          </div>
        )}

        {/* 로그인 버튼 */}
        <button
          disabled={loading}
          className="w-full rounded-xl p-3.5 bg-emerald-500 text-white font-semibold hover:bg-emerald-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? (
            <span className="inline-flex items-center gap-2">
              <svg
                className="animate-spin h-4 w-4"
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                ></circle>
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
                ></path>
              </svg>
              로그인 중...
            </span>
          ) : (
            "로그인"
          )}
        </button>

        {/* 보조 링크 */}
        <div className="text-center text-sm text-gray-500">
          <a href="/find-account" className="hover:underline">
            아이디/비밀번호 찾기
          </a>
          <span className="mx-2 text-gray-300">|</span>
          <a href="/signup" className="hover:underline">
            회원가입
          </a>
        </div>
      </form>
    </main>
  );
}
