import React, { useState, useEffect } from 'react';
import Header from './header';
import HomePage from './homePage';
import Utils from './Utils';

const Options = () => {

  const [optionType, setOptionType] = useState("");
  const [data, setdata] = useState({});
  const [dataChanged, setDataChanged] = useState(false);

  useEffect(() => {
    chrome.storage.local.get(["cedarlingConfig"], (cedarlingConfigResult: Record<string, unknown>) => {
      let cedarlingConfig = Utils.isEmpty(cedarlingConfigResult) ? {} : cedarlingConfigResult;

      chrome.storage.local.get(["oidcClients"], (oidcClientResults: Record<string, unknown>) => {
        if (!Utils.isEmpty(oidcClientResults) && Object.keys(oidcClientResults).length !== 0) {
          chrome.storage.local.get(["loginDetails"], (loginDetailsResult: Record<string, unknown>) => {
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

  function renderPage({ optionType, data }: { optionType: string; data: Record<string, unknown> }) {
    switch (optionType) {
      case 'homePage':
      case 'loginPage':
        return <HomePage
          data={data}
          notifyOnDataChange={handleDataChange}
        />;
      default:
        return null;
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      {renderPage({ optionType, data })}
    </div>
  );
};

export default Options;
