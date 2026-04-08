import * as React from 'react';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import Typography from '@mui/material/Typography';

interface HelpDrawerProps {
  isOpen: boolean;
  handleDrawer: (open: boolean) => void;
}

export default function HelpDrawer({ isOpen, handleDrawer }: HelpDrawerProps) {

  const toggleDrawer = (open: boolean) => () => {
    handleDrawer(open);
  };

  const images = [
    'tarpDocs1.png',
    'tarpDocs2.png',
    'tarpDocs3.png',
    'tarpDocs4.png',
    'tarpDocs5.png',
    'tarpDocs6.png',
    'tarpDocs7.png',
  ];

  return (
    <Drawer open={isOpen} onClose={toggleDrawer(false)}>
      <Box
        role="presentation"
        sx={{
          width: { xs: '100vw', sm: '70vw', md: '50vw' },
          maxWidth: '100%',
        }}
      >
        {/* Header */}
        <Box sx={{ p: 2 }}>
          <Typography variant="h5" gutterBottom>
            Janssen-Tarp — Quick Start Guide
          </Typography>
          <Typography variant="body2" color="text.secondary">
            7 steps to register a client and test Cedarling authorization
          </Typography>
        </Box>

        {/* Images */}
        {images.map((src, index) => (
          <Box key={index} sx={{ width: '100%' }}>
            <Box
              component="img"
              src={src}
              alt={`Guide step ${index + 1}`}
              sx={{
                width: '100%',
                height: 'auto',
                objectFit: 'cover',
              }}
            />
          </Box>
        ))}
      </Box>
    </Drawer>
  );
}