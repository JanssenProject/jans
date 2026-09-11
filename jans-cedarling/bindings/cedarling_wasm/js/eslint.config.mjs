export default [
  {
    ignores: [
      "**/node_modules/**",
      "**/pkg/**",
      "dist/**",
      ".build/**",
      "test-results/**",
    ],
  },
  {
    files: ["eslint.config.mjs", "scripts/**/*.mjs", "tests/**/*.mjs"],
    rules: { "no-shadow": "error" },
  },
];
