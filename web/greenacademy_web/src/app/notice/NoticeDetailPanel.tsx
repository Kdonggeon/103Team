// C:\project\103Team-sub\web\greenacademy_web\src\app\notice\NoticeDetailPanel.tsx
"use client";

import React, { useEffect, useState } from "react";

/** API 베이스 & fetch 래퍼
 * - 브라우저: NEXT_PUBLIC_API_BASE || "/backend"
 * - SSR/기타: NEXT_PUBLIC_API_BASE || "http://localhost:9090"
 */
const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ??
  (typeof window !== "undefined" ? "/backend" : "http://localhost:9090");

async function fetchApi(path: string, init?: RequestInit) {
  const url = `${API_BASE}${path}`;
  const opts: RequestInit = {
    credentials: init?.credentials ?? "include",
    ...init,
  };

  const res = await fetch(url, opts);
  const ct = res.headers.get("content-type") || "";

  // 🔹 로컬 개발 편의용: Next dev 서버가 404 HTML 주면 localhost:9090으로 한 번 더 시도
  if (
    res.status === 404 &&
    ct.includes("text/html") &&
    typeof window !== "undefined" &&
    (location.hostname === "localhost" || location.hostname === "127.0.0.1")
  ) {
    try {
      const devUrl = `http://localhost:9090${path}`;
      return await fetch(devUrl, opts);
    } catch {
      // ignore, 아래에서 기존 res 반환
    }
  }

  return res;
}

/** 상대 경로를 절대 URL로 변환 */
function abs(src: string) {
  if (!src) return "";
  if (/^https?:\/\//i.test(src)) return src;
  const base = (API_BASE || "").replace(/\/$/, "");
  const path = src.startsWith("/") ? src : `/${src}`;
  return `${base}${path}`;
}

/** 타입 */
type Role = "student" | "parent" | "teacher" | "director";
type Session = { role: Role; username: string; token?: string; academyNumbers?: number[] };

type Notice = {
  id: string;
  title: string;
  content: string;
  author?: string;            // 컨트롤러에서 teacherName으로 매핑됨
  academyNumber?: number;
  createdAt?: string;
  classId?: string | null;
  className?: string | null;
  // 서버에 따라 다를 수 있으니 방어적으로 처리
  imageUrls?: string[];       // ["https://...","/files/..."]
  images?: string[];          // 백호환
};

function authHeaders(session: Session | null): HeadersInit {
  return {
    "Content-Type": "application/json",
    ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
  };
}

/** yyyy-mm-dd hh:mm (KST) */
function formatKST(iso?: string) {
  if (!iso) return "";
  try {
    const d = new Date(iso);
    const y = d.getFullYear();
    const m = `${d.getMonth() + 1}`.padStart(2, "0");
    const day = `${d.getDate()}`.padStart(2, "0");
    const hh = `${d.getHours()}`.padStart(2, "0");
    const mm = `${d.getMinutes()}`.padStart(2, "0");
    return `${y}-${m}-${day} ${hh}:${mm}`;
  } catch {
    return "";
  }
}

export default function NoticeDetailPanel({
  noticeId,
  session,
  onClose,
  onDeleted,
}: {
  noticeId: string;
  session: Session | null;
  onClose: () => void;
  onDeleted?: () => void;
}) {
  const [notice, setNotice] = useState<Notice | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const canWrite = session?.role === "teacher" || session?.role === "director";

  useEffect(() => {
    if (!noticeId || !session?.token) return;
    (async () => {
      try {
        setLoading(true);
        setErr(null);
        const r = await fetchApi(`/api/notices/${encodeURIComponent(noticeId)}`, {
          headers: authHeaders(session),
        });
        if (!r.ok) {
          if (r.status === 401) throw new Error("로그인이 필요합니다. (401)");
          if (r.status === 403) throw new Error("공지 조회 권한이 없습니다. (403)");
          throw new Error(await r.text());
        }
        const data = (await r.json()) as Notice;
        setNotice(data);
      } catch (e: any) {
        setErr(e?.message || "공지 상세를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [noticeId, session?.token]);

  async function handleDelete() {
    if (!noticeId || !session?.token) return;
    const ok = confirm("이 공지를 삭제할까요? 삭제 후 되돌릴 수 없습니다.");
    if (!ok) return;

    try {
      setDeleting(true);
      const r = await fetchApi(`/api/notices/${encodeURIComponent(noticeId)}`, {
        method: "DELETE",
        headers: authHeaders(session),
      });
      if (!r.ok) {
        if (r.status === 401) throw new Error("로그인이 필요합니다. (401)");
        if (r.status === 403) throw new Error("공지 삭제 권한이 없습니다. (403)");
        throw new Error(await r.text());
      }
      onDeleted?.();
    } catch (e: any) {
      alert(e?.message || "삭제에 실패했습니다.");
    } finally {
      setDeleting(false);
    }
  }

  const rawImgList: string[] =
    (notice as any)?.imageUrls ??
    (notice as any)?.images ??
    [];

  const imgList: string[] = Array.isArray(rawImgList) ? rawImgList.map(abs) : [];

  return (
    <section className="max-w-4xl mx-auto p-6 space-y-6">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">공지 상세</h1>
        <div className="flex gap-2">
          <button
            onClick={onClose}
            className="px-4 h-10 rounded-xl border border-gray-300 text-gray-800 bg-white hover:bg-gray-50"
          >
            목록으로
          </button>
          {canWrite && (
            <button
              onClick={handleDelete}
              disabled={deleting}
              className="px-4 h-10 rounded-xl bg-red-500 text-white font-semibold hover:bg-red-600 disabled:opacity-50"
            >
              {deleting ? "삭제 중…" : "삭제"}
            </button>
          )}
        </div>
      </header>

      {err && (
        <div className="px-4 py-3 rounded-xl text-sm bg-red-50 text-red-700 ring-1 ring-red-200">
          {err}
        </div>
      )}

      {loading && (
        <div className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm p-6 animate-pulse space-y-3">
          <div className="h-6 bg-gray-200 rounded w-3/5" />
          <div className="h-4 bg-gray-200 rounded w-1/3" />
          <div className="h-4 bg-gray-200 rounded w-1/2" />
          <div className="mt-4 h-28 bg-gray-200 rounded" />
        </div>
      )}

      {!loading && notice && (
        <article className="rounded-2xl bg-white ring-1 ring-black/5 shadow-sm">
          <div className="p-6">
            <h2 className="text-xl sm:text-2xl font-semibold text-gray-900 break-words">
              {notice.title || "(제목 없음)"}
            </h2>

            <div className="mt-2 text-xs sm:text-sm text-gray-600 flex flex-wrap gap-x-2 gap-y-1">
              <span className="inline-flex items-center gap-1">
                <span className="text-gray-500">작성자</span>
                <span className="text-gray-800 font-medium">{notice.author ?? "관리자"}</span>
              </span>
              {(notice.academyNumber != null || notice.className || notice.createdAt) && (
                <span className="text-gray-300">•</span>
              )}
              {notice.academyNumber != null && (
                <span className="inline-flex items-center gap-1">
                  <span className="text-gray-500">학원</span>
                  <span className="text-gray-800">#{notice.academyNumber}</span>
                </span>
              )}
              {notice.className && (
                <>
                  <span className="text-gray-300">•</span>
                  <span className="inline-flex items-center gap-1">
                    <span className="text-gray-500">반</span>
                    <span className="text-gray-800">{notice.className}</span>
                  </span>
                </>
              )}
              {notice.createdAt && (
                <>
                  <span className="text-gray-300">•</span>
                  <span className="inline-flex items-center gap-1">
                    <span className="text-gray-500">게시</span>
                    <span className="text-gray-800">{formatKST(notice.createdAt)}</span>
                  </span>
                </>
              )}
            </div>

            <div className="mt-5 whitespace-pre-wrap text-gray-900 leading-relaxed break-words">
              {notice.content}
            </div>

            {Array.isArray(imgList) && imgList.length > 0 && (
              <div className="mt-6 grid grid-cols-2 sm:grid-cols-3 gap-3">
                {imgList.map((src, i) => (
                  <a
                    key={i}
                    href={src}
                    target="_blank"
                    rel="noreferrer"
                    className="block group"
                    title="이미지 열기"
                  >
                    <img
                      src={src}
                      alt={`notice-image-${i}`}
                      className="w-full h-32 object-cover rounded-xl ring-1 ring-black/5 group-hover:opacity-95"
                      loading="lazy"
                    />
                  </a>
                ))}
              </div>
            )}
          </div>
        </article>
      )}
    </section>
  );
}
