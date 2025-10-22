// C:\project\103Team-sub\web\greenacademy_web\src\app\page.tsx
"use client";

import React, { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { getRecentQna } from "@/lib/qna"; // 최근 QnA 선택(미확인 우선 → 최신)

import QnaPanel from "./qna/QnaPanel";
import TeacherQnaPanel from "./qna/TeacherQnaPanel";

/** 색상 토큰 */
const colors = {
  green: "#65E478",
  grayBg: "#F2F4F7",
};

/** 타입들 */
type LoginSession = {
  status?: string;
  role: "student" | "teacher" | "parent" | "director";
  username: string;
  name?: string;
  token?: string;
  academyNumbers?: number[];
  parentsNumber?: number | null;
  childStudentId?: string | null;
};

type StudentAttendanceRow = {
  classId: string;
  className: string;
  date: string;
  status: "PRESENT" | "LATE" | "ABSENT" | string;
};

type TeacherClass = {
  classId: string;
  className: string;
  schedule?: string;
};

type TeacherAttendanceRow = {
  classId: string;
  className?: string;
  date: string;
  studentId: string;
  status: "PRESENT" | "LATE" | "ABSENT" | string;
};

type SeatCell = {
  id: number | string;
  name?: string;
  status?: "end" | "label" | "empty" | "filled";
};

/** 유틸 */
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "";
const toYmd = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate()
  ).padStart(2, "0")}`;
const isSameDate = (isoOrYmd: string, base = new Date()) => {
  try {
    if (/^\d{4}-\d{2}-\d{2}$/.test(isoOrYmd)) return isoOrYmd === toYmd(base);
    const d = new Date(isoOrYmd);
    return toYmd(d) === toYmd(base);
  } catch {
    return false;
  }
};

async function apiGet<T>(url: string, token?: string): Promise<T> {
  const res = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    cache: "no-store",
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`${res.status} ${res.statusText} ${text}`.trim());
  }
  return res.json();
}

/** 통계 카드 */
function StatCard({ title, value }: { title: string; value: number }) {
  return (
    <div className="rounded-2xl bg-white shadow-sm ring-1 ring-black/5 px-6 py-4 text-center min-w-[220px]">
      <div className="text-sm text-gray-700 mb-1">{title}</div>
      <div className="text-2xl font-semibold text-gray-900">{value}</div>
    </div>
  );
}

/** 상단 탭 */
function NavTabs({
  active,
  onChange,
}: {
  active: string;
  onChange: (tab: string) => void;
}) {
  const tabs = ["종합정보", "관리", "시간표", "Q&A", "공지사항", "가이드"];
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
        >
          {t}
        </button>
      ))}
    </div>
  );
}

/** 프로필 드롭다운 메뉴 */
function ProfileMenu({ user }: { user: LoginSession | null }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (!ref.current) return;
      if (!ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  const initial =
    user?.name?.[0]?.toUpperCase() ?? user?.username?.[0]?.toUpperCase() ?? "?";

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
          <div className="px-4 py-2 text-xs font-semibold text-gray-900 border-b border-gray-100">
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

/** 사이드 프로필 */
function SidebarProfile({
  user,
  onLogout,
  onOpenRecentQna,
}: {
  user: {
    role?: "student" | "teacher" | "parent" | "director" | string;
    username?: string;
    name?: string;
    academyNumbers?: (number | string)[];
  } | null;
  onLogout: () => void;
  onOpenRecentQna?: () => void;
}) {
  const router = useRouter();

  const roleColor =
    user?.role === "teacher"
      ? "bg-blue-100 text-blue-700 ring-blue-200"
      : user?.role === "student"
      ? "bg-emerald-100 text-emerald-700 ring-emerald-200"
      : user?.role === "parent"
      ? "bg-amber-100 text-amber-700 ring-amber-200"
      : user?.role === "director"
      ? "bg-purple-100 text-purple-700 ring-purple-200"
      : "bg-gray-100 text-gray-700 ring-gray-200";

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
                className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-medium ring-1 ${roleColor}`}
                title={`role: ${user.role}`}
              >
                <span className="inline-block w-2 h-2 rounded-full bg-current opacity-70" />
                {user.role}
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

/** 역할별 리스트 */
function WaitingList({
  title,
  list,
  loading,
  error,
}: {
  title: string;
  list: Array<{ label: string; sub?: string }>;
  loading: boolean;
  error?: string | null;
}) {
  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
      <div className="flex items-center gap-2 mb-3">
        <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">
          {title}
        </span>
      </div>

      <div className="rounded-xl overflow-hidden ring-1 ring-black/5">
        {loading && (
          <div className="px-3 py-2 text-sm text-gray-700">불러오는 중…</div>
        )}
        {error && (
          <div className="px-3 py-2 text-sm text-red-600">오류: {error}</div>
        )}
        {!loading && !error && list.length === 0 && (
          <div className="px-3 py-2 text-sm text-gray-500">표시할 항목이 없습니다.</div>
        )}
        {!loading &&
          !error &&
          list.map((w, i) => (
            <div
              key={i}
              className="px-3 py-2 border-b last:border-none text-sm bg-white"
            >
              <div className="font-medium text-gray-900">{w.label}</div>
              {w.sub && <div className="text-xs text-gray-600">{w.sub}</div>}
            </div>
          ))}
      </div>
    </div>
  );
}

/** 좌석 그리드 */
function SeatGrid({ seats }: { seats: SeatCell[] | null }) {
  if (!seats || seats.length === 0) {
    return (
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
        좌석 데이터가 연결되어 있지 않습니다. (수업 선택 후 좌석 API를 연동해 주세요)
      </div>
    );
  }
  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
      <div className="grid grid-cols-5 gap-3">
        {seats.map((s) => (
          <div
            key={s.id}
            className="h-14 rounded-xl flex items-center justify-center text-sm ring-1 ring-black/5 bg-gray-100 text-gray-900"
            title={s.name}
          >
            {s.name || ""}
          </div>
        ))}
      </div>
      <div className="mt-4 text-right text-xs text-gray-500">* 좌석 배치 (실데이터)</div>
    </div>
  );
}

/** 메인 대시보드 */
export default function GreenAcademyDashboard() {
  const router = useRouter();

  const [user, setUser] = useState<LoginSession | null>(null);
  const [ready, setReady] = useState(false);

  const [activeTab, setActiveTab] = useState<string>("종합정보");

  // QnA 특정 스레드 강제 오픈
  const [forcedQnaId, setForcedQnaId] = useState<string | null>(null);

  // 통계/리스트/좌석
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [present, setPresent] = useState(0);
  const [late, setLate] = useState(0);
  const [absent, setAbsent] = useState(0);
  const [list, setList] = useState<Array<{ label: string; sub?: string }>>([]);
  const [seats, setSeats] = useState<SeatCell[] | null>(null);

  // 학생/학부모 QnA용 학원번호
  const [academyNumber, setAcademyNumber] = useState<number | null>(null);

  /** 세션 로드 & 가드 */
  useEffect(() => {
    const raw = localStorage.getItem("login");
    if (!raw) {
      router.replace("/login");
      return;
    }
    try {
      const parsed = JSON.parse(raw) as LoginSession;
      setUser(parsed);
    } catch {
      localStorage.removeItem("login");
      router.replace("/login");
      return;
    } finally {
      setReady(true);
    }
  }, [router]);

  /** 학원번호 초기화 (학생/학부모 전용) */
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

  /** 역할별 데이터 로딩 (종합정보 탭) */
  useEffect(() => {
    if (!ready || !user) return;
    if (activeTab !== "종합정보") return;

    setLoading(true);
    setErr(null);

    (async () => {
      try {
        setList([]);
        setSeats(null);

        if (user.role === "teacher") {
          const classes = await apiGet<TeacherClass[]>(
            `${API_BASE}/api/teachers/${encodeURIComponent(user.username)}/classes`,
            user.token
          );
          const todayClasses = classes.filter((c) =>
            !c.schedule ? true : isSameDate(c.schedule)
          );

          setList(
            todayClasses.map((c) => ({
              label: c.className,
              sub: c.classId,
            }))
          );

          let all: TeacherAttendanceRow[] = [];
          for (const c of todayClasses) {
            const att = await apiGet<TeacherAttendanceRow[]>(
              `${API_BASE}/api/teachers/classes/${encodeURIComponent(
                c.classId
              )}/attendance`,
              user.token
            );
            const today = att.filter((r) => isSameDate(r.date));
            all = all.concat(today.map((t) => ({ ...t, className: c.className })));
          }

          const sum = summarizeAttendance(all);
          setPresent(sum.present);
          setLate(sum.late);
          setAbsent(sum.absent);
          return;
        }

        // Student/Parent
        const targetStudentId =
          user.role === "parent"
            ? user.childStudentId || user.username
            : user.username;

        const rows = await apiGet<StudentAttendanceRow[]>(
          `${API_BASE}/api/students/${encodeURIComponent(targetStudentId)}/attendance`,
          user.token
        );
        const todayRows = rows.filter((r) => isSameDate(r.date));

        const sum = summarizeAttendance(todayRows);
        setPresent(sum.present);
        setLate(sum.late);
        setAbsent(sum.absent);

        setList(
          todayRows.map((r) => ({
            label: r.className,
            sub: `${r.status} • ${r.date}`,
          }))
        );
      } catch (e: any) {
        setErr(e?.message ?? "데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [ready, user, activeTab]);

  /** 최근 QnA 버튼: 탭 전환 + 미확인 우선 최신 스레드 강제 오픈 */
  const handleOpenRecentQna = async () => {
    try {
      const recent = await getRecentQna();
      if (recent?.questionId) {
        setForcedQnaId(recent.questionId);
        setActiveTab("Q&A");
      } else {
        alert("최근 QnA가 없습니다.");
      }
    } catch {
      alert("최근 QnA 정보를 불러오지 못했습니다.");
    }
  };

  /** 사용자가 Q&A 탭으로 들어갔을 때 자동으로 최근 스레드 열기(미지정 시) */
  useEffect(() => {
    if (activeTab !== "Q&A") return;
    if (forcedQnaId) return;

    let aborted = false;
    (async () => {
      try {
        const recent = await getRecentQna();
        if (aborted) return;
        if (recent?.questionId) setForcedQnaId(recent.questionId);
      } catch {
        /* ignore */
      }
    })();

    return () => {
      aborted = true;
    };
  }, [activeTab, forcedQnaId]);

  const handleLogout = () => {
    localStorage.removeItem("login");
    router.replace("/login");
  };

  if (!ready) return null;

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
              <div className="text-sm text-gray-600 -mt-0.5">Partner</div>
            </div>
          </div>

          <NavTabs active={activeTab} onChange={setActiveTab} />

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

        {/* 탭별 콘텐츠 */}
        {activeTab === "종합정보" && (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">
                  강의실 찾기 추가 예정
                </span>
              </div>
              <div className="flex gap-3">
                <StatCard title="금일 출석 학생 수" value={present} />
                <StatCard title="금일 지각 학생 수" value={late} />
                <StatCard title="금일 미출석 학생 수" value={absent} />
              </div>
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-[300px_1fr] gap-6">
              <WaitingList
                title={user!.role === "teacher" ? "오늘 수업" : "오늘 일정"}
                list={list}
                loading={loading}
                error={err}
              />
              <SeatGrid seats={seats} />
            </div>
          </div>
        )}

        {activeTab === "관리" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">관리</h2>
              <p className="text-sm text-gray-700">
                사용자/수업/좌석 관리 기능을 연결하세요. (예: /api/admin/*)
              </p>
            </div>
          </div>
        )}

        {activeTab === "시간표" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">시간표</h2>
              <p className="text-sm text-gray-700">
                역할별 시간표 API를 연동하세요. (교사: 오늘 수업, 학생/학부모: 수업 목록)
              </p>
            </div>
          </div>
        )}

        {/* Q&A 탭 */}
        {activeTab === "Q&A" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Q&amp;A</h2>

              {/* 역할별 패널 분기 */}
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
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow_sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">가이드</h2>
              <p className="text-sm text-gray-700">
                사용 설명서/튜토리얼 문서를 표시합니다.
              </p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

/** 통계 합산 함수 */
function summarizeAttendance<T extends { status: string }>(rows: T[]) {
  let present = 0,
    late = 0,
    absent = 0;
  rows.forEach((r) => {
    const s = r.status.toUpperCase();
    if (s.includes("LATE")) late += 1;
    else if (s.includes("ABSENT") || s.includes("ABS")) absent += 1;
    else present += 1;
  });
  return { present, late, absent };
}
