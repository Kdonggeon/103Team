"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

type LoginSession = {
  role: "student"|"teacher"|"parent"|"director";
  username: string;
  name?: string;
  token?: string;
};

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "";

export default function AccountDeletePage() {
  const router = useRouter();
  const [me, setMe] = useState<LoginSession | null>(null);
  const [ready, setReady] = useState(false);

  // form
  const [password, setPassword] = useState("");
  const [why, setWhy] = useState("");
  const [confirm, setConfirm] = useState(false);
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => {
    const raw = localStorage.getItem("login");
    if (!raw) {
      router.replace("/login");
      return;
    }
    try {
      const parsed = JSON.parse(raw);
      setMe({
        role: parsed.role,
        username: parsed.username,
        name: parsed.name,
        token: parsed.token,
      });
    } catch {
      localStorage.removeItem("login");
      router.replace("/login");
      return;
    } finally {
      setReady(true);
    }
  }, [router]);

  const roleLabel = useMemo(() => ({
    student: "학생",
    teacher: "교사",
    parent: "학부모",
    director: "원장",
  } as const)[me?.role ?? "student"], [me?.role]);

  const canSubmit = !!me && !!password && confirm && !loading;

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!me) return;

    setLoading(true);
    setMsg(null);
    try {
      const res = await fetch(`${API_BASE}/api/account/delete`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          // 토큰 검증을 서버에서 쓴다면: Authorization 헤더도 같이 보낼 수 있음
          ...(me.token ? { Authorization: `Bearer ${me.token}` } : {}),
        },
        body: JSON.stringify({
          role: me.role,
          id: me.username,
          password,
          reason: why,
          confirm: true,
        }),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data?.message || `탈퇴 실패 (${res.status})`);
      }

      // 세션 정리 후 로그인으로
      localStorage.removeItem("login");
      alert("계정이 삭제되었습니다. 이용해 주셔서 감사합니다.");
      router.replace("/login");
    } catch (err: any) {
      setMsg(err?.message ?? "처리 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  if (!ready) return null;

  return (
    <main className="min-h-[100svh] bg-neutral-950 text-white">
      <div className="max-w-2xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold mb-6">계정 탈퇴</h1>

        <div className="rounded-2xl bg-neutral-900 ring-1 ring-white/10 p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <InfoItem label="이름" value={me?.name || "-"} />
            <InfoItem label="역할" value={roleLabel} />
            <InfoItem label="아이디" value={me?.username || "-"} />
          </div>

          <form onSubmit={onSubmit} className="space-y-4">
            <div>
              <label className="block mb-2 text-sm">현재 비밀번호</label>
              <div className="flex h-12 rounded-xl overflow-hidden ring-1 ring-white/10 bg-neutral-800">
                <input
                  className="flex-1 bg-transparent px-4 outline-none"
                  type={showPw ? "text" : "password"}
                  placeholder="현재 비밀번호를 입력하세요"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  onClick={() => setShowPw(v => !v)}
                  className="px-4 bg-emerald-500 text-white text-sm font-semibold"
                >
                  {showPw ? "숨김" : "보기"}
                </button>
              </div>
            </div>

            <div>
              <label className="block mb-2 text-sm">탈퇴 사유 (선택)</label>
              <textarea
                value={why}
                onChange={(e) => setWhy(e.target.value)}
                className="w-full min-h-[96px] rounded-xl bg-neutral-800 ring-1 ring-white/10 px-4 py-3 outline-none"
                placeholder="서비스 개선에 참고하겠습니다."
              />
            </div>

            <div className="flex items-start gap-3">
              <input
                id="confirm"
                type="checkbox"
                className="mt-1 w-5 h-5 accent-emerald-500"
                checked={confirm}
                onChange={(e)=>setConfirm(e.target.checked)}
              />
              <label htmlFor="confirm" className="text-sm text-neutral-200">
                네, 위의 안내를 모두 확인했으며 계정과 데이터가 삭제되는 것에 동의합니다.
              </label>
            </div>

            {msg && (
              <div className="text-sm text-red-400 bg-red-950/40 border border-red-800 px-4 py-3 rounded-xl">
                {msg}
              </div>
            )}

            <div className="flex gap-3 pt-2">
              <button
                type="submit"
                disabled={!canSubmit}
                className="rounded-xl px-5 h-12 bg-red-600 hover:bg-red-700 disabled:opacity-50 font-semibold"
              >
                {loading ? "처리 중..." : "영구 탈퇴"}
              </button>
              <button
                type="button"
                onClick={() => router.back()}
                className="rounded-xl px-5 h-12 bg-neutral-800 ring-1 ring-white/10"
              >
                돌아가기
              </button>
            </div>
          </form>
        </div>

        <ul className="mt-6 text-sm text-neutral-400 list-disc space-y-1 pl-5">
          <li>탈퇴 시 계정과 데이터가 즉시 삭제됩니다(복구 불가).</li>
          <li>학사 행정 기록 등 필수 데이터는 법령/정책에 따라 저장될 수 있습니다.</li>
        </ul>
      </div>
    </main>
  );
}

function InfoItem({label, value}:{label:string; value:string}) {
  return (
    <div className="rounded-xl bg-neutral-800 ring-1 ring-white/10 p-3">
      <div className="text-xs text-neutral-400">{label}</div>
      <div className="mt-1 font-semibold">{value}</div>
    </div>
  );
}
