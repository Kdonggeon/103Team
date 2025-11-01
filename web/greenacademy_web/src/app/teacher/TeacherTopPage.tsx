"use client";
import { useEffect, useState } from "react";
import TeacherManagePanel from "./TeacherManagePanel";
import type { LoginResponse } from "@/app/lib/api";

export default function TeacherTopPage() {
  const [user, setUser] = useState<LoginResponse | null>(null);

  useEffect(() => {
    const raw = localStorage.getItem("session") ?? localStorage.getItem("login");
    if (!raw) return;
    try {
      const s = JSON.parse(raw);
      setUser(s);
    } catch {
      console.warn("session parse error");
    }
  }, []);

  if (!user) return <div className="p-6 text-gray-900">로그인 정보를 불러오는 중…</div>;

  return (
    <div className="max-w-6xl mx-auto p-6">
      <TeacherManagePanel user={user} />
    </div>
  );
}
