"use client";
import { useRouter } from "next/navigation";

export default function AccountDeletePage() {
  const router = useRouter();
  return (
    <main className="max-w-3xl mx-auto p-6 space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">계정탈퇴</h1>
      <p className="text-gray-700">탈퇴 전 유의사항을 안내하고 확인 받으세요.</p>
      <div className="flex gap-3">
        <button
          onClick={() => router.back()}
          className="px-4 py-2 rounded-lg ring-1 ring-gray-300 bg-white hover:bg-gray-50"
        >
          취소
        </button>
        <button
          onClick={() => alert("탈퇴 요청 보냄(여기에 API 연동)")}
          className="px-4 py-2 rounded-lg text-white"
          style={{ backgroundColor: "#EF4444" }}
        >
          탈퇴하기
        </button>
      </div>
    </main>
  );
}
