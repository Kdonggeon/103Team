import { NextResponse } from "next/server";

/**
 * 백엔드 베이스 URL
 * 1순위: LOGIN_API_BASE (서버용, Vercel에 http://13.217.211.242:9090 처럼 세팅)
 * 2순위: NEXT_PUBLIC_API_BASE (없으면)
 * 3순위: 개발용 localhost:9090
 */
export const API_BASE =
  process.env.LOGIN_API_BASE ??
  process.env.NEXT_PUBLIC_API_BASE ??
  "http://localhost:9090";

export async function proxyJsonPost(upstreamPath: string, req: Request) {
  try {
    const bodyText = await req.text(); // 요청 바디 그대로 전달

    const upstream = await fetch(`${API_BASE}${upstreamPath}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: bodyText,
      credentials: "include",
    });

    const text = await upstream.text();
    if (!upstream.ok) {
      return new NextResponse(text || "요청 실패", { status: upstream.status });
    }

    // JSON/텍스트 모두 처리
    try {
      return NextResponse.json(text ? JSON.parse(text) : {});
    } catch {
      return new NextResponse(text);
    }
  } catch (e: any) {
    return new NextResponse(e?.message || "프록시 오류", { status: 500 });
  }
}
