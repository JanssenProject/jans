import {Text, View, Image, TouchableOpacity} from 'react-native';
import styles from './styles';
import React, {Component} from 'react';
import WrapperContainer from '../../Components/WrapperContainer';
import imagePath from '../../constants/imagePath';
import ModalComp from '../../Components/ModalComp';
import colors from '../../styles/colors';
import { CodeField } from 'react-native-confirmation-code-field';
import ModalView from "./ModalComp"
import navigationStrings from '../../constants/navigationStrings';
const CELL_COUNT = 6;
const RESEND_OTP_TIME_LIMIT = 90;

export class LandingPage extends Component {

  state = {
    showPassModal: false,
     statusbarColor: "#fff",
     value: "",

  }

  toggleModal = (show, statusColor) => () => {
    this.setState({showPassModal: show, statusbarColor: statusColor})
  }

  render() {
    let {showPassModal, statusbarColor, value} = this.state
    return (
      <WrapperContainer bgColor="#f8f8f8" statusBarColor={statusbarColor}>
        <View style={[styles.flex50, styles.alignCenter]}>
          <Image source={imagePath.appIcon} style={styles.appIcon} />
        </View>
        <View style={[styles.flex50]}>
          <TouchableOpacity style={{flex: 0.8, justifyContent: 'space-evenly'}} onPress = {this.toggleModal(true, colors.blackOpacity40)}>
            <Text style={styles.centerText}>Add Secure Entry</Text>
            <View style={styles.flexDirectionRow}>
              <View style={styles.passCodeButotn}>
                <Image
                  source={imagePath.passCodeIcon}
                  style={styles.passIcon}
                />
                <Text style={styles.passcodeText}>Passcode</Text>
              </View>
              <Image />
            </View>
          </TouchableOpacity>
        </View>
        <ModalComp visibility={showPassModal}>
          <View style = {{flex: 1, justifyContent: "flex-end"}}>
            <View style = {{flex: 0.9}}>
            <ModalView 
            value={value}
            setValue = {(val) => {
              this.setState({value: val}, () => {
                if (this.state.value.length == 4) {
                  setTimeout(() => {
                    this.setState({showPassModal: false}, () => {
                      console.log(this.props)
                      this.props.navigation.navigate(navigationStrings.DRAWER_STACK)
                    })                    
                  }, 1000);
                }
              })
            }}
            />
            </View>
          </View>
        </ModalComp>
      </WrapperContainer>
    );
  }
}

export default LandingPage;
