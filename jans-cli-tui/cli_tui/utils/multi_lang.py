import locale
import gettext

language = 'en'  

try:
    current_locale, encoding = locale.getdefaultlocale()
    language = gettext.translation (language, 'locale/', languages=[language] )
    language.install()
except Exception:
    pass  # no translation catalog; fall back to untranslated strings

_ = gettext.gettext


