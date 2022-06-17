import React, { useState, useEffect } from "react";
import { SafeAreaView, Text, View, TouchableOpacity } from "react-native";
import {
	CodeField,
	Cursor,
	useBlurOnFulfill,
	useClearByFocusCell,
} from "react-native-confirmation-code-field";
import colors from "../../styles/colors";
import commonStyles from "../../styles/commonStyles";
import fontFamily from "../../styles/fontFamily";
import { moderateScale } from "../../styles/responsiveDesign";
import styles from "./styles";

const CELL_COUNT = 4;
const RESEND_OTP_TIME_LIMIT = 90;

const VerifyCode = ({ value, setValue }) => {
	const [resendButtonDisabledTime, setResendButtonDisabledTime] = useState(
		RESEND_OTP_TIME_LIMIT
	);

	// const [value, setValue] = useState('');
	const ref = useBlurOnFulfill({ value, cellCount: CELL_COUNT });
	const [props, getCellOnLayoutHandler] = useClearByFocusCell({
		value,
		setValue,
	});

	return (
		<SafeAreaView style={{ flex: 1 }}>
			<View
				style={{
					backgroundColor: colors.themeColor,
					paddingVertical: 16,
					borderTopLeftRadius: 16,
					borderTopRightRadius: 16,
				}}
			>
				<Text
					style={{
						...commonStyles.fontSize20,
						fontFamily: fontFamily.medium,
						color: "#fff",
						textAlign: "center",
					}}
				>
					Set Passcode
				</Text>
			</View>
			<View style={styles.root}>
				<Text
					style={{
						...commonStyles.fontSize16,
						fontFamily: fontFamily.semiBold,
						textAlign: "center",
					}}
				>
					Enter a passcode
				</Text>
				<CodeField
					ref={ref}
					{...props}
					value={value}
					onChangeText={setValue}
					cellCount={CELL_COUNT}
					rootStyle={styles.codeFieldRoot}
					keyboardType="number-pad"
					textContentType="oneTimeCode"
					renderCell={({ index, symbol, isFocused }) => (
						<View
							onLayout={getCellOnLayoutHandler(index)}
							key={index}
							style={[styles.cellRoot, isFocused && styles.focusCell]}
						>
							<Text style={styles.cellText}>
								{symbol ? (
									<View
										style={{
											borderRadius: 25,
											backgroundColor: "#000",
										}}
									/>
								) : isFocused ? (
									<View
                                     style = {{
                                        width: moderateScale(30),
                                        height: 30,
                                        justifyContent: "center",
                                        alignItems: "center"
                                        // marginTop: 10
                                     }}
                                    >
									<View
										style={{
											backgroundColor: "#000",
											borderRadius: 100,
											width: 18,
											height: 5,
										}}
									/>
                                    </View>
								) : (
                                    <View
                                     style = {{
                                        width: moderateScale(30),
                                        height: 30,
                                        justifyContent: "center",
                                        alignItems: "center"
                                        // marginTop: 10
                                     }}
                                    >
									<View
										style={{
											backgroundColor: "#000",
											borderRadius: 100,
											width: 18,
											height: 5,
										}}
									/>
                                    </View>
								)}
							</Text>
						</View>
					)}
				/>
			</View>
		</SafeAreaView>
	);
};

export default VerifyCode;
