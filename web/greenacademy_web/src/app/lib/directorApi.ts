// src/app/lib/directorApi.ts
import { request, ApiError } from "@/app/lib/api";
/* ===== 타입 ===== */
export type DirectorSeat = {
  // grid
  seatNumber?: number | null;
  row?: number | null;
  col?: number | null;

  // vector (0~1 정규화)
  x?: number | null;
  y?: number | null;
  w?: number | null;
  h?: number | null;
  r?: number | null;

  disabled?: boolean | null;
  studentId?: string | null;
  studentName?: string | null;
  attendanceStatus?: string | null;   // 출석/지각/결석/이동/휴식/미기록
};

// src/app/lib/directorApi.ts
export type DirectorRoomStatus = {
  roomNumber: number;
  classId?: string | null;      // ← 추가 (오늘 이 방에서 진행 중인 수업)
  className?: string | null;
  seats: DirectorSeat[];
  presentCount?: number | null;
  lateCount?: number | null;
  absentCount?: number | null;
  moveOrBreakCount?: number | null;
  notRecordedCount?: number | null;

  layoutType?: "grid" | "vector";
  rows?: number | null;
  cols?: number | null;
  canvasW?: number | null;
  canvasH?: number | null;
};


export type DirectorWaiting = {
  studentId: string;
  studentName?: string | null;
  status?: string | null;        // LOBBY / MOVE / BREAK ...
  checkedInAt?: string | null;
};

export type DirectorOverviewResponse = {
  date: string;
  rooms: DirectorRoomStatus[];
  waiting: DirectorWaiting[];
};

/* ===== API ===== */
// 백엔드 컨트롤러 매핑은 예시: GET /api/director/overview?academyNumber=403&date=2025-11-07


export async function fetchDirectorOverview(
  academyNumber: number,
  ymd?: string
): Promise<DirectorOverviewResponse> {
  const qs = new URLSearchParams({ academyNumber: String(academyNumber) });
  if (ymd) qs.set("date", ymd);

  const url1 = `/api/director/overview?${qs.toString()}`;        // 구버전 경로
  const url2 = `/api/director/overview/rooms?${qs.toString()}`;  // 신버전 경로

  try {
    // ✅ request()는 자동으로 Authorization 헤더 붙여줌
    return await request<DirectorOverviewResponse>(url1, { cache: "no-store" });
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      // 404면 신버전 경로로 재시도
      return await request<DirectorOverviewResponse>(url2, { cache: "no-store" });
    }
    throw e;
  }
}