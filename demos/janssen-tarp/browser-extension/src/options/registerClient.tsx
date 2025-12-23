import * as React from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { v4 as uuidv4 } from 'uuid';
import CircularProgress from "@mui/material/CircularProgress";
import { DemoContainer } from '@mui/x-date-pickers/internals/demo';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment'
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import Autocomplete, { createFilterOptions } from '@mui/material/Autocomplete';
import Stack from '@mui/material/Stack';
import axios from 'axios';
import Alert, { AlertColor } from '@mui/material/Alert';
import { RegistrationRequest, OIDCClient, OpenIDConfiguration } from './types';

export default function RegisterClient({ isOpen, handleDialog }) {
  const [open, setOpen] = React.useState(isOpen);
  const [selectedScopes, setSelectedScopes] = React.useState([])
  const [scopeOptions, setScopeOptions] = React.useState([{ name: "openid" }]);
  const [expireAt, setExpireAt] = React.useState(null);
  const [issuer, setIssuer] = React.useState(null);
  const [issuerError, setIssuerError] = React.useState("")
  const [alert, setAlert] = React.useState("")
  const [alertSeverity, setAlertSeverity] = React.useState<AlertColor>('success');
  const [loading, setLoading] = React.useState(false);

  const REGISTRATION_ERROR = 'Error in registration. Check web console for logs.'
  const filter = createFilterOptions();

  React.useEffect(() => {
    if (isOpen) {
      handleOpen();
    } else {
      handleClose();
    }
  }, [isOpen])

  const handleClose = () => {
    handleDialog(false)
    setOpen(false);
  };

  const handleOpen = () => {
    setIssuerError('');
    setAlert('');
    setLoading(false);
    handleDialog(true)
    setOpen(true);
  };

  const validateIssuer = async (e) => {

    setIssuerError('');
    let issuer = e.target.value;
    if (issuer.length === 0) {
      e.target.value = '';
      return;
    }
    setLoading(true);
    const openIdConfigurationURL = generateOpenIdConfigurationURL(issuer);
    try {
      const opConfiguration = await getOpenidConfiguration(openIdConfigurationURL);
      if (!opConfiguration || !opConfiguration.data.issuer) {
        setIssuerError('Invalid input. Either enter correct Issuer or OpenID Configuration URL.');
        e.target.value = '';
      }
      setIssuer(openIdConfigurationURL);
    } catch (err) {
      console.error(err)
      setIssuerError('Invalid input. Either enter correct Issuer or OpenID Configuration URL.');
      e.target.value = '';
    }
    setLoading(false);
  };

  const generateOpenIdConfigurationURL = (issuer) => {
    if (issuer.length === 0) {
      return '';
    }
    if (!issuer.includes('/.well-known/openid-configuration')) {
      issuer = issuer + '/.well-known/openid-configuration';
    }

    if (!issuer.includes('https') && !issuer.includes('http')) {
      issuer = 'https://' + issuer;
    }
    return issuer;
  };

  const registerOIDCClient = async (registration_endpoint, registerObj) => {
    try {
      setAlert('');
      const registerReqOptions = {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        data: JSON.stringify(registerObj),
        url: registration_endpoint,
      };

      const response = await axios(registerReqOptions);
      return await response;
    } catch (err) {
      console.error('Error in fetching Openid configuration: ' + err);
      setAlert('Error in fetching Openid configuration. Check error log on console.');
      setAlertSeverity('error');
    }
  };

  const getOpenidConfiguration = async (opConfigurationEndpoint) => {
    try {
      setAlert('');
      const oidcConfigOptions = {
        method: 'GET',
        url: opConfigurationEndpoint,
      };
      const response = await axios(oidcConfigOptions);
      return await response;
    } catch (err) {
      console.error(err)
    }
  };

  const registerClient = async (): Promise<void> => {
    try {
      setLoading(true);

      // Validate issuer
      if (!issuer || issuer === '') {
        setIssuerError('Issuer cannot be left blank. Either enter correct Issuer or OpenID Configuration URL.');
        return;
      }

      // Normalize issuer URL
      let issuerUrl: string;
      let opConfigurationEndpoint: string;

      try {
        // Check if the issuer is already a configuration endpoint or needs .well-known/openid-configuration
        if (issuer.includes('.well-known/openid-configuration')) {
          opConfigurationEndpoint = issuer;
          const url = new URL(issuer);
          issuerUrl = issuer.replace(/\/?\.well-known\/openid-configuration\/?$/, '');
        } else {
          issuerUrl = issuer.endsWith('/') ? issuer.slice(0, -1) : issuer;
          opConfigurationEndpoint = `${issuerUrl}/.well-known/openid-configuration`;
        }
      } catch (error) {
        setAlert('Invalid URL format. Please enter a valid URL.');
        setAlertSeverity('error');
        return;
      }

      // Prepare scopes
      const scopes = selectedScopes.map((ele) => ele.name).join(" ");

      // Fetch OpenID configuration
      const openidConfig = await getOpenidConfiguration(opConfigurationEndpoint);

      if (!openidConfig?.data) {
        setAlert('Error in fetching OpenID configuration!');
        setAlertSeverity('error');
        return;
      }

      const configData = openidConfig.data;

      // Validate required endpoints
      if (!configData.registration_endpoint) {
        setAlert('OpenID configuration does not contain a registration endpoint');
        setAlertSeverity('error');
        return;
      }

      // Store multiple OpenID configurations
      await storeOpenIDConfiguration(configData);

      // Register OIDC client
      const registrationUrl = configData.registration_endpoint;
      const clientId = `janssen-tarp-${uuidv4()}`;

      const registerObj: RegistrationRequest = {
        redirect_uris: [chrome.identity.getRedirectURL()],
        scope: scopes,
        post_logout_redirect_uris: [chrome.identity.getRedirectURL('logout')],
        response_types: ['code'],
        grant_types: ['authorization_code'],
        application_type: 'web',
        client_name: clientId,
        token_endpoint_auth_method: 'client_secret_basic',
        access_token_as_jwt: true,
        userinfo_signed_response_alg: "RS256",
        jansInclClaimsInIdTkn: "true",
        access_token_lifetime: 86400 // 1 day
      };

      // Add expiration if specified
      if (expireAt) {
        const lifetimeSeconds = Math.floor((expireAt.valueOf() - Date.now()) / 1000);
        if (lifetimeSeconds > 0) {
          registerObj.lifetime = lifetimeSeconds;
        }
      }

      const registrationResp = await registerOIDCClient(registrationUrl, registerObj);

      if (!registrationResp?.data) {
        setAlert(REGISTRATION_ERROR);
        setAlertSeverity('error');
        return;
      }

      // Create client object
      const newClient: OIDCClient = {
        id: uuidv4(),
        opHost: issuerUrl,
        clientId: registrationResp.data.client_id,
        clientSecret: registrationResp.data.client_secret,
        scope: registerObj.scope,
        redirectUris: registerObj.redirect_uris,
        authorizationEndpoint: configData.authorization_endpoint,
        tokenEndpoint: configData.token_endpoint,
        userinfoEndpoint: configData.userinfo_endpoint,
        acrValuesSupported: configData.acr_values_supported,
        endSessionEndpoint: configData.end_session_endpoint,
        responseType: registerObj.response_types,
        postLogoutRedirectUris: registerObj.post_logout_redirect_uris,
        expireAt: expireAt ? expireAt.valueOf() : undefined,
        showClientExpiry: !!expireAt,
        registrationDate: Date.now(),
        openidConfiguration: configData
      };

      // Store the new client
      await storeOIDCClient(newClient);

      console.log('OIDC client registered successfully!', {
        clientId: newClient.clientId,
        opHost: newClient.opHost
      });

      setAlert('Registration successful!');
      setAlertSeverity('success');

      handleClose();

    } catch (err) {
      console.error('Client registration failed:', err);
      setAlert(err instanceof Error ? err.message : REGISTRATION_ERROR);
      setAlertSeverity('error');
    } finally {
      setLoading(false);
    }
  };

  // Helper function to store multiple OpenID configurations
  const storeOpenIDConfiguration = async (config: OpenIDConfiguration): Promise<void> => {
    return new Promise((resolve, reject) => {
      chrome.storage.local.get(["openidConfigurations"], (result) => {
        if (chrome.runtime.lastError) {
          reject(chrome.runtime.lastError);
          return;
        }
        try {
          const configs: OpenIDConfiguration[] = result.openidConfigurations || [];

          // Use the authoritative issuer from the config, not the derived URL
          const authoritativeIssuer: string = config.issuer;

          // Check if configuration already exists (by issuer )
          const existingIndex = configs.findIndex(c =>
            c.issuer === authoritativeIssuer
          );

          if (existingIndex >= 0) {
            // Update existing configuration
            configs[existingIndex] = config;
          } else {
            // Add new configuration
            configs.push(config);
          }

          chrome.storage.local.set({ openidConfigurations: configs }, () => {
            if (chrome.runtime.lastError) {
              reject(chrome.runtime.lastError);
            } else {
              console.log(`OpenID configuration stored for ${authoritativeIssuer}`);
              resolve();
            }
          });

        } catch (error) {
          reject(error);
        }
      });
    });
  };

  // Helper function to store OIDC client with support for multiple clients
  const storeOIDCClient = async (client: OIDCClient): Promise<void> => {
    return new Promise((resolve, reject) => {
      chrome.storage.local.get(["oidcClients"], (result) => {
        if (chrome.runtime.lastError) {
          reject(chrome.runtime.lastError);
          return;
        }
        try {
          const clients: OIDCClient[] = result.oidcClients || [];

          // Check if client already exists (by clientId or opHost + clientId combination)
          const existingIndex = clients.findIndex(c =>
            c.clientId === client.clientId && c.opHost === client.opHost
          );

          if (existingIndex >= 0) {
            // Update existing client
            clients[existingIndex] = client;
          } else {
            // Add new client
            clients.push(client);
          }

          // Sort clients by registration date (newest first)
          clients.sort((a, b) => b.registrationDate - a.registrationDate);

          chrome.storage.local.set({ oidcClients: clients }, () => {
            if (chrome.runtime.lastError) {
              reject(chrome.runtime.lastError);
            } else {
              console.log(`Client stored: ${client.clientId}`);
              resolve();
            }
          });
        } catch (error) {
          reject(error);
        }
      });
    });
  };

  return (
    <React.Fragment>
      <Dialog
        open={open}
        onClose={handleClose}
        PaperProps={{
          component: 'form',
          onSubmit: (event) => {
            event.preventDefault();
          },
        }}
        className="form-container"
      >
        <DialogTitle>Register OIDC Client</DialogTitle>
        {loading ? (
          <div className="loader-overlay">
            <CircularProgress color="success" />
          </div>
        ) : (
          ""
        )}
        <DialogContent>
          <DialogContentText>
            Submit below details to create a new OIDC client.
          </DialogContentText>
          <Stack
            component="form"
            sx={{
              width: '75ch',
            }}
            spacing={2}
            noValidate
            autoComplete="off"
          >
            {(!!alert || alert !== '') ?
              <Alert severity={alertSeverity}>{alert}</Alert> : ''
            }
            <TextField
              error={issuerError.length !== 0}
              autoFocus
              required
              margin="dense"
              id="issuer"
              name="issuer"
              label="Issuer"
              type="text"
              fullWidth
              variant="outlined"
              helperText={issuerError}
              onBlur={(e) => {
                validateIssuer(e);
              }}
            />
            <LocalizationProvider dateAdapter={AdapterMoment}>
              <DemoContainer components={['DateTimePicker']}>
                <DateTimePicker label="Client Expiry Date"
                  disablePast
                  onChange={(newValue) => setExpireAt(newValue)}
                />
              </DemoContainer>
            </LocalizationProvider>
            <Autocomplete
              value={selectedScopes}
              multiple
              defaultValue={[scopeOptions[0]]}
              onChange={(event, newValue, reason, details) => {
                let valueList = selectedScopes;
                if (details.option.create && reason !== 'removeOption') {
                  valueList.push({ id: undefined, name: details.option.name, create: details.option.create });
                  setSelectedScopes(valueList);
                } else if (reason === 'createOption') {
                  valueList.push({ id: undefined, name: details.option, create: true });
                } else {
                  setSelectedScopes(newValue);
                }
              }}
              filterSelectedOptions
              filterOptions={(options, params) => {
                const filtered = filter(options, params);

                const { inputValue } = params;
                // Suggest the creation of a new value
                const isExisting = options.some((option) => inputValue === option.name);
                if (inputValue !== '' && !isExisting) {
                  filtered.push({
                    name: inputValue,
                    label: `Add "${inputValue}"`,
                    create: true
                  });
                }

                return filtered;
              }}
              selectOnFocus
              clearOnBlur
              handleHomeEndKeys
              id="scope"
              options={scopeOptions}
              getOptionLabel={(option) => {
                // Value selected with enter, right from the input
                if (typeof option === 'string') {
                  return option;
                }
                // Add "xxx" option created dynamically
                if (option.label) {
                  return option.name;
                }
                // Regular option
                return option.name;
              }}
              renderOption={(props, option) => <li {...props}>{option.create ? option.label : option.name}</li>}
              freeSolo
              renderInput={(params) => (
                <TextField {...params} label="Scopes" />
              )}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button type="submit" onClick={registerClient}>Register</Button>
        </DialogActions>
      </Dialog>
    </React.Fragment>
  );
}