import { Text, View, TouchableOpacity, Image, FlatList } from 'react-native'
import React, { Component } from 'react'
import WrapperContainer from '../../Components/WrapperContainer'
import colors from '../../styles/colors'
import imagePath from '../../constants/imagePath'
import styles from './styles'
import { width } from '../../styles/responsiveDesign'
import moment from 'moment'
import navigationStrings from "../../constants/navigationStrings"

export class Logs extends Component {

  state = {
  }

  render() {
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
            Key Details
          </Text>
            <View style={{width: 25, height: 25}}></View>
        </View>
        <View style = {{flex: 1, backgroundColor: "rgb(243,243,243)", paddingTop: 10}}>
          <View style = {{backgroundColor: colors.white}}>
          <View style = {styles.mainImageContainer}>
            <Image source={imagePath.keyRowImage} style = {styles.mainImage} />
          </View>
          <View style = {styles.section}>
            <Text style = {styles.rightTestOne} >Username</Text>
            <Text style = {styles.rightTestTwo} >harsukhbir</Text>
          </View>
          <View style = {styles.section}>
            <Text style = {styles.rightTestOne} >Created</Text>
            <Text style = {styles.rightTestTwo} >Mar 23, 2022 06:48:46</Text>
          </View>
          <View style = {styles.section}>
            <Text style = {styles.rightTestOne} >Id Provider</Text>
            <Text style = {styles.rightTestTwo} >test-casa.gluu.org</Text>
          </View>
          <View style = {styles.section}>
            <Text style = {styles.rightTestOne} >Key Handle</Text>
            <Text style = {styles.rightTestTwo} >789456123</Text>

          </View>
          </View>
        </View>
        
      </WrapperContainer>
    )
  }
}

export default Logs