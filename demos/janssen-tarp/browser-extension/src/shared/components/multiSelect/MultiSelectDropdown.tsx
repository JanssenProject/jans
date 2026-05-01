import * as React from 'react';
import { Tag } from '../Tag';

type Option = { name: string; label?: string; create?: boolean };

export const MultiSelectDropdown = ({
  options,
  selected,
  onChange,
  placeholder = "Select scopes",
}: {
  options: Option[];
  selected: Option[];
  onChange: (value: Option[]) => void;
  placeholder?: string;
}) => {
  const [open, setOpen] = React.useState(false);
  const ref = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const toggleOption = (opt: Option) => {
    const exists = selected.some((s) => s.name === opt.name);

    if (exists) {
      onChange(selected.filter((s) => s.name !== opt.name));
    } else {
      onChange([...selected, opt]);
    }
  };

  const removeTag = (name: string) => {
    onChange(selected.filter((s) => s.name !== name));
  };

  return (
    <div ref={ref} className="relative w-full">
      {/* Trigger */}
      <div
        tabIndex={0}
        role="listbox"
        onClick={() => setOpen((v) => !v)}
        className="min-h-[48px] w-full border border-slate-200 rounded-lg px-3 py-2 flex flex-wrap gap-1.5
        bg-slate-50/60 cursor-pointer
        focus-within:ring-2 focus-within:ring-[#1a6b3c]/30 focus-within:border-[#1a6b3c]"
      >
        {selected.length > 0 ? (
          selected.map((s) => (
            <Tag key={s.name} name={s.name} onRemove={() => removeTag(s.name)} />
          ))
        ) : (
          <span className="text-sm text-slate-400">{placeholder}</span>
        )}

        <span className="ml-auto self-center">
          <svg
            viewBox="0 0 24 24"
            className={`w-4 h-4 text-slate-400 transition-transform ${
              open ? "rotate-180" : ""
            }`}
          >
            <polyline
              points="6 9 12 15 18 9"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            />
          </svg>
        </span>
      </div>

      {/* Dropdown */}
      {open && (
        <ul className="absolute z-30 mt-1 w-full bg-white border border-slate-200 rounded-lg shadow-lg max-h-56 overflow-y-auto">
          {options.map((opt) => {
            const isSelected = selected.some((s) => s.name === opt.name);

            return (
              <li
                key={opt.name}
                onClick={() => toggleOption(opt)}
                className="flex items-center justify-between px-4 py-2.5 text-sm cursor-pointer hover:bg-slate-50"
              >
                <span className="text-slate-700">{opt.name}</span>

                {isSelected && (
                  <svg
                    viewBox="0 0 20 20"
                    className="w-4 h-4 text-[#22a05a]"
                  >
                    <path
                      d="M5 10l3 3 7-7"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                    />
                  </svg>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};