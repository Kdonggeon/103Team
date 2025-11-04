// C:\project\103Team-sub\web\greenacademy_web\src\app\page.tsx
"use client";

import React, { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";

// 세션/API
import { getSession, clearSession } from "@/app/lib/session";
import api, { type LoginResponse } from "@/app/lib/api";

// QnA
import { getRecentQna } from "@/lib/qna";
import QnaPanel from "./qna/QnaPanel";
import TeacherQnaPanel from "./qna/TeacherQnaPanel";

// 관리 패널(외부 컴포넌트 사용)
import TeacherProfileCard from "@/app/teacher/TeacherProfileCard";
import TeacherStudentManage from "@/app/teacher/StudentManage";
import QRGeneratorPanel from "@/app/teacher/QRGeneratorPanel";
import DirectorRoomsPanel from "@/components/rooms/director/DirectorRoomsPanel";
import TeacherManagePanel from "@/components/manage/TeacherManagePanel";
import TeacherSchedulePanelInline from "@/components/manage/TeacherSchedulePanelInline";
import DirectorMyInfoCard from "@/app/director/DirectorMyInfoCard";

/** 색상 토큰 */
const colors = { green: "#65E478", grayBg: "#F2F4F7" };

/** 타입 */
type LoginSession = LoginResponse | null;

type StudentAttendanceRow = {
  classId: string;
  className: string;
  date: string;
  status: string; // PRESENT | LATE | ABSENT | ...
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
  attendance?: string; // PRESENT | LATE | ABSENT | ...
};

/** 날짜 유틸 */
const toYmd = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

const isSameDate = (isoOrYmd: string, base = new Date()) => {
  try {
    if (/^\d{4}-\d{2}-\d{2}$/.test(isoOrYmd)) return isoOrYmd === toYmd(base);
    const d = new Date(isoOrYmd);
    return toYmd(d) === toYmd(base);
  } catch {
    return false;
  }
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

/** 통계 카드 */
function StatCard({ title, value }: { title: string; value: number }) {
  return (
    <div className="rounded-2xl bg-white shadow-sm ring-1 ring-black/5 px-6 py-4 text-center min-w-[220px]">
      <div className="text-sm text-gray-700 mb-1">{title}</div>
      <div className="text-2xl font-semibold text-gray-900">{value}</div>
    </div>
  );
}

/** 상단 탭(통합) + 관리 드롭다운 */
function NavTabs({
  active,
  onChange,
  role,
  manageMenu,
  onSelectManage,
}: {
  active: string;
  onChange: (tab: string) => void;
  role?: string | null;
  manageMenu?: string | null;
  onSelectManage?: (item: "내정보" | "학생관리" | "반 관리" | "학원관리" | "QR 생성") => void;
}) {
  const left = ["종합정보"] as const;
  const right =
    role === "director"
      ? (["관리", "시간표", "출결확인", "Q&A", "공지사항", "가이드"] as const)
      : (["관리", "시간표", "Q&A", "공지사항", "가이드"] as const);

  const manageItems =
    role === "director"
      ? (["내정보", "학생관리", "반 관리", "학원관리", "QR 생성"] as const)
      : role === "teacher"
      ? (["내정보", "학생관리", "반 관리", "QR 생성"] as const)
      : (["내정보", "학생관리", "반 관리"] as const);

  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    const click = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const esc = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", click);
    document.addEventListener("keydown", esc);
    return () => {
      document.removeEventListener("mousedown", click);
      document.removeEventListener("keydown", esc);
    };
  }, []);

  const TabBtn = (t: string) => (
    <button
      key={t}
      onClick={() => onChange(t)}
      className={`px-5 py-2 rounded-full font-medium shadow-sm ring-1 ring-black/5 transition ${
        active === t ? "bg-[#8CF39B] text-gray-900" : "bg-[#CFF9D6] text-gray-700 hover:bg-[#B7F2C0]"
      }`}
    >
      {t}
    </button>
  );

  return (
    <div className="flex items-center gap-3 md:gap-4">
      {left.map(TabBtn)}

      {/* 관리 탭 + 드롭다운 */}
      {right.includes("관리" as any) && (
        <div className="relative" ref={ref}>
          <button
            onClick={() => {
              onChange("관리");
              setOpen((p) => !p);
            }}
            className={`px-5 py-2 rounded-full font-medium shadow-sm ring-1 ring-black/5 transition ${
              active === "관리" ? "bg-[#8CF39B] text-gray-900" : "bg-[#CFF9D6] text-gray-700 hover:bg-[#B7F2C0]"
            }`}
            aria-haspopup="menu"
            aria-expanded={open}
          >
            관리
          </button>
          {open && onSelectManage && (
            <div className="absolute left-0 mt-2 w-48 rounded-xl bg-white shadow-lg ring-1 ring-black/5 overflow-hidden z-20">
              {manageItems.map((item) => (
                <button
                  key={item}
                  onClick={() => {
                    onSelectManage(item);
                    setOpen(false);
                  }}
                  className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 ${
                    manageMenu === item ? "text-gray-900 font-semibold" : "text-gray-800"
                  }`}
                >
                  {item}
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 나머지 우측 탭 */}
      {right.filter((t) => t !== "관리").map(TabBtn)}
    </div>
  );
}

/** 프로필 드롭다운 메뉴 */
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

  const roleLabel =
    role === "teacher" ? "선생님" : role === "director" ? "원장" : role === "parent" ? "학부모" : role === "student" ? "학생" : role ?? "";

  const academies =
    Array.isArray(user?.academyNumbers) && user!.academyNumbers!.length > 0 ? user!.academyNumbers! : [];

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

          <div className="flex flex-col gap-1">
            <button
              onClick={onOpenRecentQna}
              className="w-full rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-sm text-gray-800"
            >
              최근 QnA 바로가기
            </button>
          </div>
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
        <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">{title}</span>
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

/** 좌석 그리드(출석 뱃지 포함) — 교사용 */
function SeatGrid({ seats }: { seats: SeatCell[] | null }) {
  if (!seats || seats.length === 0) {
    return (
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
        현재 시간 수업의 좌석/출석 데이터가 없습니다.
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
      upper.includes("ABS") || upper.includes("ABSENT") ? "결석" : upper.includes("LATE") ? "지각" : "출석";
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

/** 유틸: 요일/시간 포맷 → 부제(subtitle) */
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
  const timeLabel = c.startTime && c.endTime ? `${c.startTime}–${c.endTime}` : c.startTime ? `${c.startTime}~` : "";
  const room = c.roomNumber != null ? ` · #${c.roomNumber}` : "";
  const combo = [dayLabel, timeLabel].filter(Boolean).join(" ");
  return combo ? `${combo}${room}` : room ? String(room).slice(3) : undefined;
};

/** 유틸: 지금 시간에 해당 수업인지 대략 판정(정보 없으면 false) */
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

/** 현재 수업 좌석/출석 가져오기(교사용) */
const fetchCurrentClassSeats = async (classes: RawClass[], setSeats: (s: SeatCell[] | null) => void) => {
  setSeats(null);
  if (!classes || classes.length === 0) return;

  const current = classes.find((c) => isNowIn(c)) || classes[0];
  const today = toYmd(new Date());
  try {
    const seatRows = await apiGet<
      Array<{ id?: number | string; seatNumber?: number | string; name?: string; attendance?: string }>
    >(`/teachers/classes/${encodeURIComponent(current.classId)}/seats?date=${encodeURIComponent(today)}`);

    const mapped: SeatCell[] = (seatRows || []).map((s, idx) => ({
      id: s.id ?? s.seatNumber ?? idx,
      seatNumber: s.seatNumber ?? idx + 1,
      name: s.name ?? "",
      attendance: s.attendance,
    }));
    setSeats(mapped);
    return;
  } catch {
    try {
      const atts = await apiGet<
        Array<{ studentId?: string; studentName?: string; seatNumber?: number | string; status?: string }>
      >(`/teachers/classes/${encodeURIComponent(current.classId)}/attendance?date=${encodeURIComponent(today)}`);

      const mapped: SeatCell[] = (atts || []).map((a, idx) => ({
        id: a.seatNumber ?? a.studentId ?? idx,
        seatNumber: a.seatNumber ?? idx + 1,
        name: a.studentName ?? a.studentId ?? "",
        attendance: a.status,
      }));
      setSeats(mapped);
    } catch {
      setSeats([]);
    }
  }
};

/** 메인 대시보드(통합) */
export default function GreenAcademyDashboard() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [user, setUser] = useState<LoginResponse | null>(null);
  const [ready, setReady] = useState(false);

  const [activeTab, setActiveTab] = useState<string>("종합정보");
  const [manageMenu, setManageMenu] =
    useState<"내정보" | "학생관리" | "반 관리" | "학원관리" | "QR 생성" | null>(null);

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [present, setPresent] = useState(0);
  const [late, setLate] = useState(0);
  const [absent, setAbsent] = useState(0);
  const [list, setList] = useState<Array<{ label: string; sub?: string }>>([]);
  const [seats, setSeats] = useState<SeatCell[] | null>(null);

  // QnA 최근 강제 오픈
  const [forcedQnaId, setForcedQnaId] = useState<string | null>(null);

  // 학생/학부모 QnA용 학원번호
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

  /** 최초 진입 시 쿼리 탭 반영 */
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

        if (user.role === "teacher") {
          const classes: RawClass[] = await api.listMyClasses(user.username);
          setList((classes || []).map((c) => ({ label: c.className, sub: formatSubtitle(c) })));
          await fetchCurrentClassSeats(classes || [], setSeats);
          setPresent(0);
          setLate(0);
          setAbsent(0);
          return;
        }

        if (user.role === "parent") {
          const target = user.childStudentId || user.username;
          const rows = await apiGet<StudentAttendanceRow[]>(`/parents/${encodeURIComponent(target)}/attendance`);
          const todayRows = rows.filter((r) => isSameDate(r.date));
          const sum = summarizeAttendance(todayRows);
          setPresent(sum.present);
          setLate(sum.late);
          setAbsent(sum.absent);
          setList(todayRows.map((r) => ({ label: r.className, sub: `${r.status} • ${r.date}` })));
          setSeats(null);
        } else if (user.role === "student") {
          const rows = await apiGet<StudentAttendanceRow[]>(
            `/students/${encodeURIComponent(user.username)}/attendance`
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
    clearSession();
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
              <Image src="/logo.png" alt="Logo" width={40} height={40} className="object-contain" priority />
            </div>
            <div className="leading-tight">
              <div className="text-lg font-semibold text-gray-900">Green Academy</div>
              <div className="text-sm text-gray-600 -mt-0.5">Partner</div>
            </div>
          </div>

          <NavTabs
            active={activeTab}
            onChange={(tab) => {
              setActiveTab(tab);
              if (tab !== "Q&A") setForcedQnaId(null);
            }}
            role={user?.role ?? null}
            manageMenu={manageMenu}
            onSelectManage={(item) => {
              setActiveTab("관리");
              setManageMenu(item);
            }}
          />

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
                <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">
                  강의실 찾기 추가 예정
                </span>
              </div>

              {user?.role !== "teacher" && (
                <div className="flex gap-3">
                  <StatCard title="금일 출석 학생 수" value={present} />
                  <StatCard title="금일 지각 학생 수" value={late} />
                  <StatCard title="금일 미출석 학생 수" value={absent} />
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-[300px_1fr] gap-6">
              <WaitingList title={user!.role === "teacher" ? "내 반 목록" : "오늘 일정"} list={list} loading={loading} error={err} />

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
          <>
            {!manageMenu && (
              <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-2">관리</h2>
                <p className="text-sm text-gray-700">상단의 ‘관리’ 드롭다운에서 항목을 선택하세요.</p>
              </div>
            )}

            {/* 내정보 */}
            {manageMenu === "내정보" && user?.role === "teacher" && <TeacherProfileCard user={user} />}
            {manageMenu === "내정보" && user?.role === "director" && <DirectorMyInfoCard />}

            {/* 학생/반 관리 */}
            {manageMenu === "학생관리" && <TeacherStudentManage />}
            {manageMenu === "반 관리" && user?.role === "teacher" && <TeacherManagePanel user={user} />}

            {/* 학원관리 */}
            {manageMenu === "학원관리" && user?.role === "director" && <DirectorRoomsPanel user={user} />}
            {manageMenu === "학원관리" && user?.role === "teacher" && (
              <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-2">학원관리</h2>
                <p className="text-sm text-gray-700">이 항목은 현재 권한에서 사용하지 않습니다.</p>
              </div>
            )}

            {/* QR 생성 */}
            {manageMenu === "QR 생성" && (user?.role === "teacher" || user?.role === "director") && (
              <QRGeneratorPanel user={user} />
            )}
          </>
        )}

        {/* 원장 전용: 출결확인 탭 */}
        {activeTab === "출결확인" && user?.role === "director" && <DirectorMyInfoCard/>}

        {/* 시간표 탭 */}
        {activeTab === "시간표" && (
          <>
            {user?.role === "teacher" ? (
              <TeacherSchedulePanelInline user={user} />
            ) : user?.role === "director" ? (
              <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-2">시간표</h2>
                <p className="text-sm text-gray-700">원장은 ‘출결확인’ 탭도 함께 사용할 수 있습니다.</p>
              </div>
            ) : (
              <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-2">시간표</h2>
                <p className="text-sm text-gray-700">현재 역할에는 시간표 기능이 준비 중입니다.</p>
              </div>
            )}
          </>
        )}

        {/* Q&A 탭 */}
        {activeTab === "Q&A" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Q&amp;A</h2>
              {user?.role === "teacher" || user?.role === "director" ? (
                <TeacherQnaPanel questionId={forcedQnaId ?? undefined} />
              ) : academyNumber == null ? (
                <p className="text-sm text-gray-700">학원번호를 확인할 수 없습니다. 프로필 또는 로그인 정보를 확인해 주세요.</p>
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
            <div className="space-y-4 rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">가이드</h2>
              <p className="text-sm text-gray-700">사용 설명서/튜토리얼 문서를 표시합니다.</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
