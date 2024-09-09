import React, { useState, useEffect } from 'react'
import './options.css'
import Header from './header'
import OIDCClients from './oidcClients'
import UserDetails from './userDetails'

const Options = () => {

  const [optionType, setOptionType] = useState("");
  const [data, setdata] = useState({});
  const [dataChanged, setDataChanged] = useState(false);

  useEffect(() => {
    chrome.storage.local.get(["oidcClients"], (oidcClientResults) => {

      if (!isEmpty(oidcClientResults) && Object.keys(oidcClientResults).length !== 0) {

        chrome.storage.local.get(["loginDetails"], (loginDetailsResult) => {
          if (!isEmpty(loginDetailsResult) && Object.keys(loginDetailsResult).length !== 0) {
            setOptionType('loginPage');
            setdata(loginDetailsResult);
          } else {
            setOptionType('oidcClientPage');
            setdata(oidcClientResults);
          }
        });
      } else {
        setOptionType('oidcClientPage');
        setdata({});
      }
      setDataChanged(false);
    })
  }, [dataChanged]);

  function isEmpty(value) {
    return (value == null || value.length === 0);
  }

  function handleDataChange() {
    setDataChanged(true);
  }

  function renderPage({ optionType, data }) {
    switch (optionType) {
      case 'oidcClientPage':
        return <OIDCClients
          data={data.oidcClients}
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
