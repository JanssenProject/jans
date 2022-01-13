"""ldif - generate and parse LDIF data (see RFC 2849)."""


import base64
import re
import logging
from collections import OrderedDict
from typing import Optional

from urllib.parse import urlparse
from urllib.request import urlopen

__version__ = "4.0.0"

__all__ = [
    # constants
    "LDIF_PATTERN",
    # classes
    "LDIFWriter",
    "LDIFParser",
]

log = logging.getLogger("ldif")

ATTRTYPE_PATTERN = r"[\w;.-]+(;[\w_-]+)*"
ATTRVALUE_PATTERN = r'(([^,]|\\,)+|".*?")'
ATTR_PATTERN = ATTRTYPE_PATTERN + r"[ ]*=[ ]*" + ATTRVALUE_PATTERN
RDN_PATTERN = ATTR_PATTERN + r"([ ]*\+[ ]*" + ATTR_PATTERN + r")*[ ]*"
DN_PATTERN = RDN_PATTERN + r"([ ]*,[ ]*" + RDN_PATTERN + r")*[ ]*"
DN_REGEX = re.compile("^%s$" % DN_PATTERN)

LDIF_PATTERN = (
    "^((dn(:|::) %(DN_PATTERN)s)|(%(ATTRTYPE_PATTERN) s(:|::) .*)$)+" % vars()
)

MOD_OPS = ["add", "delete", "replace"]
CHANGE_TYPES = ["add", "delete", "modify", "modrdn"]


def is_dn(s):
    """Return True if s is a LDAP DN."""
    if s == "":
        return True
    rm = DN_REGEX.match(s)
    return rm is not None and rm.group(0) == s


UNSAFE_STRING_PATTERN = (
    "(^[^\x01-\x09\x0b-\x0c\x0e-\x1f\x21-\x39\x3b\x3d-\x7f]"
    "|[^\x01-\x09\x0b-\x0c\x0e-\x7f])"
)
UNSAFE_STRING_RE = re.compile(UNSAFE_STRING_PATTERN)


def lower(l):
    """Return a list with the lowercased items of l."""
    return [i.lower() for i in l or []]


class LDIFWriter:
    """Write LDIF entry or change records to file object.

    :type output_file: file-like object in binary mode
    :param output_file: File for output

    :type base64_attrs: List[string]
    :param base64_attrs: List of attribute types to be base64-encoded in any
        case

    :type cols: int
    :param cols: Specifies how many columns a line may have before it is
        folded into many lines

    :type line_sep: bytearray
    :param line_sep: line separator

    :type encoding: string
    :param encoding: Encoding to use for converting values to bytes.  Note that
        the spec requires the dn field to be UTF-8 encoded, so it does not
        really make sense to use anything else.  Default: ``'utf8'``.
    """

    def __init__(
        self, output_file, base64_attrs=(), cols=76, line_sep=b"\n", encoding="utf8"
    ):
        self._output_file = output_file
        self._base64_attrs = lower(base64_attrs)
        self._cols = cols
        self._line_sep = line_sep
        self._encoding = encoding

        self.records_written = 0  #: number of records that have been written

    def _fold_line(self, line):
        """Write string line as one or more folded lines."""
        if len(line) <= self._cols:
            self._output_file.write(line)
            self._output_file.write(self._line_sep)
        else:
            pos = self._cols
            self._output_file.write(line[0 : self._cols])
            self._output_file.write(self._line_sep)
            while pos < len(line):
                self._output_file.write(b" ")
                end = min(len(line), pos + self._cols - 1)
                self._output_file.write(line[pos:end])
                self._output_file.write(self._line_sep)
                pos = end

    def _needs_base64_encoding(self, attr_type, attr_value):
        """Return True if attr_value has to be base-64 encoded.

        This is the case because of special chars or because attr_type is in
        self._base64_attrs
        """
        return (
            attr_type.lower() in self._base64_attrs
            or isinstance(attr_value, bytes)
            or UNSAFE_STRING_RE.search(attr_value) is not None
        )

    def _unparse_attr(self, attr_type, attr_value):
        """Write a single attribute type/value pair."""
        if self._needs_base64_encoding(attr_type, attr_value):
            if not isinstance(attr_value, bytes):
                attr_value = attr_value.encode(self._encoding)
            encoded = base64.encodebytes(attr_value).replace(b"\n", b"").decode("ascii")
            line = ":: ".join([attr_type, encoded])
        else:
            line = ": ".join([attr_type, attr_value])
        self._fold_line(line.encode("ascii"))

    def _unparse_entry_record(self, entry):
        """
        :type entry: Dict[string, List[string]]
        :param entry: Dictionary holding an entry
        """
        for attr_type in sorted(entry.keys()):
            for attr_value in entry[attr_type]:
                self._unparse_attr(attr_type, attr_value)

    def _unparse_changetype(self, mod_len):
        """Detect and write the changetype."""
        if mod_len == 2:
            changetype = "add"
        elif mod_len == 3:
            changetype = "modify"
        else:
            raise ValueError("modlist item of wrong length")

        self._unparse_attr("changetype", changetype)

    def _unparse_change_record(self, modlist):
        """
        :type modlist: List[Tuple]
        :param modlist: List of additions (2-tuple) or modifications (3-tuple)
        """
        mod_len = len(modlist[0])
        self._unparse_changetype(mod_len)

        for mod in modlist:
            if len(mod) != mod_len:
                raise ValueError("Subsequent modlist item of wrong length")

            if mod_len == 2:
                mod_type, mod_vals = mod
            elif mod_len == 3:
                mod_op, mod_type, mod_vals = mod
                self._unparse_attr(MOD_OPS[mod_op], mod_type)

            for mod_val in mod_vals:
                self._unparse_attr(mod_type, mod_val)

            if mod_len == 3:
                self._output_file.write(b"-" + self._line_sep)

    def unparse(self, dn, record):
        """Write an entry or change record to the output file.

        :type dn: string
        :param dn: distinguished name

        :type record: Union[Dict[string, List[string]], List[Tuple]]
        :param record: Either a dictionary holding  an entry or a list of
            additions (2-tuple) or modifications (3-tuple).
        """
        self._unparse_attr("dn", dn)
        if isinstance(record, dict):
            self._unparse_entry_record(record)
        elif isinstance(record, list):
            self._unparse_change_record(record)
        else:
            raise ValueError("Argument record must be dictionary or list")
        self._output_file.write(self._line_sep)
        self.records_written += 1


class LDIFParser:
    """Read LDIF entry or change records from file object.

    :type input_file: file-like object in binary mode
    :param input_file: file to read the LDIF input from

    :type ignored_attr_types: List[string]
    :param ignored_attr_types: List of attribute types that will be ignored

    :type process_url_schemes: List[bytearray]
    :param process_url_schemes: List of URL schemes to process with urllib.
        An empty list turns off all URL processing and the attribute is
        ignored completely.

    :type line_sep: bytearray
    :param line_sep: line separator

    :type encoding: string
    :param encoding: Encoding to use for converting values to unicode strings.
        If decoding failes, the raw bytestring will be used instead. You can
        also pass ``None`` which will skip decoding and always produce
        bytestrings. Note that this only applies to entry values. ``dn`` and
        entry keys will always be unicode strings.

    :type strict: boolean
    :param strict: If set to ``False``, recoverable parse errors will produce
        log warnings rather than exceptions.
    """

    def _strip_line_sep(self, s):
        """Strip trailing line separators from s, but no other whitespaces."""
        if s[-2:] == b"\r\n":
            return s[:-2]
        elif s[-1:] == b"\n":
            return s[:-1]
        else:
            return s

    def __init__(
        self,
        input_file,
        ignored_attr_types=(),
        process_url_schemes=(),
        line_sep=b"\n",
        encoding: Optional[str] = "utf8",
        strict=True,
    ):
        self._input_file = input_file
        self._process_url_schemes = lower(process_url_schemes)
        self._ignored_attr_types = lower(ignored_attr_types)
        self._line_sep = line_sep
        self._encoding = encoding
        self._strict = strict

        self.line_counter = 0  #: number of lines that have been read
        self.byte_counter = 0  #: number of bytes that have been read
        self.records_read = 0  #: number of records that have been read

    def _iter_unfolded_lines(self):
        """Iter input unfoled lines. Skip comments."""
        line = self._input_file.readline()
        while line:
            self.line_counter += 1
            self.byte_counter += len(line)

            line = self._strip_line_sep(line)

            nextline = self._input_file.readline()
            while nextline and nextline[:1] == b" ":
                line += self._strip_line_sep(nextline)[1:]
                nextline = self._input_file.readline()

            if not line.startswith(b"#"):
                yield line
            line = nextline

    def _iter_blocks(self):
        """Iter input lines in blocks separated by blank lines."""
        lines = []
        for line in self._iter_unfolded_lines():
            if line:
                lines.append(line)
            elif lines:
                self.records_read += 1
                yield lines
                lines = []
        if lines:
            self.records_read += 1
            yield lines

    def _decode_value(self, attr_type, attr_value):
        if attr_type == "dn":
            try:
                return attr_type, attr_value.decode("utf8")
            except UnicodeError as err:
                self._error(err)
                return attr_type, attr_value.decode("utf8", "ignore")

        elif self._encoding is not None:
            try:
                return attr_type, attr_value.decode(self._encoding)
            except UnicodeError:
                pass

        return attr_type, attr_value

    def _parse_attr(self, line: bytes):
        """Parse a single attribute type/value pair."""
        colon_pos = line.index(b":")
        attr_type = line[0:colon_pos].decode("ascii")

        if line[colon_pos:].startswith(b"::"):
            attr_value = base64.decodebytes(line[colon_pos + 2 :])
        elif line[colon_pos:].startswith(b":<"):
            url = line[colon_pos + 2 :].strip()
            attr_value = b""
            if self._process_url_schemes:
                u = urlparse(url)
                if u[0] in self._process_url_schemes:
                    attr_value = urlopen(url.decode("ascii")).read()
        else:
            attr_value = line[colon_pos + 1 :].strip()

        return self._decode_value(attr_type, attr_value)

    def _error(self, msg):
        if self._strict:
            raise ValueError(msg)
        else:
            log.warning(msg)

    def _check_dn(self, dn, attr_value):
        """Check dn attribute for issues."""
        if dn is not None:
            self._error("Two lines starting with dn: in one record.")
        if not is_dn(attr_value):
            self._error(
                "No valid string-representation of "
                "distinguished name %s." % attr_value
            )

    def _check_changetype(self, dn, changetype, attr_value):
        """Check changetype attribute for issues."""
        if dn is None:
            self._error("Read changetype: before getting valid dn: line.")
        if changetype is not None:
            self._error("Two lines starting with changetype: in one record.")
        if attr_value not in CHANGE_TYPES:
            self._error("changetype value %s is invalid." % attr_value)

    def _parse_entry_record(self, lines):
        """Parse a single entry record from a list of lines."""
        dn = None
        entry = OrderedDict()

        for line in lines:
            attr_type, attr_value = self._parse_attr(line)

            if attr_type == "dn":
                self._check_dn(dn, attr_value)
                dn = attr_value
            elif attr_type == "version" and dn is None:
                pass  # version = 1
            else:
                if dn is None:
                    self._error(
                        "First line of record does not start "
                        'with "dn:": %s' % attr_type
                    )
                if (
                    attr_value is not None
                    and attr_type.lower() not in self._ignored_attr_types
                ):
                    if attr_type in entry:
                        entry[attr_type].append(attr_value)
                    else:
                        entry[attr_type] = [attr_value]

        return dn, entry

    def parse(self):
        """Iterate LDIF entry records.

        :rtype: Iterator[Tuple[string, Dict]]
        :return: (dn, entry)
        """
        for block in self._iter_blocks():
            yield self._parse_entry_record(block)
