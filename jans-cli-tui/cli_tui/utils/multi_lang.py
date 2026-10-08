import contextlib
import locale
import gettext

language = 'en'  

with contextlib.suppress(Exception):
    current_locale, encoding = locale.getdefaultlocale()
    language = gettext.translation (language, 'locale/', languages=[language] )
    language.install()

_ = gettext.gettext


