import os
from os import walk
path='./'
license="""/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
"""

licenseWithPackage = license + """
package """

licenseSearch = licenseWithPackage.replace('\n', '!')

for root, dirs, files in walk(path):
    for name in files:            
        with open(os.path.join(root, name),'r') as f:
            if not name.endswith(".java"):
                continue

            data = f.read()

            if data.replace('\n', '!').startswith(licenseSearch):
                continue

            if data.startswith("package "):
                data = license + "\n" + data
                with open(os.path.join(root, name),'w') as f:
                    f.write(data)
                continue

            if False:
                data = license + "\n" + "!!!!!!!!!!" + "\n" + data
                with open(os.path.join(root, name),'w') as f:
                    f.write(data)
#            continue

#            print(data)

            print(os.path.join(root, name))

#            data=data.lower()

#            print(data)