// src/app/teacher/classes/[classId]/page.tsx
import ClassDetailClient from "./ClassDetailClient";

export default async function Page({
  params,
}: {
  params: Promise<{ classId: string }>;
}) {
  // ✅ Next.js 15: params는 Promise — 언랩해서 사용
  const { classId } = await params;
  return <ClassDetailClient classId={classId} />;
}
