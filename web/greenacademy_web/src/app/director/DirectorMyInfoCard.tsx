// C:\project\103Team-sub\web\greenacademy_web\src\app\director\DirectorMyInfoCard.tsx
"use client";

import React from "react";
import { getSession } from "@/app/lib/session";
import { useRouter } from "next/navigation";

/** ==== 타입 (프로필 페이지 Session과 동일하게 맞춤) ==== */
type Role = "student" | "parent" | "teacher" | "director";
type Session = {
  role: Role;
  username: string;
  name?: string;
  phone?: string;
  token?: string;
  academyNumbers?: number[];
};

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

/** DELETE: 학원 삭제 */
async function deleteAcademy(academyNumber: number): Promise<void> {
  const token = getSession()?.token ?? null;
  const res = await fetch(`/backend/api/directors/academies/${encodeURIComponent(academyNumber)}`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
}

/** POST: 원장 전용 학원 생성 */
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

/** ✅ 프로필 페이지와 동일한 하드 새로고침 방식 */
function hardReload() {
  if (typeof window === "undefined") return;
  if (window.parent && window.parent !== window) {
    // iframe 안에서 열린 경우 → 부모 페이지 리로드
    window.parent.location.reload();
  } else {
    // 일반 케이스
    window.location.reload();
  }
}

/**
 * ✅ /api/directors/me 기준으로 최신 원장 정보를 읽어서
 *    localStorage("login")을 프로필 페이지 onSave 패턴과 동일하게 갱신
 */
async function syncLoginFromDirectorMe() {
  if (typeof window === "undefined") return;

  // 1) 서버 기준 내 정보 재조회
  const mine = await apiGet<DirectorMe>("/api/directors/me");

  // 2) 기존 login 세션 읽기
  const raw = localStorage.getItem("login");
  if (!raw) return;

  let cur: Session;
  try {
    cur = JSON.parse(raw) as Session;
  } catch {
    return;
  }

  // 3) 프로필 onSave와 동일한 패턴으로 next 세션 구성
  const next: Session = {
    ...cur,
    name: mine.name,
    phone: mine.phone,
    ...(cur.role === "director"
      ? {
          academyNumbers: Array.isArray(mine.academyNumbers) ? mine.academyNumbers : [],
        }
      : {}),
  };

  // 4) localStorage("login") 갱신
  localStorage.setItem("login", JSON.stringify(next));
}

/** ---- 공용: 프로필 수정 iframe 모달 ---- */
function ProfileEditModal({
  open,
  onClose,
  onSaved,
  src = "/settings/profile",
}: {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
  src?: string;
}) {
  React.useEffect(() => {
    if (!open) return;
    const handler = (e: MessageEvent) => {
      const data = e?.data;
      const ok = data === "profile:saved" || (data && typeof data === "object" && data.type === "profile:saved");
      if (ok) onSaved();
    };
    window.addEventListener("message", handler);
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
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
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-xl ring-1 ring-black/10 w-full max-w-3xl h-[80vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <h3 className="text-base font-semibold text-gray-900">내 정보 수정</h3>
          <button onClick={onClose} className="px-2 py-1 rounded hover:bg-gray-100">
            닫기
          </button>
        </div>
        <iframe title="profile-edit" src={src} className="w-full h-full" />
      </div>
    </div>
  );
}

export default function DirectorMyInfoCard() {
  const router = useRouter();

  const [me, setMe] = React.useState<DirectorMe | null>(null);
  const [academies, setAcademies] = React.useState<Academy[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [err, setErr] = React.useState<string | null>(null);

  const [editing, setEditing] = React.useState<number | null>(null);
  const [form, setForm] = React.useState<{ name?: string; address?: string; phone?: string }>({});
  const [saving, setSaving] = React.useState(false);

  const [deleting, setDeleting] = React.useState<number | null>(null);

  const [addOpen, setAddOpen] = React.useState(false);
  const [addName, setAddName] = React.useState("");
  const [addPhone, setAddPhone] = React.useState("");
  const [addAddress, setAddAddress] = React.useState("");
  const [adding, setAdding] = React.useState(false);
  const [addErr, setAddErr] = React.useState<string | null>(null);

  const [openEdit, setOpenEdit] = React.useState(false);
  const [refreshTick, setRefreshTick] = React.useState(0);

  /** 🔥 계정 삭제 이벤트 수신 */
  React.useEffect(() => {
    const handler = (e: MessageEvent) => {
      if (e.data === "account:deleted") {
        setOpenEdit(false);
        localStorage.removeItem("login");
        window.location.href = "/login";
      }
    };
    window.addEventListener("message", handler);
    return () => window.removeEventListener("message", handler);
  }, []);

  const reload = React.useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const mine = await apiGet<DirectorMe>("/api/directors/me");
      setMe(mine);

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

      // 1) 백엔드에 학원 정보 수정
      await patchAcademy(academyNumber, payload);

      // 2) 서버 기준 내 정보로 login 세션 동기화 (프로필 onSave 패턴과 동일)
      await syncLoginFromDirectorMe();

      // 3) 프로필 페이지와 동일하게 전체 페이지 리로드
      hardReload();
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
      if (!addName.trim()) return setAddErr("학원 이름을 입력하세요.");

      setAdding(true);

      // 1) 새 학원 생성 (서비스에서 Director.academyNumbers 도 함께 갱신)
      await postCreateAcademyForDirector(me.username, {
        name: addName.trim(),
        phone: addPhone.trim() || undefined,
        address: addAddress.trim() || undefined,
      });

      // 2) 서버 기준 내 정보로 login 세션 동기화
      await syncLoginFromDirectorMe();

      // 3) 전체 페이지 리로드
      hardReload();
    } catch (e: any) {
      setAddErr(e?.message ?? "생성 실패");
    } finally {
      setAdding(false);
    }
  };

  const handleDelete = async (academyNumber: number) => {
    if (!window.confirm("해당 학원을 삭제하시겠습니까?\n관련 데이터가 있다면 서버 정책에 따라 막힐 수 있습니다.")) return;

    try {
      setErr(null);
      setDeleting(academyNumber);

      // 1) 백엔드에서 학원 삭제
      await deleteAcademy(academyNumber);

      // 2) 서버 기준 내 정보로 login 세션 동기화
      await syncLoginFromDirectorMe();

      // 3) 전체 페이지 리로드
      hardReload();
    } catch (e: any) {
      setErr(e?.message || "삭제에 실패했습니다.");
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div className="space-y-6">
      {err && <div className="text-sm text-red-600 bg-red-50 border border-red-200 p-3 rounded-lg">{err}</div>}

      {/* 기본 정보 */}
      <section className="bg-white ring-1 ring-black/5 rounded-2xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-black">기본 정보</h2>
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
                className="px-3 py-2 rounded-lg border bg-white text-black"
                value={addName}
                onChange={(e) => setAddName(e.target.value)}
                placeholder="예) 103학"
              />
            </label>
            <label className="grid gap-1">
              <span className="text-xs text-gray-500">대표번호</span>
              <input
                className="px-3 py-2 rounded-lg border bg-white text-black"
                value={addPhone}
                onChange={(e) => setAddPhone(e.target.value)}
                placeholder="예) 8221234567"
              />
            </label>
            <label className="grid gap-1">
              <span className="text-xs text-gray-500">주소</span>
              <input
                className="px-3 py-2 rounded-lg border bg-white text-black"
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

      {/* 소속 학원 목록 */}
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
              const isDeleting = deleting === a.academyNumber;

              return (
                <div key={a.academyNumber} className="bg-white ring-1 ring-black/5 rounded-2xl p-5">
                  <div className="flex items-center justify-between">
                    <div className="text-base font-semibold text-black">
                      {isEdit ? (
                        <div className="flex flex-col gap-1">
                          <span className="text-xs text-gray-500">학원 이름</span>
                          <input
                            className="w-[14rem] rounded-lg border px-3 py-1 outline-none"
                            value={form.name ?? ""}
                            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                            placeholder="학원 이름"
                          />
                        </div>
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
                          />
                        ) : (
                          a.phone ?? "—"
                        )}
                      </dd>
                    </div>
                  </dl>

                  <div className="mt-4 flex gap-2 justify-end">
                    {!isEdit ? (
                      <>
                        <button
                          disabled={isDeleting}
                          onClick={() => handleDelete(a.academyNumber)}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-red-400 text-red-600 hover:bg-red-50"
                        >
                          {isDeleting ? "삭제 중…" : "삭제"}
                        </button>
                        <button
                          onClick={() => onEdit(a)}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50"
                        >
                          편집
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          disabled={saving || isDeleting}
                          onClick={onCancel}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50"
                        >
                          취소
                        </button>
                        <button
                          disabled={saving || isDeleting}
                          onClick={() => onSave(a.academyNumber)}
                          className="px-3 py-1.5 text-sm rounded-lg ring-1 ring-gray-300 hover:bg-gray-50"
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

      {/* 프로필 수정 모달 */}
      <ProfileEditModal
        open={openEdit}
        onClose={() => setOpenEdit(false)}
        onSaved={() => {
          setOpenEdit(false);
          setRefreshTick((t) => t + 1);
          hardReload();
        }}
        src="/settings/profile"
      />
    </div>
  );
}
