"use client";

import React from "react";
import { getSession } from "@/app/lib/session";
import type { LoginResponse } from "@/app/lib/api";
import Panel from "@/components/ui/Panel";
import TeacherMonthCalendarContainer from "@/app/teacher/schedule/components/TeacherMonthCalendarContainer";

export default function Page() {
  // 세션에서 사용자 정보 가져오기 (프로젝트에 맞춰 조정)
  const session = getSession() as LoginResponse | null;
  if (!session || session.role !== "teacher") {
    return <div className="p-4">교사 계정으로 로그인해 주세요.</div>;
  }

  return (
    <div className="p-4 space-y-4">
      <Panel title="월간 시간표">
        <TeacherMonthCalendarContainer user={session as NonNullable<LoginResponse>} />
      </Panel>
    </div>
  );
}
