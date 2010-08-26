<!--
XSL script for converting Beast version 1 files to Beast 2.0 XML files
-->
<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>

<xsl:output method='xml' indent='yes'/>

<xsl:template match='/'>
    <beast version='2.0' namespace='beast.core:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood'>
        <xsl:apply-templates  select='//alignment[not(name(@*)="idref")]'/>

    <input spec='HKY' id='hky'>
        <kappa idref='hky.kappa'/>
        <frequencies id='freqs' spec='Frequencies'>
            <data idref='alignment'/>
        </frequencies>
    </input>

    <input spec='SiteModel' id="siteModel">
        <frequencies idref='freqs'/>
        <substModel idref='hky'/>
    </input>

    <input spec='TreeLikelihood' id="treeLikelihood">
        <data idref="alignment"/>
        <tree idref="tree"/>
        <siteModel idref="siteModel"/>
    </input>

    <parameter id="hky.kappa" value="1.0" lower="0.0"/>

    <tree spec='beast.util.ClusterTree' id='tree' clusterType='upgma'>
        <taxa idref='alignment'/>
    </tree>



        <xsl:apply-templates  select='//mcmc'/>
    </beast>
</xsl:template>


<xsl:template match='alignment'>
    <data dataType='{@dataType}'  id='{@id}'>
        <xsl:apply-templates select='sequence[not(@id="")]'/> 
    </data>
</xsl:template>

<xsl:template match='sequence'>
    <xsl:variable name='taxon'>
        <xsl:value-of select='taxon/@id'/>
        <xsl:value-of select='taxon/@idref'/>
    </xsl:variable>
    <sequence taxon='{$taxon}'><xsl:value-of select='.'/></sequence>
</xsl:template>







<xsl:template match='mcmc'>
    <run spec='beast.core.MCMC' chainLength='{@chainLength}' preBurnin='{@preBurnin}'>
        <state>
            <xsl:for-each select='//operators/*/*|//log//*'>
                <xsl:if test='string-length(concat(@idref,@id))&gt;0'>
                    <stateNode idref='{@id}{@idref}'/>
                </xsl:if>
            </xsl:for-each>
        </state>

        <distribution id='likelihood' idref="treeLikelihood"/>

        <xsl:apply-templates select='//operators[not(name(@*)="idref")]/*' mode='operator'/>
        <xsl:apply-templates select='log|logTree'/>
    </run>
</xsl:template>



<xsl:template match='log|logTree'>
    <log logEvery='@logEvery' fileName='@fileName'>
        <xsl:for-each select='*[not(name()="column")]|column/*'>
            <xsl:copy>
                <xsl:attribute name='name'>log</xsl:attribute>
                <xsl:attribute name='idref'><xsl:value-of select='@id'/><xsl:value-of select='@idref'/></xsl:attribute>
            </xsl:copy>
        </xsl:for-each>
    </log>
</xsl:template>

<xsl:template match='@*|node()' mode='operator'>
    <xsl:variable name='name' select='name()'/>
  <operator>
    <xsl:attribute name='spec'>
        <xsl:choose>
        <xsl:when test='$name="uniformOperator"'>Uniform</xsl:when>
        <xsl:when test='$name="wideExchange"'>Exchange</xsl:when>
        <xsl:when test='$name="narrowExchange"'>Exchange</xsl:when>
        <xsl:otherwise><xsl:value-of select='translate(substring($name,1,1),"abcdefghijklmnopqrstuvwxyz","ABCDEFGHIJKLMNOPQRSTUVWXYZ")'/><xsl:value-of select='substring($name,2)'/></xsl:otherwise>
        </xsl:choose>
    </xsl:attribute>
    <xsl:if test='$name="wideExchange"'><xsl:attribute name='isNarrow'>false</xsl:attribute></xsl:if>
    <xsl:apply-templates select='@*|node()'/>
  </operator>
</xsl:template>

<xsl:template match='@idref'>
    <xsl:attribute name='idref'>
        <xsl:choose>
            <xsl:when test='contains(.,".rootHeight")'>
                <xsl:value-of select='substring-before(.,".rootHeight")'/>
            </xsl:when>
            <xsl:when test='contains(.,".internalNodeHeights")'>
                <xsl:value-of select='substring-before(.,".internalNodeHeights")'/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select='.'/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:attribute>
</xsl:template>


<xsl:template match='treeModel'>
  <tree>
    <xsl:apply-templates select='@*|node()'/>
  </tree>
</xsl:template>

<xsl:template match='@*|node()'>
  <xsl:copy>
    <xsl:apply-templates select='@*|node()'/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>

