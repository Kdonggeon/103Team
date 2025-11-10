// C:\project\103Team-sub\web\greenacademy_web\src\app\director\DirectorPeoplePanel.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import TeacherStudentManage from "@/app/teacher/StudentManage";

/** ============= 공통 유틸/타입 ============= **/
type LoginSession = {
  role?: string;
  username?: string;
  name?: string;
  token?: string;
  academyNumbers?: number[];
};

type TeacherListItem = {
  id: string;
  name?: string | null;
  phone?: string | null;
  subjects?: string[] | null; // 프론트 집계로 채움
  academyNumbers?: number[];
};

type TeacherDetail = Record<string, any>;
type AcaSubjectsMap = Record<number, string[]>;

function readLogin(): LoginSession | null {
  try {
    const raw = localStorage.getItem("session") ?? localStorage.getItem("login");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function toNumber(x: any) {
  const n = Number(x);
  return Number.isFinite(n) ? n : undefined;
}
function ensureArray(raw: any): any[] {
  if (Array.isArray(raw)) return raw;
  if (Array.isArray(raw?.content)) return raw.content;
  if (Array.isArray(raw?.data)) return raw.data;
  if (Array.isArray(raw?.results)) return raw.results;
  return [];
}
function normalizeAcademyNumbers(src: any): number[] {
  const eat = (v: any): number[] => {
    if (v == null) return [];
    if (Array.isArray(v)) return v.map(toNumber).filter((n): n is number => typeof n === "number");
    if (typeof v === "number") return [v];
    if (typeof v === "string") {
      const s = v.trim();
      if ((s.startsWith("[") && s.endsWith("]")) || (s.startsWith("{") && s.endsWith("}"))) {
        try { return eat(JSON.parse(s)); } catch {}
      }
      if (s.includes(",") || s.includes(" ")) {
        return s.split(/[,\s]+/g).map(toNumber).filter((n): n is number => typeof n === "number");
      }
      const n = toNumber(s);
      return typeof n === "number" ? [n] : [];
    }
    if (typeof v === "object") {
      const cand =
        (v as any).Academy_Numbers ??
        (v as any).Academy_Number ??
        (v as any).academyNumbers ??
        (v as any).academies ??
        (v as any).academyNumber ??
        (v as any)["AcademyNumbers"];
      if (cand != null) return eat(cand);
    }
    return [];
  };
  return Array.from(new Set(eat(src)));
}
function hasAcaOverlap(a?: number[] | null, b?: number[] | null) {
  if (!Array.isArray(a) || a.length === 0) return false;
  if (!Array.isArray(b) || b.length === 0) return false;
  const set = new Set(b);
  return a.some((n) => set.has(n));
}

/** 공통 요청: /backend 프록시 + 토큰 헤더 */
async function apiGet<T>(path: string, token?: string): Promise<T> {
  const url = path.startsWith("/backend") ? path : `/backend${path}`;
  const r = await fetch(url, {
    method: "GET",
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  const text = await r.text();
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}${text ? " | " + text : ""}`);
  return text ? (JSON.parse(text) as T) : ({} as T);
}
async function apiPut<T>(path: string, body: any, token?: string): Promise<T> {
  const url = path.startsWith("/backend") ? path : `/backend${path}`;
  const r = await fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body ?? {}),
  });
  const text = await r.text();
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}${text ? " | " + text : ""}`);
  return text ? (JSON.parse(text) as T) : ({} as T);
}

/** 과목/학원 추출 */
function extractSubjectFromClass(c: any): string | null {
  const cand =
    c?.Class_Name ?? c?.className ??
    c?.Subject ?? c?.subject ??
    c?.Course_Name ?? c?.courseName ??
    c?.name ?? c?.title ?? null;
  if (cand == null) return null;
  const s = String(cand).trim();
  return s.length ? s : null;
}
function extractAcademyNumberFromClass(c: any): number | undefined {
  // 다양한 필드명/중첩 구조 대응
  const tryVals = [
    c?.Academy_Number, c?.academyNumber, c?.academy_no, c?.academyNo,
    c?.Academy?.Number, c?.academy?.number, c?.academy?.Academy_Number,
    c?.Academy_ID, c?.AcademyId, c?.academyId, c?.academy_id,
  ];
  for (const v of tryVals) {
    const n = toNumber(v);
    if (typeof n === "number") return n;
  }
  if (Array.isArray(c?.Academy_Number)) {
    for (const v of c.Academy_Number) {
      const n = toNumber(v);
      if (typeof n === "number") return n;
    }
  }
  return undefined;
}

/** 유틸 */
const chunk = <T,>(arr: T[], size: number) =>
  arr.reduce<T[][]>((a, _, i) => (i % size ? a : [...a, arr.slice(i, i + size)]), []);
const uniqSorted = (arr: string[]) =>
  Array.from(new Set(arr.map((s) => s.trim()).filter(Boolean))).sort((a, b) => a.localeCompare(b));

/** 여러 교사에 대해 과목 집계 */
async function fetchSubjectsForTeachers(teacherIds: string[], token?: string): Promise<Record<string, string[]>> {
  const out: Record<string, string[]> = {};
  const groups = chunk(teacherIds, 6);
  for (const g of groups) {
    await Promise.all(
      g.map(async (tid) => {
        try {
          // 1) /subjects
          try {
            const s = await apiGet<any>(`/api/teachers/${encodeURIComponent(tid)}/subjects`, token);
            if (Array.isArray(s?.subjects)) {
              out[tid] = uniqSorted(s.subjects.map(String));
              return;
            }
          } catch {}
          // 2) /api/classes?teacherId=...
          try {
            const raw = await apiGet<any>(`/api/classes?teacherId=${encodeURIComponent(tid)}`, token);
            const subs = uniqSorted(ensureArray(raw).map(extractSubjectFromClass).filter((v): v is string => !!v));
            if (subs.length) { out[tid] = subs; return; }
          } catch {}
          // 3) /api/teachers/{id}/classes
          try {
            const raw2 = await apiGet<any>(`/api/teachers/${encodeURIComponent(tid)}/classes`, token);
            const subs2 = uniqSorted(ensureArray(raw2).map(extractSubjectFromClass).filter((v): v is string => !!v));
            out[tid] = subs2;
          } catch { out[tid] = []; }
        } catch { out[tid] = []; }
      })
    );
  }
  return out;
}

/** ============= 재사용 UI 조각 (학생/학부모 폼과 동일 톤) ============= **/
function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-black">
      <svg className="h-4 w-4 animate-spin text-black" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx={12} cy={12} r={10} stroke="currentColor" strokeWidth={4} className="opacity-25" />
        <path d="M4 12a8 8 0 0 1 8-8" stroke="currentColor" strokeWidth={4} strokeLinecap="round" className="opacity-90" />
      </svg>
      {label && <span>{label}</span>}
    </div>
  );
}
function FieldBox({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
      <div className="text-[11px] text-gray-700">{label}</div>
      <div className="text-sm font-medium text-gray-900 mt-0.5 break-words">{value}</div>
    </div>
  );
}
function Badge({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium bg-gray-100 text-gray-900 ring-1 ring-gray-300">
      {children}
    </span>
  );
}

/** =========================================================================================
 *  선생님 검색 패널 (상세검색: 과목만 유지) + 상세 편집(학생/학부모 수정폼과 동일 톤)
 * ========================================================================================= */
function DirectorTeacherSearchPanel() {
  const login = readLogin();
  const tokenDep = useMemo(() => String(login?.token ?? ""), [login?.token]);

  const academyOptions: number[] = useMemo(() => {
    const arr = Array.isArray(login?.academyNumbers) ? login!.academyNumbers! : [];
    return Array.from(new Set(arr.filter(Number.isFinite)));
  }, [login]);

  type AcademyFilter = number | "ALL";
  const [academyNumber, setAcademyNumber] = useState<AcademyFilter>("ALL");
  const [idQ, setIdQ] = useState("");
  const [nameQ, setNameQ] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [deptQ, setDeptQ] = useState("");

  const [sortKey, setSortKey] = useState<"name" | "phone">("name");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [hits, setHits] = useState<TeacherListItem[]>([]);
  const [selected, setSelected] = useState<TeacherListItem | null>(null);

  const [detailLoading, setDetailLoading] = useState(false);
  const [detailErr, setDetailErr] = useState<string | null>(null);
  const [teacherDetail, setTeacherDetail] = useState<TeacherDetail | null>(null);
  const [detailSubjects, setDetailSubjects] = useState<string[] | null>(null); // 유지(저장 시 merge에 사용)
  const [detailAcaSubjects, setDetailAcaSubjects] = useState<AcaSubjectsMap | null>(null);

  // 편집 상태(학생/학부모 수정폼과 동일한 레이아웃/클래스)
  const [editing, setEditing] = useState(false);
  const [saveErr, setSaveErr] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [formId, setFormId] = useState("");
  const [formName, setFormName] = useState("");
  const [formPhone, setFormPhone] = useState("");

  function sortByKey(list: TeacherListItem[], key: "name" | "phone") {
    const arr = [...list];
    arr.sort((a, b) =>
      key === "name"
        ? (a.name ?? "").localeCompare(b.name ?? "")
        : (a.phone ?? "").localeCompare(b.phone ?? "")
    );
    return arr;
  }
  useEffect(() => { setHits((prev) => sortByKey(prev, sortKey)); }, [sortKey]);

  const search = async () => {
    setLoading(true);
    setErr(null);
    setSelected(null);
    setTeacherDetail(null);
    setDetailErr(null);
    setDetailSubjects(null);
    setDetailAcaSubjects(null);
    setEditing(false);

    try {
      // 서버 검색(있으면 활용)
      const params = new URLSearchParams();
      if (idQ.trim()) params.set("teacherId", idQ.trim());
      if (nameQ.trim()) params.set("name", nameQ.trim());
      if (deptQ.trim()) params.set("subject", deptQ.trim());
      const effAca = academyNumber !== "ALL" ? Number(academyNumber) : undefined;
      if (typeof effAca === "number") params.set("academyNumber", String(effAca));

      let list: TeacherListItem[] = [];
      let serverOk = false;

      if (params.toString()) {
        try {
          const raw = await apiGet<any>(`/api/teachers/search?${params.toString()}`, tokenDep);
          list = ensureArray(raw)
            .map((t: any) => ({
              id: String(t.id ?? t.teacherId ?? t.Teacher_ID ?? ""),
              name: t.name ?? t.teacherName ?? t.Teacher_Name ?? null,
              phone: t.phone ?? t.teacherPhoneNumber ?? t.Teacher_Phone_Number ?? null,
              subjects: Array.isArray(t.subjects) ? t.subjects : null,
              academyNumbers: normalizeAcademyNumbers(t),
            }))
            .filter((x) => !!x.id);
          serverOk = true;
        } catch { serverOk = false; }
      }

      // 폴백: 전체 조회 후 클라 필터
      if (!serverOk) {
        const rawAll = await apiGet<any>("/api/teachers", tokenDep);
        list = ensureArray(rawAll)
          .map((t: any) => {
            const id = t.Teacher_ID ?? t.teacherId ?? t.id ?? t.username ?? "";
            const name = t.Teacher_Name ?? t.teacherName ?? t.name ?? null;
            const phone = t.Teacher_Phone_Number ?? t.teacherPhoneNumber ?? t.phone ?? t.mobile ?? null;
            const subjects: string[] | null =
              Array.isArray(t.subjects) ? t.subjects :
              Array.isArray(t.Subjects) ? t.Subjects : null;
            const academyNumbers = normalizeAcademyNumbers(t);
            return { id: String(id), name, phone, subjects, academyNumbers };
          })
          .filter((x: TeacherListItem) => !!x.id);

        const idTrim = idQ.trim().toLowerCase();
        const nameTrim = nameQ.trim().toLowerCase();
        if (idTrim) list = list.filter((x) => String(x.id).toLowerCase().includes(idTrim));
        if (nameTrim) list = list.filter((x) => (x.name ?? "").toLowerCase().includes(nameTrim));

        if (typeof effAca === "number") {
          list = list.filter((x) => (x.academyNumbers ?? []).some((n) => Number(n) === effAca));
        } else if (academyOptions.length) {
          list = list.filter((x) => hasAcaOverlap(x.academyNumbers, academyOptions));
        }

        const deptTrim = deptQ.trim().toLowerCase();
        if (deptTrim) {
          const ids = list.map((x) => x.id);
          const map = await fetchSubjectsForTeachers(ids, tokenDep);
          list = list
            .map((row) => {
              const mergedSubs = Array.from(new Set([...(row.subjects ?? []), ...(map[row.id] ?? [])]));
              return { ...row, subjects: mergedSubs };
            })
            .filter((row) => (row.subjects ?? []).some((s) => s.toLowerCase().includes(deptTrim)));
        }
      }

      list = sortByKey(list, sortKey);
      setHits(list);
    } catch (e: any) {
      console.error("[DirectorTeacherSearchPanel] search error:", e);
      setErr(e.message ?? String(e));
      setHits([]);
    } finally {
      setLoading(false);
    }
  };

  /** 행 선택 → 상세 로드(학원별 과목 맵 구성) */
  const selKey = selected ? selected.id : "";
  const fetchSeqRef = useRef(0);
  useEffect(() => {
    setTeacherDetail((prev) => (prev === null ? prev : null));
    setDetailErr((prev) => (prev === null ? prev : null));
    setDetailSubjects(null);
    setDetailAcaSubjects(null);
    setEditing(false);
    setSaveErr(null);

    if (!selected) return;
    let cancelled = false;
    const seq = ++fetchSeqRef.current;

    (async () => {
      setDetailLoading(true);
      try {
        const tid = selected.id;

        // 상세
        const d = await apiGet<TeacherDetail>(`/api/teachers/${encodeURIComponent(tid)}`, tokenDep);
        if (cancelled || fetchSeqRef.current !== seq) return;
        setTeacherDetail(d ?? {});

        // 편집폼 초기값
        setFormId(String(d?.Teacher_ID ?? d?.teacherId ?? d?.id ?? tid));
        setFormName(String(d?.Teacher_Name ?? d?.teacherName ?? d?.name ?? selected?.name ?? ""));
        setFormPhone(String(d?.Teacher_Phone_Number ?? d?.teacherPhoneNumber ?? d?.phone ?? d?.mobile ?? selected?.phone ?? ""));

        // 과목/학원별 과목 수집
        const subsUnion = new Set<string>();
        const classesBag: any[] = [];

        try {
          const s = await apiGet<any>(`/api/teachers/${encodeURIComponent(tid)}/subjects`, tokenDep);
          if (Array.isArray(s?.subjects)) {
            s.subjects.map(String).forEach((v: string) => subsUnion.add(v));
          }
        } catch {}

        try {
          const rawA = await apiGet<any>(`/api/classes?teacherId=${encodeURIComponent(tid)}`, tokenDep);
          const listA = ensureArray(rawA);
          classesBag.push(...listA);
          listA.map(extractSubjectFromClass).filter(Boolean).forEach((v) => subsUnion.add(v as string));
        } catch {}

        try {
          const rawB = await apiGet<any>(`/api/teachers/${encodeURIComponent(tid)}/classes`, tokenDep);
          const listB = ensureArray(rawB);
          classesBag.push(...listB);
          listB.map(extractSubjectFromClass).filter(Boolean).forEach((v) => subsUnion.add(v as string));
        } catch {}

        if (!cancelled && fetchSeqRef.current === seq) {
          setDetailSubjects(uniqSorted(Array.from(subsUnion)));

          // 학원별 과목 맵
          const tmp: Record<number, Set<string>> = {};
          for (const c of classesBag) {
            const aca = extractAcademyNumberFromClass(c);
            const sub = extractSubjectFromClass(c);
            if (typeof aca === "number" && sub) {
              if (!tmp[aca]) tmp[aca] = new Set<string>();
              tmp[aca].add(sub);
            }
          }
          const academiesAll = normalizeAcademyNumbers(d ?? selected);
          const finalMap: AcaSubjectsMap = {};
          Object.keys(tmp).forEach((k) => {
            const n = Number(k);
            finalMap[n] = uniqSorted(Array.from(tmp[n]));
          });
          for (const n of academiesAll) {
            if (finalMap[n] == null) finalMap[n] = [];
          }
          setDetailAcaSubjects(finalMap);
        }
      } catch (e: any) {
        if (!cancelled && fetchSeqRef.current === seq) {
          console.error("[DirectorTeacherSearchPanel] detail error:", e);
          setDetailErr(e.message ?? String(e));
        }
      } finally {
        if (!cancelled && fetchSeqRef.current === seq) setDetailLoading(false);
      }
    })();

    return () => { cancelled = true; };
  }, [selKey, tokenDep]);

  /** 저장 */
  const onSave = async () => {
    if (!teacherDetail) return;
    setSaving(true);
    setSaveErr(null);
    try {
      const prevId = String(teacherDetail?.Teacher_ID ?? teacherDetail?.teacherId ?? teacherDetail?.id ?? selected?.id ?? "");
      const payload = {
        teacherId: formId?.trim() || prevId,
        teacherName: formName?.trim() || null,
        teacherPhoneNumber: formPhone?.trim() || null,
      };
      const updated = await apiPut<TeacherDetail>(`/api/teachers/${encodeURIComponent(prevId)}`, payload, tokenDep);

      // 상세/폼 반영
      setTeacherDetail(updated);
      setFormId(String(updated?.Teacher_ID ?? updated?.teacherId ?? payload.teacherId));
      setFormName(String(updated?.Teacher_Name ?? updated?.teacherName ?? payload.teacherName ?? ""));
      setFormPhone(String(updated?.Teacher_Phone_Number ?? updated?.teacherPhoneNumber ?? payload.teacherPhoneNumber ?? ""));

      // 목록/선택 갱신 (아이디 변경 포함)
      const newId = String(updated?.Teacher_ID ?? updated?.teacherId ?? payload.teacherId);
      setHits((old) => {
        const arr = [...old];
        const idx = arr.findIndex((x) => x.id === prevId);
        const mergedSubjects = Array.from(new Set([...(arr[idx]?.subjects ?? []), ...(detailSubjects ?? [])]));
        if (idx >= 0) {
          arr[idx] = {
            ...arr[idx],
            id: newId,
            name: updated?.Teacher_Name ?? updated?.teacherName ?? formName ?? arr[idx].name,
            phone: updated?.Teacher_Phone_Number ?? updated?.teacherPhoneNumber ?? formPhone ?? arr[idx].phone,
            subjects: mergedSubjects,
          };
        } else {
          arr.push({
            id: newId,
            name: updated?.Teacher_Name ?? updated?.teacherName ?? formName ?? null,
            phone: updated?.Teacher_Phone_Number ?? updated?.teacherPhoneNumber ?? formPhone ?? null,
            subjects: mergedSubjects,
            academyNumbers: normalizeAcademyNumbers(updated),
          });
        }
        return arr;
      });
      setSelected((s) => (s ? { ...s, id: newId, name: formName || s.name, phone: formPhone || s.phone } : s));
      setEditing(false);
    } catch (e: any) {
      setSaveErr(e.message ?? String(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-screen-2xl mx-auto px-6 py-6 space-y-5 text-gray-900">
      {/* 검색 바 — 학생/학부모 폼과 동일 톤 */}
      <div className="relative rounded-2xl bg-white ring-1 ring-gray-300 p-4 md:p-5">
        <div className="grid grid-cols-[160px_200px_minmax(280px,1fr)_100px_120px] gap-2 items-center">
          <select
            className="border border-gray-400 rounded px-2 py-2 text-sm bg-white text-gray-900"
            value={academyNumber === "ALL" ? "ALL" : String(academyNumber)}
            onChange={(e) => setAcademyNumber(e.target.value === "ALL" ? "ALL" : Number(e.target.value))}
          >
            <option value="ALL">전체</option>
            {academyOptions.map((n) => (<option key={n} value={n}>{n}</option>))}
          </select>

          <input
            className="border border-gray-400 rounded px-3 py-2 text-sm bg-white text-gray-900 placeholder-gray-500"
            value={idQ}
            onChange={(e) => setIdQ(e.target.value)}
            placeholder="아이디"
          />

          <input
            className="border border-gray-400 rounded px-3 py-2 text-sm bg-white text-gray-900 placeholder-gray-500 w-full"
            value={nameQ}
            onChange={(e) => setNameQ(e.target.value)}
            placeholder="이름 검색어"
          />

          <button
            onClick={search}
            className="px-4 py-2 rounded bg-black text-white text-sm hover:bg-gray-800 whitespace-nowrap min-w-[88px]"
          >
            검색
          </button>

          <button
            onClick={() => setShowAdvanced((v) => !v)}
            className="px-3 py-2 rounded border border-gray-500 text-sm bg-white hover:bg-gray-50 whitespace-nowrap"
          >
            {showAdvanced ? "상세검색 닫기" : "상세검색"}
          </button>
        </div>

        {showAdvanced && (
          <div className="mt-3 grid grid-cols-1 gap-3">
            <div className="flex items-center gap-2">
              <label className="text-[11px] text-gray-700 w-16 shrink-0">과목</label>
              <input
                className="border border-gray-400 rounded px-2 py-2 text-sm bg-white text-gray-900 w-full placeholder-gray-500"
                value={deptQ}
                onChange={(e) => setDeptQ(e.target.value)}
                placeholder="과목"
              />
            </div>
          </div>
        )}
      </div>

      {/* 본문 2열 — 좌: 결과, 우: 상세/수정(학생/학부모 폼과 동일 레이아웃) */}
      <div className="grid grid-cols-1 lg:grid-cols-[520px_1fr] gap-5">
        {/* 좌: 결과표 */}
        <div className="rounded-2xl bg-white ring-1 ring-gray-300 shadow-sm overflow-hidden">
          <div className="px-4 py-3 border-b flex items-center justify-between">
            <div className="text-sm font-semibold text-gray-900">검색 결과 ({hits.length})</div>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1 text-sm">
                <span className="text-gray-700">정렬</span>
                <select
                  className="border border-gray-400 rounded px-2 py-1.5 text-sm bg-white text-gray-900"
                  value={sortKey}
                  onChange={(e) => setSortKey(e.target.value as any)}
                >
                  <option value="name">이름</option>
                  <option value="phone">전화</option>
                </select>
              </div>
              {loading && <Spinner label="불러오는 중…" />}
            </div>
          </div>

          {err ? (
            <div className="p-4 text-sm text-red-600">오류: {err}</div>
          ) : hits.length === 0 ? (
            <div className="p-4 text-sm text-gray-700">
              데이터가 없습니다{academyNumber !== "ALL" ? ` (학원번호 #${academyNumber} 기준)` : ""}.
            </div>
          ) : (
            <div className="overflow-auto">
              <table className="min-w-full text-sm text-gray-900">
                <thead className="bg-gray-50 text-gray-900">
                  <tr>
                    <th className="px-3 py-2 text-left w-14">선택</th>
                    <th className="px-3 py-2 text-left">이름</th>
                    <th className="px-3 py-2 text-left">전화</th>
                    <th className="px-3 py-2 text-left">아이디</th>
                    <th className="px-3 py-2 text-left">소속학원</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {hits.map((r) => {
                    const isSel = selected?.id === r.id;
                    return (
                      <tr
                        key={`teacher-${r.id}`}
                        className={isSel ? "bg-emerald-50" : "hover:bg-gray-50 cursor-pointer"}
                        onClick={() => setSelected((prev) => (prev && prev.id === r.id ? prev : r))}
                      >
                        <td className="px-3 py-2"><input type="checkbox" readOnly checked={isSel} /></td>
                        <td className="px-3 py-2 font-medium">{r.name ?? r.id}</td>
                        <td className="px-3 py-2">{r.phone ?? "—"}</td>
                        <td className="px-3 py-2">{r.id}</td>
                        <td className="px-3 py-2">
                          {(r.academyNumbers ?? []).length ? (
                            <div className="flex flex-wrap gap-1.5">
                              {(r.academyNumbers ?? []).map((n) => <Badge key={n}>#{n}</Badge>)}
                            </div>
                          ) : "—"}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* 우: 상세/수정 */}
        <section className="space-y-4">
          <div className="rounded-2xl bg-white ring-1 ring-gray-300 shadow-sm p-5">
            <div className="mb-3 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/10 ring-1 ring-emerald-200 text-emerald-700 font-semibold">
                  {(() => {
                    const nm = (teacherDetail?.Teacher_Name ??
                      teacherDetail?.teacherName ??
                      teacherDetail?.name ??
                      selected?.name ??
                      selected?.id ??
                      "U") as string;
                    return nm.toString().trim().charAt(0).toUpperCase();
                  })()}
                </div>
                <div className="text-base font-semibold text-gray-900">선생님정보</div>
              </div>
              <div className="flex items-center gap-2">
                {detailLoading && <Spinner />}
                {!detailLoading && selected && (
                  editing ? (
                    <>
                      <button
                        onClick={() => { setEditing(false); setSaveErr(null); }}
                        className="px-3 py-2 rounded border border-gray-500 text-sm bg-white hover:bg-gray-50"
                      >
                        취소
                      </button>
                      <button
                        onClick={onSave}
                        disabled={saving}
                        className="px-4 py-2 rounded bg-black text-white text-sm hover:bg-gray-800 disabled:opacity-60"
                      >
                        {saving ? "저장 중…" : "저장"}
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => { setEditing(true); setSaveErr(null); }}
                      className="px-4 py-2 rounded bg-black text-white text-sm hover:bg-gray-800"
                    >
                      수정하기
                    </button>
                  )
                )}
              </div>
            </div>

            {detailErr ? (
              <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">오류: {detailErr}</div>
            ) : !selected ? (
              <div className="text-sm text-gray-800">왼쪽 목록에서 대상을 선택하세요.</div>
            ) : teacherDetail == null ? (
              <div className="text-sm text-gray-800">표시할 선생님 정보가 없습니다.</div>
            ) : (
              <>
                {/* 보기 모드 */}
                {!editing && (
                  <div className="grid grid-cols-1 gap-3">
                    <FieldBox
                      label="이름"
                      value={
                        teacherDetail?.Teacher_Name ??
                        teacherDetail?.teacherName ??
                        teacherDetail?.name ??
                        selected?.name ?? "—"
                      }
                    />
                    <FieldBox
                      label="아이디"
                      value={
                        teacherDetail?.Teacher_ID ??
                        teacherDetail?.teacherId ??
                        teacherDetail?.id ??
                        teacherDetail?.username ??
                        selected?.id ?? "—"
                      }
                    />
                    <FieldBox
                      label="전화"
                      value={
                        teacherDetail?.Teacher_Phone_Number ??
                        teacherDetail?.teacherPhoneNumber ??
                        teacherDetail?.phone ??
                        teacherDetail?.mobile ??
                        selected?.phone ?? "—"
                      }
                    />
                    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
                      <div className="text-[11px] text-gray-700">학원번호</div>
                      {(() => {
                        const academies = normalizeAcademyNumbers(teacherDetail ?? selected);
                        return academies.length === 0 ? (
                          <div className="text-sm font-medium text-gray-900 mt-0.5">—</div>
                        ) : (
                          <div className="mt-1 flex flex-wrap gap-1.5">
                            {academies.map((n) => (<Badge key={n}>#{n}</Badge>))}
                          </div>
                        );
                      })()}
                    </div>

                    {/* 학원별 과목 블록만 표시 */}
                    {(() => {
                      const map = detailAcaSubjects || {};
                      const academiesAll = normalizeAcademyNumbers(teacherDetail ?? selected);
                      const keysFromMap = Object.keys(map).map((k) => Number(k));
                      const order = Array.from(new Set([...academiesAll, ...keysFromMap])).sort((a, b) => a - b);
                      if (order.length === 0) return null;
                      return (
                        <>
                          {order.map((n) => (
                            <FieldBox
                              key={`aca-${n}`}
                              label={`학원 #${n} 과목`}
                              value={(map[n] && map[n].length) ? map[n].join(", ") : "—"}
                            />
                          ))}
                        </>
                      );
                    })()}
                  </div>
                )}

                {/* 편집 모드 — 학생/학부모 수정폼과 동일 스타일 */}
                {editing && (
                  <div className="space-y-3">
                    {saveErr && (
                      <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">
                        저장 오류: {saveErr}
                      </div>
                    )}

                    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
                      <div className="text-[11px] text-gray-700 mb-1.5">아이디</div>
                      <input
                        className="border border-gray-400 rounded w-full px-3 py-2 text-sm bg-white text-gray-900 placeholder-gray-500"
                        value={formId}
                        onChange={(e) => setFormId(e.target.value)}
                        placeholder="teacher id"
                      />
                      <div className="mt-1 text-[11px] text-gray-500">아이디 변경 시 중복되지 않아야 합니다.</div>
                    </div>

                    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
                      <div className="text-[11px] text-gray-700 mb-1.5">이름</div>
                      <input
                        className="border border-gray-400 rounded w-full px-3 py-2 text-sm bg-white text-gray-900 placeholder-gray-500"
                        value={formName}
                        onChange={(e) => setFormName(e.target.value)}
                        placeholder="이름"
                      />
                    </div>

                    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
                      <div className="text-[11px] text-gray-700 mb-1.5">전화</div>
                      <input
                        className="border border-gray-400 rounded w-full px-3 py-2 text-sm bg-white text-gray-900 placeholder-gray-500"
                        value={formPhone}
                        onChange={(e) => setFormPhone(e.target.value)}
                        placeholder="숫자/하이픈"
                      />
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

/** =========================================================================================
 *  DirectorPeoplePanel
 * ========================================================================================= */
export default function DirectorPeoplePanel() {
  const [mode, setMode] = useState<"students" | "teachers">("students");
  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">소속 인원 관리</h2>
          <p className="text-sm text-gray-600 mt-1">
            원장 소속 학원 범위에서 <span className="font-medium text-gray-900">학생·학부모</span> 또는{" "}
            <span className="font-medium text-gray-900">선생님</span>을 전환해 조회합니다.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setMode("students")}
            className={`px-4 py-2 rounded-lg ring-1 transition text-white ${
              mode === "students" ? "bg-black ring-black" : "bg-black/70 hover:bg-black"
            }`}
            title="학생/학부모 관리"
          >
            학생/학부모
          </button>
          <button
            onClick={() => setMode("teachers")}
            className={`px-4 py-2 rounded-lg ring-1 transition text-white ${
              mode === "teachers" ? "bg-black ring-black" : "bg-black/70 hover:bg-black"
            }`}
            title="선생님 검색"
          >
            선생님
          </button>
        </div>
      </div>

      {mode === "students" ? <TeacherStudentManage /> : <DirectorTeacherSearchPanel />}
    </div>
  );
}
