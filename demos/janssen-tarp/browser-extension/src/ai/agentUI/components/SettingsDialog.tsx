import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Grid,
  FormControl,
  InputLabel,
  OutlinedInput,
  IconButton,
  InputAdornment,
  Select,
  MenuItem,
  FormHelperText,
  ToggleButtonGroup,
  ToggleButton,
  Alert,
  Divider
} from '@mui/material';
import SettingsIcon from '@mui/icons-material/Settings';
import KeyIcon from '@mui/icons-material/Key';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import LinkIcon from '@mui/icons-material/Link';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import CloudIcon from '@mui/icons-material/Cloud';
import { LLM_PROVIDERS } from '../constants';
import { LLMProvider, ConnectionStatus } from '../types';

interface SettingsDialogProps {
  open: boolean;
  onClose: () => void;
  apiKey: string;
  showApiKey: boolean;
  provider: string;
  model: string;
  customModel: string;
  mcpServerUrl: string;
  connectionStatus: ConnectionStatus;
  apiKeyError: string;
  modelError: string;
  mcpUrlError: string;
  onApiKeyChange: (key: string) => void;
  onToggleShowApiKey: () => void;
  onProviderChange: (provider: string) => void;
  onModelChange: (model: string) => void;
  onCustomModelChange: (model: string) => void;
  onMcpUrlChange: (url: string) => void;
  onTestConnection: () => void;
  onSaveSettings: () => void;
  onClearSettings: () => void;
}

const SettingsDialog: React.FC<SettingsDialogProps> = ({
  open,
  onClose,
  apiKey,
  showApiKey,
  provider,
  model,
  customModel,
  mcpServerUrl,
  connectionStatus,
  apiKeyError,
  modelError,
  mcpUrlError,
  onApiKeyChange,
  onToggleShowApiKey,
  onProviderChange,
  onModelChange,
  onCustomModelChange,
  onMcpUrlChange,
  onTestConnection,
  onSaveSettings,
  onClearSettings
}) => {
  const getCurrentProviderConfig = (): LLMProvider => {
    return LLM_PROVIDERS.find(p => p.value === provider) || LLM_PROVIDERS[0];
  };

  const getAvailableModels = () => {
    return getCurrentProviderConfig().models;
  };

  const getConnectionStatusColor = () => {
    switch (connectionStatus) {
      case 'connected': return '#4caf50';
      case 'connecting': return '#ff9800';
      case 'disconnected': return '#f44336';
      default: return '#9e9e9e'; // grey for unknown status
    }
  };

  const getConnectionStatusText = () => {
    switch (connectionStatus) {
      case 'connected': return 'Connected';
      case 'connecting': return 'Connecting...';
      case 'disconnected': return 'Disconnected';
      default: return 'Unknown';
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <SettingsIcon sx={{ color: 'primary.main' }} />
          <Typography variant="h6">AI Assistant Settings</Typography>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        <Box sx={{ pt: 2 }}>
          <Grid container spacing={3}>
            {/* LLM Provider Selection */}
            <Grid size={{xs: 12}}>
              <>
              <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                <CloudIcon sx={{ fontSize: 18, mr: 1, verticalAlign: 'middle' }} />
                AI Provider Selection
              </Typography>
              
              <ToggleButtonGroup
                value={provider}
                exclusive
                onChange={(_, value) => value && onProviderChange(value)}
                aria-label="AI provider"
                fullWidth
                sx={{ mb: 2 }}
              >
                {LLM_PROVIDERS.map((providerOption) => (
                  <ToggleButton 
                    key={providerOption.value} 
                    value={providerOption.value}
                    sx={{ 
                      py: 1.5,
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 1
                    }}
                  >
                    {providerOption.icon}
                    <Typography variant="body2" fontWeight="medium">
                      {providerOption.label}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {providerOption.description}
                    </Typography>
                  </ToggleButton>
                ))}
              </ToggleButtonGroup>
              </>
            </Grid>

            {/* API Key Section */}
            <Grid size={{ xs:12}}>
              <>
              <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                <KeyIcon sx={{ fontSize: 18, mr: 1, verticalAlign: 'middle' }} />
                {getCurrentProviderConfig().label} API Key
              </Typography>
              
              <FormControl fullWidth variant="outlined">
                <InputLabel htmlFor="api-key-input">
                  {getCurrentProviderConfig().label} API Key
                </InputLabel>
                <OutlinedInput
                  id="api-key-input"
                  type={showApiKey ? 'text' : 'password'}
                  value={apiKey}
                  onChange={(e) => onApiKeyChange(e.target.value)}
                  endAdornment={
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="toggle password visibility"
                        onClick={onToggleShowApiKey}
                        edge="end"
                      >
                        {showApiKey ? <VisibilityOffIcon /> : <VisibilityIcon />}
                      </IconButton>
                    </InputAdornment>
                  }
                  label={`${getCurrentProviderConfig().label} API Key`}
                  placeholder={getCurrentProviderConfig().apiKeyPlaceholder}
                  error={!!apiKeyError}
                />
                {apiKeyError && (
                  <Typography variant="caption" color="error" sx={{ mt: 1 }}>
                    {apiKeyError}
                  </Typography>
                )}
                <FormHelperText>
                  Format: {getCurrentProviderConfig().apiKeyFormat}
                </FormHelperText>
              </FormControl>
              </>
            </Grid>

            {/* Model Selection */}
            <Grid size={{ xs: 12, md:6}}>
              <>
              <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                <SmartToyIcon sx={{ fontSize: 18, mr: 1, verticalAlign: 'middle' }} />
                Model Selection
              </Typography>
              
              <FormControl fullWidth>
                <InputLabel id="model-select-label">AI Model</InputLabel>
                <Select
                  labelId="model-select-label"
                  id="model-select"
                  value={model}
                  label="AI Model"
                  onChange={(e) => onModelChange(e.target.value)}
                >
                  {getAvailableModels().map((modelOption) => (
                    <MenuItem key={modelOption.value} value={modelOption.value}>
                      <Box>
                        <Typography variant="body1">{modelOption.label}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {modelOption.description}
                        </Typography>
                      </Box>
                    </MenuItem>
                  ))}
                  <MenuItem value="custom">
                    <Box>
                      <Typography variant="body1">Custom Model</Typography>
                      <Typography variant="caption" color="text.secondary">
                        Enter your own model name
                      </Typography>
                    </Box>
                  </MenuItem>
                </Select>
                <FormHelperText>
                  Choose the AI model to use for processing requests
                </FormHelperText>
              </FormControl>

              {model === 'custom' && (
                <FormControl fullWidth sx={{ mt: 2 }}>
                  <InputLabel htmlFor="custom-model-input">Custom Model Name</InputLabel>
                  <OutlinedInput
                    id="custom-model-input"
                    value={customModel}
                    onChange={(e) => onCustomModelChange(e.target.value)}
                    label="Custom Model Name"
                    placeholder="e.g., your-company-model-name"
                    error={!!modelError}
                  />
                  {modelError && (
                    <Typography variant="caption" color="error" sx={{ mt: 1 }}>
                      {modelError}
                    </Typography>
                  )}
                  <FormHelperText>
                    Enter the exact model name as defined in your API
                  </FormHelperText>
                </FormControl>
              )}
              </>
            </Grid>

            {/* MCP Server Configuration */}
            <Grid size={{xs: 12, md:6}}>
              <>
              <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                <LinkIcon sx={{ fontSize: 18, mr: 1, verticalAlign: 'middle' }} />
                MCP Server Configuration
              </Typography>
              
              <FormControl fullWidth>
                <InputLabel htmlFor="mcp-url-input">MCP Server URL</InputLabel>
                <OutlinedInput
                  id="mcp-url-input"
                  value={mcpServerUrl}
                  onChange={(e) => onMcpUrlChange(e.target.value)}
                  label="MCP Server URL"
                  placeholder="http://localhost:3001"
                  error={!!mcpUrlError}
                  endAdornment={
                    <InputAdornment position="end">
                      <Button 
                        size="small" 
                        onClick={onTestConnection}
                        disabled={connectionStatus === 'connecting'}
                        sx={{ mr: -1 }}
                      >
                        {connectionStatus === 'connecting' ? 'Testing...' : 'Test'}
                      </Button>
                    </InputAdornment>
                  }
                />
                {mcpUrlError && (
                  <Typography variant="caption" color="error" sx={{ mt: 1 }}>
                    {mcpUrlError}
                  </Typography>
                )}
                <FormHelperText>
                  URL of your MCP server (e.g., http://localhost:3001)
                </FormHelperText>
              </FormControl>
              
              <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <Box sx={{ 
                  width: 10, 
                  height: 10, 
                  borderRadius: '50%', 
                  backgroundColor: getConnectionStatusColor() 
                }} />
                <Typography variant="body2">
                  Status: {getConnectionStatusText()}
                </Typography>
              </Box>
              </>
            </Grid>

            {/* Information Section */}
            <Grid size={{ xs:12}}>
              <>
              <Divider sx={{ my: 2 }} />
              <Alert severity="info">
                <Typography variant="body2">
                  <strong>Where to get API keys:</strong>
                  <br />
                  • <strong>OpenAI:</strong> Visit <a href="https://platform.openai.com/api-keys" target="_blank" rel="noopener noreferrer">platform.openai.com</a>
                  <br />
                  • <strong>Google Gemini:</strong> Visit <a href="https://makersuite.google.com/app/apikey" target="_blank" rel="noopener noreferrer">makersuite.google.com</a>
                  <br />
                  • <strong>DeepSeek:</strong> Visit <a href="https://platform.deepseek.com/api_keys" target="_blank" rel="noopener noreferrer">platform.deepseek.com</a>
                  <br /><br />
                  <strong>MCP Server Setup:</strong>
                  <br />
                  The MCP server must be running locally at the specified URL. 
                  Make sure your MCP server supports the tools you want to use.
                </Typography>
              </Alert>
              </>
            </Grid>
          </Grid>
        </Box>
      </DialogContent>
      
      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button 
          onClick={onClearSettings} 
          color="inherit"
        >
          Clear All
        </Button>
        <Button 
          onClick={onClose}
          color="inherit"
        >
          Cancel
        </Button>
        <Button 
          onClick={onSaveSettings}
          variant="contained"
          disabled={!apiKey.trim() || (model === 'custom' && !customModel.trim())}
          startIcon={<SettingsIcon />}
        >
          Save Settings
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default SettingsDialog;