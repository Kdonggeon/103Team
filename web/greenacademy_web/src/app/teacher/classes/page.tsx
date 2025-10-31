"use client";
import TeacherManagePanel from "@/app/teacher/TeacherManagePanel";
import { getSession } from "@/app/lib/session";

export default function TeacherClassesPage() {
  const me = getSession();
  if (!me) {
    if (typeof window !== "undefined") location.href = "/login";
    return null;
  }

  return (
    <div className="max-w-7xl mx-auto px-6 py-6">
      <TeacherManagePanel user={me} />
    </div>
  );
}
