// C:\project\103Team-sub\web\greenacademy_web\src\app\teacher\StudentManage.tsx
"use client";

import React, {
  useEffect,
  useMemo,
  useState,
  useRef,
  forwardRef,
  useImperativeHandle,
} from "react";
import api from "@/app/lib/api";

/** ================== 유틸/타입 ================== **/
type LoginSession = {
  role?: string;
  username?: string;
  name?: string;
  token?: string;
  academyNumbers?: number[];
};

type PersonListItem = {
  id: string;
  name?: string | null;
  role: "student";
  grade?: number | null;
  school?: string | null;
  gender?: string | null;
  academyNumbers?: number[];
  parentNames?: string[];
};

type StudentDetail = Record<string, any>;
type ParentDetail = Record<string, any>;

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
function isEmpty(v: any) {
  if (v == null) return true;
  if (typeof v === "string" && v.trim() === "") return true;
  if (Array.isArray(v) && v.length === 0) return true;
  if (typeof v === "object" && !Array.isArray(v) && Object.keys(v).length === 0) return true;
  return false;
}
function ensureArray(raw: any): any[] {
  if (Array.isArray(raw)) return raw;
  if (Array.isArray(raw?.content)) return raw.content;
  if (Array.isArray(raw?.data)) return raw.data;
  return [];
}
function ensureParentArray(raw: any): any[] {
  if (Array.isArray(raw)) return raw;
  const containers = [raw, raw?.data, raw?.content, raw?.result, raw?.payload];
  const arrayKeys = ["parents", "parentsList", "parentList", "items", "results", "rows", "list", "value", "values", "data", "content"];
  for (const box of containers) {
    if (!box || typeof box !== "object") continue;
    for (const k of arrayKeys) {
      if (Array.isArray((box as any)[k])) return (box as any)[k];
    }
    if (box.parent && typeof box.parent === "object" && !Array.isArray(box.parent)) return [box.parent];
    if (box.parents && typeof box.parents === "object" && !Array.isArray(box.parents)) return [box.parents];
  }
  if (raw && typeof raw === "object") {
    const parentKeys = ["Parent_ID", "parentsId", "parentId", "username"];
    if (parentKeys.some((k) => k in raw)) return [raw];
  }
  return [];
}
function toNumArr(v: any): number[] {
  const arr = Array.isArray(v) ? v : v == null ? [] : [v];
  return arr.map(toNumber).filter((n): n is number => typeof n === "number");
}
function hasAcaOverlap(a?: number[] | null, b?: number[] | null) {
  if (!Array.isArray(a) || a.length === 0) return false;
  if (!Array.isArray(b) || b.length === 0) return false;
  const set = new Set(b);
  return a.some((n) => set.has(n));
}
function normalizeId(x: any) {
  return String(x ?? "").trim();
}
function extractLinkedStudentIdsFromParent(p: any): string[] {
  const fromList =
    (Array.isArray(p.Student_ID_List) && p.Student_ID_List) ||
    (Array.isArray(p.studentIdList) && p.studentIdList) ||
    [];
  const kids =
    (Array.isArray(p.students) && p.students) ||
    (Array.isArray(p.children) && p.children) ||
    (Array.isArray(p.linkedStudents) && p.linkedStudents) ||
    [];
  const idsFromKids = kids
    .map((c: any) => c?.Student_ID ?? c?.studentId ?? c?.id)
    .filter(Boolean);
  const all = [...fromList, ...idsFromKids].map(normalizeId).filter(Boolean);
  return Array.from(new Set(all.map((s) => s.toLowerCase())));
}
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
async function apiPatchOrPut<T>(
  path: string,
  body: any,
  token?: string,
  extraHeaders?: Record<string, string>
): Promise<T> {
  const url = path.startsWith("/backend") ? path : `/backend${path}`;
  const headers = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(extraHeaders ?? {}),
  };

  let r = await fetch(url, { method: "PATCH", headers, body: JSON.stringify(body) });
  let text = await r.text();
  if (r.ok) {
    try {
      return text ? (JSON.parse(text) as T) : ({} as T);
    } catch {
      return {} as T;
    }
  }

  if (r.status === 405) {
    r = await fetch(url, { method: "PUT", headers, body: JSON.stringify(body) });
    text = await r.text();
    if (!r.ok) throw new Error(`${r.status} ${r.statusText}${text ? " | " + text : ""}`);
    try {
      return text ? (JSON.parse(text) as T) : ({} as T);
    } catch {
      return {} as T;
    }
  }

  throw new Error(`${r.status} ${r.statusText}${text ? " | " + text : ""}`);
}

/** 🔧 학원번호 정규화 */
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
      const cand = v.Academy_Numbers ?? v.Academy_Number ?? v.academyNumbers ?? v.academies ?? v.academyNumber ?? v["AcademyNumbers"];
      if (cand != null) return eat(cand);
    }
    return [];
  };
  return Array.from(new Set(eat(src)));
}

/** UI 조각 */
function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-black">
      <svg className="h-4 w-4 animate-spin text-black" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" className="opacity-25" />
        <path d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="4" strokeLinecap="round" className="opacity-90" />
      </svg>
      {label && <span>{label}</span>}
    </div>
  );
}
function Info({ label, value }: { label: string; value: React.ReactNode }) {
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
function Toast({ message, onClose }: { message: string; onClose: () => void }) {
  useEffect(() => {
    const t = setTimeout(onClose, 1800);
    return () => clearTimeout(t);
  }, [onClose]);
  if (!message) return null;
  return (
    <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-[1000]">
      <div className="rounded-xl bg-black text-white text-sm px-4 py-2 shadow-lg">
        {message}
      </div>
    </div>
  );
}

/** ================== 메인 ================== **/
export default function TeacherStudentManage() {
  const login = readLogin();
  const token = login?.token ?? "";

  const [toastMsg, setToastMsg] = useState("");

  type AcademyFilter = number | "ALL";
  const [academyNumber, setAcademyNumber] = useState<AcademyFilter>("ALL");
  const [idQ, setIdQ] = useState("");
  const [nameQ, setNameQ] = useState("");

  const [parentNameQ, setParentNameQ] = useState("");
  const [grade, setGrade] = useState<string>("");
  const [school, setSchool] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(false);

  const [sortKey, setSortKey] = useState<"name" | "school" | "grade">("name");

  function sortByKey(list: PersonListItem[], key: "name" | "school" | "grade") {
    const arr = [...list];
    arr.sort((a, b) => {
      if (key === "name") return (a.name ?? "").localeCompare(b.name ?? "");
      if (key === "school") return (a.school ?? "").localeCompare(b.school ?? "");
      const ga = a.grade ?? 0, gb = b.grade ?? 0;
      return ga - gb;
    });
    return arr;
  }

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [hits, setHits] = useState<PersonListItem[]>([]);
  const [selected, setSelected] = useState<PersonListItem | null>(null);

  const [detailLoading, setDetailLoading] = useState(false);
  const [detailErr, setDetailErr] = useState<string | null>(null);
  const [studentDetail, setStudentDetail] = useState<StudentDetail | null>(null);
  const [studentParents, setStudentParents] = useState<ParentDetail[]>([]);

  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  const studentViewRef = useRef<{ save: () => void; reset: () => void } | null>(null);

  const academyOptions: number[] = useMemo(() => {
    const arr = Array.isArray(login?.academyNumbers) ? login!.academyNumbers! : [];
    return Array.from(new Set(arr.filter(Number.isFinite)));
  }, [login]);

  useEffect(() => {
    setHits((prev) => sortByKey(prev, sortKey));
  }, [sortKey]);

  const chunk = <T,>(arr: T[], size: number) =>
    arr.reduce<T[][]>((a, _, i) => (i % size ? a : [...a, arr.slice(i, i + size)]), []);

  async function fetchParentsForStudents(ids: string[], tokenStr: string): Promise<Record<string, ParentDetail[]>> {
    const out: Record<string, ParentDetail[]> = {};
    const groups = chunk(ids, 6);
    for (const g of groups) {
      await Promise.all(
        g.map(async (_sid) => {
          const sid = normalizeId(_sid);
          const sidLc = sid.toLowerCase();
          let parents: ParentDetail[] = [];
          try {
            const raw = await apiGet<any>(`/api/students/${encodeURIComponent(sid)}/parents`, tokenStr);
            parents = ensureParentArray(raw);
          } catch { parents = []; }
          if (!parents.length) {
            try {
              const raw2 = await apiGet<any>(`/api/parents?studentId=${encodeURIComponent(sid)}`, tokenStr);
              const arr2 = ensureArray(raw2);
              parents = arr2.filter((p: any) => extractLinkedStudentIdsFromParent(p).includes(sidLc));
            } catch {}
          }
          if (!parents.length) {
            try {
              const raw3 = await apiGet<any>(`/api/parents/by-student/${encodeURIComponent(sid)}`, tokenStr);
              const arr3 = ensureArray(raw3);
              parents = arr3.filter((p: any) => extractLinkedStudentIdsFromParent(p).includes(sidLc));
            } catch {}
          }
          out[sid] = parents;
        })
      );
    }
    return out;
  }

  function matchName(nm?: string | null, q?: string): boolean {
    const name = (nm ?? "").toLowerCase().trim();
    const query = (q ?? "").toLowerCase().trim();
    if (!query) return true;
    if (!name) return false;
    return name === query || name.startsWith(query) || name.includes(query);
  }

  async function renameParentId(oldId: string, newId: string, token?: string, extraHeaders?: Record<string,string>) {
    const tried: string[] = [];
    const headers = {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders ?? {}),
    };
    async function tryCall(method: "PATCH"|"POST"|"PUT", url: string, body: any) {
      tried.push(`${method} ${url}`);
      const r = await fetch(url.startsWith("/backend") ? url : `/backend${url}`, {
        method, headers, body: JSON.stringify(body),
      });
      const text = await r.text();
      if (!r.ok) throw new Error(`${r.status} ${r.statusText}${text ? " | " + text : ""}`);
    }
    try { await tryCall("PATCH", `/api/parents/${encodeURIComponent(oldId)}/change-id`, { newId }); return; } catch {}
    try { await tryCall("PATCH", `/api/manage/teachers/parents/${encodeURIComponent(oldId)}/change-id`, { newId }); return; } catch {}
    try { await tryCall("POST", `/api/parents/rename`, { oldId, newId }); return; } catch {}
    try { await tryCall("POST", `/api/manage/parents/rename`, { oldId, newId }); return; } catch {}
    try { await tryCall("PUT", `/api/parents/${encodeURIComponent(oldId)}?newId=${encodeURIComponent(newId)}`, {}); return; } catch {}
    try {
      await tryCall("PATCH", `/api/parents/${encodeURIComponent(oldId)}`, {
        newId, parentId: newId, Parent_ID: newId, parentsId: newId, username: newId, userId: newId, User_ID: newId,
      });
      return;
    } catch (e: any) {
      throw new Error(`아이디 변경 실패: ${(e?.message ?? e)}\n시도 경로:\n- ${tried.join("\n- ")}`);
    }
  }

  /** ================== 검색 ================== **/
  const search = async () => {
    setLoading(true);
    setErr(null);
    setSelected(null);
    setStudentDetail(null);
    setStudentParents([]);
    setDetailErr(null);
    setEditing(false);

    try {
      const idTrim = idQ.trim().toLowerCase();
      const nameTrim = nameQ.trim().toLowerCase();
      const hasId = idTrim.length > 0;
      const hasName = nameTrim.length > 0;

      const gradeTrim = grade.trim();
      const gradeValid = gradeTrim !== "" && Number.isFinite(Number(gradeTrim));
      const gradeNum = gradeValid ? Number(gradeTrim) : undefined;
      const schoolTrim = school.trim();
      const parentTrim = parentNameQ.trim().toLowerCase();
      const effectiveAca: AcademyFilter = academyNumber !== "ALL" ? (academyNumber as number) : "ALL";

      let rawAll: any[] = [];
      try {
        const raw = await apiGet<any>(`/api/students`, token);
        rawAll = ensureArray(raw);
      } catch {
        rawAll = [];
      }

      let list: PersonListItem[] = (rawAll || [])
        .map((s: any) => {
          const studentId = s.Student_ID ?? s.studentId ?? s.id ?? s.username ?? "";
          const nums = normalizeAcademyNumbers(s);
          return {
            id: String(studentId),
            name: s.Student_Name ?? s.studentName ?? s.name ?? null,
            role: "student",
            grade: toNumber(s.Grade ?? s.grade) ?? null,
            school: s.School ?? s.school ?? s.schoolName ?? null,
            gender: s.Gender ?? s.gender ?? null,
            academyNumbers: nums,
          } as PersonListItem;
        })
        .filter((x) => !!x.id);

      if (hasId) list = list.filter((x) => String(x.id).toLowerCase().includes(idTrim));
      if (hasName) list = list.filter((x) => matchName(x.name, nameQ));
      if (gradeValid) list = list.filter((x) => (x.grade ?? NaN) === gradeNum);
      if (schoolTrim) {
        const t = schoolTrim.toLowerCase();
        list = list.filter((x) => (x.school ?? "").toLowerCase().includes(t));
      }

      if (effectiveAca !== "ALL") {
        const sel = Number(effectiveAca);
        list = list.filter((x) => (x.academyNumbers ?? []).some((n) => Number(n) === sel));
      }

      const teacherAca = academyOptions;
      if (effectiveAca === "ALL" && teacherAca.length) {
        list = list.filter((x) => hasAcaOverlap(x.academyNumbers, teacherAca));
      }

      const needsEnrich = list.filter((x) => x.role === "student" && (isEmpty(x.school) || isEmpty(x.gender)));
      if (needsEnrich.length) {
        const groups = chunk(needsEnrich, 6);
        const idToDetail: Record<string, { school?: string | null; gender?: string | null; grade?: number | null }> = {};
        for (const g of groups) {
          await Promise.all(
            g.map(async (st) => {
              try {
                const d = await apiGet<any>(`/api/students/${encodeURIComponent(st.id)}`, token);
                idToDetail[st.id] = {
                  school: d.School ?? d.school ?? d.schoolName ?? st.school ?? null,
                  gender: d.Gender ?? d.gender ?? st.gender ?? null,
                  grade: toNumber(d.Grade ?? d.grade) ?? st.grade ?? null,
                };
              } catch {}
            })
          );
        }
        if (Object.keys(idToDetail).length) {
          list = list.map((row) => {
            const e = idToDetail[row.id];
            return e ? { ...row, school: e.school ?? row.school, gender: e.gender ?? row.gender, grade: e.grade ?? row.grade } : row;
          });
        }
      }

      if (list.length) {
        const idList = list.map((x) => x.id);
        const parentsMap = await fetchParentsForStudents(idList, token);
        list = list.map((row) => {
          const parents = parentsMap[row.id] || [];
          const names = parents
            .map((p: any) => p.parentsName ?? p.Parent_Name ?? p.parentName ?? p.Parents_Name ?? p.name)
            .filter(Boolean)
            .map(String);
          return { ...row, parentNames: names };
        });
      }

      if (parentTrim) {
        list = list.filter((row) =>
          (row.parentNames ?? []).some((nm) => nm?.toLowerCase?.().includes(parentTrim))
        );
      }

      list = sortByKey(list, sortKey);

      setHits(list);
    } catch (e: any) {
      setErr(e.message);
      setHits([]);
    } finally {
      setLoading(false);
    }
  };

  /** 행 선택 → 상세 로드 */
  const selKey = selected ? selected.id : "";
  const fetchSeqRef = useRef(0);

  useEffect(() => {
    setStudentDetail((prev) => (prev === null ? prev : null));
    setStudentParents((prev) => (Array.isArray(prev) && prev.length === 0 ? prev : []));
    setDetailErr((prev) => (prev === null ? prev : null));
    setEditing((prev) => (prev ? false : prev));

    if (!selected) return;

    let cancelled = false;
    const seq = ++fetchSeqRef.current;

    (async () => {
      setDetailLoading(true);
      try {
        const sid = selected.id;
        const sidLc = sid.toLowerCase();

        const d = await apiGet<StudentDetail>(`/api/students/${encodeURIComponent(sid)}`, token);
        if (cancelled || fetchSeqRef.current !== seq) return;
        setStudentDetail(d ?? {});

        let parents: ParentDetail[] = [];
        try {
          const raw = await apiGet<any>(`/api/students/${encodeURIComponent(sid)}/parents`, token);
          parents = ensureParentArray(raw);
        } catch {}
        if (!parents.length) {
          try {
            const raw2 = await apiGet<any>(`/api/parents?studentId=${encodeURIComponent(sid)}`, token);
            const arr2 = ensureArray(raw2);
            parents = arr2.filter((p: any) => extractLinkedStudentIdsFromParent(p).includes(sidLc));
          } catch {}
        }
        if (!parents.length) {
          try {
            const raw3 = await apiGet<any>(`/api/parents/by-student/${encodeURIComponent(sid)}`, token);
            const arr3 = ensureArray(raw3);
            parents = arr3.filter((p: any) => extractLinkedStudentIdsFromParent(p).includes(sidLc));
          } catch {}
        }
        if (cancelled || fetchSeqRef.current !== seq) return;
        setStudentParents(parents);
      } catch (e: any) {
        if (!cancelled && fetchSeqRef.current === seq) setDetailErr(e.message);
      } finally {
        if (!cancelled && fetchSeqRef.current === seq) setDetailLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [selKey, token]);

  /** ===== 학생 저장 ===== */
  const handleSaveStudent = async (draft: {
    id?: string;
    name?: string;
    school?: string;
    grade?: number | null;
    gender?: string;
  }) => {
    if (!selected) return;
    const id = selected.id;
    const newId = draft.id?.trim() && draft.id.trim() !== id ? draft.id.trim() : undefined;

    setSaving(true);
    try {
      const teacherAca = academyOptions;
      if (teacherAca.length) {
        const stuAca =
          toNumArr(
            studentDetail?.Academy_Numbers ??
              studentDetail?.Academy_Number ??
              studentDetail?.academyNumbers ??
              studentDetail?.academies ??
              []
          ) || (selected.academyNumbers ?? []);
        if (!hasAcaOverlap(stuAca, teacherAca)) {
          alert("선생 소속 학원번호와 학생의 학원번호가 일치하지 않아 수정할 수 없습니다.");
          setSaving(false);
          return;
        }
      }

      const body: any = {};
      if (draft.name !== undefined) { body.studentName = draft.name; body.Student_Name = draft.name; }
      if (draft.school !== undefined) { body.school = draft.school; body.School = draft.school; }
      if (draft.grade !== undefined) { body.grade = draft.grade; body.Grade = draft.grade; }
      if (draft.gender !== undefined) { body.gender = draft.gender; body.Gender = draft.gender; }
      if (newId !== undefined) {
        body.studentId = newId; body.Student_ID = newId; body.username = newId;
        body.newId = newId; body.newStudentId = newId; body.userId = newId; body.User_ID = newId;
      }

      try {
        await apiPatchOrPut(`/api/students/${encodeURIComponent(id)}`, body, token);
      } catch (e: any) {
        const m = String(e?.message ?? "");
        if (m.includes("403") || m.includes("NO_PERMISSION")) {
          await apiPatchOrPut(`/api/manage/teachers/students/${encodeURIComponent(id)}`, body, token);
        } else {
          throw e;
        }
      }

      const fetchId = newId ?? id;
      let fresh: StudentDetail = {};
      try { fresh = await apiGet<StudentDetail>(`/api/students/${encodeURIComponent(fetchId)}`, token); }
      catch { fresh = await apiGet<StudentDetail>(`/api/students/${encodeURIComponent(id)}`, token); }
      setStudentDetail(fresh ?? {});

      try {
        const sidLc = fetchId.toLowerCase();
        const raw = await apiGet<any>(`/api/students/${encodeURIComponent(fetchId)}/parents`, token);
        const parents = ensureParentArray(raw).filter((p: any) => extractLinkedStudentIdsFromParent(p).includes(sidLc));
        setStudentParents(parents);
        const parentNames = parents
          .map((p: any) => p.parentsName ?? p.Parent_Name ?? p.parentName ?? p.Parents_Name ?? p.name)
          .filter(Boolean).map(String);
        setHits((prev) => {
          const updated = prev.map((row) =>
            row.id === id
              ? { ...row, id: fetchId, name: draft.name ?? row.name, school: draft.school ?? row.school,
                  grade: draft.grade !== undefined ? draft.grade : row.grade, gender: draft.gender ?? row.gender, parentNames }
              : row
          );
          return sortByKey(updated, sortKey);
        });
      } catch {
        setHits((prev) => {
          const updated = prev.map((row) =>
            row.id === id
              ? { ...row, id: fetchId, name: draft.name ?? row.name, school: draft.school ?? row.school,
                  grade: draft.grade !== undefined ? draft.grade : row.grade, gender: draft.gender ?? row.gender }
              : row
          );
          return sortByKey(updated, sortKey);
        });
      }

      setSelected((prev) => (prev ? { ...prev, id: newId ?? prev.id } : prev));
      setEditing(false);
      setToastMsg("학생 정보가 저장되었습니다.");
    } catch (e: any) {
      alert(`학생 저장 실패: ${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  /** ===== 보호자 저장(아이디 인라인 입력 포함) ===== */
  const handleSaveParentInline = async (
    parentId: string,
    draft: { id?: string; name?: string; phone?: string }
  ) => {
    const requestedNewId = draft.id?.trim() && draft.id.trim() !== parentId ? draft.id.trim() : undefined;

    const teacherAca = academyOptions;
    if (teacherAca.length) {
      const p = (studentParents || []).find(
        (x) => (x.parentsId ?? x.Parent_ID ?? x.parentId ?? x.username ?? "") === parentId
      );
      const parentAca = toNumArr(p?.Academy_Numbers ?? p?.Academy_Number ?? p?.academyNumbers ?? []);
      const studentAca = toNumArr(
        studentDetail?.Academy_Numbers ??
          studentDetail?.Academy_Number ??
          studentDetail?.academyNumbers ??
          studentDetail?.academies ?? []
      );
      const acaForCheck = parentAca.length ? parentAca : studentAca;
      if (!hasAcaOverlap(acaForCheck, teacherAca)) {
        alert("선생 소속 학원번호와 학부모의 학원번호가 일치하지 않아 수정할 수 없습니다.");
        return;
      }
    }

    const pAcaHeader =
      toNumArr(
        (studentParents || []).find(
          (pp) => (pp.parentsId ?? pp.Parent_ID ?? pp.parentId ?? pp.username ?? "") === parentId
        )?.Academy_Numbers ??
          (studentParents || []).find(
            (pp) => (pp.parentsId ?? pp.Parent_ID ?? pp.parentId ?? pp.username ?? "") === parentId
          )?.Academy_Number ??
          (studentParents || []).find(
            (pp) => (pp.parentsId ?? pp.Parent_ID ?? pp.parentId ?? pp.username ?? "") === parentId
          )?.academyNumbers ?? []
      )[0] ??
      toNumArr(
        studentDetail?.Academy_Numbers ??
          studentDetail?.Academy_Number ??
          studentDetail?.academyNumbers ??
          studentDetail?.academies ?? []
      )[0];

    const extraHeaders = pAcaHeader ? { "X-Academy-Number": String(pAcaHeader) } : undefined;

    // 1) 아이디 변경 먼저(인라인 입력 반영)
    let targetId = parentId;
    if (requestedNewId) {
      try {
        await renameParentId(parentId, requestedNewId, token, extraHeaders);
        targetId = requestedNewId;
      } catch (e: any) {
        alert(String(e?.message ?? e));
        return;
      }
    }

    // 2) 이름/전화 저장
    const body: any = {};
    if (draft.name !== undefined) {
      body.parentsName = draft.name;
      body.Parent_Name = draft.name;
      body.parentName = draft.name;
      body.Parents_Name = draft.name;
    }
    if (draft.phone !== undefined) {
      body.parentsPhoneNumber = draft.phone;
      body.Parent_Phone_Number = draft.phone;
      body.Parents_Phone_Number = draft.phone;
      body.phone = draft.phone;
      body.mobile = draft.phone;
    }

    if (Object.keys(body).length) {
      const tried: string[] = [];
      const tryUpdate = async (u: string) => {
        tried.push(`PATCH/PUT ${u}`);
        await apiPatchOrPut(u, body, token, extraHeaders);
      };
            const shouldFallback = (err: any) => {
        const m = String(err?.message ?? "");
        return /403|404|405|NO_PERMISSION|Forbidden|Not Found|Method Not Allowed/i.test(m);
      };

      try {
        await tryUpdate(`/api/parents/${encodeURIComponent(targetId)}`);
      } catch (e) {
        if (!shouldFallback(e)) throw e;
        const fallbacks = [
          `/api/manage/teachers/parents/${encodeURIComponent(targetId)}`,
          `/api/teachers/parents/${encodeURIComponent(targetId)}`,
          `/api/manage/parents/${encodeURIComponent(targetId)}`,
        ];
        let ok = false, lastErr: any = e;
        for (const u of fallbacks) {
          try { await tryUpdate(u); ok = true; break; } catch (ee) { lastErr = ee; if (!shouldFallback(ee)) throw ee; }
        }
        if (!ok) throw new Error(`보호자 저장 실패: ${(lastErr as any)?.message ?? lastErr}\n시도 경로:\n- ${tried.join("\n- ")}`);
      }
    }

    // 3) 낙관적 반영
    setStudentParents((prev) => {
      const next = (prev ?? []).map((p) => {
        const pid = (p.parentsId ?? p.Parent_ID ?? p.parentId ?? p.username ?? "").toString();
        if (pid !== parentId) return p;
        const merged = { ...p };
        if (requestedNewId) {
          merged.parentsId = requestedNewId;
          delete (merged as any).Parent_ID;
          delete (merged as any).parentId;
          delete (merged as any).username;
        }
        if (draft.name) {
          merged.parentsName = draft.name;
          merged.Parent_Name = draft.name;
          merged.parentName = draft.name;
          merged.Parents_Name = draft.name;
        }
        if (draft.phone) {
          merged.parentsPhoneNumber = draft.phone;
          merged.Parent_Phone_Number = draft.phone;
          merged.Parents_Phone_Number = draft.phone;
          merged.phone = draft.phone;
          merged.mobile = draft.phone;
        }
        return merged;
      });
      return next;
    });

    // 4) 서버 재조회(비었으면 낙관적 상태 유지)
    if (selected) {
      const sid = selected.id;
      const sidLc = sid.toLowerCase();
      try {
        const raw = await apiGet<any>(`/api/students/${encodeURIComponent(sid)}/parents`, token);
        const parents = ensureParentArray(raw).filter((p: any) =>
          extractLinkedStudentIdsFromParent(p).includes(sidLc)
        );
        if (parents.length) setStudentParents(parents);

        const src = parents.length ? parents : studentParents;
        const names = (src ?? [])
          .map((p: any) => p.parentsName ?? p.Parent_Name ?? p.parentName ?? p.Parents_Name ?? p.name)
          .filter(Boolean)
          .map(String);

        setHits((prev) => {
          const updated = prev.map((row) => (row.id === sid ? { ...row, parentNames: names } : row));
          return sortByKey(updated, sortKey);
        });
      } catch {}
    }

    setToastMsg(requestedNewId ? "보호자 아이디/정보가 저장되었습니다." : "보호자 정보가 저장되었습니다.");
  };

  /** UI */
  return (
    <div className="max-w-screen-2xl mx-auto px-6 py-6 space-y-5 text-gray-900">
      <Toast message={toastMsg} onClose={() => setToastMsg("")} />

      {/* 검색 바 */}
      <div className="relative rounded-2xl bg-white ring-1 ring-gray-300 p-4 md:p-5">
        {/* 상단 검색바 스피너는 제거 (요청사항) */}

        <div className="grid grid-cols-[160px_200px_minmax(280px,1fr)_100px_120px] gap-2 items-center">
          <select
            className="border border-gray-400 rounded px-2 py-2 text-sm bg-white text-gray-900"
            value={academyNumber === "ALL" ? "ALL" : String(academyNumber)}
            onChange={(e) => setAcademyNumber(e.target.value === "ALL" ? "ALL" : Number(e.target.value))}
          >
            <option value="ALL">전체</option>
            {academyOptions.map((n) => (
              <option key={n} value={n}>{n}</option>
            ))}
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
          <div className="mt-3 grid grid-cols-[1fr_160px_1fr] gap-3">
            <div className="flex items-center gap-2">
              <label className="text-[11px] text-gray-700 w-18 shrink-0">보호자명</label>
              <input
                className="border border-gray-400 rounded px-2 py-2 text-sm bg-white text-gray-900 w-full placeholder-gray-500"
                value={parentNameQ}
                onChange={(e) => setParentNameQ(e.target.value)}
                placeholder="보호자 이름"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-[11px] text-gray-700 w-10 shrink-0">학년</label>
              <input
                className="border border-gray-400 rounded px-2 py-2 text-sm bg-white text-gray-900 w-full placeholder-gray-500"
                value={grade}
                onChange={(e) => setGrade(e.target.value)}
                placeholder="숫자"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-[11px] text-gray-700 w-10 shrink-0">학교</label>
              <input
                className="border border-gray-400 rounded px-2 py-2 text-sm bg-white text-gray-900 w-full placeholder-gray-500"
                value={school}
                onChange={(e) => setSchool(e.target.value)}
                placeholder="학교명"
              />
            </div>
          </div>
        )}
      </div>

      {/* 본문 2열 */}
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
                  <option value="school">학교</option>
                  <option value="grade">학년</option>
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
                    <th className="px-3 py-2 text-left">학교</th>
                    <th className="px-3 py-2 text-left">학년</th>
                    <th className="px-3 py-2 text-left">성별</th>
                    <th className="px-3 py-2 text-left">학부모 성함</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {hits.map((r) => {
                    const isSel = selected?.id === r.id;
                    return (
                      <tr
                        key={`student-${r.id}`}
                        className={isSel ? "bg-emerald-50" : "hover:bg-gray-50 cursor-pointer"}
                        onClick={() => setSelected((prev) => (prev && prev.id === r.id ? prev : r))}
                      >
                        <td className="px-3 py-2">
                          <input type="checkbox" readOnly checked={isSel} />
                        </td>
                        <td className="px-3 py-2 font-medium">{r.name ?? r.id}</td>
                        <td className="px-3 py-2">{r.school ?? "—"}</td>
                        <td className="px-3 py-2">{r.grade ?? "—"}</td>
                        <td className="px-3 py-2">{r.gender ?? "—"}</td>
                        <td className="px-3 py-2">
                          {r.parentNames && r.parentNames.length ? r.parentNames.join(", ") : "—"}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* 우: 상세 */}
        <section className="space-y-4">
          <div className="rounded-2xl bg-white ring-1 ring-gray-300 shadow-sm p-5">
            <div className="mb-3 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/10 ring-1 ring-emerald-200 text-emerald-700 font-semibold">
                  {(() => {
                    const nm = (studentDetail?.Student_Name ?? studentDetail?.studentName ?? studentDetail?.name ?? selected?.name ?? selected?.id ?? "U") as string;
                    return nm.toString().trim().charAt(0).toUpperCase();
                  })()}
                </div>
                <div className="text-base font-semibold text-gray-900">학생정보</div>
              </div>

              <div className="flex items-center gap-2">
                {detailLoading && <Spinner />}
                {selected && !detailLoading && (!editing ? (
                  <button
                    onClick={() => setEditing(true)}
                    className="px-3 py-1.5 rounded bg-emerald-600 text-white text-xs hover:bg-emerald-700"
                  >
                    정보 수정
                  </button>
                ) : (
                  <>
                    <button
                      onClick={() => { studentViewRef.current?.reset(); setEditing(false); }}
                      disabled={saving}
                      className="px-3 py-1.5 rounded border border-gray-400 text-xs bg-white hover:bg-gray-50 disabled:opacity-50"
                    >
                      취소
                    </button>
                    <button
                      onClick={() => studentViewRef.current?.save()}
                      disabled={saving}
                      className="px-3 py-1.5 rounded bg-emerald-600 text-white text-xs hover:bg-emerald-700 disabled:opacity-50"
                    >
                      {saving ? "저장 중…" : "저장"}
                    </button>
                  </>
                ))}
              </div>
            </div>

            {detailErr ? (
              <div className="rounded-lg bg-red-50 text-red-700 text-sm px-3 py-2 ring-1 ring-red-200">
                오류: {detailErr}
              </div>
            ) : !selected ? (
              <div className="text-sm text-gray-800">위의 표에서 대상을 선택하세요.</div>
            ) : isEmpty(studentDetail) ? (
              <div className="text-sm text-gray-800">표시할 학생 정보가 없습니다.</div>
            ) : (
              <StudentDetailView
                ref={studentViewRef}
                detail={studentDetail!}
                parents={studentParents}
                fallbackId={selected.id}
                editing={editing}
                saving={saving}
                onSubmit={handleSaveStudent}
                onSaveParentInline={handleSaveParentInline}
              />
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

/** ====== 상세뷰 (학생) + 보호자 섹션/인라인수정 ====== */
const StudentDetailView = forwardRef(function StudentDetailView(
  {
    detail,
    parents,
    fallbackId,
    editing,
    saving,
    onSubmit,
    onSaveParentInline,
  }: {
    detail: StudentDetail;
    parents: ParentDetail[];
    fallbackId: string;
    editing: boolean;
    saving: boolean;
    onSubmit: (draft: {
      id?: string;
      name?: string;
      school?: string;
      grade?: number | null;
      gender?: string;
    }) => void;
    onSaveParentInline: (parentId: string, draft: { id?: string; name?: string; phone?: string }) => Promise<void> | void;
  },
  ref: React.Ref<{ save: () => void; reset: () => void }>
) {
  const initialName = detail.Student_Name ?? detail.studentName ?? detail.name ?? fallbackId;
  const initialId = detail.Student_ID ?? detail.studentId ?? fallbackId;
  const initialSchool = detail.School ?? detail.school ?? detail.schoolName ?? "";
  const initialGrade = toNumber(detail.Grade ?? detail.grade);
  const initialGender = (detail.Gender ?? detail.gender ?? "") as string;

  const academies = useMemo(() => normalizeAcademyNumbers(detail), [detail]);

  const [sid, setSid] = useState<string>(String(initialId ?? ""));
  const [name, setName] = useState<string>(String(initialName ?? ""));
  const [school, setSchool] = useState<string>(String(initialSchool ?? ""));
  const [grade, setGrade] = useState<string>(initialGrade != null ? String(initialGrade) : "");
  const [gender, setGender] = useState<string>(String(initialGender ?? ""));

  useEffect(() => {
    setSid(String(initialId ?? ""));
    setName(String(initialName ?? ""));
    setSchool(String(initialSchool ?? ""));
    setGrade(initialGrade != null ? String(initialGrade) : "");
    setGender(String(initialGender ?? ""));
  }, [initialId, initialName, initialSchool, initialGrade, initialGender]);

  const save = () => {
    const g = grade.trim() === "" ? null : Number(grade);
    onSubmit({
      id: sid.trim(),
      name: name.trim(),
      school: school.trim(),
      grade: g == null || Number.isNaN(g) ? null : g,
      gender: gender.trim(),
    });
  };
  const reset = () => {
    setSid(String(initialId ?? ""));
    setName(String(initialName ?? ""));
    setSchool(String(initialSchool ?? ""));
    setGrade(initialGrade != null ? String(initialGrade) : "");
    setGender(String(initialGender ?? ""));
  };
  useImperativeHandle(ref, () => ({ save, reset }));

  return (
    <div className="space-y-4">
      {!editing ? (
        <div className="grid grid-cols-1 gap-3">
          <Info label="이름" value={initialName} />
          <Info label="아이디" value={initialId} />
          <Info label="학교" value={initialSchool || "—"} />
          <Info label="학년" value={initialGrade ?? "—"} />
          <Info label="성별" value={initialGender || "—"} />
          <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
            <div className="text-[11px] text-gray-700">학원번호</div>
            {academies.length === 0 ? (
              <div className="text-sm font-medium text-gray-900 mt-0.5">—</div>
            ) : (
              <div className="mt-1 flex flex-wrap gap-1.5">
                {academies.map((n: number) => (
                  <Badge key={n}>#{n}</Badge>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3">
          <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
            <div className="text:[11px] text-gray-700">아이디</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={sid}
              onChange={(e) => setSid(e.target.value)}
              placeholder="학생 아이디"
            />
          </div>

          <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
            <div className="text-[11px] text-gray-700">이름</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
            <div className="text-[11px] text-gray-700">학교</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={school}
              onChange={(e) => setSchool(e.target.value)}
            />
          </div>

          <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
            <div className="text-[11px] text-gray-700">학년</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={grade}
              onChange={(e) => setGrade(e.target.value)}
              placeholder="숫자 또는 비움"
            />
          </div>

          <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
            <div className="text-[11px] text-gray-700">성별</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={gender}
              onChange={(e) => setGender(e.target.value)}
              placeholder="예: 남/여"
            />
          </div>
        </div>
      )}

      {/* 보호자 섹션 */}
      <div className="pt-2">
        <div className="text-sm font-semibold text-gray-900 mb-2">보호자</div>
        {!parents || parents.length === 0 ? (
          <div className="text-sm text-gray-800">연결된 보호자 정보가 없습니다.</div>
        ) : (
          <div className="grid grid-cols-1 gap-3">
            {parents.map((p: any, idx: number) => (
              <ParentInlineCard
                key={(p.parentsId ?? p.Parent_ID ?? p.parentId ?? p.username ?? idx).toString()}
                parent={p}
                onSave={onSaveParentInline}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
});

/** 보호자 카드 (아이디 인라인 입력 방식) */
function ParentInlineCard({
  parent,
  onSave,
}: {
  parent: ParentDetail;
  onSave: (parentId: string, draft: { id?: string; name?: string; phone?: string }) => Promise<void> | void;
}) {
  const pid = parent.parentsId ?? parent.Parent_ID ?? parent.parentId ?? parent.username ?? "";
  const pname =
    parent.parentsName ?? parent.Parent_Name ?? parent.parentName ?? parent.Parents_Name ?? parent.name ?? "";
  const pphone =
    parent.parentsPhoneNumber ?? parent.Parent_Phone_Number ?? parent.Parents_Phone_Number ?? parent.phone ?? parent.mobile ?? "";

  const [editing, setEditing] = useState(false);
  const [idVal, setIdVal] = useState<string>(String(pid ?? ""));
  const [nmVal, setNmVal] = useState<string>(String(pname ?? ""));
  const [phVal, setPhVal] = useState<string>(String(pphone ?? ""));
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setIdVal(String(pid ?? ""));
    setNmVal(String(pname ?? ""));
    setPhVal(String(pphone ?? ""));
  }, [pid, pname, pphone]);

  const doSave = async () => {
    setSaving(true);
    try {
      await onSave(String(pid ?? ""), { id: idVal.trim(), name: nmVal.trim(), phone: phVal.trim() });
      setEditing(false);
    } catch (e: any) {
      alert(`보호자 저장 실패: ${e.message ?? e}`);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-xl bg-gray-50 ring-1 ring-gray-300 px-4 py-3">
      {!editing ? (
        <div className="flex flex-col gap-1.5">
          <div className="text-sm font-medium text-gray-900">{nmVal || "—"}</div>
          <div className="text-xs text-gray-700">아이디: {idVal || "—"}</div>
          <div className="text-xs text-gray-700">전화: {phVal || "—"}</div>
          <div className="mt-2">
            <button
              onClick={() => setEditing(true)}
              className="px-2.5 py-1 rounded bg-emerald-600 text-white text-xs hover:bg-emerald-700"
            >
              정보 수정
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-2">
          <div>
            <div className="text-[11px] text-gray-700">아이디</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={idVal}
              onChange={(e) => setIdVal(e.target.value)}
              placeholder="보호자 아이디"
            />
          </div>
          <div>
            <div className="text-[11px] text-gray-700">이름</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={nmVal}
              onChange={(e) => setNmVal(e.target.value)}
              placeholder="보호자 이름"
            />
          </div>
          <div>
            <div className="text-[11px] text-gray-700">전화번호</div>
            <input
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1 text-sm"
              value={phVal}
              onChange={(e) => setPhVal(e.target.value)}
              placeholder="숫자/하이픈"
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => {
                setIdVal(String(pid ?? ""));
                setNmVal(String(pname ?? ""));
                setPhVal(String(pphone ?? ""));
                setEditing(false);
              }}
              disabled={saving}
              className="px-2.5 py-1 rounded border border-gray-400 text-xs bg-white hover:bg-gray-50 disabled:opacity-50"
            >
              취소
            </button>
            <button
              onClick={doSave}
              disabled={saving}
              className="px-2.5 py-1 rounded bg-emerald-600 text-white text-xs hover:bg-emerald-700 disabled:opacity-50"
            >
              {saving ? "저장 중…" : "수정 완료"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
