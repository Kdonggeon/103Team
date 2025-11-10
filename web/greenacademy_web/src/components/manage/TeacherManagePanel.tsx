"use client";

import React, { useEffect, useMemo, useState, lazy, Suspense } from "react";
import { request, api, type CourseLite } from "@/app/lib/api";
import type { LoginResponse } from "@/app/lib/api";
import { roomsVectorApi, type AdminRoomLite } from "@/app/lib/rooms.vector";

/** ▣ 아주 간단한 로더 아이콘 대체 */
function Spinner() {
  return (
    <div className="animate-pulse text-black/70 text-sm flex items-center gap-2">
      <div className="w-4 h-4 rounded-full border-2 border-black/20 border-t-black/80 animate-spin" />
      로딩중…
    </div>
  );
}

/** ▣ 가운데 뜨는 모달(외부 의존성 없음) */
function CenterModal({
  open,
  onOpenChange,
  title,
  maxWidth = 1120,
  children,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  title?: React.ReactNode;
  maxWidth?: number;
  children?: React.ReactNode;
}) {
  // ESC로 닫기
  React.useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onOpenChange(false); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onOpenChange]);

  return (
    <>
      {/* Overlay */}
      <div
        onClick={() => onOpenChange(false)}
        className="fixed inset-0 bg-black/40 transition-opacity z-[1000]"
        style={{ pointerEvents: open ? "auto" : "none", opacity: open ? 1 : 0 }}
      />

      {/* Modal (centered) */}
      <div
        className="fixed inset-0 z-[1001] flex items-center justify-center p-4"
        style={{ pointerEvents: open ? "auto" : "none" }}
      >
        <div
          className="bg-white w-full rounded-2xl shadow-2xl border border-black/10 transition-all duration-200"
          style={{
            maxWidth,
            transform: open ? "scale(1)" : "scale(0.98)",
            opacity: open ? 1 : 0,
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* 헤더 */}
          <div className="flex items-center justify-between px-6 h-14 border-b">
            <div className="text-base font-semibold text-black">{title}</div>
            <button
              onClick={() => onOpenChange(false)}
              className="text-black/70 hover:text-black"
              aria-label="close"
              title="닫기"
            >
              ×
            </button>
          </div>

          {/* 바디: 높이 제한 + 스크롤 */}
          <div className="max-h-[85vh] overflow-auto">{children}</div>
        </div>
      </div>
    </>
  );
}

/** 숫자 1글자만 허용하는 정규화 */
const normalizeOneDigit = (s: string) => s.replace(/\D+/g, "").slice(0, 1);

/** 작은 뱃지 스타일 */
function Chip({
  children,
  active = false,
  onClick,
}: {
  children: React.ReactNode;
  active?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        "px-2.5 py-1 rounded-full text-sm ring-1 transition",
        active
          ? "bg-emerald-100 text-black ring-emerald-300"
          : "bg-white text-black ring-gray-300 hover:bg-gray-50",
      ].join(" ")}
    >
      {children}
    </button>
  );
}

const ClassDetailClient = lazy(() =>
  import("@/app/teacher/classes/[classId]/ClassDetailClient") // 필요 시 실제 경로로 조정
);

export default function TeacherManagePanel({
  user,
}: {
  user: NonNullable<LoginResponse>;
}) {
  const teacherId = user.username;
  const defaultAcademy = user.academyNumbers?.[0] ?? null;

  // 내 반 목록
  const [items, setItems] = useState<CourseLite[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);

  // 반 생성 폼
  const [className, setClassName] = useState("");

  // 학생 검색/선택
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState<string>("");
  const [hits, setHits] = useState<
    Array<{ studentId: string; studentName?: string | null; grade?: number | null }>
  >([]);
  const [searching, setSearching] = useState(false);
  const [pickedStudents, setPickedStudents] = useState<string[]>([]);

  // 강의실(rooms) 목록 + 다중 선택
  const [rooms, setRooms] = useState<AdminRoomLite[]>([]);
  const [roomsErr, setRoomsErr] = useState<string | null>(null);
  const [pickedRooms, setPickedRooms] = useState<number[]>([]);

  // 모달 상태
  const [panelOpen, setPanelOpen] = useState(false);
  const [selected, setSelected] =
    useState<{ classId: string; roomNumber?: number | null } | null>(null);

  const loadClasses = async () => {
    if (!teacherId) {
      setErr("로그인이 필요합니다.");
      setLoading(false);
      return;
    }
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      const res = await api.listMyClasses(teacherId);
      setItems(res || []);
    } catch (e: any) {
      setErr(e?.message ?? "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const loadRooms = async () => {
    if (!defaultAcademy) return;
    setRoomsErr(null);
    try {
      const list = await roomsVectorApi.list(defaultAcademy);
      setRooms(Array.isArray(list) ? list : []);
    } catch (e: any) {
      setRoomsErr(e?.message ?? "강의실 목록 로드 실패");
      setRooms([]);
    }
  };

  useEffect(() => {
    void loadClasses();
  }, []);

  useEffect(() => {
    void loadRooms();
  }, [defaultAcademy]);

  // 학생 검색
  const search = async () => {
    if (!defaultAcademy) {
      setErr("학원번호가 없습니다.");
      return;
    }
    try {
      setSearching(true);
      setErr(null);
      const res = await api.searchStudents(
        defaultAcademy,
        q ?? "",
        grade ? Number(grade) : undefined
      );
      setHits(res);
    } catch (e: any) {
      setErr(e?.message ?? "학생 검색 실패");
    } finally {
      setSearching(false);
    }
  };

  const togglePickStudent = (sid: string) =>
    setPickedStudents((prev) =>
      prev.includes(sid) ? prev.filter((x) => x !== sid) : [...prev, sid]
    );

  const togglePickRoom = (roomNumber: number) =>
    setPickedRooms((prev) =>
      prev.includes(roomNumber)
        ? prev.filter((n) => n !== roomNumber)
        : [...prev, roomNumber]
    );

  // 반 생성
  const create = async () => {
    if (!defaultAcademy) {
      setErr("학원번호가 없습니다.");
      return;
    }
    if (!className.trim()) {
      setErr("반 이름을 입력하세요.");
      return;
    }
    try {
      setErr(null);
      setMsg(null);

      const body: any = {
        className: className.trim(),
        teacherId,
        academyNumber: defaultAcademy,
      };
      if (pickedRooms.length > 0) {
        body.roomNumbers = pickedRooms;
      }

      const created = await api.createClass(body as any);

      if (created?.classId && pickedStudents.length > 0) {
        for (const sid of pickedStudents) {
          await api.addStudentToClass(created.classId, sid);
        }
      }

      setClassName("");
      setQ("");
      setGrade("");
      setHits([]);
      setPickedStudents([]);
      setPickedRooms([]);

      await loadClasses();
      setMsg("반이 생성되었습니다.");
    } catch (e: any) {
      setErr(e?.message ?? "반 생성 실패");
    }
  };

  // 반 삭제
  const removeClass = async (classId: string) => {
    if (!confirm("이 반을 삭제할까요?")) return;
    try {
      setErr(null);
      setMsg(null);
      await request<void>(
        `/api/manage/teachers/classes/${encodeURIComponent(classId)}`,
        { method: "DELETE" }
      );
      await loadClasses();
      setMsg("삭제 완료");
    } catch (e: any) {
      setErr(e?.message ?? "삭제 실패");
    }
  };

  const sortedRooms = useMemo(
    () => [...rooms].sort((a, b) => a.roomNumber - b.roomNumber),
    [rooms]
  );

  // 목록 카드 클릭 → 같은 화면 모달 오픈
  const openClassPanel = (c: CourseLite) => {
    setSelected({
      classId: c.classId,
      roomNumber: (c as any).roomNumber ?? null,
    });
    setPanelOpen(true);
  };

  return (
    <div className="space-y-4">
      <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
        <h2 className="text-lg font-semibold text-black mb-3">내 반 관리</h2>

        {/* 생성 폼 */}
        <div className="bg-[#F6FBF8] border rounded-xl p-4 space-y-5">
          {/* 1) 반 이름 */}
          <div>
            <label className="block text-sm text-black">반 이름</label>
            <input
              value={className}
              onChange={(e) => setClassName(e.target.value)}
              className="border rounded px-3 py-2 w-full text-black"
              placeholder="예: 중1 영어 A반"
            />
          </div>

          {/* 2) 학생 검색/선택 */}
          <div className="space-y-2">
            <div className="text-sm font-medium text-black">학생 추가(선택)</div>
            <div className="flex flex-wrap gap-2">
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="이름 검색"
                className="border rounded px-3 py-2 text-black"
              />
              <input
                value={grade}
                onChange={(e) => setGrade(normalizeOneDigit(e.target.value))}
                onKeyDown={(e) => {
                  const allowed = ["Backspace","Delete","Tab","ArrowLeft","ArrowRight","Home","End"];
                  if (allowed.includes(e.key)) return;
                  if (!/^[0-9]$/.test(e.key)) { e.preventDefault(); return; }
                  if (grade.length >= 1) e.preventDefault();
                }}
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={1}
                placeholder="학년(한 자리)"
                className="border rounded px-3 py-2 w-32 text-black"
              />
              <button
                onClick={search}
                className="px-4 py-2 rounded bg-gray-900 text-white"
              >
                검색
              </button>
              {searching && (
                <span className="text-sm text-black">
                  <Spinner />
                </span>
              )}
            </div>

            {/* 검색 결과 */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
              {hits.map((h) => {
                const picked = pickedStudents.includes(h.studentId);
                return (
                  <button
                    key={h.studentId}
                    onClick={() => togglePickStudent(h.studentId)}
                    className={[
                      "text-left border rounded px-3 py-2 transition",
                      picked ? "bg-emerald-50 border-emerald-200" : "bg-white hover:bg-gray-50",
                    ].join(" ")}
                  >
                    <div className="font-medium text-black">
                      {h.studentName ?? h.studentId}
                      {picked && <span className="ml-2 text-emerald-600 text-xs">선택됨</span>}
                    </div>
                    <div className="text-xs text-black/70">
                      ID: {h.studentId} · 학년: {h.grade ?? "-"}
                    </div>
                  </button>
                );
              })}
              {hits.length === 0 && (
                <div className="text-sm text-black/80">검색 결과 없음</div>
              )}
            </div>

            {/* 선택된 학생 토큰 */}
            {pickedStudents.length > 0 && (
              <div className="flex flex-wrap gap-2 pt-1">
                {pickedStudents.map((sid) => (
                  <span
                    key={sid}
                    className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full text-sm bg-emerald-100 text-black ring-1 ring-emerald-200"
                  >
                    {sid}
                    <button
                      onClick={() => togglePickStudent(sid)}
                      className="text-emerald-700 hover:underline"
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* 3) 강의실 선택(여러 개) */}
          <div className="space-y-2">
            <div className="text-sm font-medium text-black">강의실 선택(여러 개)</div>
            {roomsErr && <div className="text-sm text-red-600">오류: {roomsErr}</div>}
            <div className="flex flex-wrap gap-2">
              {sortedRooms.map((r) => {
                const active = pickedRooms.includes(r.roomNumber);
                return (
                  <Chip
                    key={r.roomNumber}
                    active={active}
                    onClick={() => togglePickRoom(r.roomNumber)}
                  >
                    {r.roomNumber}
                  </Chip>
                );
              })}
              {sortedRooms.length === 0 && (
                <span className="text-sm text-black/80">
                  강의실이 없습니다. (원장에서 생성하세요)
                </span>
              )}
            </div>

            {/* 선택된 강의실 미리보기 */}
            {pickedRooms.length > 0 && (
              <div className="text-sm text-black">
                선택됨: {pickedRooms.sort((a, b) => a - b).join(", ")}
              </div>
            )}
          </div>

          {/* 액션 */}
          <div className="flex items-center gap-3 pt-1">
            <button
              onClick={create}
              className="bg-emerald-600 text-white px-4 py-2 rounded active:scale-[0.99]"
            >
              반 만들기
            </button>
            {msg && <span className="text-emerald-700">{msg}</span>}
            {err && <span className="text-red-600">{err}</span>}
          </div>
        </div>

        {/* 목록 */}
        {loading && <div className="mt-3 text-sm text-black/80">불러오는 중…</div>}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 mt-4">
          {items.map((c) => (
            <div
              key={c.classId}
              className="relative bg-white border rounded p-3 hover:shadow"
            >
              {/* 삭제 버튼 (X) */}
              <button
                title="삭제"
                onClick={() => removeClass(c.classId)}
                className="absolute right-2 top-2 text-black/60 hover:text-red-600"
              >
                ×
              </button>

              {/* 카드 클릭 → 가운데 모달 오픈 */}
              <button
                onClick={() => openClassPanel(c)}
                className="block text-left w-full"
              >
                <div className="font-semibold text-black">{c.className}</div>
                <div className="text-sm text-black/80">
                  Room {(c as any).roomNumber ?? "-"}
                </div>
                <div className="text-sm text-black/80">
                  학생 수: {c.students?.length ?? 0}
                </div>
                <div className="mt-2 text-sm">
                  <span className="text-emerald-700 hover:underline">학생 관리</span>{" "}
                  · <span className="text-black hover:underline">시간표</span>
                </div>
              </button>
            </div>
          ))}
          {!loading && items.length === 0 && (
            <div className="text-sm text-black/80">아직 생성된 반이 없습니다.</div>
          )}
        </div>
      </div>

      {/* ▣ 같은 화면에서 뜨는 가운데 모달 */}
      <CenterModal
        open={panelOpen}
        onOpenChange={setPanelOpen}
        title="반/강의실 상세"
        maxWidth={1120}  // 필요 시 960~1280 사이로 조절
      >
        <div className="p-4">
          {selected ? (
            <Suspense
              fallback={
                <div className="h-[60vh] flex items-center justify-center">
                  <Spinner />
                </div>
              }
            >
              <ClassDetailClient
                asPanel
                onClose={() => setPanelOpen(false)}
                classId={selected.classId}
                initialRoomNumber={selected.roomNumber ?? null}
              />
            </Suspense>
          ) : null}
        </div>
      </CenterModal>
    </div>
  );
}
