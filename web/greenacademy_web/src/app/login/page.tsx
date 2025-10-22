// src/app/login/page.tsx
"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api, type LoginRequest, type LoginResponse } from "@/app/lib/api";
import { setSession } from "@/app/lib/session";

export default function LoginPage() {
  const router = useRouter();

  const [id, setId] = useState("");
  const [pw, setPw] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [keepLogin, setKeepLogin] = useState(true);
  const [ipSecure, setIpSecure] = useState(true);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !pw) return setMsg("아이디와 비밀번호를 입력해주세요.");
    setLoading(true);
    setMsg(null);
    try {
      const body: LoginRequest = { username: id, password: pw };
      const data: LoginResponse = await api.login(body);

// 1) 토큰 필수 확인
if (!data?.token || typeof data.token !== "string" || data.token.length === 0) {
  setLoading(false);
  return setMsg("로그인 토큰이 없습니다. 관리자에게 문의하세요.");
}

// 2) 세션 저장: 상태(Session) 반영
setSession(data);

// 3) 로컬 영구 저장(localStorage)
//    - 호환성 위해 keepLogin, ipSecure도 함께 저장(기존 sub1 코드 유지)
localStorage.setItem(
  "login",
  JSON.stringify({
    token: data.token,
    role: data.role,
    username: data.username,
    name: data.name ?? null,
    academyNumbers: Array.isArray(data.academyNumbers) ? data.academyNumbers : [],
    keepLogin,
    ipSecure,
  })
);

// 4) 옵션은 별도 키로도 저장(기존 HEAD 동작 유지)
localStorage.setItem("client_prefs", JSON.stringify({ keepLogin, ipSecure }));

      // 역할별 라우팅
      let next = "/";
      if (data.role === "student" || data.role === "parent") {
        next = "/family-portal"; // 필요 시 실제 경로로 변경
      }
      router.replace(next);
    } catch (err: any) {
      setMsg(err?.message || "로그인 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-[100svh] bg-gray-50 grid place-items-center px-4">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-gray-100 p-8 space-y-6"
        aria-describedby={msg ? "login-error" : undefined}
      >
        <div className="text-center">
          <h1 className="text-3xl font-extrabold text-emerald-600">
            GREEN ACADEMY
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            한 번의 로그인으로 학사/출석/수업 관리까지.
          </p>
        </div>

        <input
          className="w-full h-12 rounded-xl border border-gray-200 px-4 outline-none bg-emerald-50/20
                     focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
          placeholder="아이디 또는 전화번호"
          value={id}
          onChange={(e) => setId(e.target.value)}
          autoComplete="username"
          autoFocus
        />

        <div
          className="flex h-12 rounded-xl border border-gray-200 overflow-hidden
                     focus-within:ring-2 focus-within:ring-emerald-300 focus-within:border-emerald-300 bg-emerald-50/20"
        >
          <input
            className="flex-1 px-4 outline-none border-0 bg-transparent"
            placeholder="비밀번호"
            type={showPw ? "text" : "password"}
            value={pw}
            onChange={(e) => setPw(e.target.value)}
            autoComplete="current-password"
          />
          <button
            type="button"
            onClick={() => setShowPw((v) => !v)}
            className={`px-4 text-xs font-semibold border-l ${
              showPw
                ? "bg-emerald-500 text-white border-emerald-500 hover:bg-emerald-600"
                : "bg-white text-emerald-600 border-gray-200 hover:bg-emerald-50"
            }`}
          >
            {showPw ? "숨김" : "보기"}
          </button>
        </div>

        <div className="flex items-center justify-between text-sm text-gray-600">
          <label className="inline-flex items-center gap-2">
            <input
              type="checkbox"
              className="accent-emerald-600 w-4 h-4 rounded"
              checked={keepLogin}
              onChange={(e) => setKeepLogin(e.target.checked)}
            />
            로그인 상태 유지
          </label>

          <button
            type="button"
            onClick={() => setIpSecure((v) => !v)}
            className="inline-flex items-center gap-2"
            aria-pressed={ipSecure}
          >
            <span>IP보안</span>
            <span
              className={`relative inline-flex h-5 w-10 items-center rounded-full transition ${
                ipSecure ? "bg-emerald-500" : "bg-gray-300"
              }`}
            >
              <span
                className={`h-4 w-4 bg-white rounded-full shadow transform transition ${
                  ipSecure ? "translate-x-5" : "translate-x-1"
                }`}
              />
            </span>
          </button>
        </div>

        {msg && (
          <div
            id="login-error"
            className="text-sm text-red-600 bg-red-50 border border-red-200 p-3 rounded-lg"
            aria-live="polite"
          >
            {msg}
          </div>
        )}

        <button
          disabled={loading || !id || !pw}
          className="w-full h-12 rounded-xl bg-emerald-500 text-white font-semibold
                     hover:bg-emerald-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "로그인 중..." : "로그인"}
        </button>

        <div className="pt-1 text-center text-sm text-gray-500">
          <Link href="/find_id" className="hover:underline">
            아이디 찾기
          </Link>
          <span className="mx-2 text-gray-300">|</span>
          <Link href="/reset_pw" className="hover:underline">
            비밀번호 찾기
          </Link>
          <span className="mx-2 text-gray-300">|</span>
          <Link href="/signup" className="hover:underline">
            회원가입
          </Link>
        </div>
      </form>
    </main>
  );
}
