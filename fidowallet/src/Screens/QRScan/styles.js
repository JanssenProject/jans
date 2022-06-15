import {StyleSheet} from "react-native"
import colors from "../../styles/colors"
import commonStyles from "../../styles/commonStyles"
import fontFamily from "../../styles/fontFamily"

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
      rectangleContainer: {
        // backgroundColor: "red",
        height: 250,
        width: 250,
        borderWidth: 1,
        borderRadius: 15,
        borderStyle: "dashed",
        borderColor: colors.white
      },
})