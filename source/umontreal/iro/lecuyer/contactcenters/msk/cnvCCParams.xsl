<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:ccmsk="http://www.iro.umontreal.ca/lecuyer/contactcenters/msk"
   xmlns:cnv="xalan://umontreal.iro.lecuyer.contactcenters.msk.OldCallCenterParamsConverter"
   exclude-result-prefixes="cnv">
   <xsl:output method="xml" indent="yes"/>
   <xsl:param name="baseURI" select="'.'"/>
   <xsl:variable name="cnvObj" select="cnv:new(MSKCCParams | mskccparams, string($baseURI))"/>

   <xsl:template match="MSKCCParams | mskccparams">
      <ccmsk:MSKCCParams>
         <xsl:apply-templates select="@*" mode="ccAttr"/>
         <xsl:apply-templates select="busynessGen"/>
         <xsl:apply-templates select="inboundType"/>
         <xsl:apply-templates select="outboundType"/>
         <xsl:apply-templates select="agentGroup"/>
         <xsl:apply-templates select="router"/>
         <xsl:apply-templates select="serviceLevel"/>
      </ccmsk:MSKCCParams>
   </xsl:template>

   <xsl:template match="@*" mode="ccAttr">
      <xsl:copy/>
   </xsl:template>

   <xsl:template match="@startTime | @startingTime" mode="ccAttr">
      <xsl:attribute name="startingTime">
         <xsl:value-of select="cnv:getStartingTime($cnvObj)"/>
      </xsl:attribute>
   </xsl:template>

   <xsl:template match="@periodDuration" mode="ccAttr">
      <xsl:attribute name="periodDuration">
         <xsl:value-of select="cnv:getPeriodDuration($cnvObj)"/>
      </xsl:attribute>
   </xsl:template>

   <xsl:template match="@queueCapacity" mode="ccAttr">
      <xsl:if test="@queueCapacity > 0">
         <xsl:copy/>
      </xsl:if>
   </xsl:template>

   <xsl:template match="busynessGen">
      <xsl:call-template name="convertRVG"/>
   </xsl:template>

   <xsl:template match="inboundType | outboundType">
      <xsl:copy>
         <xsl:for-each select="@*[local-name() != 'sourceEnabled']">
            <xsl:copy/>
         </xsl:for-each>
         <xsl:for-each select="probAbandon">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="patienceTime">
            <xsl:call-template name="convertMPG"/>
         </xsl:for-each>
         <xsl:for-each select="serviceTime">
            <xsl:call-template name="convertMPG"/>
         </xsl:for-each>
         <xsl:for-each select="dialer/probReach">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="dialer/reachTime">
            <xsl:call-template name="convertMPG"/>
         </xsl:for-each>
         <xsl:for-each select="dialer/failTime">
            <xsl:call-template name="convertMPG"/>
         </xsl:for-each>
         <xsl:apply-templates select="arrivalProcess"/>
         <xsl:apply-templates select="dialer"/>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="arrivalProcess">
      <xsl:copy>
         <xsl:for-each select="@*">
            <xsl:copy/>
         </xsl:for-each>
         <xsl:if test="../@sourceEnabled">
            <xsl:attribute name="sourceEnabled">
               <xsl:value-of select="../@sourceEnabled"/>
            </xsl:attribute>
         </xsl:if>
         <xsl:copy-of select="cnv:convertSourceToggleTimes($cnvObj,string(../sourceToggleTimes))/*"/>
         <xsl:for-each select="data">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'int')"/>
         </xsl:for-each>
         <xsl:for-each select="arvGen">
            <xsl:call-template name="convertRVG"/>
         </xsl:for-each>
         <xsl:for-each select="arrivals">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="poissonGammaParams">
            <xsl:copy-of select="cnv:convertPoissonGammaParams($cnvObj,.)/*"/>
         </xsl:for-each>
         <xsl:for-each select="copulaSigma">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="nortaSigma">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'double')"/>
         </xsl:for-each>
        <xsl:for-each select="nortaGamma">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each>
        <xsl:for-each select="nortaP">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each>
        <xsl:for-each select="splineTimes">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'duration')"/>
         </xsl:for-each>
        <xsl:for-each select="splineLambdas">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'duration')"/>
         </xsl:for-each>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="dialer">
      <xsl:copy>
         <xsl:for-each select="@*[local-name() != 'checkedPeriodDuration']">
            <xsl:copy/>
         </xsl:for-each>
         <xsl:if test="../@sourceEnabled">
            <xsl:attribute name="sourceEnabled">
               <xsl:value-of select="../@sourceEnabled"/>
            </xsl:attribute>
         </xsl:if>
         <xsl:for-each select="@checkedPeriodDuration">
            <xsl:attribute name="{local-name()}">
               <xsl:value-of select="cnv:timeToDuration($cnvObj,.)"/>
            </xsl:attribute>
         </xsl:for-each>
         <xsl:copy-of select="cnv:convertSourceToggleTimes($cnvObj,string(../sourceToggleTimes))/*"/>
         <xsl:for-each select="minFreeAgentsTest">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'int')"/>
         </xsl:for-each>
         <xsl:for-each select="minFreeAgentsTarget">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'int')"/>
         </xsl:for-each>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="agentGroup">
      <xsl:copy>
         <xsl:for-each select="@*[local-name() != 'detailed']">
            <xsl:copy/>
         </xsl:for-each>
         <xsl:choose>
            <xsl:when test="../router/@routerPolicy = 'AGENTSPREFWITHDELAYS' or ../router/@routerPolicy = 'AGENTSPREF' or ../router/@routerPolicy = 'LOCALSPEC'">
               <xsl:attribute name="detailed">true</xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
               <xsl:for-each select="@detailed">
                  <xsl:copy/>
               </xsl:for-each>
            </xsl:otherwise>
         </xsl:choose>
         <xsl:for-each select="staffing">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'int')"/>
         </xsl:for-each>
         <xsl:for-each select="schedule">
            <schedule>
               <xsl:for-each select="shift">
                  <xsl:copy-of select="cnv:convertScheduleShift($cnvObj,.)"/>
               </xsl:for-each>
            </schedule>
         </xsl:for-each>
<!--         <xsl:for-each select="serviceTimeMultipliers">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each> -->
         <xsl:for-each select="serviceTimeMultipliers">
            <serviceTimesMult>
<xsl:value-of select="cnv:convertArray($cnvObj,.,'double')"/>
            </serviceTimesMult>
         </xsl:for-each>
         <xsl:for-each select="probDisconnect">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="disconnectTime">
            <xsl:call-template name="convertMPG"/>
         </xsl:for-each>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="router">
      <xsl:copy>
         <xsl:apply-templates select="@*" mode="routerAttr"/>
         <xsl:for-each select="typeToGroupMap">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'int')"/>
         </xsl:for-each>
         <xsl:for-each select="groupToTypeMap">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'int')"/>
         </xsl:for-each>
         <xsl:for-each select="ranksTG">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="ranksGT">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="weightsTG">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="weightsGT">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'double')"/>
         </xsl:for-each>
         <xsl:for-each select="delaysGT">
            <xsl:copy-of select="cnv:convertArray2D($cnvObj,.,'duration')"/>
         </xsl:for-each>
         <xsl:for-each select="queueWeights | targetFracAgents | targetQueueRatio">
            <xsl:copy-of select="cnv:convertArray($cnvObj,.,'double')"/>
         </xsl:for-each>
      <xsl:choose>
         <xsl:when test="@routerPolicy = 'AGENTSPREF' or @routerPolicy = 'AGENTSPREFWITHDELAYS'">
            <xsl:choose>
            <xsl:when test="ranksTG | ranksGT">
               <routingTableSources ranksTG="ranksGT" ranksGT="ranksTG"
                  typeToGroupMap="ranksTG" groupToTypeMap="ranksGT"/>
            </xsl:when>
            <xsl:when test="typeToGroupMap">
               <routingTableSources ranksTG="typeToGroupMap" ranksGT="ranksTG"/>
            </xsl:when>
            <xsl:when test="groupToTypeMap">
               <routingTableSources ranksGT="groupToTypeMap" ranksTG="ranksGT"/>
            </xsl:when>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="@routerPolicy = 'LOCALSPEC'">
            <xsl:choose>
            <xsl:when test="ranksTG | ranksGT">
               <routingTableSources ranksTG="ranksGT" ranksGT="ranksTG"
                  typeToGroupMap="ranksTGAndRegions" groupToTypeMap="ranksGTAndRegions"/>
            </xsl:when>
            <xsl:when test="typeToGroupMap">
               <routingTableSources incidenceMatrixTG="typeToGroupMap"
               ranksTG="incidenceMatrixTGAndSkillCounts" ranksGT="ranksTG"/>
            </xsl:when>
            </xsl:choose>
         </xsl:when>
      </xsl:choose>
      </xsl:copy>
   </xsl:template>

   <xsl:template match="@*" mode="routerAttr">
      <xsl:copy/>
   </xsl:template>

   <xsl:template match="@localSpecOverflowDelay | @checkedPeriodDuration" mode="routerAttr">
      <xsl:attribute name="{local-name()}">
         <xsl:value-of select="cnv:timeToDuration($cnvObj,.)"/>
      </xsl:attribute>
   </xsl:template>

   <xsl:template match="serviceLevel">
      <xsl:copy>
      <xsl:copy-of select="cnv:convertAWT($cnvObj,awt,count(../inboundType),number(../@numPeriods))"/>
      <xsl:copy-of select="cnv:convertTarget($cnvObj,target,count(../inboundType),number(../@numPeriods))"/>
      </xsl:copy>
   </xsl:template>

   <xsl:template name="convertRVG">
      <xsl:choose>
         <xsl:when test="@xref">
            <xsl:variable name="xref" select="@xref"/>
            <xsl:copy-of select="cnv:convertRVG($cnvObj,/*//*[@id=$xref],local-name())"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:copy-of select="cnv:convertRVG($cnvObj,.,local-name())"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="convertMPG">
      <xsl:choose>
         <xsl:when test="@xref">
            <xsl:variable name="xref" select="@xref"/>
            <xsl:copy-of select="cnv:convertMPG($cnvObj, /*//*[@id=$xref],local-name())"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:copy-of select="cnv:convertMPG($cnvObj,.,local-name())"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
</xsl:stylesheet>
