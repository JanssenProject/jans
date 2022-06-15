import {StyleSheet} from 'react-native';
import colors from '../../styles/colors';
import commonStyles from '../../styles/commonStyles';
import fontFamily from '../../styles/fontFamily';
import { width } from '../../styles/responsiveDesign';

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
  mainImageContainer: {
    justifyContent: "center",
    alignItems: "center",
    paddingVertical: 50
  },
  mainImage: {
      width: width/4,
      height: width/4
  },
  mainText: {
      ...commonStyles.fontSize12,
      marginHorizontal: width/8,
      color: "#666",
      textAlign: "center",
      marginBottom: 25
  },
  rowContainer: {
      flexDirection: "row",
      justifyContent: "space-between",
      paddingHorizontal: 12,
      paddingVertical: 16,
      backgroundColor: "#fff"
  },
  rowText: {
      ...commonStyles.fontSize14,
      fontFamily: fontFamily.semiBold,
      marginBottom: 4
  },
  rowTextSecondary: {
      ...commonStyles.fontSize12,
       color: colors.blackOpacity40
  }
});
