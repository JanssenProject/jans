import React, { useState, useEffect } from 'react';
import '../static/css/options.css';
import Header from './header';
import HomePage from './homePage';
import Utils from './Utils';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import theme from '../theme/theme';

const Options = () => {

  const [optionType, setOptionType] = useState("");
  const [data, setdata] = useState({});
  const [dataChanged, setDataChanged] = useState(false);

  useEffect(() => {
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
      case 'loginPage':
        return <HomePage
          data={data}
          notifyOnDataChange={handleDataChange}
        />;
      default:
        return null;
    }
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box
        sx={{
          minHeight: '100vh',
          width: '100%',
          display: 'flex',
          flexDirection: 'column',
          bgcolor: 'background.default',
        }}
      >
        <Header />
        <Container sx={{ py: 4, flex: 1 }}>
          <Box
            sx={{
              bgcolor: 'background.paper',
              boxShadow: 2,
              borderRadius: 3,
              overflow: 'hidden',
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
