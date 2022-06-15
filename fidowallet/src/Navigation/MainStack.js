import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native'

import {createStackNavigator} from '@react-navigation/stack';
import DrawerStack from './DrawerStack';
import navigationStrings from '../constants/navigationStrings';
import { KeyDetails, Keys, LogDetails, Logs, Passcode, QRScan, TrustSSL } from '../Screens';

const Stack = createStackNavigator();

export default function AuthStack() {
  return (
    <React.Fragment>
      <Stack.Screen
        name={navigationStrings.DRAWER_STACK}
        component={DrawerStack}
      />
      <Stack.Screen name={navigationStrings.LOGS} component={Logs} />
      <Stack.Screen name={navigationStrings.KEY} component={Keys} />
      <Stack.Screen name={navigationStrings.PASSCODE} component={Passcode} />
      <Stack.Screen name={navigationStrings.TRUST_SSL} component={TrustSSL} />
      <Stack.Screen name={navigationStrings.KEY_DETAIL} component={KeyDetails} />
      <Stack.Screen name={navigationStrings.LOG_DETAILS} component={LogDetails} />
      <Stack.Screen name={navigationStrings.QR_SCAN} component={QRScan} />
    </React.Fragment>
  );
}
