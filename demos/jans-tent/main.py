'''
Project: Test Auth Client
Author: Christian Hawk
Copyright 2023 Christian Hawk

Licensed under the Apache License, Version 2.0 (the 'License');
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''
from clientapp import create_app
# import logging

# log = logging.getLogger(__name__)
# log.info('TESTINGl')

if __name__ == '__main__':
    app = create_app()
    app.debug = True
    app.run(host='0.0.0.0', ssl_context=('cert.pem', 'key.pem'), port=9090)
