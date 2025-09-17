// src/lib/routeByRole.ts

export type Role = "student" | "parent" | "teacher" | "director";

const ROUTES: Record<Role, string> = {
  student: "/student",
  parent: "/parent",
  teacher: "/teacher",
  director: "/director",
};

export function isRole(value: string): value is Role {
  return (["student", "parent", "teacher", "director"] as string[]).includes(value);
}

/** 역할에 맞는 홈 경로 반환 (알 수 없는 값이면 "/") */
export function routeByRole(role: string): string {
  return isRole(role) ? ROUTES[role] : "/";
}
