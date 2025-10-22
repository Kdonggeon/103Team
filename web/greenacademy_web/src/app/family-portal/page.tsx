"use client";

import React, { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { getRecentQna } from "@/lib/qna";

import QnaPanel from "../qna/QnaPanel";
import TeacherQnaPanel from "../qna/TeacherQnaPanel";

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

type AttendanceRow = {
  classId: string;
  className: string;
  date: string; // ISO or YYYY-MM-DD
  status: "PRESENT" | "LATE" | "ABSENT" | string;
};

type Notice = { id: string; title: string; createdAt: string };

/** 유틸 */
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "";
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

/** 공통 UI */
function StatCard({ title, value }: { title: string; value: number }) {
  return (
    <div className="rounded-2xl bg-white shadow-sm ring-1 ring-black/5 px-6 py-4 text-center min-w-[220px]">
      <div className="text-sm text-gray-700 mb-1">{title}</div>
      <div className="text-2xl font-semibold text-gray-900">{value}</div>
    </div>
  );
}

function NavTabs({
  active,
  onChange,
}: {
  active: string;
  onChange: (tab: string) => void;
}) {
  const tabs = ["종합정보", "시간표", "Q&A", "공지사항", "가이드"];
  return (
    <div className="flex gap-3 md:gap-4">
      {tabs.map((t) => (
        <button
          key={t}
          onClick={() => onChange(t)}
          className={`px-5 py-2 rounded-full font-medium shadow-sm ring-1 ring-black/5 transition ${
            active === t
              ? "bg-[#8CF39B] text-gray-900"
              : "bg-[#CFF9D6] text-gray-700 hover:bg-[#B7F2C0]"
          }`}
          aria-current={active === t ? "page" : undefined}
        >
          {t}
        </button>
      ))}
    </div>
  );
}

/** 역할 문자열 정규화(부분일치) */
function normalizeRole(raw?: unknown): Role {
  const s = String(raw ?? "").toLowerCase();
  if (s.includes("teacher")) return "teacher";
  if (s.includes("director")) return "director";
  if (s.includes("parent")) return "parent";
  return "student";
}

/** 프로필 드롭다운 */
function ProfileMenu({ user }: { user: LoginSession | null }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (!ref.current) return;
      if (!ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  const initial =
    user?.name?.[0]?.toUpperCase() ??
    user?.username?.[0]?.toUpperCase() ??
    "?";

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((p) => !p)}
        className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-sm font-semibold text-gray-900 hover:bg-gray-300 transition"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="프로필 메뉴 열기"
      >
        {initial}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-52 rounded-xl bg-white shadow-lg ring-1 ring-black/5 overflow-hidden z-20">
          <div className="px-4 py-2 text-xs font-medium text-gray-900 border-b border-gray-100">
            {user?.name || user?.username}
          </div>
          <button
            onClick={() => {
              setOpen(false);
              router.push("/notifications");
            }}
            className="w-full text-left px-4 py-2 text-sm text-gray-900 hover:bg-gray-50"
          >
            🔔 내 알림
          </button>
          <button
            onClick={() => {
              setOpen(false);
              router.push("/settings/theme");
            }}
            className="w-full text-left px-4 py-2 text-sm text-gray-900 hover:bg-gray-50"
          >
            🎨 테마 설정
          </button>
          <button
            onClick={() => {
              setOpen(false);
              router.push("/settings");
            }}
            className="w-full text-left px-4 py-2 text-sm text-gray-900 hover:bg-gray-50"
          >
            ⚙️ 환경 설정
          </button>
        </div>
      )}
    </div>
  );
}

/** 사이드바 */
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
      : "bg-purple-100 text-purple-700 ring-purple-200"; // director

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

          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => router.push("/settings/profile")}
              className="rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-xs font-medium text-gray-800"
            >
              개인정보 수정
            </button>
            <button
              onClick={() => router.push("/account/delete")}
              className="rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-xs font-medium text-gray-800"
            >
              계정탈퇴
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

      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4 space-y-3 mt-4">
        <div className="text-sm font-semibold text-gray-900">빠른 실행</div>
        <div className="grid gap-2">
          <button
            onClick={() => router.push("/settings")}
            className="w-full rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-sm text-gray-800"
          >
            환경 설정
          </button>
          <button
            onClick={onOpenRecentQna}
            className="w-full rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-sm text-gray-800"
          >
            최근 QnA 바로가기
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

/** 오른쪽 카드 (최근 공지) */
function NoticeCard({ notices }: { notices: Notice[] }) {
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
          {notices.map((n) => (
            <li key={n.id} className="py-3">
              <div className="font-medium text-gray-900">{n.title}</div>
              <div className="text-xs text-gray-600">{n.createdAt}</div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** 메인 페이지 */
export default function FamilyPortalPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [user, setUser] = useState<LoginSession | null>(null);
  const [ready, setReady] = useState(false);

  const [activeTab, setActiveTab] = useState("종합정보");
  const [forcedQnaId, setForcedQnaId] = useState<string | null>(null);

  // 데이터 상태
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [list, setList] = useState<Array<{ label: string; sub?: string; status?: string }>>([]);
  const [notices, setNotices] = useState<Notice[]>([]);

  // 통계
  const [present, setPresent] = useState(0);
  const [late, setLate] = useState(0);
  const [absent, setAbsent] = useState(0);

  // 학원번호 상태(학생/학부모의 Q&A 패널에만 사용)
  const [academyNumber, setAcademyNumber] = useState<number | null>(null);

  // 세션 로딩 (+ role/academyNumbers 정규화)
  useEffect(() => {
    const raw = localStorage.getItem("login");
    if (!raw) {
      router.replace("/login");
      return;
    }
    try {
      const parsed: any = JSON.parse(raw);
      const nums =
        Array.isArray(parsed?.academyNumbers)
          ? parsed.academyNumbers
              .map((n: any) => Number(n))
              .filter((n: number) => Number.isFinite(n))
          : [];
      const normalized: LoginSession = {
        role: normalizeRole(parsed?.role),
        username: parsed?.username ?? "",
        name: parsed?.name ?? undefined,
        token: parsed?.token ?? undefined,
        childStudentId: parsed?.childStudentId ?? null,
        academyNumbers: nums,
      };
      setUser(normalized);
    } catch {
      localStorage.removeItem("login");
      router.replace("/login");
      return;
    } finally {
      setReady(true);
    }
  }, [router]);

  // 학원번호 선택(학생/학부모만)
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

  // URL 쿼리로 탭 전환 + 특정 QnA 열기
  useEffect(() => {
    const tab = searchParams.get("tab");
    const qid = searchParams.get("qnaId");
    if (tab === "qna") setActiveTab("Q&A");
    if (qid) setForcedQnaId(qid);
  }, [searchParams]);

  // 종합정보 탭 데이터(학생/학부모만 의미 있음)
  useEffect(() => {
    if (!ready || !user) return;
    if (activeTab !== "종합정보") return;

    (async () => {
      // 교사/원장: 공란(안내만)
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

        try {
          const ns = await apiGet<Notice[]>(
            `${API_BASE}/api/notices?scope=student&limit=5`,
            user.token
          );
          setNotices(ns);
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

  // 최근 QnA 바로가기 → 탭 전환 + 해당 스레드 강제 오픈(미확인 우선)
  const handleOpenRecentQna = async () => {
    try {
      const recent = await getRecentQna();
      if (recent?.questionId) {
        setActiveTab("Q&A");
        setForcedQnaId(recent.questionId);

        // URL 동기화(얕은 replace)
        const params = new URLSearchParams(searchParams.toString());
        params.set("tab", "qna");
        params.set("qnaId", recent.questionId);
        router.replace(`?${params.toString()}`);
      } else {
        alert("최근 QnA가 없습니다.");
      }
    } catch {
      alert("최근 QnA 정보를 불러오지 못했습니다.");
    }
  };

  // Q&A 탭 수동 진입 시에도 자동으로 “최근 스레드” 열기(강제 id 없을 때만)
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

          const params = new URLSearchParams(searchParams.toString());
          params.set("tab", "qna");
          params.set("qnaId", recent.questionId);
          router.replace(`?${params.toString()}`);
        }
      } catch {
        // 무시
      }
    })();

    return () => {
      aborted = true;
    };
  }, [activeTab, forcedQnaId, searchParams, router]);

  // 탭 클릭 시: Q&A로 전환하는 경우 최근 스레드 자동 로드 트리거(중복 방지)
  const onChangeTab = async (tab: string) => {
    setActiveTab(tab);
    if (tab !== "Q&A") return;

    if (!forcedQnaId) {
      try {
        const recent = await getRecentQna();
        if (recent?.questionId) {
          setForcedQnaId(recent.questionId);
          const params = new URLSearchParams(searchParams.toString());
          params.set("tab", "qna");
          params.set("qnaId", recent.questionId);
          router.replace(`?${params.toString()}`);
        }
      } catch {
        // 무시
      }
    }
  };

  if (!ready) return null;

  const subtitle =
    user?.role === "teacher" || user?.role === "director"
      ? "Staff Portal"
      : "Family Portal";

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

          <NavTabs active={activeTab} onChange={onChangeTab} />

          <ProfileMenu user={user} />
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
                <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">
                  오늘
                </span>
              </div>
              {(user?.role === "student" || user?.role === "parent") && (
                <div className="flex gap-3">
                  <StatCard title="금일 출석" value={present} />
                  <StatCard title="금일 지각" value={late} />
                  <StatCard title="금일 결석" value={absent} />
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-[300px_1fr] gap-6">
              {user?.role === "student" || user?.role === "parent" ? (
                <>
                  <TodayList list={list} loading={loading} error={err} />
                  <NoticeCard notices={notices} />
                </>
              ) : (
                <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-600">
                  교사/원장 계정은 ‘종합정보’ 위젯이 없습니다. 상단 탭에서 <b>Q&amp;A</b>를 이용해 주세요.
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === "시간표" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">시간표</h2>
              <p className="text-sm text-gray-700">
                시간표 API를 연동해 오늘/주간 수업을 보여 주세요.
              </p>
            </div>
          </div>
        )}

        {activeTab === "Q&A" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Q&amp;A</h2>

              {/* 역할별 패널 분기 + 최근 질문 강제 오픈 */}
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
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">공지사항</h2>
              <p className="text-sm text-gray-700">공지 API 또는 CMS를 연결하세요.</p>
            </div>
          </div>
        )}

        {activeTab === "가이드" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">가이드</h2>
              <p className="text-sm text-gray-700">사용 설명서/튜토리얼 영역입니다.</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
