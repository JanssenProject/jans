import React from "react";
import './popup.css'

const Popup = () => {
    chrome.tabs.create({url: 'options.html', active: true});
    return (
        <div>
            <div className="logo"></div>
        </div>
    )
};

export default Popup;