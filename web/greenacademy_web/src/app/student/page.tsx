"use client";
import QnaPanel from "@/app/qna/QnaPanel";
import { getSavedSession } from "@/lib/api";

export default function Page() {
  const session = getSavedSession() || null;


  const role = (session?.role === "parent" ? "parent" : "student") as "student" | "parent";


  const academies = (session?.academyNumbers || [])
    .map((n: any) => Number(n))
    .filter((n: number) => Number.isFinite(n));

  const academyNumber = academies.length ? academies[0] : undefined;

  if (!academyNumber) {
    return <div className="p-6 text-sm text-red-600">소속된 학원이 없습니다. 관리자에게 문의하세요.</div>;
  }

  return (
    <div className="p-6">
      <h1 className="text-xl font-semibold mb-4">학생 Q&amp;A</h1>
      <QnaPanel academyNumber={academyNumber} role={role} />
    </div>
  );
}
