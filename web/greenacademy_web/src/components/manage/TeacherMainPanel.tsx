"use client";

import React, { useEffect, useState } from "react";
import type { LoginResponse } from "@/app/lib/api";
import {
  fetchSeatBoard,
  fetchTodayClasses,
  type SeatBoardResponse,
  type TeacherClassLite,
  todayYmd,
} from "@/app/lib/teachermainApi";
import Panel, { PanelGrid } from "@/components/ui/Panel";
import SeatBoardCard from "@/components/manage/SeatBoardCard";

/* ================== main ================== */
export default function TeacherMainPanel({ user }: { user: NonNullable<LoginResponse> }) {
  const teacherId = user.username;

  const [classes, setClasses] = useState<TeacherClassLite[]>([]);
  const [selected, setSelected] = useState<TeacherClassLite | null>(null);
  const [loadingClasses, setLoadingClasses] = useState(true);

  const [board, setBoard] = useState<SeatBoardResponse | null>(null);
  const [loadingBoard, setLoadingBoard] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  /* -------- load classes (left list) -------- */
  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        setLoadingClasses(true);
        const list = await fetchTodayClasses(teacherId);
        if (!alive) return;
        setClasses(list);
        setSelected((prev) => prev && list.some((c) => c.classId === prev.classId) ? prev : list[0] ?? null);
      } catch (e: any) {
        if (alive) setErr(e?.message ?? String(e));
      } finally {
        if (alive) setLoadingClasses(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [teacherId]);

  /* -------- load seat board -------- */
  useEffect(() => {
    if (!selected?.classId) {
      setBoard(null);
      return;
    }
    let alive = true;
    (async () => {
      try {
        setLoadingBoard(true);
        const data = await fetchSeatBoard(selected.classId, todayYmd());
        if (!alive) return;
        setBoard(data);
      } catch (e: any) {
        if (alive) setErr(e?.message ?? String(e));
      } finally {
        if (alive) setLoadingBoard(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [selected?.classId]);

  /* ESC로 모달 닫기 */
  useEffect(() => {
    if (!modalOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setModalOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [modalOpen]);

  return (
    <PanelGrid>
      {/* 왼쪽: 오늘 수업 */}
      <Panel title="오늘 수업" className="h-full">
        {loadingClasses ? (
          <div className="text-sm text-gray-500">불러오는 중…</div>
        ) : classes.length === 0 ? (
          <div className="text-sm text-gray-600">오늘 일정이 없습니다.</div>
        ) : (
          <ul className="space-y-2">
            {classes.map((c) => {
              const isSel = selected?.classId === c.classId;
              return (
                <li key={c.classId}>
                  <button
                    onClick={() => setSelected(c)}
                    className={`w-full text-left rounded-xl ring-1 ring-black/5 shadow-sm bg-white px-3 py-2 hover:bg-gray-50 transition ${
                      isSel ? "outline outline-2 outline-emerald-400" : ""
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="font-medium text-black">
                        {c.className}
                        {typeof c.roomNumber === "number" ? (
                          <span className="ml-2 text-gray-600">• {c.roomNumber}호</span>
                        ) : null}
                      </div>
                    </div>
                    <div className="mt-1 text-xs text-gray-600">
                      {c.startTime ?? "--:--"} ~ {c.endTime ?? "--:--"}
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </Panel>

      {/* 오른쪽: 좌석 현황 (원장 카드 재사용) */}
      <Panel title={selected ? `${selected.className} — 좌석 현황` : "좌석 현황"}>
        {!selected?.classId ? (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 text-sm text-gray-700">
            왼쪽에서 수업을 선택하면 좌석판이 표시됩니다.
          </div>
        ) : (
          <>
            <SeatBoardCard
              title={selected.className ?? "좌석 현황"}
              date={todayYmd()}
              board={board}
              loading={loadingBoard}
              error={err}
              onExpand={() => setModalOpen(true)}
            />

            {modalOpen && (
              <div
                className="fixed inset-0 z-[300] bg-black/50 flex items-center justify-center p-4"
                onClick={() => setModalOpen(false)}
              >
                <div
                  className="w-full max-w-6xl max-h-[90vh] overflow-auto bg-white rounded-2xl border border-gray-300 shadow-2xl p-4"
                  onClick={(e) => e.stopPropagation()}
                >
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold text-black">좌석 현황 확대</h3>
                    <button
                      onClick={() => setModalOpen(false)}
                      className="px-3 py-1 rounded border text-sm text-gray-800 hover:bg-gray-100"
                    >
                      닫기
                    </button>
                  </div>
                  <SeatBoardCard
                    title={selected.className ?? "좌석 현황"}
                    date={todayYmd()}
                    board={board}
                    loading={loadingBoard}
                    error={err}
                  />
                </div>
              </div>
            )}
          </>
        )}
      </Panel>
    </PanelGrid>
  );
}
