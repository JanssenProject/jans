
import os
import hashlib

from enum import Enum
from Crypto.Cipher import AES

class AESKeyLength(Enum):
    KL128 = 128
    KL192 = 192
    KL256 = 256

AES_ECB_P = 'AES/ECB/PKCS5Padding'
AES_CBC_P = 'AES/CBC/PKCS5Padding'
AES_GCM_NP = 'AES/GCM/NoPadding'

class AESCipher(object):

    def __init__(self, mode, key_len, passw, salt):
        if  isinstance (mode, int) != True:
            raise AttributeError("mode isn't int type")
        if  isinstance (key_len, AESKeyLength) != True:
            raise AttributeError("key_len isn't AESKeyLength type")
        self.mode = mode
        self.key_len = key_len
        if salt is None:
            self.key = hashlib.sha256(passw.encode()).digest()
        else:
            self.key = hashlib.pbkdf2_hmac('sha512', passw.encode(), salt.encode(), 1000, int(self.key_len.value/8))

    def encrypt(self, raw):
        if self.mode == AES.MODE_CBC:
            iv = os.urandom(16)
            self.cipher = AES.new(self.key, self.mode, iv)
            raw = self._pad(raw)
            enc_raw = self.cipher.encrypt(raw.encode())
            return (iv + enc_raw)
        elif self.mode == AES.MODE_GCM:
            nonce = os.urandom(16)
            self.cipher = AES.new(self.key, self.mode, nonce=nonce, mac_len=16)
            enc_raw = self.cipher.encrypt(raw.encode())
            tag = self.cipher.digest()
            return (nonce + enc_raw + tag)
        elif self.mode == AES.MODE_ECB:
            self.cipher = AES.new(self.key, self.mode)
            raw = self._pad(raw)
            return self.cipher.encrypt(raw.encode())
        else:
            raise AttributeError("mode is not supported: mode = " + self.mode)

    def decrypt(self, enc):
        if self.mode == AES.MODE_CBC:
            iv = enc[0:16]
            self.cipher = AES.new(self.key, self.mode, iv)
            return self._unpad(self.cipher.decrypt(enc[16:]))
        elif self.mode == AES.MODE_GCM:
            nonce = enc[0:16]
            self.cipher = AES.new(self.key, self.mode, nonce=nonce, mac_len=16)
            tag = enc[ len(enc)-16 : ]
            enc_text = enc [16 : len(enc)-16]
            dec_text = self.cipher.decrypt(enc_text)
            self.cipher.verify(tag)
            return dec_text
        elif self.mode == AES.MODE_ECB:
            self.cipher = AES.new(self.key, self.mode)
            return self._unpad(self.cipher.decrypt(enc))
        else:
            raise AttributeError("mode is not supported: mode = " + self.mode)

    def _pad(self, s):
        bs = AES.block_size
        return s + (bs - len(s) % bs) * chr(bs - len(s) % bs)

    @staticmethod
    def _unpad(s):
        return s[:-ord(s[len(s)-1:])]
