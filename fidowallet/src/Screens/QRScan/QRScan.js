import { Text, View, TouchableOpacity, Image } from 'react-native'
import React, { Component } from 'react'
import WrapperContainer from '../../Components/WrapperContainer'
import styles from "./styles"
import imagePath from '../../constants/imagePath'
import colors from '../../styles/colors'
import QRCodeScanner from 'react-native-qrcode-scanner'
import { height } from '../../styles/responsiveDesign'

export class QRScan extends Component {

  onSuccess = e => {
    console.log(e)
    // Linking.openURL(e.data).catch(err =>
    //   console.error('An error occured', err)
    // );
  };

  render() {
    return (
      <WrapperContainer statusBarColor={colors.themeColor}>
                <View style={styles.header}>
          <TouchableOpacity
            onPress={() => this.props.navigation.goBack()}
            style={{alignItems: 'center', justifyContent: 'center'}}>
            <Image source={imagePath.leftIconWhite} style={styles.backIcon} />
          </TouchableOpacity>
          <Text style={styles.headingText}>Keys</Text>
          <View style={{width: 25, height: 25}} />
        </View>
        <QRCodeScanner
        onRead={this.onSuccess}
        cameraStyle = {{height: height}}
        showMarker={true}
        customMarker={
          <View style={styles.rectangleContainer}>
            {/* <View style={{ flex: 1, backgroundColor: 'pink' }} ><Text style={{ fontSize: 32 }}>1</Text></View>
            <View style={{ flex: 1, backgroundColor: 'blue' }} ><Text style={{ fontSize: 32 }}>2</Text></View>
            <View style={{ flex: 1, backgroundColor: 'yellow' }} ><Text style={{ fontSize: 32 }}>3</Text></View> */}
          </View>
        }
        // markerStyle = {"dashed"}
        // topContent={
        //   <Text style={styles.centerText}>
        //     Go to{' '}
        //     <Text style={styles.textBold}>wikipedia.org/wiki/QR_code</Text> on
        //     your computer and scan the QR code.
        //   </Text>
        // }
        // bottomContent={
        //   <TouchableOpacity style={styles.buttonTouchable}>
        //     <Text style={styles.buttonText}>OK. Got it!</Text>
        //   </TouchableOpacity>
        // }
      />
      </WrapperContainer>
    )
  }
}

export default QRScan