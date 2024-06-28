package io.jans.chip.utils

class AppConfig {
    companion object {
        const val INTEGRITY_APP_SERVER_URL = "https://play-integrity-checker-server-2eua-3ndysngub.vercel.app"
        const val GOOGLE_CLOUD_PROJECT_ID = 618764598105L
        const val SQLITE_DB_NAME = "chipDB"
        const val APP_NAME = "jans-chip"
        const val DEFAULT_S_NO = "1"
        const val ROOM_DATABASE_VERSION = 1
        const val FIDO_CONFIG_URL = "/jans-fido2/restv1/configuration"
        const val OP_CONFIG_URL = "/.well-known/openid-configuration"

        const val KEY_NAME = "thumbSecrxet"
        const val FIDO_ENROLMENT = "FIDO_ENROLMENT"
        const val FIDO_AUTHENTICATION = "FIDO_AUTHENTICATION"

        const val ALLOWED_REGISTRATION_SCOPES = "openid authorization_challenge profile"
        const val SSA = "eyJraWQiOiJzc2FfZTljMGIzZGUtMTU4Yy00Njg4LWIwN2UtYzA1NjM1YzI1ZDllX3NpZ19yczI1NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzb2Z0d2FyZV9pZCI6InRlc3QiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiLCJwYXNzd29yZCJdLCJvcmdfaWQiOiJHbHV1IiwiaXNzIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnIiwic29mdHdhcmVfcm9sZXMiOlsiYWRtaW4iXSwiZXhwIjoxNzIwOTU0NjM3LCJpYXQiOjE3MTgzNjI2MzcsImp0aSI6IjFjZjFhMWUxLTljY2EtNDllNy05ZmIzLWVjNTQ5M2U0YzhkYiJ9.eVVlthC9_xTqzCOcnNrU8lJYtVwy71Y-u4Ja60yzBIxjnM8WKjz9UX8LsIizp9xu7y9CDHRCjUfKXNuzLuegPYnHUoWOxKyaVxKmS1M9_tosW6F3tyKPWSXDqEnQt21lDh5AOy-C7wGTg9f6VueHJd1ALzmpw6doox3z04K9YCo0096-5o5YR28_jV-3yf3bAdze5HjpR3MgMVcqA9O2Tk5T-3NFHM1kBPknB8YKDFFQTJxhlFUaljWe5Enim7ZFksJpqtXaNoRAI-7Fg1DUNnWa1GjPZvUa-gK5qOpJ0gkGBsa0Bhl1LhtdgbRNJd8CNx_Hc62qePe7I9lBdxVK0A"
    }
}