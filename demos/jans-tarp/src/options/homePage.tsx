import * as React from 'react';
import Container from '@mui/material/Container';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Box from '@mui/material/Box';
import Password from '@mui/icons-material/Password';
import LockPerson from '@mui/icons-material/LockPerson';
import OIDCClients from './OIDCClients';
import Cedarling from './cedarling';
import Grid from '@mui/material/Grid';
import Paper from '@mui/material/Paper';
import { styled } from '@mui/material/styles';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function CustomTabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

function a11yProps(index: number) {
  return {
    id: `simple-tab-${index}`,
    'aria-controls': `simple-tabpanel-${index}`,
  };
}

export default function HomePage({ data, notifyOnDataChange }) {

  const [value, setValue] = React.useState(0);

  const handleChange = (event: React.SyntheticEvent, newValue: number) => {
    setValue(newValue);
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ maxWidth: "lg", bgcolor: 'background.paper' }}>
        <Tabs
          value={value}
          onChange={handleChange}
          variant="scrollable"
          scrollButtons
          allowScrollButtonsMobile
          aria-label="scrollable force tabs example"
          TabIndicatorProps={{ sx: { bgcolor: "#148514" } }}
        >
          <Tab label="Authentication Flow" icon={<Password />} />
          <Tab label="Cedarling" icon={<LockPerson />} />
        </Tabs>
      </Box>
      <CustomTabPanel value={value} index={0}>
        <OIDCClients
          data={data.oidcClients}
          notifyOnDataChange={notifyOnDataChange}
        />
      </CustomTabPanel>
      <CustomTabPanel value={value} index={1}>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <Cedarling
              data={data.cedarlingConfig}
              notifyOnDataChange={notifyOnDataChange}
              isOidcClientRegistered={(data?.oidcClients !== undefined && data?.oidcClients?.length !== 0)}
            />
          </Grid>
        </Grid>
      </CustomTabPanel>
    </Container>
  );
}