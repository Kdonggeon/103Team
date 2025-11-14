// src/app/page.tsx
import { Suspense } from "react";
import HomeClient from "./HomeClient";

export const dynamic = "force-dynamic"; // (선택) 검색 파라미터, 로그인 등 있을 땐 추천

export default function Page() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center text-gray-600">
          메인 화면 불러오는 중…
        </div>
      }
    >
      <HomeClient />
    </Suspense>
  );
}
