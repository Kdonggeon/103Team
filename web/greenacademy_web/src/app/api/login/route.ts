import { NextResponse } from "next/server";

// ✅ 우선순위
// 1) LOGIN_API_BASE (서버용, Vercel에 이걸 `http://13.217.211.242:9090` 같은 걸로 세팅)
// 2) NEXT_PUBLIC_API_BASE (프론트에서 쓰는 값 재활용)
// 3) 없으면 개발용 localhost:9090
const API_BASE =
  process.env.LOGIN_API_BASE ??
  process.env.NEXT_PUBLIC_API_BASE ;

export async function POST(req: Request) {
  try {
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

    // 1) 백엔드가 세션/리프레시 등을 Set-Cookie로 준 경우 → 그대로 전달
    const setCookie = upstream.headers.get("set-cookie");
    const res = new NextResponse(raw || "{}", {
      status: 200,
      headers: {
        "Content-Type":
          upstream.headers.get("Content-Type") || "application/json",
      },
    });

    if (setCookie) {
      const cookies =
        upstream.headers.getSetCookie?.() ??
        setCookie.split(/,(?=\s*[a-zA-Z0-9_\-]+=)/);

      for (const ck of cookies) {
        res.headers.append("set-cookie", ck);
      }
    } else {
      // 2) 토큰 JSON으로 내려주는 경우 → 우리 쪽에서 쿠키로 심기
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
