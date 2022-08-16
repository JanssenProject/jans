
    def handel_long_string (self,text,values,cb):
        lines = []
        if len(text) > 20 :
            title_list=text.split(' ')
            dum = ''
            for i in range(len(title_list)):
                if len(dum) < 20 :
                    if len(title_list[i] + dum) < 30 :
                        dum+=title_list[i] +' '
                    else :
                        lines.append(dum.strip())
                        dum = title_list[i] + ' '
                else :
                    lines.append(dum.strip())
                    dum = title_list[i]  + ' '
            lines.append(dum)
            num_lines = len(lines)
            width = len(max(lines, key=len))
        else:
            width = len(text)
            lines.append(text)
            num_lines = len(lines)


        new_title,title_lines = '\n'.join(lines) , num_lines 


        if title_lines <= len(values) :  ### if num of values (value lines) < = title_lines
            lines_under_value = 0   
        else :
            lines_under_value = abs(title_lines-len(values))
        
        if lines_under_value !=0 :
            cd = HSplit([   
                cb,
                Label(text=('\n')*(lines_under_value-1)) 
            ])
        else :
            cd = cb     

        if title_lines <= len(values) :  ### if num of values (value lines) < = title_lines
            lines_under_title = abs(len(values) - title_lines)
        else :
            lines_under_title = 0   

        # first one >> solved >> title
        if lines_under_title >=1 :
            for i in range(lines_under_title):
                new_title +='\n' 
        else :
            pass

        return  new_title , cd , width



    def getTitledCheckBox(self, title, name, values,style=''):
        ## two problem here
        ## first one is the space under title (if many values)
        ## Sec one is the space under values (if multiline title)
        
        
        cb = CheckboxList(values=[(o,o) for o in values],)
        cb.window.jans_name = name

        li,cd,width = self.handel_long_string(title,values,cb)


        return VSplit([Label(text=li, width=width,style=style,wrap_lines=False), cd])



