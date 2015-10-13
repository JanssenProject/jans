Twilio SMS Authentication Script

This is a two step authentication workflow. The first step is standard username password authentication
against the local Gluu Server LDAP. The second step requires the person to enter a code that is sent via 
SMS to the person's mobile number. 

This script uses the Twilio service to send the message. You'll need to sign-up with Twilio to 
get an account to acquire credentials to call the API. You'll also need to download the
latest Twilio Java helper jar file with dependencies http://search.maven.org/#browse%7C-1416163511 
and install it in /opt/tomcat/endorsed folder.

There are three required custom properties:
    twilio_sid     Your account id at Twilio
    twilio_token   The API secret provided by Twilio
    from_number    The number you are using E.164 number formatting, see
                   https://www.twilio.com/help/faq/phone-numbers/how-do-i-format-phone-numbers-to-work-internationally 

