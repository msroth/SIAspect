/* 
 * ****************************************************************************
 * Copyright 2013 MS Roth
 * http://msroth.wordpress.com
 * 
 * This file contains the WDK component which implements the find similar 
 * feature.
 * 
 * Please see the document "How to Find Similar Documents in a Document 
 * Repository Without Using a Full Text Index" by M. Scott Roth / Armedia, LLC
 * July 2013 for more details.
 * ****************************************************************************
 */

package com.dm_misc.similarity.webtop;

import java.util.ArrayList;

import com.dm_misc.similarity.ISIAspect;
import com.documentum.fc.client.DfAuthenticationException;
import com.documentum.fc.client.DfIdentityException;
import com.documentum.fc.client.DfPrincipalException;
import com.documentum.fc.client.DfServiceException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.aspect.IDfAspects;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfList;
import com.documentum.web.common.ArgumentList;
import com.documentum.web.form.control.databound.Datagrid;
import com.documentum.web.form.control.databound.TableResultSet;
import com.documentum.web.form.control.Label;
import com.documentum.web.formext.component.Component;
import com.documentum.web.formext.session.SessionManagerHttpBinding;

public class findSimilar extends Component {

	static final long serialVersionUID = 1L;
	String si_value = "";
	String text = "";

	public void onInit(ArgumentList args)	{
		super.onInit(args);

		try {
			// get the object
			String objectId = getInitArgs().get("objectId");
			IDfSysObject sObj = (IDfSysObject) getSession().getObject(new DfId(objectId));

			// check for content
			if (sObj.getContentSize() > 0) {

				// check for aspect - attach if needed
				if (!hasAspect(sObj,"si_aspect")) {
					// attach aspect
					((IDfAspects) sObj).attachAspect("si_aspect", null);
					sObj.save();
					sObj = (IDfSysObject) getSession().getObject(sObj.getObjectId());
				}

				// get si value
				((ISIAspect) sObj).computeSimHashValue();
				sObj.save();
				si_value = ((ISIAspect) sObj).getSimHashValue();

				// set similar list
				ArrayList<IDfSysObject> simList = ((ISIAspect) sObj).findSimilarObjects();

				// initialize the table result set using attribute names
				String[] columnHeaders = new String[] { "r_object_id", 
						"object_name",
						"similarity", 
						"si_value", 
						"path" };

				// create new table result set
				TableResultSet trs = new TableResultSet(columnHeaders);

				// loop over SI ArrayList
				for (IDfSysObject sObj2: simList) {

					String[] tableRow = new String[5];
					tableRow[0] = sObj2.getObjectId().toString();
					tableRow[1] = sObj2.getObjectName();
					tableRow[2] = ((ISIAspect) sObj).getSimilarity(sObj2)*100 + "%";
					tableRow[3] = sObj2.getString("si_aspect.si_value");
					tableRow[4] = getFolderPath(sObj2.getFolderId(0));

					// finally, add the row to the table result set
					trs.add(tableRow);
				}

				if (trs.getResultsCount() == 0) {
					text = "No results.";
				}
					
				// update datagrid
				Datagrid dg = (Datagrid) getControl("SI_LIST",Datagrid.class);
				dg.getDataProvider().setDfSession(getDfSession());
				dg.getDataProvider().setScrollableResultSet(trs);

			} else {
				text = "This object has no content.";
			}

			// update the form

			Label threshold = (Label) getControl("SI_THRESHOLD",Label.class);
			threshold.setLabel("80%");

			Label objName = (Label) getControl("OBJECT_NAME",Label.class);
			objName.setLabel(sObj.getObjectName());

			Label objId = (Label) getControl("OBJECT_ID",Label.class);
			objId.setLabel(sObj.getObjectId().toString());

			Label SIvalue= (Label) getControl("SI_VALUE",Label.class);
			SIvalue.setLabel(si_value);
			
			Label msg= (Label) getControl("MSG",Label.class);
			msg.setLabel(text);
			
		} catch (DfException e) {
			System.out.println(e.getMessage());
		}

	}

	
	private String getFolderPath(IDfId folderId) throws DfException {
		IDfFolder folder = (IDfFolder) getSession().getObject(folderId);
		return folder.getFolderPath(0);
	}
	
	
	private boolean hasAspect(IDfSysObject sObj, String aspect) {
		try {
			IDfList aspectList = ((IDfAspects) sObj).getAspects();
			for (int i=0; i < aspectList.getCount(); i++) {    
				if (((String) aspectList.get(i)).equalsIgnoreCase(aspect))
					return true;
			}
		} catch (DfException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	// these methods force the 'close' button to appear on the container dialog
	public boolean canCommitChanges() {
		return false;
	}

	public boolean canCancelChanges() {
		return false;
	}

	public IDfSession getSession() {
		IDfSessionManager sessionManager = null;
		IDfSession dfSession = null;
		try {
			sessionManager = SessionManagerHttpBinding.getSessionManager();
			String docbase = SessionManagerHttpBinding.getCurrentDocbase();
			dfSession = sessionManager.getSession(docbase);
			System.out.println("Logged in for " + docbase);
		} catch(DfIdentityException dfe) {
			System.out.println("Error while obtaining Session. Id exception" + dfe.getMessage());
		} catch(DfAuthenticationException ae) {
			System.out.println("Authentication exception while " + "getting client " + ae.getMessage());
		} catch(DfPrincipalException pe) {
			System.out.println("Principal exception " + pe.getMessage());
		} catch(DfServiceException se) {
			System.out.println("Service exception " + se.getMessage());
		}

		return (dfSession);
	}

}

// <SDG><
