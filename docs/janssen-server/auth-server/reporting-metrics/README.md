---
tags:
  - administration
  - auth-server
  - reporting
  - metric
---

# Reporting and Metrics

Authorization Server (AS) supports different metric data:

- Monthly Active Users (MAU)
- Health Check
- Token Issued
- Audit Logs

## Statistic Endpoint 

Statistic data (MAU, Token Issued) is returned from `/jans-auth/restv1/internal/stat` protected endpoint.

Endpoint is protected by authorization token which must contain configurable scope (via `statAuthorizationScope` AS configuration property).
Default scope value of `statAuthorizationScope`  is `jans_stat` which means that token must contain this scope or otherwise `UNAUTHORIZED` 401 response is returned.

**Parameters**
- `month` - specify month in YYYYMM format (e.g. `January 2022` is `202201`)
- `start-month` - start month for range request
- `end-month` - end month for range request
- `format` - if no value is specified than json value is returns. Possible explicit values are `openmetrics` (open metrics format) and `jsonmonth` (all data are flattened for given month) 

**Example:** Request single month data (recommended)
```
GET /jans-auth/restv1/internal/stat?month=202101
Authorization: Bearer czZCaGRSa3F0MzpnWDFmQmF0M2JW
```

**Example:** request multiple months data
```
GET /jans-auth/restv1/internal/stat?month=202012%20202101
Authorization: Bearer czZCaGRSa3F0MzpnWDFmQmF0M2JW
```

It is also possible to request data by specifying month ranges via `start-month` and `end-month` parameters.

**Example:** request multiple months by specifying month range (all months from February 2021 till February 2022)
```
GET /jans-auth/restv1/internal/stat?start-month=202102&end-month=202202
Authorization: Bearer czZCaGRSa3F0MzpnWDFmQmF0M2JW
```

If both month range parameters (`start-month` or `end-month` ) and `month` parameter are used at the same time AS will return 400 Bad Request error.

Note that data returned by endpoint is calculated and depending on request can be **expensive** to recalculate each time.
Thus data (response) is cached during one hour and is not refreshed during this period.  

**Example:** response when `format=jsonmonth`

```
[ 
  {
    "month": "202108",
    "montly_active_users": 5,
    "token_count_per_granttype": {
       ...
    }
  },
  {
    "month": "202109",
    "montly_active_users": 7,
    "token_count_per_granttype": {
       ...
    }
  }
  ...
]
``` 

## Monthly Active Users

Server uses algorithmic [HLL](https://github.com/aggregateknowledge/java-hll) approach of tracking active users. Means it is not exact number but approximation.
MAU can be requested at `/jans-auth/restv1/internal/stat` protected endpoint and is returned per month in `YYYYMM` format for month.

Example:
10 servers with 1000000 unique users with 10M total logins. log2m=15, regwidth = 5
```
server0 - size: 20483bytes, cardinality: 996522
server1 - size: 20483bytes, cardinality: 998560
server2 - size: 20483bytes, cardinality: 1001721
server3 - size: 20483bytes, cardinality: 988345
server4 - size: 20483bytes, cardinality: 998151
server5 - size: 20483bytes, cardinality: 1005606
server6 - size: 20483bytes, cardinality: 993774
server7 - size: 20483bytes, cardinality: 992465
server8 - size: 20483bytes, cardinality: 988403
server9 - size: 20483bytes, cardinality: 1002353
UNION - size: 20483bytes, cardinality: 10010897
```

**Example:** MAU with no `format` parameter in request   
```
{
  "response": {
    "202101": {       
      "monthly_active_users": 1939497078,
      ...
    },
    "202102": {       
          "monthly_active_users": 1939497078,
          ...
    },
  }
}      
```

## Health Check

AS provides `/sys/health-check` endpoint which can be used to perform health check.

Sample reply
```json
{
  "status": "running", 
  "db_status":"online"
}
```

Sample curl
```curl
curl -k https://janssen-host-name/jans-auth/sys/health-check
```

## Token Issued

Token Issued report shows how many tokens were issued per grant type.

Sample response from `/jans-auth/restv1/internal/stat` protected endpoint.

```json
{
  "response": {
    "202101": {
      "monthly_active_users": 1939497078,
      "token_count_per_granttype": {
        "implicit": {
          "access_token": 8843443,
          "refresh_token": 361678620,
          "uma_token": 257653798,
          "id_token": 1196937417
        },
        "refresh_token": {
          "access_token": 198484847,
          "refresh_token": 527458534,
          "uma_token": 1552211159,
          "id_token": 2077968838
        },
        "password": {
          "access_token": 21293868,
          "refresh_token": 2003904704,
          "uma_token": 44144850,
          "id_token": 1713870170
        },
        "client_credentials": {
          "access_token": 1656091541,
          "refresh_token": 875245682,
          "uma_token": 1920358732,
          "id_token": 2029924857
        },
        "urn:ietf:params:oauth:grant-type:device_code": {
          "access_token": 871050693,
          "refresh_token": 735390384,
          "uma_token": 902227655,
          "id_token": 572398177
        },
        "authorization_code": {
          "access_token": 1468057290,
          "refresh_token": 1576996227,
          "uma_token": 1755565333,
          "id_token": 944346498
        },
        "urn:openid:params:grant-type:ciba": {
          "access_token": 1187334485,
          "refresh_token": 1043290537,
          "uma_token": 206572517,
          "id_token": 1850166398
        },
        "urn:ietf:params:oauth:grant-type:uma-ticket": {
          "access_token": 933970698,
          "refresh_token": 1089756841,
          "uma_token": 352343374,
          "id_token": 645686974
        }
      }
    },
    "202012": {
      "monthly_active_users": 1620607010,
      "token_count_per_granttype": {
        "implicit": {
          "access_token": 20676511,
          "refresh_token": 960308503,
          "uma_token": 703633153,
          "id_token": 573544490
        },
        "refresh_token": {
          "access_token": 526463917,
          "refresh_token": 483679318,
          "uma_token": 545888615,
          "id_token": 951286042
        },
        "password": {
          "access_token": 1883167951,
          "refresh_token": 1636169697,
          "uma_token": 1753544837,
          "id_token": 1675535757
        },
        "client_credentials": {
          "access_token": 290209545,
          "refresh_token": 1635984977,
          "uma_token": 1066472511,
          "id_token": 463520941
        },
        "urn:ietf:params:oauth:grant-type:device_code": {
          "access_token": 84124820,
          "refresh_token": 1705729386,
          "uma_token": 2070635541,
          "id_token": 970034925
        },
        "authorization_code": {
          "access_token": 387026047,
          "refresh_token": 907425179,
          "uma_token": 370523863,
          "id_token": 1807260512
        },
        "urn:openid:params:grant-type:ciba": {
          "access_token": 1408098991,
          "refresh_token": 819153423,
          "uma_token": 936295536,
          "id_token": 378210748
        },
        "urn:ietf:params:oauth:grant-type:uma-ticket": {
          "access_token": 381038617,
          "refresh_token": 828068056,
          "uma_token": 448746427,
          "id_token": 650520169
        }
      }
    }
  }
}
```

## Audit logs

AS supports audit logs. Please check [audit logs page](../logging/audit-logs.md)

It can help answer on questions like:
- successful (`USER_AUTHORIZATION` with `isSuccess=true`) vs failed authentication (`USER_AUTHORIZATION` with `isSuccess=false`) 
- client registration and update, etc. 

which can help track successful vs failed authenticatio
Success vs Failed authentications

### Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

### Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
