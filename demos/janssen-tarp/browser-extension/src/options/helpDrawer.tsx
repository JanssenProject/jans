import * as React from 'react';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import Typography from '@mui/material/Typography';

export default function HelpDrawer({ isOpen, handleDrawer }) {
  const [open, setOpen] = React.useState(isOpen);
  const [width, setWidth] = React.useState((window.innerWidth * 2)/3 )
  React.useEffect(() => {
    handleDrawer(isOpen)
    setOpen(isOpen);
  }, [isOpen])

  const toggleDrawer = (newOpen: boolean) => () => {
    handleDrawer(newOpen)
    setOpen(newOpen);
  };

  const DrawerList = (

    <Box sx={{ width: width }} role="presentation">
      <div style={{ padding: "20px" }}>
        <Typography gutterBottom variant="h5" component="div">
          Step 1
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Click on 'Add Client' button for Dynamic Client Registration on authentication server.
        </Typography>
      </div>
      <div className="image-container">
        <img id="dynamic-image" src={'tarpDocs1.png'} />
      </div>

      <div style={{ padding: "20px" }}>
        <Typography gutterBottom variant="h5" component="div">
          Step 2
        </Typography>
        <Typography variant="body2" color="text.secondary">
          On registration form enter details and register.
        </Typography>
      </div>
      <div className="image-container">
        <img id="dynamic-image" src={'tarpDocs2.png'} />
      </div>

      <div style={{ padding: "20px" }}>
        <Typography gutterBottom variant="h5" component="div">
          Step 3 (Optional: For Cedarlig authorization testing)
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Add Cedarling bootstrap configuration on <b>Cedarling</b> tab.
        </Typography>
      </div>
      <div className="image-container">
        <img id="dynamic-image" src={'tarpDocs3.png'} />
      </div>

      <div style={{ padding: "20px" }}>
        <Typography gutterBottom variant="h5" component="div">
          Step 4
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Start Authentication code flow.
        </Typography>
      </div>
      <div className="image-container">
        <img id="dynamic-image" src={'tarpDocs4.png'} />
      </div>

      <div style={{ padding: "20px" }}>
        <Typography gutterBottom variant="h5" component="div">
          Step 5
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Enter required details before starting Authentication Code flow.
        </Typography>
      </div>
      <div className="image-container">
        <img id="dynamic-image" src={'tarpDocs5.png'} />
      </div>

      <div style={{ padding: "20px" }}>
        <Typography gutterBottom variant="h5" component="div">
          Step 6 (Optional: For Cedarlig authorization testing)
        </Typography>
        <Typography variant="body2" color="text.secondary">
          After authentication, test Cedarling Authorization decision using following request form.
        </Typography>
      </div>
      <div className="image-container">
        <img id="dynamic-image" src={'tarpDocs6.png'} />
      </div>
    </Box>

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