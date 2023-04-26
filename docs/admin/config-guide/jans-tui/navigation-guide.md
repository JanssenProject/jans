---
tags:
- installation
- administration
- configuration
- config
- tui
---

## General Structure:

1. The TUI consists of many tabs, each tab may or may not contain nested tabs.
2. The TUI consists of one main navigation bar which contains (Auth Server, FIDO, SCIM, Scripts, Users, and Jans CLI)

![image](https://user-images.githubusercontent.com/63171603/216954725-46c556bd-11ae-46f0-bb1c-e9c5f1e89d60.png)

3. The tab currently in focus is highlighted at the top.
ex:

![image](https://user-images.githubusercontent.com/63171603/216954883-749f5493-9313-4326-8429-7898e0cd55f3.png)

Note:

* in the image provided below `SCIM` is focused 
* We can notice that there is a cursor mark on the first of the Navigation bar line, which indicates that the focus currently in this widget
![cursor_line](https://user-images.githubusercontent.com/63171603/216955914-01f28440-81f3-437d-a018-c0f117dcd24e.gif)

---------------------------------------------

## General Purpose shortcuts 

*  `f1` :  
Open help dialog (currently, help dialog is static, in the near future it will dynamically change according to the current position inside the TUI )

![f1](https://user-images.githubusercontent.com/63171603/210387872-099413f4-5e33-43e9-86c7-440e55c9f85c.png)

*  `ESC`, `f4` :  
Closes any dialog, even nested dialogs

![esc](https://user-images.githubusercontent.com/63171603/210389011-97da92b1-57b8-4628-9810-3a571255ea20.gif)

* `Ctrl+c`(Terminate) :  
Terminate the program (SigKILL)

------------------------------------

## Navigation Shortcuts  
### General Navigation shortcuts  

* `tab`(Next):  
focus on the next field or widget.  

![tab](https://user-images.githubusercontent.com/63171603/216956961-f88010e9-6f81-4401-b920-e7522c774115.gif)

* `shift + tab`(Previous):  
focus on the previous field or widget  

![shift_tab](https://user-images.githubusercontent.com/63171603/216957299-595b5ca1-9d9a-4d8b-8b19-ae939390f9ea.gif)


* `PageUp`(Next tab):  
Jump to the next tab in dialogs (ex: Client dialog)

Note:  
* the focus must be inside the current tab  
* In the Gif provided, the focus was inside the tab itself before we can press the `PageUp` key  
* If the focus is on the tab name itself, we can navigate to other tabs by the `Up` key and `Down` key  

![page_down](https://user-images.githubusercontent.com/63171603/216957110-8a0629fa-dab8-4254-9900-dcc4fed5225f.gif)

* `PageDown`(Previous tab) :  
Jump to the previous tab in dialogs (ex: Client dialog)  
Note:  
* the focus must be inside the current tab  
* In the Gif provided, the focus was inside the tab itself before we can press the `PageDown` key  
* If the focus is on the tab name itself, we can navigate to other tabs by the `Up` key and `Down` key  

![page_up](https://user-images.githubusercontent.com/63171603/216956999-5c813da5-f3a8-4755-9324-a4694b06ab68.gif)

### Main tabs Navigation shortcuts  

* `Alt + key`  
You can navigate to any tab inside the TUI by using the `Alt` key followed by a certain letter from the tab you want to navigate too  

Note:  
* the colored character inside each Tab name is the specified letter for the navigation.  
* you can't navigate from a tab in the main top navigation bar to a nested navigation bar inside another tab, you must navigate to it first.  

examples on the navigation letters for the main navigation bar:

* `Auth Server` -> `A`  
* `FIDO` -> `F`  
* `SCIM` -> `C`  
* `Scripts` -> `r`  
* `Users` -> `U`  
* `Jans CLI` -> `J`  

![navigation](https://user-images.githubusercontent.com/63171603/210389648-d4f7692d-d2c1-48bd-a05d-61dd887c0063.gif)

-----------------------------------------

## Custom widget shortcuts 

Note:  
* This section contains any widgets with special shortcuts and behavior  

### Lists (Vertical Nav bar)  
currently, we have about 6 List widgets

Note:  
* Any list widget can be navigated between its items by the `Up` key and `Down` key  
* There are some shortcuts here that can be used in most but not all List widgets  

* `delete`:  
can be used to delete a certain item

* `d`:  
can be used to view all the selected item data in the `JSON` format

* `Enter`:  
can be used to edit the current item

ex:

![image](https://user-images.githubusercontent.com/63171603/216964422-1dcd692d-b9d0-4386-b12c-8a66b2dc3b4a.png)

--------------------------------------------------
### Date Widgets
The date widget can be used to select a date and time. ex (Client Expiration Date)

![image](https://user-images.githubusercontent.com/63171603/216964886-12eefe6a-5617-4ce5-9db9-646c844ce18b.png)

* `tab`, `shift + tab`:  
can be used to navigate between date and time

![image](https://user-images.githubusercontent.com/63171603/216969015-26263039-abef-404a-bcf7-4f4a080ded1f.png)

* `Up`, `Down`, `Right`, and `Left` arrows:  
can be used to change the date and time

Note:
* in the `date` section, if you navigate to the right border and then pressed `Right`, the year will be increased.  
* in the `date` section, if you navigate to the left border and then pressed `Left`, the year will be decreased.  
* in the `date` section, if you navigate to the up border and then pressed `Up`, the month will be decreased.  
* in the `date` section, if you navigate to the down border and then pressed `Down`, the month will be increased.  

![date_wid_year](https://user-images.githubusercontent.com/63171603/216966941-90cfed2e-5e56-4f92-b10c-7bebbb3c3229.gif)

* in the `time` section, you can navigate between hour, minute, and second by the `Right` key and the `Left` key.  
* in the `time` section, you can increase the time (hour, minute, or second) by the `Up` key and the `Down` key.

![date_wid_time](https://user-images.githubusercontent.com/63171603/216968005-74f5a56c-fa9d-495d-95ad-ca71576198ab.gif)

--------------------------------------------------
### Label Widgets

The label widget is used with scopes to show certain items instead of a long one, it is similar to the list widget

* `tab`, `shift + tab`:  
can be used to navigate between the button and the labels

* `Right` key and `Left` key  
can be used to navigate between the labels

* `d`:  
can be used to view all the data of the selected label

* `delete`:  
can be used to delete a certain label

![image](https://user-images.githubusercontent.com/63171603/216968417-4cb15f2a-f01c-48bb-a27d-e4e73edb8082.png)

* `p`: 
can be used to update user's password. 

![Clipboard - April 3, 2023 9_29 PM](https://user-images.githubusercontent.com/7156097/229603328-45232024-26e0-473b-b74a-4b08b5bcaae7.png)


