// C:\project\103Team-sub\web\greenacademy_web\src\app\director\DirectorMyInfoCard.tsx
"use client";

import React from "react";
import { getSession } from "@/app/lib/session";

/** 타입 */
type DirectorMe = {
  username: string;
  name: string;
  phone?: string;
  academyNumbers: number[];
};
type Academy = {
  academyNumber: number;
  name: string;
  address?: string;
  phone?: string;
};

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

/** POST: 원장 전용 학원 생성(랜덤 4자리 번호, 서버에서 중복 방지) */
async function postCreateAcademyForDirector(
  username: string,
  payload: { name: string; phone?: string; address?: string }
): Promise<Academy> {
  const token = getSession()?.token ?? null;
  const res = await fetch(`/backend/api/academy/directors/${encodeURIComponent(username)}`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`Academy create failed: ${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  return JSON.parse(text) as Academy;
}

/** ---- 공용: 프로필 수정 iframe 모달 ---- */
function ProfileEditModal({
  open,
  onClose,
  onSaved,
  src = "/settings/profile", // 기존 수정 화면을 그대로 사용 (라우팅 이동 대신 모달로)
}: {
  open: boolean;
  onClose: () => void;
  onSaved: () => void; // 저장 완료 시 콜백(데이터 재조회)
  src?: string;
}) {
  React.useEffect(() => {
    if (!open) return;
    const handler = (e: MessageEvent) => {
      // 동일 출처 확인(필요 시 e.origin === window.location.origin 강화)
      const data = e?.data;
      const ok = data === "profile:saved" || (data && typeof data === "object" && data.type === "profile:saved");
      if (ok) {
        onSaved();
      }
    };
    window.addEventListener("message", handler);
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("message", handler);
      document.removeEventListener("keydown", onKey);
    };
  }, [open, onClose, onSaved]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-label="내 정보 수정"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-xl ring-1 ring-black/10 w-full max-w-3xl h-[80vh] overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <h3 className="text-base font-semibold text-gray-900">내 정보 수정</h3>
          <button
            onClick={onClose}
            className="rounded-lg px-2 py-1 text-sm text-gray-700 hover:bg-gray-100"
            aria-label="닫기"
            type="button"
          >
            닫기
          </button>
        </div>
        <iframe
          title="profile-edit"
          src={src}
          className="w-full h-full"
          // 저장 화면에서 window.parent.postMessage('profile:saved','*') 호출 필요
        />
      </div>
    </div>
  );
}

export default function DirectorMyInfoCard() {
  const [me, setMe] = React.useState<DirectorMe | null>(null);
  const [academies, setAcademies] = React.useState<Academy[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [err, setErr] = React.useState<string | null>(null);

  // 편집 상태
  const [editing, setEditing] = React.useState<number | null>(null);
  const [form, setForm] = React.useState<{ name?: string; address?: string; phone?: string }>({});
  const [saving, setSaving] = React.useState(false);

  // 새 학원 추가 상태
  const [addOpen, setAddOpen] = React.useState(false);
  const [addName, setAddName] = React.useState("");
  const [addPhone, setAddPhone] = React.useState("");
  const [addAddress, setAddAddress] = React.useState("");
  const [adding, setAdding] = React.useState(false);
  const [addErr, setAddErr] = React.useState<string | null>(null);

  // 프로필 수정 모달
  const [openEdit, setOpenEdit] = React.useState(false);
  const [refreshTick, setRefreshTick] = React.useState(0); // 저장 후 재조회 트리거

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
  }, [reload, refreshTick]);

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

  const handleAdd = async () => {
    try {
      setAddErr(null);
      if (!me) throw new Error("세션 정보가 없습니다.");
      if (!addName.trim()) {
        setAddErr("학원 이름을 입력하세요.");
        return;
      }
      setAdding(true);
      const created = await postCreateAcademyForDirector(me.username, {
        name: addName.trim(),
        phone: addPhone.trim() || undefined,
        address: addAddress.trim() || undefined,
      });

      // 목록/내 정보에 즉시 반영
      setAcademies((prev) => [
        {
          academyNumber: created.academyNumber,
          name: created.name,
          address: created.address,
          phone: created.phone,
        },
        ...prev,
      ]);
      setMe((prev) =>
        prev
          ? { ...prev, academyNumbers: [...new Set([created.academyNumber, ...(prev.academyNumbers || [])])] }
          : prev
      );

      // 폼 리셋
      setAddName("");
      setAddPhone("");
      setAddAddress("");
      setAddOpen(false);
    } catch (e: any) {
      setAddErr(e?.message ?? "생성 실패");
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="space-y-6">
      {err && <div className="text-sm text-red-600 bg-red-50 border border-red-200 p-3 rounded-lg">{err}</div>}

      {/* 기본 정보 */}
      <section className="bg-white ring-1 ring-black/5 rounded-2xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-black">기본 정보</h2>
          {/* ▶ 우상단 버튼: 수정 모달 열기 (기존 router.push 대체) */}
          <button
            onClick={() => setOpenEdit(true)}
            className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50 text-black"
            type="button"
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

      {/* 새 학원 추가 */}
      <section className="bg-white ring-1 ring-black/5 rounded-2xl p-6">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-semibold text-black">새 학원 추가</h3>
          <button
            className="text-sm underline text-black"
            onClick={() => setAddOpen((v) => !v)}
            aria-expanded={addOpen}
            type="button"
          >
            {addOpen ? "닫기" : "열기"}
          </button>
        </div>

        {addOpen && (
          <div className="mt-3 grid gap-3">
            <label className="grid gap-1">
              <span className="text-xs text-gray-500">학원 이름 *</span>
              <input
                className="px-3 py-2 rounded-lg border bg-white text-black outline-none"
                value={addName}
                onChange={(e) => setAddName(e.target.value)}
                placeholder="예) 103학"
              />
            </label>
            <label className="grid gap-1">
              <span className="text-xs text-gray-500">대표번호</span>
              <input
                className="px-3 py-2 rounded-lg border bg-white text-black outline-none"
                value={addPhone}
                onChange={(e) => setAddPhone(e.target.value)}
                placeholder="예) 8221234567"
              />
            </label>
            <label className="grid gap-1">
              <span className="text-xs text-gray-500">주소</span>
              <input
                className="px-3 py-2 rounded-lg border bg-white text-black outline-none"
                value={addAddress}
                onChange={(e) => setAddAddress(e.target.value)}
                placeholder="예) 인천광역시 중"
              />
            </label>
            {addErr && <p className="text-sm text-red-600">{addErr}</p>}
            <div>
              <button
                onClick={handleAdd}
                disabled={adding}
                className="px-4 py-2 rounded-lg bg-emerald-500/90 hover:bg-emerald-500 text-white disabled:opacity-60"
                type="button"
              >
                {adding ? "추가 중…" : "학원 추가"}
              </button>
            </div>
          </div>
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
                        type="button"
                      >
                        편집
                      </button>
                    ) : (
                      <>
                        <button
                          disabled={saving}
                          onClick={onCancel}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50 text-black disabled:opacity-50"
                          type="button"
                        >
                          취소
                        </button>
                        <button
                          disabled={saving}
                          onClick={() => onSave(a.academyNumber)}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50 text-black disabled:opacity-50"
                          type="button"
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

      {/* 프로필 수정 모달 (기존 /settings/profile 화면을 iframe으로 띄움) */}
      <ProfileEditModal
        open={openEdit}
        onClose={() => setOpenEdit(false)}
        onSaved={() => {
          setOpenEdit(false);
          setRefreshTick((t) => t + 1); // 저장 후 즉시 재조회
        }}
        src="/settings/profile"
      />
    </div>
  );
}
