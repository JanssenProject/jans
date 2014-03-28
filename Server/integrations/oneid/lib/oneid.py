#!/usr/bin/python

# OneID Python API Library
# Copyright 2013 by OneID

import urllib
import urllib2
import datetime
import base64
try:
    import json
except ImportError:
    import simplejson as json

#import requests
import StringIO
import os
import random



class OneID:

    def __init__(self, server_flag=""):
        """server_flag should be (for example) "-test" when using a non-production server"""
        self.helper_server = "https://keychain%s.oneid.com" % server_flag
        self.script_header = '<script src="https://api%s.oneid.com/js/includeexternal.js" type="text/javascript"></script>' % server_flag
        self.oneid_form_script = '<script src="https://api%s.oneid.com/form/form.js" type="text/javascript"></script>' % server_flag
        self.creds_file = "api_key"+server_flag+".json"
        random.seed()


    def _call_helper(self, method, data={}):
        """Call the OneID Helper Service. """
        url = "%s/%s" % (self.helper_server, method)
        r = requests.post(url, json.dumps(data), auth=(self.api_id, self.api_key))
        return r.json

    def set_credentials(self, api_id="", api_key=""):
        """Set the credentials used for access to the OneID Helper Service"""
        if api_id != "":
            self.api_id = api_id
            self.api_key = api_key
        else:
            f = open(self.creds_file,'r')
            creds = json.loads(f.read())
            f.close()
            self.api_id = creds["API_ID"]
            self.api_key = creds["API_KEY"]

    def validate(self,line):
        """Validate the data received by a callback"""
        resp = json.loads(line)
        valdata = dict([("nonces",resp["nonces"]),("uid",resp["uid"])])
        if "attr_claim_tokens" in resp:
            valdata["attr_claim_tokens"] = resp["attr_claim_tokens"]
        valresp = self._call_helper("validate",valdata)
        if (not self.success(valresp)):
            valresp["failed"] = "failed"
            return valresp

        for x in valresp:
            resp[x] = valresp[x]

        return resp

    def draw_signin_button(self, callback_url, attrs="", http_post=False):
        """Create a OneID Sign In button on the web page"""
        challenge = {"attr" : attrs,
                     'auth_level' : "OOB",
                     "callback" : callback_url}
        if http_post:
            challenge["request_method"] = "HTTP_POST"

        params = json.dumps({"challenge" : challenge })

        js = "<span class='oneid_login_ctr'></span>"
        js+= "<script type='text/javascript'>"
        js+= "OneIdExtern.registerApiReadyFunction(function(){"
        js+= "OneId.loginButton('.oneid_login_ctr'," + params +")"
        js+= "})"
        js+="</script>"

        return js

    def draw_quickfill_button(self, attrs):
        """Create a OneID QuickFill button on the web page"""
        js = "<span class='oneid_quickfill_ctr'></span>"
        js+= "<script type='text/javascript'>"
        js+= "OneIdExtern.registerApiReadyFunction(function(){"
        js+= "OneId.accuFillButton('.oneid_quickfill_ctr'," + attrs +")"
        js+= "})"
        js+="</script>"

        return js

    def draw_provision_button(self, attrs):
        """Create a provision button on the web page"""
        js = "<div class='oneid_create_ctr'></div>"
        js+= "<script type='text/javascript'>"
        js+= "OneIdExtern.registerApiReadyFunction(function(){"
        js+= "OneId.createOneIdButton('.oneid_create_ctr'," + json.dumps(attrs) +")"
        js+= "})"
        js+="</script>"

        return js

    def redirect(self, page, response, sessionid):
        """Create the JSON string that instructs the AJAX code to redirect the browser to the account"""
        if self.success(response):
            suffix = "?sessionid="+sessionid
        else:
            suffix = ""

        return json.dumps({"error":response['error'],"errorcode":str(response['errorcode']),\
                           "url":page + suffix})
        
    def success(self, response):
        """Check errorcode in a response"""
        return response["errorcode"] == 0

    def save_session(self, response):
        """Save attributes and UID in a temporary file for account page"""
        sessionid = str(random.getrandbits(128))
        sessionfile = "/tmp/"+sessionid+".OneID"
        f = open(sessionfile, "w")
        f.write(json.dumps({"uid":response["uid"], "attr":response["attr"]}))
        f.close()
        return sessionid;

    def get_session(self, sessionid):
        """Retrieve attributes and session ID saved by validation page"""
        sessionfile = "/tmp/"+sessionid+".OneID"
        f = open(sessionfile, "r")
        data = f.read()
        f.close()
        os.remove(sessionfile)
        return json.loads(data)

    def _getnonce(self, response):
        """Extract base64-encoded nonce from JWT in a response"""
        return response["nonces"]["repo"]["nonce"].split('.')[1]

    
        
