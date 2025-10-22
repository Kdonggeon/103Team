// src/app/lib/schedule-derive.ts
export const toYmd = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`;

export const normalizeTime = (t?: string|null) => {
  if (!t) return "";
  const [h,m="0"] = String(t).split(":");
  const hh = String(parseInt(h || "0", 10)).padStart(2,"0");
  const mm = String(parseInt(m || "0", 10)).padStart(2,"0");
  return `${hh}:${mm}`;
};

export const normalizeDays = (v:any): number[] => {
  if (!v) return [];
  const fw: Record<string,string> = {"１":"1","２":"2","３":"3","４":"4","５":"5","６":"6","７":"7"};
  const ko: Record<string,number> = {"월":1,"화":2,"수":3,"목":4,"금":5,"토":6,"일":7};
  return (Array.isArray(v)?v:[v]).map(x=>{
    if (typeof x==="number") return x;
    const s = fw[String(x)] ?? String(x);
    if (ko[s]!=null) return ko[s];
    const n = parseInt(s,10); return (n>=1 && n<=7) ? n : null;
  }).filter(Boolean) as number[];
};

export const dowOf = (ymd:string) => {
  const [y,m,d] = ymd.split("-").map(Number);
  const js = new Date(y, m-1, d).getDay(); // 0=Sun
  return js===0?7:js; // 1=Mon..7=Sun
};

export const daysInMonth = (y:number,m1to12:number) => new Date(y, m1to12, 0).getDate();

type CourseForDerive = {
  classId: string;
  className: string;
  roomNumber?: number;
  startTime?: string|null;
  endTime?: string|null;
  daysOfWeek?: any;
  extraDates?: string[];
  cancelledDates?: string[];
};

/** 특정 날짜의 수업을 classes로부터 파생 */
export function deriveDayFromClasses(ymd: string, courses: CourseForDerive[]) {
  const dow = dowOf(ymd);
  const out: Array<{
    id: string; date: string; classId: string; title: string;
    startTime: string; endTime: string; roomNumber?: number;
  }> = [];

  for (const c of courses) {
    const start = normalizeTime(c.startTime ?? undefined);
    const end   = normalizeTime(c.endTime ?? undefined);
    if (!start || !end) continue;

    const extras = c.extraDates ?? [];
    const canc   = c.cancelledDates ?? [];
    if (canc.includes(ymd)) continue;

    const isExtra = extras.includes(ymd);
    const onPattern = normalizeDays(c.daysOfWeek).includes(dow);

    if (isExtra || onPattern) {
      out.push({
        id: `${c.classId}-${ymd}`,
        date: ymd,
        classId: c.classId,
        title: c.className,
        startTime: start,
        endTime: end,
        roomNumber: c.roomNumber,
      });
    }
  }

  out.sort((a,b)=>a.startTime.localeCompare(b.startTime) || a.endTime.localeCompare(b.endTime));
  return out;
}

/** 해당 월(1~12)의 날짜별 개수 집계 */
export function deriveMonthCountFromClasses(
  year:number, month:number, courses: CourseForDerive[]
){
  const last = daysInMonth(year, month);
  const count: Record<string,number> = {};
  for (let d=1; d<=last; d++) {
    const ymd = `${year}-${String(month).padStart(2,"0")}-${String(d).padStart(2,"0")}`;
    const daily = deriveDayFromClasses(ymd, courses);
    if (daily.length) count[ymd] = daily.length;
  }
  return count;
}
