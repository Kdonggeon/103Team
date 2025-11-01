"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { api, type CourseLite, type StudentHit } from "@/app/lib/api";
import { getSession } from "@/app/lib/session";
import {
  roomsVectorApi,
  type VectorLayout,
  type VectorSeat,
} from "@/app/lib/rooms.vector";
import VectorSeatEditor from "@/components/rooms/vector/VectorSeatEditor";
import { generateAutoLayout } from "@/components/rooms/vector/generate";

/* ───────────────────── 유틸 ───────────────────── */
const pickRoomNumber = (o: any): number | undefined => {
  const rn =
    o?.roomNumber ??
    o?.Room_Number ??
    o?.number ??
    o?.room_no ??
    o?.RoomNo;
  const n = typeof rn === "string" ? parseInt(rn, 10) : Number(rn);
  return Number.isFinite(n) ? n : undefined;
};
const uniqSorted = (arr: number[]) =>
  Array.from(new Set(arr)).sort((a, b) => a - b);

// 에디터가 기대하는 좌석 구조로 보정
const normalizeSeat = (s: any, i: number): VectorSeat & { studentId?: string | null } => {
  const id = s?.id ?? s?.seatId ?? String(i + 1);
  const x = Number(s?.x ?? 0);
  const y = Number(s?.y ?? 0);
  const w = Number(s?.w ?? 40);
  const h = Number(s?.h ?? 40);
  const label = String(s?.label ?? s?.name ?? s?.seatNumber ?? s?.number ?? (i + 1));
  const disabled = Boolean(s?.disabled ?? s?.isDisabled ?? false);
  const studentId = s?.studentId ?? s?.student_id ?? s?.student ?? null;
  return { id, x, y, w, h, label, disabled, studentId } as VectorSeat & { studentId?: string | null };
};

// 다양한 응답 포맷을 표준 VectorLayout으로 통일
const normalizeLayout = (raw: any): VectorLayout & { seats: (VectorSeat & { studentId?: string | null })[] } => {
  const src = raw?.seats ? raw : raw?.layout ? raw.layout : raw?.data ? raw.data : raw ?? {};
  const version = Number(src?.version ?? 1);
  const canvasW = Number(src?.canvasW ?? 1000);
  const canvasH = Number(src?.canvasH ?? 700);
  const rawSeats = src?.seats ?? src?.vectorSeats ?? src?.layout?.seats ?? [];
  const seats = Array.isArray(rawSeats) ? rawSeats.map((s: any, i: number) => normalizeSeat(s, i)) : [];
  return { version, canvasW, canvasH, seats };
};

// 좌석 + 배정 확장 타입(내부 상태에서만 사용)
type SeatWithAssign = VectorSeat & { studentId?: string | null };
type LayoutWithAssign = Omit<VectorLayout, "seats"> & { seats: SeatWithAssign[] };

type CourseDetail = CourseLite & {
  roomNumbers?: number[]; // 배열 호환 필드
};

/* 클라이언트에서만 세션 접근 (SSR 안전) */
function getSessionClient() {
  if (typeof window === "undefined") return null;
  try { return getSession(); } catch { return null; }
}

export default function ClassDetailClient({ classId }: { classId: string }) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => { setMounted(true); }, []);

  const me = getSessionClient();
  const academyNumber = me?.academyNumbers?.[0] ?? null;

  /* ===== 반 기본 정보 ===== */
  const [data, setData] = useState<CourseDetail | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  /* ===== 반 정보 수정 필드 ===== */
  const [newName, setNewName] = useState("");
  const [selectedRoomNumbers, setSelectedRoomNumbers] = useState<number[]>([]);
  const [editingRoom, setEditingRoom] = useState<number | null>(null);

  /* ===== 학생 검색/추가 ===== */
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState<string>("");
  const [hits, setHits] = useState<StudentHit[]>([]);
  const [searching, setSearching] = useState(false);

  /* ===== 학원 방 목록 ===== */
  const [allRooms, setAllRooms] = useState<number[]>([]);
  const [roomsInfo, setRoomsInfo] = useState<string>("");

  /* ===== 좌석 레이아웃(편집 대상 방) ===== */
  const [layout, setLayout] = useState<LayoutWithAssign | null>(null);
  const [selectedSeatIndex, setSelectedSeatIndex] = useState<number | null>(null);
  const [selectedStudentId, setSelectedStudentId] = useState<string | null>(null);

  const selectedSeat = useMemo(
    () =>
      selectedSeatIndex != null && layout
        ? layout.seats[selectedSeatIndex] ?? null
        : null,
    [selectedSeatIndex, layout]
  );

  /* ───────── 데이터 로드 ───────── */
  const loadClass = async () => {
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      const c = await api.getClassDetail(classId);
      const d = c as CourseDetail;
      setData(d);

      setNewName(d.className || "");
      const rooms =
        Array.isArray(d.roomNumbers) && d.roomNumbers.length
          ? d.roomNumbers
          : d.roomNumber != null
          ? [Number(d.roomNumber)]
          : [];
      setSelectedRoomNumbers(rooms);
      setEditingRoom((r) => rooms[0] ?? r ?? null);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  const loadRooms = async () => {
    if (!academyNumber) return;
    try {
      const vlist: any[] = await roomsVectorApi.list(academyNumber);
      const nums = (vlist || [])
        .map(pickRoomNumber)
        .filter((n): n is number => n !== undefined);
      const opts = uniqSorted(nums);
      setAllRooms(opts);
      setRoomsInfo(`방 ${opts.length}개`);
    } catch (e: any) {
      setRoomsInfo(`방 목록 로드 실패: ${e?.message ?? ""}`);
      setAllRooms([]);
    }
  };

  const loadLayout = async (room: number | null) => {
    if (!academyNumber || !room) {
      setLayout(null);
      return;
    }
    try {
      const raw = await roomsVectorApi.get(room, academyNumber);
      const norm = normalizeLayout(raw);
      if (!norm.seats?.length) {
        setLayout(generateAutoLayout(30) as unknown as LayoutWithAssign);
        setMsg("저장된 좌석이 없어 초기 배치를 생성했습니다.");
      } else {
        setLayout(norm as LayoutWithAssign);
      }
    } catch {
      setLayout(generateAutoLayout(30) as unknown as LayoutWithAssign);
      setMsg("레이아웃 불러오기 실패 — 초기 배치로 대체.");
    } finally {
      setSelectedSeatIndex(null);
    }
  };

  useEffect(() => {
    if (!mounted) return;
    if (!me) {
      // 클라이언트 전용 리다이렉트
      if (typeof window !== "undefined") location.href = "/login";
      return;
    }
    loadClass();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId, mounted]);

  useEffect(() => { if (mounted) loadRooms(); /* eslint-disable-next-line */ }, [academyNumber, mounted]);
  useEffect(() => { if (mounted) loadLayout(editingRoom); /* eslint-disable-next-line */ }, [academyNumber, editingRoom, mounted]);

  /* ───────── 학생 검색/추가/삭제 ───────── */
  const search = async () => {
    if (!academyNumber) { setErr("학원번호 없음"); return; }
    try {
      setSearching(true);
      setErr(null);
      const res = await api.searchStudents(academyNumber, q, grade ? Number(grade) : undefined);
      setHits(res);
    } catch (e: any) { setErr(e.message); }
    finally { setSearching(false); }
  };

  const addStudent = async (sid: string) => {
    try {
      setErr(null); setMsg(null);
      await api.addStudentToClass(classId, sid);
      await loadClass();
      setMsg("학생이 추가되었습니다.");
    } catch (e: any) { setErr(e.message); }
  };

  const removeStudent = async (sid: string) => {
    try {
      setErr(null); setMsg(null);
      await api.removeStudentFromClass(classId, sid);
      await loadClass();
      setMsg("학생이 제거되었습니다.");
    } catch (e: any) { setErr(e.message); }
  };

  /* ───────── 반 정보 저장 (이름 + 방 목록) ───────── */
  const saveClassInfo = async () => {
    try {
      setErr(null); setMsg(null);
      const rooms = selectedRoomNumbers;
      await api.patchClass(classId, {
        className: newName || undefined,
        roomNumber: rooms.length ? rooms[0] : undefined, // 호환
        roomNumbers: rooms.length ? rooms : undefined,   // 배열 저장(백엔드 확장 시 사용)
      } as any);
      await loadClass();
      setMsg("반 정보가 저장되었습니다.");
    } catch (e: any) { setErr(e.message); }
  };

  /* ───────── 좌석 편집 & 배정 ───────── */
  const mergeSeats = (base: SeatWithAssign[], incoming: VectorSeat[]): SeatWithAssign[] => {
    const byKey = (s: any) => String(s.id ?? s.label ?? "");
    const baseMap = new Map(base.map(s => [byKey(s), s]));
    return incoming.map(s => {
      const k = byKey(s);
      const prev = baseMap.get(k);
      return { ...(s as SeatWithAssign), studentId: prev?.studentId ?? null };
    });
  };

  useEffect(() => {
    const prevent = (e: MouseEvent) => {
      if ((e.target as HTMLElement)?.closest?.("[data-seat-editor]")) e.preventDefault();
    };
    document.addEventListener("contextmenu", prevent);
    return () => document.removeEventListener("contextmenu", prevent);
  }, []);

  const onSeatClick = (index: number) => setSelectedSeatIndex(index);

  const toggleSeatDisabled = () => {
    if (selectedSeatIndex == null || !layout) return;
    const next = [...layout.seats];
    next[selectedSeatIndex] = { ...next[selectedSeatIndex], disabled: !next[selectedSeatIndex]?.disabled };
    setLayout({ ...layout, seats: next });
  };

  const assignStudentToSeat = () => {
    if (!layout || selectedSeatIndex == null) return;
    const next = [...layout.seats];
    next[selectedSeatIndex] = { ...next[selectedSeatIndex], studentId: selectedStudentId ?? null };
    setLayout({ ...layout, seats: next });
  };

  const unassignSeat = () => {
    if (!layout || selectedSeatIndex == null) return;
    const next = [...layout.seats];
    next[selectedSeatIndex] = { ...next[selectedSeatIndex], studentId: null };
    setLayout({ ...layout, seats: next });
  };

  const saveLayout = async () => {
    if (!academyNumber || !editingRoom || !layout) return;
    try {
      setErr(null); setMsg(null);
      const payload: any = {
        version: Number(layout.version ?? 1),
        canvasW: Number(layout.canvasW ?? 1000),
        canvasH: Number(layout.canvasH ?? 700),
        seats: layout.seats, // studentId 포함 가능 (백엔드가 수용하면 저장됨)
      };
      await roomsVectorApi.put(editingRoom, academyNumber, payload as any);
      setMsg(`Room #${editingRoom} 좌석 배치를 저장했습니다.`);
    } catch (e: any) { setErr(e?.message ?? "좌석 저장 실패"); }
  };

  /* ───── 렌더 ───── */
  // ✅ SSR 시에는 아무것도 렌더하지 않음 → Hydration mismatch 방지
  if (!mounted) return <div className="p-6 text-gray-900">불러오는 중…</div>;
  if (!me) return null;

  if (loading) return <div className="p-6 text-gray-900">불러오는 중…</div>;
  if (err) return <div className="p-6 text-red-600">{err}</div>;
  if (!data) return <div className="p-6 text-gray-900">데이터가 없습니다.</div>;

  const currentStudents = data.students ?? [];
  const roomChecked = (n: number) => selectedRoomNumbers.includes(n);
  const toggleRoom = (n: number) =>
    setSelectedRoomNumbers((prev) => (prev.includes(n) ? prev.filter((x) => x !== n) : [...prev, n]));

  return (
    <div className="max-w-7xl mx-auto px-6 py-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-end gap-3">
          <h1 className="text-2xl font-bold text-gray-900">{data.className}</h1>
          <span className="text-gray-900">({data.classId})</span>
        </div>
        <Link href="/" className="text-sm text-emerald-700 hover:underline">← 대시보드로</Link>
      </div>

      {/* 반 정보 (이름 + 방 선택 다중) */}
      <div className="bg-white border border-black rounded-2xl p-5 space-y-3">
        <div className="text-lg font-semibold text-gray-900">반 정보</div>
        <div className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm text-gray-900">반 이름</label>
            <input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="border border-black rounded px-2 py-1 w-full text-gray-900"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-900">강의실 선택(여러 개)</label>
            <div className="border border-black rounded px-2 py-2">
              {allRooms.length === 0 ? (
                <div className="text-sm text-gray-900">{roomsInfo || "방 없음"}</div>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {allRooms.map((n) => (
                    <label
                      key={n}
                      className={`px-3 py-1 rounded-full border cursor-pointer ${
                        roomChecked(n) ? "bg-emerald-100 border-emerald-300" : "bg-white"
                      }`}
                    >
                      <input
                        type="checkbox"
                        className="hidden"
                        checked={roomChecked(n)}
                        onChange={() => toggleRoom(n)}
                      />
                      Room {n}
                    </label>
                  ))}
                </div>
              )}
            </div>
            <div className="text-xs text-gray-900 mt-1">{roomsInfo}</div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button onClick={saveClassInfo} className="bg-emerald-600 text-white px-3 py-2 rounded">반 정보 저장</button>
          {msg && <span className="text-emerald-700">{msg}</span>}
          {err && <span className="text-red-600">{err}</span>}
        </div>
      </div>

      {/* 좌석 편집(캔버스) */}
      <div className="bg-white border border-black rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between">
          <div className="text-lg font-semibold text-gray-900">좌석 배치 / 배정</div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-900">편집할 방</span>
            <select
              value={editingRoom ?? ""}
              onChange={(e) => setEditingRoom(e.target.value ? Number(e.target.value) : null)}
              className="border border-black rounded px-2 py-1 text-gray-900 bg-white"
            >
              <option value="">선택…</option>
              {selectedRoomNumbers.map((n) => (
                <option key={n} value={n}>Room {n}</option>
              ))}
            </select>
            <button
              onClick={saveLayout}
              disabled={!layout || !editingRoom}
              className="bg-gray-900 text-white px-3 py-2 rounded disabled:opacity-50"
            >
              좌석 저장
            </button>
          </div>
        </div>

        {!editingRoom ? (
          <div className="text-sm text-gray-900">위에서 편집할 방을 선택하세요.</div>
        ) : !layout ? (
          <div className="text-sm text-gray-900">레이아웃 로딩 중…</div>
        ) : (
          <>
            <div
              data-seat-editor
              className="border border-black rounded-xl p-3 bg-white"
              onClickCapture={(e) => {
                const el = (e.target as HTMLElement).closest("[data-seat-index]") as HTMLElement | null;
                if (el) {
                  const idx = Number(el.dataset.seatIndex);
                  if (Number.isFinite(idx)) setSelectedSeatIndex(idx);
                }
              }}
            >
              <VectorSeatEditor
                value={layout.seats as VectorSeat[]}
                onChange={(seats) =>
                  setLayout(prev => prev ? { ...prev, seats: mergeSeats(prev.seats, seats) } : null)
                }
              />
            </div>

            {/* 배정 패널 */}
            <div className="grid md:grid-cols-[1fr_1fr] gap-3">
              <div className="border border-black rounded p-3">
                <div className="font-semibold text-gray-900 mb-2">선택한 좌석</div>
                {selectedSeat ? (
                  <div className="space-y-2 text-gray-900">
                    <div>Seat: {selectedSeat.label}</div>
                    <div>상태: {selectedSeat.disabled ? "비활성" : "활성"}</div>
                    <div>배정 학생: {selectedSeat.studentId ?? "없음"}</div>
                    <div className="flex gap-2">
                      <button onClick={toggleSeatDisabled} className="px-3 py-1.5 rounded bg-gray-900 text-white">
                        {selectedSeat.disabled ? "활성화" : "비활성화"}
                      </button>
                      <button onClick={unassignSeat} className="px-3 py-1.5 rounded bg-red-600 text-white">
                        배정 해제
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="text-sm text-gray-900">좌석을 클릭해 선택하세요.</div>
                )}
              </div>

              <div className="border border-black rounded p-3">
                <div className="font-semibold text-gray-900 mb-2">학생 배정</div>
                <div className="flex items-center gap-2">
                  <select
                    value={selectedStudentId ?? ""}
                    onChange={(e) => setSelectedStudentId(e.target.value || null)}
                    className="border border-black rounded px-2 py-1 text-gray-900 bg-white"
                  >
                    <option value="">학생 선택…</option>
                    {currentStudents.map((sid) => (
                      <option key={sid} value={sid}>{sid}</option>
                    ))}
                  </select>
                  <button
                    onClick={assignStudentToSeat}
                    disabled={selectedSeatIndex == null || !selectedStudentId}
                    className="px-3 py-1.5 rounded bg-emerald-600 text-white disabled:opacity-50"
                  >
                    배정
                  </button>
                </div>
                <div className="text-xs text-gray-900 mt-2">
                  * 좌클릭으로 좌석 선택 / 우클릭으로 좌석 다중선택 / Delete로 통로처리 on/off 전환
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      {/* 현재 학생 목록 */}
      <div className="bg-white border border-black rounded-2xl p-5">
        <div className="text-lg font-semibold mb-2 text-gray-900">등록 학생 ({currentStudents.length})</div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
          {currentStudents.map((sid) => (
            <div key={sid} className="border border-black rounded px-3 py-2 flex items-center justify-between">
              <span className="text-gray-900">{sid}</span>
              <button onClick={() => removeStudent(sid)} className="text-red-600 text-sm hover:underline">삭제</button>
            </div>
          ))}
          {currentStudents.length === 0 && <div className="text-gray-900">아직 학생이 없습니다.</div>}
        </div>
      </div>

      {/* 학생 검색/추가 */}
      <div className="bg-white border border-black rounded-2xl p-5 space-y-3">
        <div className="text-lg font-semibold text-gray-900">학생 검색</div>
        <div className="flex flex-wrap gap-2">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="이름 검색"
                 className="border border-black rounded px-2 py-1 text-gray-900" />
          <input value={grade} onChange={(e) => setGrade(e.target.value)} placeholder="학년(선택)"
                 className="border border-black rounded px-2 py-1 w-32 text-gray-900" />
          <button onClick={search} className="bg-gray-900 text-white px-3 py-1 rounded">검색</button>
          {searching && <span className="text-gray-900 text-sm">검색중…</span>}
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
          {hits.map((h) => (
            <div key={h.studentId} className="border border-black rounded px-3 py-2 flex items-center justify-between">
              <div>
                <div className="font-medium text-gray-900">{h.studentName ?? h.studentId}</div>
                <div className="text-xs text-gray-900">ID: {h.studentId} · 학년: {h.grade ?? "-"}</div>
              </div>
              <button onClick={() => addStudent(h.studentId)} className="text-emerald-700 text-sm hover:underline">추가</button>
            </div>
          ))}
          {hits.length === 0 && <div className="text-gray-900">검색 결과 없음</div>}
        </div>
      </div>
    </div>
  );
}
