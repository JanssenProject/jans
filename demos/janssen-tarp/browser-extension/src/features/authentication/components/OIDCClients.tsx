import * as React from 'react';
import {
  Trash2,
  Plus,
  ChevronLeft,
  ChevronRight,
  Zap,
} from 'lucide-react';

import RegisterClient from './RegisterClient';
import AuthFlowInputs from './AuthFlowInputs';
import { ClientDetails } from '../type/Authentication';
import { SuccessAlert } from '../../../shared/components/Common';

/* -------------------------------------------------------------------------- */
/* Types                                                                      */
/* -------------------------------------------------------------------------- */

export type OIDCClient = {
  opHost: string;
  clientId: string;
  clientSecret: string;
  showClientExpiry: boolean;
  expireAt: number;
  scope: string;
  redirectUris: string[];
  authorizationEndpoint: string;
  tokenEndpoint: string;
  userinfoEndpoint: string;
  endSessionEndpoint: string;
  responseType: string;
  postLogoutRedirectUris: string[];
  acrValuesSupported: string[];
};

type OIDCClientsProps = {
  data: OIDCClient[];
  notifyOnDataChange: () => void;
};

type ClientRowProps = {
  row: ClientDetails;
  index: number;
  notifyOnDataChange: () => void;
  onDelete?: (client: ClientDetails) => void;
};

/* -------------------------------------------------------------------------- */
/* Reusable Row Component                                                     */
/* -------------------------------------------------------------------------- */

function ClientRow({
  row,
  index,
  notifyOnDataChange,
  onDelete,
}: ClientRowProps) {
  const [open, setOpen] = React.useState(false);

  const lifetime = row.expireAt
    ? Math.floor((row.expireAt - Date.now()) / 1000)
    : -1;

  const isEnabled =
    row.showClientExpiry && lifetime > 0;

  const handleDialog = (isOpen: boolean) => {
    setOpen(isOpen);
    notifyOnDataChange();
  };

  return (
    <>
      <AuthFlowInputs
        isOpen={open}
        handleDialog={handleDialog}
        client={row}
        notifyOnDataChange={notifyOnDataChange}
      />

      <tr
        key={index}
        className="border-b border-gray-100 hover:bg-gray-50 transition-colors"
      >
        <td className="px-6 py-5 text-sm text-gray-800">
          {row.opHost}
        </td>

        <td className="px-6 py-5 text-sm text-gray-800 break-all">
          {row.clientId}
        </td>

        <td className="px-6 py-5 text-sm text-gray-800 break-all">
          {row.clientSecret}
        </td>

        <td className="px-6 py-5">
          <span
            className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${
              isEnabled
                ? 'bg-emerald-100 text-emerald-700'
                : 'bg-gray-100 text-gray-600'
            }`}
          >
            {isEnabled ? 'Enabled' : 'Disabled'}
          </span>
        </td>

        <td className="px-6 py-5">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setOpen(true)}
              title="Open Flow"
              className="rounded-md p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
            >
              <Zap className="w-4 h-4" fill="currentColor" />
            </button>

            <button
              onClick={() => onDelete?.(row)}
              title="Delete"
              className="rounded-md p-2 text-gray-500 hover:bg-red-50 hover:text-red-600"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </td>
      </tr>
    </>
  );
}

/* -------------------------------------------------------------------------- */
/* Main Component                                                             */
/* -------------------------------------------------------------------------- */

export default function OIDCClients({
  data = [],
  notifyOnDataChange,
}: OIDCClientsProps) {
  const [modelOpen, setModelOpen] = React.useState(false);
  const [rowsPerPage, setRowsPerPage] = React.useState(10);
  const [page, setPage] = React.useState(0);

  const handleDialog = (isOpen: boolean) => {
    setModelOpen(isOpen);
    notifyOnDataChange();
  };

  const paginatedRows = React.useMemo(() => {
    const start = page * rowsPerPage;
    return data.slice(start, start + rowsPerPage);
  }, [data, page, rowsPerPage]);

  const totalPages = Math.ceil(data.length / rowsPerPage);

  const handleDelete = (client: ClientDetails) => {
    chrome.storage.local.get(
      ['oidcClients'],
      (result: { oidcClients?: { clientId: string }[] }) => {
        let clientArr: { clientId: string }[] = [];

        if (result.oidcClients) {
          clientArr = result.oidcClients;

          chrome.storage.local.set({
            oidcClients: clientArr.filter(
              (obj) => obj.clientId !== client.clientId
            ),
          });
        }
      }
    );

    notifyOnDataChange();
  };

  return (
    <div className="rounded-2xl bg-white p-8 shadow-sm border border-gray-100">
      <RegisterClient
        isOpen={modelOpen}
        handleDialog={handleDialog}
      />

      {/* Header */}
      <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            OIDC Clients
          </h1>
          <p className="mt-2 text-sm text-gray-500">
            Manage authentication clients for secure access.
          </p>
        </div>

        <button
          onClick={() => setModelOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-emerald-700"
        >
          <Plus className="w-4 h-4" />
          Add Client
        </button>
      </div>

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-gray-200">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr className="border-b border-gray-200">
              <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                Issuer
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                Client ID
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                Client Secret
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                Active
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                Action
              </th>
            </tr>
          </thead>

          <tbody className="bg-white">
            {data.length === 0 ? (
              <tr>
                <td colSpan={5} className="p-6">
                  <SuccessAlert>
                    No clients configured yet. Use
                    <span className="font-medium">
                      {' '}
                      Add Client
                    </span>{' '}
                    to get started.
                  </SuccessAlert>
                </td>
              </tr>
            ) : (
              paginatedRows.map((row, index) => (
                <ClientRow
                  key={row.clientId}
                  row={{...row} as any}
                  index={index}
                  notifyOnDataChange={notifyOnDataChange}
                  onDelete={handleDelete}
                />
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {data.length > 0 && (
        <div className="mt-6 flex flex-col gap-4 border-t border-gray-200 pt-6 md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-3">
            <span className="text-sm text-gray-600">
              Rows per page:
            </span>

            <select
              value={rowsPerPage}
              onChange={(e) => {
                setRowsPerPage(Number(e.target.value));
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-200"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </div>

          <span className="text-sm text-gray-600">
            {page * rowsPerPage + 1}–
            {Math.min(
              (page + 1) * rowsPerPage,
              data.length
            )}{' '}
            of {data.length}
          </span>

          <div className="flex items-center gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((prev) => prev - 1)}
              className="rounded-md border border-gray-200 p-2 text-gray-500 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((prev) => prev + 1)}
              className="rounded-md border border-gray-200 p-2 text-gray-500 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}