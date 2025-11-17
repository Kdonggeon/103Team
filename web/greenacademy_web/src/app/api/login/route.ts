import { NextResponse } from "next/server";

// ✅ 우선순위
// 1) LOGIN_API_BASE (서버용, Vercel 같은 데서 http://13.217.211.242:9090 처럼 세팅)
// 2) NEXT_PUBLIC_API_BASE (프론트에서 쓰는 값 재활용)
const RAW_API_BASE =
  process.env.LOGIN_API_BASE ?? process.env.NEXT_PUBLIC_API_BASE ?? "";

// 뒤에 슬래시 여러 개 있으면 제거
const API_BASE = RAW_API_BASE.replace(/\/+$/, "");

export async function POST(req: Request) {
  try {
    if (!API_BASE) {
      return new NextResponse("API_BASE not configured", { status: 500 });
    }

    const body = await req.json(); // { username, password, fcmToken? }
    const payload = body;

    // 백엔드로 로그인 프록시
    const upstream = await fetch(`${API_BASE}/api/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    // 실패면 그대로 에러 바디/코드 전달
    const raw = await upstream.text();
    if (!upstream.ok) {
      return new NextResponse(raw || "로그인 실패", { status: upstream.status });
    }

    // 응답 생성
    const res = new NextResponse(raw || "{}", {
      status: 200,
      headers: {
        "Content-Type":
          upstream.headers.get("Content-Type") || "application/json",
      },
    });

    // 1) 백엔드가 Set-Cookie를 준 경우 → 그대로 전달
    const setCookieHeader = upstream.headers.get("set-cookie");
    if (setCookieHeader) {
      const cookies =
        (upstream.headers as any).getSetCookie?.() ??
        setCookieHeader.split(/,(?=\s*[a-zA-Z0-9_\-]+=)/);

      for (const ck of cookies) {
        res.headers.append("set-cookie", ck);
      }
    } else {
      // 2) 토큰을 JSON으로 내려주는 경우 → 여기서 쿠키 심기 (옵션)
      try {
        const data = raw ? JSON.parse(raw) : {};
        if (data?.token) {
          res.cookies.set("access_token", data.token, {
            httpOnly: true,
            sameSite: "lax",
            path: "/",
          });
        }
      } catch {
        // JSON 아니면 무시
      }
    }

    return res;
  } catch (e: any) {
    return new NextResponse(e?.message || "서버 통신 오류", { status: 500 });
  }
}
