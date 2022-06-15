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
            Logs Details
          </Text>
            <TouchableOpacity style={{width: 25, height: 25}}>
            <Image
            source={imagePath.deleteIcon}
            style={styles.deleteIcon}
          />
            </TouchableOpacity>
        </View>
        <View style = {{flex: 1, backgroundColor: "rgb(243,243,243)", paddingTop: 10}}>
          <View style = {{backgroundColor: colors.white}}>
          <View style = {styles.mainImageContainer}>
            <Image source={imagePath.logRowImage} style = {styles.mainImage} />
          </View>
          <Text style = {styles.mainText}>
            Gluu Server test-casa.gluu.org
          </Text>
          <Text style = {styles.secondText}>
            https://test-casa.gluu.org
          </Text>
          <View style = {styles.section}>
            <Image source={imagePath.userIcon} style={styles.rowIcon} />
            <View style = {{alignItems: "flex-end"}}>
            <Text style = {styles.rightTestOne} >harsukhbir</Text>
            </View>
          </View>
          <View style = {styles.section}>
            <Image source={imagePath.locationIcon} style={styles.rowIcon} />
            <View style = {{alignItems: "flex-end"}}>
            <Text style = {styles.rightTestOne} >202.164.36.11</Text>
            <Text style = {styles.rightTestTwo} >India, Chandigarh, Chandigarh</Text>
            </View>
          </View>
          <View style = {styles.section}>
            <Image source={imagePath.clockIcon} style={styles.rowIcon} />
            <View style = {{alignItems: "flex-end"}}>
            <Text style = {styles.rightTestOne} >12.18.55</Text>
            <Text style = {styles.rightTestTwo} >Mar 23, 2022</Text>
            </View>
          </View>
          <View style = {styles.section}>
            <Image source={imagePath.linkIcon} style={styles.rowIcon} />
            <View style = {{alignItems: "flex-end"}}>
            <Text style = {styles.rightTestOne} >Enroll</Text>
            </View>
          </View>
          </View>
        </View>
        
      </WrapperContainer>
    )
  }
}

export default Logs