// greenacademy_web/src/app/teacher/page.tsx
"use client";
import TeacherQnaPanel from "@/app/qna/TeacherQnaPanel";

export default function Page() {
  return (
    <div className="p-6">
      <h1 className="text-xl font-semibold mb-4">교사 Q&A</h1>
      <TeacherQnaPanel />
    </div>
  );
}
