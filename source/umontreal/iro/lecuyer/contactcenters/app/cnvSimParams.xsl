<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:ccapp="http://www.iro.umontreal.ca/lecuyer/contactcenters/app"
   xmlns:cnv="xalan://umontreal.iro.lecuyer.contactcenters.app.OldSimParamsConverter"
   exclude-result-prefixes="cnv">
   <xsl:output method="xml" indent="yes"/>

   <xsl:template match="repSimParams | batchSimParams">
      <xsl:element name="ccapp:{local-name()}" namespace="http://www.iro.umontreal.ca/lecuyer/contactcenters/app">
         <xsl:apply-templates select="@*" mode="simAttr"/>
         <xsl:apply-templates select="callTrace"/>
         <xsl:apply-templates select="reportParams"/>
         <xsl:if test="@caching | @randomStreamClass | @randomStreamSeed">
            <randomStreams>
               <xsl:for-each select="@caching">
                  <xsl:copy/>
               </xsl:for-each>
               <xsl:if test="@randomStreamClass">
                  <xsl:attribute name="streamClass">
                     <xsl:value-of select="@randomStreamClass"/>
                  </xsl:attribute>
               </xsl:if>
               <xsl:if test="@randomStreamSeed">
                  <streamSeed>
                     <xsl:value-of select="@randomStreamSeed"/>
                     <xsl:message>The streamSeed element may need corrections</xsl:message> 
                  </streamSeed>
               </xsl:if>
            </randomStreams>
         </xsl:if>
         <xsl:if test="@targetError &gt; 0">
            <xsl:variable name="confidenceLevel">
               <xsl:value-of select="@level | reportParams/@confidenceLevel"/>
            </xsl:variable>
            <sequentialSampling confidenceLevel="{$confidenceLevel}" measure="SERVICELEVEL" targetError="{@targetError}"/>
            <xsl:if test="@onlyServiceLevel != 'false'">
            <sequentialSampling confidenceLevel="{$confidenceLevel}" measure="ABANDONMENTRATIO" targetError="{@targetError}"/>
            <sequentialSampling confidenceLevel="{$confidenceLevel}" measure="WAITINGTIME" targetError="{@targetError}"/>
            <sequentialSampling confidenceLevel="{$confidenceLevel}" measure="OCCUPANCY" targetError="{@targetError}"/>
            </xsl:if>
         </xsl:if>
         <xsl:apply-templates select="controlVariable"/>
      </xsl:element>
   </xsl:template>

   <xsl:template match="@*" mode="simAttr">
      <xsl:copy/>
   </xsl:template>

   <xsl:template match="@targetError | @onlyServiceLevel | @caching | @randomStreamClass | @randomStreamSeed" mode="simAttr"/>

   <xsl:template match="@cpuTimeLimit | @batchSize" mode="simAttr">
      <xsl:attribute name="{local-name()}">
               <xsl:value-of select="cnv:timeToDuration(.)"/>
      </xsl:attribute>
   </xsl:template>

   <xsl:template match="callTrace">
      <xsl:copy>
         <xsl:for-each select="@*">
            <xsl:copy/>
         </xsl:for-each>
         <xsl:apply-templates select="database"/>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="reportParams">
      <report>
         <xsl:for-each select="@*">
            <xsl:copy/>
         </xsl:for-each>
         <xsl:if test="not(@confidenceLevel) and ../@level">
            <xsl:attribute name="confidenceLevel">
               <xsl:value-of select="../@level"/>
            </xsl:attribute>
         </xsl:if>
         <xsl:apply-templates/>
      </report>      
   </xsl:template>

   <xsl:template match="printedStat | controlVariable">
      <xsl:copy>
         <xsl:for-each select="@* | node()">
            <xsl:copy/>
         </xsl:for-each>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="database">
         <database>
         <xsl:for-each select="@*">
            <xsl:copy/>
         </xsl:for-each>
         <xsl:if test="property">
            <properties>
               <xsl:for-each select="property">
                  <xsl:element name="string">
                     <xsl:attribute name="name">
                        <xsl:value-of select="@name"/>
                     </xsl:attribute>
                     <xsl:if test="@value">
                        <xsl:attribute name="value">
                           <xsl:value-of select="@value"/>
                        </xsl:attribute>
                     </xsl:if>
                  </xsl:element>
               </xsl:for-each>
            </properties>
         </xsl:if>
         </database>
    </xsl:template>
</xsl:stylesheet>
