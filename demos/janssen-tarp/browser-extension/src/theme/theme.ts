import { createTheme } from '@mui/material/styles';

const FLAT_SHADOW = '0 1px 3px rgba(0,0,0,0.08)';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1565c0',
      light: '#1976d2',
      dark: '#0d47a1',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#7b1fa2',
      light: '#9c27b0',
      dark: '#4a148c',
    },
    success: {
      main: '#2e7d32',
      light: '#4caf50',
      dark: '#1b5e20',
    },
    background: {
      default: '#f0f4f8',
      paper: '#ffffff',
    },
    text: {
      primary: '#1a2027',
      secondary: '#556070',
    },
  },
  typography: {
    fontFamily: '"Nunito", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: { fontWeight: 700, letterSpacing: '-0.02em' },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 600 },
    button: { fontWeight: 600 },
  },
  shape: {
    borderRadius: 10,
  },
  shadows: [
    'none',
    '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.12)',
    '0 3px 6px rgba(0,0,0,0.08), 0 3px 6px rgba(0,0,0,0.10)',
    '0 10px 20px rgba(0,0,0,0.08), 0 6px 6px rgba(0,0,0,0.10)',
    '0 14px 28px rgba(0,0,0,0.10), 0 10px 10px rgba(0,0,0,0.08)',
    '0 19px 38px rgba(0,0,0,0.10), 0 15px 12px rgba(0,0,0,0.08)',
    FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW,
    FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW,
    FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW,
    FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW, FLAT_SHADOW,
  ],
  components: {
    MuiContainer: {
      defaultProps: {
        maxWidth: 'lg',
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          borderRadius: 8,
        },
        containedPrimary: {
          boxShadow: '0 2px 8px rgba(21, 101, 192, 0.35)',
          '&:hover': {
            boxShadow: '0 4px 12px rgba(21, 101, 192, 0.45)',
          },
        },
        containedSuccess: {
          boxShadow: '0 2px 8px rgba(46, 125, 50, 0.35)',
          '&:hover': {
            boxShadow: '0 4px 12px rgba(46, 125, 50, 0.45)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontWeight: 700,
          fontSize: '0.8125rem',
          letterSpacing: '0.04em',
          textTransform: 'uppercase',
        },
      },
    },
    MuiAccordion: {
      styleOverrides: {
        root: {
          '&:before': {
            display: 'none',
          },
          border: '1px solid rgba(0, 0, 0, 0.08)',
          borderRadius: '8px !important',
          marginBottom: 8,
          '&:last-of-type': {
            marginBottom: 0,
          },
        },
      },
    },
    MuiAccordionSummary: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          minHeight: 48,
          '&.Mui-expanded': {
            minHeight: 48,
            borderBottom: '1px solid rgba(0, 0, 0, 0.08)',
            borderRadius: '8px 8px 0 0',
          },
        },
        content: {
          margin: '12px 0',
          '&.Mui-expanded': {
            margin: '12px 0',
          },
        },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          fontSize: '0.9rem',
          minHeight: 52,
        },
      },
    },
  },
});

export default theme;
