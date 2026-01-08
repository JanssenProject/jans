import React from 'react';
import { Box, Paper, Typography, CircularProgress } from '@mui/material';

interface AIResponseProps {
  result: string | any;
  loading: boolean;
  error: string | null;
  provider: string;
  modelName: string;
  providerLabel: string;
}

const AIResponse: React.FC<AIResponseProps> = ({
  result,
  loading,
  error,
  provider,
  modelName,
  providerLabel
}) => {
  const getProviderColor = () => {
    switch (provider) {
      case 'openai': return 'primary';
      case 'gemini': return 'secondary';
      case 'deepseek': return 'success';
      default: return 'primary';
    }
  };

  if (!result && !loading && !error) return null;

  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
        AI Response
      </Typography>
      
      <Paper 
        elevation={2} 
        sx={{ 
          p: 3, 
          minHeight: 200,
          backgroundColor: 'background.default',
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 2
        }}
      >
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200 }}>
            <CircularProgress sx={{ color: getProviderColor() }} />
            <Typography variant="body1" sx={{ ml: 2 }}>
              Processing with {providerLabel} - {modelName}...
            </Typography>
          </Box>
        )}
        
        {error && (
          <Box sx={{ color: 'error.main' }}>
            <Typography variant="h6" color="error" sx={{ mb: 1 }}>
              Error
            </Typography>
            <Typography variant="body1">
              {error}
            </Typography>
          </Box>
        )}
        
        {result && !loading && (
          <Box sx={{ maxHeight: '400px', overflow: 'auto' }}>
            <Typography variant="caption" sx={{ mb: 2, color: 'text.secondary', display: 'block' }}>
              Response from {providerLabel} - {modelName} at {new Date().toLocaleTimeString()}
            </Typography>
            {typeof result === 'string' ? (
              <Typography 
                variant="body1" 
                component="pre" 
                sx={{ 
                  whiteSpace: 'pre-wrap',
                  wordWrap: 'break-word',
                  fontFamily: 'monospace',
                  fontSize: '0.875rem',
                  m: 0
                }}
              >
                {result}
              </Typography>
            ) : (
              <pre style={{ 
                margin: 0, 
                whiteSpace: 'pre-wrap',
                wordWrap: 'break-word',
                fontFamily: 'monospace',
                fontSize: '0.875rem'
              }}>
                {JSON.stringify(result, null, 2)}
              </pre>
            )}
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default AIResponse;