import React from 'react';
import TextField from '@mui/material/TextField';
import Container from '@mui/material/Container';
import { handleUserPrompt } from "../mcp/index";
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';
import AirplanemodeActiveOutlinedIcon from '@mui/icons-material/AirplanemodeActiveOutlined';
import KeyIcon from '@mui/icons-material/Key';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { green, amber, blue, orange } from '@mui/material/colors';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Alert, { AlertColor } from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';
import Tooltip from '@mui/material/Tooltip';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import Stack from '@mui/material/Stack';
import SettingsIcon from '@mui/icons-material/Settings';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import FormHelperText from '@mui/material/FormHelperText';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import CloudIcon from '@mui/icons-material/Cloud';
import LinkIcon from '@mui/icons-material/Link';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import ToggleButton from '@mui/material/ToggleButton';
import OpenAIIcon from '@mui/icons-material/Cloud';
import GeminiIcon from '@mui/icons-material/SmartToy';
import DeepSeekIcon from '@mui/icons-material/TravelExplore';
import Grid from '@mui/material/Grid';
import Divider from '@mui/material/Divider';

// Keys for storage
const LLM_API_KEY_STORAGE_KEY = 'llm_api_key';
const LLM_MODEL_STORAGE_KEY = 'llm_model';
const LLM_PROVIDER_STORAGE_KEY = 'llm_provider';
const MCP_SERVER_URL = 'mcp_server_url';

// Available LLM providers
const LLM_PROVIDERS = [
  { 
    value: 'openai', 
    label: 'OpenAI', 
    icon: <OpenAIIcon />,
    description: 'GPT models from OpenAI',
    apiKeyFormat: 'sk-...',
    apiKeyPlaceholder: 'sk-...',
    apiKeyValidation: (key: string) => key.startsWith('sk-') && key.length > 20,
    apiKeyValidationMessage: "API key should start with 'sk-' and be at least 20 characters",
    models: [
      { value: 'gpt-4o', label: 'GPT-4o', description: 'Latest and most capable model' },
      { value: 'gpt-4o-mini', label: 'GPT-4o-mini', description: 'Latest and most capable model' },
      { value: 'gpt-4-turbo', label: 'GPT-4 Turbo', description: 'High intelligence with 128K context' },
      { value: 'gpt-4', label: 'GPT-4', description: 'Original GPT-4 model' },
      { value: 'gpt-3.5-turbo', label: 'GPT-3.5 Turbo', description: 'Fast and cost-effective' },
      { value: 'gpt-3.5-turbo-16k', label: 'GPT-3.5 Turbo 16K', description: 'Larger context window' },
    ]
  },
  { 
    value: 'gemini', 
    label: 'Google Gemini', 
    icon: <GeminiIcon />,
    description: 'Google\'s Gemini models',
    apiKeyFormat: 'AIza...',
    apiKeyPlaceholder: 'AIza...',
    apiKeyValidation: (key: string) => key.startsWith('AIza') && key.length > 30,
    apiKeyValidationMessage: "API key should start with 'AIza' and be at least 30 characters",
    models: [
      { value: 'gemini-1.5-pro', label: 'Gemini 1.5 Pro', description: 'Most capable Gemini model' },
      { value: 'gemini-1.5-flash', label: 'Gemini 1.5 Flash', description: 'Fast and efficient model' },
      { value: 'gemini-pro', label: 'Gemini Pro', description: 'Original Gemini Pro model' },
    ]
  },
  { 
    value: 'deepseek', 
    label: 'DeepSeek', 
    icon: <DeepSeekIcon />,
    description: 'DeepSeek AI models',
    apiKeyFormat: 'Bearer ...',
    apiKeyPlaceholder: 'Enter your DeepSeek API key',
    apiKeyValidation: (key: string) => key.length > 10,
    apiKeyValidationMessage: "API key should be at least 10 characters",
    models: [
      { value: 'deepseek-chat', label: 'DeepSeek Chat', description: 'Main chat model' },
      { value: 'deepseek-coder', label: 'DeepSeek Coder', description: 'Specialized for coding' },
    ]
  }
];

// Provider icons mapping
const PROVIDER_ICONS = {
  openai: <OpenAIIcon />,
  gemini: <GeminiIcon />,
  deepseek: <DeepSeekIcon />
};

const AIAgent = (props) => {
    const [query, setQuery] = React.useState("");
    const [result, setResult] = React.useState(null);
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(null);
    
    // Settings state
    const [settingsOpen, setSettingsOpen] = React.useState(false);
    const [apiKey, setApiKey] = React.useState("");
    const [showApiKey, setShowApiKey] = React.useState(false);
    const [apiKeyError, setApiKeyError] = React.useState("");
    const [apiKeyValid, setApiKeyValid] = React.useState(false);
    const [snackbar, setSnackbar] = React.useState({ open: false, message: "", severity: "success" });
    const [provider, setProvider] = React.useState("openai");
    const [model, setModel] = React.useState("gpt-4o-mini");
    const [mcpServerUrl, setMcpServerUrl] = React.useState("http://localhost:3001");
    const [customModel, setCustomModel] = React.useState("");
    const [modelError, setModelError] = React.useState("");
    const [mcpUrlError, setMcpUrlError] = React.useState("");
    const [connectionStatus, setConnectionStatus] = React.useState<"disconnected" | "connecting" | "connected">("disconnected");

    // Load settings from storage on component mount
    React.useEffect(() => {
        const initialize = async () => {
            const results = await new Promise((resolve) => {
                chrome.storage.local.get([
                    LLM_API_KEY_STORAGE_KEY,
                    LLM_MODEL_STORAGE_KEY,
                    LLM_PROVIDER_STORAGE_KEY,
                    MCP_SERVER_URL
                ], (result) => {
                    resolve(result);
                });
            });
            
            const savedApiKey = results[LLM_API_KEY_STORAGE_KEY];
            if (savedApiKey) {
                setApiKey(savedApiKey);
                validateApiKey(savedApiKey, provider);
            }
            
            const savedModel = results[LLM_MODEL_STORAGE_KEY];
            if (savedModel) {
                setModel(savedModel);
            }
            
            const savedProvider = results[LLM_PROVIDER_STORAGE_KEY];
            if (savedProvider) {
                setProvider(savedProvider);
            }
            
            const savedMcpUrl = results[MCP_SERVER_URL];
            if (savedMcpUrl) {
                setMcpServerUrl(savedMcpUrl);
                validateMcpUrl(savedMcpUrl);
            }
            
            // Test MCP connection on load
            if (savedMcpUrl) {
                testMCPConnection(savedMcpUrl);
            }
        };
        initialize();
    }, []);

    // Validate API key based on provider
    const validateApiKey = (key: string, currentProvider: string = provider) => {
        const providerConfig = LLM_PROVIDERS.find(p => p.value === currentProvider);
        if (!providerConfig) return false;
        
        const isValid = providerConfig.apiKeyValidation(key);
        setApiKeyValid(isValid);
        
        if (!isValid && key) {
            setApiKeyError(providerConfig.apiKeyValidationMessage);
        } else {
            setApiKeyError("");
        }
        
        return isValid;
    };

    // Validate MCP Server URL
    const validateMcpUrl = (url: string) => {
        try {
            const urlObj = new URL(url);
            const isValid = urlObj.protocol === 'http:' || urlObj.protocol === 'https:';
            setMcpUrlError(isValid ? "" : "URL must use http:// or https:// protocol");
            return isValid;
        } catch (e) {
            setMcpUrlError("Please enter a valid URL (e.g., http://localhost:3001)");
            return false;
        }
    };

    // Test MCP connection
    const testMCPConnection = async (url: string) => {
        setConnectionStatus("connecting");
        try {
            // Simple fetch to test connection
            const response = await fetch(url+'/', {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
            });
            
            if (response.ok) {
                setConnectionStatus("connected");
                return true;
            } else {
                setConnectionStatus("disconnected");
                return false;
            }
        } catch (error) {
            setConnectionStatus("disconnected");
            return false;
        }
    };

    const handleApiKeyChange = (e) => {
        const newKey = e.target.value;
        setApiKey(newKey);
        validateApiKey(newKey);
    };

    const handleProviderChange = (event, newProvider) => {
        if (newProvider) {
            setProvider(newProvider);
            // Reset model to default for new provider
            const providerConfig = LLM_PROVIDERS.find(p => p.value === newProvider);
            if (providerConfig && providerConfig.models.length > 0) {
                setModel(providerConfig.models[0].value);
            }
            // Re-validate API key with new provider
            validateApiKey(apiKey, newProvider);
        }
    };

    const handleModelChange = (e) => {
        const newModel = e.target.value;
        setModel(newModel);
        setModelError("");
        
        if (model === 'custom' && newModel !== 'custom') {
            setCustomModel("");
        }
    };

    const handleMcpUrlChange = (e) => {
        const newUrl = e.target.value;
        setMcpServerUrl(newUrl);
        validateMcpUrl(newUrl);
    };

    const handleCustomModelChange = (e) => {
        const value = e.target.value;
        setCustomModel(value);
        
        if (value && value.trim()) {
            if (value.length < 3) {
                setModelError("Model name must be at least 3 characters");
            } else {
                setModelError("");
            }
        } else {
            setModelError("Please enter a model name");
        }
    };

    const getCurrentProviderConfig = () => {
        return LLM_PROVIDERS.find(p => p.value === provider) || LLM_PROVIDERS[0];
    };

    const getAvailableModels = () => {
        const providerConfig = getCurrentProviderConfig();
        return providerConfig.models;
    };

    const getFinalModelName = () => {
        return model === 'custom' ? customModel.trim() : model;
    };

    const validateSettings = () => {
        const apiKeyValid = validateApiKey(apiKey);
        const mcpUrlValid = validateMcpUrl(mcpServerUrl);
        
        let modelValid = true;
        if (model === 'custom' && (!customModel || customModel.trim().length < 3)) {
            setModelError("Please enter a valid model name");
            modelValid = false;
        }
        
        return apiKeyValid && modelValid && mcpUrlValid;
    };

    const saveSettings = async () => {
        if (!apiKey.trim()) {
            setSnackbar({
                open: true,
                message: "Please enter an API key",
                severity: "error"
            });
            return;
        }

        if (!validateSettings()) {
            setSnackbar({
                open: true,
                message: "Please fix the errors in the form",
                severity: "error"
            });
            return;
        }

        try {
            // Save all settings
            await chrome.storage.local.set({
                [LLM_API_KEY_STORAGE_KEY]: apiKey,
                [LLM_MODEL_STORAGE_KEY]: getFinalModelName(),
                [LLM_PROVIDER_STORAGE_KEY]: provider,
                [MCP_SERVER_URL]: mcpServerUrl
            });
            
            // Test MCP connection after saving
            await testMCPConnection(mcpServerUrl);
            
            setSnackbar({
                open: true,
                message: "Settings saved successfully!",
                severity: "success"
            });
            
            // Update model state with final name
            setModel(getFinalModelName());
            
            // Close settings after a short delay
            setTimeout(() => {
                setSettingsOpen(false);
            }, 1500);
            
        } catch (error) {
            setSnackbar({
                open: true,
                message: "Failed to save settings to storage",
                severity: "error"
            });
        }
    };

    const clearSettings = () => {
        chrome.storage.local.remove([
            LLM_API_KEY_STORAGE_KEY,
            LLM_MODEL_STORAGE_KEY,
            LLM_PROVIDER_STORAGE_KEY,
            MCP_SERVER_URL
        ]);

        setApiKey("");
        setApiKeyValid(false);
        setApiKeyError("");
        setProvider("openai");
        setModel("gpt-4o-mini");
        setCustomModel("");
        setModelError("");
        setMcpServerUrl("http://localhost:3001");
        setMcpUrlError("");
        setConnectionStatus("disconnected");
        
        setSnackbar({
            open: true,
            message: "All settings cleared",
            severity: "info"
        });
    };

    const testConnection = async () => {
        const success = await testMCPConnection(mcpServerUrl);
        if (success) {
            setSnackbar({
                open: true,
                message: "Successfully connected to MCP server!",
                severity: "success"
            });
        } else {
            setSnackbar({
                open: true,
                message: "Failed to connect to MCP server. Please check the URL and ensure the server is running.",
                severity: "error"
            });
        }
    };

    const send = async () => {
        if (!query.trim()) return;
        
        // Check if API key is configured
        const results = await new Promise((resolve) => {
            chrome.storage.local.get([
                LLM_API_KEY_STORAGE_KEY,
                LLM_PROVIDER_STORAGE_KEY,
                MCP_SERVER_URL
            ], (result) => {
                resolve(result);
            });
        });
        
        const savedApiKey = results[LLM_API_KEY_STORAGE_KEY];
        const savedProvider = results[LLM_PROVIDER_STORAGE_KEY] || 'openai';
        const savedMcpUrl = results[MCP_SERVER_URL];
        
        if (!savedApiKey) {
            setSnackbar({
                open: true,
                message: "Please configure your API key first",
                severity: "warning"
            });
            setSettingsOpen(true);
            return;
        }

        if (!savedMcpUrl) {
            setSnackbar({
                open: true,
                message: "Please configure MCP server URL first",
                severity: "warning"
            });
            setSettingsOpen(true);
            return;
        }
        
        setLoading(true);
        setError(null);
        setResult(null);
        
        try {
            const result = await handleUserPrompt(query);
            if(result?.type === 'text') {
              setResult(result.content);  
            } else if(result?.type === 'tool_results') {
              setResult(result.results);  
            } else {
              setResult(result);
            }
            
        } catch (err) {
            console.error("Error:", err);
            setError(err.message || "An error occurred while processing your request");
        } finally {
            setLoading(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
            send();
        }
    };

    // Get current model name for display
    const getCurrentModelName = () => {
        const providerConfig = getCurrentProviderConfig();
        const modelConfig = providerConfig.models.find(m => m.value === model);
        if (modelConfig) return modelConfig.label;
        return model;
    };

    // Get provider color
    const getProviderColor = () => {
        switch (provider) {
            case 'openai': return blue[500];
            case 'gemini': return orange[500];
            case 'deepseek': return green[500];
            default: return 'primary';
        }
    };

    // Get connection status color
    const getConnectionStatusColor = () => {
        switch (connectionStatus) {
            case 'connected': return green[500];
            case 'connecting': return amber[500];
            case 'disconnected': return 'error';
        }
    };

    // Get connection status text
    const getConnectionStatusText = () => {
        switch (connectionStatus) {
            case 'connected': return 'Connected';
            case 'connecting': return 'Connecting...';
            case 'disconnected': return 'Disconnected';
        }
    };

    return (
        <Container maxWidth="lg" sx={{ py: 4 }}>
            {/* Header with Settings Button */}
            <Stack direction="column" spacing={2} sx={{ mb: 1 }}>
                <Stack direction="row" spacing={2} sx={{ mb: 1 }} style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h4" component="h1" sx={{ fontWeight: 600 }}>
                        AI Assistant
                    </Typography>
                    <Tooltip title="Settings & API Key">
                        <Button color="success" variant="outlined" startIcon={<SettingsIcon />} onClick={() => setSettingsOpen(true)} style={{ maxWidth: '200px' }}>
                            Settings
                        </Button>
                    </Tooltip>
                </Stack>
                
                {/* Status Info */}
                <Grid container spacing={2} sx={{ mb: 3 }}>
                    <Grid item xs={12} md={6}>
                        <Alert 
                            severity={apiKeyValid ? "success" : "warning"}
                            icon={PROVIDER_ICONS[provider]}
                            sx={{ height: '100%' }}
                        >
                            <Typography variant="body2">
                                <strong>LLM:</strong> {getCurrentProviderConfig().label}
                                <br />
                                <strong>Model:</strong> {getCurrentModelName()}
                                <br />
                                <strong>Status:</strong> {apiKeyValid ? '✅ Configured' : '⚠️ Not configured'}
                            </Typography>
                        </Alert>
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <Alert 
                            severity={connectionStatus === 'connected' ? "success" : 
                                     connectionStatus === 'connecting' ? "warning" : "error"}
                            icon={<LinkIcon />}
                            sx={{ height: '100%' }}
                            action={
                                <Button 
                                    color="inherit" 
                                    size="small" 
                                    onClick={testConnection}
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
                    onKeyPress={handleKeyPress}
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
                                    onClick={send}
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
            {(result || loading || error) && (
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
                                    Processing with {getCurrentProviderConfig().label} - {getCurrentModelName()}...
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
                                    Response from {getCurrentProviderConfig().label} - {getCurrentModelName()} at {new Date().toLocaleTimeString()}
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
            )}

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
            <Dialog open={settingsOpen} onClose={() => setSettingsOpen(false)} maxWidth="md" fullWidth>
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
                            <Grid item xs={12}>
                                <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                                    <CloudIcon sx={{ fontSize: 18, mr: 1, verticalAlign: 'middle' }} />
                                    AI Provider Selection
                                </Typography>
                                
                                <ToggleButtonGroup
                                    value={provider}
                                    exclusive
                                    onChange={handleProviderChange}
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
                            </Grid>

                            {/* API Key Section */}
                            <Grid item xs={12}>
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
                                        onChange={handleApiKeyChange}
                                        endAdornment={
                                            <InputAdornment position="end">
                                                <IconButton
                                                    aria-label="toggle password visibility"
                                                    onClick={() => setShowApiKey(!showApiKey)}
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
                                    {apiKeyValid && (
                                        <Typography variant="caption" color="success.main" sx={{ mt: 1, display: 'block' }}>
                                            ✓ Valid API key format
                                        </Typography>
                                    )}
                                    <FormHelperText>
                                        Format: {getCurrentProviderConfig().apiKeyFormat}
                                    </FormHelperText>
                                </FormControl>
                            </Grid>

                            {/* Model Selection */}
                            <Grid item xs={12} md={6}>
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
                                        onChange={handleModelChange}
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
                                            onChange={handleCustomModelChange}
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
                            </Grid>

                            {/* MCP Server Configuration */}
                            <Grid item xs={12} md={6}>
                                <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                                    <LinkIcon sx={{ fontSize: 18, mr: 1, verticalAlign: 'middle' }} />
                                    MCP Server Configuration
                                </Typography>
                                
                                <FormControl fullWidth>
                                    <InputLabel htmlFor="mcp-url-input">MCP Server URL</InputLabel>
                                    <OutlinedInput
                                        id="mcp-url-input"
                                        value={mcpServerUrl}
                                        onChange={handleMcpUrlChange}
                                        label="MCP Server URL"
                                        placeholder="http://localhost:3001"
                                        error={!!mcpUrlError}
                                        endAdornment={
                                            <InputAdornment position="end">
                                                <Button 
                                                    size="small" 
                                                    onClick={testConnection}
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
                            </Grid>

                            {/* Information Section */}
                            <Grid item xs={12}>
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
                            </Grid>
                        </Grid>
                    </Box>
                </DialogContent>
                
                <DialogActions sx={{ px: 3, pb: 3 }}>
                    <Button 
                        onClick={clearSettings} 
                        color="inherit"
                    >
                        Clear All
                    </Button>
                    <Button 
                        onClick={() => setSettingsOpen(false)}
                        color="inherit"
                    >
                        Cancel
                    </Button>
                    <Button 
                        onClick={saveSettings}
                        variant="contained"
                        disabled={!apiKey.trim() || (model === 'custom' && !customModel.trim())}
                        startIcon={<SettingsIcon />}
                    >
                        Save Settings
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Snackbar for notifications */}
            <Snackbar 
                open={snackbar.open} 
                autoHideDuration={4000} 
                onClose={() => setSnackbar({ ...snackbar, open: false })}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
                <Alert 
                    onClose={() => setSnackbar({ ...snackbar, open: false })} 
                    severity={snackbar.severity as AlertColor}
                    variant="filled"
                    sx={{ width: '100%' }}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Container>
    );
}

export default AIAgent;