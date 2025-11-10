// C:\project\103Team-sub\web\greenacademy_web\src\app\notice\NoticeEditorPanel.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";

/** API 베이스 & fetch 래퍼 (.env 없을 때 9090 폴백 + Next 404 HTML이면 9090 재시도) */
const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ??
  (typeof window !== "undefined" ? `${location.protocol}//${location.hostname}:9090` : "");

async function fetchApi(path: string, init?: RequestInit) {
  const url = `${API_BASE}${path}`;
  const opts: RequestInit = {
    credentials: init?.credentials ?? "include",
    ...init,
  };
  const res = await fetch(url, opts);
  const ct = res.headers.get("content-type") || "";
  if (res.status === 404 && ct.includes("text/html") && typeof window !== "undefined") {
    try {
      const devUrl = `${location.protocol}//${location.hostname}:9090${path}`;
      return await fetch(devUrl, opts);
    } catch {}
  }
  return res;
}

/** 타입 */
type Role = "student" | "parent" | "teacher" | "director";
type Session = { role: Role; username: string; token?: string; academyNumbers?: number[] };
type Academy = { academyNumber?: number; name?: string };
type ClassLite = { id: string; name: string };

type Props = {
  onClose?: () => void;
  onCreated?: () => void;
};

function authHeaders(session: Session | null, json = true): HeadersInit {
  return {
    ...(json ? { "Content-Type": "application/json" } : {}),
    ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
  };
}

/** ─────────────── 유틸 ─────────────── */
function pickId(obj: any): string {
  const raw = obj?.id ?? obj?.classId ?? obj?.Class_ID ?? obj?.class_id ?? "";
  return raw != null ? String(raw).trim() : "";
}
function pickName(obj: any): string {
  return (
    (obj?.name ??
      obj?.className ??
      obj?.Class_Name ??
      obj?.courseName ??
      obj?.Course_Name ??
      obj?.title ??
      obj?.subjectName ??
      obj?.Subject_Name ??
      "")
      .toString()
      .trim()
  );
}
function pickTeacherId(t: any): string {
  const raw = t?.teacherId ?? t?.Teacher_ID ?? t?.id ?? t?.username ?? "";
  return raw != null ? String(raw).trim() : "";
}
function pickTeacherNameFromTeacher(t: any): string {
  const raw = t?.teacherName ?? t?.Teacher_Name ?? t?.name ?? "";
  return raw != null ? String(raw).trim() : "";
}
function appendTeacher(base: string, teacherName: string): string {
  const b = (base ?? "").toString().trim();
  const tn = (teacherName ?? "").toString().trim();
  if (!tn) return b;
  if (!b) return `(${tn})`;
  if (b.endsWith(`(${tn})`) || b.includes(`(${tn})`)) return b;
  return `${b}(${tn})`;
}
function normalizeAcademyNumberFromObject(o: any): number | null {
  const raw =
    o?.academyNumber ??
    o?.Academy_Number ??
    o?.academy_number ??
    (Array.isArray(o?.academyNumbers) ? o.academyNumbers[0] : undefined);
  if (raw === null || raw === undefined || raw === "") return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}
function normAcadNum(v: any): number | null {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

/** 리스트를 {id,name} 유니크 정규화 */
function normalizeClassList(arr: any[]): ClassLite[] {
  const collected: Record<string, ClassLite> = {};
  for (const c of arr ?? []) {
    const id = typeof c === "string" ? c : pickId(c);
    if (!id) continue;
    const nm = typeof c === "string" ? "" : pickName(c);
    if (!collected[id]) collected[id] = { id, name: nm || "" };
    else if (nm && !collected[id].name) collected[id].name = nm;
  }
  return Object.values(collected);
}

/** 파일<->dataURL 직렬화 */
function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}
function dataUrlToFile(dataUrl: string, name: string, type?: string): File {
  const [meta, base64] = dataUrl.split(",");
  const mime = type ?? /data:(.*?);/.exec(meta)?.[1] ?? "application/octet-stream";
  const bin = atob(base64 || "");
  const len = bin.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) bytes[i] = bin.charCodeAt(i);
  return new File([bytes], name, { type: mime });
}

/** ─────────────── 컴포넌트 ─────────────── */
export default function NoticeEditorPanel({ onClose, onCreated }: Props) {
  const [session, setSession] = useState<Session | null>(null);

  // 권한
  const canWrite = session?.role === "teacher" || session?.role === "director";

  // 메타
  const [academies, setAcademies] = useState<Academy[]>([]);
  const [classes, setClasses] = useState<ClassLite[]>([]);
  const [loadingMeta, setLoadingMeta] = useState(false);
  const [metaError, setMetaError] = useState<string | null>(null);

  // 폼
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  // 대상 선택 (전체 = "")
  const [academy, setAcademy] = useState<string>(""); // ""=전체
  const [klass, setKlass] = useState<string>(""); // ""=해당 학원 전체

  // 이미지
  const [files, setFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 상태
  const [saving, setSaving] = useState(false);          // 게시하기 서버 저장
  const [savingDraft, setSavingDraft] = useState(false); // 임시저장(로컬)
  const [error, setError] = useState<string | null>(null);
  const [okMsg, setOkMsg] = useState<string | null>(null);

  // 임시저장 키 (텍스트: localStorage / 첨부: sessionStorage)
  const draftKey = useMemo(() => {
    const u = session?.username ?? "unknown";
    return `notice_draft_${u}`;
  }, [session?.username]);
  const filesDraftKey = useMemo(() => `${draftKey}_files`, [draftKey]);

  /** 세션 로드 & 임시저장 복원(수동 저장분만) */
  useEffect(() => {
    const raw = localStorage.getItem("login");
    if (raw) {
      try {
        setSession(JSON.parse(raw) as Session);
      } catch {}
    }
  }, []);
  useEffect(() => {
    if (!draftKey) return;
    // 텍스트 복원
    try {
      const draftRaw = localStorage.getItem(draftKey);
      if (draftRaw) {
        const d = JSON.parse(draftRaw) as {
          title?: string;
          content?: string;
          academy?: string;
          klass?: string;
        };
        if (d.title) setTitle(d.title);
        if (d.content) setContent(d.content);
        if (typeof d.academy === "string") setAcademy(d.academy);
        if (typeof d.klass === "string") setKlass(d.klass);
      }
    } catch {}
    // 첨부 복원
    try {
      const raw = sessionStorage.getItem(filesDraftKey);
      if (raw) {
        const arr = JSON.parse(raw) as { name: string; type: string; dataUrl: string }[];
        if (Array.isArray(arr) && arr.length > 0) {
          const rebuilt = arr.map((x) => dataUrlToFile(x.dataUrl, x.name, x.type));
          setFiles(rebuilt);
        }
      }
    } catch {}
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filesDraftKey, draftKey]);

  /** 프리뷰 */
  useEffect(() => {
    const urls = files.map((f) => URL.createObjectURL(f));
    setPreviews(urls);
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, [files]);

  /** 학원 목록: 항상 session.academyNumbers로 제한 */
  useEffect(() => {
    (async () => {
      if (!session?.token) return;
      const allowed = new Set<number>(
        (session.academyNumbers ?? []).map(normAcadNum).filter((n): n is number => n !== null)
      );

      try {
        setLoadingMeta(true);
        setMetaError(null);

        let serverList: Academy[] = [];
        const r = await fetchApi("/api/academy", { headers: authHeaders(session) });
        if (r.ok) {
          serverList = (((await r.json()) as Academy[]) ?? []).filter(
            (a) => normAcadNum(a?.academyNumber) !== null
          );
        } else if (r.status !== 403) {
          throw new Error(await r.text());
        }

        let filtered = serverList.filter((a) => {
          const n = normAcadNum(a?.academyNumber);
          return n !== null && allowed.has(n);
        });

        const have = new Set(filtered.map((a) => String(a.academyNumber)));
        for (const n of allowed) if (!have.has(String(n))) filtered.push({ academyNumber: n, name: "학원" });

        filtered.sort(
          (a, b) => (normAcadNum(a.academyNumber) ?? 0) - (normAcadNum(b.academyNumber) ?? 0)
        );
        setAcademies(filtered);
      } catch (e: any) {
        setMetaError(e?.message || "학원 목록을 불러오지 못했습니다.");
        const fb = (session?.academyNumbers ?? []).map((n) => ({ academyNumber: n, name: "학원" }));
        setAcademies(fb);
      } finally {
        setLoadingMeta(false);
      }
    })();
  }, [session?.token, session?.academyNumbers]);

  /** 과목(반) 목록 (권한/학원 선택에 따라) — 원장: "과목(교사이름)" 라벨 */
  useEffect(() => {
    (async () => {
      try {
        setLoadingMeta(true);
        setMetaError(null);
        setClasses([]);
        if (!session) return;
        const headers = authHeaders(session);
        const acadNum = academy ? Number(academy) : null;

        if (session.role === "teacher") {
          let ok = false;
          let arr: any[] = [];
          let r = await fetchApi(`/api/teachers/${encodeURIComponent(session.username)}/classes`, { headers });
          if (r.ok) {
            ok = true; arr = (await r.json()) as any[];
          } else if ([404, 405, 501].includes(r.status)) {
            r = await fetchApi(`/api/manage/teachers/${encodeURIComponent(session.username)}/classes`, { headers });
            if (r.ok) { ok = true; arr = (await r.json()) as any[]; }
          }
          if (ok) {
            const filtered = acadNum
              ? (arr ?? []).filter((c) => normalizeAcademyNumberFromObject(c) === acadNum)
              : (arr ?? []);
            setClasses(
              normalizeClassList(
                filtered.map((c) => ({ id: pickId(c), name: pickName(c) || pickId(c) }))
              )
            );
          }
        } else if (session.role === "director") {
          let tRes = await fetchApi(`/api/teachers`, { headers });
          if (!tRes.ok && [404, 405, 501].includes(tRes.status)) {
            tRes = await fetchApi(`/api/manage/teachers`, { headers });
          }
          if (tRes.ok) {
            const teachers = (await tRes.json()) as any[];
            const filteredTeachers = acadNum
              ? (teachers ?? []).filter(
                  (t) => Array.isArray(t?.academyNumbers) && t.academyNumbers.includes(acadNum)
                )
              : (teachers ?? []);

            const collected: Record<string, ClassLite> = {};
            await Promise.all(
              filteredTeachers.map(async (t) => {
                const tid = pickTeacherId(t);
                if (!tid) return;
                const tname = pickTeacherNameFromTeacher(t);

                let cRes = await fetchApi(`/api/teachers/${encodeURIComponent(tid)}/classes`, { headers });
                if (!cRes.ok && [404, 405, 501].includes(cRes.status)) {
                  cRes = await fetchApi(`/api/manage/teachers/${encodeURIComponent(tid)}/classes`, { headers });
                }
                if (!cRes.ok) return;

                const arr = (await cRes.json()) as any[];
                for (const c of arr ?? []) {
                  if (acadNum != null) {
                    const cn = normalizeAcademyNumberFromObject(c);
                    if (cn != null && cn !== acadNum) continue;
                  }
                  const id = pickId(c);
                  if (!id) continue;
                  const baseName = pickName(c) || id;
                  const label = appendTeacher(baseName, tname);
                  if (!collected[id]) collected[id] = { id, name: label };
                }
              })
            );
            setClasses(Object.values(collected));
          }
        }
      } catch (e: any) {
        setMetaError(e?.message || "과목 목록을 불러오지 못했습니다.");
      } finally {
        setLoadingMeta(false);
      }
    })();
  }, [session, academy]);

  /** 대상 범위 판정 */
  const audience = useMemo<"ALL" | "ACADEMY" | "CLASS">(() => {
    if (klass) return "CLASS";
    if (academy) return "ACADEMY";
    return "ALL";
  }, [academy, klass]);

  /** 학원=전체이면 과목 스피너 비활성 & 프롬프트 */
  const classOptions = useMemo(() => {
    if (!academy) return [{ value: "", label: "학원 먼저 선택" }];
    const list = classes
      .map((c) => ({ value: c.id, label: c.name || c.id }))
      .filter((c) => c.value);
    return [{ value: "", label: "해당 학원 전체" }, ...list];
  }, [academy, classes]);

  /** 유효성(게시 전) */
  const validate = () => {
    if (!title.trim()) return "제목을 입력해 주세요.";
    if (!content.trim()) return "내용을 입력해 주세요.";
    if (audience === "ACADEMY" && !academy) return "학원을 선택해 주세요.";
    if (audience === "CLASS" && !klass) return "과목(반)을 선택해 주세요.";
    if (audience === "ALL" && !session?.academyNumbers?.length) return "세션에 학원번호가 없습니다.";
    return null;
  };

  /** JSON 또는 Multipart로 1회 생성 (게시하기) */
  async function postOnceJSON(payload: Record<string, any>) {
    return fetchApi("/api/notices", {
      method: "POST",
      headers: authHeaders(session, true),
      body: JSON.stringify(payload),
    });
  }
  async function postOnceMultipart(payload: Record<string, any>) {
    const fd = new FormData();
    Object.entries(payload).forEach(([k, v]) => {
      if (v == null) return;
      if (Array.isArray(v)) {
        v.forEach((item) => fd.append(k, String(item)));
      } else {
        fd.append(k, String(v));
      }
    });
    files.forEach((f) => fd.append("images", f));
    return fetchApi("/api/notices", {
      method: "POST",
      headers: authHeaders(session, false), // FormData 사용 시 Content-Type 자동
      body: fd,
    });
  }

  /** 임시저장(클라이언트 로컬 보관) → 저장 후 목록으로 이동 */
  const saveDraftLocally = async () => {
    setSavingDraft(true);
    setError(null);
    setOkMsg(null);
    try {
      // 텍스트 저장
      const payload = JSON.stringify({ title, content, academy, klass });
      localStorage.setItem(draftKey, payload);

      // 첨부 저장 (dataURL, 총 8MB/파일당 3MB 제한)
      const serialized: { name: string; type: string; dataUrl: string }[] = [];
      let totalBytes = 0;
      const MAX_TOTAL = 8 * 1024 * 1024;
      const MAX_EACH = 3 * 1024 * 1024;
      for (const f of files) {
        if (f.size > MAX_EACH) continue;
        const dataUrl = await fileToDataUrl(f);
        const approxBytes = Math.ceil((dataUrl.length * 3) / 4);
        if (totalBytes + approxBytes > MAX_TOTAL) break;
        serialized.push({ name: f.name, type: f.type || "application/octet-stream", dataUrl });
        totalBytes += approxBytes;
      }
      if (serialized.length > 0) {
        sessionStorage.setItem(filesDraftKey, JSON.stringify(serialized));
      } else {
        sessionStorage.removeItem(filesDraftKey);
      }

      // 알림 → 목록으로
      if (typeof window !== "undefined") {
        window.alert("임시저장했습니다. 다음에 이어서 작성할 수 있습니다");
      }
      if (onClose) {
        onClose();
      } else if (typeof window !== "undefined") {
        window.history.back();
      }
    } catch (e: any) {
      setError(e?.message || "임시저장에 실패했습니다.");
    } finally {
      setSavingDraft(false);
    }
  };

  /** 임시저장 삭제(닫기/초기화에서 사용) */
  const clearDraft = () => {
    localStorage.removeItem(draftKey);
    sessionStorage.removeItem(filesDraftKey);
  };

  /** 게시(서버 저장) */
  const submit = async () => {
    setSaving(true);
    setError(null);
    setOkMsg(null);
    try {
      const v = validate();
      if (v) throw new Error(v);
      if (!session?.token) throw new Error("로그인이 필요합니다. (토큰 없음)");

      let nums: number[] = [];
      if (audience === "ALL") {
        nums = [...(session.academyNumbers ?? [])];
      } else if (audience === "ACADEMY" || audience === "CLASS") {
        if (academy) nums = [Number(academy)];
      }

      const basePayload: any = {
        title: title.trim(),
        content: content.trim(),
        author: session.username,
        academyNumbers: nums,
      };
      if (audience === "CLASS" && klass) basePayload.classId = klass;

      const res = files.length === 0 ? await postOnceJSON(basePayload) : await postOnceMultipart(basePayload);
      if (!res.ok) {
        if (files.length > 0 && (res.status === 415 || res.status === 400)) {
          throw new Error("이미지 업로드가 지원되지 않습니다. (multipart/form-data 미지원)");
        }
        throw new Error((await res.text()) || "공지 저장에 실패했습니다.");
      }

      // 성공: 임시저장본 제거
      clearDraft();
      setOkMsg("저장되었습니다.");
      onCreated?.();
    } catch (e: any) {
      setError(e?.message || "오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  };

  if (!canWrite) {
    return (
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
        공지 작성 권한이 없습니다. (원장/선생 전용)
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {(metaError || error || okMsg) && (
        <div
          className={`px-4 py-3 rounded-xl text-sm ${
            error
              ? "bg-red-50 text-red-700 ring-1 ring-red-200"
              : metaError
              ? "bg-amber-50 text-amber-800 ring-1 ring-amber-200"
              : "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200"
          }`}
        >
          {error || metaError || okMsg}
        </div>
      )}

      {/* 대상 선택 */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Spinner
            label="학원"
            value={academy}
            onChange={(v) => {
              setAcademy(v);
              setKlass(""); // 학원 바뀌면 반 초기화
            }}
            loading={loadingMeta}
            options={[
              { value: "", label: "전체" },
              ...academies
                .map((a) => ({
                  value: String(a.academyNumber ?? ""),
                  label:
                    a.academyNumber != null ? `${a.name ?? "학원"} (#${a.academyNumber})` : a.name ?? "학원",
                }))
                .filter((o) => o.value !== ""),
            ]}
          />
          <Spinner
            label="과목(반)"
            value={klass}
            onChange={setKlass}
            loading={loadingMeta}
            options={classOptions}
            disabled={!academy} // 학원=전체 → 비활성
            hint={!academy ? "학원을 먼저 선택해 주세요." : classes.length === 0 ? "담당 반이 없으면 ‘해당 학원 전체’로 공지됩니다." : undefined}
          />
        </div>
      </div>

      {/* 본문 */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 space-y-4">
        <Field label="제목">
          <input
            className="w-full h-11 rounded-xl border border-gray-300 px-3 bg-white text-gray-900 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="공지 제목을 입력하세요"
          />
        </Field>

        <Field label="내용">
          <textarea
            className="w-full min-h-[160px] rounded-xl border border-gray-300 p-3 bg-white text-gray-900 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="공지 내용을 입력하세요"
          />
          <div className="mt-1 text-xs text-gray-500 text-right">{content.trim().length}자</div>
        </Field>

        {/* 이미지 (버튼 트리거) */}
        <Field label="이미지 첨부">
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="px-4 h-10 rounded-xl border border-gray-300 text-gray-800 bg-white hover:bg-gray-50"
              aria-label="이미지 선택"
            >
              이미지 선택
            </button>
            {files.length > 0 && (
              <button
                type="button"
                onClick={() => {
                  setFiles([]);
                }}
                className="px-3 h-10 rounded-xl border border-gray-200 text-gray-700 bg-gray-50 hover:bg-gray-100"
                aria-label="선택한 이미지 모두 제거"
              >
                모두 제거
              </button>
            )}
            <span className="text-xs text-gray-500">
              {files.length > 0 ? `${files.length}개 선택됨` : "JPG, PNG 등 이미지 업로드 가능"}
            </span>
          </div>

          {/* 숨김 파일 입력 */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            multiple
            className="hidden"
            onChange={(e) => {
              const list = Array.from(e.target.files || []);
              if (list.length === 0) return;
              setFiles((prev) => [...prev, ...list]);
              e.currentTarget.value = "";
            }}
          />

          {files.length > 0 && (
            <div className="mt-3 grid grid-cols-2 sm:grid-cols-4 gap-3">
              {previews.map((src, i) => (
                <div key={i} className="relative group">
                  <img
                    src={src}
                    alt={`preview-${i}`}
                    className="w-full h-28 object-cover rounded-xl ring-1 ring-black/5"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      setFiles((prev) => prev.filter((_, idx) => idx !== i));
                    }}
                    className="absolute top-2 right-2 px-2 py-1 text-xs rounded-lg bg-black/60 text-white opacity-0 group-hover:opacity-100"
                    aria-label="이미지 제거"
                  >
                    제거
                  </button>
                </div>
              ))}
            </div>
          )}
        </Field>
      </div>

      {/* 액션 */}
      <div className="flex flex-wrap items-center gap-3">
        <button
          onClick={submit}
          disabled={saving || savingDraft}
          className="px-5 h-11 rounded-xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600 disabled:opacity-50"
        >
          {saving ? "게시 중…" : "게시하기"}
        </button>

        {/* 임시저장: 로컬 보관 → 저장 후 목록으로 이동 */}
        <button
          onClick={saveDraftLocally}
          disabled={saving || savingDraft}
          className="px-5 h-11 rounded-xl border-2 border-amber-500 text-amber-700 font-semibold bg-amber-50 hover:bg-amber-100 disabled:opacity-50"
        >
          {savingDraft ? "임시저장 중…" : "임시저장"}
        </button>

        <button
          onClick={() => {
            setTitle("");
            setContent("");
            setAcademy("");
            setKlass("");
            setFiles([]);
            clearDraft(); // 임시저장본도 함께 제거
            setOkMsg("입력값을 초기화했습니다.");
            setTimeout(() => setOkMsg(null), 1800);
          }}
          disabled={saving || savingDraft}
          className="px-5 h-11 rounded-xl border border-gray-300 text-gray-800 bg-white hover:bg-gray-50 disabled:opacity-50"
        >
          초기화
        </button>

        <div className="ml-auto flex gap-2">
          <button
            onClick={() => {
              clearDraft();  // 닫기 시 임시저장본 전부 삭제
              onClose?.();
            }}
            className="px-5 h-11 rounded-xl border border-gray-300 text-gray-800 bg-white hover:bg-gray-50"
            title="닫으면 임시저장본이 삭제됩니다"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}

/** 공통 폼 섹션 */
function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="block text-sm text-gray-900 mb-1">{label}</span>
      {children}
    </label>
  );
}

function Spinner({
  label,
  value,
  onChange,
  options,
  loading,
  hint,
  disabled,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  loading?: boolean;
  hint?: string;
  disabled?: boolean;
}) {
  return (
    <label className="block">
      <span className="block text-sm text-gray-900 mb-1">{label}</span>
      <select
        className="w-full h-11 rounded-xl border border-gray-300 px-3 bg-white text-gray-900 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300 disabled:bg-gray-100 disabled:text-gray-400"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={loading || disabled}
      >
        {options.map((o) => (
          <option key={`${label}-${o.value}`} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
    </label>
  );
}
