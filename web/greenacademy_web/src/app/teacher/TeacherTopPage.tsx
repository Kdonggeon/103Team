"use client";
import { useEffect, useState } from "react";
import TeacherManagePanel from "./TeacherManagePanel";

export default function TeacherTopPage() {
  const [teacherId, setTeacherId] = useState("");
  const [defaultAcademy, setDefaultAcademy] = useState<number | null>(null);

  useEffect(() => {
    const raw = localStorage.getItem("session") ?? localStorage.getItem("login");
    if (!raw) return;
    const s = JSON.parse(raw);
    setTeacherId(s?.username ?? "");
    const first =
      Array.isArray(s?.academyNumbers) && s.academyNumbers.length > 0
        ? Number(s.academyNumbers[0])
        : null;
    setDefaultAcademy(Number.isFinite(first) ? first : null);
  }, []);

  return (
    <div className="max-w-6xl mx-auto p-6">
      <TeacherManagePanel teacherId={teacherId} defaultAcademy={defaultAcademy} />
    </div>
  );
}
