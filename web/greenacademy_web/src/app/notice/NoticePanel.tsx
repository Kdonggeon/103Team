// C:\project\103Team-sub\web\greenacademy_web\src\app\notice\NoticePanel.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";
import NoticeEditorPanel from "./NoticeEditorPanel";
import NoticeDetailPanel from "./NoticeDetailPanel";

/** API 베이스 & fetch 래퍼 */
const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ??
  (typeof window !== "undefined" ? `${location.protocol}//${location.hostname}:9090` : "");

async function fetchApi(path: string, init?: RequestInit) {
  const url = `${API_BASE}${path}`;
  const opts: RequestInit = { credentials: init?.credentials ?? "include", ...init };
  const res = await fetch(url, opts);
  const ct = res.headers.get("content-type") || "";
  // Next의 404 HTML을 받은 경우 로컬 9090로 재시도
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

type Notice = {
  id: string;
  title: string;
  content: string;
  author?: string;
  createdAt?: string;
  academyNumbers?: number[]; // 배열 스키마
  academyNumber?: number; // 구버전 단일 스키마(혼재 대비)
  classId?: string | null;
  className?: string | null; // 일부 공지에 이름이 직접 들어올 수도 있음
};

type Academy = { academyNumber?: number; name?: string };
type ClassLite = { id: string; name: string };

/** 스페셜 값: ‘내 과목 전체’ */
const MY_CLASSES_ALL = "__MY_CLASSES_ALL__";

function authHeaders(session: Session | null): HeadersInit {
  return {
    "Content-Type": "application/json",
    ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
  };
}

/** 숫자 정규화 */
function normAcadNum(v: any): number | null {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

/** 단일/배열 스키마 → 항상 배열로 정규화 */
function normalizeAcademies(n: Notice): Notice {
  const nums = Array.isArray(n.academyNumbers)
    ? n.academyNumbers
    : typeof n.academyNumber === "number"
    ? [n.academyNumber]
    : [];
  return { ...n, academyNumbers: nums };
}

/** 다양한 케이스에서 name/id 추출 */
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

function pickId(obj: any): string {
  const raw =
    obj?.id ??
    obj?.classId ??
    obj?.Class_ID ??
    obj?.class_id ??
    obj?.courseId ??
    obj?.Course_ID ??
    obj?.course_id ??
    "";
  return raw != null ? String(raw).trim() : "";
}

/** 교사 정보 추출(원장 분기에서 사용) */
function pickTeacherId(t: any): string {
  const raw = t?.teacherId ?? t?.Teacher_ID ?? t?.id ?? t?.username ?? "";
  return raw != null ? String(raw).trim() : "";
}
function pickTeacherNameFromTeacher(t: any): string {
  const raw = t?.teacherName ?? t?.Teacher_Name ?? t?.name ?? "";
  return raw != null ? String(raw).trim() : "";
}
/** 과목명에 (교사명) 붙이기, 중복 방지 */
function appendTeacher(base: string, teacherName: string): string {
  const b = (base ?? "").toString().trim();
  const tn = (teacherName ?? "").toString().trim();
  if (!tn) return b;
  if (!b) return `(${tn})`;
  if (b.endsWith(`(${tn})`) || b.includes(`(${tn})`)) return b;
  return `${b}(${tn})`;
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

/** 이름 채우기: /api/lookup/classes/names?ids=... (있으면 사용) */
async function fetchClassNamesBulk(ids: string[], session: Session | null): Promise<Record<string, string>> {
  if (!session?.token || ids.length === 0) return {};
  const headers = authHeaders(session);
  const query = encodeURIComponent(ids.join(","));
  try {
    const r = await fetchApi(`/api/lookup/classes/names?ids=${query}`, { headers });
    if (!r.ok) return {};
    const data = await r.json();
    const map: Record<string, string> = {};
    if (Array.isArray(data)) {
      for (const it of data) {
        const id = pickId(it);
        const name = pickName(it);
        if (id && name) map[id] = name;
      }
    } else if (data && typeof data === "object") {
      for (const [k, v] of Object.entries<any>(data)) {
        const name = pickName({ name: v });
        if (k && name) map[String(k)] = name;
      }
    }
    return map;
  } catch {
    return {};
  }
}

/** classes 병합 유틸 */
function mergeClassNames(list: ClassLite[], nameMap: Record<string, string>): ClassLite[] {
  const byId: Record<string, ClassLite> = {};
  for (const c of list) byId[c.id] = { ...c };
  for (const [id, name] of Object.entries(nameMap)) {
    if (!byId[id]) byId[id] = { id, name };
    else if (name && !byId[id].name) byId[id].name = name;
  }
  return Object.values(byId);
}

export default function NoticePanel() {
  const [session, setSession] = useState<Session | null>(null);
  const [createMode, setCreateMode] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // 데이터
  const [academies, setAcademies] = useState<Academy[]>([]);
  const [classes, setClasses] = useState<ClassLite[]>([]);
  const [notices, setNotices] = useState<Notice[]>([]);

  // 선택값
  const [selAcademy, setSelAcademy] = useState<string>("");
  const [selClass, setSelClass] = useState<string>("");

  // 상태
  const [loadingMeta, setLoadingMeta] = useState(false);
  const [loadingList, setLoadingList] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /** 세션 로드 */
  useEffect(() => {
    const raw = localStorage.getItem("login");
    if (!raw) return;
    try {
      setSession(JSON.parse(raw) as Session);
    } catch {
      setSession(null);
    }
  }, []);

  /** 공지 로드 */
  async function refreshNotices() {
    if (!session?.token) return;
    try {
      setLoadingList(true);
      setError(null);
      const r = await fetchApi("/api/notices", { headers: authHeaders(session) });
      if (!r.ok) {
        if (r.status === 401) throw new Error("로그인이 필요합니다. (401)");
        if (r.status === 403) throw new Error("공지 조회 권한이 없습니다. (403)");
        throw new Error(await r.text());
      }
      const raw = (await r.json()) as Notice[];
      setNotices((raw ?? []).map(normalizeAcademies));
    } catch (e: any) {
      setError(e?.message || "공지 목록을 불러오지 못했습니다.");
    } finally {
      setLoadingList(false);
    }
  }

  /** 학원 목록 (최종적으로 session.academyNumbers로 필터) */
  useEffect(() => {
    if (!session?.token) return;
    (async () => {
      const isStaff = session?.role === "teacher" || session?.role === "director";
      const allowedNums = new Set<number>(
        (session?.academyNumbers ?? []).map(normAcadNum).filter((n): n is number => n !== null)
      );

      try {
        setLoadingMeta(true);
        setError(null);

        let fetched: Academy[] = [];
        if (isStaff) {
          const r = await fetchApi("/api/academy", { headers: authHeaders(session) });
          if (r.ok) {
            fetched = (((await r.json()) as Academy[]) ?? []).filter(
              (a) => normAcadNum(a?.academyNumber) !== null
            );
          } else if (r.status !== 403) {
            if (r.status === 401) throw new Error("로그인이 필요합니다. (401)");
            throw new Error(await r.text());
          }
        }

        let filtered = fetched.filter((a) => {
          const n = normAcadNum(a?.academyNumber);
          return n !== null && allowedNums.has(n);
        });

        const have = new Set(filtered.map((a) => String(a.academyNumber)));
        for (const n of allowedNums) {
          if (!have.has(String(n))) {
            filtered.push({ academyNumber: n, name: "학원" });
          }
        }

        if (!isStaff) {
          filtered = Array.from(allowedNums).map((n) => ({ academyNumber: n, name: "학원" }));
        }

        filtered.sort(
          (a, b) => (normAcadNum(a.academyNumber) ?? 0) - (normAcadNum(b.academyNumber) ?? 0)
        );
        setAcademies(filtered);
      } catch (e: any) {
        setError(e?.message || "학원 목록을 불러오지 못했습니다.");
        const fallback = (session?.academyNumbers ?? []).map((n) => ({ academyNumber: n, name: "학원" }));
        setAcademies(fallback);
      } finally {
        setLoadingMeta(false);
      }
    })();
  }, [session?.token, session?.role, session?.academyNumbers]);

  /** 과목 목록: 역할별 */
  useEffect(() => {
    if (!session?.token) return;
    (async () => {
      try {
        setLoadingMeta(true);
        setClasses([]);

        const headers = authHeaders(session);

        if (session.role === "teacher") {
          const r = await fetchApi(
            `/api/manage/teachers/${encodeURIComponent(session.username)}/classes`,
            { headers }
          );
          if (r.ok) {
            const arr = (await r.json()) as any[];
            setClasses(normalizeClassList(arr));
          }
        } else if (session.role === "student") {
          let loaded: ClassLite[] = [];
          const r1 = await fetchApi(
            `/api/lookup/classes/by-student/${encodeURIComponent(session.username)}`,
            { headers }
          );
          if (r1.ok) loaded = normalizeClassList((await r1.json()) as any[]);
          if (loaded.length === 0) {
            const r2 = await fetchApi(
              `/api/manage/students/${encodeURIComponent(session.username)}/classes?lite=true`,
              { headers }
            );
            if (r2.ok) loaded = normalizeClassList((await r2.json()) as any[]);
          }
          setClasses(loaded);
        } else if (session.role === "parent") {
          const r1 = await fetchApi(
            `/api/lookup/classes/by-parent/${encodeURIComponent(session.username)}`,
            { headers }
          );
          if (r1.ok) setClasses(normalizeClassList((await r1.json()) as any[]));
        } else if (session.role === "director") {
          const tRes = await fetchApi(`/api/manage/teachers`, { headers });
          if (tRes.ok) {
            const teachers = (await tRes.json()) as any[];
            const bucket: ClassLite[] = [];
            for (const t of teachers ?? []) {
              const tid = pickTeacherId(t);
              if (!tid) continue;
              const tname = pickTeacherNameFromTeacher(t);
              const cRes = await fetchApi(
                `/api/manage/teachers/${encodeURIComponent(tid)}/classes`,
                { headers }
              );
              if (!cRes.ok) continue;
              const arr = (await cRes.json()) as any[];
              const list = normalizeClassList(arr).map((c) => ({
                id: c.id,
                name: appendTeacher(c.name, tname),
              }));
              bucket.push(...list);
            }
            setClasses(normalizeClassList(bucket));
          }
        }
      } catch (e: any) {
        setError(e?.message || "과목 목록을 불러오지 못했습니다.");
      } finally {
        setLoadingMeta(false);
      }
    })();
  }, [session?.token, session?.role, session?.username]);

  /** 최초 공지 */
  useEffect(() => {
    if (!session?.token) return;
    refreshNotices();
  }, [session?.token]);

  /** 현재 화면에서 보이는 모든 과목 id 집계 */
  const idsOnScreen = useMemo(() => {
    const ids = new Set<string>();
    for (const n of notices) {
      const id = (n.classId || "").trim();
      if (id) ids.add(id);
    }
    for (const c of classes) {
      const id = (c.id || "").trim();
      if (id) ids.add(id);
    }
    return Array.from(ids);
  }, [notices, classes]);

  /** 이름이 비어있는 id들만 골라서 일괄 이름조회 → classes에 병합 */
  useEffect(() => {
    (async () => {
      if (!session?.token) return;
      const knownMap = new Map<string, string>();
      for (const c of classes) if (c.name) knownMap.set(c.id, c.name);

      for (const n of notices) {
        const id = (n.classId || "").trim();
        const nm = pickName({ name: n.className, Class_Name: (n as any)?.Class_Name });
        if (id && nm) knownMap.set(id, nm);
      }

      const missing = idsOnScreen.filter((id) => !knownMap.get(id));
      if (missing.length === 0) return;

      const fetched = await fetchClassNamesBulk(missing, session);
      if (Object.keys(fetched).length === 0) return;

      setClasses((prev) => mergeClassNames(prev, fetched));
    })();
  }, [idsOnScreen, session?.token, classes, notices]);

  /** 학원 필터 1차 */
  const byAcademy = useMemo(() => {
    if (!selAcademy) return notices;
    const target = Number(selAcademy);
    return notices.filter((n) => (n.academyNumbers ?? []).includes(target));
  }, [notices, selAcademy]);

  /** id->name 매핑 (공지 우선, API/조회 보강) */
  const classNameMap = useMemo(() => {
    const m = new Map<string, string>();
    for (const n of notices) {
      const id = (n.classId || "").trim();
      const nm = pickName({ name: n.className, Class_Name: (n as any)?.Class_Name });
      if (id && nm) m.set(id, nm);
    }
    for (const c of classes) {
      const id = (c?.id || "").trim();
      const nm = (c?.name || "").trim();
      if (id && nm) m.set(id, nm);
    }
    return m;
  }, [notices, classes]);

  /** 역할/라벨 */
  const role = session?.role;
  const isStaff = role === "teacher" || role === "director";

  const academyAllLabel = useMemo(() => "전체 학원", [isStaff]);

  // ⬇️ 변경: 과목 스피너의 기본 항목 라벨은 항상 "전체"
  const classAllLabel = useMemo(() => "전체", []);

  /** 내 과목 id 집합 (교사/학생/학부모만 의미 있음) */
  const myClassesSet = useMemo(() => {
    const set = new Set<string>();
    for (const c of classes) {
      const id = (c.id || "").trim();
      if (id) set.add(id);
    }
    return set;
  }, [classes]);

  const showMyClassesOption = useMemo(() => {
    if (!role || role === "director") return false;
    return myClassesSet.size > 0;
  }, [role, myClassesSet]);

  /** 과목 스피너 옵션 구성 */
  const classOptions = useMemo(() => {
    const ids = new Set<string>();
    for (const n of byAcademy) {
      const id = (n.classId || "").trim();
      if (id) ids.add(id);
    }
    for (const c of classes) {
      const id = (c.id || "").trim();
      if (id) ids.add(id);
    }
    const list = Array.from(ids)
      .map((id) => {
        const label = classNameMap.get(id) || id;
        return { value: id, label };
      })
      .sort((a, b) => a.label.localeCompare(b.label, "ko"));

    const base: { value: string; label: string }[] = [{ value: "", label: classAllLabel }];
    // ⬇️ 변경: MY_CLASSES_ALL 라벨을 역할에 따라 분기
    if (showMyClassesOption) {
      base.push({
        value: MY_CLASSES_ALL,
        label: role === "parent" ? "자녀 과목 전체" : "내 과목 전체",
      });
    }
    return [...base, ...list];
  }, [byAcademy, classes, classNameMap, classAllLabel, showMyClassesOption, role]);

  /** selClass 유효성 */
  useEffect(() => {
    if (!selClass) return;
    const exists = classOptions.some((o) => o.value === selClass);
    if (!exists) setSelClass("");
  }, [classOptions, selClass]);

  /** 학원 변경 시 과목 필터 초기화 (혼동 방지) */
  useEffect(() => {
    setSelClass("");
  }, [selAcademy]);

  /** selAcademy 유효성 */
  useEffect(() => {
    const allowedValues = new Set(
      academies.map((a) => String(a.academyNumber ?? "")).filter((v) => v !== "")
    );
    if (selAcademy && !allowedValues.has(selAcademy)) {
      setSelAcademy("");
    }
  }, [academies, selAcademy]);

  /** 과목 필터 2차 */
  const filtered = useMemo(() => {
    let base = byAcademy;
    if (!selClass) return base;

    if (selClass === MY_CLASSES_ALL) {
      if (myClassesSet.size === 0) return [];
      // ‘내/자녀 과목 전체’: 내 과목(classId)로 지정된 공지만 표시 (학원 전체 공지는 제외)
      return base.filter((n) => {
        const id = (n.classId || "").trim();
        return id && myClassesSet.has(id);
      });
    }

    return base.filter((n) => (n.classId || "") === selClass);
  }, [byAcademy, selClass, myClassesSet]);

  /** 작성 권한 */
  const canWrite = session?.role === "teacher" || session?.role === "director";

  /** 상세 모드 */
  if (selectedId) {
    return (
      <NoticeDetailPanel
        noticeId={selectedId}
        session={session}
        onClose={() => setSelectedId(null)}
        onDeleted={async () => {
          setSelectedId(null);
          await refreshNotices();
        }}
      />
    );
  }

  if (createMode && canWrite) {
    return (
      <section className="max-w-5xl mx-auto p-6 space-y-6">
        <header className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">공지 작성</h1>
          <div className="flex gap-2">
            <button
              onClick={() => setCreateMode(false)}
              className="px-4 h-10 rounded-xl border border-gray-300 text-gray-800 bg-white hover:bg-gray-50"
            >
              목록으로
            </button>
          </div>
        </header>

        <NoticeEditorPanel
          onClose={() => setCreateMode(false)}
          onCreated={async () => {
            setCreateMode(false);
            await refreshNotices();
          }}
        />
      </section>
    );
  }

  return (
    <section className="max-w-5xl mx-auto p-6 space-y-4">
      {/* 상단 툴바 */}
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-2xl font-bold text-gray-900 truncate">공지사항</h1>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <Spinner
            label="학원"
            value={selAcademy}
            onChange={setSelAcademy}
            loading={loadingMeta}
            options={[
              { value: "", label: academyAllLabel },
              ...academies
                .map((a) => ({
                  value: String(a.academyNumber ?? ""),
                  label:
                    a.academyNumber != null ? `${a.name ?? "학원"} (#${a.academyNumber})` : a.name ?? "학원",
                }))
                .filter((o) => o.value !== ""),
            ]}
            compact
            className="w-40 md:w-44"
          />
          <Spinner
            label="과목"
            value={selClass}
            onChange={setSelClass}
            loading={loadingMeta}
            options={classOptions}
            compact
            className="w-36 md:w-40"
          />
          {canWrite && (
            <button
              onClick={() => setCreateMode(true)}
              className="ml-1 px-4 h-10 rounded-xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600"
            >
              + 새 공지
            </button>
          )}
        </div>
      </div>

      {/* 목록 카드 */}
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm">
        {error && <div className="p-4 text-sm text-red-600 bg-red-50 border-b border-red-200">{error}</div>}

        {loadingList ? (
          <ListSkeleton />
        ) : filtered.length === 0 ? (
          <div className="p-8 text-center text-gray-600">
            <div className="mx-auto mb-2 h-10 w-10 rounded-full bg-gray-100 flex items-center justify-center">
              <span className="text-lg">🗒️</span>
            </div>
            표시할 공지사항이 없습니다.
          </div>
        ) : (
          <ul className="divide-y divide-gray-200">
            {filtered.map((n) => (
              <li
                key={n.id}
                className="p-4 sm:p-5 hover:bg-gray-50 transition cursor-pointer"
                onClick={() => setSelectedId(n.id)}
              >
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                  <div className="min-w-0">
                    <h3 className="text-base sm:text-lg font-semibold text-gray-900 truncate">
                      {n.title ?? "(제목 없음)"}
                    </h3>
                    <p className="mt-1 text-sm text-gray-600 line-clamp-2">{n.content}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <div className="text-sm text-gray-900">{n.author ?? "관리자"}</div>
                    <div className="text-xs text-gray-500">{n.createdAt ? formatKST(n.createdAt) : ""}</div>
                    <div className="mt-1 flex flex-wrap gap-1 justify-end">
                      {/* 과목명 칩: classNameMap 우선 → 공지 className 보강 → id 폴백 */}
                      <ClassChip
                        id={n.classId ?? undefined}
                        label={
                          (n.classId ? (classNameMap.get(n.classId) ?? "") : "") ||
                          pickName({ name: n.className, Class_Name: (n as any)?.Class_Name })
                        }
                      />
                      <AcademyBadges nums={n.academyNumbers ?? []} />
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

/** 학원번호 칩 */
function AcademyBadges({ nums, max = 3 }: { nums: number[]; max?: number }) {
  if (!nums || nums.length === 0) return null;
  const shown = nums.slice(0, max);
  const rest = nums.length - shown.length;
  return (
    <>
      {shown.map((n) => (
        <span
          key={`acad-${n}`}
          className="inline-flex items-center rounded-full bg-gray-100 text-gray-700 text-[11px] px-2 py-0.5"
          title={`학원 #${n}`}
        >
          #{n}
        </span>
      ))}
      {rest > 0 && (
        <span className="inline-flex items-center rounded-full bg-gray-100 text-gray-700 text-[11px] px-2 py-0.5">{`+${rest}`}</span>
      )}
    </>
  );
}

/** 과목명 칩 */
function ClassChip({ id, label }: { id?: string; label?: string }) {
  if (!id) return null;
  const text = (label ?? "").toString().trim() || id; // 이름 없으면 id로 폴백
  return (
    <span
      className="inline-flex items-center rounded-full bg-gray-100 text-gray-700 text-[11px] px-2 py-0.5"
      title={`과목 ${text}`}
    >
      {text}
    </span>
  );
}

/** 공통 스피너 */
function Spinner({
  label,
  value,
  onChange,
  options,
  loading,
  hint,
  compact,
  className,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  loading?: boolean;
  hint?: string;
  compact?: boolean;
  className?: string;
}) {
  const selectBase =
    "w-full rounded-xl border border-gray-300 px-3 bg-white text-gray-900 focus:ring-2 focus:ring-emerald-300 focus:border-emerald-300";
  const selectSize = compact ? "h-9 text-sm" : "h-11";
  return (
    <label className={`block ${className ?? ""}`}>
      <span className={`block ${compact ? "text-xs mb-0.5" : "text-sm mb-1"} text-gray-900`}>{label}</span>
      <select
        className={`${selectBase} ${selectSize}`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={loading}
      >
        {options.map((o) => (
          <option key={`${label}-${o.value}`} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {/* hint는 사용 안 함 */}
      {false && hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
    </label>
  );
}

function ListSkeleton() {
  return (
    <ul className="divide-y divide-gray-200 animate-pulse">
      {Array.from({ length: 6 }).map((_, i) => (
        <li key={i} className="p-4 sm:p-5">
          <div className="flex items-center justify-between">
            <div className="h-4 bg-gray-200 rounded w-2/3" />
            <div className="ml-4 h-3 bg-gray-200 rounded w-24" />
          </div>
          <div className="mt-2 h-3 bg-gray-200 rounded w-5/6" />
          <div className="mt-1 h-3 bg-gray-200 rounded w-3/4" />
        </li>
      ))}
    </ul>
  );
}

/** yyyy-mm-dd hh:mm (KST) */
function formatKST(iso?: string) {
  if (!iso) return "";
  try {
    const d = new Date(iso);
    const y = d.getFullYear();
    const m = `${d.getMonth() + 1}`.padStart(2, "0");
    const day = `${d.getDate()}`.padStart(2, "0");
    const hh = `${d.getHours()}`.padStart(2, "0");
    const mm = `${d.getMinutes()}`.padStart(2, "0");
    return `${y}-${m}-${day} ${hh}:${mm}`;
  } catch {
    return "";
  }
}
