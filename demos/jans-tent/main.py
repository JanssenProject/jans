from clientapp import create_app

if __name__ == '__main__':
    app = create_app()
    app.debug = True
    app.run(host='0.0.0.0', ssl_context=('cert.pem', 'key.pem'), port=9090, use_reloader=False)
