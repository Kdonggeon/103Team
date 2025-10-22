"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getSession, clearSession } from "@/app/lib/session";
import api, { type LoginResponse, type CourseLite } from "@/app/lib/api";
// 강의실 API + 에디터
import { roomsApi, type Room } from "@/app/lib/rooms";
import RoomGridEditor, { type SeatCell as EditorSeat } from "@/components/rooms/RoomGridEditor";


// 시간표 UI
import Panel, { PanelGrid } from "@/components/ui/Panel";
import WeekCalendar, { type CalendarEvent } from "@/components/ui/calendar/week-calendar";

/** 색상 토큰 */
const colors = { green: "#65E478", grayBg: "#F2F4F7" };

/** 대시보드에서 쓰는 타입 */
type LoginSession = LoginResponse | null;

type StudentAttendanceRow = {
  classId: string;
  className: string;
  date: string;     // "yyyy-MM-dd" or ISO
  status: string;   // "PRESENT" | "LATE" | "ABSENT" | ...
};

type SeatCell = {
  id: number | string;
  name?: string;
  status?: "end" | "label" | "empty" | "filled";
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
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
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

/** 상단 탭 */
function NavTabs({ active, onChange }: { active: string; onChange: (tab: string) => void }) {
  const tabs = ["종합정보", "관리", "시간표", "Q&A", "공지사항", "가이드"];
  return (
    <div className="flex gap-3 md:gap-4">
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

/** 프로필 드롭다운 메뉴 */
function ProfileMenu({ user }: { user: NonNullable<LoginSession> | null }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onClick = (e: MouseEvent) => { if (!ref.current) return; if (!ref.current.contains(e.target as Node)) setOpen(false); };
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") setOpen(false); };
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
        aria-haspopup="menu" aria-expanded={open} aria-label="프로필 메뉴 열기"
      >{initial}</button>

      {open && (
        <div className="absolute right-0 mt-2 w-52 rounded-xl bg-white shadow-lg ring-1 ring-black/5 overflow-hidden z-20">
          <div className="px-4 py-2 text-xs font-semibold text-gray-900 border-b border-gray-100">
            {user?.name || user?.username}
          </div>
          <button onClick={() => { setOpen(false); router.push("/notifications"); }} className="w-full text-left px-4 py-2 text-sm text-gray-900 hover:bg-gray-50">🔔 내 알림</button>
          <button onClick={() => { setOpen(false); router.push("/settings/theme"); }} className="w-full text-left px-4 py-2 text-sm text-gray-900 hover:bg-gray-50">🎨 테마 설정</button>
          <button onClick={() => { setOpen(false); router.push("/settings"); }} className="w-full text-left px-4 py-2 text-sm text-gray-900 hover:bg-gray-50">⚙️ 환경 설정</button>
        </div>
      )}
    </div>
  );
}

/** 사이드 프로필 */
function SidebarProfile({ user, onLogout }: { user: NonNullable<LoginResponse> | null; onLogout: () => void }) {
  const router = useRouter();
  const role = user?.role;
  const roleColor =
    role === "teacher" ? "bg-blue-100 text-blue-700 ring-blue-200" :
    role === "student" ? "bg-emerald-100 text-emerald-700 ring-emerald-200" :
    role === "parent"  ? "bg-amber-100 text-amber-700 ring-amber-200" :
    role === "director"? "bg-purple-100 text-purple-700 ring-purple-200" :
                         "bg-gray-100 text-gray-700 ring-gray-200";

  const academies = Array.isArray(user?.academyNumbers) && user!.academyNumbers!.length > 0 ? user!.academyNumbers! : [];

  return (
    <aside className="w-[260px] shrink-0">
      <div className="rounded-2xl overflow-hidden ring-1 ring-black/5 shadow-sm bg-white">
        <div className="p-5 bg-gradient-to-br from-[#CFF9D6] via-[#B7F2C0] to-[#8CF39B]">
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <div className="text-xl font-semibold text-gray-900 truncate">{user?.name || user?.username || "사용자"}</div>
            </div>
            {role && (
              <span className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-medium ring-1 ${roleColor}`} title={`role: ${role}`}>
                <span className="inline-block w-2 h-2 rounded-full bg-current opacity-70" />
                {role}
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
                    <span key={`${n}-${i}`} className="inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium bg-gray-100 text-gray-800 ring-1 ring-gray-200">#{n}</span>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="h-px bg-gradient-to-r from-transparent via-gray-200 to-transparent my-2" />

          <div className="grid grid-cols-2 gap-2">
            <button onClick={() => router.push("/settings/profile")} className="rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-xs font-medium text-gray-800">개인정보 수정</button>
            <button onClick={() => router.push("/account/delete")} className="rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-xs font-medium text-gray-800">계정탈퇴</button>
          </div>

          <button onClick={onLogout} className="w-full rounded-xl py-3 text-white font-semibold mt-1 active:scale-[0.99] transition" style={{ backgroundColor: colors.green }}>
            로그아웃
          </button>
        </div>
      </div>

      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-4 space-y-3 mt-4">
        <div className="text-sm font-semibold text-gray-900">빠른 실행</div>
        <div className="grid gap-2">
          <button onClick={() => router.push("/settings")} className="w-full rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-sm text-gray-800">환경 설정</button>
          <button onClick={() => router.push("/qna/recent")} className="w-full rounded-xl bg-gray-50 hover:bg-gray-100 active:scale-[0.99] transition ring-1 ring-gray-200 py-2 text-sm text-gray-800">최근 QnA 바로가기</button>
        </div>
      </div>
    </aside>
  );
}

/** 역할별 리스트 */
function WaitingList({
  title, list, loading, error,
}: { title: string; list: Array<{ label: string; sub?: string }>; loading: boolean; error?: string | null; }) {
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
        {!loading && !error && list.map((w, i) => (
          <div key={i} className="px-3 py-2 border-b last:border-none text-sm bg-white">
            <div className="font-medium text-gray-900">{w.label}</div>
            {w.sub && <div className="text-xs text-gray-600">{w.sub}</div>}
          </div>
        ))}
      </div>
    </div>
  );
}

/** 좌석 그리드(추후 연동) */
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
          <div key={s.id} className="h-14 rounded-xl flex items-center justify-center text-sm ring-1 ring-black/5 bg-gray-100 text-gray-900" title={s.name}>
            {s.name || ""}
          </div>
        ))}
      </div>
      <div className="mt-4 text-right text-xs text-gray-500">* 좌석 배치 (실데이터)</div>
    </div>
  );
}

/** ✅ 교사용 ‘관리’ 패널(페이지 내부 컴포넌트) */
function TeacherManagePanel({ user }: { user: NonNullable<LoginResponse> }) {
  const teacherId = user.username;
  const defaultAcademy = user.academyNumbers?.[0] ?? null;

  const [items, setItems] = useState<CourseLite[]>([]);
  const [className, setClassName] = useState("");
  const [roomNumber, setRoomNumber] = useState<string>("");

  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // 🔎 학생 검색/선택 상태
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState<string>("");
  const [hits, setHits] = useState<Array<{studentId:string; studentName?:string|null; grade?:number|null}>>([]);
  const [searching, setSearching] = useState(false);
  const [selected, setSelected] = useState<string[]>([]); // 선택된 학생 ID

  const load = async () => {
    if (!teacherId) { setErr("로그인이 필요합니다."); setLoading(false); return; }
    setErr(null); setMsg(null); setLoading(true);
    try {
      const res = await api.listMyClasses(teacherId);
      setItems(res || []);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, []);

  // 학생 검색
  const search = async () => {
    if (!defaultAcademy) { setErr("학원번호가 없습니다."); return; }
    try {
      setSearching(true); setErr(null);
      const res = await api.searchStudents(defaultAcademy, q, grade ? Number(grade) : undefined);
      setHits(res);
    } catch (e:any) { setErr(e.message); } finally { setSearching(false); }
  };

  const togglePick = (sid: string) =>
    setSelected(prev => prev.includes(sid) ? prev.filter(x=>x!==sid) : [...prev, sid]);

  // 반 생성 + 선택 학생 일괄 추가
  const create = async () => {
    if (!defaultAcademy) { setErr("학원번호가 없습니다."); return; }
    if (!className.trim()) { setErr("반 이름을 입력하세요."); return; }
    try {
      setErr(null); setMsg(null);

      // 1) 반 생성
      const created = await api.createClass({
        className: className.trim(),
        teacherId,
        academyNumber: defaultAcademy,
        roomNumber: roomNumber ? Number(roomNumber) : undefined,
      });

      // 2) 선택 학생 추가
      if (created?.classId && selected.length > 0) {
        for (const sid of selected) {
          await api.addStudentToClass(created.classId, sid);
        }
      }

      // 3) 초기화 & 리로드
      setClassName(""); setRoomNumber("");
      setSelected([]); setHits([]); setQ(""); setGrade("");
      await load();
      setMsg("반이 생성되었습니다.");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
    <div className="space-y-4">
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-3">내 반 관리</h2>

        {/* 생성 폼 */}
        <div className="bg-gray-50/60 border rounded p-4 space-y-4">
          {/* 기본정보 */}
          <div className="flex flex-wrap gap-3 items-end">
            <div>
              <label className="block text-sm text-gray-600">반 이름</label>
              <input value={className} onChange={(e)=>setClassName(e.target.value)}
                     className="border rounded px-2 py-1" />
            </div>
            <div>
              <label className="block text-sm text-gray-600">방 번호(선택)</label>
              <input value={roomNumber} onChange={(e)=>setRoomNumber(e.target.value)}
                     className="border rounded px-2 py-1 w-32" />
            </div>
          </div>

          {/* 학생 검색/선택 */}
          <div className="space-y-2">
            <div className="text-sm font-medium text-gray-900">학생 추가(선택)</div>
            <div className="flex flex-wrap gap-2">
              <input value={q} onChange={(e)=>setQ(e.target.value)} placeholder="이름 검색"
                     className="border rounded px-2 py-1" />
              <input value={grade} onChange={(e)=>setGrade(e.target.value)} placeholder="학년(선택)"
                     className="border rounded px-2 py-1 w-28" />
              <button onClick={search} className="px-3 py-1.5 rounded bg-gray-800 text-white">검색</button>
              {searching && <span className="text-xs text-gray-500">검색중…</span>}
            </div>

            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
              {hits.map(h => {
                const picked = selected.includes(h.studentId);
                return (
                  <button key={h.studentId} onClick={() => togglePick(h.studentId)}
                          className={`text-left border rounded px-3 py-2 transition
                            ${picked ? "bg-emerald-50 border-emerald-200" : "bg-white hover:bg-gray-50"}`}>
                    <div className="font-medium">
                      {h.studentName ?? h.studentId}
                      {picked && <span className="ml-2 text-emerald-600 text-xs">선택됨</span>}
                    </div>
                    <div className="text-xs text-gray-600">
                      ID: {h.studentId} · 학년: {h.grade ?? "-"}
                    </div>
                  </button>
                );
              })}
              {hits.length === 0 && <div className="text-sm text-gray-500">검색 결과 없음</div>}
            </div>

            {selected.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {selected.map(sid => (
                  <span key={sid}
                        className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full text-sm bg-emerald-100 text-emerald-800">
                    {sid}
                    <button onClick={()=>togglePick(sid)} className="text-emerald-700 hover:underline">×</button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-center gap-3">
            <button onClick={create} className="bg-emerald-600 text-white px-4 py-2 rounded">
              반 만들기
            </button>
            {msg && <span className="text-emerald-600">{msg}</span>}
            {err && <span className="text-red-600">{err}</span>}
          </div>
        </div>

        {/* 목록 */}
        {loading && <div className="mt-3 text-sm text-gray-600">불러오는 중…</div>}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 mt-4">
          {items.map(c => (
            <a key={c.classId} href={`/teacher/classes/${encodeURIComponent(c.classId)}`}
               className="bg-white border rounded p-3 hover:shadow">
              <div className="font-semibold">{c.className}</div>
              <div className="text-sm text-gray-600">Room #{c.roomNumber ?? "-"}</div>
              <div className="text-sm text-gray-600">학생 수: {c.students?.length ?? 0}</div>
              <div className="mt-2 text-sm">
                <span className="text-emerald-700 hover:underline">학생 관리</span> ·{" "}
                <span className="text-gray-700 hover:underline">시간표</span>
              </div>
            </a>
          ))}
          {(!loading && items.length === 0) && (
            <div className="text-sm text-gray-500">아직 생성된 반이 없습니다.</div>
          )}
        </div>
      </div>
    </div>
  );
}


/** 🔧 요일 문자열 → 숫자(1~7) 정규화 */
function normalizeDays(days: any): number[] {
  if (!days) return [];
  const mapFullWidth: Record<string, string> = { "１":"1","２":"2","３":"3","４":"4","５":"5","６":"6","７":"7" };
  const mapKorean: Record<string, number> = { "월":1, "화":2, "수":3, "목":4, "금":5, "토":6, "일":7 };
  return (Array.isArray(days) ? days : [days]).map((d: any) => {
    if (typeof d === "number") return d;
    if (typeof d === "string") {
      const s = mapFullWidth[d] ?? d;
      if (mapKorean[s]) return mapKorean[s];
      const n = parseInt(s, 10);
      if (n >= 1 && n <= 7) return n;
    }
    return null;
  }).filter(Boolean) as number[];
}

type CourseDetail = CourseLite & {
  startTime?: string;
  endTime?: string;
  daysOfWeek?: number[];
};

/** ✅ 교사용 시간표 패널(“시간표” 탭에서 사용) */
function TeacherSchedulePanelInline({ user }: { user: NonNullable<LoginResponse> }) {
  const teacherId = user.username;
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [courses, setCourses] = useState<CourseDetail[]>([]);
  const router = useRouter();

  useEffect(() => {
    (async () => {
      setErr(null); setLoading(true);
      try {
        const list = await api.listMyClasses(teacherId);
        const details = await Promise.all(
          (list || []).map(async (c) => {
            try {
              const d = await api.getClassDetail(c.classId);
              const startTime = (d as any).startTime ?? (d as any).Start_Time ?? undefined;
              const endTime   = (d as any).endTime   ?? (d as any).End_Time   ?? undefined;
              const daysRaw   = (d as any).daysOfWeek ?? (d as any).Days_Of_Week ?? undefined;
              const daysOfWeek = normalizeDays(daysRaw);
              return { ...c, startTime, endTime, daysOfWeek } as CourseDetail;
            } catch {
              return { ...c } as CourseDetail;
            }
          })
        );
        setCourses(details);
      } catch (e: any) {
        setErr(e?.message ?? "시간표를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [teacherId]);

  const events: CalendarEvent[] = useMemo(() => {
    const out: CalendarEvent[] = [];
    for (const c of courses) {
      if (!c.daysOfWeek || !c.startTime || !c.endTime) continue;
      for (const d of c.daysOfWeek) {
        out.push({
          id: `${c.classId}-${d}`,
          title: c.className,
          ...(c.roomNumber != null ? { room: `Room ${c.roomNumber}` } : {}),
          dayOfWeek: d as CalendarEvent["dayOfWeek"],
          startTime: c.startTime!,
          endTime: c.endTime!,
          href: `/teacher/classes/${encodeURIComponent(c.classId)}`,
        });
      }
    }
    return out;
  }, [courses]);

  return (
    <div className="space-y-4">
      {err && <Panel title="오류"><div className="text-red-600">{err}</div></Panel>}

      <PanelGrid>
        {/* 좌: 도움말 */}
        <Panel title="도움말">
          <ul className="text-sm text-gray-700 list-disc pl-5 space-y-1">
            <li>블록 클릭 시 반 상세로 이동합니다.</li>
            <li>반 상세에서 요일/시간을 설정·수정하세요.</li>
            <li>새 스케줄 추가/수정은 우측 상단 <b>스케줄 관리(+)</b>에서 합니다.</li>
          </ul>
        </Panel>

        {/* 우: 주간 캘린더 + 스케줄 관리 버튼 */}
         <Panel
            title="주간 캘린더"
            right={
              <div className="flex items-center gap-3">
                {loading && <span className="text-xs text-gray-500">불러오는 중…</span>}
                <button
                  onClick={() => router.push("/teacher/schedule")} 
                  className="px-3 py-1.5 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700"
                >
                  스케줄 관리(+)
                  
                </button>
              </div>
            }
          >
          {loading ? (
            <div className="text-sm text-gray-600">로딩 중…</div>
          ) : (
            <WeekCalendar startHour={8} endHour={22} events={events} />
          )}
        </Panel>
      </PanelGrid>
    </div>
  );
}

/** 메인 대시보드 */
export default function GreenAcademyDashboard() {
  const router = useRouter();

  const [user, setUser] = useState<LoginResponse | null>(null);
  const [ready, setReady] = useState(false);

  const [activeTab, setActiveTab] = useState<string>("종합정보");

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [present, setPresent] = useState(0);
  const [late, setLate] = useState(0);
  const [absent, setAbsent] = useState(0);

  const [list, setList] = useState<Array<{ label: string; sub?: string }>>([]);

  const [seats] = useState<SeatCell[] | null>(null);

  /** 세션 로드 & 가드 */
  useEffect(() => {
    const s = getSession();
    if (!s) { router.replace("/login"); return; }
    setUser(s);
    setReady(true);
  }, [router]);

  /** 역할별 데이터 로딩 (종합정보) */
  useEffect(() => {
    if (!ready || !user) return;
    if (activeTab !== "종합정보") return;

    setLoading(true); setErr(null);

    (async () => {
      try {
        setList([]);

        if (user.role === "teacher") {
          const classes = await api.listMyClasses(user.username);
          setList((classes || []).map((c) => ({ label: c.className, sub: c.classId })));
          setPresent(0); setLate(0); setAbsent(0);
          return;
        }

        if (user.role === "parent") {
          const target = user.childStudentId || user.username;
          const rows = await apiGet<StudentAttendanceRow[]>(`/parents/${encodeURIComponent(target)}/attendance`);
          const todayRows = rows.filter((r) => isSameDate(r.date));
          const sum = summarizeAttendance(todayRows);
          setPresent(sum.present); setLate(sum.late); setAbsent(sum.absent);
          setList(todayRows.map((r) => ({ label: r.className, sub: `${r.status} • ${r.date}` })));
        } else if (user.role === "student") {
          const rows = await apiGet<StudentAttendanceRow[]>(`/students/${encodeURIComponent(user.username)}/attendance`);
          const todayRows = rows.filter((r) => isSameDate(r.date));
          const sum = summarizeAttendance(todayRows);
          setPresent(sum.present); setLate(sum.late); setAbsent(sum.absent);
          setList(todayRows.map((r) => ({ label: r.className, sub: `${r.status} • ${r.date}` })));
        } else {
          setPresent(0); setLate(0); setAbsent(0); setList([]);
        }
      } catch (e: any) {
        setErr(e?.message ?? "데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [ready, user, activeTab]);

  const handleLogout = () => { clearSession(); router.replace("/login"); };
  const handleTab = (t: string) => setActiveTab(t);

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

          <NavTabs active={activeTab} onChange={handleTab} />

          <ProfileMenu user={user} />
        </div>
      </header>

      {/* 본문 */}
      <main className="max-w-7xl mx-auto px-6 py-6 grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
        <SidebarProfile user={user} onLogout={handleLogout} />

        {/* 탭별 콘텐츠 */}
        {activeTab === "종합정보" && (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <span className="px-4 py-2 rounded-full bg-gray-100 text-sm text-gray-900 font-medium">강의실 찾기 추가 예정</span>
              </div>
              <div className="flex gap-3">
                <StatCard title="금일 출석 학생 수" value={present} />
                <StatCard title="금일 지각 학생 수" value={late} />
                <StatCard title="금일 미출석 학생 수" value={absent} />
              </div>
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-[300px_1fr] gap-6">
              <WaitingList title={user!.role === "teacher" ? "내 반 목록" : "오늘 일정"} list={list} loading={loading} error={err} />
              <SeatGrid seats={seats} />
            </div>
          </div>
        )}

        {activeTab === "관리" && (
          <>
            {user?.role === "teacher" && <TeacherManagePanel user={user} />}

            {user?.role === "director" && (
      <DirectorRoomsPanel user={user} />
    )}

            {(user?.role === "student" || user?.role === "parent") && (
              <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-2">관리</h2>
                <p className="text-sm text-gray-700">이 역할에는 관리 메뉴가 없습니다.</p>
              </div>
            )}
          </>
        )}

        {activeTab === "시간표" && (
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

        {activeTab === "Q&A" && (
          <div className="space-y-4">
            <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Q&A</h2>
              <p className="text-sm text-gray-700">Q&A 게시판을 연결하세요.</p>
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
              <p className="text-sm text-gray-700">사용 설명서/튜토리얼 문서를 표시합니다.</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

/** 통계 합산 */
function summarizeAttendance<T extends { status: string }>(rows: T[]) {
  let present = 0, late = 0, absent = 0;
  rows.forEach((r) => {
    const s = (r.status || "").toUpperCase();
    if (s.includes("LATE")) late += 1;
    else if (s.includes("ABSENT") || s.includes("ABS")) absent += 1;
    else present += 1;
  });
  return { present, late, absent };
}
/** ✅ 원장용 강의실 관리 패널 (Panel 스타일) */
function DirectorRoomsPanel({ user }: { user: NonNullable<LoginResponse> }) {
  // 학원번호
  const academyOptions = Array.isArray(user.academyNumbers) ? user.academyNumbers : [];
  const [academyNumber, setAcademyNumber] = useState<number | undefined>(academyOptions[0]);

  const [rooms, setRooms] = useState<Room[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // ✅ 생성 폼: n×m
  const [roomNumber, setRoomNumber] = useState<number>(401);
  const [rowsN, setRowsN] = useState<number>(5);
  const [colsN, setColsN] = useState<number>(6);
  const [preview, setPreview] = useState<EditorSeat[]>([]);

  // 0-based <-> 1-based 변환 (백엔드가 1-base면 유지)
  const toBackend = (v: EditorSeat[]) =>
    v.map(s => ({ seatNumber: s.seatNumber, row: s.row + 1, col: s.col + 1, disabled: !!s.disabled }));
  const fromBackend = (v: any[]) =>
    (v || []).map(s => ({ seatNumber: s.seatNumber, row: (s.row ?? 1) - 1, col: (s.col ?? 1) - 1, disabled: !!s.disabled })) as EditorSeat[];

  // n×m 초기 좌석
  useEffect(() => {
    const a: EditorSeat[] = [];
    let k = 1;
    for (let i = 0; i < rowsN; i++) for (let j = 0; j < colsN; j++) a.push({ seatNumber: k++, row: i, col: j, disabled: false });
    setPreview(a);
  }, [rowsN, colsN]);

  // 목록 로드
  const load = async () => {
    if (!academyNumber) return;
    setLoading(true); setErr(null);
    try {
      const list = await roomsApi.listRooms(academyNumber);
      setRooms(list || []);
    } catch (e: any) { setErr(e.message); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [academyNumber]);

  // 생성: getOrCreate -> layout 저장
  const create = async () => {
    if (!academyNumber) return alert("학원번호를 선택하세요.");
    setErr(null);
    try {
      await roomsApi.saveRoomLayout(roomNumber, {
        academyNumber,
        rows: rowsN,
        cols: colsN,
        layout: toBackend(preview),
      });
      await load();
            await roomsApi.saveRoomLayout(roomNumber, {
              academyNumber,
              rows: rowsN,
              cols: colsN,
              layout: toBackend(preview),
            });
            await load();
    } catch (e: any) { setErr(e.message); }
  };

  // 레이아웃 저장
  const saveLayout = async (room: Room, layout: EditorSeat[]) => {
    if (!academyNumber) return;
    try {
      await roomsApi.saveRoomLayout(room.roomNumber, {
        academyNumber,
        rows: room.rows ?? Math.max(...layout.map(s => s.row), 0) + 1,
        cols: room.cols ?? Math.max(...layout.map(s => s.col), 0) + 1,
        layout: toBackend(layout),
      });
      await load();
    } catch (e: any) { setErr(e.message); }
  };

  // 삭제
  const remove = async (room: Room) => {
    if (!academyNumber) return;
    if (!confirm(`Room #${room.roomNumber} 삭제할까요?`)) return;
    try {
      await roomsApi.deleteRoom(academyNumber, room.roomNumber);
      await load();
    } catch (e: any) { setErr(e.message); }
  };

  return (
    <Panel
      title="강의실 관리"
      right={
        <div className="flex items-center gap-2">
          <label className="text-sm text-black">학원번호</label>
          <select
            className="border border-black rounded p-2 text-black bg-white"
            value={academyNumber ?? ""}
            onChange={(e) => setAcademyNumber(parseInt(e.target.value))}
          >
            {academyOptions.map(n => <option key={n} value={n}>{n}</option>)}
          </select>
        </div>
      }
    >
      {/* 생성 폼 */}
      <div className="border border-black rounded p-4 space-y-4 bg-white">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="block text-sm text-black">방 번호</label>
            <input
              type="number"
              className="border border-black rounded px-2 py-1 w-32 text-black"
              value={roomNumber}
              onChange={e=>setRoomNumber(parseInt(e.target.value||""))}
            />
          </div>
          <div>
            <label className="block text-sm text-black">행 (rows)</label>
            <input
              type="number" min={1} max={30}
              className="border border-black rounded px-2 py-1 w-24 text-black"
              value={rowsN}
              onChange={e=>setRowsN(parseInt(e.target.value||"1"))}
            />
          </div>
          <div>
            <label className="block text-sm text-black">열 (cols)</label>
            <input
              type="number" min={1} max={30}
              className="border border-black rounded px-2 py-1 w-24 text-black"
              value={colsN}
              onChange={e=>setColsN(parseInt(e.target.value||"1"))}
            />
          </div>
          <button onClick={create} className="ml-auto px-4 py-2 rounded bg-black text-white">반 만들기</button>
        </div>

        <div>
          <div className="text-sm text-black mb-2">초기 좌석 미리보기</div>
          <RoomGridEditor rows={rowsN} cols={colsN} value={preview} onChange={setPreview}/>
        </div>
      </div>

      {/* 목록 */}
      <div className="mt-4">
        {loading && <div className="text-sm text-black">불러오는 중…</div>}
        {err && <div className="text-sm text-red-600">오류: {err}</div>}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {rooms.map(room => {
            const seats = fromBackend(room.layout as any[]);
            const rowsCalc = room.rows ?? (seats.length ? Math.max(...seats.map(s=>s.row),0)+1 : 1);
            const colsCalc = room.cols ?? (seats.length ? Math.max(...seats.map(s=>s.col),0)+1 : 1);
            return (
              <div key={`${room.academyNumber}-${room.roomNumber}`} className="relative border border-black rounded-xl p-4 bg-white">
                {/* 우상단 X */}
                <button
                  onClick={()=>remove(room)}
                  className="absolute -top-2 -right-2 w-8 h-8 rounded-full bg-black text-white text-sm"
                >×</button>

                <div className="text-black mb-1">Room #{room.roomNumber}</div>
                <div className="text-sm mb-3 text-black">좌석 수: {seats.filter(s=>!s.disabled).length}</div>

                <RoomGridEditor
                  rows={rowsCalc}
                  cols={colsCalc}
                  value={seats}
                  onChange={(v)=>saveLayout(room, v)}
                />
                <div className="text-xs text-black mt-2">변경 즉시 저장</div>
              </div>
            );
          })}
          {!loading && rooms.length === 0 && (
            <div className="text-sm text-black">아직 생성된 강의실이 없습니다.</div>
          )}
        </div>
      </div>
    </Panel>
  );
}
