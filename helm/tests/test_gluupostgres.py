import pygluu.kubernetes.postgres as module0
from pygluu.kubernetes.postgres import Postgres


def test_base_exception():
    try:
        var0 = module0.Postgres()
    except BaseException:
        pass
