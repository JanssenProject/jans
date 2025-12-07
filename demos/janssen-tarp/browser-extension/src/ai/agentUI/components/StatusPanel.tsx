import React from 'react';
import { Grid, Alert, Typography, Button } from '@mui/material';
import LinkIcon from '@mui/icons-material/Link';
import { PROVIDER_ICONS } from '../constants';
import { ConnectionStatus } from '../types';

interface StatusPanelProps {
  apiKeyValid: boolean;
  provider: string;
  modelName: string;
  providerLabel: string;
  mcpServerUrl: string;
  connectionStatus: ConnectionStatus;
  onTestConnection: () => void;
}

const StatusPanel: React.FC<StatusPanelProps> = ({
  apiKeyValid,
  provider,
  modelName,
  providerLabel,
  mcpServerUrl,
  connectionStatus,
  onTestConnection
}) => {
  const getConnectionStatusColor = () => {
    switch (connectionStatus) {
      case 'connected': return 'success';
      case 'connecting': return 'warning';
      case 'disconnected': return 'error';
    }
  };

  const getConnectionStatusText = () => {
    switch (connectionStatus) {
      case 'connected': return 'Connected';
      case 'connecting': return 'Connecting...';
      case 'disconnected': return 'Disconnected';
    }
  };

  return (
    <Grid container spacing={2} sx={{ mb: 3 }}>
      <Grid item xs={12} md={6}>
        <Alert 
          severity={apiKeyValid ? "success" : "warning"}
          icon={PROVIDER_ICONS[provider as keyof typeof PROVIDER_ICONS]}
          sx={{ height: '100%' }}
        >
          <Typography variant="body2">
            <strong>LLM:</strong> {providerLabel}
            <br />
            <strong>Model:</strong> {modelName}
            <br />
            <strong>Status:</strong> {apiKeyValid ? '✅ Configured' : '⚠️ Not configured'}
          </Typography>
        </Alert>
      </Grid>
      <Grid item xs={12} md={6}>
        <Alert 
          severity={getConnectionStatusColor()}
          icon={<LinkIcon />}
          sx={{ height: '100%' }}
          action={
            <Button 
              color="inherit" 
              size="small" 
              onClick={onTestConnection}
              disabled={connectionStatus === 'connecting'}
            >
              Test
            </Button>
          }
        >
          <Typography variant="body2">
            <strong>MCP Server:</strong> {mcpServerUrl}
            <br />
            <strong>Status:</strong> {getConnectionStatusText()}
          </Typography>
        </Alert>
      </Grid>
    </Grid>
  );
};

export default StatusPanel;