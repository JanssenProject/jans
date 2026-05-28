import * as React from 'react';

// ── Tag pill (for scope tags) ──────────────────────────────────────────────────

export const Tag = ({ name, onRemove }: { name: string; onRemove: () => void }) => (
  <span className="inline-flex items-center gap-1 bg-[#d1fae5] text-[#065f46] text-xs font-semibold px-2.5 py-1 rounded-full">
    {name}
    <button type="button" onClick={(e) => {
      e.stopPropagation();
      onRemove();
    }} className="hover:text-red-500 transition-colors leading-none ml-0.5">×</button>
  </span>
);
