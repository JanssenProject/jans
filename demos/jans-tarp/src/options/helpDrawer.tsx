import * as React from 'react';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardMedia from '@mui/material/CardMedia';
import Typography from '@mui/material/Typography';

export default function HelpDrawer({ isOpen, handleDrawer }) {
  const [open, setOpen] = React.useState(isOpen);

  React.useEffect(() => {
    handleDrawer(isOpen)
    setOpen(isOpen);
  }, [isOpen])

  const toggleDrawer = (newOpen: boolean) => () => {
    handleDrawer(newOpen)
    setOpen(newOpen);
  };

  const DrawerList = (
    <>
      <Box sx={{ width: 1184 }} role="presentation">
        <Card sx={{ maxWidth: 1184 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 1
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Click on 'Add Client' button for Dynamic Client Registration on authentication server.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 266 }}
            image={'tarpDocs1.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 586 }} role="presentation">
        <Card sx={{ maxWidth: 586 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 2
            </Typography>
            <Typography variant="body2" color="text.secondary">
              On registration form enter details and register.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 445 }}
            image={'tarpDocs2.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 1184 }} role="presentation">
        <Card sx={{ maxWidth: 1184 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 3
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Trigger authentication flow.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 266 }}
            image={'tarpDocs3.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 575 }} role="presentation">
        <Card sx={{ maxWidth: 575 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 4
            </Typography>
            <Typography variant="body2" color="text.secondary">
              The Registered client can be used to trigger authentication flow until it is expired.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 459 }}
            image={'tarpDocs4.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 575 }} role="presentation">
        <Card sx={{ maxWidth: 575 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Optional
            </Typography>
            <Typography variant="body2" color="text.secondary">
              For <b>'Acr Values'</b> input, you can also add a new ACR option.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 459 }}
            image={'tarpDocs5.png'}
          />
        </Card>
      </Box>
    </>
  );

  return (
    <div>
      {/*<Button onClick={toggleDrawer(true)}>Open drawer</Button>*/}
      <Drawer open={open} onClose={toggleDrawer(false)}>
        {DrawerList}
      </Drawer>
    </div>
  );
}