// CedarlingMgmt.tsx - Main component
import React from 'react';
import AddCedarlingConfig from './AddCedarlingConfig';
import UnsignedAuthzForm from './UnsignedAuthzForm';
import MultiIssuerAuthzForm from './MultiIssuerAuthzForm';
import HelpDrawer from '../../../options/helpDrawer';
import { JsonEditor } from 'json-edit-react';
import { Pencil, Trash2, Plus, ChevronRight } from 'lucide-react';
import { SuccessAlert } from '../../../shared/components/Common';
function Row(props: { row: any; rowIndex: number; notifyOnDataChange: () => void }) {
  const { row, rowIndex, notifyOnDataChange } = props;
  const [open, setOpen] = React.useState(false);
  const [expanded, setExpanded] = React.useState(false);

  const handleDialog = (isOpen: boolean) => {
    setOpen(isOpen);
    notifyOnDataChange();
  };

  async function resetBootstrap() {
    chrome.storage.local.get(['cedarlingConfig'], (result) => {
      const cedarlingConfigArr = Array.isArray(result.cedarlingConfig)
        ? result.cedarlingConfig.filter((_: unknown, index: number) => index !== rowIndex)
        : [];
      chrome.storage.local.set({ cedarlingConfig: cedarlingConfigArr }, () => {
        notifyOnDataChange();
      });
    });
  }

  return (
    <>
      <AddCedarlingConfig isOpen={open} handleDialog={handleDialog} newData={row} />
      <tr className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
        <td className="px-4 py-3 w-10" colSpan={2}>
          <button
            onClick={() => setExpanded(!expanded)}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <ChevronRight
              size={16}
              className={`transition-transform duration-200 ${expanded ? 'rotate-90' : ''}`}
            />
          </button>
        </td>
        <td className="px-4 py-3 flex-1" colSpan={2}>
          {expanded ? (
            <div className="json-editor-custom">
              <JsonEditor
                data={row}
                restrictTypeSelection={true}
                collapse={false}
                restrictEdit={true}
                restrictDelete={true}
                restrictAdd={true}
                rootName="bootstrapConfig"
                theme={{
                  container: {
                    backgroundColor: '#f9fafb',
                    borderRadius: '8px',
                    padding: '16px',
                    fontFamily: 'ui-monospace, monospace',
                  },
                  property: {
                    color: '#374151',
                    fontSize: '14px',
                  },
                  bracket: {
                    color: '#6b7280',
                  },
                  values: {
                    string: {
                      color: '#059669',
                      fontSize: '14px',
                    },
                    number: {
                      color: '#2563eb',
                    },
                    boolean: {
                      color: '#dc2626',
                    },
                  },
                  iconEdit: {
                    color: '#6b7280',
                  },
                  iconDelete: {
                    color: '#6b7280',
                  },
                  iconAdd: {
                    color: '#6b7280',
                  },
                } as any}
              />
            </div>
          ) : (
            <span className="text-sm text-gray-700 font-mono">
              bootstrapConfig:{' '}
              <span className="text-green-600 font-medium">
                {'{ '}
                {Object.keys(row || {}).length} items{' }'}
              </span>
            </span>
          )}
        </td>
        <td className="px-4 py-3 w-24 text-right">
          <div className="flex items-center justify-end gap-2">
            <button
              onClick={() => { setOpen(true); notifyOnDataChange(); }}
              className="p-1.5 text-gray-500 hover:text-gray-800 rounded transition-colors"
              title="Edit"
            >
              <Pencil size={16} />
            </button>
            <button
              onClick={resetBootstrap}
              className="p-1.5 text-gray-500 hover:text-red-500 rounded transition-colors"
              title="Delete"
            >
              <Trash2 size={16} />
            </button>
          </div>
        </td>
      </tr>
      {/* Custom CSS for json-edit-react */}
      <style>{`
        .json-editor-custom :global(.jer-editor-container) {
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
        }
        
        .json-editor-custom :global(.jer-value-string) {
          color: #059669 !important;
        }
        
        .json-editor-custom :global(.jer-icon-button) {
          opacity: 0.6;
          transition: opacity 0.2s;
        }
        
        .json-editor-custom :global(.jer-icon-button:hover) {
          opacity: 1;
        }

        .json-editor-custom :global(.jer-input-value) {
          border: 1px solid #d1d5db;
          border-radius: 4px;
          padding: 4px 8px;
          font-size: 14px;
        }

        .json-editor-custom :global(.jer-input-value:focus) {
          outline: none;
          border-color: #10b981;
          box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
        }
      `}</style>
    </>
  );
}

interface CedarlingMgmtProps {
  data: any;
  notifyOnDataChange: () => void;
  isLoggedIn: boolean;
}

type TabType = 'config' | 'unsignedAuthz' | 'multiIssuerAuthz';

export default function CedarlingMgmt({ data, notifyOnDataChange, isLoggedIn }: CedarlingMgmtProps) {
  const [modelOpen, setModelOpen] = React.useState(false);
  const [drawerOpen, setDrawerOpen] = React.useState(false);
  const [screenType, setScreenType] = React.useState<TabType>('config');
  const [cedarlingConfig, setCedarlingConfig] = React.useState<any[]>([]);
  const [page, setPage] = React.useState(0);
  const rowsPerPage = 10;

  React.useEffect(() => {
    setCedarlingConfig(data?.cedarlingConfig ?? []);
  }, [data]);

  const handleDialog = (isOpen: boolean) => {
    setModelOpen(isOpen);
    notifyOnDataChange();
  };

  const isEmpty = !cedarlingConfig || cedarlingConfig.length === 0;
  const totalRows = cedarlingConfig?.length ?? 0;
  const pagedRows = cedarlingConfig?.slice(page * rowsPerPage, (page + 1) * rowsPerPage) ?? [];

  const tabs: { key: TabType; label: string }[] = [
    { key: 'config', label: 'Bootstrap Configuration' },
    { key: 'unsignedAuthz', label: 'Cedarling Unsigned Authz Form' },
    { key: 'multiIssuerAuthz', label: 'Cedarling Multi-Issuer Authz Form' },
  ];

  return (
    <div className="rounded-2xl bg-white p-8 shadow-sm border border-gray-100">
      <AddCedarlingConfig isOpen={modelOpen} handleDialog={handleDialog} newData={{}} />
      <HelpDrawer isOpen={drawerOpen} handleDrawer={setDrawerOpen} />

      {/* Header */}
      {screenType === 'config' && (
        <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              Bootstrap Configuration
            </h1>
            <p className="mt-2 text-sm text-gray-500">
              Configure initial setup and connection parameters for your system.
            </p>
          </div>
          {isEmpty && (
            <button
              onClick={() => setModelOpen(true)}
              className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-emerald-700"
            >
              <Plus size={16} />
              Add Configurations
            </button>
          )}
        </div>
      )}

      {/* Tabs — only show when config exists */}
      {!isEmpty && (
        <div className="flex rounded-lg border border-gray-200 overflow-hidden mb-6 w-fit">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setScreenType(tab.key)}
              className={`px-5 py-2.5 text-sm font-medium transition-colors whitespace-nowrap ${screenType === tab.key
                ? 'bg-green-600 text-white'
                : 'bg-white text-gray-600 hover:bg-gray-50'
                }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      )}

      {/* Config Table */}
      {screenType === 'config' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          {isEmpty ? (
            <div className="p-6">
              <SuccessAlert>
                No bootstrap configuration yet.
                <span className="font-medium">
                  {' '}
                  Add configuration
                </span>{' '}
                to get started.
              </SuccessAlert>
            </div>
          ) : (
            <>
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr className="border-b border-gray-200">
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600" colSpan={2} />
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600" colSpan={2}>
                      Bootstrap Configuration
                    </th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                      Action
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {isEmpty ? (
                    <tr>
                      <td colSpan={5} className="p-6">
                        <SuccessAlert>
                          No bootstrap configuration yet.
                          <span className="font-medium">
                            {' '}
                            Add configuration
                          </span>{' '}
                          to get started.
                        </SuccessAlert>
                      </td>
                    </tr>
                  ) : (
                    pagedRows.map((row, index) => (
                      <Row
                        key={index}
                        rowIndex={page * rowsPerPage + index}
                        row={row}
                        notifyOnDataChange={notifyOnDataChange}
                      />
                    ))
                  )}
                </tbody>
              </table>
              {/* Pagination */}
              <div className="flex items-center justify-center gap-4 px-4 py-3 border-t border-gray-100 text-sm text-gray-600">
                <span>Rows per page:</span>
                <span className="font-medium">{rowsPerPage} ▾</span>
                <span>
                  {page * rowsPerPage + 1}–{Math.min((page + 1) * rowsPerPage, totalRows)} of {totalRows}
                </span>
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="p-1 rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  ‹
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={(page + 1) * rowsPerPage >= totalRows}
                  className="p-1 rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  ›
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {!isEmpty && screenType === 'unsignedAuthz' && <UnsignedAuthzForm data={data} />}
      {!isEmpty && screenType === 'multiIssuerAuthz' && <MultiIssuerAuthzForm data={data} />}
    </div>
  );
}