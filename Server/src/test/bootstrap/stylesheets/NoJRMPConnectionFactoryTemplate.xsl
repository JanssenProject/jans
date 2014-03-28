<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="xml" indent="yes"/>

  <!--top level template converts to top level "server" tag-->
  <xsl:template match="datasources|connection-factories"><!--|server|service"-->

    <server>

      <xsl:apply-templates/>

    </server>

  </xsl:template>


  <!-- template for generic resource adapters supporting transactions -->
  <xsl:template match="tx-connection-factory">

    <mbean code="org.jboss.resource.connectionmanager.TxConnectionManager" 
           name="jboss.jca:service=TxCM,name={jndi-name}" 
           display-name="ConnectionManager for ConnectionFactory {jndi-name}">

      <xsl:choose>
        <xsl:when test="(xa-transaction) and (track-connection-by-tx)">
          <attribute name="TrackConnectionByTx">true</attribute>
          <attribute name="LocalTransactions">false</attribute>
        </xsl:when>
        <xsl:when test="(xa-transaction)">
          <attribute name="TrackConnectionByTx">false</attribute>
          <attribute name="LocalTransactions">false</attribute>
        </xsl:when>
        <xsl:otherwise>
          <attribute name="TrackConnectionByTx">true</attribute>
          <attribute name="LocalTransactions">true</attribute>
        </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
        <xsl:when test="wrap-xa-resource">
          <attribute name="WrapXAResource"><xsl:value-of select="normalize-space(wrap-xa-resource)"/></attribute>
        </xsl:when>
        <xsl:otherwise>
          <attribute name="WrapXAResource">false</attribute>
        </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
        <xsl:when test="pad-xid">
          <attribute name="PadXid"><xsl:value-of select="normalize-space(pad-xid)"/></attribute>
        </xsl:when>
        <xsl:otherwise>
          <attribute name="PadXid">false</attribute>
        </xsl:otherwise>
      </xsl:choose>      
      
      <xsl:if test="isSameRM-override-value">
        <config-property name="IsSameRMOverrideValue" type="java.lang.Boolean"><xsl:value-of select="normalize-space(isSameRM-override-value)"/></config-property>
      </xsl:if>
                  
      <xsl:if test="xa-resource-timeout">
         <attribute name="XAResourceTransactionTimeout"><xsl:value-of select="normalize-space(xa-resource-timeout)"/></attribute>
      </xsl:if>

      <xsl:call-template name="pool">
        <xsl:with-param name="mcf-template">generic-mcf</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="cm-common"/>
      <xsl:call-template name="tx-manager"/>

    </mbean>
    <xsl:call-template name="cf-binding">
       <xsl:with-param name="cm-name">jboss.jca:service=TxCM</xsl:with-param>
    </xsl:call-template>

    <xsl:call-template name="type-mapping">
       <xsl:with-param name="datasource" select="."/>
    </xsl:call-template>
  </xsl:template>


  <!--template for generic resource adapters that do not support transactions-->
  <xsl:template match="no-tx-connection-factory">

    <mbean code="org.jboss.resource.connectionmanager.NoTxConnectionManager" 
           name="jboss.jca:service=NoTxCM,name={jndi-name}" 
           display-name="ConnectionManager for ConnectionFactory {jndi-name}">

      <xsl:call-template name="pool">
        <xsl:with-param name="mcf-template">generic-mcf</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="cm-common"/>
    </mbean>
    <xsl:call-template name="cf-binding">
       <xsl:with-param name="cm-name">jboss.jca:service=NoTxCM</xsl:with-param>
    </xsl:call-template>

    <xsl:call-template name="type-mapping">
       <xsl:with-param name="datasource" select="."/>
    </xsl:call-template>
  </xsl:template>


  <!-- Template for our jca-jdbc non-XADatasource (local) wrapper, using local transactions. -->
  <xsl:template match="local-tx-datasource">

    <mbean code="org.jboss.resource.connectionmanager.TxConnectionManager" 
           name="jboss.jca:service=LocalTxCM,name={jndi-name}" 
           display-name="ConnectionManager for DataSource {jndi-name}">

      <attribute name="TrackConnectionByTx">true</attribute>
      <attribute name="LocalTransactions">true</attribute>

      <xsl:call-template name="pool">
        <xsl:with-param name="mcf-template">local-wrapper</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="cm-common"/>
      <xsl:call-template name="tx-manager"/>

    </mbean>
    <xsl:call-template name="cf-binding">
       <xsl:with-param name="cm-name">jboss.jca:service=LocalTxCM</xsl:with-param>
    </xsl:call-template>

    <xsl:call-template name="type-mapping">
       <xsl:with-param name="datasource" select="."/>
    </xsl:call-template>
  </xsl:template>

  <!-- Template for our jca-jdbc non-XADatasource (local) wrapper, using no transactions. -->
  <xsl:template match="no-tx-datasource">

    <mbean code="org.jboss.resource.connectionmanager.NoTxConnectionManager" 
           name="jboss.jca:service=NoTxCM,name={jndi-name}"
           display-name="ConnectionManager for DataSource {jndi-name}">

      <xsl:call-template name="pool">
        <xsl:with-param name="mcf-template">local-wrapper</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="cm-common"/>
    </mbean>
    <xsl:call-template name="cf-binding">
       <xsl:with-param name="cm-name">jboss.jca:service=NoTxCM</xsl:with-param>
    </xsl:call-template>

    <xsl:call-template name="type-mapping">
       <xsl:with-param name="datasource" select="."/>
    </xsl:call-template>
  </xsl:template>

  <!-- Template for our jca-jdbc XADatasource wrapper. -->
  <xsl:template match="xa-datasource">

    <mbean code="org.jboss.resource.connectionmanager.TxConnectionManager" 
           name="jboss.jca:service=XATxCM,name={jndi-name}"
           display-name="ConnectionManager for DataSource {jndi-name}">

      <xsl:choose>
        <xsl:when test="track-connection-by-tx">
          <attribute name="TrackConnectionByTx">true</attribute>
          <attribute name="LocalTransactions">false</attribute>
        </xsl:when>
        <xsl:otherwise>
          <attribute name="TrackConnectionByTx">false</attribute>
          <attribute name="LocalTransactions">false</attribute>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:if test="isSameRM-override-value">
        <config-property name="IsSameRMOverrideValue" type="java.lang.Boolean"><xsl:value-of select="normalize-space(isSameRM-override-value)"/></config-property>
      </xsl:if>
      
      <xsl:choose>
        <xsl:when test="wrap-xa-resource">
          <attribute name="WrapXAResource"><xsl:value-of select="normalize-space(wrap-xa-resource)"/></attribute>
        </xsl:when>
        <xsl:otherwise>
          <attribute name="WrapXAResource">false</attribute>
        </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
        <xsl:when test="pad-xid">
          <attribute name="PadXid"><xsl:value-of select="normalize-space(pad-xid)"/></attribute>
        </xsl:when>
        <xsl:otherwise>
          <attribute name="PadXid">false</attribute>
        </xsl:otherwise>
      </xsl:choose>      
      
      
      <xsl:if test="xa-resource-timeout">
         <attribute name="XAResourceTransactionTimeout"><xsl:value-of select="normalize-space(xa-resource-timeout)"/></attribute>
      </xsl:if>

      <xsl:call-template name="pool">
        <xsl:with-param name="mcf-template">xa-wrapper</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="cm-common"/>
      <xsl:call-template name="tx-manager"/>

    </mbean>
    <xsl:call-template name="cf-binding">
       <xsl:with-param name="cm-name">jboss.jca:service=XATxCM</xsl:with-param>
    </xsl:call-template>

    <xsl:call-template name="type-mapping">
       <xsl:with-param name="datasource" select="."/>
    </xsl:call-template>
  </xsl:template>

  <!-- template to generate a property file format from a set of               -->
  <!-- <xa-datasource-property name="blah">blah-value</xa-datasource-property> -->
  <!-- or                                                                      -->
  <!-- <connection-property name="foo">bar</connection-property>               -->
  <!-- tags. The newline in the xsl:text element is crucial!                   -->
  <!-- this makes a property file format, not the ; delimited format-->
  <xsl:template match="xa-datasource-property|connection-property">
    <xsl:value-of select="@name"/>=<xsl:value-of select="normalize-space(.)"/><xsl:text>
</xsl:text>
  </xsl:template>

  <!-- template to generate the ManagedConnectionFactory mbean for a generic jca adapter -->
  <xsl:template name="generic-mcf">
      <depends optional-attribute-name="ManagedConnectionFactoryName">
      <!--embedded mbean-->
        <mbean code="org.jboss.resource.connectionmanager.RARDeployment" name="jboss.jca:service=ManagedConnectionFactory,name={jndi-name}" display-name="ManagedConnectionFactory for ConnectionFactory {jndi-name}">

          <xsl:apply-templates select="depends" mode="anonymous"/>
          <attribute name="ManagedConnectionFactoryProperties">
            <properties>

              <!--we need the other standard properties here-->
              <xsl:if test="user-name">
                <config-property name="UserName" type="java.lang.String"><xsl:value-of select="normalize-space(user-name)"/></config-property>
              </xsl:if>
              <xsl:if test="password">
                <config-property name="Password" type="java.lang.String"><xsl:value-of select="normalize-space(password)"/></config-property>
              </xsl:if>
              <xsl:apply-templates select="config-property"/>
            </properties>
          </attribute>

          <attribute name="RARName"><xsl:value-of select="rar-name"/></attribute>
          <attribute name="ConnectionDefinition"><xsl:value-of select="connection-definition"/></attribute>

          <depends optional-attribute-name="OldRarDeployment">jboss.jca:service=RARDeployment,name='<xsl:value-of select="rar-name"/>'</depends>

        </mbean>
      </depends>
  </xsl:template>

  <!-- template to copy config-property elements.  This actually does a literal copy -->
  <!-- Please keep this for consistency with the jb4 version which does not do a literal copy -->
  <xsl:template match="config-property">
    <config-property name="{@name}" type="{@type}"><xsl:apply-templates/></config-property>
  </xsl:template>

  <!-- template to generate the ManagedConnectionFactory mbean for our jca-jdbc local wrapper -->
  <xsl:template name="local-wrapper">

      <depends optional-attribute-name="ManagedConnectionFactoryName">
      <!--embedded mbean-->
        <mbean code="org.jboss.resource.connectionmanager.RARDeployment" name="jboss.jca:service=ManagedConnectionFactory,name={jndi-name}" display-name="ManagedConnectionFactory for DataSource {jndi-name}">

          <xsl:apply-templates select="depends" mode="anonymous"/>

          <depends optional-attribute-name="OldRarDeployment">jboss.jca:service=RARDeployment,name='jboss-local-jdbc.rar'</depends>
          <attribute name="RARName"><xsl:value-of select="jboss-local-jdbc.rar"/></attribute>
          <attribute name="ConnectionDefinition">javax.sql.DataSource</attribute>

          <attribute name="ManagedConnectionFactoryProperties">
            <properties>
              <config-property name="ConnectionURL" type="java.lang.String"><xsl:value-of select="normalize-space(connection-url)"/></config-property>
              <config-property name="DriverClass" type="java.lang.String"><xsl:value-of select="normalize-space(driver-class)"/></config-property>

              <xsl:call-template name="wrapper-common-properties"/>
              <xsl:if test="connection-property">
                <config-property name="ConnectionProperties" type="java.lang.String">
                  <xsl:apply-templates select="connection-property"/>
                </config-property>
              </xsl:if>

            </properties>
          </attribute>
        </mbean>
      </depends>
  </xsl:template>

  <xsl:template name="xa-wrapper">
     <depends optional-attribute-name="ManagedConnectionFactoryName">
        <!--embedded mbean-->
        <mbean code="org.jboss.resource.connectionmanager.RARDeployment"
              name="jboss.jca:service=ManagedConnectionFactory,name={jndi-name}"
              displayname="ManagedConnectionFactory for DataSource {jndi-name}">
          
          <xsl:apply-templates select="depends" mode="anonymous"/>

          <depends optional-attribute-name="OldRarDeployment">jboss.jca:service=RARDeployment,name='jboss-xa-jdbc.rar'</depends>
          <attribute name="RARName"><xsl:value-of select="jboss-xa-jdbc.rar"/></attribute>
          <attribute name="ConnectionDefinition">javax.sql.DataSource</attribute>

          <attribute name="ManagedConnectionFactoryProperties">
            <properties>
              <config-property name="XADataSourceClass" type="java.lang.String"><xsl:value-of select="normalize-space(xa-datasource-class)"/></config-property>

              <config-property name="XADataSourceProperties" type="java.lang.String">
                <xsl:apply-templates select="xa-datasource-property"/>
              </config-property>
              
              <!-- remove for new XA handling
              <xsl:if test="isSameRM-override-value">
                <config-property name="IsSameRMOverrideValue" type="java.lang.Boolean"><xsl:value-of select="normalize-space(isSameRM-override-value)"/></config-property>
              </xsl:if>
              -->
              <xsl:call-template name="wrapper-common-properties"/>

            </properties>
          </attribute>
        </mbean>
      </depends>
  </xsl:template>

  <!-- template for the ManagedConnectionFactory properties shared between our local and xa wrappers -->
  <xsl:template name="wrapper-common-properties">

          <xsl:if test="transaction-isolation">
            <config-property name="TransactionIsolation" type="java.lang.String"><xsl:value-of select="normalize-space(transaction-isolation)"/></config-property>
          </xsl:if>
          <xsl:if test="user-name">
            <config-property name="UserName" type="java.lang.String"><xsl:value-of select="normalize-space(user-name)"/></config-property>
          </xsl:if>
          <xsl:if test="password">
            <config-property name="Password" type="java.lang.String"><xsl:value-of select="normalize-space(password)"/></config-property>
          </xsl:if>
          <xsl:if test="new-connection-sql">
            <config-property name="NewConnectionSQL" type="java.lang.String"><xsl:value-of select="normalize-space(new-connection-sql)"/></config-property>
          </xsl:if>
          <xsl:if test="check-valid-connection-sql">
            <config-property name="CheckValidConnectionSQL" type="java.lang.String"><xsl:value-of select="normalize-space(check-valid-connection-sql)"/></config-property>
          </xsl:if>
          <xsl:if test="valid-connection-checker-class-name">
            <config-property name="ValidConnectionCheckerClassName" type="java.lang.String"><xsl:value-of select="normalize-space(valid-connection-checker-class-name)"/></config-property>
          </xsl:if>
          <xsl:if test="exception-sorter-class-name">
            <config-property name="ExceptionSorterClassName" type="java.lang.String"><xsl:value-of select="normalize-space(exception-sorter-class-name)"/></config-property>
          </xsl:if>
          <xsl:if test="stale-connection-checker-class-name">
          <config-property name="StaleConnectionCheckerClassName" type="java.lang.String"><xsl:value-of select="normalize-space(stale-connection-checker-class-name)"/></config-property>
          </xsl:if>
          <xsl:if test="track-statements">
            <config-property name="TrackStatements" type="java.lang.String"><xsl:value-of select="normalize-space(track-statements)"/></config-property>
          </xsl:if>
          <xsl:if test="prepared-statement-cache-size">
            <config-property name="PreparedStatementCacheSize" type="int"><xsl:value-of select="normalize-space(prepared-statement-cache-size)"/></config-property>
          </xsl:if>
          <xsl:if test="share-prepared-statements">
            <config-property name="SharePreparedStatements" type="boolean"><xsl:value-of select="normalize-space(share-prepared-statements)"/></config-property>
          </xsl:if>
          <xsl:if test="set-tx-query-timeout">
            <config-property name="TransactionQueryTimeout" type="boolean">true</config-property>
          </xsl:if>
          <xsl:if test="query-timeout">
            <config-property name="QueryTimeout" type="int"><xsl:value-of select="normalize-space(query-timeout)"/></config-property>
          </xsl:if>
          <xsl:if test="url-delimeter">
            <config-property name="URLDelimeter" type="java.lang.String"><xsl:value-of select="normalize-space(url-delimeter)"/></config-property>
          </xsl:if>
          <!-- new matching/background validation-->
          <xsl:choose>
            <xsl:when test="validate-on-match">
              <config-property name="ValidateOnMatch" type="boolean"><xsl:value-of select="normalize-space(validate-on-match)"/></config-property>
            </xsl:when>
            <xsl:otherwise>
              <config-property name="ValidateOnMatch" type="boolean">true</config-property>
            </xsl:otherwise>
          </xsl:choose>    
  </xsl:template>

  <!-- template to generate the pool mbean -->
  <xsl:template name="pool">
      <xsl:param name="mcf-template">generic-mcf</xsl:param>
      <depends optional-attribute-name="ManagedConnectionPool">

        <!--embedded mbean-->
        <mbean code="org.jboss.resource.connectionmanager.JBossManagedConnectionPool" name="jboss.jca:service=ManagedConnectionPool,name={jndi-name}" display-name="Connection Pool for DataSource {jndi-name}">
          <xsl:choose>
            <xsl:when test="$mcf-template='generic-mcf'">
              <xsl:call-template name="generic-mcf"/>
            </xsl:when>
             <xsl:when test="$mcf-template='local-wrapper'">
               <xsl:call-template name="local-wrapper"/>
             </xsl:when>
             <xsl:when test="$mcf-template='ha-local-wrapper'">
               <xsl:call-template name="ha-local-wrapper"/>
             </xsl:when>
             <xsl:when test="$mcf-template='ha-xa-wrapper'">
               <xsl:call-template name="ha-xa-wrapper"/>
             </xsl:when>
            <xsl:when test="$mcf-template='xa-wrapper'">
              <xsl:call-template name="xa-wrapper"/>
            </xsl:when>
          </xsl:choose>
          
          <attribute name="PoolJndiName"><xsl:value-of select="jndi-name"/></attribute>
          
          <xsl:choose>
            <xsl:when test="min-pool-size">
              <attribute name="MinSize"><xsl:value-of select="min-pool-size"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="MinSize">0</attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="max-pool-size">
              <attribute name="MaxSize"><xsl:value-of select="max-pool-size"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="MaxSize">20</attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="blocking-timeout-millis">
              <attribute name="BlockingTimeoutMillis"><xsl:value-of select="blocking-timeout-millis"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="BlockingTimeoutMillis">30000</attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="idle-timeout-minutes">
              <attribute name="IdleTimeoutMinutes"><xsl:value-of select="idle-timeout-minutes"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="IdleTimeoutMinutes">15</attribute>
            </xsl:otherwise>
          </xsl:choose>
          <!-- background validation -->
          <xsl:choose>            
            <xsl:when test="background-validation">
              <attribute name="BackGroundValidation"><xsl:value-of select="background-validation"/></attribute>
            </xsl:when>            
            <xsl:otherwise>
              <attribute name="BackGroundValidation">False</attribute>              
            </xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="background-validation-minutes">
              <attribute name="BackGroundValidationMinutes"><xsl:value-of select="background-validation-minutes"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="BackGroundValidationMinutes">10</attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="prefill">
              <attribute name="PreFill"><xsl:value-of select="prefill"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="PreFill">False</attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="strict-min">
              <attribute name="StrictMin"><xsl:value-of select="strict-min"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="StrictMin">False</attribute>
            </xsl:otherwise>
          </xsl:choose>
          
          
          <xsl:choose>            
            <xsl:when test="statistics-formatter">
              <attribute name="StatisticsFormatter"><xsl:value-of select="statistics-formatter"/></attribute>
            </xsl:when>
            <xsl:otherwise>
              <attribute name="StatisticsFormatter">org.jboss.resource.statistic.pool.JBossDefaultSubPoolStatisticFormatter</attribute>
            </xsl:otherwise>            
          </xsl:choose>
          
          <!--
		criteria indicates if Subject (from security domain) or app supplied
            parameters (such as from getConnection(user, pw)) are used to distinguish
            connections in the pool. Choices are 
            ByContainerAndApplication (use both), 
            ByContainer (use Subject),
            ByApplication (use app supplied params only),
            ByNothing (all connections are equivalent, usually if adapter supports
              reauthentication)-->
          <attribute name="Criteria">
	    <xsl:choose>
              <xsl:when test="application-managed-security">ByApplication</xsl:when>
              <xsl:when test="security-domain-and-application">ByContainerAndApplication</xsl:when>
              <xsl:when test="security-domain">ByContainer</xsl:when>
              <xsl:otherwise>ByNothing</xsl:otherwise>
            </xsl:choose>
          </attribute>
         <xsl:choose>
           <xsl:when test="no-tx-separate-pools">
             <attribute name="NoTxSeparatePools">true</attribute>
           </xsl:when>
         </xsl:choose>
        </mbean>
      </depends>
  </xsl:template>


  <!-- template for ConnectionManager attributes shared among all ConnectionManagers.-->
  <xsl:template name="cm-common">

      <attribute name="JndiName"><xsl:value-of select="jndi-name"/></attribute>
      <depends optional-attribute-name="CachedConnectionManager">jboss.jca:service=CachedConnectionManager</depends>

      <xsl:if test="security-domain|security-domain-and-application">
        <attribute name="SecurityDomainJndiName"><xsl:value-of select="security-domain|security-domain-and-application"/></attribute>
        <depends optional-attribute-name="JaasSecurityManagerService">jboss.security:service=JaasSecurityManager</depends>
      </xsl:if>

  </xsl:template>

  <!-- Datasource binding -->
  <xsl:template name="ds-binding">
    <xsl:param name="cm-name"></xsl:param>

    <mbean code="org.jboss.resource.adapter.jdbc.remote.WrapperDataSourceService" 
           name="jboss.jca:service=DataSourceBinding,name={jndi-name}" 
           display-name="Binding for DataSource {jndi-name}">
      <attribute name="JndiName"><xsl:value-of select="jndi-name"/></attribute>
      <!--
      <xsl:choose>
         <xsl:when test="use-java-context">
            <xsl:call-template name="use-java-context">
               <xsl:with-param name="use-java"><xsl:value-of select="use-java-context"/></xsl:with-param>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <attribute name="UseJavaContext">true</attribute>
         </xsl:otherwise>
       </xsl:choose>
      -->
      <xsl:choose>
        <xsl:when test="use-java-context">
          <attribute name="UseJavaContext"><xsl:value-of select="use-java-context"/></attribute>
        </xsl:when>
        <xsl:otherwise>
          <attribute name="UseJavaContext">true</attribute>
        </xsl:otherwise>
      </xsl:choose>
      <depends optional-attribute-name="ConnectionManager">
         <xsl:value-of select="$cm-name"/>,name=<xsl:value-of select="jndi-name"/>
      </depends>
      <xsl:choose>
        <xsl:when test="jmx-invoker-name">
          <depends optional-attribute-name="JMXInvokerName"><xsl:value-of select="jmx-invoker-name"/></depends>
        </xsl:when>
        <xsl:otherwise>
          <depends optional-attribute-name="JMXInvokerName">jboss:service=invoker,type=jrmp</depends>
        </xsl:otherwise>
      </xsl:choose>
    
    </mbean>
  </xsl:template>

   <xsl:template name="use-java-context">
      <xsl:param name="use-java" />
      <attribute name="UseJavaContext"><xsl:value-of select="$use-java"/></attribute>
      <!-- Only assign the JMXInvokerName attribute and dependency if use-java-context is false -->
      <xsl:if test="$use-java = 'false'">
         <xsl:choose>
            <xsl:when test="jmx-invoker-name">
               <depends optional-attribute-name="JMXInvokerName"><xsl:value-of select="jmx-invoker-name"/></depends>
            </xsl:when>
            <xsl:otherwise>
               <depends optional-attribute-name="JMXInvokerName">jboss:service=invoker,type=jrmp</depends>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:if>
   </xsl:template>

  <!-- Connection factory binding -->
  <xsl:template name="cf-binding">
    <xsl:param name="cm-name"></xsl:param>

    <mbean code="org.jboss.resource.connectionmanager.ConnectionFactoryBindingService" 
           name="jboss.jca:service=DataSourceBinding,name={jndi-name}" 
           display-name="Binding for ConnectionFactory {jndi-name}">
      <attribute name="JndiName"><xsl:value-of select="jndi-name"/></attribute>
      <xsl:choose>
         <xsl:when test="use-java-context">
            <attribute name="UseJavaContext"><xsl:value-of select="use-java-context"/></attribute>
         </xsl:when>
         <xsl:otherwise>
            <attribute name="UseJavaContext">true</attribute>
         </xsl:otherwise>
       </xsl:choose>
      <depends optional-attribute-name="ConnectionManager">
         <xsl:value-of select="$cm-name"/>,name=<xsl:value-of select="jndi-name"/>
      </depends>
    </mbean>
  </xsl:template>

  <xsl:template name="tx-manager">
      <depends optional-attribute-name="TransactionManagerService">jboss:service=TransactionManager</depends>
  </xsl:template>

   <xsl:template name="type-mapping">
      <xsl:param name="datasource"/>

      <xsl:if test="$datasource/metadata or $datasource/type-mapping">
         <xsl:element name="mbean">
            <xsl:attribute name="code">org.jboss.ejb.plugins.cmp.jdbc.metadata.DataSourceMetaData</xsl:attribute>
            <xsl:attribute name="name">
               <xsl:text>jboss.jdbc:service=metadata,datasource=</xsl:text>
               <xsl:value-of select="$datasource/jndi-name"/>
            </xsl:attribute>

            <xsl:element name="depends">
               <xsl:attribute name="optional-attribute-name">MetadataLibrary</xsl:attribute>
               <xsl:text>jboss.jdbc:service=metadata</xsl:text>
            </xsl:element>

            <xsl:if test="$datasource/type-mapping">
               <xsl:element name="attribute">
                  <xsl:attribute name="name">TypeMapping</xsl:attribute>
                  <xsl:value-of select="$datasource/type-mapping"/>
               </xsl:element>
            </xsl:if>

            <!-- DEPRECATED in favor of type-mapping -->
            <xsl:if test="$datasource/metadata">
               <xsl:element name="attribute">
                  <xsl:attribute name="name">TypeMapping</xsl:attribute>
                  <xsl:value-of select="$datasource/metadata/type-mapping"/>
               </xsl:element>
            </xsl:if>
         </xsl:element>
      </xsl:if>
   </xsl:template>

  <!-- template to copy any anonymous depends elements inside a cf/ds configuration element -->
  <xsl:template match="depends" mode="anonymous">
    <depends><xsl:value-of select="."/></depends>
  </xsl:template>

  <!-- template to copy all other elements literally, mbeans for instance-->
  <xsl:template match="*|@*|text()">
    <xsl:copy>
      <xsl:apply-templates select="*|@*|text()"/>
    </xsl:copy>
  </xsl:template>

   <!--
      | new experimental ha stuff
   -->

   <xsl:template match="ha-local-tx-datasource">

     <mbean code="org.jboss.resource.connectionmanager.TxConnectionManager"
            name="jboss.jca:service=LocalTxCM,name={jndi-name}"
            display-name="ConnectionManager for DataSource {jndi-name}">

       <attribute name="TrackConnectionByTx">true</attribute>
       <attribute name="LocalTransactions">true</attribute>

       <xsl:call-template name="pool">
         <xsl:with-param name="mcf-template">ha-local-wrapper</xsl:with-param>
       </xsl:call-template>
       <xsl:call-template name="cm-common"/>
       <xsl:call-template name="tx-manager"/>

     </mbean>
     <xsl:call-template name="ds-binding">
        <xsl:with-param name="cm-name">jboss.jca:service=LocalTxCM</xsl:with-param>
     </xsl:call-template>

     <xsl:call-template name="type-mapping">
        <xsl:with-param name="datasource" select="."/>
     </xsl:call-template>
   </xsl:template>

   <!-- template to generate the ManagedConnectionFactory mbean for our jca-jdbc local wrapper -->
   <xsl:template name="ha-local-wrapper">

       <depends optional-attribute-name="ManagedConnectionFactoryName">
       <!--embedded mbean-->
         <mbean code="org.jboss.resource.connectionmanager.RARDeployment" name="jboss.jca:service=ManagedConnectionFactory,name={jndi-name}" display-name="ManagedConnectionFactory for DataSource {jndi-name}">

           <xsl:apply-templates select="depends" mode="anonymous"/>

           <depends optional-attribute-name="OldRarDeployment">jboss.jca:service=RARDeployment,name='jboss-ha-local-jdbc.rar'</depends>
           <attribute name="RARName"><xsl:value-of select="jboss-ha-local-jdbc.rar"/></attribute>
           <attribute name="ConnectionDefinition">javax.sql.DataSource</attribute>

           <attribute name="ManagedConnectionFactoryProperties">
             <properties>
                <config-property name="ConnectionURL" type="java.lang.String"><xsl:value-of select="normalize-space(connection-url)"/></config-property>
                <config-property name="DriverClass" type="java.lang.String"><xsl:value-of select="normalize-space(driver-class)"/></config-property>

                <xsl:call-template name="wrapper-common-properties"/>
                <xsl:if test="connection-property">
                  <config-property name="ConnectionProperties" type="java.lang.String">
                    <xsl:apply-templates select="connection-property"/>
                  </config-property>
                </xsl:if>

             </properties>
           </attribute>
         </mbean>
       </depends>
   </xsl:template>

   <xsl:template match="ha-xa-datasource">

     <mbean code="org.jboss.resource.connectionmanager.TxConnectionManager"
            name="jboss.jca:service=XATxCM,name={jndi-name}"
            display-name="ConnectionManager for DataSource {jndi-name}">

       <xsl:choose>
         <xsl:when test="track-connection-by-tx">
           <attribute name="TrackConnectionByTx">true</attribute>
           <attribute name="LocalTransactions">false</attribute>
         </xsl:when>
         <xsl:otherwise>
           <attribute name="TrackConnectionByTx">false</attribute>
           <attribute name="LocalTransactions">false</attribute>
         </xsl:otherwise>
       </xsl:choose>

      <xsl:if test="xa-resource-timeout">
         <attribute name="XAResourceTransactionTimeout"><xsl:value-of select="normalize-space(xa-resource-timeout)"/></attribute>
      </xsl:if>

       <xsl:call-template name="pool">
         <xsl:with-param name="mcf-template">ha-xa-wrapper</xsl:with-param>
       </xsl:call-template>
       <xsl:call-template name="cm-common"/>
       <xsl:call-template name="tx-manager"/>

     </mbean>
     <xsl:call-template name="ds-binding">
        <xsl:with-param name="cm-name">jboss.jca:service=XATxCM</xsl:with-param>
     </xsl:call-template>

     <xsl:call-template name="type-mapping">
        <xsl:with-param name="datasource" select="."/>
     </xsl:call-template>
   </xsl:template>

   <xsl:template name="ha-xa-wrapper">
      <depends optional-attribute-name="ManagedConnectionFactoryName">
         <!--embedded mbean-->
         <mbean code="org.jboss.resource.connectionmanager.RARDeployment"
               name="jboss.jca:service=ManagedConnectionFactory,name={jndi-name}"
               displayname="ManagedConnectionFactory for DataSource {jndi-name}">

           <xsl:apply-templates select="depends" mode="anonymous"/>

           <depends optional-attribute-name="OldRarDeployment">jboss.jca:service=RARDeployment,name='jboss-ha-xa-jdbc.rar'</depends>
           <attribute name="RARName"><xsl:value-of select="jboss-ha-xa-jdbc.rar"/></attribute>
           <attribute name="ConnectionDefinition">javax.sql.DataSource</attribute>

           <attribute name="ManagedConnectionFactoryProperties">
             <properties>
               <config-property name="XADataSourceClass" type="java.lang.String"><xsl:value-of select="normalize-space(xa-datasource-class)"/></config-property>

               <config-property name="XADataSourceProperties" type="java.lang.String">
                 <xsl:apply-templates select="xa-datasource-property"/>
               </config-property>

               <xsl:if test="isSameRM-override-value">
                 <config-property name="IsSameRMOverrideValue" type="java.lang.Boolean"><xsl:value-of select="normalize-space(isSameRM-override-value)"/></config-property>
               </xsl:if>

                <xsl:if test="url-property">
                  <config-property name="URLProperty" type="java.lang.String"><xsl:value-of select="normalize-space(url-property)"/></config-property>
                </xsl:if>

               <xsl:call-template name="wrapper-common-properties"/>

             </properties>
           </attribute>
         </mbean>
       </depends>
   </xsl:template>

</xsl:stylesheet>
