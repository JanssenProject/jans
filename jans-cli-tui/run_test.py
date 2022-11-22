#!/usr/bin/env python3
"""
"""
import unittest
from test import test_drop_down
from test import test_getTitledCheckBoxList, test_drop_down, test_date_picker, test_getTitledText, test_getTitledCheckBox, test_getTitledRadioButton
from mock import patch


class Test_DropDownWidget(unittest.TestCase):                                                               
    def test_drop_down1(self):   ### existing value
        with patch('test.prompt', return_value = ('HS1',[('HS1','HS1'),('HS2','HS2'),])) as prompt : ## return_value >> value, values
            self.assertEqual(test_drop_down(), True)
            prompt.assert_called_once_with('input')

    def test_drop_down2(self):  ### non-existing value
        with patch('test.prompt', return_value = ('HS5',[('HS1','HS1'),('HS2','HS2'),])) as prompt :
            self.assertEqual(test_drop_down(), True)
            prompt.assert_called_once_with('input')

    def test_drop_down3(self):  ### wrong type value
        with patch('test.prompt', return_value = (5.2,[('HS1','HS1'),('HS2','HS2'),])) as prompt :
            self.assertEqual(test_drop_down(), True)
            prompt.assert_called_once_with('input')

class Test_DateSelectWidget(unittest.TestCase):
    def test_date_picker1(self):   ### valid value
        with patch('test.prompt', return_value = ('2022-10-08T06:18:20',self)) as prompt : ## return_value >> value, parent 
            self.assertEqual(test_date_picker(), True)
            prompt.assert_called_once_with('input')

    def test_date_picker2(self):   ### non-valid value   >>>>>>>>>>>>>>>>>>>>>>>>>>> ## TODO FALID
        with patch('test.prompt', return_value = ('dumy string',self)) as prompt : ## return_value >> value, parent 
            self.assertEqual(test_date_picker(), False)
            prompt.assert_called_once_with('input')

    def test_date_picker3(self):   ### wrong type value   >>>>>>>>>>>>>>>>>>>>>>>>>>> ## TODO FALID
        with patch('test.prompt', return_value = (5,self)) as prompt : ## return_value >> value, parent 
            self.assertEqual(test_date_picker(), False)
            prompt.assert_called_once_with('input')

class Test_GetTitledText(unittest.TestCase):
    def test_getTitledText1(self):   ### valid values
        with patch('test.prompt', return_value = ("Title","value",1,False,0)) as prompt : ## return_value >> title, value, height, read_only, width
            self.assertEqual(test_getTitledText(), True)
            prompt.assert_called_once_with('input')

    def test_getTitledText2(self):   ### valid values with hight=3, and read_only
        with patch('test.prompt', return_value = ("Title","value",3,True,2)) as prompt : ## return_value >> title, value, height, read_only, width
            self.assertEqual(test_getTitledText(), True)
            prompt.assert_called_once_with('input')


    def test_getTitledText3(self):   ### non-valid title >>>>>>>>>>>>>>>>>>>>>>>>>>> ## TODO FALID
        with patch('test.prompt', return_value = (5,"value",1,False,0)) as prompt : ## return_value >> title, value, height, read_only, width
            self.assertEqual(test_getTitledText(), True)
            prompt.assert_called_once_with('input')


    def test_getTitledText4(self):   ### non-valid value >>>>>>>>>>>>>>>>>>>>>>>>>>> ## TODO FALID
        with patch('test.prompt', return_value = ("Title",2.2,1,False,0)) as prompt : ## return_value >> title, value, height, read_only, width
            self.assertEqual(test_getTitledText(), True)
            prompt.assert_called_once_with('input')


    def test_getTitledText5(self):   ### non-valid height 
        with patch('test.prompt', return_value = ("Title","value",2.2,False,0)) as prompt : ## return_value >> title, value, height, read_only, width
            self.assertEqual(test_getTitledText(), True)
            prompt.assert_called_once_with('input')   

    def test_getTitledText6(self):   ### non-valid width 
        with patch('test.prompt', return_value = ("Title","value",1,False,4.5)) as prompt : ## return_value >> title, value, height, read_only, width
            self.assertEqual(test_getTitledText(), True)
            prompt.assert_called_once_with('input') 

class Test_GetTitledCheckBoxList(unittest.TestCase):
    def test_getTitledCheckBoxList1(self):   ### valid values
        with patch('test.prompt', return_value = ("Title",['value1','value2'],[('value1', 'value1'), ('value2', 'value2'), ('value3', 'value3')])) as prompt : ## return_value >> title,values,current_values 
            self.assertEqual(test_getTitledCheckBoxList(), True)
            prompt.assert_called_once_with('input')
        
    def test_getTitledCheckBoxList2(self):   ### invalid selected values
        with patch('test.prompt', return_value = ("Title",['value5','value6'],[('value1', 'value1'), ('value2', 'value2'), ('value3', 'value3')])) as prompt : ## return_value >> title,values,current_values 
            self.assertEqual(test_getTitledCheckBoxList(), True)
            prompt.assert_called_once_with('input')

    def test_getTitledCheckBoxList3(self):   ### invalid selected values type  ### in-valid values >>>>>>>>>>>>>>>>>>>>>>>>>>> ##FALID
        with patch('test.prompt', return_value = ("Title",[5,6],[('value1', 'value1'), ('value2', 'value2'), ('value3', 'value3')])) as prompt : ## return_value >> title,values,current_values 
            self.assertEqual(test_getTitledCheckBoxList(), True)
            prompt.assert_called_once_with('input')

class Test_GetTitledCheckBox(unittest.TestCase):
    def test_getTitledCheckBox1(self):   ### valid values
        with patch('test.prompt', return_value = ("Title",False)) as prompt : ## return_value >> title,checked
            self.assertEqual(test_getTitledCheckBox(), True)
            prompt.assert_called_once_with('input')
        
    def test_getTitledCheckBox2(self):    ### valid values
        with patch('test.prompt', return_value = ("Title",False)) as prompt : ## return_value >> title,checked
            self.assertEqual(test_getTitledCheckBox(), True)
            prompt.assert_called_once_with('input')

    def test_getTitledCheckBox3(self):    ### valid values type  
        with patch('test.prompt', return_value = ("Title",'False')) as prompt : ## return_value >> title,checked
            self.assertEqual(test_getTitledCheckBox(), False)
            prompt.assert_called_once_with('input')

class Test_GetTitledRadioButton(unittest.TestCase):
   def test_getTitledRadioButton1(self):   ### valid values
    with patch('test.prompt', return_value = ( "title",[('value1', 'value1'),('value2', 'value2')],'value1')) as prompt : ## return_value >> title,values,current_value 
        self.assertEqual(test_getTitledRadioButton(), True)
        prompt.assert_called_once_with('input')

   def test_getTitledRadioButton2(self):   ### in-valid values >>>>>>>>>>>>>>>>>>>>>>>>>>>  ## TODO FALID
    with patch('test.prompt', return_value = ( "title",[('value1', 'value1'),('value2', 'value2')],'value5'))  as prompt : ## return_value >> title,values,current_value 
        self.assertEqual(test_getTitledRadioButton(), True)
        prompt.assert_called_once_with('input')

   def test_getTitledRadioButton3(self):   ### in-valid values >>>>>>>>>>>>>>>>>>>>>>>>>>>  ## TODO FALID
    with patch('test.prompt', return_value = ( "title",[('value1', 'value1'),('value2', 'value2')],5))  as prompt : ## return_value >> title,values,current_value 
        self.assertEqual(test_getTitledRadioButton(), True)
        prompt.assert_called_once_with('input')

if __name__ == '__main__':
   unittest.main()