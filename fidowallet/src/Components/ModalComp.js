import React, { Component } from "react";
import { Modal, View, Pressable, Text, StyleSheet } from "react-native";
import colors from "../styles/colors";

const styles = StyleSheet.create({
  centeredView: {
    flex: 1,
    justifyContent: "center",
    backgroundColor: colors.blackOpacity40,
  },
});

const ModalComp = ({ visibility, closeModal = () => {}, children }) => {
  return (
      <Modal
        animationType="slide"
        transparent={true}
        visible={visibility}
        onRequestClose={closeModal}
      >
        <View style={styles.centeredView}>{children}</View>
      </Modal>
  )
}

export default ModalComp
