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
    <div className="max-w-7xl mx-auto px-6 py-6">
      <TeacherManagePanel user={me} />
    </div>
  );
}
