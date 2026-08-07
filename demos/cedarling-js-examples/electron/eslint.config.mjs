import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
  {
    ignores: ["coverage/**", "node_modules/**", "out/**"],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: [
      "electron.vite.config.ts",
      "postcss.config.js",
      "scripts/**/*.mjs",
      "src/main/**/*.ts",
      "src/preload/**/*.ts",
      "src/shared/**/*.ts",
    ],
    languageOptions: {
      globals: globals.node,
    },
  },
  {
    files: ["src/renderer/**/*.{ts,tsx}"],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    files: ["src/__tests__/**/*.{ts,tsx}"],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.jest,
        ...globals.node,
      },
    },
  },
  {
    files: ["**/*.{js,mjs,ts,tsx}"],
    languageOptions: {
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: "module",
      },
    },
    rules: {
      "@typescript-eslint/ban-ts-comment": "off",
      "@typescript-eslint/no-require-imports": "off",
      "@typescript-eslint/no-shadow": "error",
      "@typescript-eslint/no-unused-vars": "error",
    },
  },
);
