import * as React from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import moment from 'moment';
import { v4 as uuidv4 } from 'uuid';
import { ILooseObject } from './ILooseObject';
import CircularProgress from "@mui/material/CircularProgress";
import { DemoContainer } from '@mui/x-date-pickers/internals/demo';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment'
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import Autocomplete, { createFilterOptions } from '@mui/material/Autocomplete';
import Stack from '@mui/material/Stack';
import axios from 'axios';
import Alert from '@mui/material/Alert';

export default function RegisterClient({ isOpen, handleDialog }) {
  const [open, setOpen] = React.useState(isOpen);
  const [selectedScopes, setSelectedScopes] = React.useState([])
  const [scopeOptions, setScopeOptions] = React.useState([{ name: "openid" }]);
  const [expireAt, setExpireAt] = React.useState(null);
  const [issuer, setIssuer] = React.useState(null);
  const [issuerError, setIssuerError] = React.useState("")
  const [errorMessage, setErrorMessage] = React.useState("")
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
    setErrorMessage('');
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
      setErrorMessage('');
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
      setErrorMessage('Error in fetching Openid configuration. Check error log on console.');
    }
  };

  const getOpenidConfiguration = async (opConfigurationEndpoint) => {
    try {
      setErrorMessage('');
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

  const registerClient = async () => {
    try {
      setLoading(true);
      if (issuer === 0) {
        setIssuerError('Issuer cannot be left blank. Either enter correct Issuer or OpenID Configuration URL.');
        return;
      }
      const opConfigurationEndpoint = issuer;
      const opConfigurationEndpointURL = new URL(opConfigurationEndpoint);
      const issuerUrl = opConfigurationEndpointURL.protocol + '//' + opConfigurationEndpointURL.hostname;
      const scopes = selectedScopes.map((ele) => ele.name).join(" ");

      const openidConfig = await getOpenidConfiguration(opConfigurationEndpoint);

      if (openidConfig != undefined) {
        chrome.storage.local.set({ opConfiguration: openidConfig.data }).then(() => {
          console.log("OP Configuration: " + JSON.stringify(openidConfig));
        });

        const registrationUrl = openidConfig.data.registration_endpoint;

        var registerObj: ILooseObject = {
          redirect_uris: [issuerUrl],
          scope: scopes,
          post_logout_redirect_uris: [chrome.runtime.getURL('options.html')],
          response_types: ['code'],
          grant_types: ['authorization_code'],
          application_type: 'web',
          client_name: 'jans-tarp-' + uuidv4(),
          token_endpoint_auth_method: 'client_secret_basic',
          access_token_as_jwt: true,
          userinfo_signed_response_alg: "RS256",
          jansInclClaimsInIdTkn: "true"
        };

        if (!!expireAt) {
          registerObj.lifetime = ((expireAt.valueOf() - moment().valueOf()) / 1000);
        }

        const registrationResp = await registerOIDCClient(registrationUrl, registerObj);

        if (registrationResp !== undefined) {
          chrome.storage.local.get(["oidcClients"], (result) => {
            let clientArr = []
            if (!!result.oidcClients) {
              clientArr = result.oidcClients;
            }

            clientArr.push({
              'opHost': issuerUrl,
              'clientId': registrationResp.data.client_id,
              'clientSecret': registrationResp.data.client_secret,
              'scope': registerObj.scope,
              'redirectUris': registerObj.redirect_uris,
              'authorizationEndpoint': openidConfig.data.authorization_endpoint,
              'tokenEndpoint': openidConfig.data.token_endpoint,
              'userinfoEndpoint': openidConfig.data.userinfo_endpoint,
              'acrValuesSupported': openidConfig.data.acr_values_supported,
              'endSessionEndpoint': openidConfig.data.end_session_endpoint,
              'responseType': registerObj.response_types,
              'postLogoutRedirectUris': registerObj.post_logout_redirect_uris,
              'expireAt': !!expireAt ? expireAt.valueOf() : undefined,
              'showClientExpiry': !!expireAt

            });
            chrome.storage.local.set({ oidcClients: clientArr });
          });

          console.log('OIDC client registered successfully!')
          console.log("oidcClient is set for client_id: " + registrationResp.data.client_id);
          setErrorMessage('Regstration successful!')
          handleClose();

        } else {
          setErrorMessage(REGISTRATION_ERROR)
        }
      } else {
        setErrorMessage('Error in fetching Openid configuration!')
      }
    } catch (err) {
      console.error(err)
      setErrorMessage(REGISTRATION_ERROR)
    }
    setLoading(false);
  }

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
            {(!!errorMessage || errorMessage !== '') ?
              <Alert severity="error">{errorMessage}</Alert> : ''
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
                } else if(reason === 'createOption') {
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