"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getSession } from "@/app/lib/session";

export default function ManageIndex() {
  const router = useRouter();

  useEffect(() => {
    const s = getSession();
    if (!s) { router.replace("/login"); return; }

    // 역할별로 관리 홈으로 라우팅
    if (s.role === "teacher") {
      router.replace("/teacher/classes");     // ⬅️ 선생: 반 관리
    } else if (s.role === "director") {
      router.replace("/director/rooms");      // ⬅️ 원장: 강의실/원장 관리
    } else {
      // 학생/학부모는 관리 권한 없음 → 홈으로 복귀(혹은 안내 페이지)
      router.replace("/");
    }
  }, [router]);

  return null; // 잠깐 비워두기 (즉시 리디렉션)
}
