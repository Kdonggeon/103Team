
import { Suspense } from "react";
import FamilyPortalClient from "./FamilyPortalClient";

export const dynamic = "force-dynamic"; // (선택) 프리렌더 강제 비활성화

export default function Page() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center text-gray-600">
          Family Portal 불러오는 중…
        </div>
      }
    >
      <FamilyPortalClient />
    </Suspense>
  );
}
