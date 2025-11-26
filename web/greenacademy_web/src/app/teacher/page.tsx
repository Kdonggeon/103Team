// src/app/teacher/page.tsx
"use client";

import TeacherMainPanel from "@/components/manage/TeacherMainPanel";
import { getSession } from "@/app/lib/session";
import type { LoginResponse } from "@/app/lib/api";

export default function TeacherHomePage() {
  const me = getSession() as LoginResponse | null;

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
