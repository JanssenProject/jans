---
tags:
  - administration
  - auth-server
  - logging
---

# log4j2

AS under the hood is using `log4j2` and `slf4j`.
AS is configured by [log4j2.xml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/resources/log4j2.xml)

Main structure of which defines [standard logs](standard-logs.md) and which can be overwritten by specifying own custom [log4j2.xml](custom-logs.md).

Please reference [log4j configuration](https://logging.apache.org/log4j/2.x/manual/configuration.html) for available logging options.


Sample
```xml
<?xml version="1.0" encoding="UTF-8"?>

<Configuration packages="io.jans.log">
	<Appenders>
		<Console name="STDOUT" target="SYSTEM_OUT">
			<PatternLayout pattern="%d %-5p [%t] [%C{6}] (%F:%L) - %m%n" />
		</Console>

		<RollingFile name="JANS_AUTH_HTTP_REQUEST_RESPONSE_FILE" fileName="${sys:log.base}/logs/http_request_response.log" filePattern="${sys:log.base}/logs/http_request_response-%d{yyyy-MM-dd}-%i.log">

			<PatternLayout pattern="%d %-5p [%t] [%C{6}] (%F:%L) - %m%n" />

			<Policies>
				<TimeBasedTriggeringPolicy interval="1" modulate="true" />
				<SizeBasedTriggeringPolicy size="250 MB" />
			</Policies>
			<DefaultRolloverStrategy max="30" />
		</RollingFile>

		<RollingFile name="FILE" fileName="${sys:log.base}/logs/jans-auth.log" filePattern="${sys:log.base}/logs/jans-auth-%d{yyyy-MM-dd}-%i.log">

			<PatternLayout pattern="%d %-5p [%t] %X{X-Correlation-Id} [%C{6}] (%F:%L) - %m%n" />

			<Policies>
				<TimeBasedTriggeringPolicy interval="1" modulate="true" />
				<SizeBasedTriggeringPolicy size="250 MB" />
			</Policies>
			<DefaultRolloverStrategy max="30" />
		</RollingFile>

		<RollingFile name="JANS_AUTH_PERSISTENCE_FILE" fileName="${sys:log.base}/logs/jans-auth_persistence.log" filePattern="${sys:log.base}/logs/jans-auth_persistence-%d{yyyy-MM-dd}-%i.log">

			<PatternLayout pattern="%d %-5p [%t] %X{X-Correlation-Id} [%C{6}] (%F:%L) - %m%n" />

			<Policies>
				<TimeBasedTriggeringPolicy interval="1" modulate="true" />
				<SizeBasedTriggeringPolicy size="250 MB" />
			</Policies>
			<DefaultRolloverStrategy max="30" />
		</RollingFile>

		<RollingFile name="JANS_AUTH_PERSISTENCE_DURATION_FILE" fileName="${sys:log.base}/logs/jans-auth_persistence_duration.log" filePattern="${sys:log.base}/logs/jans-auth_persistence_duration-%d{yyyy-MM-dd}-%i.log">

			<PatternLayout pattern="%d %-5p [%t] %X{X-Correlation-Id} [%C{6}] (%F:%L) - %m%n" />

			<Policies>
				<TimeBasedTriggeringPolicy interval="1" modulate="true" />
				<SizeBasedTriggeringPolicy size="250 MB" />
			</Policies>
			<DefaultRolloverStrategy max="30" />
		</RollingFile>

	

		<RollingFile name="JANS_AUTH_SCRIPT_LOG_FILE" fileName="${sys:log.base}/logs/jans-auth_script.log" filePattern="${sys:log.base}/logs/jans-auth_script-%d{yyyy-MM-dd}-%i.log">

			<PatternLayout pattern="%d %-5p [%t] %X{X-Correlation-Id} [%C{6}] (%F:%L) - %m%n" />

			<Policies>
				<TimeBasedTriggeringPolicy interval="1" modulate="true" />
				<SizeBasedTriggeringPolicy size="250 MB" />
			</Policies>
			<DefaultRolloverStrategy max="30" />
		</RollingFile>

		<RollingFile name="JANS_AUTH_AUDIT_LOG_FILE" fileName="${sys:log.base}/logs/jans-auth_audit.log" filePattern="${sys:log.base}/logs/jans-auth_audit-%d{yyyy-MM-dd}-%i.log">

			<PatternLayout pattern="%d %-5p [%macAddr] [%t] %X{X-Correlation-Id} [%C{6}] (%F:%L) - %m%n" />

			<Policies>
				<TimeBasedTriggeringPolicy interval="1" modulate="true" />
				<SizeBasedTriggeringPolicy size="250 MB" />
			</Policies>
			<DefaultRolloverStrategy max="30" />
		</RollingFile>
	</Appenders>

	<Loggers>
		<!-- ############### Gluu ################# -->
		<Logger name="io.jans" level="${log4j.default.log.level}" />

		<!-- EMB-6, JMS activation throws an error due to deployment ordering, but as there is a timeout and retry the tests pass. Hide the error message -->
		<Logger name="jboss.resource.adapter.jms.inflow.JmsActivation" level="error" />

	    <Logger name="com.ocpsoft" level="info" />

		<!-- ############### Hibernate logging ################# -->
		<Logger name="org.hibernate" level="error" />

		<Logger name="io.jans.as.server.audit.debug" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_HTTP_REQUEST_RESPONSE_FILE" />
		</Logger>

		<Logger name="io.jans.orm" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_PERSISTENCE_FILE" />
		</Logger>

		
		<logger name="com.couchbase.client" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_PERSISTENCE_FILE" />
		</logger>

	

		<Logger name="io.jans.orm.couchbase.operation.watch" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_PERSISTENCE_DURATION_FILE" />
		</Logger>

		<Logger name="io.jans.orm.watch" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_PERSISTENCE_DURATION_FILE" />
		</Logger>



		<Logger name="io.jans.service.PythonService" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_SCRIPT_LOG_FILE" />
		</Logger>

		<Logger name="io.jans.service.custom.script" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_SCRIPT_LOG_FILE" />
		</Logger>

		<Logger name="io.jans.as.server.service.custom" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_SCRIPT_LOG_FILE" />
		</Logger>

		<Logger name="io.jans.agama.engine.script.LogUtils" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_SCRIPT_LOG_FILE" />
		</Logger>

		<Logger name="io.jans.as.server.audit.ApplicationAuditLogger" level="${log4j.default.log.level}" additivity="false">
			<AppenderRef ref="JANS_AUTH_AUDIT_LOG_FILE" />
		</Logger>

		<Root level="info">
			<AppenderRef ref="FILE" />
			<AppenderRef ref="STDOUT" />
		</Root>
	</Loggers>
</Configuration>

```


## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).