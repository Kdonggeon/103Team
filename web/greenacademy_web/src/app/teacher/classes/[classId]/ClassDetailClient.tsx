// src/app/teacher/classes/[classId]/ClassDetailClient.tsx
"use client";

import { useEffect, useMemo, useRef, useState } from "react";
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

/** 다양한 백엔드 필드를 모두 커버해 roomNumber 추출 */
const pickRoomNumber = (o: any): number | undefined => {
  const rn =
    o?.roomNumber ??
    o?.Room_Number ??
    o?.number ??
    o?.room_no ??
    o?.RoomNo ??
    o?.RoomNumber ??
    o?.ROOM_NUMBER;

  const n = typeof rn === "string" ? parseInt(rn, 10) : Number(rn);
  return Number.isFinite(n) ? n : undefined;
};

/** 숫자 정렬 + 중복 제거 */
const uniqSorted = (arr: number[]) =>
  Array.from(new Set(arr)).sort((a, b) => a - b);

/** 좌석 1개 정규화: 모든 가능한 필드를 허용 */
const normalizeSeat = (
  s: any,
  i: number
): VectorSeat & { studentId?: string | null } => {
  const id =
    s?.id ??
    s?._id ??
    s?.seatId ??
    s?.seat_id ??
    s?.label ??
    s?.name ??
    s?.number ??
    s?.seatNumber ??
    String(i + 1);

  const x = Number(s?.x ?? s?.posX ?? 0);
  const y = Number(s?.y ?? s?.posY ?? 0);
  const w = Number(s?.w ?? s?.width ?? 40);
  const h = Number(s?.h ?? s?.height ?? 40);

  const label =
    s?.label ??
    s?.seatNumber ??
    s?.number ??
    s?.name ??
    s?.id ??
    s?._id ??
    String(i + 1);

  const disabled = Boolean(
    s?.disabled ?? s?.isDisabled ?? s?.is_disabled ?? false
  );

  return {
    id: String(id),
    label: String(label),
    x,
    y,
    w,
    h,
    disabled,
    studentId: null,
  };
};

/** 좌석 JSON 구조를 전부 수용하는 강력한 normalizeLayout */
const normalizeLayout = (
  raw: any
): VectorLayout & { seats: (VectorSeat & { studentId?: string | null })[] } => {
  if (!raw) return { version: 1, canvasW: 1000, canvasH: 700, seats: [] };

  // 1) vectorLayoutV2 (새 버전)
  if (Array.isArray(raw?.vectorLayoutV2)) {
    const version = Number(raw?.vectorVersion ?? 1);
    const canvasW = Number(raw?.vectorCanvasW ?? raw?.canvasW ?? 1000);
    const canvasH = Number(raw?.vectorCanvasH ?? raw?.canvasH ?? 700);
    const seats = raw.vectorLayoutV2.map((s: any, i: number) =>
      normalizeSeat(s, i)
    );
    return { version, canvasW, canvasH, seats };
  }

  // 2) seats / vectorSeats / layout.seats 등 모든 fallback
  const src =
    raw?.seats
      ? raw
      : raw?.layout?.seats
      ? raw.layout
      : raw?.vectorSeats
      ? { seats: raw.vectorSeats, ...raw }
      : raw?.data?.seats
      ? { seats: raw.data.seats, ...raw }
      : raw ?? {};

  const version = Number(
    raw?.version ?? raw?.vectorVersion ?? src?.version ?? 1
  );
  const canvasW = Number(
    raw?.canvasW ??
      raw?.vectorCanvasW ??
      src?.canvasW ??
      raw?.width ??
      1000
  );
  const canvasH = Number(
    raw?.canvasH ??
      raw?.vectorCanvasH ??
      src?.canvasH ??
      raw?.height ??
      700
  );

  const rawSeats =
    src?.seats ??
    src?.vectorSeats ??
    src?.layout?.seats ??
    src?.data?.seats ??
    [];

  const seats = Array.isArray(rawSeats)
    ? rawSeats.map((s: any, i: number) => normalizeSeat(s, i))
    : [];

  return { version, canvasW, canvasH, seats };
};

/* 타입 */
type SeatWithAssign = VectorSeat & { studentId?: string | null };
type LayoutWithAssign = Omit<VectorLayout, "seats"> & {
  seats: SeatWithAssign[];
};

type CourseDetail = CourseLite & {
  roomNumbers?: number[];
};

/** 세션 복구 */
function getSessionClient() {
  if (typeof window === "undefined") return null;
  try {
    return getSession();
  } catch {
    return null;
  }
}
/** 패널 모드 지원 옵션 */
export default function ClassDetailClient({
  classId,
  asPanel = false,
  onClose,
  initialRoomNumber = null,
}: {
  classId: string;
  asPanel?: boolean;
  onClose?: () => void;
  initialRoomNumber?: number | null;
}) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  const me = getSessionClient();
  const academyNumber = me?.academyNumbers?.[0] ?? null;

  /* ========== 반 기본 정보 ========== */
  const [data, setData] = useState<CourseDetail | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  /* ========== 반 정보 수정 상태값 ========== */
  const [newName, setNewName] = useState("");
  const [selectedRoomNumbers, setSelectedRoomNumbers] = useState<number[]>([]);
  const [editingRoom, setEditingRoom] = useState<number | null>(initialRoomNumber);

  /* ========== 학생 검색/추가 ========== */
  const [q, setQ] = useState("");
  const [grade, setGrade] = useState<string>("");
  const [hits, setHits] = useState<StudentHit[]>([]);
  const [searching, setSearching] = useState(false);

  /* ========== 학원 모든 방 목록 ========== */
  const [allRooms, setAllRooms] = useState<number[]>([]);
  const [roomsInfo, setRoomsInfo] = useState<string>("");

  /* ========== 좌석 레이아웃 ========== */
  const [layout, setLayout] = useState<LayoutWithAssign | null>(null);
  const [selectedSeatIndex, setSelectedSeatIndex] = useState<number | null>(null);
  const selectedSeat = useMemo(
    () =>
      selectedSeatIndex != null && layout
        ? layout.seats[selectedSeatIndex] ?? null
        : null,
    [selectedSeatIndex, layout]
  );

  const [seatNumInput, setSeatNumInput] = useState<string>("");
  const [selectedStudentId, setSelectedStudentId] = useState<string | null>(null);

  const [nameMap, setNameMap] = useState<Map<string, string>>(new Map());
  const displayName = (sid: string) => nameMap.get(sid) ?? sid;

  /* ========== 자동 저장 디바운스 ========== */
  const AUTO_SAVE_MS = 600;
  const saveRef = useRef<{ t: any; dirty: boolean }>({ t: null, dirty: false });

  const doSaveNow = async () => {
    if (!academyNumber || !editingRoom || !layout) return;

    const seatsV2 = layout.seats.map((s) => ({
      _id: String((s as any)._id ?? s.id ?? s.label ?? ""),
      label: String(s.label ?? ""),
      x: Number(s.x ?? 0),
      y: Number(s.y ?? 0),
      w: Number(s.w ?? 40),
      h: Number(s.h ?? 40),
      disabled: Boolean(s.disabled ?? false),
    }));

    const body = {
      vectorVersion: Number(layout.version ?? 1),
      vectorCanvasW: Number(layout.canvasW ?? 1000),
      vectorCanvasH: Number(layout.canvasH ?? 700),
      vectorLayoutV2: seatsV2,
      version: Number(layout.version ?? 1),
      canvasW: Number(layout.canvasW ?? 1000),
      canvasH: Number(layout.canvasH ?? 700),
      seats: seatsV2.map((s) => ({
        id: s._id,
        label: s.label,
        x: s.x,
        y: s.y,
        w: s.w,
        h: s.h,
        disabled: s.disabled,
      })),
    };

    try {
      await roomsVectorApi.put(editingRoom, academyNumber, body as any);
      const fresh = await roomsVectorApi.get(editingRoom, academyNumber);
      const norm = normalizeLayout(fresh) as LayoutWithAssign;

      try {
        const seatMap = await api.getSeatMap(classId, editingRoom);
        const assigned = new Map<string, string>(Object.entries(seatMap.map || {}));
        const merged = norm.seats.map((s) => ({
          ...s,
          studentId: assigned.get(String(s.label)) ?? null,
        }));
        setLayout({ ...norm, seats: merged });
      } catch {
        setLayout(norm);
      }
      setMsg("자동 저장됨");
    } catch (e: any) {
      setErr(e?.message ?? "좌석 저장 실패");
    } finally {
      saveRef.current.dirty = false;
    }
  };

  const scheduleAutoSave = () => {
    saveRef.current.dirty = true;
    if (saveRef.current.t) clearTimeout(saveRef.current.t);
    saveRef.current.t = setTimeout(() => void doSaveNow(), AUTO_SAVE_MS);
  };

  /* ========== 학생 → 좌석 매핑 ========== */
  const studentToSeatLabel = useMemo(() => {
    const map = new Map<string, string>();
    if (layout?.seats) {
      for (const s of layout.seats)
        if (s.studentId) map.set(String(s.studentId), String(s.label));
    }
    return map;
  }, [layout]);

  /* ─────────── DATA LOAD ─────────── */

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

      setEditingRoom((r) => initialRoomNumber ?? rooms[0] ?? r ?? null);
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
      const norm = normalizeLayout(raw) as LayoutWithAssign;

      try {
        const seatMap = await api.getSeatMap(classId, room);
        const assigned = new Map<string, string>(Object.entries(seatMap.map || {}));
        const merged = norm.seats.map((s) => ({
          ...s,
          studentId: assigned.get(String(s.label)) ?? null,
        }));
        setLayout({ ...norm, seats: merged });
      } catch {
        setLayout(norm);
      }

      if (!norm.seats?.length) {
        setLayout(generateAutoLayout(30) as unknown as LayoutWithAssign);
        setMsg("저장된 좌석이 없어 초기 배치를 생성했습니다.");
      }
    } catch (e) {
      setLayout(generateAutoLayout(30) as unknown as LayoutWithAssign);
      setMsg("레이아웃 불러오기 실패 — 초기 배치로 대체.");
    } finally {
      setSelectedSeatIndex(null);
    }
  };

  useEffect(() => {
    if (!mounted) return;
    if (!me) {
      if (typeof window !== "undefined") location.href = "/login";
      return;
    }
    loadClass();
  }, [classId, mounted]);

  useEffect(() => {
    if (mounted) loadRooms();
  }, [academyNumber, mounted]);

  useEffect(() => {
    if (mounted) loadLayout(editingRoom);
  }, [academyNumber, editingRoom, mounted]);

  /* ───────── 학생 검색/추가/삭제 ───────── */

  const filterHits = (src: StudentHit[], q: string) => {
    const qq = q.trim();
    if (!qq) return src;
    const isNum = /^[0-9]+$/.test(qq);
    if (isNum) return src.filter((h) => (h.studentId ?? "").includes(qq));
    const low = qq.toLowerCase();
    return src.filter((h) => (h.studentName ?? "").toLowerCase().includes(low));
  };

  const search = async () => {
    if (!academyNumber) {
      setErr("학원번호 없음");
      return;
    }
    try {
      setSearching(true);
      setErr(null);
      const res = await api.searchStudents(
        academyNumber,
        q,
        grade ? Number(grade) : undefined
      );
      const filtered = filterHits(res, q);
      setHits(filtered);

      setNameMap((prev) => {
        const next = new Map(prev);
        for (const h of filtered)
          if (h.studentId) next.set(h.studentId, h.studentName ?? h.studentId);
        return next;
      });
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setSearching(false);
    }
  };

  const addStudent = async (sid: string) => {
    try {
      setErr(null);
      setMsg(null);
      await api.addStudentToClass(classId, sid);
      await loadClass();
      setMsg("학생이 추가되었습니다.");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const removeStudent = async (sid: string) => {
    try {
      setErr(null);
      setMsg(null);
      const roster = data?.students ?? [];
      if (roster.includes(sid)) {
        await api.removeStudentFromClass(classId, sid);
        await loadClass();
      }

      if (layout?.seats && editingRoom) {
        const idx = layout.seats.findIndex((s) => s.studentId === sid);
        if (idx >= 0) {
          const next = layout.seats.map((s, i) =>
            i === idx ? { ...s, studentId: null } : s
          );
          setLayout({ ...(layout as LayoutWithAssign), seats: next });
        }
        await api.assignSeat(classId, {
          roomNumber: editingRoom,
          seatLabel:
            (layout?.seats[idx]?.label ??
              String(idx + 1)) as string,
          studentId: null,
        });
      }
      setMsg("처리되었습니다.");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  /* ───────── 좌석 편집/배정 ───────── */

  const mergeSeats = (base: SeatWithAssign[], incoming: VectorSeat[]) => {
    const key = (s: any) =>
      String(s.id ?? s._id ?? s.label ?? s.seatNumber ?? "");
    const baseMap = new Map(base.map((s) => [key(s), s]));
    return incoming.map((s) => {
      const k = key(s);
      const prev = baseMap.get(k);
      return { ...(s as SeatWithAssign), studentId: prev?.studentId ?? null };
    });
  };

  useEffect(() => {
    const prevent = (e: MouseEvent) => {
      if ((e.target as HTMLElement)?.closest?.("[data-seat-editor]"))
        e.preventDefault();
    };
    document.addEventListener("contextmenu", prevent);
    return () => document.removeEventListener("contextmenu", prevent);
  }, []);

  const onSeatClick = (index: number) => {
    setSelectedSeatIndex(index);
    const label = layout?.seats[index]?.label;
    if (label) setSeatNumInput(String(label));
  };

  const toggleSeatDisabled = () => {
    if (selectedSeatIndex == null || !layout) return;
    const next = [...layout.seats];
    next[selectedSeatIndex] = {
      ...next[selectedSeatIndex],
      disabled: !next[selectedSeatIndex].disabled,
    };
    setLayout({ ...layout, seats: next });
    scheduleAutoSave();
  };

  const assignSidToIndex = async (sid: string, targetIdx: number) => {
    if (!layout || editingRoom == null) return;
    const next = layout.seats.map((s, i) => {
      if (i === targetIdx) return { ...s, studentId: sid };
      if (s.studentId === sid) return { ...s, studentId: null };
      return s;
    });
    setLayout({ ...layout, seats: next });
    setMsg(
      `좌석 ${next[targetIdx].label}에 ${displayName(sid)} 배정 완료`
    );

    try {
      await api.assignSeat(classId, {
        roomNumber: editingRoom,
        seatLabel: String(next[targetIdx].label),
        studentId: sid,
      });
    } catch (e: any) {
      setErr(e?.message ?? "배정 저장 실패");
    }
    scheduleAutoSave();
  };

  const maxSeatCount = layout?.seats?.length ?? 0;
  const normalizeSeatNumberText = (text: string) => {
    const digits = text.replace(/\D+/g, "");
    if (!digits) return "";
    let n = parseInt(digits, 10);
    if (!Number.isFinite(n) || n <= 0) return "";
    if (maxSeatCount > 0 && n > maxSeatCount) n = maxSeatCount;
    return String(n);
  };

  const assignByInputs = () => {
    if (!layout) {
      setErr("레이아웃이 없습니다.");
      return;
    }
    let idx =
      selectedSeatIndex != null
        ? selectedSeatIndex
        : -1;
    if (idx < 0 && seatNumInput.trim()) {
      idx = layout.seats.findIndex(
        (s) => String(s.label) === String(seatNumInput.trim())
      );
    }
    if (idx < 0) {
      setErr("좌석을 선택하거나 좌석번호를 입력하세요.");
      return;
    }
    const sid = (selectedStudentId ?? "").trim();
    if (!sid) {
      setErr("학생을 선택하세요.");
      return;
    }
    void assignSidToIndex(sid, idx);
  };

  const seatSort = (a: SeatWithAssign, b: SeatWithAssign) => {
    const an = Number(a.label),
      bn = Number(b.label);
    const aNum = Number.isFinite(an),
      bNum = Number.isFinite(bn);
    if (aNum && bNum) return an - bn;
    return String(a.label).localeCompare(String(b.label), "ko");
  };

  const currentRosterIds = useMemo(
    () => data?.students ?? [],
    [data]
  );
  const assignedIds = useMemo(() => {
    if (!layout?.seats) return [];
    return Array.from(
      new Set(layout.seats.map((s) => s.studentId).filter(Boolean))
    ) as string[];
  }, [layout]);

  const currentStudents = useMemo(() => {
    const set = new Set<string>(currentRosterIds);
    for (const sid of assignedIds) set.add(sid);
    return Array.from(set);
  }, [currentRosterIds, assignedIds]);

  const resolveStudentNames = async (academyNo: number, ids: string[]) => {
    const unique = Array.from(new Set(ids.filter(Boolean)));
    if (unique.length === 0) return;
    try {
      const pairs: Array<[string, string]> = [];
      await Promise.all(
        unique.map(async (sid) => {
          try {
            const res: StudentHit[] = await api.searchStudents(
              academyNo,
              sid,
              undefined
            );
            const hit = res.find((r) => r.studentId === sid) ?? res[0];
            if (hit?.studentId)
              pairs.push([
                hit.studentId,
                hit.studentName ?? hit.studentId,
              ]);
          } catch {}
        })
      );

      setNameMap((prev) => {
        const next = new Map(prev);
        for (const [id, name] of pairs) next.set(id, name);
        return next;
      });
    } catch {}
  };

  useEffect(() => {
    if (!academyNumber) return;
    const unknown = currentStudents.filter((id) => !nameMap.has(id));
    if (unknown.length > 0)
      void resolveStudentNames(academyNumber, unknown);
  }, [academyNumber, currentStudents.join("|")]);
  /* ───── 렌더 ───── */
  if (!mounted) return <div className="p-6 text-gray-900">불러오는 중…</div>;
  if (!me) return null;
  if (loading) return <div className="p-6 text-gray-900">불러오는 중…</div>;
  if (err) return <div className="p-6 text-red-600">{err}</div>;
  if (!data) return <div className="p-6 text-gray-900">데이터가 없습니다.</div>;

  const roomChecked = (n: number) => selectedRoomNumbers.includes(n);
  const toggleRoom = (n: number) =>
    setSelectedRoomNumbers((prev) =>
      prev.includes(n) ? prev.filter((x) => x !== n) : [...prev, n]
    );

  const wrapperClass = asPanel ? "h-full flex flex-col" : "";
  const scrollBodyClass = asPanel ? "flex-1 overflow-y-auto" : "";

  return (
    <div className={wrapperClass}>
      {/* ▶ 헤더 */}
      <div className="bg-white rounded-2xl ring-1 ring-black/5 shadow-sm px-5 py-4 flex items-center justify-between">
        <div className="flex items-end gap-3">
          {/* 🔹 classId 표시는 제거 */}
          <h1 className="text-2xl font-bold text-gray-900">{data.className}</h1>
        </div>
        {asPanel ? (
          <button
            onClick={onClose}
            className="inline-flex items-center gap-1 h-10 px-3 rounded-xl border border-gray-300 text-gray-900 hover:bg-gray-50"
            aria-label="닫기"
          >
            ✕ 닫기
          </button>
        ) : (
          <Link
            href="/"
            className="inline-flex items-center gap-1 h-10 px-3 rounded-xl border border-gray-300 text-gray-900 hover:bg-gray-50"
          >
            ← 대시보드로
          </Link>
        )}
      </div>

      {/* ▶ 본문 */}
      <div className={`max-w-7xl mx-auto px-6 py-6 space-y-6 ${scrollBodyClass}`}>
        {/* 반 정보 */}
        <div className="bg-white border border-black rounded-2xl p-5 space-y-3">
          <div className="text-lg font-semibold text-gray-900">반 정보</div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-sm text-gray-900">반 이름</label>
              <input
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                className="border border-black rounded px-2 py-1 w-full text-gray-900 bg-white"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-900">강의실 선택(여러 개)</label>
              <div className="border border-black rounded px-2 py-2 bg-white">
                {allRooms.length === 0 ? (
                  <div className="text-sm text-gray-900">
                    {roomsInfo || "방 없음"}
                  </div>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {allRooms.map((n) => {
                      const checked = roomChecked(n);
                      return (
                        <label
                          key={n}
                          className={`px-3 py-1 rounded-full border cursor-pointer transition ${
                            checked
                              ? "bg-black text-white border-black"
                              : "bg-white text-gray-900 border-black/60 hover:border-black"
                          }`}
                        >
                          <input
                            type="checkbox"
                            className="hidden"
                            checked={checked}
                            onChange={() => toggleRoom(n)}
                          />
                          Room {n}
                        </label>
                      );
                    })}
                  </div>
                )}
              </div>
              <div className="text-xs text-gray-700 mt-1">{roomsInfo}</div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={async () => {
                await api.patchClass(classId, {
                  className: newName || undefined,
                  roomNumber: selectedRoomNumbers.length
                    ? selectedRoomNumbers[0]
                    : undefined,
                  roomNumbers: selectedRoomNumbers.length
                    ? selectedRoomNumbers
                    : undefined,
                } as any);
                await loadClass();
                setMsg("반 정보가 저장되었습니다.");
              }}
              className="bg-black text-white px-3 py-2 rounded"
            >
              반 정보 저장
            </button>
            {msg && <span className="text-gray-700">{msg}</span>}
            {err && <span className="text-red-600">{err}</span>}
          </div>
        </div>

        {/* 좌석 편집 + 배정 */}
        <div className="bg-white border border-black rounded-2xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-lg font-semibold text-gray-900">
              좌석 배치 / 배정
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-900">편집할 방</span>
              <select
                value={editingRoom ?? ""}
                onChange={(e) =>
                  setEditingRoom(e.target.value ? Number(e.target.value) : null)
                }
                className="border border-black rounded px-2 py-1 text-gray-900 bg-white"
              >
                <option value="">선택…</option>
                {selectedRoomNumbers.map((n) => (
                  <option key={n} value={n}>
                    Room {n}
                  </option>
                ))}
              </select>
              <button
                onClick={doSaveNow}
                disabled={!layout || !editingRoom}
                className="bg-black text-white px-3 py-2 rounded disabled:opacity-50"
              >
                좌석 저장
              </button>
            </div>
          </div>

          {!editingRoom ? (
            <div className="text-sm text-gray-900">
              위에서 편집할 방을 선택하세요.
            </div>
          ) : !layout ? (
            <div className="text-sm text-gray-900">레이아웃 로딩 중…</div>
          ) : (
            <>
              <div className="text-xs text-gray-700">
                좌석을 클릭하거나, 아래 표에서 행을 클릭해도 선택됩니다.
              </div>
              <div
                data-seat-editor
                className="border border-black rounded-xl p-3 bg-white"
                onClickCapture={(e) => {
                  const el = (e.target as HTMLElement).closest(
                    "[data-seat-index]"
                  ) as HTMLElement | null;
                  if (el) {
                    const idx = Number(el.dataset.seatIndex);
                    if (Number.isFinite(idx)) onSeatClick(idx);
                  }
                }}
              >
                <VectorSeatEditor
                  value={layout.seats as VectorSeat[]}
                  onChange={(seats) => {
                    setLayout((prev) =>
                      prev ? { ...prev, seats: mergeSeats(prev.seats, seats) } : null
                    );
                    scheduleAutoSave();
                  }}
                />
              </div>

              <div className="grid md:grid-cols-[1fr_1fr] gap-3">
                {/* 선택 좌석 패널 */}
                {selectedSeat && (
                  <div className="border border-black rounded p-3">
                    <div className="font-semibold text-gray-900 mb-2">
                      선택한 좌석
                    </div>
                    <div className="space-y-2 text-gray-900">
                      <div>
                        좌석번호:{" "}
                        <span className="font-semibold">
                          {selectedSeat.label}
                        </span>
                      </div>
                      <div>
                        상태: {selectedSeat.disabled ? "비활성" : "활성"}
                      </div>
                      <div>
                        배정 학생:{" "}
                        <span className="font-semibold">
                          {selectedSeat.studentId
                            ? displayName(selectedSeat.studentId)
                            : "없음"}
                        </span>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={toggleSeatDisabled}
                          className="px-3 py-1.5 rounded bg-black text-white"
                        >
                          {selectedSeat.disabled ? "활성화" : "비활성화"}
                        </button>
                        <button
                          onClick={async () => {
                            if (
                              selectedSeatIndex != null &&
                              layout &&
                              editingRoom != null
                            ) {
                              const next = [...layout.seats];
                              const label = String(next[selectedSeatIndex].label);
                              next[selectedSeatIndex] = {
                                ...next[selectedSeatIndex],
                                studentId: null,
                              };
                              setLayout({ ...layout, seats: next });
                              try {
                                await api.assignSeat(classId, {
                                  roomNumber: editingRoom,
                                  seatLabel: label,
                                  studentId: null,
                                });
                                setMsg(`좌석 ${label} 배정 해제`);
                              } catch (e: any) {
                                setErr(e?.message ?? "배정 해제 실패");
                              }
                              scheduleAutoSave();
                            }
                          }}
                          className="px-3 py-1.5 rounded bg-black text-white/90"
                        >
                          배정 해제
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* 학생 배정 패널 */}
                <div className="border border-black rounded p-3">
                  <div className="font-semibold text-gray-900 mb-2">
                    학생 배정
                  </div>
                  <div className="flex items-center gap-2">
                    <select
                      value={selectedStudentId ?? ""}
                      onChange={(e) =>
                        setSelectedStudentId(e.target.value || null)
                      }
                      className="border border-black rounded px-2 py-1 text-gray-900 bg-white"
                    >
                      <option value="">학생 선택…</option>
                      {currentStudents.map((sid) => (
                        <option key={sid} value={sid}>
                          {displayName(sid)}
                        </option>
                      ))}
                    </select>
                    <input
                      value={seatNumInput}
                      onChange={(e) =>
                        setSeatNumInput(normalizeSeatNumberText(e.target.value))
                      }
                      placeholder={`좌석번호(1~${Math.max(maxSeatCount, 1)})`}
                      className="border border-black rounded px-2 py-1 text-gray-900 w-28 bg-white"
                    />
                    <button
                      onClick={assignByInputs}
                      className="px-4 py-2 rounded bg-black text-white active:scale-[0.99]"
                    >
                      배정
                    </button>
                  </div>

                  {/* 학생 리스트 */}
                  <div className="mt-3 grid sm:grid-cols-2 gap-2 max-h-56 overflow-auto">
                    {currentStudents.map((sid) => {
                      const seatLabel = studentToSeatLabel.get(sid) || null;
                      const isSelected = sid === selectedStudentId;
                      return (
                        <button
                          key={sid}
                          onClick={() => setSelectedStudentId(sid)}
                          className={`text-left border rounded px-3 py-2 transition ${
                            isSelected
                              ? "bg-black/5 border-black"
                              : "bg-white hover:bg-black/5 border-black/60"
                          }`}
                        >
                          <div className="font-medium text-gray-900">
                            {displayName(sid)}
                          </div>
                          <div className="text-xs text-gray-600">
                            {seatLabel ? `좌석 ${seatLabel}` : "미배정"}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* 현재 배정표 */}
              <div className="border border-black rounded p-3">
                <div className="font-semibold text-gray-900 mb-2">
                  현재 배정표
                </div>
                <div className="overflow-x-auto">
                  <table className="min-w-[420px] text-sm">
                    <thead>
                      <tr className="text-left text-gray-900">
                        <th className="py-2 pr-4">좌석</th>
                        <th className="py-2 pr-4">학생</th>
                        <th className="py-2">작업</th>
                      </tr>
                    </thead>
                    <tbody>
                      {layout.seats
                        .slice()
                        .sort(seatSort)
                        .map((s, i) => (
                          <tr
                            key={i}
                            className="border-t hover:bg-black/5 cursor-pointer"
                            onClick={() => {
                              setSelectedSeatIndex(i);
                              setSeatNumInput(String(s.label));
                            }}
                          >
                            <td className="py-2 pr-4 text-gray-900">
                              {s.label}
                            </td>
                            <td className="py-2 pr-4 text-gray-900">
                              {s.studentId
                                ? displayName(s.studentId)
                                : "-"}
                            </td>
                            <td className="py-2">
                              {s.studentId && (
                                <button
                                  onClick={async (e) => {
                                    e.stopPropagation();
                                    if (editingRoom == null) return;
                                    const next = [...layout.seats];
                                    next[i] = { ...next[i], studentId: null };
                                    setLayout({ ...layout, seats: next });
                                    try {
                                      await api.assignSeat(classId, {
                                        roomNumber: editingRoom,
                                        seatLabel: String(s.label),
                                        studentId: null,
                                      });
                                    } catch (err: any) {
                                      setErr(
                                        err?.message ?? "해제 실패"
                                      );
                                    }
                                    scheduleAutoSave();
                                  }}
                                  className="text-sm text-black hover:underline"
                                >
                                  해제
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
                <div className="text-xs text-gray-600 mt-2">
                  * 행을 클릭해도 좌석이 선택됩니다.
                </div>
              </div>
            </>
          )}
        </div>

        {/* 현재 학생 목록 */}
        <div className="bg-white border border-black rounded-2xl p-5">
          <div className="text-lg font-semibold mb-2 text-gray-900">
            등록 학생 ({currentStudents.length})
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
            {currentStudents.map((sid) => (
              <div
                key={sid}
                className="border border-black rounded px-3 py-2 flex items-center justify-between bg-white"
              >
                <span className="text-gray-900 font-medium">
                  {displayName(sid)}
                </span>
                <div className="flex items-center gap-3">
                  {studentToSeatLabel.get(sid) && (
                    <span className="text-xs text-gray-600">
                      좌석 {studentToSeatLabel.get(sid)}
                    </span>
                  )}
                  <button
                    onClick={() => removeStudent(sid)}
                    className="text-black text-sm hover:underline"
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))}
            {currentStudents.length === 0 && (
              <div className="text-gray-900">아직 학생이 없습니다.</div>
            )}
          </div>
        </div>

        {/* 학생 검색/추가 */}
        <div className="bg-white border border-black rounded-2xl p-5 space-y-3">
          <div className="text-lg font-semibold text-gray-900">학생 검색</div>
          <div className="flex flex-wrap gap-2">
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="이름 또는 ID 검색"
              className="border border-black rounded px-2 py-1 text-gray-900 bg-white"
            />
          </div>
          <div className="flex flex-wrap gap-2 mt-2">
            <input
              value={grade}
              onChange={(e) => setGrade(e.target.value)}
              placeholder="학년(선택)"
              className="border border-black rounded px-2 py-1 w-32 text-gray-900 bg-white"
            />
            <button
              onClick={search}
              className="bg-black text-white px-3 py-1 rounded"
            >
              검색
            </button>
            {searching && (
              <span className="text-gray-900 text-sm">검색중…</span>
            )}
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2 mt-2">
            {hits.map((h) => (
              <div
                key={h.studentId}
                className="border border-black rounded px-3 py-2 flex items-center justify-between bg-white"
              >
                <div className="font-medium text-gray-900">
                  {h.studentName ?? h.studentId}
                </div>
                <button
                  onClick={() => addStudent(h.studentId)}
                  className="text-black text-sm hover:underline"
                >
                  추가
                </button>
              </div>
            ))}
            {hits.length === 0 && (
              <div className="text-gray-900">검색 결과 없음</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
