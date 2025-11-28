// src/app/student/StudentProfileCard.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";

/** ===== 타입/유틸 ===== */
type Role = "parent" | "student" | "teacher" | "director";
type LoginSession = { role: Role; username: string; name?: string; token?: string; academyNumbers?: number[] };
type StudentDetail = Record<string, any>;
type AcademyRequest = {
  id: string;
  academyNumber: number;
  status: "PENDING" | "APPROVED" | "REJECTED" | string;
  memo?: string;
  processedMemo?: string;
  processedBy?: string;
  createdAt?: string;
  updatedAt?: string;
};

const API_BASE = "/backend";


async function apiGet<T>(path: string, token?: string): Promise<T> {
  const r = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    cache: "no-store",
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}
async function apiPost<T>(path: string, body: any, token?: string): Promise<T> {
  const r = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(body),
    cache: "no-store",
  });
  if (!r.ok) {
    const txt = await r.text().catch(() => "");
    throw new Error(`${r.status} ${r.statusText}${txt ? " | " + txt : ""}`);
  }
  const txt = await r.text().catch(() => "");
  return txt ? (JSON.parse(txt) as T) : (undefined as unknown as T);
}
function readLogin(): LoginSession | null {
  try {
    const raw = localStorage.getItem("login");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}
function isEmpty(v: any) {
  if (v === null || v === undefined) return true;
  if (typeof v === "string" && v.trim() === "") return true;
  if (Array.isArray(v) && v.length === 0) return true;
  if (typeof v === "object" && !Array.isArray(v) && Object.keys(v).length === 0) return true;
  return false;
}
function pick(obj: StudentDetail, keys: string[]) {
  for (const k of keys) if (k in obj && !isEmpty(obj[k])) return obj[k];
  return undefined;
}
function formatAcademies(v: any) {
  const arr = Array.isArray(v) ? v : [v];
  const nums = arr.map((x) => Number(x)).filter(Number.isFinite);
  return nums.length ? nums.map((n) => `#${n}`).join(", ") : (isEmpty(v) ? "" : String(v));
}

/** ===== 공통 UI ===== */
function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-black">
      <svg className="h-4 w-4 animate-spin text-black" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-90" d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
      </svg>
      {label && <span>{label}</span>}
    </div>
  );
}
function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-200 px-4 py-3">
      <div className="text-[11px] text-gray-600">{label}</div>
      <div className="text-sm font-medium text-gray-900 mt-0.5 break-words">{value}</div>
    </div>
  );
}

/** ---- 프로필 수정 iframe 모달 (기존 /settings/profile 재사용) ---- */
function ProfileEditModal({
  open,
  onClose,
  onSaved,
  src = "/settings/profile",
}: {
  open: boolean;
  onClose: () => void;
  onSaved: () => void; // 저장 완료 시 데이터 재조회
  src?: string;
}) {
  useEffect(() => {
    if (!open) return;
    const handler = (e: MessageEvent) => {
      const data = e?.data;
      const ok = data === "profile:saved" || (data && typeof data === "object" && data.type === "profile:saved");
      if (ok) onSaved();
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("message", handler);
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
          title="student-profile-edit"
          src={src}
          className="w-full h-full"
          // 저장 완료 시 /settings/profile 내부에서:
          // window.parent.postMessage('profile:saved', '*')
        />
      </div>
    </div>
  );
}

/** ===== 메인 ===== */
export default function StudentProfileCard() {
  const login = readLogin();
  const token = login?.token ?? "";
  const studentId = login?.username ?? "";

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [detail, setDetail] = useState<StudentDetail | null>(null);
  const [academyInput, setAcademyInput] = useState("");
  const [requests, setRequests] = useState<AcademyRequest[]>([]);
  const [reqMsg, setReqMsg] = useState<string | null>(null);
  const [reqErr, setReqErr] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // 모달 열림/저장 후 갱신을 위한 tick
  const [openEdit, setOpenEdit] = useState(false);
  const [refreshTick, setRefreshTick] = useState(0);

  useEffect(() => {
    if (!studentId) return;
    let aborted = false;
    (async () => {
      setLoading(true);
      setErr(null);
      try {
        const d = await apiGet<StudentDetail>(`/api/students/${encodeURIComponent(studentId)}`, token);
        if (!aborted) setDetail(d ?? null);
      } catch (e: any) {
        if (!aborted) setErr(e?.message ?? "정보를 불러오지 못했습니다.");
      } finally {
        if (!aborted) setLoading(false);
      }
    })();
    return () => {
      aborted = true;
    };
  }, [studentId, token, refreshTick]);

  const academies: number[] = useMemo(() => {
    const v = detail?.Academy_Numbers ?? detail?.academyNumbers ?? detail?.academies ?? login?.academyNumbers ?? [];
    const arr = Array.isArray(v) ? v : [v];
    return Array.from(new Set(arr.map((x) => Number(x)).filter(Number.isFinite)));
  }, [detail, login]);

  // 내 승인 요청 목록
  useEffect(() => {
    if (!studentId) return;
    let aborted = false;
    (async () => {
      try {
        const list = await apiGet<AcademyRequest[]>(
          `/api/academy-requests?scope=mine&requesterId=${encodeURIComponent(studentId)}&requesterRole=student`,
          token
        );
        if (!aborted) setRequests(Array.isArray(list) ? list : []);
      } catch {
        if (!aborted) setRequests([]);
      }
    })();
    return () => { aborted = true; };
  }, [studentId, token]);

  const submitRequest = async () => {
    const raw = academyInput.trim();
    const num = Number(raw);
    if (!Number.isFinite(num)) { setReqErr("숫자만 입력하세요."); setReqMsg(null); return; }
    setSaving(true); setReqMsg(null); setReqErr(null);
    try {
      await apiPost(`/api/academy-requests`, {
        academyNumber: num,
        requesterId: studentId,
        requesterRole: "student",
        memo: "학생 본인 학원 연결 요청",
      }, token);
      setReqMsg("승인 요청을 등록했습니다."); setAcademyInput("");
      const list = await apiGet<AcademyRequest[]>(
        `/api/academy-requests?scope=mine&requesterId=${encodeURIComponent(studentId)}&requesterRole=student`,
        token
      );
      setRequests(Array.isArray(list) ? list : []);
    } catch (e: any) {
      setReqErr(e?.message ?? "승인 요청에 실패했습니다.");
    } finally {
      setSaving(false);
      setTimeout(() => setReqMsg(null), 1500);
    }
  };

  const entries = useMemo(() => {
    if (!detail) return [];
    const out: { label: string; value: string }[] = [];
    const push = (label: string, raw: any, fmt?: (v: any) => string) => {
      if (raw === undefined) return;
      const value = fmt ? fmt(raw) : typeof raw === "object" ? JSON.stringify(raw) : String(raw);
      if (!isEmpty(value)) out.push({ label, value });
    };
    push("이름", pick(detail, ["Student_Name", "studentName", "student_name", "name"]));
    push("주소", pick(detail, ["Student_Address", "studentAddress", "student_address", "address"]));
    push("핸드폰 번호", pick(detail, ["Student_Phone_Number", "studentPhoneNumber", "student_phone_number", "phone"]));
    push("학교", pick(detail, ["School", "school", "schoolName", "school_name"]));
    push("학년", pick(detail, ["Grade", "grade", "year"]));
    push("성별", pick(detail, ["Gender", "gender", "sex"]));
    // 학원번호는 보기만
    push("학원번호", academies, formatAcademies);
    return out;
  }, [detail, academies]);

  return (
    <div className="space-y-5">
      {/* 헤더 */}
      <div className="rounded-2xl bg-gradient-to-r from-emerald-50 to-white ring-1 ring-black/5 p-5 md:p-6 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 ring-1 ring-emerald-200 text-emerald-700 font-semibold">
            {login?.name?.[0]?.toUpperCase() ?? login?.username?.[0]?.toUpperCase() ?? "S"}
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">내 정보</h2>
            <div className="text-sm text-gray-600">아이디: {studentId}</div>
          </div>
        </div>
        <button
          onClick={() => setOpenEdit(true)}
          className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm hover:bg-emerald-700 active:scale-[0.99] transition"
          type="button"
        >
          내 정보 수정
        </button>
      </div>

      {/* 본문 */}
      <section className="rounded-2xl border bg-white p-5 md:p-6 shadow-sm ring-1 ring-black/5">
        {loading ? (
          <Spinner label="불러오는 중…" />
        ) : err ? (
          <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {err}</div>
        ) : !detail ? (
          <div className="text-sm text-gray-700">표시할 학생 정보가 없습니다.</div>
        ) : entries.length === 0 ? (
          <div className="text-sm text-gray-700">표시할 학생 정보가 없습니다.</div>
        ) : (
          <div className="grid grid-cols-1 gap-3">
            {entries.map((e, i) => (
              <Info key={i} label={e.label} value={e.value} />
            ))}
          </div>
        )}

        {/* 학원번호 추가(승인 요청) */}
        <div className="mt-6 rounded-2xl border border-gray-200 p-4">
          <div className="text-sm font-semibold text-gray-900 mb-2">학원 연결 요청</div>
          <div className="flex flex-wrap items-end gap-3">
            <div className="w-full sm:w-auto sm:max-w-[180px]">
              <label className="block text-xs text-gray-600">학원번호</label>
              <input
                value={academyInput}
                onChange={(e) => setAcademyInput(e.target.value)}
                placeholder="예) 103"
                className="mt-1 w-full rounded-xl border border-black/30 px-3 py-2 text-sm text-black placeholder-black/50 ring-1 ring-black/20 focus:outline-none focus:ring-2 focus:ring-black"
                inputMode="numeric"
              />
            </div>
            <button
              onClick={submitRequest}
              type="button"
              className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm disabled:opacity-50 hover:bg-emerald-700 active:scale-[0.99] transition"
              disabled={!academyInput || saving}
            >
              {saving ? "요청 중…" : "요청"}
            </button>
            {reqErr && <span className="text-sm text-red-700 bg-red-50 ring-1 ring-red-200 rounded px-2 py-1">{reqErr}</span>}
            {reqMsg && <span className="text-sm text-emerald-700 bg-emerald-50 ring-1 ring-emerald-200 rounded px-2 py-1">{reqMsg}</span>}
          </div>

          <div className="mt-3">
            <div className="text-xs font-medium text-gray-900 mb-1">내 승인 요청</div>
            {!requests.length ? (
              <div className="text-sm text-gray-600">대기 중인 요청이 없습니다.</div>
            ) : (
              <div className="space-y-2">
                {requests.map((r) => (
                  <div key={r.id} className="rounded-xl bg-white ring-1 ring-gray-200 px-3 py-2 flex items-center justify-between text-sm">
                    <div>
                      <div className="font-semibold text-gray-900">학원 #{r.academyNumber}</div>
                      <div className="text-xs text-gray-600">
                        {r.status === "PENDING" ? "대기" : r.status === "APPROVED" ? "승인" : "거절"}
                        {r.processedMemo ? ` · ${r.processedMemo}` : ""}
                      </div>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-xs ${
                      r.status === "APPROVED"
                        ? "bg-emerald-100 text-emerald-700"
                        : r.status === "REJECTED"
                        ? "bg-rose-100 text-rose-700"
                        : "bg-amber-100 text-amber-700"
                    }`}>
                      {r.status === "PENDING" ? "대기" : r.status === "APPROVED" ? "승인" : "거절"}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>

      {/* 프로필 수정 모달: 기존 /settings/profile 그대로 사용 */}
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
