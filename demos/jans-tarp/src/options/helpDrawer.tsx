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
      <Box sx={{ width: 1126 }} role="presentation">
        <Card sx={{ maxWidth: 1126 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 1
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Click on 'Add Client' button for Dynamic Client Registration on authentication server.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 460 }}
            image={'tarpDocs1.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 1126 }} role="presentation">
        <Card sx={{ maxWidth: 1126 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 2
            </Typography>
            <Typography variant="body2" color="text.secondary">
              On registration form enter details and register.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 617 }}
            image={'tarpDocs2.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 1126 }} role="presentation">
        <Card sx={{ maxWidth: 1126 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 3 (Optional: For Cedarlig authorization testing)
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Add Cedarling bootstrap configuration on <b>Cedarling</b> tab.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 294 }}
            image={'tarpDocs3.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 1126 }} role="presentation">
        <Card sx={{ maxWidth: 1126 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 4
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Start Authentication code flow.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 294 }}
            image={'tarpDocs4.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 1126 }} role="presentation">
        <Card sx={{ maxWidth: 1126 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 5
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Enter required details before starting Authentication Code flow.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 653 }}
            image={'tarpDocs5.png'}
          />
        </Card>
      </Box>
      <Box sx={{ width: 739 }} role="presentation">
        <Card sx={{ maxWidth: 739 }}>
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              Step 6 (Optional: For Cedarlig authorization testing)
            </Typography>
            <Typography variant="body2" color="text.secondary">
              After authentication, test Cedarling Authorization decision using following request form.
            </Typography>
          </CardContent>
          <CardMedia
            sx={{ height: 767 }}
            image={'tarpDocs6.png'}
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