// app/teacher/classes/[classId]/page.tsx
import ClassDetailClient from "./ClassDetailClient";

export default function Page({ params }: { params: { classId: string } }) {
  return <ClassDetailClient classId={params.classId} />;
}
