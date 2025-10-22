import { request } from "@/app/lib/api"; 


export type SeatCell = {
  seatNumber: number;   // 1..N
  row: number;          // 0..rows-1 혹은 1..rows (백엔드 기준에 맞춰 사용)
  col: number;          // 0..cols-1 혹은 1..cols
  disabled?: boolean;   // 통로/비활성
};

export type RoomLayoutBody = {
  academyNumber: number;
  rows: number;
  cols: number;
  layout: SeatCell[];
};

export type Room = {
  id: number;           // Mongo에서 int _id 사용 중
  roomNumber: number;
  academyNumber: number;
  rows?: number;
  cols?: number;
  layout?: SeatCell[];
  // currentClass, seats 등은 생략 가능
};

export const roomsApi = {
  // 목록 조회
  listRooms: (academyNumber: number) =>
    request<Room[]>(`/api/admin/rooms?academyNumber=${academyNumber}`),

  // 상세(없으면 생성까지) — "반 만들기" 누를 때 호출
  getRoom: (academyNumber: number, roomNumber: number) =>
    request<Room>(`/api/admin/rooms/${roomNumber}?academyNumber=${academyNumber}`),

  // 레이아웃 저장(n×n 및 편집)
  saveRoomLayout: (roomNumber: number, body: RoomLayoutBody) =>
    request<Room>(`/api/admin/rooms/${roomNumber}/layout`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  // ⛔ 삭제(카드 우상단 X)
  deleteRoom: (academyNumber: number, roomNumber: number) =>
    request<void>(`/api/admin/rooms/${roomNumber}?academyNumber=${academyNumber}`, {
      method: "DELETE",
    }),
};
