import React from 'react';
import Box from '@mui/material/Box';
import Tooltip from '@mui/material/Tooltip';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

export const labelWithTooltip = (label: string, tooltip: string) => (
  <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75 }}>
    {label}
    <Tooltip title={tooltip} placement="top" arrow>
      <InfoOutlinedIcon sx={{ fontSize: 18, opacity: 0.75 }} />
    </Tooltip>
  </Box>
);