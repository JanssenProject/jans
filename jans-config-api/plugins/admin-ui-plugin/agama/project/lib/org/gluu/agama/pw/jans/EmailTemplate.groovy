package org.gluu.agama.pw.jans;

import java.time.*;
import java.time.format.DateTimeFormatter;


class EmailTemplate {
    
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, YYYY, HH:mma (O)");

    static String get(String otp, String line1, String line2, String line3, String line4) {     

        """

<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Email Verification test</title>
  <link href="https://fonts.googleapis.com/css?family=Nunito+Sans:400,700&display=swap" rel="stylesheet" />
  <style>
    body {
      margin: 0;
      padding: 0;
      background-color: #f5f5f5;
      font-family: 'Nunito Sans', sans-serif;
    }
    .email-container {
      max-width: 640px;
      margin: 0 auto;
      background-color: #ffffff;
      font-size: 18px;
      font-weight: 300;
      padding: 20px;
    }
    .logo {
      text-align: center;
      padding-bottom: 10px;
    }
    .logo img {
      max-height: 60px;
      width: auto;
    }
    .content {
      padding: 12px 0;
      border-top: 1px solid #ccc;
      border-bottom: 1px solid #ccc;
    }
    .otp-box {
      background-color: #f5f5f5; /* now matches outer background */
      color: #AD9269;
      font-size: 40px;
      font-weight: 700;
      letter-spacing: 6px;
      padding: 10px 24px;
      border-radius: 6px;
      display: inline-block;
      margin: 24px auto;
    }

    @media only screen and (max-width: 600px) {
      .email-container {
        padding: 16px;
        font-size: 16px;
      }
      .otp-box {
        font-size: 32px;
        padding: 10px 16px;
        letter-spacing: 4px;
      }
      .logo img {
        max-height: 48px;
      }
    }
  </style>
</head>
<body>

  <div style="padding: 40px 0; background-color: #f5f5f5;">
    <div class="email-container">
      
      <div class="logo">
        <img src="https://gluu.org/wp-content/uploads/elementor/thumbs/Logo-qbe8p4qgmufqni0becxda6fnfib6krzb65uihag270.png" alt="Gluu Inc." />
      </div>
      
      <div class="content">
        <p><strong>Hi,</strong><br>
        ${line1}</p>
        <div style="text-align: center;">
          <div class="otp-box">${otp}</div>
        </div>

        <p>${line2}</p>
        <p>${line3}<br>${line4}</p>
      </div>

    </div>
  </div>

</body>
</html>


        """
    }

    private static String computeDateTime(String zone) {

        Instant now = Instant.now();
        try {
            return now.atZone(ZoneId.of(zone)).format(formatter);
        } catch (Exception e) {
            return now.atOffset(ZoneOffset.UTC).format(formatter);
        }
        
    }
    
}
