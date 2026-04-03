import React from 'react';
import Box from '@mui/material/Box';
import Tooltip from '@mui/material/Tooltip';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import IconButton from '@mui/material/IconButton';

export const labelWithTooltip = (
  label: React.ReactNode,
  tooltip: React.ReactNode
) => (
  <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75 }}>
    {label}
    <Tooltip title={tooltip} placement="top" arrow tabIndex={-1} >
      <IconButton
        size="small"
        tabIndex={-1}
        aria-label={`${label} help`}
        sx={{ p: 0.25 }}
      >
        <InfoOutlinedIcon sx={{ fontSize: 18, opacity: 0.75 }} />
      </IconButton>
    </Tooltip>
  </Box>
);