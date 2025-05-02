import * as React from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import InputLabel from '@mui/material/InputLabel';
import CircularProgress from "@mui/material/CircularProgress";
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import qs from 'qs';
import axios from 'axios';
import Utils from './Utils';
import { v4 as uuidv4 } from 'uuid';
import Stack from '@mui/material/Stack';
import Alert from '@mui/material/Alert';
import { ILooseObject } from './ILooseObject';
import Autocomplete, { createFilterOptions } from '@mui/material/Autocomplete';
const createOption = (label: string) => ({
  name: label,
});
const filter = createFilterOptions();
export default function AuthFlowInputs({ isOpen, handleDialog, client, notifyOnDataChange }) {
  const [open, setOpen] = React.useState(isOpen);
  const [errorMessage, setErrorMessage] = React.useState("")
  const [additionalParamError, setAdditionalParamError] = React.useState("")
  const [displayToken, setDisplayToken] = React.useState(false);
  const [additionalParams, setAdditionalParams] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  const [acrValueOption, setAcrValueOption] = React.useState([]);
  const [selectedAcr, setSelectedAcr] = React.useState([])
  const [selectedScopes, setSelectedScopes] = React.useState([])
  const [scopeOptions, setScopeOptions] = React.useState([{ name: "openid" }]);

  React.useEffect(() => {
    if (isOpen) {
      handleOpen();
    } else {
      handleClose();
    }
  }, [isOpen])

  React.useEffect(() => {
    (async () => {
      const scopes = client?.scope.split(" ");
      setAcrValueOption(client?.acrValuesSupported.map((ele) => createOption(ele)));
      setScopeOptions(scopes.map((ele) => ({name: ele})))
      
    })();
  }, [])

  const handleClose = () => {
    handleDialog(false)
    setOpen(false);
  };

  const handleOpen = () => {
    handleDialog(true)
    setOpen(true);
  };

  const validateJson = (e) => {
    try {
      setAdditionalParamError('')
      let addParams = e.target.value;
      if(addParams.trim() === '') {
        return;
      }
      JSON.parse(addParams);
      setAdditionalParams(addParams);
    } catch (e) {
      setAdditionalParamError('Error in parsisng JSON.')
    }
  }

  const triggerCodeFlow = async () => {
    setLoading(true);
    const redirectUrl = client?.redirectUris[0];
    const { secret, hashed } = await Utils.generateRandomChallengePair();
    let scopes = selectedScopes.map((ele) => ele.name).join(" ");
    if(!(!!scopes && scopes.length > 0)) {
      scopes = client?.scope;
    }

    let options: ILooseObject = {
      scope: scopes,
      response_type: client?.responseType[0],
      redirect_uri: redirectUrl,
      client_id: client?.clientId,
      code_challenge_method: 'S256',
      code_challenge: hashed,
      nonce: uuidv4(),
    };

    if (!!selectedAcr && selectedAcr.length > 0) {
      options.acr_values = selectedAcr[0].name;
    }

    let authzUrl = `${client?.authorizationEndpoint}?${qs.stringify(options)}`;

    if (!!additionalParams && additionalParams.trim() != '') {
      client.additionalParams = additionalParams.trim();
      chrome.storage.local.get(["oidcClients"], (result) => {
        let clientArr = []
        if (!!result.oidcClients) {
          clientArr = result.oidcClients;
          clientArr = clientArr.map(obj => obj.clientId === client.clientId ? client : obj);
          chrome.storage.local.set({ oidcClients: clientArr });
        }
      });

      let additionalParamJSON = JSON.parse(additionalParams)
      console.log('Processing additional parameters');
      Object.keys(additionalParamJSON).forEach(key => {
        console.log(key + "~~~" + additionalParamJSON[key]);
        authzUrl += `&${key}=${additionalParamJSON[key]}`
      });
    }

    console.log('Obtained autorization URL: ' + authzUrl)

    const resultUrl: string = await new Promise((resolve, reject) => {
    chrome.identity.launchWebAuthFlow({
      url: authzUrl,
      interactive: true
    }, (responseUrl) => {
      if (chrome.runtime.lastError || !responseUrl) {
        console.error("Authentication failed:", chrome.runtime.lastError || "No redirect URL");
        reject("Authentication failed:" + chrome.runtime.lastError || "No redirect URL")
      } else {
        resolve(responseUrl)
      }
    });
  });

    if (resultUrl) {
      const urlParams = new URLSearchParams(new URL(resultUrl).search)
      const code = urlParams.get('code')
      console.log('code:' + code)

      const tokenReqData = qs.stringify({
        redirect_uri: redirectUrl,
        grant_type: 'authorization_code',
        code_verifier: secret,
        client_id: client?.clientId,
        code,
        scope: scopes
      })

      const tokenReqOptions = {
        method: 'POST',
        headers: { 'content-type': 'application/x-www-form-urlencoded', 'Authorization': 'Basic ' + btoa(`${client?.clientId}:${client?.clientSecret}`) },
        data: tokenReqData,
        url: client.tokenEndpoint,
      };

      const tokenResponse = await axios(tokenReqOptions);

      if (
        tokenResponse &&
        tokenResponse.data &&
        tokenResponse.data.access_token
      ) {
        console.log('tokenResponse:' + JSON.stringify(tokenResponse))

        const userInfoOptions = {
          method: 'GET',
          headers: { 'Authorization': `Bearer ${tokenResponse.data.access_token}` },
          url: client.userinfoEndpoint,
        };

        const userInfoResponse = await axios(userInfoOptions);

        chrome.storage.local.set({
          loginDetails: {
            'access_token': tokenResponse.data.access_token,
            'userDetails': userInfoResponse.data,
            'id_token': tokenResponse.data.id_token,
            'displayToken': displayToken,
          }
        }).then(async () => {
          console.log("userDetails: " + JSON.stringify(userInfoResponse.data));
          handleClose();
        });
        notifyOnDataChange();
      }
    }
  }

  return (
    <React.Fragment>
      <Dialog
        open={open}
        onClose={(event, reason) => {
          if (reason !== "backdropClick") {
            handleClose();
          }
        }}
        PaperProps={{
          component: 'form',
          onSubmit: (event) => {
            event.preventDefault();            
          },
        }}
        className="form-container"
      >
        <DialogTitle>Authentication Flow Inputs</DialogTitle>
        {loading ? (
          <div className="loader-overlay">
            <CircularProgress color="success" />
          </div>
        ) : (
          ""
        )}
        <DialogContent>
          <DialogContentText>
            Enter inputs (optional) before initiating authentication flow.
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
              error={additionalParamError.length !== 0}
              placeholder='e.g. {"paramOne": "valueOne", "paramTwo": "valueTwo"}'
              autoFocus
              margin="dense"
              id="additionalParams"
              name="additionalParams"
              label="Additional Params"
              type="text"
              fullWidth
              variant="outlined"
              helperText={additionalParamError}
              onBlur={(e) => {
                validateJson(e);
              }}
              defaultValue={client.additionalParams}
            />

            <InputLabel id="acr-value-label">Acr Value</InputLabel>
            <Autocomplete
              value={selectedAcr}
              multiple
              defaultValue={[acrValueOption[0]]}
              onChange={(event, newValue, reason, details) => {
                if (reason === 'removeOption') {
                  setSelectedAcr([]);  
                } else if (reason === 'selectOption') {
                  setSelectedAcr([{id: undefined, name: details.option.name}]);
                } else if(reason === 'createOption') {
                  setSelectedAcr([{id: undefined, name: details.option}]);
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
              //selectOnFocus
              clearOnBlur
              handleHomeEndKeys
              id="acrValue"
              options={acrValueOption}
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
                <TextField {...params} label="Acr Values" />
              )}
            />
            <InputLabel id="scope-value-label">Scope</InputLabel>
            <Autocomplete
              value={selectedScopes}
              multiple
              defaultValue={[scopeOptions[0]]}
              onChange={(event, newValue, reason, details) => {
                let valueList = selectedScopes;
                if (details.option.create && reason !== 'removeOption') {
                  valueList.push({ id: undefined, name: details.option.name, create: details.option.create });
                  setSelectedScopes(valueList);
                }
                else {
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
            
            <FormControlLabel control={<Checkbox color="success" onChange={() => setDisplayToken(!displayToken)}/>} label="Display Access Token and ID Token after authentication" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button type="submit" onClick={triggerCodeFlow}>Trigger Auth Flow</Button>
        </DialogActions>
      </Dialog>
    </React.Fragment>
  );
}