import pytest


# =========
# base meta
# =========


def test_meta_get_containers(gmeta):
    with pytest.raises(NotImplementedError) as exc:
        gmeta.get_containers("foo")
    assert "" in str(exc.value)


def test_meta_get_container_ip(gmeta):
    with pytest.raises(NotImplementedError) as exc:
        gmeta.get_container_ip("foo")
    assert "" in str(exc.value)


def test_meta_get_container_name(gmeta):
    with pytest.raises(NotImplementedError) as exc:
        gmeta.get_container_name("foo")
    assert "" in str(exc.value)


def test_meta_copy_to_container(gmeta):
    with pytest.raises(NotImplementedError) as exc:
        gmeta.copy_to_container("foo", "/tmp/foo")
    assert "" in str(exc.value)


def test_meta_exec_cmd(gmeta):
    with pytest.raises(NotImplementedError) as exc:
        gmeta.exec_cmd("foo", "a command")
    assert "" in str(exc.value)


# ========
# k8s meta
# ========

def test_gk8s_meta_client(gk8s_meta):
    assert gk8s_meta.client is not None
