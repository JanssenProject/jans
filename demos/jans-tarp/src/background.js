'use strict';
const fetch = require('node-fetch');

chrome.runtime.onMessage.addListener(function (request, sender, sendResponse) {
   if (request.type == "register_click_event") {
      console.log("click event captured in current webpage");
      // Call the callback passed to chrome.action.onClicked
   }
});
