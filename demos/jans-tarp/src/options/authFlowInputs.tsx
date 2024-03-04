import * as React from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import LinearProgress from '@mui/material/LinearProgress';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import qs from 'qs';
import axios from 'axios';
import Utils from './Utils';
import { v4 as uuidv4 } from 'uuid';
import Stack from '@mui/material/Stack';
import MenuItem from '@mui/material/MenuItem';
import { IOption } from './IOption';
import Alert from '@mui/material/Alert';
import { ILooseObject } from './ILooseObject';

const createOption = (label: string) => ({
  label,
  value: label,
});

export default function AuthFlowInputs({ isOpen, handleDialog, client }) {
  const [open, setOpen] = React.useState(isOpen);
  const [errorMessage, setErrorMessage] = React.useState("")
  const [additionalParamError, setAdditionalParamError] = React.useState("")
  const [displayToken, setDisplayToken] = React.useState(false);
  const [acrValues, setAcrValues] = React.useState<readonly IOption[]>([]);
  const [acrValueOption, setAcrValueOption] = React.useState("");
  const [additionalParams, setAdditionalParams] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  

  React.useEffect(() => {
    if (isOpen) {
      handleOpen();
    } else {
      handleClose();
    }
  }, [isOpen])

  React.useEffect(() => {
    (async () => {
      setAcrValues(client.acrValuesSupported.map((ele) => createOption(ele)));
    })();
  }, [])

  const handleAcrChange = (event) => {
    setAcrValueOption(event.target.value);
  };

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

    let options: ILooseObject = {
      scope: client?.scope,
      response_type: client?.responseType[0],
      redirect_uri: redirectUrl,
      client_id: client?.clientId,
      code_challenge_method: 'S256',
      code_challenge: hashed,
      nonce: uuidv4(),
    };

    if (!!acrValueOption) {
      options.acr_values = acrValueOption;
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
      customLaunchWebAuthFlow({
        url: authzUrl,
        redirectUrl: redirectUrl
      }, (callbackUrl, error) => {
        if (!!error) {
          console.error('Error in executing auth url: ', error)
          logout()
          reject(error)
        } else {
          console.log('Callback Url: ', callbackUrl)
          resolve(callbackUrl);
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
        scope: client?.scope
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
      }
    }
  }

  const logout = () => {
    chrome.identity.clearAllCachedAuthTokens(async () => {

      const loginDetails: string = await new Promise((resolve, reject) => {
        chrome.storage.local.get(["loginDetails"], (result) => {
          resolve(JSON.stringify(result));
        });
      });

      chrome.storage.local.remove(["loginDetails"], function () {
        var error = chrome.runtime.lastError;
        if (error) {
          console.error(error);
        } else {
          window.location.href = `${client.endSessionEndpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.runtime.getURL('options.html')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`
        }
      });
    });
  }

  const customLaunchWebAuthFlow = (options, callback) => {
    var requestId = Math.random().toString(36).substring(2);
    var redirectUrl = options.redirectUrl;
    var authUrl = options.url + "&state=" + requestId;
    chrome.tabs.create({ url: authUrl }, function (tab) {
      var intervalId = setInterval(function () {
        chrome.tabs.get(tab.id, function (currentTab) {
          if (!currentTab) {
            clearInterval(intervalId);
            chrome.tabs.remove(tab.id);
            callback(undefined, new Error('Authorization tab was closed.'));
          }
        });
      }, 1000);

      chrome.tabs.onUpdated.addListener(function listener(tabId, changeInfo, tab) {
        if (!!chrome.runtime.lastError) {
          chrome.tabs.remove(tabId);
          callback(undefined, chrome.runtime.lastError);
          chrome.tabs.onUpdated.removeListener(listener);
        }
        if (tabId === tab?.id && changeInfo?.status === "complete") {
          chrome.tabs.sendMessage(tab.id, { requestId: requestId }, function (response) {
            clearInterval(intervalId);
            chrome.tabs.query({ active: true, lastFocusedWindow: true }, tabs => {
              let url = tabs[0].url;
              const urlParams = new URLSearchParams(new URL(url).search)
              const code = urlParams.get('code')
              if (code != null && areUrlsEqual(url, redirectUrl)) {
                callback(url, undefined);
                chrome.tabs.remove(tab.id);
                setLoading(false);
                chrome.tabs.onUpdated.removeListener(listener);
              }
            });
          });
        }
      })
    });
  }

  const areUrlsEqual = (url1, url2) => {
    // Create URL objects for comparison

    const parsedUrl1 = new URL(url1);
    const parsedUrl2 = new URL(url2);

    // Compare individual components
    return (
      parsedUrl1.protocol === parsedUrl2.protocol &&
      parsedUrl1.host === parsedUrl2.host &&
      parsedUrl1.pathname === parsedUrl2.pathname &&
      parsedUrl1.port === parsedUrl2.port
    );
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
      >
        <DialogTitle>Authentication Flow Inputs</DialogTitle>
        {loading ? <LinearProgress color="success" /> : ''}
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
              error={additionalParamError.length !== 0}
              placeholder='e.g. {"paramOne": "valueOne", "paramTwo": "valueTwo"}'
              autoFocus
              required
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
              value={client.additionalParams}
            />

            <InputLabel id="acr-value-label">Acr Value</InputLabel>
            <Select
              labelId="acr-value-label"
              id="acrValue"
              onChange={handleAcrChange}
            >
              {acrValues.map((ele) => (
                <MenuItem value={ele.label}>{ele.label}</MenuItem>
              ))}
            </Select>
            <FormControlLabel control={<Checkbox color="success" onChange={() => setDisplayToken(!displayToken)}/>} label="Display Access Token and ID Token after authentication" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button type="submit" onClick={triggerCodeFlow}>Trigger</Button>
        </DialogActions>
      </Dialog>
    </React.Fragment>
  );
}