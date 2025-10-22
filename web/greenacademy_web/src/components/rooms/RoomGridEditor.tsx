"use client";
import { useEffect, useMemo, useRef, useState } from "react";

export type SeatCell = { seatNumber:number; row:number; col:number; disabled?:boolean };

export default function RoomGridEditor({
  rows, cols, value, onChange
}: { rows:number; cols:number; value:SeatCell[]; onChange:(v:SeatCell[])=>void }) {

  /* -------- grid 재구성 -------- */
  const grid = useMemo(()=>{
    const m:(SeatCell|null)[][] = Array.from({length:rows},()=>Array(cols).fill(null));
    value.forEach(s=>{ if(s.row<rows && s.col<cols) m[s.row][s.col]=s; });
    return m;
  }, [rows, cols, value]);

  /* -------- 상태 -------- */
  const [isSelecting, setIsSelecting] = useState(false);          // 좌클릭 드래그로 선택중?
  const [selected, setSelected] = useState<Set<number>>(new Set()); // seatNumber 집합

  // 컨텍스트 메뉴 (우클릭)
  const [ctxOpen, setCtxOpen] = useState(false);
  const [ctxPos, setCtxPos] = useState<{x:number;y:number}>({x:0,y:0});

  const classCell = "w-10 h-10 rounded border border-black bg-white text-black text-sm flex items-center justify-center select-none";
  const classActive = "outline outline-2 outline-black";
  const isSel = (id:number)=> selected.has(id);
  const addSel = (id:number)=> setSelected(prev => { const n = new Set(prev); n.add(id); return n; });
  const clearSel = ()=> setSelected(new Set());

  /* -------- 전역 pointerup로 선택 종료 -------- */
  useEffect(()=>{
    const up = ()=> setIsSelecting(false);
    window.addEventListener("pointerup", up);
    window.addEventListener("pointercancel", up);
    return ()=>{ window.removeEventListener("pointerup", up); window.removeEventListener("pointercancel", up); };
  }, []);

  /* -------- 일괄 변경 -------- */
  const applyDisabled = (ids:Set<number>, disabled:boolean)=>{
    if (ids.size===0) return;
    onChange(value.map(v => ids.has(v.seatNumber) ? {...v, disabled} : v));
  };

  /* -------- 렌더 -------- */
  return (
    <div className="inline-block relative">
      {/* GRID */}
      {grid.map((row,i)=>(
        <div key={i} className="flex">
          {row.map((c,j)=>{
            if (!c) return <div key={j} className={classCell}></div>;
            const active = isSel(c.seatNumber);

            return (
              <div
                key={c.seatNumber}
                className={`${classCell} ${active?classActive:""} ${c.disabled?"border-dashed":""}`}
                title="좌:드래그로 다중선택 · 우:메뉴(미사용/사용/해제) · 더블:번호변경"
                // 좌클릭: 선택 시작 + 현재 셀 선택
                onPointerDown={(e)=>{
                  if (e.button!==0) return;           // 좌클릭만
                  setIsSelecting(true);
                  // 기존 선택 유지하려면 Shift 누르고 드래그
                  if (!e.shiftKey && !e.ctrlKey) clearSel();
                  addSel(c.seatNumber);
                  (e.currentTarget as HTMLElement).setPointerCapture?.(e.pointerId);
                }}
                // 드래그 중 지나가면 선택 집합에 추가
                onPointerEnter={()=>{
                  if (!isSelecting) return;
                  addSel(c.seatNumber);
                }}
                // 우클릭: 메뉴
                onContextMenu={(e)=>{
                  e.preventDefault();
                  // 선택이 비어있으면 우클릭한 셀만 선택
                  if (selected.size===0 || !selected.has(c.seatNumber)) {
                    setSelected(new Set([c.seatNumber]));
                  }
                  setCtxPos({ x: e.clientX, y: e.clientY });
                  setCtxOpen(true);
                }}
                // 번호 개별 수정(옵션)
                onDoubleClick={()=>{
                  const n = prompt("좌석 번호 변경", String(c.seatNumber));
                  const nv = n ? parseInt(n,10) : NaN;
                  if (isNaN(nv)) return;
                  if (value.some(v=>v.seatNumber===nv && v.seatNumber!==c.seatNumber)) {
                    alert("이미 존재하는 좌석번호입니다."); return;
                  }
                  onChange(value.map(v=> v.seatNumber===c.seatNumber ? {...v, seatNumber:nv} : v));
                }}
              >
                {c.disabled ? "—" : c.seatNumber}
              </div>
            );
          })}
        </div>
      ))}

      {/* 하단 툴바 (모바일/접근성용) */}
      <div className="mt-2 flex items-center gap-2 text-xs text-black">
        <span>선택: {selected.size}개</span>
        <button className="px-2 py-1 border border-black rounded" onClick={()=>applyDisabled(selected, true)}>미사용으로</button>
        <button className="px-2 py-1 border border-black rounded" onClick={()=>applyDisabled(selected, false)}>사용으로</button>
        <button className="px-2 py-1 border border-black rounded" onClick={clearSel}>선택 해제</button>
      </div>

      {/* 우클릭 컨텍스트 메뉴 */}
      {ctxOpen && (
        <div
          className="absolute z-10 bg-white border border-black rounded shadow-sm text-sm"
          style={{ left: ctxPos.x, top: ctxPos.y }}
          onMouseLeave={()=>setCtxOpen(false)}
        >
          <button className="block px-3 py-2 w-full text-left hover:bg-gray-50"
                  onClick={()=>{ applyDisabled(selected, true); setCtxOpen(false); }}>
            미사용으로 표시
          </button>
          <button className="block px-3 py-2 w-full text-left hover:bg-gray-50"
                  onClick={()=>{ applyDisabled(selected, false); setCtxOpen(false); }}>
            사용으로 표시
          </button>
          <div className="h-px bg-black/10" />
          <button className="block px-3 py-2 w-full text-left hover:bg-gray-50"
                  onClick={()=>{ clearSel(); setCtxOpen(false); }}>
            선택 해제
          </button>
        </div>
      )}
    </div>
  );
}
