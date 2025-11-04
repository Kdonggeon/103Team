"use client";

import React from "react";
import { getSession } from "@/app/lib/session";

/** 공통 타입 */
type PersonRow = {
  id: string;
  name: string;
  phone?: string;
  role: "student" | "parent" | "teacher" | "unknown";
  academyNumbers: number[];
};

/** /backend 프록시 + Authorization 자동 주입(GET 전용 간단 래퍼) */
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

/** 백엔드 엔드포인트 경로 (프로젝트에 맞게 필요 시 조정) */
const ENDPOINTS = {
  me: "/api/directors/me",
  // 아래 두 개는 백엔드에 맞춰 라우팅만 맞춰주면 됨.
  // 예: /api/directors/search/people?numbers=103,105&q=홍길
  //     /api/directors/search/teachers?numbers=103,105&q=김
  searchPeople: "/api/directors/search/people",
  searchTeachers: "/api/directors/search/teachers",
};

type DirectorMe = {
  username: string;
  name: string;
  academyNumbers: number[];
};

type RawAny = Record<string, any>;

function toArrayNumber(v: unknown): number[] {
  if (Array.isArray(v)) return v.map((x) => Number(x)).filter((n) => !Number.isNaN(n));
  if (v == null) return [];
  const n = Number(v);
  return Number.isNaN(n) ? [] : [n];
}

/** 다양한 스키마를 넓게 허용해 공통 Row로 매핑 */
function mapToPersonRow(x: RawAny, fallbackRole: PersonRow["role"]): PersonRow {
  const academyNumbers =
    toArrayNumber(x?.academyNumbers) ||
    toArrayNumber(x?.Academy_Numbers) ||
    toArrayNumber(x?.academyNumber);

  // id 후보
  const id =
    x?.username ??
    x?.userId ??
    x?.studentId ??
    x?.Student_ID ??
    x?.parentId ??
    x?.Parent_ID ??
    x?.teacherId ??
    x?.Teacher_ID ??
    "";

  // name 후보
  const name =
    x?.name ??
    x?.studentName ??
    x?.Student_Name ??
    x?.parentName ??
    x?.Parent_Name ??
    x?.teacherName ??
    x?.Teacher_Name ??
    x?.displayName ??
    "";

  // phone 후보 (항상 검은색으로 표시)
  const phone =
    x?.phone ??
    x?.phoneNumber ??
    x?.Phone_Number ??
    x?.Parents_Phone_Number ??
    x?.Parents_Number ??
    x?.Teacher_Phone_Number ??
    x?.Student_Phone_Number ??
    undefined;

  // role 추론
  let role: PersonRow["role"] = fallbackRole;
  const r = String(x?.role ?? "").toLowerCase();
  if (r.includes("student")) role = "student";
  else if (r.includes("parent")) role = "parent";
  else if (r.includes("teacher")) role = "teacher";
  else if (fallbackRole === "teacher") role = "teacher";

  return {
    id: String(id ?? ""),
    name: String(name ?? ""),
    phone: phone ? String(phone) : undefined,
    role,
    academyNumbers: academyNumbers,
  };
}

/** chips */
function Chips({ items }: { items: Array<string | number> }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {items.map((n, i) => (
        <span
          key={`${n}-${i}`}
          className="inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium bg-gray-100 text-gray-800 ring-1 ring-gray-200"
        >
          #{n}
        </span>
      ))}
    </div>
  );
}

export default function DirectorPeoplePanel() {
  const [me, setMe] = React.useState<DirectorMe | null>(null);
  const [mode, setMode] = React.useState<"people" | "teachers">("people"); // 버튼 전환
  const [q, setQ] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  const [err, setErr] = React.useState<string | null>(null);
  const [rows, setRows] = React.useState<PersonRow[]>([]);

  /** 내 소속 학원 */
  React.useEffect(() => {
    (async () => {
      try {
        setErr(null);
        const mine = await apiGet<DirectorMe>(ENDPOINTS.me);
        setMe(mine);
      } catch (e: any) {
        setErr(e?.message || "원장 정보를 불러오지 못했습니다.");
      }
    })();
  }, []);

  /** 검색 */
  const doSearch = React.useCallback(async () => {
    if (!me?.academyNumbers?.length) {
      setRows([]);
      return;
    }
    setLoading(true);
    setErr(null);
    try {
      const numbers = encodeURIComponent(me.academyNumbers.join(","));
      const query = encodeURIComponent(q.trim());
      const path =
        mode === "teachers"
          ? `${ENDPOINTS.searchTeachers}?numbers=${numbers}&q=${query}`
          : `${ENDPOINTS.searchPeople}?numbers=${numbers}&q=${query}`;

      const list = await apiGet<RawAny[]>(path);
      const mapped = (Array.isArray(list) ? list : []).map((x) =>
        mapToPersonRow(x, mode === "teachers" ? "teacher" : "unknown")
      );

      // 안전장치: 서버가 학원 필터를 안 걸었을 경우, 프런트에서 한 번 더 필터
      const setA = new Set(me.academyNumbers);
      const filtered = mapped.filter((r) => r.academyNumbers.some((n) => setA.has(Number(n))));

      setRows(filtered);
    } catch (e: any) {
      setErr(e?.message || "검색에 실패했습니다. (백엔드 엔드포인트를 확인하세요)");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [me, mode, q]);

  /** Enter 검색 */
  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") doSearch();
  };

  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">출결확인 / 소속 인원 검색</h2>
          <p className="text-sm text-gray-600 mt-1">
            원장 소속 학원 전체에서 <span className="font-medium text-gray-900">학생·학부모</span> 또는{" "}
            <span className="font-medium text-gray-900">선생님</span>을 전환해 검색합니다.
          </p>
        </div>

        {/* 모드 토글 (검은색 버튼) */}
        <div className="flex gap-2">
          <button
            onClick={() => setMode("people")}
            className={`px-4 py-2 rounded-lg ring-1 transition text-white ${
              mode === "people" ? "bg-black ring-black" : "bg-black/70 hover:bg-black"
            }`}
            title="학생/학부모 검색"
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

      {/* 소속 학원 표시 */}
      <div className="rounded-xl ring-1 ring-gray-200 p-3 bg-gray-50">
        <div className="text-xs text-gray-500 mb-1">검색 범위(원장 소속 학원)</div>
        {me?.academyNumbers?.length ? (
          <Chips items={me.academyNumbers} />
        ) : (
          <div className="text-sm text-gray-600">소속 학원 정보가 없습니다.</div>
        )}
      </div>

      {/* 검색 바 */}
      <div className="flex flex-col sm:flex-row gap-2">
        <input
          className="flex-1 rounded-lg border px-3 py-2 outline-none text-sm"
          placeholder={mode === "teachers" ? "예) 김, 010, T001…" : "예) 홍, 010, S001/학부모 이름…"}
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={onKeyDown}
        />
        <button
          onClick={doSearch}
          className="px-5 py-2 rounded-lg text-white bg-black hover:bg-black/90 active:scale-[0.99] transition"
        >
          검색
        </button>
      </div>

      {/* 상태 */}
      {err && <div className="text-sm text-red-600 bg-red-50 border border-red-200 p-3 rounded-lg">{err}</div>}
      {loading && <div className="text-sm text-gray-700">검색 중…</div>}

      {/* 결과 테이블 */}
      {!loading && !err && (
        <div className="rounded-xl overflow-hidden ring-1 ring-gray-200">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr className="text-left text-gray-600">
                <th className="px-3 py-2 font-medium">이름</th>
                <th className="px-3 py-2 font-medium">아이디</th>
                <th className="px-3 py-2 font-medium">연락처</th>
                <th className="px-3 py-2 font-medium">소속학원</th>
                <th className="px-3 py-2 font-medium">역할</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-3 py-4 text-center text-gray-500">
                    결과가 없습니다. 검색어를 입력해 보세요.
                  </td>
                </tr>
              ) : (
                rows.map((r, idx) => (
                  <tr key={`${r.id}-${idx}`} className="border-t last:border-b-0">
                    <td className="px-3 py-2 text-gray-900">{r.name || "—"}</td>
                    <td className="px-3 py-2 text-gray-900">{r.id || "—"}</td>
                    {/* 전화번호는 항상 검은색 */}
                    <td className="px-3 py-2 text-black">{r.phone ?? "—"}</td>
                    <td className="px-3 py-2">
                      {r.academyNumbers?.length ? <Chips items={r.academyNumbers} /> : <span className="text-gray-500">—</span>}
                    </td>
                    <td className="px-3 py-2 text-gray-900">
                      {r.role === "teacher" ? "선생님" : r.role === "student" ? "학생" : r.role === "parent" ? "학부모" : "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
