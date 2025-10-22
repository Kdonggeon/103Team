// src/components/ui/Panel.tsx
"use client";
import type { ReactNode } from "react";

/** 간단한 클래스 병합 유틸 (clsx 대체) */
function cx(...classes: Array<string | false | null | undefined>) {
  return classes.filter(Boolean).join(" ");
}

export default function Panel({
  title,
  right,
  children,
  className,
}: {
  title?: ReactNode;
  right?: ReactNode;   // 우측 액션(버튼, 링크 등)
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cx("rounded-2xl bg-white ring-1 ring-black/5 shadow-sm", className)}>
      {(title || right) && (
        <header className="flex items-center justify-between px-4 py-3 border-b">
          <h2 className="text-sm font-semibold text-gray-900">{title}</h2>
          {right}
        </header>
      )}
      <div className="p-4">{children}</div>
    </section>
  );
}

export function PanelGrid({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return <div className={cx("grid gap-4 lg:grid-cols-2", className)}>{children}</div>;
}
