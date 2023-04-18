from clientapp import create_app
from clientapp import config as cfg
from urllib.parse import urlparse

if __name__ == '__main__':
    app = create_app()
    app.debug = True
    redirect_host_name = urlparse(cfg.REDIRECT_URIS[0]).hostname
    app.run(host=redirect_host_name, ssl_context=('cert.pem', 'key.pem'), port=9090, use_reloader=False)
