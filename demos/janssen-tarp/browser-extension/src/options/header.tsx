import React from "react";
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

const Header = () => {
  return (
    <Box
      sx={{
        width: '100%',
        background: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)',
        color: '#ffffff',
        px: { xs: 3, sm: 5 },
        py: 1.5,
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        boxShadow: '0 2px 10px rgba(13, 71, 161, 0.4)',
        flexShrink: 0,
      }}
    >
      <Box
        component="img"
        src="logo.jpg"
        alt="Janssen"
        sx={{
          height: 44,
          width: 'auto',
          borderRadius: 1.5,
          flexShrink: 0,
          boxShadow: '0 2px 6px rgba(0,0,0,0.2)',
        }}
      />
      <Box>
        <Typography
          variant="h6"
          component="div"
          sx={{
            fontWeight: 800,
            letterSpacing: 0.5,
            lineHeight: 1.2,
            color: '#ffffff',
          }}
        >
          Janssen TARP
        </Typography>
        <Typography
          variant="caption"
          sx={{
            opacity: 0.8,
            letterSpacing: 1.5,
            textTransform: 'uppercase',
            fontSize: '0.65rem',
            color: '#ffffff',
          }}
        >
          Test Authentication Relying Party
        </Typography>
      </Box>
    </Box>
  );
};

export default Header;
