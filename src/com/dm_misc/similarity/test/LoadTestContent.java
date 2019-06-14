/* 
 * ****************************************************************************
 * Copyright 2013 MS Roth
 * http://msroth.wordpress.com
 * 
 * This file loads documents from the hard drive into a Documentum repository,
 * applies the SIAspect Aspect, generates the document's SI, and then runs
 * some tests.
 * 
 * Please see the document "How to Find Similar Documents in a Document 
 * Repository Without Using a Full Text Index" by M. Scott Roth / Armedia, LLC
 * July 2013 for more details.
 * ****************************************************************************
 */

package com.dm_misc.similarity.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.dm_misc.similarity.ISIAspect;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.aspect.IDfAspects;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfLoginInfo;

public class LoadTestContent {

	// path constants
	private static final String DOC_PATH = "c:/temp/SIDocs";
	private static final String OBJ_PATH = "/Temp/SIDocs";
	
	// other constants
	private static final String DOCBASE = "repo1";
	private static final String USERNAME = "dmadmin";
	private static final String PASSWORD = "dmadmin";
	private static final String SI_ASPECT_NAME = "si_aspect";
	private static final int NUM_TESTS = 10;
	
	// test docs
	private static final String[] TEST_DOCS = new String[] {"a16.txt","b16.txt","ab88.txt","ba88.txt","a12b4.txt","a4b12.txt",
                                                            "gettysburg.txt","getty_d_1.txt","getty_d_5.txt", "getty_d_10.txt",
															"getty_i_1.txt", "getty_i_5.txt", "getty_i_10.txt",
															"us-const.txt","us-const_d_5.txt","us-const_d_10.txt",
															"us-const_i_5.txt","us-const_i_10.txt",
															"MS Word file.docx", "MS Word file_d_10.docx",
															"MS Word file.pdf", "MS Word file_d_10.pdf",
															"VaticanStaircase.jpg"};
	
	// global vars
	private static IDfSessionManager sessMgr = null;
	private static IDfSession session = null;
	private static ArrayList<String> objIdList = new ArrayList<String>();
	
	
	public static void main(String[] args) {
		
		try {
			
			// login
			login(DOCBASE, USERNAME, PASSWORD);
			if (session == null) {
				System.out.println("Could not login.");
				System.exit(-1);
			}
			System.out.println("Logged in " + USERNAME + "@" + DOCBASE);
			
			// load docs
			loadSITestDocs(DOC_PATH, OBJ_PATH);
			
			// apply aspect
			applySIAspectToDocs(OBJ_PATH);
			
			// randomly test 10
			testRandomSI(NUM_TESTS);

			// do specific tests
			testSpecificSI();
			
			System.out.println("done");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// test the specific documents in the TEST_DOCS array
	private static void testSpecificSI() throws DfException {
	
		System.out.println("testing for know documents...");
		for (String doc : TEST_DOCS) {
			IDfSysObject sObj = (IDfSysObject) session.getObjectByQualification("dm_document where object_name = '" + doc + "' and folder('" + OBJ_PATH + "')");
			if (sObj != null)
				System.out.println(getSimilarObjs(sObj));
		}
	}
	
	// find all docs similar to the one passed in
	private static String getSimilarObjs(IDfSysObject sObj) throws DfException {
		String out = sObj.getObjectName() + " (" + sObj.getObjectId().toString() + ") [" + ((ISIAspect) sObj).getSimHashValue() + "]\n";
		ArrayList<IDfSysObject> list = ((ISIAspect) sObj).findSimilarObjects();
		out += "found " + list.size() + " similar objects\n";
		for (int j=0; j<list.size(); j++) {
			IDfSysObject sObj2 = list.get(j);
			out += "\t" + sObj2.getObjectName() + " ("+ sObj2.getObjectId().toString() + ") [" + ((ISIAspect) sObj2).getSimHashValue() + 
			"] >> " + ((ISIAspect) sObj).getSimilarity(sObj2) + "\n"; 
		}
		out += "\n";
		return out;
	}
	
	// randomly test some docs to see if any are similar
	private static void testRandomSI(int num) throws DfException {
		
		System.out.println("testing " + num + " random documents...");
		Random generator = new Random(System.currentTimeMillis());
		
		for (int i = 0; i<num; i++) {
			int idx = generator.nextInt(objIdList.size());
			String id = objIdList.get(idx);
			IDfSysObject sObj = (IDfSysObject) session.getObject(new DfId(id));
			System.out.println(getSimilarObjs(sObj));
		}
	}
	
	// apply the SIAspect to all docs in the OBJ_PATH folder
	private static void applySIAspectToDocs(String objpath) throws DfException {
		boolean found = false;
		
		System.out.println("applying " + SI_ASPECT_NAME + "...");
		// get all docs in test folder
		IDfQuery q = new DfQuery();
		IDfCollection col = null;
		q.setDQL("select r_object_id from dm_document where folder('" + objpath + "',descend)");
		col = q.execute(session, DfQuery.DF_READ_QUERY);
		
		while (col.next()) {
			IDfSysObject sObj = (IDfSysObject) session.getObject(col.getId("r_object_id"));
			
			// check for aspect
			IDfList aspectList = ((IDfAspects) sObj).getAspects();
			for (int i=0; i < aspectList.getCount(); i++) {    
				if (((String) aspectList.get(i)).equalsIgnoreCase(SI_ASPECT_NAME)) {
						found = true;
				}
			}
			
			// attach aspect
			if (!found) {
				((IDfAspects) sObj).attachAspect("si_aspect", null);
				sObj.save();
				sObj = (IDfSysObject) session.getObject(sObj.getObjectId());
			}
			
			// compute simhash
			((ISIAspect) sObj).computeSimHashValue();
			sObj.save();
			
			// add to list of obj ids
			objIdList.add(sObj.getObjectId().toString());
		}
		col.close();	
		System.out.println("applied to "+ objIdList.size() + " objects");
	}
	
	// login to Documentum
	private static void login(String docbase, String username, String password) throws DfException {
		
		DfClientX cx = new DfClientX();
		IDfClient client = cx.getLocalClient();
		sessMgr = client.newSessionManager();
		IDfLoginInfo li = new DfLoginInfo();
		li.setUser(username);
		li.setPassword(password);
		sessMgr.setIdentity(docbase,li);
		session = sessMgr.getSession(docbase);
		
	}
	
	// load docs from file system into documentum
	private static void loadSITestDocs(String docpath, String objpath) throws Exception {
		
		// check that file system path exists
		File dir = new File(docpath);
		if (!dir.exists() || !dir.isDirectory())
			throw new DfException ("File path " + docpath + " does not exist on file system");
		
		// check that docbase path exists
		IDfFolder folder = (IDfFolder) session.getObjectByQualification("dm_folder where any r_folder_path = '" + objpath +"'");
		if (folder == null)
			throw new DfException("Path " + objpath + " does not exist in " + DOCBASE);
		
		// check that docbase path is empty
		IDfQuery q = new DfQuery();
		q.setDQL("select count(*) as cnt from dm_document where folder('" + objpath + "')");
		IDfCollection col = q.execute(session, DfQuery.DF_READ_QUERY);
		col.next();
		int c = col.getInt("cnt");
		col.close();
		
		// if not empty, delete existing files
		if (c > 0) {
			System.out.println("deleting " + c + " objects from " + objpath + "...");
			q.setDQL("delete dm_document (all) objects where folder('" + objpath + "')");
			col = q.execute(session, DfQuery.DF_READ_QUERY);
			col.close();
		}
		
		// get files
        System.out.print("scanning dir " + docpath + "...");
		ArrayList<String> fileList = getFileList(docpath);
		System.out.println("found " + fileList.size() + " files");
		
		// load into DCTM
		System.out.println("loading files to " + objpath + "...");
		for (String filePath : fileList) {
			File f = new File(filePath);
			String filename = f.getName();
			
			IDfSysObject sObj = (IDfSysObject) session.newObject("dm_document");
			sObj.setObjectName(filename);
			sObj.setContentType("crtext");
			sObj.setFile(filePath);
			sObj.link(objpath);
			sObj.save();
			
			// randomly create similar file
			File simFile = createSimilarFile(filePath);
			if (simFile != null) {
				filename = simFile.getName();
				sObj = (IDfSysObject) session.newObject("dm_document");
				sObj.setObjectName(filename);
				sObj.setContentType("crtext");
				sObj.setFile(simFile.getAbsolutePath());
				sObj.link(objpath);
				sObj.save();
				
				// delete modified file so it doesn't clutter up the archive
				simFile.delete();
			}
		}
	}
	
	// randomly modify files passed in
	private static File createSimilarFile(String origFile) throws Exception {
		String newFileName = "";
		
		// random select files to modify: 50-50 chance
        int mod = (int) (Math.random() * 10);
        if (mod > 5) {
            return null;
        } else {
        	File srcFile = new File(origFile);
        	
        	// skip binary files
        	if (srcFile.getName().endsWith(".pdf") || srcFile.getName().endsWith(".jpg") || srcFile.getName().endsWith(".docx"))
        		return null;
        	
        	// get content into String
            String content = FileUtils.readFileToString(srcFile);
            
            // if the file is too short, return
            if (content.length() < 10) {
                return null;
            }

            int len = 0;
            int del = -1;
            int op = -100;

            // get number between 1 - 15
            del = (int) (Math.random() * 15);
            
            // figure out how long 30% of document is
            len = (int) (Math.random() * (content.length() * 0.3));
            
            // find a random starting place in document
            int start = (int) (Math.random() * (content.length() - len - 1));
            
            // get percent diff the chosen length is
            double dif = (double) (((double) len / (double) content.length()) * 100);

            
			// create duplicate file
			if (del > 10) {
				op = 0;
				dif = 0D;
			
			// delete random segment
            } else if (del >= 5 && del <= 10) {
                op = -1;

                String del1 = content.substring(0, start);
                String del2 = content.substring(start + len, content.length());
                content = del1 + "\n[delete]\n" + del2;
 
            // append random segment	
            } else {
                op = 1;

                String insert1 = content.substring(0, start);
                String insertStr = content.substring(0, len);
                String insert2 = content.substring(start + 1, content.length());
                content = insert1 + "\n[insert]\n" + insertStr + "\n[end insert]\n" + insert2;
            }
            
			// build new file name
            newFileName = renameFile(origFile, op, dif);

            // save file
            File destFile = new File(newFileName);
            if (op==0) {
            	FileUtils.copyFile(srcFile, destFile);
            } else {
            	FileUtils.writeStringToFile(destFile, content);
            }
            return destFile;
        }
	}
	
	// rename file to contain _i_, _d_, or _dup to indicate how it was modified
    private static String renameFile(String path, int op, double dif) {

        int i = path.lastIndexOf("/");
        if (i == -1) {
            i = path.lastIndexOf("\\");
        }

        String fileName = path.substring(i + 1);
        path = path.substring(0,i) + "/";
        String ext = "";
        String newFileName = "";

        int j = fileName.lastIndexOf(".");
        if (j > 0) {
            ext = fileName.substring(j + 1);
            newFileName = fileName.substring(0, j);
        } else {
            ext = "";
            newFileName = fileName;
        }

		if (op == 0) {
			newFileName = path + String.format("%s_%s.%s", newFileName, "dup", ext);
		} else if (op == 1) {
            newFileName = path + String.format("%s_i_%.0f.%s", newFileName, dif, ext);
        } else {
            newFileName = path + String.format("%s_d_%.0f.%s", newFileName, dif, ext);
        }

        return newFileName;
    }
    
    // get the list of files in the DOC_PATH to load
    private static ArrayList<String> getFileList(String dirPath) {
        ArrayList<String> files = new ArrayList<String>();
        File dir = new File(dirPath);

        String[] children = dir.list();
        if (children == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < children.length; i++) {
                // Get filename of file or directory
                String filename = dir.getAbsolutePath() + "/" + children[i];
                if ((filename != null)
                        && (filename.length() > 1)
                        && (!children[i].startsWith("."))
                        && (!children[i].toLowerCase().endsWith(".zip"))) {

                    File test = new File(filename);

                    if (test.isDirectory()) {
                        // recursion
                        files.addAll(getFileList(filename));
                    } else {
                        if (test.length() > 0) {
                            files.add(filename);
                        } else {
                            // empty file; skip
                        }
                    }
                } else {
                    // invalid; skip
                }
            }
        }

        return files;
    }
	
}

// <SDG><

