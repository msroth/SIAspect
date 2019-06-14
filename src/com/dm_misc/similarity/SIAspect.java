/* 
 * ****************************************************************************
 * Copyright 2013 MS Roth
 * http://msroth.wordpress.com
 * 
 * This file implements the Similarity Index (SI) Aspect.  The Aspect 
 * generates, saves, and compares SimHash values to determine similarity 
 * among documents.  It relies on the the sbo.si_view database table to 
 * determine similarity.
 * 
 * Please see the document "How to Find Similar Documents in a Document 
 * Repository Without Using a Full Text Index" by M. Scott Roth / Armedia, LLC
 * July 2013 for more details.
 * ****************************************************************************
 */

package com.dm_misc.similarity;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;

import org.commoncrawl.util.SimHash;

import com.documentum.com.DfClientX;
import com.documentum.fc.client.DfDocument;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.aspect.IDfAspects;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfList;


public class SIAspect extends DfDocument implements ISIAspect {
	
	// constants
	private static final String SI_ASPECT_NAME = "si_aspect";
	private static final String SI_ASPECT_ATTR_NAME = SI_ASPECT_NAME + ".si_value";
	private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.8D;
	private static final String SI_VIEW_TABLE = "dbo.si_view";
	private static final String SI_VIEW_SIMILARITY = "similarity";
	private static final String SI_VIEW_SIMILAR_OBJ_ID = "similar_obj_id";
	private static final String SI_VIEW_THIS_INDEX = "r_object_id";
	private static final String FIND_SIMILAR_QUERY = "select " + SI_VIEW_SIMILAR_OBJ_ID + " from " + SI_VIEW_TABLE + " where " + 
									SI_VIEW_THIS_INDEX + " = '%s' and " + SI_VIEW_SIMILARITY + " >= %s";
	
	// return SimHash value
	public String getSimHashValue() throws DfException {
		return pad(this.getString(SI_ASPECT_ATTR_NAME),64);
	}
	
	// calculate the SimHash value
	public void computeSimHashValue() throws DfException {
		this.setString(SI_ASPECT_ATTR_NAME,computeSimHash());
	}
	
	// determine if this object is similar to the one passed in using the default 
	// similarity threshold
	public boolean isSimilar(IDfSysObject sObj) throws DfException {
		return isSimilar(sObj,DEFAULT_SIMILARITY_THRESHOLD);
	}
	
	// determine if this ovject is similar to the one passed in using the similarity 
	// threshold provided
	public boolean isSimilar(IDfSysObject sObj, double threshold) throws DfException {
		if (hasSIAspect(sObj)) {
			String si_value = sObj.getString(SI_ASPECT_ATTR_NAME);
			return isSimilar(si_value,threshold);
		} else {
			return false;
		}
	}
	
	// determine if this object's SimHash value is similar to the SimHash passed in 
	// using the default similarity threshold
	public boolean isSimilar(String si_value) throws DfException {
		return isSimilar(si_value,DEFAULT_SIMILARITY_THRESHOLD);
	}
	
	// determine if this object's SimHash value is similar to the SimHash passed in 
	// using the similarity threshold provided
	public boolean isSimilar(String si_value, double threshold) throws DfException {
		double sim = getSimilarity(si_value);
		if (sim >= threshold)
			return true;
		else
			return false;
	}
	
	// returns the percent similarity between this object and the one passed in
	public double getSimilarity(IDfSysObject sObj) throws DfException {
		if (hasSIAspect(sObj)) {
			String si_value = sObj.getString(SI_ASPECT_ATTR_NAME);
			return getSimilarity(si_value);
		} else {
			return 0D;
		}
	}
	
	// returns the percent similarity between this object's SimHash and the one
	// passed in
	public double getSimilarity(String si_value) throws DfException {
		int distance = getDistance(getSimHashValue(),si_value);
		return (((double) (SimHash.HASH_SIZE - distance)/(double) SimHash.HASH_SIZE));
	}
	
	// get the Hamming Distance between two SimHash values
	private int getDistance(String s1, String s2) {
		long l1 = new BigInteger(s1, 2).longValue();
		long l2 = new BigInteger(s2, 2).longValue();
		return SimHash.hammingDistance(l1,l2);
	}
	
	// calculate the SimHash value for this object
	private String computeSimHash() throws DfException {
		if (this.getContentSize() > 0) {
	        ByteArrayInputStream content = this.getContent();
	        DfClientX cx = new DfClientX();
	        String string1 = cx.ByteArrayInputStreamToString(content);
	        return pad(Long.toBinaryString(SimHash.computeOptimizedSimHashForString(string1)),64);
		} else {
			return null;
		}
	}
	
	// find similar objects using the default similarity threshold
	public ArrayList<IDfSysObject> findSimilarObjects() throws DfException {
		return findSimilarObjects(DEFAULT_SIMILARITY_THRESHOLD);
	}
	
	// find objects similar to this one using the provided similarity threshold
	public ArrayList<IDfSysObject> findSimilarObjects(double threshold) throws DfException {
		
		IDfCollection col = null;
		ArrayList<IDfSysObject> results = new ArrayList<IDfSysObject>();
		
		try {
		
			// query the dbo.si_view registered table for similar SimHash values
			IDfQuery q = new DfQuery();
			String dql = String.format(FIND_SIMILAR_QUERY,this.getObjectId().toString(),Double.toString(threshold));
			q.setDQL(dql);
			col = q.execute(this.getSession(),DfQuery.DF_READ_QUERY);
			
			// add objects to result set
			while (col.next()) {
				IDfSysObject sObj = (IDfSysObject) getSession().getObject(new DfId(col.getString(SI_VIEW_SIMILAR_OBJ_ID)));
				if (sObj != null)
					results.add(sObj);
			}
		} catch (DfException e) {
			throw new DfException(e);
		} finally {
			if (col != null)
				col.close();
		}
		
		return results;
	}
	
	// pad strings to 64 characters by prepending 0 if necessary
	private static String pad(String s, int n) {
		String s1 = "";
		
		if (s.length() == n)
			s1=s;
		else if (s.length() >= n)
			s1 = s.substring(n);
		else {
			for (int i=0; i<(n-s.length()); i++) {
				s1 += "0";
			}
			s1 += s;
		}
		return s1;
	}
	
	// determine if the object passed in has the SIAspect Aspect attached
	private boolean hasSIAspect(IDfSysObject sObj) throws DfException {
		IDfList aspectList = ((IDfAspects) sObj).getAspects();
		for (int i=0; i < aspectList.getCount(); i++) {    
			if (((String) aspectList.get(i)).equalsIgnoreCase(SI_ASPECT_NAME)) {
					return true;
			}
		}
		return false;
	}


}

// <SDG><


