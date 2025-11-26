"use client";
import TeacherMainPanel from "@/components/manage/TeacherMainPanel";
import { getSession } from "@/app/lib/session";

export default function TeacherClassesPage() {
  const me = getSession();
  if (!me) {
    if (typeof window !== "undefined") location.href = "/login";
    return null;
  }

  return (
    <div className="max-w-7xl mx-auto px-6 py-6">
      <TeacherMainPanel user={me} />
    </div>
  );
}
