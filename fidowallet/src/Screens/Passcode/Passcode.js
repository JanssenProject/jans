import { Text, View, TouchableOpacity, Image, } from 'react-native'
import React, { Component } from 'react'
import WrapperContainer from '../../Components/WrapperContainer'
import colors from '../../styles/colors'
import imagePath from '../../constants/imagePath'
import styles from './styles'
import commonStyles from '../../styles/commonStyles'

export class Passcode extends Component {

  state = {
    pass: true
  }

  togglePass = () => {
    this.setState({pass: !this.state.pass})
  }

  render() {
    let {pass} = this.state
    return (
      <WrapperContainer statusBarColor={colors.themeColor} bgColor = "#f3f3f3">
        <View style={styles.header}>
          <TouchableOpacity
          onPress={() => this.props.navigation.goBack()}
          style = {{alignItems: "center", justifyContent: "center"}}
          >
          <Image
            source={imagePath.leftIconWhite}
            style={styles.backIcon}
          />
          </TouchableOpacity>
          <Text style = {styles.headingText}>
            Passcode
          </Text>
            <View style={{width: 25, height: 25}} />
        </View>
        <View style = {{paddingTop: 25}}>
          <TouchableOpacity style={styles.passCodeRow} onPress = {this.togglePass}>
            <View style={{flexDirection: "row", alignItems: "center" }}>
              <Image source={imagePath.passCodeIcon} style={styles.menuIcon} />
              <Text style={{...commonStyles.fontSize15, color: colors.blackOpacity80}}>
                Passcode
              </Text>
            </View>
            <Image source={pass ? imagePath.toggleOn : imagePath.toggleOff} style = {styles.toggleImg} />
          </TouchableOpacity>
          <Text style={styles.passcodeText}>
            When enabled, access to Super Gluu will be protected by a passcode of your choice
          </Text>
        </View>

        <View style = {{paddingTop: 20}}>
          <View style={styles.passCodeRow}>
              <Text style={{...commonStyles.fontSize15, color: colors.blackOpacity60}}>
                Change Passcode
              </Text>
            <Image source={imagePath.rightIconGrey} style = {styles.rightImgArrow} />
          </View>
          <Text style={styles.passcodeText}>
            Super gluu will be locked for 10 minutes after 5 failed attempts
          </Text>
        </View>
      </WrapperContainer>
    )
  }
}

export default Passcode