"use client";

import React from "react";
import { useForm, FormProvider, useFormContext } from "react-hook-form";
import type { UseFormReturn } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";

/**
 * 역할별 회원가입 (학생/학부모/교사/원장)
 * - 백엔드 DTO/엔드포인트와 1:1 매핑
 * - 성공 시 /login 이동
 */

const RAW_BASE = (process.env.NEXT_PUBLIC_API_BASE || "").trim();
const API_BASE = "/backend";

console.log("[signup] API_BASE =", API_BASE);

type Role = "student" | "parent" | "teacher" | "director";

// 공통 유틸
const baseRequired = (label: string) =>
  z.string().min(1, { message: `${label}을(를) 입력하세요.` });

const phoneString = (label: string) =>
  baseRequired(label).regex(/^[0-9\-+() ]{7,20}$/i, {
    message: `${label} 형식이 올바르지 않습니다.`,
  });

// ===== 역할별 스키마 (백엔드 DTO 네이밍 맞춤) =====
const studentSchema = z.object({
  role: z.literal("student"),
  studentId: baseRequired("아이디"),
  studentPw: baseRequired("비밀번호").min(4, { message: "비밀번호는 최소 4자 이상" }),
  studentName: baseRequired("이름"),
  address: z.string().optional(),
  studentPhoneNumber: phoneString("전화번호"), // 통일
  school: z.string().optional(),
  grade: z.coerce.number().int().min(0).max(99).optional(),
  parentsNumber: z.string().optional(),
  gender: z.enum(["M", "F", "Other"]).optional(),
});

const parentSchema = z.object({
  role: z.literal("parent"),
  parentsId: baseRequired("아이디"),
  parentsPw: baseRequired("비밀번호").min(4),
  parentsName: baseRequired("이름"),
  parentsPhoneNumber: phoneString("전화번호"),
  parentsNumber: z.string().optional(),
  academyNumber: z.coerce.number().int().nonnegative().optional(), // 단일 숫자
});

const teacherSchema = z.object({
  role: z.literal("teacher"),
  teacherId: baseRequired("아이디"),
  teacherPw: baseRequired("비밀번호").min(4),
  teacherName: baseRequired("이름"),
  teacherPhoneNumber: phoneString("전화번호"),
  academyNumber: z.coerce.number().int().nonnegative().optional(), // 단일 숫자
});

// ✅ 원장은 학원번호 입력 안 함 (백엔드에서 자동 생성)
const directorSchema = z.object({
  role: z.literal("director"),
  directorId: baseRequired("아이디"),
  directorPw: baseRequired("비밀번호").min(4),
  directorName: baseRequired("이름"),
  directorPhoneNumber: phoneString("전화번호"),
});

const unionSchema = z.discriminatedUnion("role", [
  studentSchema,
  parentSchema,
  teacherSchema,
  directorSchema,
]);

type FormValues = z.infer<typeof unionSchema>;

const defaultValues: FormValues = {
  role: "student",
  studentId: "",
  studentPw: "",
  studentName: "",
  address: "",
  studentPhoneNumber: "",
  school: "",
  grade: 0,
  parentsNumber: undefined,
  gender: undefined,
} as FormValues;

// 서버로 보낼 payload (이미 스키마가 서버 DTO에 맞춰져 있으므로 얕은 복사)
export function payloadMapper(values: FormValues) {
  switch (values.role) {
    case "student":
      return {
        studentId: values.studentId,
        studentPw: values.studentPw,
        studentName: values.studentName,
        address: values.address ?? null,
        studentPhoneNumber: values.studentPhoneNumber,
        school: values.school ?? null,
        grade: values.grade ?? 0,
        parentsNumber: values.parentsNumber ?? null,
        gender: values.gender ?? null,
      };
    case "parent":
      return {
        parentsId: values.parentsId,
        parentsPw: values.parentsPw,
        parentsName: values.parentsName,
        parentsPhoneNumber: values.parentsPhoneNumber,
        parentsNumber: values.parentsNumber ?? null,
        academyNumber: values.academyNumber ?? 0,
      };
    case "teacher":
      return {
        teacherId: values.teacherId,
        teacherPw: values.teacherPw,
        teacherName: values.teacherName,
        teacherPhoneNumber: values.teacherPhoneNumber,
        academyNumber: values.academyNumber ?? 0,
      };
    case "director":
      // ✅ 학원번호는 백엔드에서 자동 생성
      return {
        username: values.directorId,
        password: values.directorPw,
        name: values.directorName,
        phone: values.directorPhoneNumber,
      };
  }
}

// 역할별 제출 URL (백엔드 직접 호출)
const submitUrlByRole: Record<Role, string> = {
  student: `${API_BASE}/api/students`,
  parent: `${API_BASE}/api/parents`,
  teacher: `${API_BASE}/api/teachers`,
  // ✅ 백엔드 컨트롤러: @RequestMapping("/api/signup/director")
  director: `${API_BASE}/api/signup/director`,
};

// ===== 공용 필드 컴포넌트 =====
function Field({
  name,
  label,
  type = "text",
  placeholder,
}: {
  name: keyof FormValues | string;
  label: string;
  type?: string;
  placeholder?: string;
}) {
  const {
    register,
    formState: { errors },
  } = useFormContext<FormValues>();
  const err = (errors as any)[name]?.message as string | undefined;
  return (
    <div className="flex flex-col gap-1">
      <label className="text-sm text-gray-200">{label}</label>
      <input
        {...register(name as any)}
        type={type}
        placeholder={placeholder}
        className={`w-full rounded-xl px-3 py-2 bg-gray-800 text-gray-100 outline-none ring-1 ring-gray-700 focus:ring-2 focus:ring-green-400 ${
          err ? "ring-red-500" : ""
        }`}
      />
      {err && <p className="text-xs text-red-400">{err}</p>}
    </div>
  );
}

function RoleFields() {
  const { watch, register } = useFormContext<FormValues>();
  const role = watch("role");

  if (role === "student") {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Field name="studentId" label="아이디" />
        <Field name="studentPw" label="비밀번호" type="password" />
        <Field name="studentName" label="이름" />
        <Field name="studentPhoneNumber" label="전화번호" placeholder="010-1234-5678" />
        <Field name="address" label="주소" />
        <Field name="school" label="학교" />
        <Field name="grade" label="학년" type="number" />
        <Field name="parentsNumber" label="학부모 번호(선택)" />
        <div className="flex flex-col gap-1">
          <label className="text-sm text-gray-200">성별</label>
          <select
            {...register("gender")}
            className="w-full rounded-xl px-3 py-2 bg-gray-800 text-gray-100 ring-1 ring-gray-700 focus:ring-2 focus:ring-green-400"
          >
            <option value="">선택 안 함</option>
            <option value="M">남</option>
            <option value="F">여</option>
            <option value="Other">기타</option>
          </select>
        </div>
      </div>
    );
  }

  if (role === "parent") {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Field name="parentsId" label="아이디" />
        <Field name="parentsPw" label="비밀번호" type="password" />
        <Field name="parentsName" label="이름" />
        <Field name="parentsPhoneNumber" label="전화번호" placeholder="010-1234-5678" />
        {/* <Field name="parentsNumber" label="학부모 번호(선택)" /> */}
        <Field name="academyNumber" label="학원 번호" type="number" />
      </div>
    );
  }

  if (role === "teacher") {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Field name="teacherId" label="아이디" />
        <Field name="teacherPw" label="비밀번호" type="password" />
        <Field name="teacherName" label="이름" />
        <Field name="teacherPhoneNumber" label="전화번호" placeholder="010-1234-5678" />
        <Field name="academyNumber" label="학원 번호" type="number" />
      </div>
    );
  }

  // director (학원번호 입력 없음)
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <Field name="directorId" label="아이디" />
      <Field name="directorPw" label="비밀번호" type="password" />
      <Field name="directorName" label="이름" />
      <Field name="directorPhoneNumber" label="전화번호" placeholder="010-1234-5678" />
      {/* 학원번호는 백엔드에서 자동 생성하므로 필드 없음 */}
    </div>
  );
}

export default function RoleBasedSignupPage() {
  const router = useRouter();
  const [role, setRole] = React.useState<Role>("student");
  const [msg, setMsg] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);

  const schemaForRole = React.useMemo(() => {
    switch (role) {
      case "student":
        return studentSchema;
      case "parent":
        return parentSchema;
      case "teacher":
        return teacherSchema;
      case "director":
        return directorSchema;
    }
  }, [role]);

  const methods = useForm<FormValues>({
    resolver: zodResolver(schemaForRole as any),
    defaultValues: { ...defaultValues, role } as any,
    mode: "onChange",
  });

  React.useEffect(() => {
    methods.reset({ ...(defaultValues as any), role });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [role]);

  async function onSubmit(values: FormValues) {
    setMsg(null);
    setLoading(true);
    try {
      const payload = payloadMapper(values);
      const url = submitUrlByRole[values.role];
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
        credentials: "include",
      });
      const text = await res.text();
      if (!res.ok) throw new Error(text || "회원가입 실패");

      setMsg("회원가입이 완료되었습니다.");
      router.replace("/login");
    } catch (e: any) {
      setMsg(e?.message || "오류가 발생했습니다");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100 flex items-center justify-center p-4">
      <div className="w-full max-w-3xl bg-gray-850/60 backdrop-blur rounded-2xl shadow-2xl p-6 md:p-8 ring-1 ring-gray-800">
        <h1 className="text-2xl md:text-3xl font-bold mb-4">역할별 회원가입</h1>

        {/* 역할 토글 */}
        <div className="flex flex-wrap gap-2 mb-6">
          {(["student", "parent", "teacher", "director"] as Role[]).map((r) => (
            <button
              key={r}
              onClick={() => setRole(r)}
              className={`px-4 py-2 rounded-full text-sm font-medium ring-1 ring-gray-700 hover:ring-green-400 ${
                role === r ? "bg-green-500 text-black" : "bg-gray-800"
              }`}
            >
              {r === "student"
                ? "학생"
                : r === "parent"
                ? "학부모"
                : r === "teacher"
                ? "교사"
                : "원장"}
            </button>
          ))}
        </div>

        <FormProvider {...methods}>
          <form onSubmit={methods.handleSubmit(onSubmit)} className="flex flex-col gap-6">
            <input type="hidden" {...methods.register("role")} value={role} readOnly />
            <RoleFields />
            <button
              type="submit"
              disabled={loading}
              className="w-full md:w-auto self-end bg-green-500 text-black font-semibold px-6 py-3 rounded-xl hover:brightness-110 disabled:opacity-60"
            >
              {loading ? "처리 중..." : "회원가입"}
            </button>
          </form>
        </FormProvider>

        {msg && (
          <div className="mt-4 text-sm rounded-lg p-3 bg-gray-800 ring-1 ring-gray-700">
            {msg}
          </div>
        )}
      </div>
    </div>
  );
}

// ──────────────────────────────────────────────────────────────
// Lightweight Dev Tests (won't run in production)
// ──────────────────────────────────────────────────────────────
if (process.env.NODE_ENV !== "production") {
  try {
    const s = payloadMapper({
      role: "student",
      studentId: "s1",
      studentPw: "pw",
      studentName: "홍길동",
      address: "서울",
      studentPhoneNumber: "010-1111-2222",
      school: "GA",
      grade: 1,
      parentsNumber: "P-1",
      gender: "M",
    } as any);
    console.assert("studentId" in s && "studentPhoneNumber" in s, "student payload shape ok");

    const p = payloadMapper({
      role: "parent",
      parentsId: "p1",
      parentsPw: "pw",
      parentsName: "부모",
      parentsPhoneNumber: "010-3333-4444",
      parentsNumber: "PN-1",
      academyNumber: 101,
    } as any);
    console.assert("parentsId" in p && typeof p.academyNumber === "number", "parent payload shape ok");

    const t = payloadMapper({
      role: "teacher",
      teacherId: "t1",
      teacherPw: "pw",
      teacherName: "선생님",
      teacherPhoneNumber: "010-5555-6666",
      academyNumber: 201,
    } as any);
    console.assert("teacherId" in t && typeof t.academyNumber === "number", "teacher payload shape ok");

    const d = payloadMapper({
      role: "director",
      directorId: "d1",
      directorPw: "pw",
      directorName: "원장",
      directorPhoneNumber: "010-7777-8888",
    } as any);
    console.assert(
      "username" in d && !("academyNumbers" in d),
      "director payload shape ok (auto academyNumber)"
    );
  } catch (e) {
    console.warn("[dev-tests] payloadMapper tests failed:", e);
  }
}
