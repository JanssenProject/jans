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
    paddingVertical: 20,
    backgroundColor: colors.white
  },
  mainImage: {
      width: width/4,
      height: width/4
  },
  mainText: {
      ...commonStyles.fontSize14,
      marginHorizontal: width/8,
      color: "#666",
      textAlign: "center",
  },
  secondText: {
    ...commonStyles.fontSize12,
    marginHorizontal: width/8,
    color: "#666",
    textAlign: "center",
    marginBottom: 25,
    color: colors.themeColor,
    marginTop: 10
},
section: {
    marginHorizontal: 15,
    borderTopWidth: 0.5,
    borderColor: colors.blackOpacity30,
    paddingVertical: 10,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    backgroundColor: colors.white
},
rowIcon: {
    width: 30, 
    height: 30, 
    resizeMode: "contain"
},
rightTestOne: {
    ...commonStyles.fontSize14,
    color: colors.blackOpacity60,
    marginBottom: 2
},
rightTestTwo: {
    ...commonStyles.fontSize14,
    color: colors.themeText
},
});
