"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import Link from "next/link";

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
      const data = await api.login({ username: id, password: pw });

// 1) 토큰 필수 확인
if (!data?.token || typeof data.token !== "string" || data.token.length === 0) {
  setLoading(false);
  return setMsg("로그인 토큰이 없습니다. 관리자에게 문의하세요.");
}

// 2) 인증에 필요한 최소 필드만 명시적으로 저장
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

      // 역할별 라우팅
      // - teacher/director: 방금 보던 대시보드 페이지(루트 "/")
      // - student: 학생 포털 새 페이지
      // - parent : 학부모 포털 새 페이지
      // 역할별 라우팅
      let next = "/";
      if (data.role === "student" || data.role === "parent") {
        next = "/family-portal"; // 학생/학부모 공용 포털
      }
      router.replace(next);


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
        {/* 상단 브랜드 영역 */}
        <div className="text-center">
          <h1 className="text-3xl font-extrabold text-emerald-600">
            GREEN ACADEMY
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            한 번의 로그인으로 학사/출석/수업 관리까지.
          </p>
        </div>

        {/* 아이디 */}
        <input
          className="w-full h-12 rounded-xl border border-gray-200 px-4 outline-none bg-emerald-50/20
                     focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
          placeholder="아이디 또는 전화번호"
          value={id}
          onChange={(e) => setId(e.target.value)}
          autoComplete="username"
          autoFocus
        />

        {/* 비밀번호 + 보기 버튼 */}
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

        {/* 옵션줄 */}
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

        {/* 에러 메시지 */}
        {msg && (
          <div
            id="login-error"
            className="text-sm text-red-600 bg-red-50 border border-red-200 p-3 rounded-lg"
            aria-live="polite"
          >
            {msg}
          </div>
        )}

        {/* 로그인 버튼 */}
        <button
          disabled={loading || !id || !pw}
          className="w-full h-12 rounded-xl bg-emerald-500 text-white font-semibold
                     hover:bg-emerald-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "로그인 중..." : "로그인"}
        </button>

        {/* 하단 링크 */}
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
