import React from 'react';
import {createStackNavigator} from '@react-navigation/stack';
import {LandingPage} from '../Screens';
import navigationStrings from '../constants/navigationStrings';
const Stack = createStackNavigator();
export default function AuthStack() {
  return (
    // <Stack.Navigator>
    <React.Fragment>
      <Stack.Screen
        name={navigationStrings.LANDING_PAGE}
        component={LandingPage}
      />
    </React.Fragment>

    // </Stack.Navigator>
  );
}
