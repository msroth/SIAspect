/* 
 * ****************************************************************************
 * Copyright 2013 MS Roth
 * http://msroth.wordpress.com
 * 
 * Please see the document "How to Find Similar Documents in a Document 
 * Repository Without Using a Full Text Index" by M. Scott Roth / Armedia, LLC
 * July 2013 for more details.
 * ****************************************************************************
 */

package com.dm_misc.similarity;

import java.util.ArrayList;

import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.*;

public interface ISIAspect {

	public String getSimHashValue() throws DfException;
	public void computeSimHashValue() throws DfException;
	public boolean isSimilar(String si_value, double threshold) throws DfException;
	public boolean isSimilar(String si_value) throws DfException;
	public boolean isSimilar(IDfSysObject sObj, double threshold) throws DfException;
	public boolean isSimilar(IDfSysObject sObj) throws DfException;
	public double getSimilarity(String si_value) throws DfException;
	public double getSimilarity(IDfSysObject sObj) throws DfException;
	public ArrayList<IDfSysObject> findSimilarObjects() throws DfException;
	public ArrayList<IDfSysObject> findSimilarObjects(double threshold) throws DfException;
}

// <SDG><
