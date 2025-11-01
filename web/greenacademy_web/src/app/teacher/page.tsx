// src/app/teacher/page.tsx
"use client";

import TeacherManagePanel from "@/components/manage/TeacherManagePanel";
import { getSession } from "@/app/lib/session";
import type { LoginResponse } from "@/app/lib/api";

export default function TeacherHomePage() {
  const me = getSession() as LoginResponse | null;

  // 비로그인 시 로그인 페이지로 이동
  if (!me) {
    if (typeof window !== "undefined") location.href = "/login";
    return null;
  }

  return (
<<<<<<< HEAD
    <div className="max-w-6xl mx-auto p-6">
      <TeacherManagePanel teacherId={teacherId} defaultAcademy={defaultAcademy} />

=======
    <div className="max-w-7xl mx-auto px-6 py-6">
      <TeacherManagePanel user={me} />
>>>>>>> main-develop/web/feature9
    </div>
  );
}
