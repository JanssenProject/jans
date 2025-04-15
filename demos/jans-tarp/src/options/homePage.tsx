import * as React from 'react';
import Container from '@mui/material/Container';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Box from '@mui/material/Box';
import Password from '@mui/icons-material/Password';
import LockPerson from '@mui/icons-material/LockPerson';
import OIDCClients from './oidcClients';
import CedarlingMgmt from './cedarling';
import Grid from '@mui/material/Grid';
import Utils from './Utils';
import UserDetails from './userDetails'

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
          {(!Utils.isEmpty(data.loginDetails) && Object.keys(data.loginDetails).length !== 0) ?
            <Tab label="User Details" icon={<Password />} /> :
            <Tab label="Authentication Flow" icon={<Password />} />}
          <Tab label="Cedarling" icon={<LockPerson />} />
        </Tabs>
      </Box>
      <CustomTabPanel value={value} index={0}>
        {(!Utils.isEmpty(data.loginDetails) && Object.keys(data.loginDetails).length !== 0) ?
          <UserDetails
            data={data.loginDetails}
            notifyOnDataChange={notifyOnDataChange}
          /> :
          <OIDCClients
            data={data.oidcClients}
            notifyOnDataChange={notifyOnDataChange}
          />}
      </CustomTabPanel>
      <CustomTabPanel value={value} index={1}>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <CedarlingMgmt
              data={data.cedarlingConfig}
              isLoggedIn={(!Utils.isEmpty(data.loginDetails) && Object.keys(data.loginDetails).length !== 0)}
              notifyOnDataChange={notifyOnDataChange}
            />
          </Grid>
        </Grid>
      </CustomTabPanel>
    </Container>
  );
}