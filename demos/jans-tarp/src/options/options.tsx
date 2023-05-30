import React, { useState, useEffect } from 'react'
import './options.css'
import Header from './header'
import RegisterForm from './registerForm'
import UserDetails from './userDetails'
import OIDCClientDetails from './oidcClientDetails'

const Options = () => {

  const [optionType, setOptionType] = useState("");
  const [data, setdata] = useState({});

  chrome.storage.local.get(["oidcClient"], (oidcClientResult) => {
    if (!isEmpty(oidcClientResult) && Object.keys(oidcClientResult).length !== 0) {

      chrome.storage.local.get(["loginDetails"], (loginDetailsResult) => {
        if (!isEmpty(loginDetailsResult) && Object.keys(loginDetailsResult).length !== 0) {
          setOptionType('loginPage');
          setdata(loginDetailsResult);
        } else {
          setOptionType('oidcClientPage');
          setdata(oidcClientResult);
        }
      });
    } else {
      setOptionType('clientRegistrationPage');
      setdata({});
    }
  });

  function isEmpty(value) {
    return (value == null || value.length === 0);
  }

  function renderPage({ optionType, data }) {
    switch (optionType) {
      case 'clientRegistrationPage':
        return <RegisterForm data={data}/>
      case 'oidcClientPage':
        return <OIDCClientDetails data={data.oidcClient} />
      case 'loginPage':
        return <UserDetails data={data.loginDetails} />
      default:
        return null
    }
  }

  return (
    <div className="container">
      <Header />
      {renderPage({ optionType, data })}
    </div>
  )
};

export default Options;
