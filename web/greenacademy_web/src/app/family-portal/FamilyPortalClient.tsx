"use client";

import ParentProfileCard from "../parent/ParentProfileCard";
import ParentChildDetailCard from "../parent/ParentChildrenDetailCard";
import React, { useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { getRecentQna } from "@/lib/qna";
import { listQuestions } from "@/lib/qna";
import QnaPanel from "../qna/QnaPanel";
import TeacherQnaPanel from "../qna/TeacherQnaPanel";
import ChildAttendancePanel from "../parent/ChildAttendancePanel";
import ChildSchedulePanel from "../parent/ChildSchedulePanel";
import StudentProfileCard from "../student/StudentProfileCard";
import StudentAttendancePanel from "../student/StudentAttendancePanel";
import StudentTimetablePanel from "../student/StudentTimetablePanel";
import { getSession as getServerSession } from "@/app/lib/session";

// ✅ 공지 패널(목록/필터)
import NoticePanel from "../notice/NoticePanel";
// ✅ 공지 상세 패널(직접 띄우기용)
import NoticeDetailPanel from "../notice/NoticeDetailPanel";

/** 색상 토큰 */
const colors = {
  green: "#65E478",
  grayBg: "#F2F4F7",
};

/** 타입 */
type Role = "student" | "parent" | "teacher" | "director";

type LoginSession = {
  role: Role;
  username: string;
  name?: string;
  token?: string;
  childStudentId?: string | null;
  academyNumbers?: number[];
};

type NoticeSession = {
  role: Role;
  username: string;
  token?: string;
  academyNumbers?: number[];
};

type AttendanceRow = {
  classId: string;
  className: string;
  date: string;
  status: "PRESENT" | "LATE" | "ABSENT" | string;
};

type Notice = {
  id: string;
  title: string;
  createdAt: string;
  academyNumbers?: number[]; // 배열 스키마
  academyNumber?: number;    // 단일 스키마(혼재 대비)
};

const notifyKey = (kind: "notice" | "qna", user?: string | null) =>
  `${kind}:lastSeen:${user || "anon"}`;

const maxTime = (...vals: (string | undefined | null)[]) =>
  Math.max(
    ...vals.map((v) => {
      if (!v) return 0;
      const t = new Date(v).getTime();
      return Number.isFinite(t) ? t : 0;
    })
  );

/** 유틸 */
// ❗ 빈 값이면 /backend 로 폴백
const RAW_BASE = (process.env.NEXT_PUBLIC_API_BASE || "").trim();
const API_BASE = RAW_BASE.length > 0 ? RAW_BASE : "/backend";

const toYmd = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate()
  ).padStart(2, "0")}`;

const isSameDate = (s: string, base = new Date()) => {
  try {
    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s === toYmd(base);
    return toYmd(new Date(s)) === toYmd(base);
  } catch {
    return false;
  }
};

async function apiGet<T>(url: string, token?: string): Promise<T> {
  const r = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    cache: "no-store",
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}

/** 숫자 정규화 (공지 학원번호용) */
function normAcadNum(v: any): number | null {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

/** 공지에서 학원번호 배열 추출 (단일/배열 스키마 모두 지원) */
function getNoticeAcademies(n: Notice): number[] {
  const nums = Array.isArray(n.academyNumbers)
    ? n.academyNumbers
    : typeof n.academyNumber === "number"
    ? [n.academyNumber]
    : [];
  return nums
    .map((v) => normAcadNum(v))
    .filter((v): v is number => v !== null);
}

/** 학원번호 최신화: 역할별 프로필 엔드포인트 조회 */
async function fetchLatestAcademies(role: Role, username: string, token?: string): Promise<number[] | null> {
  let path = "";
  if (role === "student") path = `/api/students/${encodeURIComponent(username)}`;
  else if (role === "parent") path = `/api/parents/${encodeURIComponent(username)}`;
  else if (role === "teacher") path = `/api/teachers/${encodeURIComponent(username)}`;
  else path = `/api/directors/${encodeURIComponent(username)}`;

  try {
    const res = await fetch(`${API_BASE}${path}`, {
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      cache: "no-store",
    });
    if (!res.ok) return null;
    const data = await res.json();
    const raw =
      data?.academyNumbers ??
      data?.academyNumber ??
      data?.academies ??
      data?.academy ??
      null;

    if (Array.isArray(raw)) {
      const nums = raw.map((v: any) => normAcadNum(v)).filter((v): v is number => v !== null);
      return nums.length ? nums : null;
    }

    const single = normAcadNum(raw);
    return single != null ? [single] : null;
  } catch {
    return null;
  }
}

/** 역할 문자열 정규화(부분일치) */
function normalizeRole(raw?: unknown): Role {
  const s = String(raw ?? "").toLowerCase();
  if (s.includes("teacher")) return "teacher";
  if (s.includes("director")) return "director";
  if (s.includes("parent")) return "parent";
  return "student";
}

/** KST 기준 YYYY/MM/DD 포맷 */
function formatYmdKST(ts: string) {
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  const y = new Intl.DateTimeFormat("en-US", { timeZone: "Asia/Seoul", year: "numeric" }).format(d);
  const m = new Intl.DateTimeFormat("en-US", { timeZone: "Asia/Seoul", month: "2-digit" }).format(d);
  const day = new Intl.DateTimeFormat("en-US", { timeZone: "Asia/Seoul", day: "2-digit" }).format(d);
  return `${y}/${m}/${day}`;
}

/** 탭 <-> 슬러그 */
const TAB_TO_SLUG: Record<string, string> = {
  "종합정보": "home",
  "마이페이지": "mypage",
  "시간표": "timetable",
  "Q&A": "qna",
  "공지사항": "notices",
};
function SLUG_TO_TAB(slug?: string | null): string {
  switch (slug) {
    case "mypage": return "마이페이지";
    case "timetable": return "시간표";
    case "qna": return "Q&A";
    case "notices": return "공지사항";
    case "home":
    default: return "종합정보";
  }
}

/** 마이페이지 item <-> 슬러그 */
function toSlug(item: string | null, role: Role | null): string | null {
  if (!item) return null;
  if (item === "내 정보") {
    if (role === "student") return "student-info";
    if (role === "parent") return "parent-info";
    return null;
  }
  if (item === "자녀 상세 보기") return "child-detail";
  if (item === "출결관리" || item === "자녀 출결 확인") return "attendance";
  return null;
}
function fromSlug(slug: string | null, role: Role | null): string | null {
  if (!slug) return null;
  if (slug === "student-info") return role === "student" ? "내 정보" : null;
  if (slug === "parent-info") return role === "parent" ? "내 정보" : null;
  if (slug === "child-detail") return role === "parent" ? "자녀 상세 보기" : null;
  if (slug === "attendance")
    return role === "student" ? "출결관리" : role === "parent" ? "자녀 출결 확인" : null;
  return null;
}

/** 공통 UI */
function StatCard({ title, value }: { title: string; value: number }) {
  return (
    <div className="rounded-2xl bg-white shadow-sm ring-1 ring-black/5 px-6 py-4 text-center min-w-[220px]">
      <div className="text-sm text-gray-700 mb-1">{title}</div>
      <div className="text-2xl font-semibold text-gray-900">{value}</div>
    </div>
  );
}

/** 상단 탭 + '마이페이지' 드롭다운 */
function NavTabs({
  active,
  tabs,
  menu,
  onChange,
  onPick,
}: {
  active: string;
  tabs: string[];
  menu?: Record<string, string[]>;
  onChange: (tab: string) => void;
  onPick?: (item: string) => void;
}) {
  const [open, setOpen] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onDocClick = (e: MouseEvent) => {
      if (!rootRef.current) return;
      if (!rootRef.current.contains(e.target as Node)) setOpen(null);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(null);
    };
    document.addEventListener("mousedown", onDocClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocClick);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  return (
    <div ref={rootRef} className="flex gap-3 md:gap-4 relative">
      {tabs.map((t) => {
        const hasMenu = !!menu?.[t]?.length;
        const isOpen = open === t;
        const isActive = active === t;

        return (
          <div key={t} className="relative">
            <button
              onClick={() => {
                if (hasMenu) {
                  setOpen((p) => (p === t ? null : t));
                  if (t === "마이페이지" && active !== "마이페이지") onChange("마이페이지");
                } else {
                  setOpen(null);
                  onChange(t);
                }
              }}
              className={`px-5 py-2 rounded-full font-medium shadow-sm ring-1 ring-black/5 transition ${
                isActive || isOpen
                  ? "bg-[#8CF39B] text-gray-900"
                  : "bg-[#CFF9D6] text-gray-700 hover:bg-[#B7F2C0]"
              }`}
              aria-expanded={hasMenu ? isOpen : undefined}
              aria-haspopup={hasMenu ? "menu" : undefined}
              aria-current={isActive ? "page" : undefined}
            >
              {t}
            </button>

            {hasMenu && isOpen && (
              <div className="absolute left-0 top-full mt-2 w-64 rounded-xl bg-white shadow-lg ring-1 ring-black/5 overflow-hidden z-20 origin-top transition transform duration-150 ease-out">
                <ul className="divide-y divide-gray-100">
                  {menu![t].map((label, idx) => (
                    <li key={idx}>
                      <button
                        className="w-full text-left px-4 py-2.5 text-sm text-gray-900 hover:bg-gray-50"
                        type="button"
                        onClick={() => {
                          setOpen(null);
                          onChange("마이페이지");
                          onPick?.(label);
                        }}
                      >
                        {label}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

/** 프로필 드롭다운 */
function ProfileMenu({
  user,
  hasNotice,
  hasQna,
  hasApproval,
  approvalSummary,
  onGoNotice,
  onGoQna,
  onGoApproval,
}: {
  user: LoginSession | null;
  hasNotice?: boolean;
  hasQna?: boolean;
  hasApproval?: boolean;
  approvalSummary?: string;
  onGoNotice?: () => void;
  onGoQna?: () => void;
  onGoApproval?: () => void;
}) {
  const initial =
    user?.name?.[0]?.toUpperCase() ??
    user?.username?.[0]?.toUpperCase() ??
    "?";

  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (!ref.current) return;
      if (!ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  const alerts: Array<{ label: string; onClick?: () => void }> = [];
  if (hasNotice) alerts.push({ label: "공지 알림이 있습니다.", onClick: onGoNotice });
  if (hasQna) alerts.push({ label: "Q&A 답변/메시지가 있습니다.", onClick: onGoQna });
  if (hasApproval && approvalSummary) alerts.push({ label: approvalSummary, onClick: onGoApproval });
  if (hasApproval && !approvalSummary) alerts.push({ label: "승인 요청이 대기 중입니다.", onClick: onGoApproval });
  if (!alerts.length) alerts.push({ label: "새 알림이 없습니다." });

  return (
    <div className="relative" title={user?.name || user?.username || "프로필"} ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((p) => !p)}
        className="relative w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-sm font-semibold text-gray-900 ring-1 ring-black/5 hover:bg-gray-300 transition"
        aria-label="프로필"
      >
        {initial}
        {(hasNotice || hasQna || hasApproval) && (
          <span className="absolute -top-0.5 -right-0.5 w-3 h-3 rounded-full bg-rose-500 ring-2 ring-white" />
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-60 rounded-2xl bg-white shadow-lg ring-1 ring-black/10 z-30 overflow-hidden">
          <div className="px-4 py-3 border-b border-gray-100">
            <div className="text-sm font-semibold text-gray-900">{user?.name || user?.username || "사용자"}</div>
            <div className="text-xs text-gray-600">알림</div>
          </div>
          <div className="divide-y divide-gray-100">
            {alerts.map((a, i) => (
              <button
                key={i}
                type="button"
                onClick={() => {
                  setOpen(false);
                  a.onClick?.();
                }}
                className={`w-full text-left px-4 py-3 text-sm ${
                  a.onClick ? "hover:bg-gray-50 text-gray-900" : "text-gray-800"
                }`}
              >
                {a.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

/** 사이드바 — 교사/원장 스타일로 통일(학생/학부모도 동일 레이아웃) */
function SidebarProfile({
  user,
  onLogout,
  onOpenRecentQna,
}: {
  user: LoginSession | null;
  onLogout: () => void;
  onOpenRecentQna?: () => void;
}) {
  const router = useRouter();

  const roleColor =
    user?.role === "student"
      ? "bg-emerald-100 text-emerald-700 ring-emerald-200"
      : user?.role === "parent"
      ? "bg-amber-100 text-amber-700 ring-amber-200"
      : user?.role === "teacher"
      ? "bg-indigo-100 text-indigo-700 ring-indigo-200"
      : "bg-purple-100 text-purple-200 ring-purple-200"; // director

  const roleLabel =
    user?.role === "parent"
      ? "학부모"
      : user?.role === "student"
      ? "학생"
      : user?.role === "teacher"
      ? "교사"
      : "원장";

  const academies =
    Array.isArray(user?.academyNumbers) && user!.academyNumbers!.length > 0
      ? user!.academyNumbers!
      : [];

  const handleMyInfoClick = () => {
    const params = new URLSearchParams(window.location.search);
    // 공지 상세 파라미터 제거
    params.delete("noticeId");

    if (user?.role === "student") {
      params.set("tab", "mypage");
      params.set("my", "student-info");
      router.replace(`?${params.toString()}`);
      return;
    }
    if (user?.role === "parent") {
      params.set("tab", "mypage");
      params.set("my", "parent-info");
      router.replace(`?${params.toString()}`);
      return;
    }
    // (교사/원장은 프로필 화면으로)
    router.push("/settings/profile");
  };

  return (
    <aside className="w-[260px] shrink-0">
      <div className="rounded-2xl overflow-hidden ring-1 ring-black/5 shadow-sm bg-white">
        <div className="p-5 bg-gradient-to-br from-[#CFF9D6] via-[#B7F2C0] to-[#8CF39B]">
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <div className="text-xl font-semibold text-gray-900 truncate">
                {user?.name || user?.username || "사용자"}
              </div>
            </div>
            {user?.role && (
              <span
                className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold ring-1 ${roleColor}`}
                title={`role: ${user.role}`}
              >
                <span className="inline-block w-2 h-2 rounded-full bg-current opacity-70" />
                {roleLabel}
              </span>
            )}
          </div>
        </div>

        <div className="p-4 space-y-3">
          <div className="flex items-start justify-between gap-3">
            <div className="text-xs text-gray-700 leading-6">아이디</div>
            <div className="flex-1 text-right">
              <span className="font-semibold text-gray-900">
                {user?.username ?? "—"}
              </span>
            </div>
          </div>

          <div className="flex items-start justify-between gap-3">
            <div className="text-xs text-gray-700 leading-6">학원번호</div>
            <div className="flex-1 text-right">
              {academies.length === 0 ? (
                <span className="text-gray-500">—</span>
              ) : (
                <div className="flex flex-wrap justify-end gap-1.5">
                  {academies.map((n, i) => (
                    <span
                      key={`${n}-${i}`}
                      className="inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium bg-gray-100 text-gray-800 ring-1 ring-gray-200"
                    >
                      #{n}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="h-px bg-gradient-to-r from-transparent via-gray-200 to-transparent my-2" />

          <div>
            <button
              onClick={handleMyInfoClick}
              className="w-full rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-sm text-gray-800"
            >
              내 정보
            </button>
          </div>

          <button
            onClick={onLogout}
            className="w-full rounded-xl py-3 text-white font-semibold mt-1 active:scale-[0.99] transition"
            style={{ backgroundColor: colors.green }}
          >
            로그아웃
          </button>
        </div>
      </div>
    </aside>
  );
}

/** 왼쪽 리스트 (오늘 일정) */
function TodayList({
  list,
  loading,
  error,
}: {
  list: Array<{ label: string; sub?: string; status?: string }>;
  loading: boolean;
  error?: string | null;
}) {
  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
      <div className="flex items-center gap-2 mb-3">
        <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">
          오늘 일정
        </span>
      </div>

      <div className="rounded-xl overflow-hidden ring-1 ring-black/5">
        {loading && <div className="px-3 py-2 text-sm text-gray-700">불러오는 중…</div>}
        {error && <div className="px-3 py-2 text-sm text-red-600">오류: {error}</div>}
        {!loading && !error && list.length === 0 && (
          <div className="px-3 py-2 text-sm text-gray-600">표시할 항목이 없습니다.</div>
        )}
        {!loading &&
          !error &&
          list.map((w, i) => (
            <div
              key={i}
              className="px-3 py-2 border-b last:border-none text-sm bg-white flex items-center justify-between"
            >
              <div>
                <div className="font-medium text-gray-900">{w.label}</div>
                {w.sub && <div className="text-xs text-gray-600">{w.sub}</div>}
              </div>
              {w.status && (
                <span
                  className={`px-2.5 py-1 rounded-full text-[11px] font-semibold ${
                    w.status.includes("ABS")
                      ? "bg-red-100 text-red-700"
                      : w.status.includes("LATE")
                      ? "bg-amber-100 text-amber-700"
                      : "bg-emerald-100 text-emerald-700"
                  }`}
                >
                  {w.status}
                </span>
              )}
            </div>
          ))}
      </div>
    </div>
  );
}

/** 오른쪽 카드 (최근 공지) – 날짜 YYYY/MM/DD, 우하단, 최대 7개, 클릭 시 상세 페이지 이동(동일 화면 내) */
function NoticeCard({ notices, onOpen }: { notices: Notice[]; onOpen: (id: string) => void }) {
  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
      <div className="flex items-center gap-2 mb-3">
        <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">
          최근 공지
        </span>
      </div>
      {notices.length === 0 ? (
        <div className="text-sm text-gray-600">표시할 공지가 없습니다.</div>
      ) : (
        <ul className="divide-y">
          {notices.slice(0, 7).map((n) => (
            <li key={n.id} className="relative py-3">
              <button
                type="button"
                onClick={() => onOpen(n.id)}
                className="w-full text-left px-2 py-2 pr-28 rounded-lg hover:bg-gray-50 transition"
              >
                <div className="font-medium text-gray-900 line-clamp-2">{n.title}</div>
              </button>
              <div className="absolute bottom-2 right-3 text-xs text-gray-600">
                {formatYmdKST(n.createdAt)}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** 메인 페이지 클라이언트 */
export default function FamilyPortalClient() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [user, setUser] = useState<LoginSession | null>(null);
  const [ready, setReady] = useState(false);

  const [activeTab, setActiveTab] = useState("종합정보");
  const [forcedQnaId, setForcedQnaId] = useState<string | null>(null);

  // 마이페이지 선택 상태
  const [myPageItem, setMyPageItem] = useState<string | null>(null);

  // 데이터 상태
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [list, setList] = useState<Array<{ label: string; sub?: string; status?: string }>>([]);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [hasNoticeAlert, setHasNoticeAlert] = useState(false);
  const [hasQnaAlert, setHasQnaAlert] = useState(false);
  const [pendingApproval, setPendingApproval] = useState<number>(0);

  // 통계
  const [present, setPresent] = useState(0);
  const [late, setLate] = useState(0);
  const [absent, setAbsent] = useState(0);

  // 학원번호 상태(학생/학부모의 Q&A만 사용)
  const [academyNumber, setAcademyNumber] = useState<number | null>(null);

  // ✅ 공지 상세용 noticeId (URL 파라미터)
  const noticeIdParam = searchParams.get("noticeId");

  // 🔄 세션 로딩 + login 변경 시 재적용
  useEffect(() => {
    if (typeof window === "undefined") return;

    let aborted = false;

    const applyLogin = async () => {
      // 1) 서버 세션 우선
      let base = getServerSession() as LoginSession | null;

      // 2) localStorage("login") 병합 (동일 사용자만)
      const raw = localStorage.getItem("login");
      if (!raw && !base) {
        router.replace("/login");
        return;
      }

      if (raw) {
        try {
          const parsed: any = JSON.parse(raw);
          const nums = Array.isArray(parsed?.academyNumbers)
            ? parsed.academyNumbers
                .map((n: any) => Number(n))
                .filter((n: number) => Number.isFinite(n))
            : [];
          const stored: LoginSession = {
            role: normalizeRole(parsed?.role),
            username: parsed?.username ?? "",
            name: parsed?.name ?? undefined,
            token: parsed?.token ?? undefined,
            childStudentId: parsed?.childStudentId ?? null,
            academyNumbers: nums,
          };

          // base가 없거나 동일 사용자면 병합
          if (!base || base.username === stored.username) {
            base = {
              ...(base ?? {}),
              ...stored,
              // 서버 세션에 학원번호가 있으면 덮어쓰지 않음
              academyNumbers:
                base?.academyNumbers && base.academyNumbers.length > 0
                  ? base.academyNumbers
                  : stored.academyNumbers,
            } as LoginSession;
          }
        } catch {
          localStorage.removeItem("login");
        }
      }

      if (!base) {
        router.replace("/login");
        return;
      }

      // 3) 최신 학원번호 동기화 (서버 조회) — 삭제/변경 반영
      if (base.role && base.username) {
        const fresh = await fetchLatestAcademies(base.role, base.username, base.token);
        if (aborted) return;
        if (fresh && fresh.length) {
          base = { ...base, academyNumbers: fresh };
          // localStorage도 최신 상태로 업데이트
          try {
            localStorage.setItem(
              "login",
              JSON.stringify({
                role: base.role,
                username: base.username,
                name: base.name,
                token: base.token,
                childStudentId: base.childStudentId ?? null,
                academyNumbers: fresh,
              })
            );
          } catch {
            /* ignore */
          }
        }
      }

      if (aborted) return;
      setUser(base);
      setReady(true);
    };

    // 처음 한 번
    void applyLogin();

    // 다른 탭에서 login이 바뀐 경우
    const onStorage = (e: StorageEvent) => {
      if (e.key === "login") {
        void applyLogin();
      }
    };

    // 같은 탭에서 /settings/profile 등에서 수정 후 다시 돌아왔을 때
    const onFocus = () => {
      void applyLogin();
    };

    window.addEventListener("storage", onStorage);
    window.addEventListener("focus", onFocus);
    return () => {
      window.removeEventListener("storage", onStorage);
      window.removeEventListener("focus", onFocus);
      aborted = true;
    };
  }, [router]);

  // 학원번호 초기값
  useEffect(() => {
    if (!user) return;
    if (
      (user.role === "student" || user.role === "parent") &&
      Array.isArray(user.academyNumbers) &&
      user.academyNumbers.length > 0
    ) {
      setAcademyNumber(user.academyNumbers[0]);
    } else {
      setAcademyNumber(null);
    }
  }, [user]);

  // 탭 진입 시 알림 해제 (공지/QnA)
  useEffect(() => {
    if (activeTab === "공지사항") {
      try { localStorage.setItem(notifyKey("notice", user?.username), new Date().toISOString()); } catch {}
      setHasNoticeAlert(false);
    }
    if (activeTab === "Q&A") {
      try { localStorage.setItem(notifyKey("qna", user?.username), new Date().toISOString()); } catch {}
      setHasQnaAlert(false);
    }
  }, [activeTab, user?.username]);

  // 공지 알림 체크 (역할 공통)
  useEffect(() => {
    if (!user) {
      setHasNoticeAlert(false);
      return;
    }
    const allowed = new Set<number>(
      (user.academyNumbers ?? [])
        .map((n) => normAcadNum(n))
        .filter((n): n is number => n !== null)
    );
    if (!allowed.size) {
      setHasNoticeAlert(false);
      return;
    }
    let aborted = false;
    (async () => {
      try {
        const nsRaw = await apiGet<Notice[]>(
          `${API_BASE}/api/notices?limit=20`,
          user.token
        );
        const filtered = Array.isArray(nsRaw)
          ? nsRaw.filter((n) => {
              const nums = getNoticeAcademies(n);
              if (nums.length === 0) return false;
              return nums.some((x) => allowed.has(x));
            })
          : [];
        const latestTs = filtered.length
          ? Math.max(...filtered.map((n) => maxTime(n.createdAt)))
          : 0;
        const lastSeenTs = (() => {
          try {
            const s = localStorage.getItem(notifyKey("notice", user.username));
            return s ? new Date(s).getTime() : 0;
          } catch {
            return 0;
          }
        })();
        if (!aborted) setHasNoticeAlert(latestTs > lastSeenTs);
      } catch {
        if (!aborted) setHasNoticeAlert(false);
      }
    })();
    return () => { aborted = true; };
  }, [user]);

  // 원장: 승인 요청 대기 건수
  useEffect(() => {
    if (!user || user.role !== "director") {
      setPendingApproval(0);
      return;
    }
    const acad = user.academyNumbers?.[0];
    if (!acad) {
      setPendingApproval(0);
      return;
    }
    let aborted = false;
    (async () => {
      try {
        const rows = await apiGet<any[]>(
          `${API_BASE}/api/academy-requests?scope=director&academyNumber=${encodeURIComponent(acad)}&status=PENDING`,
          user.token
        );
        if (!aborted) setPendingApproval(Array.isArray(rows) ? rows.length : 0);
      } catch {
        if (!aborted) setPendingApproval(0);
      }
    })();
    return () => { aborted = true; };
  }, [user]);
  // QnA 알림 체크
  useEffect(() => {
    (async () => {
      if (!user) return;
      try {
        const qs = await listQuestions();
        const unread = qs.some((q: any) => (q.unreadCount ?? 0) > 0);
        const latestTs = qs.length
          ? Math.max(
              ...qs.map((q: any) =>
                maxTime(
                  q.lastFollowupAt,
                  q.lastParentMsgAt,
                  q.lastStudentMsgAt,
                  q.updatedAt as any,
                  q.createdAt as any
                )
              )
            )
          : 0;
        const lastSeenTs = (() => {
          try {
            const s = localStorage.getItem(notifyKey("qna", user.username));
            return s ? new Date(s).getTime() : 0;
          } catch {
            return 0;
          }
        })();
        setHasQnaAlert(unread || latestTs > lastSeenTs);
      } catch {
        /* ignore */
      }
    })();
  }, [user]);

  // URL 파라미터
  const tabParam = searchParams.get("tab") ?? "home";
  const myParam = searchParams.get("my") ?? "";
  const qnaParam = searchParams.get("qnaId") ?? "";
  const roleKey = user?.role ?? "";

  /** URL → 탭/마이페이지 상태 반영 */
  useEffect(() => {
    const tabName = SLUG_TO_TAB(tabParam);
    if (activeTab !== tabName) setActiveTab(tabName);

    if (qnaParam && qnaParam !== forcedQnaId) setForcedQnaId(qnaParam);

    if (tabParam === "mypage") {
      const mapped = fromSlug(myParam || null, user?.role ?? null);
      if (mapped) {
        setMyPageItem((prev) => (prev === mapped ? prev : mapped));
      } else {
        const def =
          (user?.role === "student" && "출결관리") ||
          (user?.role === "parent" && "자녀 상세 보기") ||
          null;
        setMyPageItem(def);
        const defSlug = toSlug(def, user?.role ?? null);
        const params = new URLSearchParams(window.location.search);
        params.set("tab", "mypage");
        if (defSlug) params.set("my", defSlug);
        else params.delete("my");
        // 공지 상세 파라미터 정리
        params.delete("noticeId");
        const nextQs = `?${params.toString()}`;
        const curQs = window.location.search || "?";
        if (nextQs !== curQs) router.replace(nextQs);
      }
    } else {
      setMyPageItem(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tabParam, myParam, qnaParam, roleKey]);

  // 탭 배열 (불필요한 "가이드" 제거)
  const tabs = useMemo(() => {
    if (user?.role === "student" || user?.role === "parent") {
      return ["종합정보", "마이페이지", "시간표", "Q&A", "공지사항"];
    }
    return ["종합정보", "시간표", "Q&A", "공지사항"];
  }, [user?.role]);

  // 마이페이지 드롭다운 항목
  const menu = useMemo(() => {
    if (user?.role === "student") {
      return { 마이페이지: ["출결관리"] } as Record<string, string[]>;
    }
    if (user?.role === "parent") {
      return {
        마이페이지: ["자녀 상세 보기", "자녀 출결 확인"],
      } as Record<string, string[]>;
    }
    return {} as Record<string, string[]>;
  }, [user?.role]);

  // 종합정보 데이터
  useEffect(() => {
    if (!ready || !user) return;
    if (activeTab !== "종합정보") return;

    (async () => {
      // 교사/원장은 종합정보 위젯 없음
      if (user.role === "teacher" || user.role === "director") {
        setLoading(false);
        setErr(null);
        setList([]);
        setNotices([]);
        setPresent(0);
        setLate(0);
        setAbsent(0);
        return;
      }

      setLoading(true);
      setErr(null);
      try {
        const targetStudentId =
          user.role === "parent" ? user.childStudentId || user.username : user.username;

        // 출결
        const rows = await apiGet<AttendanceRow[]>(
          `${API_BASE}/api/students/${encodeURIComponent(targetStudentId)}/attendance`,
          user.token
        );
        const today = rows.filter((r) => isSameDate(r.date));
        const p = today.filter((r) => r.status.toUpperCase().includes("PRESENT")).length;
        const l = today.filter((r) => r.status.toUpperCase().includes("LATE")).length;
        const a = today.filter((r) => r.status.toUpperCase().includes("ABS")).length;
        setPresent(p);
        setLate(l);
        setAbsent(a);
        setList(
          today.map((r) => ({
            label: r.className,
            sub: r.date,
            status: r.status.toUpperCase(),
          }))
        );

        // ✅ 최근 공지: 로그인한 학생/학부모의 학원번호에 속한 공지만 필터
        try {
          const nsRaw = await apiGet<Notice[]>(
            `${API_BASE}/api/notices?scope=student&limit=7`,
            user.token
          );

          const allowed = new Set<number>(
            (user.academyNumbers ?? [])
              .map((n) => normAcadNum(n))
              .filter((n): n is number => n !== null)
          );

          const filtered = allowed.size
            ? nsRaw.filter((n) => {
                const nums = getNoticeAcademies(n);
                if (nums.length === 0) return false;
                return nums.some((x) => allowed.has(x));
              })
            : [];

          setNotices(filtered.slice(0, 7));
          // 알림(공지) 최신 시각 비교
          const latestTs = filtered.length
            ? Math.max(...filtered.map((n) => maxTime(n.createdAt)))
            : 0;
          const lastSeenTs = (() => {
            try {
              const s = localStorage.getItem(notifyKey("notice", user.username));
              return s ? new Date(s).getTime() : 0;
            } catch {
              return 0;
            }
          })();
          setHasNoticeAlert(latestTs > lastSeenTs);
        } catch {
          setNotices([]);
        }
      } catch (e: any) {
        setErr(e?.message ?? "데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [ready, user, activeTab]);

  const handleLogout = () => {
    localStorage.removeItem("login");
    router.replace("/login");
  };

  // 최근 QnA 바로가기
  const handleOpenRecentQna = async () => {
    try {
      const recent = await getRecentQna();
      if (recent?.questionId) {
        setActiveTab("Q&A");
        setForcedQnaId(recent.questionId);

        const params = new URLSearchParams(window.location.search);
        params.set("tab", "qna");
        params.set("qnaId", recent.questionId);
        // 공지 상세 파라미터 정리
        params.delete("noticeId");
        router.replace(`?${params.toString()}`);
      } else {
        alert("최근 QnA가 없습니다.");
      }
    } catch {
      alert("최근 QnA 정보를 불러오지 못했습니다.");
    }
  };

  // Q&A 탭 진입 시 최근 스레드 오픈
  useEffect(() => {
    if (activeTab !== "Q&A") return;
    if (forcedQnaId) return;

    let aborted = false;
    (async () => {
      try {
        const recent = await getRecentQna();
        if (aborted) return;
        if (recent?.questionId) {
          setForcedQnaId(recent.questionId);

          const params = new URLSearchParams(window.location.search);
          params.set("tab", "qna");
          params.set("qnaId", recent.questionId);
          // 공지 상세 파라미터 정리
          params.delete("noticeId");
          router.replace(`?${params.toString()}`);
        }
      } catch {
        // 무시
      }
    })();

    return () => {
      aborted = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, forcedQnaId]);

  /** ✅ ‘최근 공지’ 클릭 시: 공지사항 탭 + noticeId 쿼리 세팅 → 동일 화면에서 상세 띄움 */
  const openNotice = (id: string) => {
    setActiveTab("공지사항");
    const params = new URLSearchParams(window.location.search);
    params.set("tab", "notices");
    params.set("noticeId", id);
    router.replace(`?${params.toString()}`);
  };

  // 탭 클릭: URL에 항상 tab 슬러그 유지
  const onChangeTab = (tab: string) => {
    setActiveTab(tab);
    const params = new URLSearchParams(window.location.search);
    const slug = TAB_TO_SLUG[tab] ?? "home";
    params.set("tab", slug);

    if (slug === "mypage") {
      const curSlug = toSlug(myPageItem, user?.role ?? null);
      if (curSlug) params.set("my", curSlug);
      else params.delete("my");
      // 공지 상세 파라미터 정리
      params.delete("noticeId");
    } else if (slug !== "notices") {
      // 공지 탭이 아닌 곳으로 이동 시 noticeId 제거
      params.delete("noticeId");
      params.delete("my");
    } else {
      // 공지 탭으로 이동할 때는 my 제거만
      params.delete("my");
      // 공지 탭 진입 시 알림 소거
      try { localStorage.setItem(notifyKey("notice", user?.username), new Date().toISOString()); } catch {}
      setHasNoticeAlert(false);
    }

    if (slug === "qna") {
      try { localStorage.setItem(notifyKey("qna", user?.username), new Date().toISOString()); } catch {}
      setHasQnaAlert(false);
    }
    router.replace(`?${params.toString()}`);
  };

  // 드롭다운에서 항목 선택 시 URL/상태 동기화
  const onPickMyPageItem = (label: string) => {
    setMyPageItem(label);
    const slug = toSlug(label, user?.role ?? null);
    const params = new URLSearchParams(window.location.search);
    params.set("tab", "mypage");
    if (slug) params.set("my", slug);
    else params.delete("my");
    // 공지 상세 파라미터 정리
    params.delete("noticeId");
    router.replace(`?${params.toString()}`);
  };

  if (!ready) return null;

  const subtitle =
    user?.role === "teacher" || user?.role === "director"
      ? "Staff Portal"
      : "Family Portal";

  // ✅ NoticeDetailPanel에 넘길 세션(NoticePanel과 호환되는 모양)
  const noticeSession: NoticeSession | null = user
    ? {
        role: user.role,
        username: user.username,
        token: user.token,
        academyNumbers: user.academyNumbers,
      }
    : null;

  return (
    <div className="min-h-screen" style={{ backgroundColor: colors.grayBg }}>
      {/* 헤더 */}
      <header className="sticky top-0 z-10 bg-white/80 backdrop-blur supports-[backdrop-filter]:bg-white/60 ring-1 ring-black/5">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center ring-1 ring-black/5 overflow-hidden">
              <Image
                src="/logo.png"
                alt="Logo"
                width={40}
                height={40}
                className="object-contain"
                priority
              />
            </div>
            <div className="leading-tight">
              <div className="text-lg font-semibold text-gray-900">Green Academy</div>
              <div className="text-sm text-gray-600 -mt-0.5">{subtitle}</div>
            </div>
          </div>

          <NavTabs
            active={activeTab}
            tabs={tabs}
            menu={menu}
            onChange={onChangeTab}
            onPick={onPickMyPageItem}
          />
          <ProfileMenu
            user={user}
            hasNotice={hasNoticeAlert}
            hasQna={hasQnaAlert}
            hasApproval={pendingApproval > 0}
            approvalSummary={pendingApproval > 0 ? `승인 요청 ${pendingApproval}건 대기` : undefined}
            onGoNotice={() => {
              setActiveTab("공지사항");
              try { localStorage.setItem(notifyKey("notice", user?.username), new Date().toISOString()); } catch {}
              const params = new URLSearchParams(window.location.search);
              params.set("tab", "notices");
              params.delete("noticeId");
              router.replace(`?${params.toString()}`);
            }}
            onGoQna={() => {
              setActiveTab("Q&A");
              try { localStorage.setItem(notifyKey("qna", user?.username), new Date().toISOString()); } catch {}
              const params = new URLSearchParams(window.location.search);
              params.set("tab", "qna");
              params.delete("noticeId");
              router.replace(`?${params.toString()}`);
            }}
            onGoApproval={() => router.push("/director/registration")}
          />
        </div>
      </header>

      {/* 본문 */}
      <main className="max-w-7xl mx-auto px-6 py-6 grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
        <SidebarProfile
          user={user}
          onLogout={handleLogout}
          onOpenRecentQna={handleOpenRecentQna}
        />

        {activeTab === "종합정보" && (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                {/* 상단 배지 등 필요하면 복원 */}
              </div>
              {/* 학생/학부모 통계 카드 필요하면 주석 해제 */}
              {/* {(user?.role === "student" || user?.role === "parent") && (
                <div className="flex gap-3">
                  <StatCard title="금일 출석" value={present} />
                  <StatCard title="금일 지각" value={late} />
                  <StatCard title="금일 결석" value={absent} />
                </div>
              )} */}
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-[300px_1fr] gap-6">
              {user?.role === "student" || user?.role === "parent" ? (
                <>
                  <TodayList list={list} loading={loading} error={err} />
                  {/* ✅ 최근 공지: YYYY/MM/DD, 우하단, 최대 7개, 클릭 시 동일 화면에서 상세 */}
                  <NoticeCard notices={notices} onOpen={openNotice} />
                </>
              ) : (
                <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-600">
                  교사/원장 계정은 ‘종합정보’ 위젯이 없습니다. 상단 탭에서 <b>Q&amp;A</b>를 이용해 주세요.
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === "마이페이지" && (
          <div className="space-y-4">
            {user?.role === "parent" ? (
              <>
                {myPageItem === "내 정보" && <ParentProfileCard />}
                {myPageItem === "자녀 상세 보기" && <ParentChildDetailCard />}
                {myPageItem === "자녀 출결 확인" && <ChildAttendancePanel />}

                {(!myPageItem ||
                  (myPageItem !== "내 정보" &&
                   myPageItem !== "자녀 상세 보기" &&
                   myPageItem !== "자녀 출결 확인")) && (
                  <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                    <h2 className="text-lg font-semibold text-gray-900 mb-2">마이페이지</h2>
                    <p className="text-sm text-gray-700">
                      사이드바의 <b>내 정보</b> 또는 상단 <b>마이페이지</b> 드롭다운에서 항목을 선택하세요.
                    </p>
                  </div>
                )}
              </>
            ) : user?.role === "student" ? (
              <>
                {myPageItem === "내 정보" && <StudentProfileCard />}
                {myPageItem === "출결관리" && <StudentAttendancePanel />}

                {(!myPageItem ||
                  (myPageItem !== "내 정보" &&
                   myPageItem !== "출결관리")) && (
                  <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                    <h2 className="text-lg font-semibold text-gray-900 mb-2">마이페이지</h2>
                    <p className="text-sm text-gray-700">
                      상단의 <b>마이페이지</b> 드롭다운에서 <b>출결관리</b>를 선택하세요. 학생 <b>내 정보</b>는 좌측 사이드바 버튼으로 이동합니다.
                    </p>
                  </div>
                )}
              </>
            ) : (
              <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-2">마이페이지</h2>
                <p className="text-sm text-gray-700">상단의 <b>마이페이지</b> 드롭다운에서 항목을 선택하세요.</p>
              </div>
            )}
          </div>
        )}

        {activeTab === "시간표" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">시간표</h2>
              {user?.role === "student" ? (
                <StudentTimetablePanel/>
              ) : user?.role === "parent" ? (
                <ChildSchedulePanel />
              ) : (
                <p className="text-sm text-gray-700">
                  시간표는 학생/학부모 계정에서 확인할 수 있습니다.
                </p>
              )}
            </div>
          </div>
        )}

        {activeTab === "Q&A" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Q&amp;A</h2>

              {user?.role === "teacher" || user?.role === "director" ? (
                <TeacherQnaPanel questionId={forcedQnaId ?? undefined} />
              ) : academyNumber == null ? (
                <p className="text-sm text-gray-700">
                  학원번호를 확인할 수 없습니다. 프로필 또는 로그인 정보를 확인해 주세요.
                </p>
              ) : (
                <QnaPanel
                  academyNumber={academyNumber}
                  role={user?.role === "parent" ? "parent" : "student"}
                  questionId={forcedQnaId ?? undefined}
                />
              )}
            </div>
          </div>
        )}

        {activeTab === "공지사항" && (
          <div className="space-y-4">
            {/* ✅ noticeId가 있으면 상세 패널, 없으면 목록 패널 */}
            {noticeIdParam ? (
              <NoticeDetailPanel
                noticeId={noticeIdParam}
                session={noticeSession}
                onClose={() => {
                  const params = new URLSearchParams(window.location.search);
                  // 공지 탭 유지 + noticeId 제거
                  params.set("tab", "notices");
                  params.delete("noticeId");
                  router.replace(`?${params.toString()}`);
                }}
                onDeleted={() => {
                  const params = new URLSearchParams(window.location.search);
                  params.set("tab", "notices");
                  params.delete("noticeId");
                  router.replace(`?${params.toString()}`);
                }}
              />
            ) : (
              <NoticePanel />
            )}
          </div>
        )}

      </main>
    </div>
  );
}
