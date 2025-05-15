# credit: barrowed from https://github.com/cannatag/ldap3/blob/dev/ldap3/utils/dn.py

from string import ascii_letters, digits, hexdigits

STATE_ANY = 0
STATE_ESCAPE = 1
STATE_ESCAPE_HEX = 2


def _validate_attribute_value(attribute_value):
    if not attribute_value:
        return False

    if attribute_value[0] == '#':  # only hex characters are valid
        for c in attribute_value:
            if c not in hexdigits:  # allowed only hex digits as per RFC 4514
                raise ValueError('character ' + c + ' not allowed in hex representation of attribute value')
        if len(attribute_value) % 2 == 0:  # string must be # + HEX HEX (an odd number of chars)
            raise ValueError('hex representation must be in the form of <HEX><HEX> pairs')
    if attribute_value[0] == ' ':  # unescaped space cannot be used as leading or last character
        raise ValueError('SPACE must be escaped as leading character of attribute value')
    if attribute_value.endswith(' ') and not attribute_value.endswith('\\ '):
        raise ValueError('SPACE must be escaped as trailing character of attribute value')

    state = STATE_ANY
    for c in attribute_value:
        if state == STATE_ANY:
            if c == '\\':
                state = STATE_ESCAPE
            elif c in '"#+,;<=>\00':
                raise ValueError('special character ' + c + ' must be escaped')
        elif state == STATE_ESCAPE:
            if c in hexdigits:
                state = STATE_ESCAPE_HEX
            elif c in ' "#+,;<=>\\\00':
                state = STATE_ANY
            else:
                raise ValueError('invalid escaped character ' + c)
        elif state == STATE_ESCAPE_HEX:
            if c in hexdigits:
                state = STATE_ANY
            else:
                raise ValueError('invalid escaped character ' + c)

    # final state
    if state != STATE_ANY:
        raise ValueError('invalid final character')

    return True



def _validate_attribute_type(attribute_type):
    if not attribute_type:
        raise ValueError('attribute type not present')

    if attribute_type == '<GUID':  # patch for AD DirSync
        return True

    for c in attribute_type:
        if not (c in ascii_letters or c in digits or c == '-'):  # allowed uppercase and lowercase letters, digits and hyphen as per RFC 4512
            raise ValueError('character \'' + c + '\' not allowed in attribute type')

    if attribute_type[0] in digits or attribute_type[0] == '-':  # digits and hyphen not allowed as first character
        raise ValueError('character \'' + attribute_type[0] + '\' not allowed as first character of attribute type')

    return True


def _split_ava(ava, escape=False, strip=True):
    equal = ava.find('=')
    while equal > 0:  # not first character
        if ava[equal - 1] != '\\':  # not an escaped equal so it must be an ava separator
            # attribute_type1 = ava[0:equal].strip() if strip else ava[0:equal]
            if strip:
                attribute_type = ava[0:equal].strip()
                attribute_value = _escape_attribute_value(ava[equal + 1:].strip()) if escape else ava[equal + 1:].strip()
            else:
                attribute_type = ava[0:equal]
                attribute_value = _escape_attribute_value(ava[equal + 1:]) if escape else ava[equal + 1:]

            return attribute_type, attribute_value
        equal = ava.find('=', equal + 1)

    return '', (ava.strip if strip else ava)  # if no equal found return only value

def _find_first_unescaped(dn, char, pos):
    while True:
        pos = dn.find(char, pos)
        if pos == -1:
            break  # no char found
        if pos > 0 and dn[pos - 1] != '\\':  # unescaped char
            break
        elif pos > 1 and dn[pos - 1] == '\\':  # may be unescaped
            escaped = True
            for c in dn[pos - 2:0:-1]:
                if c == '\\':
                    escaped = not escaped
                else:
                    break
            if not escaped:
                break
        pos += 1

    return pos


def _find_last_unescaped(dn, char, start, stop=0):
    while True:
        stop = dn.rfind(char, start, stop)
        if stop == -1:
            break
        if stop >= 0 and dn[stop - 1] != '\\':
            break
        elif stop > 1 and dn[stop - 1] == '\\':  # may be unescaped
            escaped = True
            for c in dn[stop - 2:0:-1]:
                if c == '\\':
                    escaped = not escaped
                else:
                    break
            if not escaped:
                break
        if stop < start:
            stop = -1
            break

    return stop

def _get_next_ava(dn):
    comma = _find_first_unescaped(dn, ',', 0)
    plus = _find_first_unescaped(dn, '+', 0)

    if plus > 0 and (plus < comma or comma == -1):
        equal = _find_first_unescaped(dn, '=', plus + 1)
        if equal > plus + 1:
            plus = _find_last_unescaped(dn, '+', plus, equal)
            return dn[:plus], '+'

    if comma > 0:
        equal = _find_first_unescaped(dn, '=', comma + 1)
        if equal > comma + 1:
            comma = _find_last_unescaped(dn, ',', comma, equal)
            return dn[:comma], ','

    return dn, ''


def _escape_attribute_value(attribute_value):
    if not attribute_value:
        return ''

    if attribute_value[0] == '#':  # with leading SHARP only pairs of hex characters are valid
        valid_hex = True
        if len(attribute_value) % 2 == 0:  # string must be # + HEX HEX (an odd number of chars)
            valid_hex = False

        if valid_hex:
            for c in attribute_value:
                if c not in hexdigits:  # allowed only hex digits as per RFC 4514
                    valid_hex = False
                    break

        if valid_hex:
            return attribute_value

    state = STATE_ANY
    escaped = ''
    tmp_buffer = ''
    for c in attribute_value:
        if state == STATE_ANY:
            if c == '\\':
                state = STATE_ESCAPE
            elif c in '"#+,;<=>\00':
                escaped += '\\' + c
            else:
                escaped += c
        elif state == STATE_ESCAPE:
            if c in hexdigits:
                tmp_buffer = c
                state = STATE_ESCAPE_HEX
            elif c in ' "#+,;<=>\\\00':
                escaped += '\\' + c
                state = STATE_ANY
            else:
                escaped += '\\\\' + c
        elif state == STATE_ESCAPE_HEX:
            if c in hexdigits:
                escaped += '\\' + tmp_buffer + c
            else:
                escaped += '\\\\' + tmp_buffer + c
            tmp_buffer = ''
            state = STATE_ANY

    # final state
    if state == STATE_ESCAPE:
        escaped += '\\\\'
    elif state == STATE_ESCAPE_HEX:
        escaped += '\\\\' + tmp_buffer

    if escaped[0] == ' ':  # leading SPACE must be escaped
        escaped = '\\' + escaped

    if escaped[-1] == ' ' and len(escaped) > 1 and escaped[-2] != '\\':  # trailing SPACE must be escaped
        escaped = escaped[:-1] + '\\ '

    return escaped


def parse_dn(dn, escape=False, strip=False):
    """
    Parses a DN into syntactic components
    :param dn:
    :param escape:
    :param strip:
    :return:
    a list of tripels representing `attributeTypeAndValue` elements
    containing `attributeType`, `attributeValue` and the following separator (`COMMA` or `PLUS`) if given, else an empty `str`.
    in their original representation, still containing escapes or encoded as hex.
    """
    rdns = []
    avas = []
    while dn:
        ava, separator = _get_next_ava(dn)  # if returned ava doesn't containg any unescaped equal it'a appended to last ava in avas

        dn = dn[len(ava) + 1:]
        if _find_first_unescaped(ava, '=', 0) > 0 or len(avas) == 0:
            avas.append((ava, separator))
        else:
            avas[len(avas) - 1] = (avas[len(avas) - 1][0] + avas[len(avas) - 1][1] + ava, separator)

    for ava, separator in avas:
        attribute_type, attribute_value = _split_ava(ava, escape, strip)

        if not _validate_attribute_type(attribute_type):
            raise ValueError('unable to validate attribute type in ' + ava)

        if not _validate_attribute_value(attribute_value):
            raise ValueError('unable to validate attribute value in ' + ava)

        rdns.append((attribute_type, attribute_value, separator))
        dn = dn[len(ava) + 1:]

    if not rdns:
        raise ValueError('empty dn')

    return rdns

