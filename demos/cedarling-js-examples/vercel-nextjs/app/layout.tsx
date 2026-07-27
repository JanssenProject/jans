import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'TaskApp — Cedarling Next.js Example',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="bg-app text-text-primary">{children}</body>
    </html>
  );
}
