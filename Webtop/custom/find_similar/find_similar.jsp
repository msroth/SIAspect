<!--  
* Copyright 2013 MS Roth
* http://msroth.wordpress.com
* 
* This file contains the JSP implementation of the find similar 
* feature.
* 
* Please see the document "How to Find Similar Documents in a Document 
* Repository Without Using a Full Text Index" by M. Scott Roth / Armedia, LLC
* July 2013 for more details.
-->

<%@ page contentType="text/html"%>
<%@ page errorPage="/wdk/errorhandler.jsp"%>
<%@ taglib uri="/WEB-INF/tlds/dmform_1_0.tld" prefix="dmf" %>
<dmf:html>
<dmf:head>
<dmf:webform/>
</dmf:head>
<dmf:body>
<h2>Find Similar</h2>

<dmf:form>
	<b>Similarity Threshold:</b> 
	<dmf:label name="SI_THRESHOLD"/><br/>
	<p>
	<b>Object name:</b> 
	<dmf:label name="OBJECT_NAME"/><br/>
	<b>Object Id:</b> 
	<dmf:label name="OBJECT_ID"/><br/>
	<b>Similarity Index Value:</b> 
	<dmf:label name="SI_VALUE"/>
	<p>
	<dmf:datagrid name="SI_LIST" width="1000" height="220" paged="true" fixedheaders="true" rowselection="true">
		<dmf:datagridTh>
			<dmf:datasortlink label="Object ID" column="r_object_id" name="objectIDSort"/>
		</dmf:datagridTh>
		<dmf:datagridTh>
			<dmf:datasortlink label="Object Name" column="object_name" name="objectNameSort"/>
		</dmf:datagridTh>
		<dmf:datagridTh>
			<dmf:datasortlink label="Similarity" column="similarity" name="similaritySort"/>
		</dmf:datagridTh>
		<dmf:datagridTh>
			<dmf:datasortlink label="SI Value" column="si_value" name="SISort"/>
		</dmf:datagridTh>
		<dmf:datagridTh>
			<dmf:datasortlink label="Path" column="path" name="pathSort"/>
		</dmf:datagridTh>

		<dmf:datagridRow>
			<dmf:datagridRowTd width="16">
				<dmf:label datafield="r_object_id" />
			</dmf:datagridRowTd>
			<dmf:datagridRowTd width="64">
				<dmf:label datafield="object_name" />
			</dmf:datagridRowTd>
			<dmf:datagridRowTd width="6">
				<dmf:label datafield="similarity" />
			</dmf:datagridRowTd>
			<dmf:datagridRowTd width="64">
				<dmf:label datafield="si_value" />
			</dmf:datagridRowTd>
			<dmf:datagridRowTd width="140">
				<dmf:label datafield="path" />
			</dmf:datagridRowTd>
		</dmf:datagridRow>
	</dmf:datagrid>
	<p>
	</p> 
	<dmf:label name="MSG"/>

</dmf:form>
</dmf:body>
</dmf:html>