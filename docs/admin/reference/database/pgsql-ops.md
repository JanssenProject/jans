---
tags:
  - administration
  - reference
  - database
---

Connect to PostgreSql workspace: `sudo -u postgres psql` 

## Search user

* Change database: `\c jansdb`
* Search for user `testUser`: `select * from "jansPerson" where uid = 'testuser';`
  * If you want pretty output, enable display mode with `\x` 
  * Re-run search query. 

## Change password for user jans

* Changing user 'jans' password to "secret": `alter user jans with password 'secret';` 

## Add user in Group

* Get DN of target user. i.e. we are searching for DN of user 'testUser' with: `select * from "jansPerson" where uid = 'testuser';`
* Get DN of Jans Admin Group. i.e.`select * from "jansGrp";`
* Add ( actually append ) new user in `member` of this group: 
```
update "jansGrp" set member = '["inum=45eefa31-f535-4a97-b1a1-09556b7e8de8,ou=people,o=jans", "inum=9b4eedd0-7e8c-4afc-a7e3-b142ac5f7d8f,ou=people,o=jans"]';
```

## List users with specific filter 

## Modify column size of jansPerson 

## Add custom attribute

## Output column data into txt

## Dump table



## This content is in progress

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
