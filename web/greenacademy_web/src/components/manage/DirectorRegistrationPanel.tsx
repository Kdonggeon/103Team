"use client";

import React, { useEffect, useMemo, useState } from "react";
import { getSession } from "@/app/lib/session";

type LoginSession = {
  username: string;
  role?: string;
  token?: string;
  academyNumbers?: Array<number | string>;
};

type Req = {
  id: string;
  academyNumber: number;
  requesterId: string;
  requesterRole: string;
  memo?: string;
  status: string;
  createdAt?: string;
  processedMemo?: string;
};

const API_PREFIX = process.env.NEXT_PUBLIC_BACKEND_PREFIX || "/backend";
const buildUrl = (path: string) => {
  const base = API_PREFIX.endsWith("/") ? API_PREFIX.slice(0, -1) : API_PREFIX;
  const suffix = path.startsWith("/") ? path : `/${path}`;
  return `${base}${suffix}`;
};

async function apiGet<T>(path: string, token?: string): Promise<T> {
  const url = buildUrl(path);
  const r = await fetch(url, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    cache: "no-store",
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}

async function apiPost<T>(path: string, body: any, token?: string): Promise<T> {
  const url = buildUrl(path);
  const r = await fetch(url, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body ?? {}),
  });
  if (!r.ok) {
    const txt = await r.text().catch(() => "");
    throw new Error(`${r.status} ${r.statusText}${txt ? " | " + txt : ""}`);
  }
  const txt = await r.text().catch(() => "");
  return txt ? (JSON.parse(txt) as T) : (undefined as unknown as T);
}

function fmtDate(v?: string) {
  if (!v) return "";
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return v;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

export default function DirectorRegistrationPanel() {
  const session = useMemo(() => getSession() as LoginSession | null, []);
  const token = session?.token ?? "";
  const academies = useMemo(
    () =>
      Array.isArray(session?.academyNumbers)
        ? session!.academyNumbers!
            .map((n) => Number(n))
            .filter((n) => Number.isFinite(n))
        : [],
    [session]
  );
  const [academy, setAcademy] = useState<number | null>(academies[0] ?? null);

  const [list, setList] = useState<Req[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [memo, setMemo] = useState<string>("");
  const [processing, setProcessing] = useState<string | null>(null);
  const [hasData, setHasData] = useState(false);

  useEffect(() => {
    setAcademy((prev) => {
      if (prev != null && academies.includes(prev)) return prev;
      return academies[0] ?? null;
    });
  }, [academies]);

  const load = async () => {
    if (academy == null) {
      setList([]);
      setHasData(false);
      return;
    }
    setLoading(true);
    setErr(null);
    try {
      const rows = await apiGet<Req[]>(
        `/api/academy-requests?scope=director&academyNumber=${encodeURIComponent(academy)}&status=PENDING`,
        token
      );
      const arr = Array.isArray(rows) ? rows : [];
      setList(arr);
      setHasData(arr.length > 0);
    } catch (e: any) {
      const msg = e?.message ?? "";
      if (msg.startsWith("500")) {
        setErr("승인 요청 목록을 불러오지 못했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요.");
      } else {
        setErr(msg || "승인 요청 목록을 불러오지 못했습니다.");
      }
      setList([]);
      setHasData(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [academy]);

  const process = async (id: string, action: "approve" | "reject") => {
    if (!id) return;
    setProcessing(id);
    try {
      await apiPost(`/api/academy-requests/${encodeURIComponent(id)}/${action}`, {
        processedBy: session?.username,
        memo,
      }, token);
      setMemo("");
      await load();
    } catch (e: any) {
      alert(e?.message ?? `${action} 실패`);
    } finally {
      setProcessing(null);
    }
  };

  if (!session || session.role?.toLowerCase() !== "director") {
    return <div className="p-6 text-sm text-gray-700">원장 계정으로 로그인해 주세요.</div>;
  }

  if (!academies.length) {
    return <div className="p-6 text-sm text-gray-700">등록된 학원번호가 없어 승인 요청을 표시할 수 없습니다.</div>;
  }

  return (
    <div className="rounded-2xl border bg-white shadow-sm ring-1 ring-black/5">
      <div className="px-5 py-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">등록 관리</h2>
          <p className="text-sm text-gray-900">학원 연결 승인 요청을 처리하세요.</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <label className="text-sm text-gray-900">학원번호</label>
          <select
            value={academy ?? ""}
            onChange={(e) => setAcademy(e.target.value ? Number(e.target.value) : null)}
            className="rounded-lg border px-3 py-1.5 text-sm text-black"
          >
            {academies.map((n) => (
              <option key={n} value={n}>#{n}</option>
            ))}
          </select>
          {loading && <span className="text-xs text-gray-900">불러오는 중…</span>}
        </div>
      </div>

      {err && <div className="mx-5 mb-4 rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {err}</div>}

      <div className="px-5 pb-5 space-y-3">
        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-900">처리 메모(선택)</label>
          <input
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
            className="rounded-lg border px-3 py-1.5 text-sm text-black w-56"
            placeholder="메모 입력"
          />
        </div>

        {(!hasData && !loading && !err) ? (
          <div className="text-sm text-gray-900">대기 중인 요청이 없습니다.</div>
        ) : (
          <div className="divide-y">
            {list.map((r) => (
              <div key={r.id} className="py-3 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-sm font-semibold text-gray-900 flex items-center gap-2">
                    <span>학원 #{r.academyNumber}</span>
                    <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 ring-1 ring-blue-200">
                      {r.requesterRole} · {r.requesterId}
                    </span>
                  </div>
                  <div className="text-xs text-gray-900 mt-0.5">
                    생성: {fmtDate(r.createdAt) || "—"} {r.memo ? `· ${r.memo}` : ""}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => process(r.id, "approve")}
                    disabled={processing === r.id}
                    className="px-3 py-1.5 rounded-lg text-sm bg-emerald-600 text-white disabled:opacity-50"
                  >
                    승인
                  </button>
                  <button
                    onClick={() => process(r.id, "reject")}
                    disabled={processing === r.id}
                    className="px-3 py-1.5 rounded-lg text-sm bg-rose-600 text-white disabled:opacity-50"
                  >
                    거절
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
