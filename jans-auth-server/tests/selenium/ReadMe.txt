Here is process how to run it:
1)	We need 2-3 Firefox instances:
-	Run Firefox: "C:\Program Files (x86)\Mozilla Firefox\firefox.exe" -P test -no-remote
-	Add Firefox profile “test”
-	Install into Firefox next addons: https://addons.mozilla.org/en-US/firefox/addon/selenium-ide/
-	
-	Run Firefox: "C:\Program Files (x86)\Mozilla Firefox\firefox.exe" -P test2 -no-remote
-	Add Firefox profile “test2”
-	Install into Firefox next addons: https://addons.mozilla.org/en-US/firefox/addon/selenium-ide/
-	…
2)	Configure SeleniumIDE in each Firefox:
-	Open SeleniumIDE in each browser.
-	Open SeleniumIDE main menu: “Options”->”Options…”
-	Select in field “Selenium Core Extensions” plugin “while.js”. It’s attached to this e-mail.
-	Repeat these steps in all Firefox instances.
3)	Load selenium suite in each instance
4)	Run test in all instances
5)	On success you should get in Selenium log:
…
[info] script is: storedVars['x']=999+1
[info] Executing: |endWhile | | |
[info] Executing: |while | storedVars['x'] < 1000 | |
[info] Test case passed
