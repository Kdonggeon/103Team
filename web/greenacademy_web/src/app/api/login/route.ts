import { NextResponse } from "next/server";

const API_BASE = process.env.LOGIN_API_BASE ?? "http://localhost:9090";

export async function POST(req: Request) {
  try {
    const body = await req.json(); // { username, password, fcmToken? }
    const payload = body;

    // 백엔드로 로그인 프록시
    const upstream = await fetch(`${API_BASE}/api/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      // ⚠️ server-to-server라 credentials는 쿠키 자동 전파와 무관합니다.
      // 여기서 중요한 건 응답의 Set-Cookie를 우리가 아래에서 브라우저로 전달하는 것!
      body: JSON.stringify(payload),
    });

    // 실패면 그대로 에러 바디/코드 전달
    const raw = await upstream.text();
    if (!upstream.ok) {
      return new NextResponse(raw || "로그인 실패", { status: upstream.status });
    }

    // 1) 백엔드가 세션/리프레시 등을 Set-Cookie로 준 경우 → 그대로 전달
    const setCookie = upstream.headers.get("set-cookie");
    // 일부 서버는 쿠키 여러 개를 다중 헤더로 내려줍니다.
    // next/server는 다중 쿠키를 append로 전달 가능.
    const res = new NextResponse(raw || "{}", {
      status: 200,
      headers: {
        "Content-Type": upstream.headers.get("Content-Type") || "application/json",
      },
    });

    if (setCookie) {
      // 단일 헤더에 다수 쿠키가 ;, , 로 섞여있을 수 있으니 안전하게 분리
      // (서버 구현에 따라 다름. 여러 개면 보통 여러 개의 set-cookie 헤더로 옴)
      const cookies = upstream.headers
        .getSetCookie?.() // Next 15에서는 getSetCookie가 있습니다(있으면 사용)
        ?? setCookie.split(/,(?=\s*[a-zA-Z0-9_\-]+=)/); // Fallback: 쿠키 경계 기준 split

      for (const ck of cookies) {
        res.headers.append("set-cookie", ck);
      }
    } else {
      // 2) 백엔드가 JSON으로 토큰을 내려주는 경우 → 우리 쪽에서 쿠키로 심기(선택)
      // ex) { token: "..." } 형태 지원
      try {
        const data = raw ? JSON.parse(raw) : {};
        if (data?.token) {
          // 필요 시 속성 조정: prod에선 Secure, SameSite=None 권장(HTTPS 필요)
          res.cookies.set("access_token", data.token, {
            httpOnly: true,
            sameSite: "lax",
            path: "/",
          });
        }
      } catch {
        // JSON이 아니라면 패스
      }
    }

    return res;
  } catch (e: any) {
    return new NextResponse(e?.message || "서버 통신 오류", { status: 500 });
  }
}
