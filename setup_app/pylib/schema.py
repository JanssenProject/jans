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
