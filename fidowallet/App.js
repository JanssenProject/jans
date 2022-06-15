import React, { Component } from "react";
import FlashMessage from "react-native-flash-message";
import Routes from "./src/Navigation/Routes";
import { LogBox } from "react-native";

LogBox.ignoreAllLogs();
export default class App extends Component {

	render() {
		return (
			<React.Fragment>
				<Routes />
				<FlashMessage position="top" />
			</React.Fragment>
		);
	}
}
