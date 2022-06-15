import {StyleSheet} from "react-native"
import { moderateScale, moderateScaleVertical, width } from "../../styles/responsiveDesign"
import commonStyles from "../../styles/commonStyles"
import fontFamily from "../../styles/fontFamily"
export default StyleSheet.create({
    container: {
        backgroundColor: "red"
    },
    flex50: {
        flex: 0.5,
    },
    alignCenter: {
        alignItems: "center",
        justifyContent: "center"
    },
    appIcon: {
        width: moderateScale(180),
        height: moderateScale(180)
    },
    passIcon: {
        width: moderateScale(30),
        height: moderateScale(30),
        marginRight: 10
    },
    flexDirectionRow: {
        flexDirection: "row",
        justifyContent: "space-between"
    },
    centerText: {
        ...commonStyles.fontSize20,
        textAlign: "center",
        color: "#484848",
        fontFamily: fontFamily.semiBold
    },
    passCodeButotn:
        {flexDirection: "row", alignItems: "center", backgroundColor: "#fff", flex: 1, paddingHorizontal: 10, paddingVertical: 6}
    ,
    passcodeText: {
        ...commonStyles.fontSize16,
        textAlign: "center",
        color: "#484848",
        fontFamily: fontFamily.semiBold
    },



    root: {
        flex: 1,
        padding: 20,
        alignContent: 'center',
        justifyContent: 'center',
        backgroundColor: "#e1e1e1"
    },
    title: {
        textAlign: 'left',
        fontSize: 20,
        marginStart: 20,
        fontWeight:'bold'
    },
    subTitle: {
        textAlign: 'left',
        fontSize: 16,
        marginStart: 20,
        marginTop: 10
    },
    codeFieldRoot: {
        marginTop: moderateScaleVertical(15),
        width: moderateScale(150),
        // marginLeft: "25%",
        alignSelf: "center",
    },
    cellRoot: {
        width: moderateScale(30),
        height: 30,
        marginHorizontal: "2%",
        justifyContent: 'center',
        alignItems: 'center',
        // borderBottomColor: '#060606',
        // borderBottomWidth: 3,
     },
     cellText: {
        color: '#000',
        fontSize: 20,
        textAlign: 'center',
    },
    focusCell: {
        borderBottomColor: '#007AFF',
        borderBottomWidth: 2,
    },
    
    button: {
        marginTop: 20
    },
    resendCode: {
        color: '#007AFF',
        marginStart: 20,
        marginTop: 40,
    },
    resendCodeText: {
        marginStart: 20,
        marginTop: 40,
    },
    resendCodeContainer: {
        flexDirection: 'row',
        alignItems: 'center'
    }
})