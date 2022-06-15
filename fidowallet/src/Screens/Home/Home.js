import { Text, View, Image, TouchableOpacity } from "react-native";
import React, { Component } from "react";
import WrapperContainer from "../../Components/WrapperContainer";
import imagePath from "../../constants/imagePath";
import navigationStrings from "../../constants/navigationStrings";
import { width } from "../../styles/responsiveDesign";
import commonStyles from "../../styles/commonStyles";
import fontFamily from "../../styles/fontFamily";
import colors from "../../styles/colors";

export class Home extends Component {

  navigateToPage = (page) => () => {
    this.props.navigation.navigate(page)
  }

	render() {
		return (
			<WrapperContainer statusBarColor={colors.themeColor}>
				<View
					style={{
						backgroundColor: colors.themeColor,
						flexDirection: "row",
						justifyContent: "space-between",
						padding: 10,
					}}
				>
					<TouchableOpacity
						onPress={() => this.props.navigation.openDrawer()}
						style={{ alignItems: "center", justifyContent: "center" }}
					>
						<Image
							source={imagePath.homeScreenIcon}
							style={{ width: 15, height: 15 }}
						/>
					</TouchableOpacity>

					<Image
						source={imagePath.appIconWhite}
						style={{ width: 20, height: 20 }}
					/>
					<View style={{ width: 25, height: 25 }} />
				</View>
				<View
					style={{ flex: 0.8, alignItems: "center", justifyContent: "center" }}
				>
					<Image
						source={imagePath.homeScreenScanImage}
						style={{ width: width / 3, height: width / 3, marginBottom: 15 }}
					/>
					<Text
						style={{
							...commonStyles.fontSize24,
							color: "#656565",
							fontFamily: fontFamily.semiBold,
						}}
					>
						Welcome to Super Gluu!
					</Text>
					<Text
						style={{
							...commonStyles.fontSize16,
							fontFamily: fontFamily.regular,
							marginHorizontal: 25,
							textAlign: "center",
							marginTop: 5,
							color: "#656565",
						}}
					>
						You may be presented with a QR code for registration or
						authentication. Tap the button to open your QR scanner
					</Text>
					<View style={{ alignItems: "center", marginTop: 15 }}>
						<TouchableOpacity
							style={{
								backgroundColor: colors.themeColor,
								paddingHorizontal: 25,
								paddingVertical: 10,
								borderRadius: 100,
							}}
              onPress = {this.navigateToPage(navigationStrings.QR_SCAN)}
						>
							<Text style={{ ...commonStyles.fontSize20, color: "#fff" }}>
								Scan QR Code
							</Text>
						</TouchableOpacity>
					</View>
				</View>
			</WrapperContainer>
		);
	}
}

export default Home;
