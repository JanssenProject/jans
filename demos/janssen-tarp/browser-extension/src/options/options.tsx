import React, { useState, useEffect } from 'react';
import '../static/css/options.css';
import Header from './header';
import HomePage from './homePage';
import Utils from './Utils';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#9c27b0',
    },
    background: {
      default: '#f4f6f8',
      paper: '#ffffff',
    },
  },
  shape: {
    borderRadius: 10,
  },
  components: {
    MuiContainer: {
      defaultProps: {
        maxWidth: 'lg',
      },
    },
  },
});
const Options = () => {

  const [optionType, setOptionType] = useState("");
  const [data, setdata] = useState({});
  const [dataChanged, setDataChanged] = useState(false);

  useEffect(() => {
    // Fetch cedarlingConfig first
    chrome.storage.local.get(["cedarlingConfig"], (cedarlingConfigResult) => {
      let cedarlingConfig = Utils.isEmpty(cedarlingConfigResult) ? {} : cedarlingConfigResult;

      chrome.storage.local.get(["oidcClients"], (oidcClientResults) => {
        if (!Utils.isEmpty(oidcClientResults) && Object.keys(oidcClientResults).length !== 0) {
          chrome.storage.local.get(["loginDetails"], (loginDetailsResult) => {
            if (!Utils.isEmpty(loginDetailsResult) && Object.keys(loginDetailsResult).length !== 0) {
              setOptionType("loginPage");
              setdata({ ...loginDetailsResult, ...cedarlingConfig });
            } else {
              setOptionType("homePage");
              setdata({ ...oidcClientResults, ...cedarlingConfig });
            }
          });
        } else {
          setOptionType("homePage");
          setdata({ ...cedarlingConfig });
        }
        setDataChanged(false);
      });
    });
  }, [dataChanged]);


  function handleDataChange() {
    setDataChanged(true);
  }

  function renderPage({ optionType, data }) {
    switch (optionType) {
      case 'homePage':
        return <HomePage
          data={data}
          notifyOnDataChange={handleDataChange}
        />
      case 'loginPage':
        return <HomePage
        data={data}
        notifyOnDataChange={handleDataChange}
      />
      default:
        return null
    }
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box
        sx={{
          height: 'auto',
          width: '100%',
          display: 'flex',
          flexDirection: 'column',
          bgcolor: 'background.default',
          alignItems: 'center',
        }}
      >
        <Header />
        <Container sx={{ py: 3 }}>
          <Box
            sx={{
              bgcolor: 'background.paper',
              boxShadow: 1,
              borderRadius: 2,
              p: { xs: 2, sm: 3 },
            }}
          >
            {renderPage({ optionType, data })}
          </Box>
        </Container>
      </Box>
    </ThemeProvider>
  );
};

export default Options;
