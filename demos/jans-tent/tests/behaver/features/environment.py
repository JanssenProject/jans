from selenium import webdriver
import os
from pyvirtualdisplay import Display

display = Display(visible=0, size=(1024, 768))


def before_all(context):
    os.environ['CURL_CA_BUNDLE'] = ""
    display.start()


def before_scenario(context, scenario):
    options = webdriver.FirefoxOptions()
    options.headless = True
    context.web = webdriver.Firefox()

    # context.web = webdriver.Firefox()


def after_scenario(context, scenario):
    context.web.delete_all_cookies()
    context.web.close()


def after_step(context, step):
    print()


def after_all(context):
    pass
