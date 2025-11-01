// src/app/lib/dateRange.ts
const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
export const ymd = (d: Date) =>
  `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

/** 이번 주 [월 00:00, 다음주 월 00:00) */
export function thisWeekRange(today = new Date()) {
  const dow = today.getDay(); // 0=Sun
  const offsetToMon = dow === 0 ? -6 : 1 - dow;
  const mon = new Date(today);
  mon.setDate(today.getDate() + offsetToMon);
  mon.setHours(0, 0, 0, 0);
  const nextMon = new Date(mon);
  nextMon.setDate(mon.getDate() + 7);
  return { from: ymd(mon), to: ymd(nextMon) }; // [from, to)
}

/** 해당 월의 [1일 00:00, 다음달 1일 00:00) */
export function monthRange(base = new Date()) {
  const first = new Date(base.getFullYear(), base.getMonth(), 1);
  const nextFirst = new Date(base.getFullYear(), base.getMonth() + 1, 1);
  first.setHours(0, 0, 0, 0);
  nextFirst.setHours(0, 0, 0, 0);
  return { from: ymd(first), to: ymd(nextFirst) }; // [from, to)
}

/** JS getDay(0..6) → 1..7(Mon=1..Sun=7) */
export function jsToIsoDow(jsDow: number) {
  return jsDow === 0 ? 7 : jsDow;
}
