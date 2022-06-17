import React from "react";
import { View, Text } from "react-native";
import { NavigationContainer } from "@react-navigation/native";
import { createStackNavigator } from "@react-navigation/stack";
import AuthStack from "./Authstack";
import MainStack from "./MainStack";

const Stack = createStackNavigator();

export default function Routes() {
  return (
    <NavigationContainer
    //  ref={(ref) => NavigationService.setTopLevelNavigator(ref)}
    >
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
        }}
      >
          {AuthStack()}
          {MainStack()}
        {/* {!userData ? AuthStack() : MainStack()} */}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
