import { createRoot } from 'react-dom/client';
import App from './App';
import { shutDownRendererCedarling } from './cedarling/init';

// A pagehide event covers renderer teardown paths that do not unmount React.
window.addEventListener(
  'pagehide',
  () => void shutDownRendererCedarling().catch((error) => console.error('Cedarling shutdown failed', error)),
  { once: true },
);

const container = document.getElementById('root') as HTMLElement;
const root = createRoot(container);
root.render(<App />);
