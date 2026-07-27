/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        app: 'var(--bg-app)',
        surface: {
          DEFAULT: 'var(--bg-surface)',
          hover: 'var(--bg-surface-hover)',
          muted: 'var(--bg-surface-muted)',
        },
        primary: {
          subtle: 'var(--bg-primary-subtle)',
          light: 'var(--bg-primary-light)',
          DEFAULT: 'var(--bg-primary)',
          hover: 'var(--bg-primary-hover)',
        },
        danger: {
          subtle: 'var(--bg-danger-subtle)',
          light: 'var(--bg-danger-light)',
          DEFAULT: 'var(--text-danger)',
        },
        border: {
          DEFAULT: 'var(--border-default)',
          light: 'var(--border-light)',
          input: 'var(--border-input)',
          primary: 'var(--border-primary)',
          danger: 'var(--border-danger)',
          'danger-bold': 'var(--border-danger-bold)',
        },
        text: {
          primary: 'var(--text-primary)',
          secondary: 'var(--text-secondary)',
          muted: 'var(--text-muted)',
          disabled: 'var(--text-disabled)',
          'primary-emphasis': 'var(--text-primary-emphasis)',
          'primary-on-light': 'var(--text-primary-on-light)',
          danger: 'var(--text-danger)',
        },
        placeholder: {
          muted: 'var(--placeholder-muted)',
        },
        ring: {
          primary: 'var(--ring-primary)',
        },
        status: {
          success: 'var(--success)',
          warning: 'var(--warning)',
        },
      },
    },
  },
  plugins: [],
};
