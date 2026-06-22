import React from 'react';
import { Info } from "lucide-react";
import { CircleAlert } from 'lucide-react';

/** Tooltip label helper */
export const LabelWithTooltip = ({ label, tip }: { label: string; tip: string }) => {
  const [show, setShow] = React.useState(false);
  return (
    <span className="flex items-center gap-1.5 text-sm font-semibold text-[#1a3a2a]">
      {label}
      <span className="relative inline-flex items-center">
        <button
          tabIndex={-1}
          type="button"
          onMouseEnter={() => setShow(true)}
          onMouseLeave={() => setShow(false)}
          className="text-slate-400 hover:text-slate-600 transition-colors"
        >
          <CircleAlert className="w-4 h-4" />
        </button>
        {show && (
          <span className="absolute z-50 left-6 top-1/2 -translate-y-1/2 w-64 bg-[#1a3a2a] text-white text-xs rounded-lg px-3 py-2.5 shadow-xl leading-relaxed pointer-events-none">
            {tip}
          </span>
        )}
      </span>
    </span>
  );
};

export const LabelWithTooltipLeft = ({ label, tip }: { label: string; tip: string }) => {
  const [show, setShow] = React.useState(false);
  return (
    <span className="flex items-center gap-1.5 text-sm font-semibold text-[#1a3a2a]">
      {label}
      <span className="relative inline-flex items-center">
        <button
          tabIndex={-1}
          type="button"
          onMouseEnter={() => setShow(true)}
          onMouseLeave={() => setShow(false)}
          className="text-slate-400 hover:text-slate-600 transition-colors"
        >
          <CircleAlert className="w-4 h-4" />
        </button>
        {show && (
          <span className="absolute z-50 right-full top-1/2 -translate-y-1/2 mr-2 w-64 bg-[#1a3a2a] text-white text-xs rounded-lg px-3 py-2.5 shadow-xl leading-relaxed pointer-events-none">
            {tip}
          </span>
        )}
      </span>
    </span>
  );
};

/** Animated spinner */
export const Spinner = () => (
  <div className="absolute inset-0 z-10 flex items-center justify-center bg-white/70 rounded-2xl">
    <svg
      className="animate-spin w-9 h-9 text-[#22a05a]"
      viewBox="0 0 24 24"
      fill="none"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
    </svg>
  </div>
);

/* -------------------------------------------------------------------------- */
/* Empty State Alert                                                          */
/* -------------------------------------------------------------------------- */

export const SuccessAlert = ({ children }: { children: React.ReactNode }) => {
  return (
    <div className="flex items-start gap-3 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
      <Info className="w-4 h-4 mt-0.5 shrink-0" />
      <div>{children}</div>
    </div>
  );
}