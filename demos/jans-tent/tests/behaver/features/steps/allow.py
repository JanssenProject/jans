from behave import when, then, given
import requests
import time
from selenium.webdriver.common.by import By

base_url = "https://chris.testingenv.org"


def cookiesTransformer(sel_session_id, sel_other_cookies):
    ''' This transform cookies from selenium to requests '''
    s = requests.Session()
    s.cookies.set('session_id', sel_session_id)
    i = 0
    while i < len(sel_other_cookies):
        s.cookies.set(sel_other_cookies[i]['name'],
                      sel_other_cookies[i]['value'],
                      path=sel_other_cookies[i]['path'],
                      domain=sel_other_cookies[i]['domain'],
                      secure=sel_other_cookies[i]['secure'],
                      rest={'httpOnly': sel_other_cookies[i]['httpOnly']})
        i = i + 1

    return s


@given(u'username is "{username}"')
def define_username(context, username):
    context.username = username
    context.password = "test123"


@given(u'user is authenticated')
def user_authenticates(context):
    context.web.get("https://chris.testingenv.org/login")
    time.sleep(3)
    context.web.set_window_size(625, 638)
    context.web.find_element(By.ID, "username").click()
    context.web.find_element(By.ID, "username").send_keys("johndoo")
    time.sleep(3)
    context.web.find_element(By.ID, "password").send_keys("test123")
    context.web.find_element(By.ID, "loginButton").click()
    time.sleep(3)


@given(u'protected content link is {protected_content}')
def define_protected_content_link(context, protected_content):
    context.protected_content = protected_content


@when(u'user clicks the protected content link')
def user_clicks_protected_content_link(context):

    context.web.get(base_url)
    time.sleep(2)
    context.web.find_element_by_xpath(
        '//a[@href="' + "https://chris.testingenv.org/protected-content" +
        '"]').click()
    context.has_clicked = True
    context.response = requests.get(context.protected_content)


@then(u'user access the protected content link')
def user_access_protected_content_link(context):
    # WE FETCH THE COOKIES FROM SELENIUM AND PASS THEM TO REQUESTS TO VALIDATE
    #sel_cookies = context.web.get_cookies()
    #sel_cookie = sel_cookies[0]
    # set cookie in requests

    # get session id from selenium
    #sel_session_id = context.web.session_id
    '''
    sess = requests.Session()
    
    sess.cookies.set('session_id',sel_session_id)
    sess.cookies.set(
        sel_cookie['name'],
        sel_cookie['value'],
        path = sel_cookie['path'],
        domain = sel_cookie['domain'],
        secure = sel_cookie['secure'],
        rest= {'httpOnly' : sel_cookie['httpOnly']}
    )
   
    new_sess = cookiesTransformer(sel_session_id,sel_cookies)
    '''
    new_sess = cookiesTransformer(context.web.session_id,
                                  context.web.get_cookies())
    res = new_sess.get(context.protected_content, verify=False)

    assert res.url == context.protected_content


@given(u'user does not exist')
def user_does_not_exist(context):
    pass


@then(u'user goes to external login page')
def user_directed_to_external_login_page(context):
    #context.web.get("https://chris.testingenv.org/login")

    time.sleep(1)
    external_login_url = 'https://chris.gluutwo.org/oxauth/login.htm'
    #import ipdb; ipdb.set_trace()
    assert (context.web.current_url == external_login_url)
    #new_sess = cookiesTransformer(context.web.session_id,context.web.get_cookies())


@given(u'user role is "{role}"')
def define_user_role(context, role):
    context.role = role


@then(u'user gets a 403 error')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then user gets a 403 error')
