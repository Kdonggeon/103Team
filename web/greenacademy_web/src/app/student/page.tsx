// C:\project\103Team-sub\web\greenacademy_web\src\app\student\page.tsx
"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import QnaPanel from "../qna/QnaPanel";
import StudentProfileCard from "./StudentProfileCard";
import StudentAttendancePanel from "./StudentAttendancePanel";
import StudentTimetablePanel from "./StudentTimetablePanel";

/** 타입 */
type LoginSession = {
  role: "student" | "parent" | "teacher" | "director";
  username: string;
  name?: string;
  token?: string;
  academyNumbers?: number[];
};

type ManageItem = "내정보" | "출결관리" | "시간표";

/** 색상 토큰 */
const colors = {
  green: "#65E478",
  grayBg: "#F2F4F7",
};

export default function StudentPage() {
  const router = useRouter();

  const [ready, setReady] = useState(false);
  const [user, setUser] = useState<LoginSession | null>(null);
  const [academyNumber, setAcademyNumber] = useState<number | null>(null);

  // 관리 드롭다운 상태
  const [activeTab, setActiveTab] = useState<"관리" | "Q&A">("관리");
  const [manageMenu, setManageMenu] = useState<ManageItem>("내정보");
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  /** 세션 로드 */
  useEffect(() => {
    const raw = localStorage.getItem("login");
    if (!raw) {
      router.replace("/login");
      return;
    }
    try {
      const parsed = JSON.parse(raw);
      const s: LoginSession = {
        role: (String(parsed?.role || "student").toLowerCase() as LoginSession["role"]) ?? "student",
        username: parsed?.username ?? "",
        name: parsed?.name ?? undefined,
        token: parsed?.token ?? undefined,
        academyNumbers: Array.isArray(parsed?.academyNumbers)
          ? parsed.academyNumbers.map((n: any) => Number(n)).filter((n: number) => Number.isFinite(n))
          : [],
      };
      setUser(s);
      setAcademyNumber(s.academyNumbers?.[0] ?? null);
    } catch {
      localStorage.removeItem("login");
      router.replace("/login");
      return;
    } finally {
      setReady(true);
    }
  }, [router]);

  /** 드롭다운 외부 클릭/ESC 닫기 */
  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (!menuRef.current) return;
      if (!menuRef.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  if (!ready) return null;

  return (
    <div className="min-h-screen" style={{ backgroundColor: colors.grayBg }}>
      {/* 헤더 */}
      <header className="sticky top-0 z-10 bg-white/80 backdrop-blur supports-[backdrop-filter]:bg-white/60 ring-1 ring-black/5">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="leading-tight">
            <div className="text-lg font-semibold text-gray-900">학생 포털</div>
            <div className="text-sm text-gray-600 -mt-0.5">{user?.name || user?.username}</div>
          </div>

          {/* 탭 + 관리 드롭다운 */}
          <div className="flex items-center gap-2" ref={menuRef}>
            <button
              onClick={() => setActiveTab("관리")}
              className={`px-4 py-2 rounded-full font-medium ring-1 ring-black/5 ${
                activeTab === "관리" ? "bg-[#8CF39B] text-gray-900" : "bg-[#CFF9D6] text-gray-700 hover:bg-[#B7F2C0]"
              }`}
            >
              관리
            </button>

            {/* 관리 드롭다운 */}
            <div className="relative">
              <button
                onClick={() => {
                  setActiveTab("관리");
                  setOpen((p) => !p);
                }}
                className="px-4 py-2 rounded-full font-medium bg-white ring-1 ring-black/5 hover:bg-gray-50"
                aria-haspopup="menu"
                aria-expanded={open}
              >
                {manageMenu}
              </button>
              {open && (
                <div className="absolute right-0 mt-2 w-40 rounded-xl bg-white shadow-lg ring-1 ring-black/5 overflow-hidden z-20">
                  <ul className="divide-y divide-gray-100">
                    {(["내정보", "출결관리", "시간표"] as ManageItem[]).map((it) => (
                      <li key={it}>
                        <button
                          type="button"
                          onClick={() => {
                            setManageMenu(it);
                            setActiveTab("관리");
                            setOpen(false);
                          }}
                          className="w-full text-left px-4 py-2.5 text-sm text-gray-900 hover:bg-gray-50"
                        >
                          {it}
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            <button
              onClick={() => setActiveTab("Q&A")}
              className={`px-4 py-2 rounded-full font-medium ring-1 ring-black/5 ${
                activeTab === "Q&A" ? "bg-[#8CF39B] text-gray-900" : "bg-[#CFF9D6] text-gray-700 hover:bg-[#B7F2C0]"
              }`}
            >
              Q&A
            </button>
          </div>
        </div>
      </header>

      {/* 본문 */}
      <main className="max-w-6xl mx-auto px-6 py-6 space-y-6">
        {activeTab === "관리" && (
          <div className="space-y-6">
            {manageMenu === "내정보" && <StudentProfileCard />}
            {manageMenu === "출결관리" && <StudentAttendancePanel />}
            {manageMenu === "시간표" && <StudentTimetablePanel />}
          </div>
        )}

        {activeTab === "Q&A" && (
          <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">Q&amp;A</h2>
            {academyNumber == null ? (
              <p className="text-sm text-gray-700">학원번호가 없습니다. 로그인 정보를 확인해 주세요.</p>
            ) : (
              <QnaPanel academyNumber={academyNumber} role="student" />
            )}
          </div>
        )}
      </main>
    </div>
  );
}
