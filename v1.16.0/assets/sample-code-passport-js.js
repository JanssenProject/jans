// Make .env and add below environments
/**
JANS_SERVER_URL=https://your.jans.org
JANS_POST_URL=https://your.jans.org/jans-auth/postlogin.htm
GOOGLE_CLIENT_ID=xxxxxxx
GOOGLE_CLIENT_SECRET=xxxxxxx
PORT=8090
SALT_FILE_PATH=/etc/jans/conf/salt
KEY_FILE_PATH=/etc/passport/keys/keystore.pem
KEY_ALG=RS512
KEY_ID=your-kid
*/

// Installation and start node server
// npm init -y
// npm i cookie-session crypto dotenv express jsonwebtoken passport passport-google-oauth20 uuid
// node index.js

require('dotenv').config()
const express = require('express')
const cookie = require('cookie-session')
const passport = require('passport')
const GoogleStrategy = require('passport-google-oauth20').Strategy;
const { v4: uuidv4 } = require('uuid')
const jwt = require('jsonwebtoken')
const fs = require('fs')
const crypto = require('crypto')

const port = process.env.PORT
const jansPostUrl = process.env.JANS_POST_URL

passport.serializeUser((user, done) => done(null, user))
passport.deserializeUser((user, done) => done(null, user))

passport.use(new GoogleStrategy({
  clientID: process.env.GOOGLE_CLIENT_ID,
  clientSecret: process.env.GOOGLE_CLIENT_SECRET,
  callbackURL: `${process.env.JANS_SERVER_URL}/passport/auth/google/callback`
},
function(accessToken, refreshToken, profile, cb) {
  return cb(null, profile);
}
));

const app = express()

app.set('trust proxy', 1)
app.use(cookie({
  maxAge: 24 * 60 * 60 * 1000,
  keys: ['qwertyzswedfsaws']
}))

app.use((err, req, { redirect }, next) => {
  console.log(`Unknown Error: ${err}`)
  console.log(err.stack)
})

app.use(passport.initialize())
app.use(passport.session())

app.get('/passport/auth/google/callback', 
  passport.authenticate('google', { failureRedirect: '/login' }),
  function(req, res) {
    // Successful authentication, redirect.
    const user = req.user
    console.log('Auth User: ',  user)
    const ldapMappedUser = {
      uid: user.username || user.id,
      mail: user.email,
      cn: user.displayName,
      displayName: user.displayName,
      givenName: user.name.givenName,
      sn: user.name.familyName,
      provider: "google"
    }
    console.log('ldap Mapped User: ', ldapMappedUser)
    const sub = ldapMappedUser.uid

    const now = new Date().getTime()
    const userJWT = jwt.sign({
      iss: jansPostUrl,
      sub: sub,
      aud: process.env.JANS_SERVER_URL,
      jti: uuidv4(),
      exp: now / 1000 + 30,
      iat: now,
      data: encrypt(ldapMappedUser)
    }, privateKey(), defaultRpOptions())

    console.log('User JWT: ', userJWT)
    return redirectAndPostUserToJans(res, userJWT)
});

app.get('/passport/auth/google/:token',
  validateToken,
  passport.authenticate('google', { scope: ['profile'] })
);

app.get('/passport/token',
  function (req, res, next) {
    const t = jwt.sign(
      { jwt: uuidv4() }, getSaltValue(), { expiresIn: 120 } // 2 min expiration
    )
    res.status(200).send({ token_: t })
  }
)

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'

app.listen(8090, () => {
  console.log('-----------------------\nServer started successfully!, Open this URL http://localhost:8090\n-----------------------')
})

function encrypt (obj) {
  // Encryption compatible with Jans EncryptionService
  const pt = JSON.stringify(obj)
  const encrypt = crypto.createCipheriv('des-ede3-ecb', getSaltValue(), '')
  let encrypted = encrypt.update(pt, 'utf8', 'base64')
  encrypted += encrypt.final('base64')
  return encrypted
}

function privateKey() {
  return fs.readFileSync(process.env.KEY_FILE_PATH, 'utf8')
}

function defaultRpOptions() {
  return {
    algorithm: process.env.KEY_ALG,
    header: {
      typ: 'JWT',
      alg: process.env.KEY_ALG,
      kid: process.env.KEY_ID
    }
  }
}

function getSaltValue() {
  const salt = fs.readFileSync(process.env.SALT_FILE_PATH, 'utf8')
  return /=\s*(\S+)/.exec(salt)[1]
}

function redirectAndPostUserToJans(res, userJWT) {
  res.set('content-type', 'text/html;charset=UTF-8')
  return res.status(200).send(`
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
      <body onload="document.forms[0].submit()">
        <noscript>
          <p>
            <b>Note:</b> your browser does not support JavaScript, please press the Continue
            button to proceed.
          </p>
        </noscript>

        <form action="${jansPostUrl}" method="post">
          <div>
            <input type="hidden" name="user" value="${userJWT}"/>
            <noscript>
              <input type="submit" value="Continue"/>
            </noscript>
          </div>
        </form>
      </body>
    </html>`
  )
}

function validateToken (req, res, next) {
  const t = req.params.token
  try {
    console.log('Validating token')
    verifyJWT(t)
    next()
  } catch (err) {
    const msg = 'Token did not pass validation.'
    console.log(msg)
    next(req, res, msg)
  }
}

const verifyJWT = token => jwt.verify(token, getSaltValue())
