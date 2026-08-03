import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import { shutDownCedarling } from './cedarling/init';
import './index.css';

// A browser page can disappear without a React unmount, so release the shared
// Cedarling engine at the page lifecycle boundary. A back/forward cache entry
// can be restored, so keep the engine alive in that case.
window.addEventListener("pagehide", (event) => {
  if (event.persisted) return;
  void shutDownCedarling().catch((error) => console.error("Cedarling shutdown failed", error));
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
