export default [
  {
    ignores: [
      "**/node_modules/**",
      "**/pkg/**",
      "**/dist/**",
      "**/.build/**",
      "**/.wrangler/**",
      "**/test-results/**",
    ],
  },
  {
    files: [
      "eslint.config.mjs",
      "scripts/**/*.mjs",
      "tests/**/*.mjs",
      "examples/**/*.{cjs,js,jsx,mjs}",
    ],
    languageOptions: { parserOptions: { ecmaFeatures: { jsx: true } } },
    rules: { "no-shadow": "error" },
  },
];
