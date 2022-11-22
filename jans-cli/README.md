# _Janssen Command Line Interface_
`jans-cli` is a **Command Line Interface** for Janssen Configuration. It also has `menu-driven` interface that makes it easier to understand how to use [Janssen Server](https://github.com/JanssenProject/home) through the Interactive Mode.

Table of Contents
=================

   * [<em>Janssen Command Line Interface</em>](#_janssen-command-line-interface_)
   * [<em>Installation</em>](#_installation_)
   * [<em>Quick Start</em>](#_quick-start_)

# _Installation_

You can directly download the `jans-cli` package file as below:

### For macOs:

```
wget https://github.com/JanssenProject/jans-cli/releases/latest/download/jans-cli-macos-amd64.pyz
```

### for linux:

```
wget https://github.com/JanssenProject/jans-cli/releases/latest/download/jans-cli-linux-amd64.pyz
```

## Build `jans-cli.pyz` manually

If you would like to build `jans-cli` manually, you can go through the following steps noted here:

## Prerequisites
1. wget
2. unzip
3. Python 3.6+.
4. Python `pip3` package.

### Building 

1. Install dependencies

   * On Ubuntu 20
    ```sh
    apt install -y wget unzip python3-pip python3-dev
    ```
   * On CentOS Stream 8
   ```sh
   yum install -y  wget unzip python3-pip python3-devel make 
   pip3 install --upgrade pip
   ```
   
   Install Shiv
    
    ```sh
    pip3 install shiv
    ```

2. Download the repository:

    ```sh
    wget https://github.com/JanssenProject/jans/archive/refs/heads/main.zip
    ```

3. Unzip package, and change to directory

    ```sh
    unzip main.zip
    cd jans-main/jans-cli
    ```

4. Build

    ```sh
    make zipapp
    ```

    You can verify with the following command line if everything is done successfully.

    ```
    python3 config-cli.pyz -h
    ```

5. Executing config-cli.pyz Remotely
  Login your Jans Server. Execute the following command to find **client-id** and **client-secret**:
    ```sh
    cat /opt/jans/jans-setup/setup.properties.last | grep "role_based_client"
    ```
    It will output like this:
    ```sh
    role_based_client_encoded_pw=+U3XiW2uM/rnidqZ2mv9sw\=\=
    role_based_client_id=2000.09b47f56-1b9e-4443-bebd-bdf970406a15
    role_based_client_pw=T68kLUz4YXnR
    ```
    **client-id** is the value of **role_based_client_id** and **client-secret** is the value of **role_based_client_pw**
    Thus we can execute CLI as:
    
    ```sh
    python3 config-cli.pyz --host demoexmple.gluu.org --client-id 2000.09b47f56-1b9e-4443-bebd-bdf970406a15 --client-secret T68kLUz4YXnR
    ```


### Standard Python package
1. Install venv module
    ```sh
    pip3 install virtualenv
    ```

2. Create virtual environment and activate:

    ```sh
    python3 -m virtualenv .venv
    source .venv/bin/activate
    ```

3. Download and install the package:

    ```
    wget https://github.com/JanssenProject/jans/archive/refs/heads/main.zip
    unzip main.zip
    cd jans-main/jans-cli
    make install
    ```

    This command will install executable called `jans-cli` available in virtual environment `PATH`.


![](../docs/assets/image-build-jans-cli-pyz-manually-03042021.png)


## Virtual Machine Setup

**jans-cli** is automatically installed if you choose `jans-config-api` during [Janssen Server](https://github.com/JanssenProject/home/blob/main/development.md#install-janssen-into-vm) Installation on Virtual Machine. 

![](../docs/assets/image-jans-config-api-03042021.png)

After successfully installed Janssen Server, you will get two command-line arguments as below:

![](../docs/assets/image-installed-03042021.png)

# _Quick Start_

As you have seen, CLI supports both of the `config-cli` and `scim-cli`. For a quick start, let's run the following command.

```
/opt/jans/jans-cli/config-cli.py
```
If you get an error, you can try in this way:

```
python3 /opt/jans/jans-cli/config-cli.py
```

Alternatively, you can make python3 to default version:
```
sudo update-alternatives --install /usr/bin/python python /usr/bin/python3 10
/opt/jans/jans-cli/config-cli.py
```

You will get a menu as below image:

![main-menu.png](../docs/assets/image-im-main-03042021.png)

From the following list, you can choose any options by selecting its number. For example, let's say number 2,
to get **Default Authentication Method**.

That returns another two options as below:

![option-2-option.png](../docs/assets/image-im-default-auth-02-03042021.png)

Now by selecting 1 it returns our desired result as below image:

![default-authentication-method.png](../docs/assets/image-im-cur-default-auth-03042021.png)

So, That was a quick start to view how this _jans-cli_ Interactive Mode works. Please, follow this [link](https://github.com/JanssenProject/jans/blob/main/docs/admin/config-guide/jans-cli/index.md) to read the _jans-cli_ docs for a better understanding of the Janssen Command-Line.
