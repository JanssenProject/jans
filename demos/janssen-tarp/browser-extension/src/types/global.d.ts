// Global ambient type declarations for the browser extension build.

// Allow importing global CSS files (webpack handles them via style-loader/css-loader).
declare module '*.css' {
  const content: string;
  export default content;
}

// Ensure `chrome` is typed as a global in all TS compilation modes.
// (Some toolchains don't automatically include `@types/chrome` globals.)
declare const chrome: typeof import('chrome');

// Some dependencies ship without TS typings in this repo.
declare module 'qs';

