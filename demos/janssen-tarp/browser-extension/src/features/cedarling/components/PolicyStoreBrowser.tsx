import React from 'react';
import { X, ChevronRight, Folder, FileText, RefreshCw } from 'lucide-react';
import {
  PolicyStoreEntry,
  TreeNode,
  getPolicyStoreUri,
  downloadAndExtractPolicyStore,
  buildTree,
  decodeText,
  isTextEntry,
  formatSize,
} from '../services/policyStoreService';

interface PolicyStoreBrowserProps {
  isOpen: boolean;
  config: any;
  onClose: () => void;
}

function TreeItem(props: {
  node: TreeNode;
  depth: number;
  selectedPath: string;
  onSelect: (entry: PolicyStoreEntry) => void;
}) {
  const { node, depth, selectedPath, onSelect } = props;
  const [expanded, setExpanded] = React.useState(true);

  if (node.isDir) {
    return (
      <div>
        <button
          onClick={() => setExpanded(!expanded)}
          className="flex items-center gap-1.5 w-full px-2 py-1.5 text-sm text-gray-700 hover:bg-gray-100 rounded transition-colors"
          style={{ paddingLeft: `${depth * 14 + 8}px` }}
        >
          <ChevronRight
            size={14}
            className={`text-gray-400 transition-transform duration-200 ${expanded ? 'rotate-90' : ''}`}
          />
          <Folder size={14} className="text-amber-500" />
          <span className="font-medium truncate">{node.name}</span>
        </button>
        {expanded &&
          node.children.map((child) => (
            <TreeItem
              key={child.path}
              node={child}
              depth={depth + 1}
              selectedPath={selectedPath}
              onSelect={onSelect}
            />
          ))}
      </div>
    );
  }

  const isSelected = node.path === selectedPath;
  return (
    <button
      onClick={() => node.entry && onSelect(node.entry)}
      className={`flex items-center gap-1.5 w-full px-2 py-1.5 text-sm rounded transition-colors ${
        isSelected ? 'bg-emerald-50 text-emerald-700' : 'text-gray-600 hover:bg-gray-100'
      }`}
      style={{ paddingLeft: `${depth * 14 + 22}px` }}
    >
      <FileText size={14} className={isSelected ? 'text-emerald-600' : 'text-gray-400'} />
      <span className="truncate">{node.name}</span>
    </button>
  );
}

export default function PolicyStoreBrowser({ isOpen, config, onClose }: PolicyStoreBrowserProps) {
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState('');
  const [entries, setEntries] = React.useState<PolicyStoreEntry[]>([]);
  const [selected, setSelected] = React.useState<PolicyStoreEntry | null>(null);

  const uri = getPolicyStoreUri(config);

  const load = React.useCallback(async () => {
    if (!uri) {
      setError('No CEDARLING_POLICY_STORE_URI found in this configuration.');
      return;
    }
    setLoading(true);
    setError('');
    setEntries([]);
    setSelected(null);
    try {
      const result = await downloadAndExtractPolicyStore(uri);
      if (result.length === 0) {
        setError('The policy store archive is empty.');
      }
      setEntries(result);
      // Auto-select metadata.json or the first file for convenience.
      const initial = result.find((e) => e.path.endsWith('metadata.json')) ?? result[0];
      if (initial) setSelected(initial);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, [uri]);

  React.useEffect(() => {
    if (isOpen) load();
  }, [isOpen, load]);

  if (!isOpen) return null;

  const tree = buildTree(entries);

  return (
    <div className="fixed inset-0 bg-black/40 z-40 flex items-center justify-center backdrop-blur-sm">
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-5xl mx-4 overflow-hidden flex flex-col h-[80vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div className="min-w-0">
            <h2 className="text-xl font-bold text-[#002B49]">Policy Store</h2>
            <p className="text-xs text-gray-500 mt-0.5 truncate" title={uri}>
              {uri || 'No policy store URI'}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={load}
              disabled={loading || !uri}
              className="p-2 text-gray-500 hover:text-gray-800 rounded transition-colors disabled:opacity-40"
              title="Reload"
            >
              <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
            </button>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
              <X size={24} />
            </button>
          </div>
        </div>

        {/* Body */}
        {error ? (
          <div className="flex-1 flex items-center justify-center p-8">
            <p className="text-red-500 text-sm font-medium bg-red-50 p-4 rounded-lg border border-red-100 max-w-lg text-center">
              {error}
            </p>
          </div>
        ) : loading ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="w-10 h-10 border-4 border-[#00B06E] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="flex-1 flex min-h-0">
            {/* Tree pane */}
            <div className="w-72 border-r border-gray-100 overflow-y-auto p-2 bg-gray-50/50 shrink-0">
              {tree.children.map((child) => (
                <TreeItem
                  key={child.path}
                  node={child}
                  depth={0}
                  selectedPath={selected?.path ?? ''}
                  onSelect={setSelected}
                />
              ))}
            </div>

            {/* File viewer pane */}
            <div className="flex-1 flex flex-col min-w-0">
              {selected ? (
                <>
                  <div className="flex items-center justify-between px-4 py-2.5 border-b border-gray-100 bg-white">
                    <span className="text-sm font-mono text-gray-700 truncate">{selected.path}</span>
                    <span className="text-xs text-gray-400 shrink-0 ml-3">{formatSize(selected.size)}</span>
                  </div>
                  <div className="flex-1 overflow-auto p-4">
                    {isTextEntry(selected) ? (
                      <pre className="text-xs font-mono text-gray-800 whitespace-pre-wrap break-words">
                        {decodeText(selected)}
                      </pre>
                    ) : (
                      <p className="text-sm text-gray-500 italic">
                        Binary file ({formatSize(selected.size)}) — preview not available.
                      </p>
                    )}
                  </div>
                </>
              ) : (
                <div className="flex-1 flex items-center justify-center text-sm text-gray-400">
                  Select a file to view its contents.
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
