FROM blazemeter/taurus:master-f96971fa-2022-12-12

# ===============
# Ubuntu packages
# ===============

RUN apt-get clean \
&& mv /var/lib/apt/lists /tmp \
&& mkdir -p /var/lib/apt/lists/partial \
&& apt-get clean

RUN apt-get update \
    &&  apt-get install openjdk-8-jre-headless wget git bash python3 python3-dev lsb-core python3-setuptools cmake build-essential apt-utils python3-pip -y

# ===============
# Environmental Variables
# ===============

ENV FQDN="https://demoexample.jans.io" \
    AUTHZ_CLIENT_ID="" \
    AUTHZ_CLIENT_SECRET="" \
    ROPC_CLIENT_ID="" \
    ROPC_CLIENT_SECRET="" \
    RUN_AUTHZ_TEST=false \
    RUN_ROPC_TEST=false \
    TEST_USERS_PREFIX_STRING="test_user" \
    THREAD_COUNT=200 \
    COUCHBASE_URL="" \
    COUCHBASE_PW="" \
    USER_NUMBER_STARTING_POINT=0 \
    USER_NUMBER_ENDING_POINT=50000000 \
    LOAD_USERS_TO_COUCHBASE=false \
    LOAD_USERS_TO_LDAP=false \
    LOAD_USERS_TO_SPANNER=false \
    LOAD_USERS_TO_RDBMS=false \
    USER_SPLIT_PARALLEL_THREADS=20 \
    GOOGLE_APPLICATION_CREDENTIALS=""\
    GOOGLE_PROJECT_ID=""\
    GOOGLE_SPANNER_INSTANCE_ID=""\
    GOOGLE_SPANNER_DATABASE_ID=""\
    LDAP_URL="opendj:1636" \
    LDAP_PW="" \
    LDAP_DN="cn=directory manager" \
    # pgsql or mysql
    RDBMS_TYPE="mysql" \
    RDBMS_DB="jans" \
    RDBMS_USER="jans" \
    RDBMS_PASSWORD="" \
    RDBMS_HOST="localhost"



# ======
# Python
# ======

COPY requirements.txt /scripts/requirements.txt
RUN pip install --no-cache-dir -U pip wheel \
    && pip3 install --no-cache-dir --default-timeout=300 -r /scripts/requirements.txt \
    && pip3 uninstall -y pip wheel

COPY scripts /scripts

RUN mkdir -p /root/.bzt/jmeter-taurus \
    && wget -q https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.5.tgz -O /scripts/apache-jmeter-5.5.tgz \
    && tar zxvf /scripts/apache-jmeter-5.5.tgz -C /root/.bzt/jmeter-taurus \
    && mv /root/.bzt/jmeter-taurus/apache-jmeter-5.5 /root/.bzt/jmeter-taurus/5.5

LABEL name="janssenproject/demo_loadtesting" \
    maintainer="Janssen Project <support@jans.io>" \
    vendor="Janssen Project" \
    version="1.0.6" \
    release="dev" \
    summary="Janssen Jmeter tests" \
    description="Janssen Jmeter and user loader image"

ENTRYPOINT ["bash", "/scripts/entrypoint.sh"]
