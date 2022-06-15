import { StyleSheet } from "react-native";

import {
  textScale,
  scale,
  moderateScaleVertical,
  width,
  verticalScale,
  moderateScale,
} from "./responsiveDesign";
import fontFamily from "./fontFamily";
import colors from "./colors"
export const hitSlopProp = {
  top: 12,
  right: 12,
  left: 12,
  bottom: 12,
};
export default StyleSheet.create({
  fontSize10: {
    fontSize: textScale(10),
    color: colors.black,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },
  fontSize11: {
    fontSize: textScale(11),
    color: colors.black,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },
  fontSize12: {
    fontSize: textScale(12),
    color: colors.black,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },
  fontSize13: {
    fontSize: textScale(13),
    color: colors.blackOpacity60,
    fontFamily: fontFamily.medium,
    textAlign: "left",
  },

  fontSize14: {
    fontSize: textScale(14),
    color: colors.black,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },

  fontSemiBold14: {
    fontSize: textScale(14),
    color: colors.black,
    fontFamily: fontFamily.semiBold,
    textAlign: "left",
  },

  fontSize15: {
    fontSize: textScale(15),
    color: colors.black,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },

  fontSize16: {
    fontSize: textScale(16),
    color: colors.black,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },
  fontSize18: {
    fontSize: textScale(18),
    color: colors.black,
    fontFamily: fontFamily.medium,
    textAlign: "left",
  },

  fontSize20: {
    fontSize: textScale(20),
    color: colors.lightDarkBlack,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },
  fontSize22: {
    fontSize: textScale(22),
    color: colors.black,
    fontFamily: fontFamily.medium,
    textAlign: "left",
  },
  fontSize24: {
    fontSize: textScale(24),
    color: colors.black,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },

  fontBold16: {
    fontSize: textScale(16),
    color: colors.black,
    fontFamily: fontFamily.bold,
    textAlign: "left",
  },

  fontSize26: {
    fontSize: textScale(26),
    color: colors.numberBlackblack,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },
  rowWithSpaceBetweenContent: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  rowWithSpaceEvenly: {
    flexDirection: "row",
    justifyContent: "space-evenly",
    alignItems: "center",
  },

  fontSize28: {
    fontSize: textScale(28),
    color: colors.black,
    fontFamily: fontFamily.medium,
    textAlign: "left",
  },

  fontSize13Purple: {
    fontSize: textScale(13),
    color: colors.purpleColor,
    fontFamily: fontFamily.regular,
    textAlign: "left",
  },

  fontSize16SemiBold: {
    fontSize: textScale(16),
    color: colors.black,
    fontFamily: fontFamily.semiBold,
    textAlign: "left",
  },

  loader: {
    position: "absolute",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    alignItems: "center",
    justifyContent: "center",
  },
  buttonRect: {
    height: moderateScaleVertical(46),
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: colors.themeColor,
    borderWidth: 1,
    borderColor: colors.themeColor,
    borderRadius: 4,
  },
  shadowStyle: {
    backgroundColor: colors.white,
    borderRadius: 4,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.4,
    shadowRadius: 2,
    elevation: 2,
    // borderColor: colors.lightWhiteGrayColor,
    // borderWidth: 0.7,
  },
  buttonTextWhite: {
    fontFamily: fontFamily.regular,
    textTransform: "uppercase",
    color: colors.white,
    textAlign: "center",
  },
  imgOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(0,0,0,0.3)",
  },
  imageSize24: {
    width: scale(24),
    height: scale(24),
    resizeMode: "contain",
  },
  imageSize16: {
    width: scale(16),
    height: scale(16),
    resizeMode: "contain",
  },
  buttonContainer: {
    minWidth: width / 2.3,
    height: verticalScale(48),
    borderRadius: 24,
    alignItems: "center",
    justifyContent: "center",
  },
  bottomButtonContainer: {
    flexDirection: "row",
    justifyContent: "flex-end",
    alignSelf: "center",
    paddingVertical: moderateScaleVertical(12),
    marginBottom: moderateScale(12)
    // backgroundColor: "red",
    // width: width
    // marginTop: moderateScaleVertical(8),
  },
  cancelButtonText: {
    color: colors.black,
    textTransform: "uppercase",
  },
  nextButtonText: {
    color: colors.white,
    textTransform: "uppercase",
  },
  inputTextStyle: {
    fontSize: textScale(14),
    fontFamily: fontFamily.regular,
    textAlign: "left",
    color: colors.black,
  },
  formIndexStyle: {
    fontSize: textScale(18),
    color: colors.black,
    fontFamily: fontFamily.medium,
    textAlign: "left",
    color: colors.green,
  },
  grayButtonContainer: {
    backgroundColor: colors.blackOpacity10,
    borderRadius: moderateScale(24),
    alignItems: "center",
    flex: 1,
  },
  greenButtonContainer: {
    backgroundColor: colors.themeColor,
    borderRadius: moderateScale(24),
    alignItems: "center",
    flex: 1,
  },
  whiteCenterModalStyle: {
    marginHorizontal: moderateScale(16),
    backgroundColor: colors.white,
    borderRadius: 24,
    paddingVertical: moderateScaleVertical(24),
    paddingHorizontal: moderateScale(16),
    shadowColor: "#000",
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  rightSeparatorWidth82: {
    height: 1,
    width: "82%",
    alignSelf: "flex-end",
    backgroundColor: colors.blackOpacity10,
  },

  graySeparatorLine: {
    height: 1,
    backgroundColor: colors.blackOpacity10,
  },
  iosShadowOpacity10: {
    shadowOffset: { width: 0, height: 10 },
    shadowColor: "rgb(0,0,0)",
    shadowOpacity: 0.1,
  },
  containerMarginB24: {
    marginBottom: moderateScaleVertical(24),
  },
  placeholderText: {
    color: colors.blackOpacity40,
    fontFamily: fontFamily.medium,
  },
});
