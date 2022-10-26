# ========
# k8s meta
# ========

def test_gk8s_meta_client(gk8s_meta):
    assert gk8s_meta.client is not None
