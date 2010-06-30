<!--
XSL script for converting Beast version 1 files to Beast 2.0 XML files
-->
<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns='http://www.w3.org/TR/xhtml1/strict'>

<xsl:output method='xml' indent='yes'/>

<xsl:template match='/'>
    <beast version='2.0'>
        <xsl:apply-templates  select='//alignment'/>
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

<xsl:template match='@*|node()'>
  <xsl:copy>
    <xsl:apply-templates select='@*|node()'/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>

