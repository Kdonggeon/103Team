// src/components/manage/TeacherSchedulePanelInline.tsx
"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import api, { type LoginResponse, type ScheduleItem } from "@/app/lib/api";
import Panel, { PanelGrid } from "@/components/ui/Panel";
import WeekCalendar, { type CalendarEvent } from "@/components/ui/calendar/week-calendar";
import { roomsApi, type Room } from "@/app/lib/rooms";

/* ================== 유틸 ================== */
const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const ymd = (d: Date) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
function jsToIsoDow(jsDow: number) { return (jsDow === 0 ? 7 : jsDow) as 1|2|3|4|5|6|7; }
function thisWeekRange(today = new Date()) {
  const dow = today.getDay();
  const offsetToMon = dow === 0 ? -6 : 1 - dow;
  const mon = new Date(today);
  mon.setDate(today.getDate() + offsetToMon);
  mon.setHours(0,0,0,0);
  const nextMon = new Date(mon); nextMon.setDate(mon.getDate() + 7);
  return { from: ymd(mon), to: ymd(nextMon) }; // [from, to)
}
const getRoomNumber = (r: Room) => Number((r as any).roomNumber ?? (r as any).number ?? (r as any).Room_Number);

/* 🎨 파스텔 팔레트 */
const PALETTE = ["#E0F2FE","#FCE7F3","#FEF3C7","#DCFCE7","#EDE9FE","#FFE4E6","#F5F5F4","#D1FAE5","#FDE68A","#E9D5FF"];
const colorByKey = (key: string) => {
  let h = 0; for (let i=0;i<key.length;i++) h = (h*31 + key.charCodeAt(i))>>>0;
  return PALETTE[h % PALETTE.length];
};

/* ✓ 안전하게 user 복구 */
function loadUserFromClient(): LoginResponse | null {
  if (typeof window === "undefined") return null;
  const keys = ["session","login","auth"];
  for (const k of keys) {
    const raw = localStorage.getItem(k);
    if (!raw) continue;
    try {
      const obj = JSON.parse(raw);
      // 토큰만 저장된 경우도 있어 필드 존재 검사 최소화
      if (obj && typeof obj === "object" && (obj.username || obj.role)) {
        return obj as LoginResponse;
      }
    } catch {
      // 무시
    }
  }
  return null;
}

/* ================== 메인 ================== */
export default function TeacherSchedulePanelInline({ user: userProp }: { user?: LoginResponse | null }) {
  const router = useRouter();

  // user 없으면 클라이언트에서 복구
  const [user, setUser] = useState<LoginResponse | null>(userProp ?? null);
  useEffect(() => {
    if (!userProp) {
      const u = loadUserFromClient();
      if (u) setUser(u);
    } else {
      setUser(userProp);
    }
  }, [userProp]);

  // 로그인 가드
  if (!user) {
    return (
      <div className="space-y-4">
        <Panel title="주간 캘린더">
          <div className="text-sm text-gray-700">
            로그인 정보가 없습니다. <button
              onClick={() => router.push("/login")}
              className="underline text-emerald-700"
            >로그인</button> 후 다시 시도하세요.
          </div>
        </Panel>
      </div>
    );
  }

  const teacherId = user.username;
  const academyNumber = user.academyNumbers?.[0] ?? null;

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomFilter, setRoomFilter] = useState<string>("ALL");
  const [weekRows, setWeekRows] = useState<ScheduleItem[]>([]);

  const loadThisWeek = async () => {
    setErr(null);
    const { from, to } = thisWeekRange(new Date());
    try {
      const rows = await api.listSchedules(teacherId, from, to);
      setWeekRows(rows ?? []);
    } catch (e: any) {
      setErr(e?.message ?? "이번 주 스케줄을 불러오지 못했습니다.");
    }
  };

  useEffect(() => {
    (async () => {
      setLoading(true);
      await loadThisWeek();
      setLoading(false);
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teacherId]);

  useEffect(() => {
    (async () => {
      if (!academyNumber) return;
      try {
        const list = await roomsApi.listRooms(academyNumber);
        setRooms(Array.isArray(list) ? list : []);
      } catch (e) {
        console.warn("roomsApi.listRooms 실패:", e);
      }
    })();
  }, [academyNumber]);

  const events: CalendarEvent[] = useMemo(() => {
    const out: CalendarEvent[] = [];
    for (const s of weekRows) {
      if (roomFilter !== "ALL") {
        const rn = Number(roomFilter);
        if (s.roomNumber == null || Number(s.roomNumber) !== rn) continue;
      }
      const d = new Date(`${s.date}T00:00:00`);
      const key = s.classId || s.title || "event";
      out.push({
        id: s.scheduleId || `${s.classId}-${s.date}`,
        title: (s.title && s.title.trim()) || s.classId,
        room: s.roomNumber != null ? `Room ${s.roomNumber}` : undefined,
        dayOfWeek: jsToIsoDow(d.getDay()),
        startTime: (s.startTime as any) || "00:00",
        endTime:   (s.endTime   as any) || "23:59",
        href: `/teacher/classes/${encodeURIComponent(s.classId)}`,
        color: colorByKey(key),
      });
    }
    return out;
  }, [weekRows, roomFilter]);

  return (
    <div className="space-y-4">
      {err && (
        <Panel title="오류">
          <div className="text-red-600">{err}</div>
        </Panel>
      )}

      <PanelGrid>
        <Panel title="도움말">
          <ul className="text-sm text-gray-700 list-disc pl-5 space-y-1">
            <li>이 패널은 이번 주 수업만 보여줍니다.</li>
            <li>오른쪽에서 강의실별 필터가 가능합니다.</li>
            <li>블록을 클릭하면 반 상세로 이동합니다.</li>
          </ul>
        </Panel>

        <Panel
          title="주간 캘린더"
          right={
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                <label className="text-xs text-gray-600">방 필터</label>
                <select
                  value={roomFilter}
                  onChange={(e) => setRoomFilter(e.target.value)}
                  className="border rounded px-2 py-1 text-sm !text-black"
                >
                  <option value="ALL">전체</option>
                  {rooms.map((r) => {
                    const rn = getRoomNumber(r);
                    return (
                      <option key={String(rn)} value={String(rn)} className="text-black">
                        Room {rn}
                      </option>
                    );
                  })}
                </select>
              </div>

              {loading && <span className="text-xs text-gray-500">불러오는 중…</span>}
              <button onClick={loadThisWeek} className="px-3 py-1.5 rounded border text-sm !text-black">
                새로고침
              </button>
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
            <WeekCalendar
              startHour={8}
              endHour={22}
              events={events}
              lineColor="rgba(0,0,0,0.18)"
              textColor="#111111"
              showNowLine
            />
          )}
        </Panel>
      </PanelGrid>
    </div>
  );
}
