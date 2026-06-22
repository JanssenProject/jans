import * as React from 'react';
import { Tag } from './Tag';
// ── Dropdown (ACR Values) ──────────────────────────────────────────────────────

type Option = { name: string; label?: string; create?: boolean };

export const Dropdown = ({
  options,
  selected,
  onSelect,
  placeholder = 'Select Here',
}: {
  options: Option[];
  selected: Option | null;
  onSelect: (opt: Option | null) => void;
  placeholder?: string;
}) => {
  const [open, setOpen] = React.useState(false);
  const [input, setInput] = React.useState('');
  const ref = React.useRef<HTMLDivElement>(null);

  // Close on outside click
  React.useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const filtered = options.filter((o) =>
    o.name.toLowerCase().includes(input.toLowerCase())
  );

  // Allow custom typed value
  const showCustom =
    input.trim() !== '' && !options.some((o) => o.name === input.trim());

  const handleSelect = (opt: Option) => {
    onSelect(opt);
    setInput('');
    setOpen(false);
  };

  return (
    <div ref={ref} className="relative w-full">
      <div
        onClick={() => setOpen((v) => !v)}
        className="flex items-center justify-between w-full border border-slate-200 rounded-lg px-4 py-3
          text-sm text-slate-700 bg-slate-50/60 cursor-pointer
          focus-within:ring-2 focus-within:ring-[#1a6b3c]/30 focus-within:border-[#1a6b3c] transition-all"
      >
        {selected ? (
          <span className="flex items-center gap-2">
            <Tag name={selected.name} onRemove={() => { onSelect(null); setInput(''); }} />
          </span>
        ) : (
          <input
            type="text"
            value={input}
            onChange={(e) => { setInput(e.target.value); setOpen(true); }}
            placeholder={placeholder}
            className="bg-transparent flex-1 outline-none placeholder-slate-400 text-sm"
            onClick={(e) => e.stopPropagation()}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && input.trim()) {
                handleSelect({ name: input.trim(), create: true });
              }
            }}
          />
        )}
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
          className={`w-4 h-4 text-slate-400 flex-shrink-0 transition-transform ${open ? 'rotate-180' : ''}`}>
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </div>

      {open && (
        <ul className="absolute z-30 mt-1 w-full bg-white border border-slate-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
          {filtered.length === 0 && !showCustom && (
            <li className="px-4 py-3 text-sm text-slate-400">No options</li>
          )}
          {filtered.map((opt) => (
            <li
              key={opt.name}
              onClick={() => handleSelect(opt)}
              className="px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50 cursor-pointer"
            >
              {opt.name}
            </li>
          ))}
          {showCustom && (
            <li
              onClick={() => handleSelect({ name: input.trim(), create: true })}
              className="px-4 py-2.5 text-sm text-[#1a6b3c] font-medium hover:bg-slate-50 cursor-pointer"
            >
              Add &quot;{input.trim()}&quot;
            </li>
          )}
        </ul>
      )}
    </div>
  );
};