"use client";

import React from "react";

export default function Modal({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean;
  title?: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  if (!open) return null;

  React.useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-[100]">
      {/* backdrop */}
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      {/* dialog */}
      <div className="absolute inset-0 flex items-center justify-center p-4">
        <div
          role="dialog"
          aria-modal="true"
          className="w-full max-w-lg rounded-2xl bg-white shadow-xl ring-1 ring-black/10 overflow-hidden"
        >
          <div className="flex items-center justify-between px-4 py-3 border-b">
            <h3 className="text-base font-semibold text-gray-900">
              {title ?? "Dialog"}
            </h3>
            <button
              onClick={onClose}
              className="rounded-lg px-2 py-1 text-sm text-gray-600 hover:bg-gray-100"
              aria-label="닫기"
            >
              ✕
            </button>
          </div>
          <div className="p-4">{children}</div>
        </div>
      </div>
    </div>
  );
}
