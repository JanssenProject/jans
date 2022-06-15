import { Text, View, TouchableOpacity, Image,Modal } from 'react-native'
import React, { Component } from 'react'
import WrapperContainer from '../../Components/WrapperContainer'
import colors from '../../styles/colors'
import imagePath from '../../constants/imagePath'
import styles from './styles'
import commonStyles from '../../styles/commonStyles'
import { width } from '../../styles/responsiveDesign'
import fontFamily from '../../styles/fontFamily'

export class Passcode extends Component {

  state = {
    ssl: false,
    showModal: false
  }

  togglePass = () => {
    let {showModal, ssl} = this.state
    if (!ssl && !showModal) {
      this.setState({showModal: true})
    } else if (!ssl && showModal) {
      this.setState({showModal: false, ssl: true})
    } else {
      this.setState({ssl: false})
    }
    this.setState({pass: !this.state.pass})
  }

  closeModal = () => {
    this.setState({showModal: false})
  }

  render() {
    let {ssl, showModal} = this.state
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
            Trust all SSL
          </Text>
            <View style={{width: 25, height: 25}} />
        </View>
        <View style = {{paddingTop: 25}}>
          <TouchableOpacity style={styles.passCodeRow} onPress = {this.togglePass}>
            <View style={{flexDirection: "row", alignItems: "center" }}>
              <Image source={imagePath.passCodeIcon} style={styles.menuIcon} />
              <Text style={{...commonStyles.fontSize15, color: colors.blackOpacity80}}>
              Trust all SSL
              </Text>
            </View>
            <Image source={!ssl ? imagePath.toggleOn : imagePath.toggleOff} style = {styles.toggleImg} />
          </TouchableOpacity>
          <Text style={styles.passcodeText}>
            When enabled,Super Gluu will trust servers with self-signed certificates. Recommended for development only.
          </Text>
        </View>
        <Modal
        animationType='slide'
        visible={showModal}
        transparent={true}
        >
          <View style = {{flex: 1, backgroundColor: colors.blackOpacity70, justifyContent: "center", alignItems: "center"}}>
            <View>
              <View style = {{height: 27}} />
              <View style = {{backgroundColor: colors.white, width: width *2/3, paddingTop: 30, paddingBottom: 10, borderRadius: 5}}>
                <Text style = {{...commonStyles.fontSize15, color: colors.themeColor, fontFamily: fontFamily.semiBold, textAlign: "center"}}>
                  Admin Testing Only
                </Text>
                <Text style = {{...commonStyles.fontSize13, textAlign: "center", marginTop: 10}}>
                  This feature is just for admin testing purposes. Are an admin needing enable it?
                </Text>
                <View style = {{flexDirection: "row", justifyContent: "space-evenly", marginTop: 10}}>
                  <View style = {{flex: 0.45, backgroundColor: colors.themeColor, borderRadius: 5}} >
                    <TouchableOpacity style = {{paddingVertical: 10, borderRadius: 5}}
                    onPress = {this.togglePass}
                    >
                      <Text style = {{ ...commonStyles.fontSize15 ,textAlign: "center", color: colors.white}}>Yes</Text>
                    </TouchableOpacity>
                  </View>
                  <View style = {{flex: 0.45, backgroundColor: colors.themeColor, borderRadius: 5}} >
                    <TouchableOpacity style = {{paddingVertical: 10, borderRadius: 5}}
                    onPress = {this.closeModal}
                    >
                      <Text style = {{ ...commonStyles.fontSize15 ,textAlign: "center", color: colors.white}}>No</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              </View>
              <View style = {{height: 54,width: width *2/3, flex: 1,alignItems: "center", justifyContent: "center", position: "absolute", top: 0}}>
                <View style={{backgroundColor: colors.white, width: 54, height: 54, borderRadius: 27, alignItems: "center", justifyContent: "center"}}>
                <Image source={imagePath.appIcon} style = {{width: 50, height: 50}} />
                </View>
              </View>
            </View>
          </View>  
        </Modal>
      </WrapperContainer>
    )
  }
}

export default Passcode