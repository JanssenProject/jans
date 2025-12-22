import React, { useCallback, useState } from 'react';
import {
  Container,
  TextField,
  Box,
  Paper,
  Typography,
  Stack,
  Button,
  IconButton,
  InputAdornment,
  CircularProgress,
  Snackbar,
  Alert,
  Tooltip
} from '@mui/material';
import { amber, green } from '@mui/material/colors';
import SettingsIcon from '@mui/icons-material/Settings';
import AirplanemodeActiveOutlinedIcon from '@mui/icons-material/AirplanemodeActiveOutlined';
import KeyIcon from '@mui/icons-material/Key';
import { useSettings } from './hooks/useSettings';
import { useAIOperations } from './hooks/useAIOperations';
import SettingsDialog from './components/SettingsDialog';
import StatusPanel from './components/StatusPanel';
import AIResponse from './components/AIResponse';
import { AIAgentProps } from './types';

const AIAgent: React.FC<AIAgentProps> = ({ notifyOnDataChange }) => {
  const [settingsOpen, setSettingsOpen] = useState(false);
  
  const {
    apiKey,
    showApiKey,
    apiKeyError,
    apiKeyValid,
    provider,
    model,
    customModel,
    mcpServerUrl,
    modelError,
    mcpUrlError,
    connectionStatus,
    snackbar,
    handleApiKeyChange,
    handleProviderChange,
    handleModelChange,
    handleCustomModelChange,
    handleMcpUrlChange,
    getCurrentProviderConfig,
    saveSettings,
    clearSettings,
    testConnection,
    closeSnackbar,
    setShowApiKey
  } = useSettings();

  const {
    query,
    setQuery,
    result,
    loading,
    error,
    send,
    handleKeyPress
  } = useAIOperations(notifyOnDataChange);

  const handleSend = useCallback(async () => {
    try {
      await send();
    } catch (err) {
      if (err.message.includes("configure your API key") || err.message.includes("configure MCP server")) {
        setSettingsOpen(true);
      }
    }
  }, [send]);

  const handleSaveSettings = useCallback(async () => {
    const success = await saveSettings();
    if (success) {
      setTimeout(() => {
        setSettingsOpen(false);
      }, 1500);
    }
  }, [saveSettings]);

  const getCurrentModelName = useCallback(() => {
    const providerConfig = getCurrentProviderConfig();
    const modelConfig = providerConfig.models.find(m => m.value === model);
    if (modelConfig) return modelConfig.label;
    return model;
  }, [getCurrentProviderConfig, model]);

  const getProviderColor = useCallback(() => {
    switch (provider) {
      case 'openai': return '#2196f3';
      case 'gemini': return '#ff9800';
      case 'deepseek': return '#4caf50';
      default: return 'primary';
    }
  }, [provider]);

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header with Settings Button */}
      <Stack direction="column" spacing={2} sx={{ mb: 1 }}>
        <Stack direction="row" spacing={2} sx={{ mb: 1 }} style={{ display: 'flex', justifyContent: 'space-between' }}>
          <Typography variant="h4" component="h1" sx={{ fontWeight: 600 }}>
            AI Assistant
          </Typography>
          <Tooltip title="Settings & API Key">
            <Button 
              color="success" 
              variant="outlined" 
              startIcon={<SettingsIcon />} 
              onClick={() => setSettingsOpen(true)} 
              style={{ maxWidth: '200px' }}
            >
              Settings
            </Button>
          </Tooltip>
        </Stack>
        
        {/* Status Info */}
        <StatusPanel
          apiKeyValid={apiKeyValid}
          provider={provider}
          modelName={getCurrentModelName()}
          providerLabel={getCurrentProviderConfig().label}
          mcpServerUrl={mcpServerUrl}
          connectionStatus={connectionStatus}
          onTestConnection={testConnection}
        />
      </Stack>
      
      {/* Input Section */}
      <Box className="box" sx={{ mb: 4 }}>
        <TextField
          fullWidth
          id="outlined-multiline-flexible"
          label="What would you like to do in Tarp?"
          multiline
          minRows={3}
          maxRows={6}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyPress}
          disabled={loading || !apiKeyValid || connectionStatus !== 'connected'}
          placeholder={apiKeyValid && connectionStatus === 'connected' 
            ? `Describe what you want to accomplish... (using ${getCurrentProviderConfig().label} - ${getCurrentModelName()})` 
            : "Configure settings to start using the assistant"}
          InputProps={{
            startAdornment: !apiKeyValid && (
              <InputAdornment position="start">
                <KeyIcon sx={{ color: amber[500] }} />
              </InputAdornment>
            ),
            endAdornment: (
              <InputAdornment position="end">
                <IconButton 
                  aria-label="Send" 
                  onClick={handleSend}
                  disabled={loading || !query.trim() || !apiKeyValid || connectionStatus !== 'connected'}
                  sx={{ 
                    color: apiKeyValid && connectionStatus === 'connected' ? getProviderColor() : 'action.disabled',
                    '&:hover': { backgroundColor: apiKeyValid ? green[50] : 'transparent' }
                  }}
                >
                  {loading ? (
                    <CircularProgress size={24} />
                  ) : (
                    <AirplanemodeActiveOutlinedIcon />
                  )}
                </IconButton>
              </InputAdornment>
            ),
          }}
          sx={{
            '& .MuiOutlinedInput-root': {
              fontSize: '1rem',
            }
          }}
        />
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Press Ctrl+Enter to send • Using {getCurrentProviderConfig().label} - {getCurrentModelName()}
          </Typography>
          {(!apiKeyValid || connectionStatus !== 'connected') && (
            <Typography variant="caption" sx={{ color: amber[700] }}>
              ⚠️ {!apiKeyValid ? 'API key not configured' : 'MCP server not connected'}
            </Typography>
          )}
        </Box>
      </Box>

      {/* AI Response Section */}
      <AIResponse
        result={result}
        loading={loading}
        error={error}
        provider={provider}
        modelName={getCurrentModelName()}
        providerLabel={getCurrentProviderConfig().label}
      />

      {/* Status Information */}
      <Paper 
        elevation={0} 
        sx={{ 
          p: 2, 
          backgroundColor: 'grey.50',
          border: '1px solid',
          borderColor: 'grey.200',
          borderRadius: 1,
          mb: 3
        }}
      >
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          <strong>Tip:</strong> The AI assistant can help you with various tasks in Tarp. 
          Describe what you want to accomplish in natural language.
          {(!apiKeyValid || connectionStatus !== 'connected') && (
            <span style={{ color: amber[700], fontWeight: 'bold', marginLeft: 8 }}>
              {!apiKeyValid ? 'Configure your API key' : 'Connect to MCP server'} to start using the assistant!
            </span>
          )}
          {apiKeyValid && connectionStatus === 'connected' && (
            <span style={{ color: green[700], fontWeight: 'bold', marginLeft: 8 }}>
              Ready to use {getCurrentProviderConfig().label} - {getCurrentModelName()}
            </span>
          )}
        </Typography>
      </Paper>

      {/* Settings Dialog */}
      <SettingsDialog
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        apiKey={apiKey}
        showApiKey={showApiKey}
        provider={provider}
        model={model}
        customModel={customModel}
        mcpServerUrl={mcpServerUrl}
        connectionStatus={connectionStatus}
        apiKeyError={apiKeyError}
        modelError={modelError}
        mcpUrlError={mcpUrlError}
        onApiKeyChange={handleApiKeyChange}
        onToggleShowApiKey={() => setShowApiKey(!showApiKey)}
        onProviderChange={handleProviderChange}
        onModelChange={handleModelChange}
        onCustomModelChange={handleCustomModelChange}
        onMcpUrlChange={handleMcpUrlChange}
        onTestConnection={testConnection}
        onSaveSettings={handleSaveSettings}
        onClearSettings={clearSettings}
      />

      {/* Snackbar for notifications */}
      <Snackbar 
        open={snackbar.open} 
        autoHideDuration={4000} 
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          onClose={closeSnackbar} 
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default AIAgent;