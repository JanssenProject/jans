import * as React from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import CircularProgress from "@mui/material/CircularProgress";
import Stack from '@mui/material/Stack';
import Alert from '@mui/material/Alert';
import initWasm, { init, Cedarling } from "@janssenproject/cedarling_wasm";
import { v4 as uuidv4 } from 'uuid';
import Radio from '@mui/material/Radio';
import RadioGroup from '@mui/material/RadioGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import { JsonEditor } from 'json-edit-react';
import axios from 'axios';
import Utils from './Utils';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import { pink } from '@mui/material/colors';
import cedarlingBootstrapJson from './cedarlingBootstrap.json';
import Chip from '@mui/material/Chip';
import ViewListIcon from '@mui/icons-material/ViewList';
import UseSnackbar from './UseSnackbar';
export default function AddCedarlingConfig({ isOpen, handleDialog, newData }) {
  const [open, setOpen] = React.useState(isOpen);
  const [bootstrap, setBootstrap] = React.useState(newData);
  const [errorMessage, setErrorMessage] = React.useState("")
  const [loading, setLoading] = React.useState(false);
  const [inputSelection, setInputSelection] = React.useState("json");
  const [showConfiguration, setShowConfiguration] = React.useState(false);
  const [showConfigurationButton, setShowConfigurationButton] = React.useState(true);
  const [snackbar, setSnackbar] = React.useState({ open: false, message: '' });

  const ADD_BOOTSTRAP_ERROR = 'Error in adding bootstrap. Check web console for logs.'

  React.useEffect(() => {
    if (isOpen) {
      handleOpen();
    } else {
      handleClose();
    }
  }, [isOpen]);

  React.useEffect(() => {
    if (Utils.isEmpty(newData) || Object.keys(newData).length === 0) {
      setBootstrap({});
      setShowConfiguration(true);
      setShowConfigurationButton(true);
    } else {
      setBootstrap(newData);
      setShowConfiguration(false);
      setShowConfigurationButton(false);
    }
  }, [newData]);

  const handleClose = () => {
    setInputSelection('json')
    handleDialog(false)
    setOpen(false);
  };

  const handleOpen = () => {
    setErrorMessage('');
    setLoading(false);
    handleDialog(true)
    setOpen(true);
  };

  const copyToClipboard = () => {
    try {
      const jsonString = JSON.stringify(bootstrap, null, 2); // pretty print
      navigator.clipboard.writeText(jsonString);
      setSnackbar({ open: true, message: 'JSON copied to clipboard!' });
    } catch (error) {
      setSnackbar({ open: true, message: 'Copy failed: ' + error.message });
    }
  };

  const validateBootstrap = async (e) => {
    let bootstrap = e.target.value;
    setErrorMessage('');
    if (inputSelection === 'url') {
      let bootstrapUrl = e.target.value;
      if (bootstrapUrl === '') {
        setErrorMessage('URL is required.');
        return false;
      }
      const oidcConfigOptions = {
        method: 'GET',
        url: bootstrapUrl,
      };
      const response = await axios(oidcConfigOptions);
      bootstrap = response.data;

    } else if (inputSelection === 'json') {
      bootstrap = e.target.value;
    }
    if (Utils.isEmpty(bootstrap) || Object.keys(bootstrap).length === 0) {
      setErrorMessage('Empty authorization request not allowed.');
      return false;
    }
    isJsonValid(bootstrap);
  };

  const isJsonValid = async (bootstrap) => {
    setErrorMessage('');
    try {
      setBootstrap(JSON.parse(JSON.stringify(bootstrap)));
      return true;
    } catch (err) {
      console.error(err)
      setErrorMessage(`Invalid input: ${err}`);
      return false;
    }
  };

  const saveBootstrap = async () => {
    try {
      setLoading(true);
      if (!isJsonValid(bootstrap)) {
        return;
      }

      await initWasm();
      let instance: Cedarling = await init(bootstrap);

      chrome.storage.local.get(["cedarlingConfig"], (result) => {
        let bootstrapArr = []

        let idObj = { id: uuidv4() };

        bootstrapArr.push({ ...bootstrap, ...idObj });
        chrome.storage.local.set({ cedarlingConfig: bootstrapArr });
        handleClose();
      });
    } catch (err) {
      console.error(err)
      setErrorMessage(ADD_BOOTSTRAP_ERROR + err)
    }
    setLoading(false);
  }

  return (
    <React.Fragment>
      <UseSnackbar isSnackbarOpen={snackbar.open} handleSnackbar={(open) => setSnackbar({ ...snackbar, open })} message={snackbar.message}/>
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
        <DialogTitle>Add Cedarling Configuration</DialogTitle>
        {loading ? (
          <div className="loader-overlay">
            <CircularProgress color="success" />
          </div>
        ) : (
          ""
        )}
        <DialogContent>
          <DialogContentText>
            Submit below details.
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
            <div style={{ display: 'flex', justifyContent: 'space-between', maxWidth: 'min(600px, 90vw)' }}>
              <RadioGroup
                row
                aria-labelledby="demo-row-radio-buttons-group-label"
                name="row-radio-buttons-group"
                defaultValue="json"
              >
                <FormControlLabel value="json" control={<Radio onClick={() => { setErrorMessage(''); setInputSelection("json"); }} color="success" />} label="JSON" />
                <FormControlLabel value="url" control={<Radio onClick={() => { setErrorMessage(''); setInputSelection("url") }} />} label="URL" />
              </RadioGroup>

              {inputSelection === 'json' &&
                (<Tooltip title="Copy JSON configuration">
                  <IconButton aria-label="Copy" style={{ maxWidth: '5vmax', float: 'right' }} onClick={copyToClipboard}>
                    <ContentCopyIcon sx={{ color: pink[500] }} />
                  </IconButton>
                </Tooltip>)}
            </div>

            {inputSelection === 'json' &&
              (
                <>
                {showConfigurationButton && (
                <div style={{maxWidth: '50vmax'}}>
                  <Chip icon={<ViewListIcon />}
                    label={showConfiguration ? "Show Minimal Configuration" : "Remove Minimal Configuration"}
                    variant="outlined"
                    onClick={() => {
                      if (showConfiguration) {
                        setBootstrap(cedarlingBootstrapJson);
                        setShowConfiguration(false);
                      } else {
                        setBootstrap({});
                        setShowConfiguration(true);
                      }
                    }
                    }
                  />
                  </div>)}
                  <JsonEditor
                    data={bootstrap}
                    setData={setBootstrap}
                    rootName="bootstrapConfig"
                    icons={{ copy: <ContentCopyIcon /> }}
                  />
                </>
              )}
            {inputSelection === 'url' &&
              (<TextField
                error={errorMessage.length !== 0}
                autoFocus
                required
                margin="dense"
                id="bootstrapUrl"
                name="bootstrapUrl"
                label="Bootstrap Configuration URL"
                type="text"
                fullWidth
                variant="outlined"
                helperText={errorMessage}
                onBlur={(e) => {
                  validateBootstrap(e);
                }}
              />)}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button type="submit" onClick={saveBootstrap}>Save</Button>
        </DialogActions>
      </Dialog>
    </React.Fragment>
  );
}
