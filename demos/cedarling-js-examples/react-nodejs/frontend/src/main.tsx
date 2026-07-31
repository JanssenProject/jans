import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import { shutDownCedarling } from './cedarling/init';
import './index.css';

// A browser page can disappear without a React unmount, so release the shared
// Cedarling engine at the page lifecycle boundary.
window.addEventListener(
  'pagehide',
  () => void shutDownCedarling().catch((error) => console.error('Cedarling shutdown failed', error)),
  { once: true },
);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
