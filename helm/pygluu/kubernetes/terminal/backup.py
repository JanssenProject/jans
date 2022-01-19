"""
pygluu.kubernetes.terminal.backup
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for terminal backup prompt.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click


class PromptBackup:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_backup(self):
        """Prompt for LDAP and or Couchbase backup strategies
        """
        if self.settings.get("global.cnPersistenceType") in ("hybrid", "couchbase"):
            if self.settings.get("installer-settings.couchbase.backup.incrementalSchedule") in (None, ''):
                self.settings.set("installer-settings.couchbase.backup.incrementalSchedule", click.prompt(
                    "Please input couchbase backup cron job schedule for incremental backups. "
                    "This will run backup job every 30 mins by default.",
                    default="*/30 * * * *",
                ))

            if self.settings.get("installer-settings.couchbase.backup.fullSchedule") in (None, ''):
                self.settings.set("installer-settings.couchbase.backup.fullSchedule", click.prompt(
                    "Please input couchbase backup cron job schedule for full backups. "
                    "This will run backup job on Saturday at 2am",
                    default="0 2 * * 6",
                ))

            if self.settings.get("installer-settings.couchbase.backup.retentionTime") in (None, ''):
                self.settings.set("installer-settings.couchbase.backup.retentionTime", click.prompt(
                    "Please enter the time period in which to retain existing backups. "
                    "Older backups outside this time frame are deleted",
                    default="168h",
                ))

            if self.settings.get("installer-settings.couchbase.backup.storageSize") in (None, ''):
                self.settings.set("installer-settings.couchbase.backup.storageSize",
                                  click.prompt("Size of couchbase backup volume storage",
                                               default="20Gi"))

        elif self.settings.get("global.cnPersistenceType") == "ldap":
            if self.settings.get("installer-settings.ldap.backup.fullSchedule") in (None, ''):
                self.settings.set("installer-settings.ldap.backup.fullSchedule", click.prompt(
                    "Please input ldap backup cron job schedule. "
                    "This will run backup job every 30 mins by default.",
                    default="*/30 * * * *",
                ))
