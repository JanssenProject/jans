/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        app: 'var(--bg-app)',
        surface: 'var(--bg-surface)',
        'surface-muted': 'var(--bg-surface-muted)',
        'primary-light': 'var(--bg-primary-light)',
        primary: 'var(--bg-primary)',
        'primary-hover': 'var(--bg-primary-hover)',
        'danger-subtle': 'var(--bg-danger-subtle)',
        line: 'var(--border-default)',
        'line-light': 'var(--border-light)',
        input: 'var(--border-input)',
        ink: 'var(--text-primary)',
        'ink-secondary': 'var(--text-secondary)',
        'ink-muted': 'var(--text-muted)',
        'ink-disabled': 'var(--text-disabled)',
        danger: 'var(--text-danger)',
        'on-primary': 'var(--text-on-primary)',
        success: 'var(--status-success)',
        warning: 'var(--status-warning)',
      },
    },
  },
  plugins: [],
};
