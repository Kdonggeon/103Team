// src/app/page.tsx
"use client";

import React, { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { getSession, clearSession } from "@/app/lib/session";
import api, { type LoginResponse } from "@/app/lib/api";

import DirectorRoomsPanel from "@/components/rooms/director/DirectorRoomsPanel";
import TeacherManagePanel from "@/components/manage/TeacherManagePanel";
import TeacherSchedulePanelInline from "@/components/manage/TeacherSchedulePanelInline";
import DirectorPeoplePanel from "@/components/manage/director/DirectorPeoplePanel";

// ✅ QnA
import { getRecentQna } from "@/lib/qna";
import QnaPanel from "./qna/QnaPanel";
import TeacherQnaPanel from "./qna/TeacherQnaPanel";

// ✅ QR 생성 패널
import QRGeneratorPanel from "@/app/teacher/QRGeneratorPanel";

/** 색상 토큰 */
const colors = { green: "#65E478", grayBg: "#F2F4F7" };

/* ================== API URL 유틸 (rooms.vector.ts와 동일 규칙) ================== */
/**
 * API_BASE: 백엔드가 다른 오리진(예: 9090)일 때 절대 URL 앞부분
 * BACKEND_PREFIX: 프록시를 /backend 로 태울 때만 "/backend", 아니면 ""(빈값)
 *
 * 예시1) 직통(CORS):
 *   NEXT_PUBLIC_API_BASE=http://localhost:9090
 *   NEXT_PUBLIC_BACKEND_PREFIX=
 *
 * 예시2) 프록시(/backend → 9090):
 *   NEXT_PUBLIC_API_BASE=
 *   NEXT_PUBLIC_BACKEND_PREFIX=/backend
 */
const API_BASE =
  (typeof window !== "undefined" && (window as any).__API_BASE__) ||
  process.env.NEXT_PUBLIC_API_BASE ||
  "";
const BACKEND_PREFIX = process.env.NEXT_PUBLIC_BACKEND_PREFIX ?? "/backend";

/** 항상 절대/정규화된 URL을 만든다 */
function absUrl(path: string) {
  const p = path.startsWith("/") ? path : `/${path}`;
  const withPrefix = `${BACKEND_PREFIX}${p}`.replace(/\/{2,}/g, "/");
  return API_BASE ? `${API_BASE}${withPrefix}` : withPrefix;
}

/** GET 전용 fetch 래퍼 (Authorization 자동 주입) */
async function apiGet<T>(path: string): Promise<T> {
  const session = getSession();
  const token = session?.token ?? null;
  const url = absUrl(path);
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
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}${text ? " | " + text : ""}`);
  }
  return text ? (JSON.parse(text) as T) : ({} as T);
}

/** 타입들 */
type LoginSession = LoginResponse | null;

type StudentAttendanceRow = {
  classId: string;
  className: string;
  date: string;
  status: string;
};

type RawClass = {
  classId: string;
  className: string;
  roomNumber?: number | string;
  days?: string[];
  dayOfWeek?: string | string[];
  scheduleText?: string;
  startTime?: string;
  endTime?: string;
};

type SeatCell = {
  id: number | string;
  name?: string;
  seatNumber?: number | string;
  attendance?: string; // PRESENT | LATE | ABSENT ...
};

/** 날짜 유틸 */
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

/** 상단 탭 — ✅ 원장일 때만 '시간표' → '출결확인' */
function NavTabs({
  active,
  onChange,
  role,
}: {
  active: string;
  onChange: (tab: string) => void;
  role?: string | null;
}) {
  const tabs =
    role === "director"
      ? ["종합정보", "관리", "출결확인", "Q&A", "공지사항", "가이드"]
      : ["종합정보", "관리", "시간표", "Q&A", "공지사항", "가이드"];

  return (
    <div className="flex items-center gap-3 md:gap-4">
      {tabs.map((t) => (
        <button
          key={t}
          onClick={() => onChange(t)}
          className={`px-5 py-2 rounded-full font-medium shadow-sm ring-1 ring-black/5 transition ${
            active === t ? "bg-[#8CF39B] text-gray-900" : "bg-[#CFF9D6] text-gray-700 hover:bg-[#B7F2C0]"
          }`}
        >
          {t}
        </button>
      ))}
    </div>
  );
}

/** 프로필 드롭다운 */
function ProfileMenu({ user }: { user: NonNullable<LoginSession> | null }) {
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

  const initial = user?.name?.[0]?.toUpperCase() ?? user?.username?.[0]?.toUpperCase() ?? "?";

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
    name?: string | null;
    academyNumbers?: (number | string)[];
  } | null;
  onLogout: () => void;
  onOpenRecentQna?: () => void;
}) {
  const router = useRouter();
  const role = user?.role;

  const roleColor =
    role === "teacher"
      ? "bg-blue-100 text-blue-700 ring-blue-200"
      : role === "student"
      ? "bg-emerald-100 text-emerald-700 ring-emerald-200"
      : role === "parent"
      ? "bg-amber-100 text-amber-700 ring-amber-200"
      : role === "director"
      ? "bg-purple-100 text-purple-700 ring-purple-200"
      : "bg-gray-100 text-gray-700 ring-gray-200";

  const academies =
    Array.isArray(user?.academyNumbers) && user!.academyNumbers!.length > 0 ? user!.academyNumbers! : [];

  const roleLabel = role === "teacher" ? "선생님" : role ?? "";

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
            {role && (
              <span
                className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-medium ring-1 ${roleColor}`}
                title={`role: ${role}`}
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
              <span className="font-semibold text-gray-900">{user?.username ?? "—"}</span>
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

/** 리스트 카드 */
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
        {loading && <div className="px-3 py-2 text-sm text-gray-700">불러오는 중…</div>}
        {error && <div className="px-3 py-2 text-sm text-red-600">오류: {error}</div>}
        {!loading && !error && list.length === 0 && (
          <div className="px-3 py-2 text-sm text-gray-500">표시할 항목이 없습니다.</div>
        )}
        {!loading &&
          !error &&
          list.map((w, i) => (
            <div key={i} className="px-3 py-2 border-b last:border-none text-sm bg-white">
              <div className="font-medium text-gray-900">{w.label}</div>
              {w.sub && <div className="text-xs text-gray-600">{w.sub}</div>}
            </div>
          ))}
      </div>
    </div>
  );
}

/** 좌석 그리드: 출석 상태 뱃지 포함 */
function SeatGrid({ seats }: { seats: SeatCell[] | null }) {
  if (!seats || seats.length === 0) {
    return (
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
        현재 시간 수업의 좌석·출석 현황
      </div>
    );
  }

  const badge = (att?: string) => {
    if (!att) return null;
    const upper = att.toUpperCase();
    const cls =
      upper.includes("ABS") || upper.includes("ABSENT")
        ? "bg-red-100 text-red-700 ring-red-200"
        : upper.includes("LATE")
        ? "bg-amber-100 text-amber-700 ring-amber-200"
        : "bg-emerald-100 text-emerald-700 ring-emerald-200";
    const label =
      upper.includes("ABS") || upper.includes("ABSENT")
        ? "결석"
        : upper.includes("LATE")
        ? "지각"
        : "출석";
    return <span className={`mt-1 inline-block text-[10px] px-2 py-0.5 rounded ring-1 ${cls}`}>{label}</span>;
  };

  return (
    <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
      <div className="grid grid-cols-5 gap-3">
        {seats.map((s) => (
          <div
            key={s.id}
            className="h-16 rounded-xl flex flex-col items-center justify-center text-sm ring-1 ring-black/5 bg-gray-50 text-gray-900"
            title={s.name}
          >
            <div className="font-medium truncate max-w-[90%]">
              {s.seatNumber ? `${s.seatNumber}. ` : ""}
              {s.name || ""}
            </div>
            {badge(s.attendance)}
          </div>
        ))}
      </div>
      <div className="mt-4 text-right text-xs text-gray-500">* 현재 시간 수업의 좌석·출석 현황</div>
    </div>
  );
}

/** 통계 합산 (학생/학부모 용) */
function summarizeAttendance<T extends { status: string }>(rows: T[]) {
  let present = 0,
    late = 0,
    absent = 0;
  rows.forEach((r) => {
    const s = (r.status || "").toUpperCase();
    if (s.includes("LATE")) late += 1;
    else if (s.includes("ABSENT") || s.includes("ABS")) absent += 1;
    else present += 1;
  });
  return { present, late, absent };
}

/** 메인 대시보드 */
export default function GreenAcademyDashboard() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [user, setUser] = useState<LoginResponse | null>(null);
  const [ready, setReady] = useState(false);

  const [activeTab, setActiveTab] = useState<string>("종합정보");

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [present, setPresent] = useState(0);
  const [late, setLate] = useState(0);
  const [absent, setAbsent] = useState(0);
  const [list, setList] = useState<Array<{ label: string; sub?: string }>>([]);
  const [seats, setSeats] = useState<SeatCell[] | null>(null);

  // 원장 토글 유지(교사는 사용 안 함)
  const [showQr, setShowQr] = useState(false);

  // QnA
  const [forcedQnaId, setForcedQnaId] = useState<string | null>(null);
  const [academyNumber, setAcademyNumber] = useState<number | null>(null);

  /** 세션 로드 & 가드 */
  useEffect(() => {
    const s = getSession();
    if (!s) {
      router.replace("/login");
      return;
    }
    setUser(s);
    setReady(true);
  }, [router]);

  /** ?tab=... 초기 라우팅 */
  useEffect(() => {
    const tab = searchParams.get("tab");
    if (!tab) return;
    const map: Record<string, string> = {
      overview: "종합정보",
      manage: "관리",
      schedule: "시간표",
      attendance: "출결확인",
      qna: "Q&A",
      notice: "공지사항",
      guide: "가이드",
    };
    const label = map[tab.toLowerCase()];
    if (label) setActiveTab(label);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** 학원번호 초기화 (학생/학부모) */
  useEffect(() => {
    if (!user) return;
    if (
      (user.role === "student" || user.role === "parent") &&
      Array.isArray(user.academyNumbers) &&
      user.academyNumbers.length > 0
    ) {
      setAcademyNumber(Number(user.academyNumbers[0]));
    } else {
      setAcademyNumber(null);
    }
  }, [user]);

  /** subtitle 포맷 */
  const formatSubtitle = (c: Partial<RawClass>) => {
    if (c.scheduleText) return c.scheduleText;
    const days = Array.isArray(c.dayOfWeek)
      ? c.dayOfWeek
      : Array.isArray(c.days)
      ? c.days
      : c.dayOfWeek
      ? [c.dayOfWeek]
      : [];
    const dayLabel =
      days.length > 0
        ? days
            .map(
              (d) =>
                (
                  {
                    MON: "월",
                    TUE: "화",
                    WED: "수",
                    THU: "목",
                    FRI: "금",
                    SAT: "토",
                    SUN: "일",
                  } as Record<string, string>
                )[String(d).toUpperCase()] || d
            )
            .join("·")
        : "";
    const timeLabel =
      c.startTime && c.endTime ? `${c.startTime}–${c.endTime}` : c.startTime ? `${c.startTime}~` : "";
    const room = c.roomNumber != null ? ` · #${c.roomNumber}` : "";
    const combo = [dayLabel, timeLabel].filter(Boolean).join(" ");
    return combo ? `${combo}${room}` : room ? String(room).slice(3) : undefined;
  };

  /** 지금 시간 수업인지 대략 판정 */
  const isNowIn = (c: Partial<RawClass>) => {
    if (!c.startTime || !c.endTime) return false;
    const now = new Date();
    const [sh, sm] = c.startTime.split(":").map((n) => parseInt(n, 10));
    const [eh, em] = c.endTime.split(":").map((n) => parseInt(n, 10));
    const start = new Date(now);
    start.setHours(sh || 0, sm || 0, 0, 0);
    const end = new Date(now);
    end.setHours(eh || 0, em || 0, 0, 0);

    const days = Array.isArray(c.dayOfWeek)
      ? c.dayOfWeek
      : Array.isArray(c.days)
      ? c.days
      : c.dayOfWeek
      ? [c.dayOfWeek]
      : [];
    if (days.length > 0) {
      const map = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];
      const today = map[new Date().getDay()];
      if (!days.map((d) => String(d).toUpperCase()).includes(today)) return false;
    }

    return now >= start && now <= end;
  };

  /** 현재 수업 출석(교사용) — attendance만 호출 */
  const fetchCurrentClassSeats = async (classes: RawClass[]) => {
    setSeats(null);
    if (!classes || classes.length === 0) return;

    const current = classes.find((c) => isNowIn(c)) || classes[0];
    const today = toYmd(new Date());

    const atts = await apiGet<Array<{ studentId?: string; status?: string }>>(
      `/api/teachers/classes/${encodeURIComponent(current.classId)}/attendance?date=${encodeURIComponent(today)}`
    );

    const mapped: SeatCell[] = (atts || []).map((a, idx) => ({
      id: a.studentId ?? idx,
      seatNumber: idx + 1, // 좌석번호가 없으니 인덱스로 표시
      name: a.studentId ?? "", // 이름 대신 studentId 노출
      attendance: a.status,
    }));
    setSeats(mapped);
  };

  /** 역할별 데이터 로딩 (종합정보) */
  useEffect(() => {
    if (!ready || !user) return;
    if (activeTab !== "종합정보") return;

    setLoading(true);
    setErr(null);

    (async () => {
      try {
        setList([]);

        if (user.role === "teacher") {
          const classes: RawClass[] = await api.listMyClasses(user.username);
          setList((classes || []).map((c) => ({ label: c.className, sub: formatSubtitle(c) })));
          await fetchCurrentClassSeats(classes || []);
          return;
        }

        if (user.role === "parent") {
          const target = (user as any).childStudentId || user.username;
          const rows = await apiGet<StudentAttendanceRow[]>(`/api/parents/${encodeURIComponent(target)}/attendance`);
          const todayRows = rows.filter((r) => isSameDate(r.date));
          const sum = summarizeAttendance(todayRows);
          setPresent(sum.present);
          setLate(sum.late);
          setAbsent(sum.absent);
          setList(todayRows.map((r) => ({ label: r.className, sub: `${r.status} • ${r.date}` })));
          setSeats(null);
        } else if (user.role === "student") {
          const rows = await apiGet<StudentAttendanceRow[]>(
            `/api/students/${encodeURIComponent(user.username)}/attendance`
          );
          const todayRows = rows.filter((r) => isSameDate(r.date));
          const sum = summarizeAttendance(todayRows);
          setPresent(sum.present);
          setLate(sum.late);
          setAbsent(sum.absent);
          setList(todayRows.map((r) => ({ label: r.className, sub: `${r.status} • ${r.date}` })));
          setSeats(null);
        } else {
          setPresent(0);
          setLate(0);
          setAbsent(0);
          setList([]);
          setSeats(null);
        }
      } catch (e: any) {
        setErr(e?.message ?? "데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [ready, user, activeTab]);

  /** QnA: 최근 스레드 열기 */
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

  /** QnA 탭 자동 로드 */
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
    clearSession();
    router.replace("/login");
  };
  const handleTab = (t: string) => setActiveTab(t);

  if (!ready) return null;

  const showTeacherStats = user?.role !== "teacher"; // 선생이면 통계 숨김

  return (
    <div className="min-h-screen" style={{ backgroundColor: colors.grayBg }}>
      {/* 헤더 */}
      <header className="sticky top-0 z-10 bg-white/80 backdrop-blur supports-[backdrop-filter]:bg-white/60 ring-1 ring-black/5">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center ring-1 ring-black/5 overflow-hidden">
              <Image src="/logo.png" alt="Logo" width={40} height={40} className="object-contain" priority />
            </div>
            <div className="leading-tight">
              <div className="text-lg font-semibold text-gray-900">Green Academy</div>
              <div className="text-sm text-gray-600 -mt-0.5">Partner</div>
            </div>
          </div>

          <NavTabs active={activeTab} onChange={handleTab} role={user?.role} />
          <ProfileMenu user={user} />
        </div>
      </header>

      {/* 본문 */}
      <main className="max-w-7xl mx-auto px-6 py-6 grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
        <SidebarProfile user={user} onLogout={handleLogout} onOpenRecentQna={handleOpenRecentQna} />

        {/* 탭별 콘텐츠 */}
        {activeTab === "종합정보" && (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium" />
              </div>

              {showTeacherStats && (
                <div className="flex gap-3">
                  <StatCard title="금일 출석 학생 수" value={present} />
                  <StatCard title="금일 지각 학생 수" value={late} />
                  <StatCard title="금일 미출석 학생 수" value={absent} />
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-[300px_1fr] gap-6">
              <WaitingList
                title={user!.role === "teacher" ? "내 반 목록" : "오늘 일정"}
                list={list}
                loading={loading}
                error={err}
              />

              {user?.role === "teacher" ? (
                <SeatGrid seats={seats} />
              ) : (
                <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
                  좌석 데이터가 연결되어 있지 않습니다. (수업 선택 후 좌석 API를 연동해 주세요)
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === "관리" && (
  // ✅ Fragment 대신 단일 div로 감싸서 "그리드 두 번째 칸"에 통째로 들어가게 함
  <div className="space-y-4">
    {/* 교사: QR + 패널 */}
    {user?.role === "teacher" && (
      <div className="space-y-4">
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">QR 생성</h2>
          <QRGeneratorPanel user={user} />
        </div>
        <TeacherManagePanel user={user} />
      </div>
    )}

    {/* 원장: ✅ QR → 강의실 관리 순서 */}
    {user?.role === "director" && (
      <div className="space-y-4">
        {/* 1) QR 토글/패널 */}
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
          <button
            onClick={() => setShowQr((v) => !v)}
            className="px-3 py-1.5 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700"
          >
            {showQr ? "QR 생성 닫기" : "QR 생성 열기"}
          </button>
        </div>
        {showQr && (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4">
            <QRGeneratorPanel user={user} />
          </div>
        )}

        {/* 2) 강의실 관리 */}
        <DirectorRoomsPanel user={user} />
      </div>
    )}

    {(user?.role === "student" || user?.role === "parent") && (
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">관리</h2>
        <p className="text-sm text-gray-700">이 역할에는 관리 메뉴가 없습니다.</p>
      </div>
    )}
  </div>
)}


        {/* 원장 전용: 출결확인 */}
        {user?.role === "director" && activeTab === "출결확인" && <DirectorPeoplePanel />}

        {/* 그 외 역할: 시간표 */}
        {activeTab === "시간표" && user?.role !== "director" && (
          <>
            {user?.role === "teacher" ? (
              <TeacherSchedulePanelInline user={user} />
            ) : (
              <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-2">시간표</h2>
                <p className="text-sm text-gray-700">현재 역할에는 시간표 기능이 준비 중입니다.</p>
              </div>
            )}
          </>
        )}

        {/* QnA */}
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

        {/* 공지/가이드 */}
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
              <p className="text-sm text-gray-700">사용 설명서/튜토리얼 문서를 표시합니다.</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
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
