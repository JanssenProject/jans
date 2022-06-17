import { View, StyleSheet, Text, TouchableOpacity, Image } from 'react-native'
import React from 'react'
import {createStackNavigator} from '@react-navigation/stack';
import navigationStrings from '../constants/navigationStrings';
import imagePath from '../constants/imagePath';
import commonStyles from '../styles/commonStyles';
import fontFamily from '../styles/fontFamily';
import WrapperContainer from "../Components/WrapperContainer"
import colors from '../styles/colors';
import { moderateScaleVertical } from '../styles/responsiveDesign';
const Stack = createStackNavigator();

const CustomDrawerContent = ({navigation}) => {

  const navigateToScreen = (screen) => () => {
    navigation.navigate(screen)
  }

  return (
    <WrapperContainer bgColor='#f3f3f3' statusBarColor={colors.themeColor}>
      <View style={styles.header}>
          <TouchableOpacity
          onPress={() => navigation.closeDrawer()}
          style = {{alignItems: "center", justifyContent: "center"}}
          >
          <Image
            source={imagePath.leftIconWhite}
            style={styles.backIcon}
          />
          </TouchableOpacity>
          <Text style = {styles.headingText}>
            {/* Logs */}
          </Text>
            <View style={{width: 25, height: 25}} />
        </View>
      <View style = {styles.section}>
        <Text style = {styles.sectionHeading}>HISTORY</Text>
        <View style = {styles.menuBox}>
          <TouchableOpacity style = {styles.menu}
          onPress = {navigateToScreen(navigationStrings.LOGS)}
          >
            <View style = {{flexDirection: "row", alignItems: "center"}}>
              <Image source={imagePath.logsDrawerIcon} style = {styles.menuIcon} />
              <Text style = {styles.menuText}>Logs</Text>
            </View>
            <Image style = {styles.menuRightIcon} source={imagePath.rightIconGrey} />
          </TouchableOpacity>
          <View style = {styles.partition} />
          <TouchableOpacity style = {styles.menu}
          onPress = {navigateToScreen(navigationStrings.KEY)}
          >
            <View style = {{flexDirection: "row", alignItems: "center"}}>
              <Image source={imagePath.keyDrawerIcon} style = {styles.menuIcon} />
              <Text style = {styles.menuText}>Keys</Text>
            </View>
            <Image style = {styles.menuRightIcon} source={imagePath.rightIconGrey} />
          </TouchableOpacity>
        </View>
      </View>

      <View style = {styles.section}>
        <Text style = {styles.sectionHeading}>SETTINGS</Text>
        <View style = {styles.menuBox}>
          <TouchableOpacity style = {styles.menu}
          onPress = {navigateToScreen(navigationStrings.PASSCODE)}
          >
            <View style = {{flexDirection: "row", alignItems: "center"}}>
              <Image source={imagePath.passcodeDrawerIcon} style = {styles.menuIcon} />
              <Text style = {styles.menuText}>Passcode</Text>
            </View>
            <Image style = {styles.menuRightIcon} source={imagePath.rightIconGrey} />
          </TouchableOpacity>
          <View style = {styles.partition} />
          <TouchableOpacity style = {styles.menu}
          onPress = {navigateToScreen(navigationStrings.TRUST_SSL)}
          >
            <View style = {{flexDirection: "row", alignItems: "center"}}>
              <Image source={imagePath.trustDrawerIcon} style = {styles.menuIcon} />
              <Text style = {styles.menuText}>Trust all SSL</Text>
            </View>
            <Image style = {styles.menuRightIcon} source={imagePath.rightIconGrey} />
          </TouchableOpacity>
        </View>
      </View>

      <View style = {styles.section}>
        <Text style = {styles.sectionHeading}>HELP</Text>
        <View style = {styles.menuBox}>
          <View style = {styles.menu}>
            <View style = {{flexDirection: "row", alignItems: "center"}}>
              <Text style = {styles.menuText}>User Guide</Text>
            </View>
            <Image style = {styles.menuRightIcon} source={imagePath.rightIconGrey} />
          </View>
          <View style = {styles.partition} />
          <View style = {styles.menu}>
            <View style = {{flexDirection: "row", alignItems: "center"}}>
              <Text style = {styles.menuText}>Provacu Policy</Text>
            </View>
            <Image style = {styles.menuRightIcon} source={imagePath.rightIconGrey} />
          </View>
        </View>
      </View>
    </WrapperContainer>
  )
}

const styles = StyleSheet.create({
  header: {
    backgroundColor: colors.themeColor,
    flexDirection: 'row',
    justifyContent: 'space-between',
    padding: 10,
  },
  backIcon: {width: 25, height: 25, resizeMode: 'contain'},
  headingText: {
    ...commonStyles.fontSize15,
    color: colors.white,
    fontFamily: fontFamily.semiBold,
  },
  topBar: {
    backgroundColor: colors.themeColor, padding: 15
  },
  backIcon: {
    width: 20, height: 20, resizeMode: "contain"
  },
  section: {
    marginTop: moderateScaleVertical(30)
  },
  sectionHeading: {
    ...commonStyles.fontSize14,
    color: "#6b6b6b",
    marginLeft: 10,
  },
  menuBox: {
    borderTopWidth: 0.5,
    borderBottomWidth: 0.5,
    marginTop: 5,
    borderColor: "#ccc"
  },
  menu: {
    flexDirection: "row",
    justifyContent: "space-between",
    padding: 8,
    alignItems: "center",
    backgroundColor: "#fff"
  },
  menuIcon: {
    width: moderateScaleVertical(30),
    height: moderateScaleVertical(30),
    resizeMode: "contain",
    marginRight: 12
  },
  menuText: {
    ...commonStyles.fontSize15,
    color: "#666",
  },
  menuRightIcon: {
    width: 15,
    height: 15,
    resizeMode: "contain"
  },
  partition: {
    marginLeft: 15,
    height: 0.5,
    backgroundColor: "#ccc"
  }
})

export default CustomDrawerContent