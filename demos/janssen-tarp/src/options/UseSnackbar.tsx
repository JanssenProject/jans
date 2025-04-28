import * as React from 'react';
import { useSnackbar } from '@mui/base/useSnackbar';
import { ClickAwayListener } from '@mui/base/ClickAwayListener';
import { css, keyframes, styled } from '@mui/system';

export default function UseSnackbar({isSnackbarOpen, handleSnackbar, message}) {
  const [open, setOpen] = React.useState(isSnackbarOpen);

  React.useEffect(() => {
    if (isSnackbarOpen) {
      handleOpen();
    } else {
      handleClose();
    }
  }, [isSnackbarOpen]);

  const handleClose = () => {
    setOpen(false);
    handleSnackbar(false);
  };

  const { getRootProps, onClickAway } = useSnackbar({
    onClose: handleClose,
    open,
    autoHideDuration: 1000,
  });

  const handleOpen = () => {
    setOpen(true);
    handleSnackbar(true);
  };

  return (
    <React.Fragment>
      {open ? (
        <ClickAwayListener onClickAway={onClickAway}>
          <CustomSnackbar {...getRootProps()}>{message}</CustomSnackbar>
        </ClickAwayListener>
      ) : null}
    </React.Fragment>
  );
}

const blue = {
  50: '#F0F7FF',
  100: '#C2E0FF',
  200: '#99CCF3',
  300: '#66B2FF',
  400: '#3399FF',
  500: '#007FFF',
  600: '#0072E5',
  700: '#0059B2',
  800: '#004C99',
  900: '#003A75',
};

const grey = {
  50: '#F3F6F9',
  100: '#E5EAF2',
  200: '#DAE2ED',
  300: '#C7D0DD',
  400: '#B0B8C4',
  500: '#9DA8B7',
  600: '#6B7A90',
  700: '#434D5B',
  800: '#303740',
  900: '#1C2025',
};

const snackbarInRight = keyframes`
  from {
    transform: translateX(100%);
  }

  to {
    transform: translateX(0);
  }
`;

const CustomSnackbar = styled('div')(
  ({ theme }) => css`
    position: fixed;
    z-index: 5500;
    display: flex;
    right: 16px;
    bottom: 16px;
    left: auto;
    justify-content: start;
    max-width: 560px;
    min-width: 300px;
    background-color: ${theme.palette.mode === 'dark' ? grey[900] : '#fff'};
    border-radius: 8px;
    border: 1px solid ${theme.palette.mode === 'dark' ? grey[700] : grey[200]};
    box-shadow: ${theme.palette.mode === 'dark'
      ? `0 4px 8px rgb(0 0 0 / 0.7)`
      : `0 4px 8px rgb(0 0 0 / 0.1)`};
    padding: 0.75rem;
    color: ${theme.palette.mode === 'dark' ? blue[200] : blue[700]};
    font-family: 'IBM Plex Sans', sans-serif;
    font-weight: 500;
    animation: ${snackbarInRight} 200ms;
    transition: transform 0.2s ease-out;
  `,
);