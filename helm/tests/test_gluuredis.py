import pygluu.kubernetes.redis as module0
from pygluu.kubernetes.redis import Redis


def test_base_exception():
    try:
        var0 = module0.Redis()
    except BaseException:
        pass
