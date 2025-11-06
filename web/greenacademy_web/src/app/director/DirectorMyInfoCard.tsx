"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { getSession } from "@/app/lib/session";

/** 타입 */
type DirectorMe = {
  username: string;
  name: string;
  phone?: string;
  academyNumbers: number[];
};
type Academy = { academyNumber: number; name: string; address?: string; phone?: string };

/** /backend 프록시 + Authorization 자동 주입(GET 전용) */
async function apiGet<T>(path: string): Promise<T> {
  const session = getSession();
  const token = session?.token ?? null;
  const url = path.startsWith("/backend") ? path : `/backend${path}`;
  const res = await fetch(url, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  return text ? (JSON.parse(text) as T) : ({} as T);
}

/** PATCH: 학원 정보 수정 */
async function patchAcademy(
  academyNumber: number,
  payload: { name?: string; address?: string; phone?: string }
): Promise<Academy | null> {
  const token = getSession()?.token ?? null;
  const res = await fetch(`/backend/api/directors/academies/${encodeURIComponent(academyNumber)}`, {
    method: "PATCH",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  return text ? (JSON.parse(text) as Academy) : null;
}

export default function DirectorMyInfoCard() {
  const router = useRouter();

  const [me, setMe] = React.useState<DirectorMe | null>(null);
  const [academies, setAcademies] = React.useState<Academy[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [err, setErr] = React.useState<string | null>(null);

  // 편집 상태
  const [editing, setEditing] = React.useState<number | null>(null);
  const [form, setForm] = React.useState<{ name?: string; address?: string; phone?: string }>({});
  const [saving, setSaving] = React.useState(false);

  const reload = React.useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      // 1) 원장 본인 정보
      const mine = await apiGet<DirectorMe>("/api/directors/me");
      setMe(mine);

      // 2) 소속 학원 정보
      if (mine.academyNumbers?.length) {
        const q = encodeURIComponent(mine.academyNumbers.join(","));
        const acads = await apiGet<Academy[]>(`/api/directors/academies?numbers=${q}`);
        setAcademies(acads);
      } else {
        setAcademies([]);
      }
    } catch (e: any) {
      setErr(e?.message || "정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    reload();
  }, [reload]);

  const onEdit = (a: Academy) => {
    setEditing(a.academyNumber);
    setForm({ name: a.name ?? "", address: a.address ?? "", phone: a.phone ?? "" });
  };

  const onCancel = () => {
    setEditing(null);
    setForm({});
  };

  const onSave = async (academyNumber: number) => {
    try {
      setSaving(true);
      setErr(null);
      const payload: Record<string, string> = {};
      if (form.name != null) payload.name = form.name.trim();
      if (form.address != null) payload.address = form.address.trim();
      if (form.phone != null) payload.phone = form.phone.trim();
      await patchAcademy(academyNumber, payload);
      await reload();
      setEditing(null);
      setForm({});
    } catch (e: any) {
      setErr(e?.message || "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      {err && <div className="text-sm text-red-600 bg-red-50 border border-red-200 p-3 rounded-lg">{err}</div>}

      {/* 기본 정보 */}
      <section className="bg-white ring-1 ring-black/5 rounded-2xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-black">기본 정보</h2>
          {/* ▶ 우상단 버튼: 정보 수정 화면으로 이동 */}
          <button
            onClick={() => router.push("/settings/profile")}
            className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50 text-black"
          >
            정보 수정하기
          </button>
        </div>

        {loading ? (
          <div className="text-sm text-gray-700">불러오는 중…</div>
        ) : (
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3">
            <div>
              <dt className="text-xs text-gray-500">아이디</dt>
              <dd className="text-sm text-black">{me?.username ?? "-"}</dd>
            </div>
            <div>
              <dt className="text-xs text-gray-500">이름</dt>
              <dd className="text-sm text-black">{me?.name ?? "-"}</dd>
            </div>
            <div>
              <dt className="text-xs text-gray-500">연락처</dt>
              {/* 요청: 전화번호 ‘검정’ 고정 */}
              <dd className="text-sm text-black">{me?.phone ?? "-"}</dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="text-xs text-gray-500">소속 학원 번호</dt>
              <dd className="mt-1">
                {me?.academyNumbers?.length ? (
                  <div className="flex flex-wrap gap-1.5">
                    {me.academyNumbers.map((n, i) => (
                      <span
                        key={`${n}-${i}`}
                        className="inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium bg-gray-100 text-gray-800 ring-1 ring-gray-200"
                      >
                        #{n}
                      </span>
                    ))}
                  </div>
                ) : (
                  <span className="text-sm text-gray-500">—</span>
                )}
              </dd>
            </div>
          </dl>
        )}
      </section>

      {/* 소속 학원 (편집 가능) */}
      <section className="space-y-3">
        <h3 className="text-lg font-bold text-black">소속 학원</h3>
        {loading ? (
          <div className="text-sm text-gray-700">불러오는 중…</div>
        ) : academies.length === 0 ? (
          <div className="bg-white ring-1 ring-black/5 rounded-2xl p-6 text-black">소속 학원 정보가 없습니다.</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {academies.map((a) => {
              const isEdit = editing === a.academyNumber;
              return (
                <div key={a.academyNumber} className="bg-white ring-1 ring-black/5 rounded-2xl p-5">
                  <div className="flex items-center justify-between">
                    <div className="text-base font-semibold text-black">
                      {isEdit ? (
                        <input
                          className="w-[14rem] rounded-lg border px-3 py-1 outline-none"
                          value={form.name ?? ""}
                          onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                          placeholder="학원명"
                        />
                      ) : (
                        a.name || "—"
                      )}
                    </div>
                    <span className="text-xs text-gray-600">#{a.academyNumber}</span>
                  </div>

                  <dl className="mt-3 grid grid-cols-1 gap-y-2">
                    <div>
                      <dt className="text-xs text-gray-500">주소</dt>
                      <dd className="text-sm text-black">
                        {isEdit ? (
                          <input
                            className="w-full rounded-lg border px-3 py-1 outline-none"
                            value={form.address ?? ""}
                            onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
                            placeholder="주소"
                          />
                        ) : (
                          a.address ?? "—"
                        )}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-xs text-gray-500">대표번호</dt>
                      {/* 요청: 전화번호 ‘검정’ 고정 */}
                      <dd className="text-sm text-black">
                        {isEdit ? (
                          <input
                            className="w-full rounded-lg border px-3 py-1 outline-none"
                            value={form.phone ?? ""}
                            onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
                            placeholder="숫자/하이픈 자유 입력"
                          />
                        ) : (
                          a.phone ?? "—"
                        )}
                      </dd>
                    </div>
                  </dl>

                  <div className="mt-4 flex gap-2 justify-end">
                    {!isEdit ? (
                      <button
                        onClick={() => onEdit(a)}
                        className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50 text-black"
                      >
                        편집
                      </button>
                    ) : (
                      <>
                        <button
                          disabled={saving}
                          onClick={onCancel}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50 text-black disabled:opacity-50"
                        >
                          취소
                        </button>
                        <button
                          disabled={saving}
                          onClick={() => onSave(a.academyNumber)}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50 text-black disabled:opacity-50"
                        >
                          {saving ? "저장 중…" : "저장"}
                        </button>
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
