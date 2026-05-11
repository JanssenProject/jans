import * as React from "react";
import { X } from "lucide-react";

interface HelpDrawerProps {
  isOpen: boolean;
  handleDrawer: (open: boolean) => void;
}

export default function HelpDrawer({
  isOpen,
  handleDrawer,
}: HelpDrawerProps) {
  const images = [
    "tarpDocs1.png",
    "tarpDocs2.png",
    "tarpDocs3.png",
    "tarpDocs4.png",
    "tarpDocs5.png",
    "tarpDocs6.png",
    "tarpDocs7.png",
  ];

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Overlay */}
      <div
        className="absolute inset-0 bg-black/40"
        onClick={() => handleDrawer(false)}
      />

      {/* Drawer */}
      <div className="relative ml-auto h-full w-full bg-white shadow-xl sm:w-[70vw] md:w-[50vw] max-w-full overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 z-10 border-b border-gray-200 bg-white px-6 py-4">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-2xl font-semibold text-gray-900">
                Janssen-Tarp — Quick Start Guide
              </h2>

              <p className="mt-1 text-sm text-gray-500">
                7 steps to register a client and test Cedarling authorization
              </p>
            </div>

            <button
              onClick={() => handleDrawer(false)}
              className="rounded-md p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
              aria-label="Close drawer"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Images */}
        <div className="divide-y divide-gray-100">
          {images.map((src, index) => (
            <div key={index} className="w-full">
              <img
                src={src}
                alt={`Guide step ${index + 1}`}
                className="h-auto w-full object-cover"
              />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}