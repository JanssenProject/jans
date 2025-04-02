import React, { useState, useEffect } from 'react'
import './options.css'
import Header from './header'
import HomePage from './homePage'
import UserDetails from './userDetails'
import { ILooseObject } from './ILooseObject'
import Utils from './Utils';
const Options = () => {

  const [optionType, setOptionType] = useState("");
  const [data, setdata] = useState({});
  const [dataChanged, setDataChanged] = useState(false);

  useEffect(() => {
    // Fetch cedarlingConfig first
    chrome.storage.local.get(["cedarlingConfig"], (cedarlingConfigResult) => {
      let cedarlingConfig = Utils.isEmpty(cedarlingConfigResult) ? {} : cedarlingConfigResult;

      chrome.storage.local.get(["oidcClients"], (oidcClientResults) => {
        if (!Utils.isEmpty(oidcClientResults) && Object.keys(oidcClientResults).length !== 0) {
          chrome.storage.local.get(["loginDetails"], (loginDetailsResult) => {
            if (!Utils.isEmpty(loginDetailsResult) && Object.keys(loginDetailsResult).length !== 0) {
              setOptionType("loginPage");
              setdata({ ...loginDetailsResult, ...cedarlingConfig });
            } else {
              setOptionType("homePage");
              setdata({ ...oidcClientResults, ...cedarlingConfig });
            }
          });
        } else {
          setOptionType("homePage");
          setdata({ ...cedarlingConfig });
        }
        setDataChanged(false);
      });
    });
  }, [dataChanged]);


  function handleDataChange() {
    setDataChanged(true);
  }

  function renderPage({ optionType, data }) {
    switch (optionType) {
      case 'homePage':
        return <HomePage
          data={data}
          notifyOnDataChange={handleDataChange}
        />
      case 'loginPage':
        return <UserDetails
          data={data.loginDetails}
          notifyOnDataChange={handleDataChange}
        />
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
