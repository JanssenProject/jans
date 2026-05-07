import * as React from 'react';
import { Tag } from './Tag';

type Option = { name: string; label?: string; create?: boolean };

export const TagInput = ({
  tags,
  onAdd,
  onRemove,
  placeholder = 'Enter Here',
}: {
  tags: Option[];
  onAdd: (name: string) => void;
  onRemove: (name: string) => void;
  placeholder?: string;
}) => {
  const [input, setInput] = React.useState('');
  const inputRef = React.useRef<HTMLInputElement>(null);

  const commit = () => {
    if (input.trim()) { onAdd(input.trim()); setInput(''); }
  };

  return (
    <div
      className="min-h-[48px] w-full border border-slate-200 rounded-lg px-3 py-2 flex flex-wrap gap-1.5
        focus-within:ring-2 focus-within:ring-[#1a6b3c]/30 focus-within:border-[#1a6b3c]
        transition-all bg-slate-50/60 cursor-text"
        onClick={() => inputRef.current?.focus()}
    >
      {tags.map((t) => (
        <Tag key={t.name} name={t.name} onRemove={() => onRemove(t.name)} />
      ))}
      <input
        ref={inputRef}
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); commit(); }
          if (e.key === 'Backspace' && !input && tags.length) onRemove(tags[tags.length - 1].name);
        }}
        onBlur={commit}
        placeholder={tags.length === 0 ? placeholder : ''}
        className="flex-1 min-w-[120px] bg-transparent text-sm text-slate-700 placeholder-slate-400 focus:outline-none py-0.5"
      />
    </div>
  );
};