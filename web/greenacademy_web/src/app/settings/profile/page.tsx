"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

// ✅ 환경변수 없을 때도 자동으로 백엔드(9090)로 붙도록 폴백
const API_BASE = "/backend";


// ✅ Next 404 HTML을 받았을 때 9090으로 한 번 더 재시도하는 래퍼
async function fetchApi(path: string, init: RequestInit) {
  const url = `${API_BASE}${path}`;
  let res = await fetch(url, init);
  const ct = res.headers.get("content-type") || "";

  // Next 라우터의 404 HTML을 받는 경우(개발환경) 한 번 더 9090으로 재시도
  if (res.status === 404 && ct.includes("text/html") && typeof window !== "undefined") {
    try {
      const devUrl = `${location.protocol}//${location.hostname}:9090${path}`;
      const retry = await fetch(devUrl, init);
      return retry;
    } catch {
      // 재시도 실패 시 원 응답 반환
      return res;
    }
  }
  return res;
}

type Role = "student" | "parent" | "teacher" | "director";
type Session = {
  role: Role;
  username: string;
  name?: string;
  phone?: string;
  token?: string;
  academyNumbers?: number[];
};

type CommonForm = { name: string; phone: string };
type StudentExtra = { address?: string; school?: string; grade?: number | string; gender?: string };
type TeacherExtra = { academyNumber?: number | string };
type DirectorExtra = { academyNumbersText?: string };

const toNums = (txt?: string) =>
  (txt ?? "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean)
    .map((n) => Number(n))
    .filter((n) => Number.isFinite(n));

export default function ProfileSettingsPage() {
  const router = useRouter();
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);

  const [form, setForm] = useState<CommonForm>({ name: "", phone: "" });
  const [stuExtra, setStuExtra] = useState<StudentExtra>({});
  const [tchExtra, setTchExtra] = useState<TeacherExtra>({});
  const [dirExtra, setDirExtra] = useState<DirectorExtra>({});

  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  // 🔥 계정 삭제 후 부모 페이지 새로고침 트리거
useEffect(() => {
  const handler = (e: MessageEvent) => {
    if (e.data === "account:deleted") {
      // 세션 제거
      localStorage.removeItem("login");

      // 🔥 로그인 페이지로 이동
      window.location.href = "/login";
    }
  };

  window.addEventListener("message", handler);
  return () => window.removeEventListener("message", handler);
}, []);

  useEffect(() => {
    const raw = localStorage.getItem("login");
    if (!raw) return router.replace("/login");
    try {
      const s = JSON.parse(raw) as Session;
      setSession(s);
      setForm({ name: s.name ?? "", phone: s.phone ?? "" });
      if (s.role === "teacher") setTchExtra({ academyNumber: s.academyNumbers?.[0] ?? "" });
      if (s.role === "director") setDirExtra({ academyNumbersText: (s.academyNumbers ?? []).join(",") });
    } catch {
      localStorage.removeItem("login");
      router.replace("/login");
    } finally {
      setLoading(false);
    }
  }, [router]);

  // 항상 true(원하면 isDirty 로 바꿔도 됨)
  const dirty = useMemo(() => true, [form, stuExtra, tchExtra, dirExtra]);

  const onSave = async () => {
    if (!session) return;
    setSaving(true);
    setErr(null);
    setMsg(null);
    try {
      const headers: HeadersInit = {
        "Content-Type": "application/json",
        ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
      };

      if (session.role === "student") {
        const payload = {
          studentId: session.username,
          studentName: form.name,
          studentPhoneNumber: form.phone,
          address: stuExtra.address ?? "",
          school: stuExtra.school ?? "",
          grade: Number(stuExtra.grade ?? 0) || 0,
          gender: stuExtra.gender ?? "",
        };
        const res = await fetch(`${API_BASE}/api/students/${encodeURIComponent(session.username)}`, {
          method: "PUT",
          headers,
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(await res.text());
      }

      if (session.role === "parent") {
        const payload = {
          parentsId: session.username,
          parentsName: form.name,
          parentsPhoneNumber: form.phone,
        };
        const res = await fetch(`${API_BASE}/api/parents/${encodeURIComponent(session.username)}`, {
          method: "PUT",
          headers,
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(await res.text());
      }

      if (session.role === "teacher") {
        const n = Number(tchExtra.academyNumber);
        const academyNumber = Number.isFinite(n) && n > 0 ? n : undefined;

        const payload = {
          teacherId: session.username,
          teacherName: form.name,
          teacherPhoneNumber: form.phone,
          ...(academyNumber != null ? { academyNumber } : {}),
        };
        const res = await fetch(`${API_BASE}/api/teachers/${encodeURIComponent(session.username)}`, {
          method: "PUT",
          headers,
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(await res.text());

      }

      if (session.role === "director") {
        const nums = toNums(dirExtra.academyNumbersText);
        const payload = {
          directorId: session.username,
          directorName: form.name,
          directorPhoneNumber: form.phone,
          academyNumbers: nums,
        };
        const res = await fetch(`${API_BASE}/api/directors/${encodeURIComponent(session.username)}`, {
          method: "PUT",
          headers,
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(await res.text());

      }

      // 세션 업데이트
      const next = {
        ...session,
        name: form.name,
        phone: form.phone,
        ...(session.role === "teacher"
          ? {
              academyNumbers: (() => {
                const n = Number(tchExtra.academyNumber);
                return Number.isFinite(n) && n > 0 ? [n] : [];
              })(),
            }
          : {}),
        ...(session.role === "director" ? { academyNumbers: toNums(dirExtra.academyNumbersText) } : {}),
      };
      localStorage.setItem("login", JSON.stringify(next));
      setSession(next);

      setMsg("저장되었습니다.");
      setTimeout(() => setMsg(null), 1800);
    } catch (e: any) {
      setErr(e?.message || "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  // 응답 본문 안전 추출(HTML 404일 때도 문자열로 에러 메시지 만들기 위함)
  async function safeText(res: Response) {
    try {
      return await res.text();
    } catch {
      return `HTTP ${res.status}`;
    }
  }

  if (loading || !session) return null;

  const roleLabel =
    session.role === "student" ? "학생" : session.role === "parent" ? "학부모" : session.role === "teacher" ? "교사" : "원장";

  return (
    <main className="max-w-4xl mx-auto p-6 space-y-6">
      {/* 상단 헤더 + 대시보드로 버튼 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">개인정보 수정</h1>
          <p className="text-sm text-gray-200 mt-1">
            역할: <span className="font-semibold">{roleLabel}</span> / 아이디:{" "}
            <span className="font-semibold">{session.username}</span>
          </p>
        </div>
      </div>

      {msg && (
        <div className="text-sm text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-2 rounded-lg">{msg}</div>
      )}
      {err && (
        <div className="text-sm text-red-600 bg-red-50 border border-red-200 px-3 py-2 rounded-lg">{err}</div>
      )}

      {/* 기본 정보 */}
      <section className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">기본 정보</h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Field label="이름" value={form.name} onChange={(v) => setForm((p) => ({ ...p, name: v }))} />
          <ReadOnly label="아이디(수정불가)" value={session.username} />

          <Field
            label="연락처"
            value={form.phone}
            onChange={(v) => setForm((p) => ({ ...p, phone: v }))}
            placeholder="010-0000-0000"
          />
          <ReadOnly label="역할" value={roleLabel} />
        </div>
      </section>

      {/* 역할별 섹션 */}
      {session.role === "student" && (
        <section className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">학생 정보</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="주소" value={stuExtra.address ?? ""} onChange={(v) => setStuExtra((p) => ({ ...p, address: v }))} />
            <Field label="학교" value={stuExtra.school ?? ""} onChange={(v) => setStuExtra((p) => ({ ...p, school: v }))} />
            <Field
              type="number"
              label="학년"
              value={String(stuExtra.grade ?? "")}
              onChange={(v) => setStuExtra((p) => ({ ...p, grade: v }))}
            />
            <Field label="성별" value={stuExtra.gender ?? ""} onChange={(v) => setStuExtra((p) => ({ ...p, gender: v }))} />
          </div>
        </section>
      )}

      {session.role === "teacher" && (
        <section className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">교사 정보</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field
              type="number"
              label="학원 번호"
              value={String(tchExtra.academyNumber ?? "")}
              onChange={(v) => setTchExtra((p) => ({ ...p, academyNumber: v }))}
            />
          </div>
        </section>
      )}

      {session.role === "director" && (
        <section className="rounded-2xl bg-white ring-1 ring-black/5 shadowsm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">원장 정보</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field
              label="학원 번호들"
              value={dirExtra.academyNumbersText ?? ""}
              onChange={(v) => setDirExtra((p) => ({ ...p, academyNumbersText: v }))}
              placeholder="예: 101,202"
            />
          </div>
        </section>
      )}

      {/* 저장 + 계정탈퇴 */}
      <div className="flex flex-wrap items-center gap-3">
        <button
          onClick={onSave}
          disabled={saving || !dirty}
          className="px-5 h-11 rounded-xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600 disabled:opacity-50"
        >
          {saving ? "저장 중…" : "변경사항 저장"}
        </button>
        <Link
          href="/account/delete"
          className="inline-flex w-32 h-11 items-center justify-center rounded-xl border-2 border-red-600 text-red-600 font-semibold hover:bg-red-600 hover:text-white transition"
        >
          계정탈퇴
        </Link>
      </div>
    </main>
  );
}

/* 재사용 입력 컴포넌트 */
function Field({
  label,
  value,
  onChange,
  placeholder,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  type?: "text" | "number";
}) {
  return (
    <label className="block">
      <span className="block text-sm text-gray-900 mb-1">{label}</span>
      <input
        type={type}
        className="w-full h-11 rounded-xl border border-gray-300 px-3 outline-none bg-white text-gray-900 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300 placeholder-gray-400"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
      />
    </label>
  );
}

function ReadOnly({ label, value }: { label: string; value: string }) {
  return (
    <label className="block">
      <span className="block text-sm text-gray-900 mb-1">{label}</span>
      <input className="w-full h-11 rounded-xl border border-gray-300 px-3 bg-gray-50 text-gray-900" value={value} readOnly />
    </label>
  );
}
