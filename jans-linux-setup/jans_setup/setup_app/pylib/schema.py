#credit: this file is ported from https://github.com/python-ldap/python-ldap/blob/master/Lib/ldap/schema/models.py

from tokenizer import split_tokens, extract_tokens


class ObjectClass:

    def __init__(self, s):
        token_defaults = {
                'NAME':(()),
                'DESC': '',
                'OBSOLETE': 0,
                'SUP':(()),
                'STRUCTURAL':None,
                'AUXILIARY':None,
                'ABSTRACT':None,
                'MUST':(()),
                'MAY':(),
                'X-ORIGIN': '',
                'X-RDBM-IGNORE':('',),
              }

        l = split_tokens(s)
        self.oid = l[1]
        self.tokens = extract_tokens(l, token_defaults)

        self.kind = 0

        if self.tokens['ABSTRACT']!=None:
          self.kind = 1
        elif self.tokens['AUXILIARY']!=None:
          self.kind = 2
        if self.kind==0 and not self.tokens['SUP'] and self.oid!='2.5.6.0':
          self.tokens['SUP'] = ('top',)

    def key_attr(self, key, value, quoted=0):
        if type(value) == type(()):
            value = value[0]
        if value:
          if quoted:
            return " %s '%s'" % (key, value.replace("'","\\'"))
          else:
            return " %s %s" % (key, value)
        else:
          return ""


    def key_list(self, key, values, sep=' ', quoted=0):
        if not values:
          return ''
        if quoted:
          quoted_values = [ "'%s'" % value.replace("'","\\'") for value in values ]
        else:
          quoted_values = values
        if len(values)==1:
          return ' %s %s' % (key,quoted_values[0])
        else:
          return ' %s ( %s )' % (key,sep.join(quoted_values))


    def getstr(self):
        result = [str(self.oid)]
        result.append(self.key_list('NAME', self.tokens['NAME'], quoted=1))
        result.append(self.key_attr('DESC', self.tokens['DESC'], quoted=1))
        result.append(self.key_list('SUP', self.tokens['SUP'], sep=' $ '))
        result.append({0:'',1:' OBSOLETE'}[self.tokens['OBSOLETE']])
        result.append({0:' STRUCTURAL',1:' ABSTRACT',2:' AUXILIARY'}[self.kind])
        result.append(self.key_list('MUST', self.tokens['MUST'], sep=' $ '))
        result.append(self.key_list('MAY', self.tokens['MAY'], sep=' $ '))
        result.append(self.key_attr('X-ORIGIN', self.tokens['X-ORIGIN'], quoted=1))

        return u'( %s )' % ''.join(result)

AttributeUsage = {
  'userApplication':0,
  'userApplications':0,
  'directoryOperation':1,
  'distributedOperation':2,
  'dSAOperation':3,
}

class AttributeType:

    def __init__(self, s=''):
        token_defaults = {
            'NAME':(()),
            'DESC':(None,),
            'OBSOLETE':None,
            'SUP':(()),
            'EQUALITY':(None,),
            'ORDERING':(None,),
            'SUBSTR':(None,),
            'SYNTAX':(None,),
            'SINGLE-VALUE':None,
            'COLLECTIVE':None,
            'NO-USER-MODIFICATION':None,
            'USAGE':('userApplications',),
            'X-ORIGIN':(None,),
            'X-ORDERED':(None,),
            'X-RDBM-ADD': (None,),
          }

        if s:

            l = split_tokens(s)
            self.oid = l[1]
            self.tokens = extract_tokens(l, token_defaults)

            try:
              syntax = self.tokens['SYNTAX'][0]
            except IndexError:
              self.syntax = None
              self.syntax_len = None
            else:
              if syntax is None:
                self.syntax = None
                self.syntax_len = None
              else:
                try:
                  self.syntax,syntax_len = self.tokens['SYNTAX'][0].split("{")
                except ValueError:
                  self.syntax = self.tokens['SYNTAX'][0]
                  self.syntax_len = None
                  for i in l:
                    if i.startswith("{") and i.endswith("}"):
                      self.syntax_len=int(i[1:-1])
                else:
                  self.syntax_len = int(syntax_len[:-1])


    def key_attr(self, key, value, quoted=0):
        if type(value) == type(()):
            value = value[0]
        if value:
          if quoted:
            return " %s '%s'" % (key, value.replace("'","\\'"))
          else:
            return " %s %s" % (key, value)
        else:
          return ""

    def key_list(self, key, values, sep=' ', quoted=0):
        if not values:
          return ''
        if quoted:
          quoted_values = [ "'%s'" % value.replace("'","\\'") for value in values ]
        else:
          quoted_values = values
        if len(values)==1:
          return ' %s %s' % (key,quoted_values[0])
        else:
          return ' %s ( %s )' % (key,sep.join(quoted_values))

    def getstr(self):
        result = [str(self.oid)]
        result.append(self.key_list('NAME', self.tokens['NAME'], quoted=1))
        result.append(self.key_attr('DESC', self.tokens['DESC'], quoted=1))
        result.append(self.key_list('SUP', self.tokens['SUP'], sep=' $ '))
        result.append({0:'',1:' OBSOLETE'}[self.tokens['OBSOLETE']!=None])
        result.append(self.key_attr('EQUALITY', self.tokens['EQUALITY']))
        result.append(self.key_attr('ORDERING', self.tokens['ORDERING']))
        result.append(self.key_attr('SUBSTR', self.tokens['SUBSTR']))
        result.append(self.key_attr('SYNTAX', self.tokens['SYNTAX']))
        if self.syntax_len!=None:
          result.append(('{%d}' % (self.syntax_len))*(self.syntax_len>0))
        result.append({0:'',1:' SINGLE-VALUE'}[self.tokens['SINGLE-VALUE']!=None])
        result.append({0:'',1:' COLLECTIVE'}[self.tokens['COLLECTIVE']!=None])
        result.append({0:'',1:' NO-USER-MODIFICATION'}[self.tokens['NO-USER-MODIFICATION']!=None])
        result.append(
          {
            0:"",
            1:" USAGE directoryOperation",
            2:" USAGE distributedOperation",
            3:" USAGE dSAOperation",
          }[AttributeUsage.get(self.tokens['USAGE'][0],0)]
        )
        result.append(self.key_attr('X-ORIGIN', self.tokens['X-ORIGIN'], quoted=1))
        result.append(self.key_attr('X-ORDERED', self.tokens['X-ORDERED'], quoted=1))
        return '( %s )' % ''.join(result)
