import {StyleSheet} from 'react-native';
import colors from '../../styles/colors';
import commonStyles from '../../styles/commonStyles';
import fontFamily from '../../styles/fontFamily';
import { moderateScaleVertical, width } from '../../styles/responsiveDesign';

export default StyleSheet.create({
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
  passCodeRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      backgroundColor: colors.white, paddingVertical: 12, paddingHorizontal: 16, borderTopWidth:0.5, borderBottomWidth: 0.5, borderColor: colors.blackOpacity30, alignItems: "center"
  },
  menuIcon: {
    width: moderateScaleVertical(30),
    height: moderateScaleVertical(30),
    resizeMode: "contain",
    marginRight: 12
  },
  passcodeText: {
      ...commonStyles.fontSize12,
      color: colors.blackOpacity50,
      paddingVertical: 15,
      paddingHorizontal: 25
  },
  rightImgArrow: {
      width: 12,
      height: 12,
      resizeMode: "contain"
  },
  toggleImg: {
    width: 30,
    height: 30,
    resizeMode: "contain"
},
});
