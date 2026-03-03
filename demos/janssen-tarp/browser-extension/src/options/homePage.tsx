import * as React from 'react';
import Container from '@mui/material/Container';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Box from '@mui/material/Box';
import Password from '@mui/icons-material/Password';
import LockPerson from '@mui/icons-material/LockPerson';
import SmartToyOutlinedIcon from '@mui/icons-material/SmartToyOutlined';
import OIDCClients from './oidcClients';
import CedarlingMgmt from './cedarling';
import Grid from '@mui/material/Grid';
import Utils from './Utils';
import UserDetails from './userDetails'
import {AIAgent} from '../ai/agentUI/index'

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

/**
 * Renders the main home page containing three tabs: user/authentication, Cedarling management, and AI Agent.
 *
 * The first tab shows either user details or the authentication (OIDC clients) flow depending on whether
 * `data.loginDetails` is present and non-empty. The second tab displays Cedarling management and receives
 * the full `data` object plus an `isLoggedIn` flag derived from `data.loginDetails`. The third tab hosts the AI Agent UI.
 *
 * @param data - Application data used to populate the tabs (expected keys include `loginDetails` and `oidcClients`)
 * @param notifyOnDataChange - Callback invoked by child components to notify the parent of data changes
 * @returns The page's React element containing the tabbed interface
 */
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
          <Tab label="AI Agent" icon={<SmartToyOutlinedIcon />} />
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
          <Grid size={{xs:12}}>
            <CedarlingMgmt
              data={data}
              isLoggedIn={(!Utils.isEmpty(data.loginDetails) && Object.keys(data.loginDetails).length !== 0)}
              notifyOnDataChange={notifyOnDataChange}
            />
          </Grid>
        </Grid>
      </CustomTabPanel>
      <CustomTabPanel value={value} index={2}>
        <Grid container spacing={2}>
          <Grid size={{xs:12}}>
            <AIAgent
              notifyOnDataChange={notifyOnDataChange}
            />
          </Grid>
        </Grid>
      </CustomTabPanel>
    </Container>
  );
}