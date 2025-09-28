import { proxyJsonPost } from "../../_utils";

export async function POST(req: Request) {
  return proxyJsonPost("/api/signup/student", req);
}
