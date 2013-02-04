package edu.utdallas.paged.db.test;

import com.hp.hpl.jena.db.*;
import com.hp.hpl.jena.db.impl.IRDBDriver;
import com.hp.hpl.jena.db.test.Data;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.DB;

import edu.utdallas.paged.db.PagedGraphRDB;
import edu.utdallas.paged.db.PagedModelRDB;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestPagedMultiModel extends TestCase
{
	String DefModel = PagedGraphRDB.DEFAULT;    

	public TestPagedMultiModel( String name )
	{ super( name ); }

	public static TestSuite suite()
	{ return new TestSuite( TestPagedMultiModel.class ); }   

	Model model = null;
	PagedModelRDB dmod1 = null;    
	PagedModelRDB dmod2 = null;
	PagedModelRDB nmod1 = null; 
	PagedModelRDB nmod2 = null; 
	IDBConnection conn = null;
	IRDBDriver dbDriver;

	protected void setUp() throws java.lang.Exception 
	{
		conn = TestPagedConnection.makeAndCleanTestConnection();
		dbDriver = conn.getDriver();
		model = PagedModelRDB.createModel(conn);
		conn.getDriver().setStoreWithModel(DefModel);
		dmod1 = PagedModelRDB.createModel(conn,"Def_Model_1");
		conn.getDriver().setStoreWithModel("Def_Model_1");
		dmod2 = PagedModelRDB.createModel(conn, "Def_Model_2");
		conn.getDriver().setStoreWithModel(null);
		nmod1 = PagedModelRDB.createModel(conn,"Named_Model_1");
		conn.getDriver().setStoreWithModel("Named_Model_1");
		nmod2 = PagedModelRDB.createModel(conn,"Named_Model_2");
	}

	protected void tearDown() throws java.lang.Exception 
	{
		dmod1.close(); dmod2.close();
		nmod1.close(); nmod2.close();
		conn.cleanDB();
		conn.close();
		conn = null;
	}


	public void addToDBGraphProp ( Model model, Property prop, String val ) 
	{
		// first, get URI of the graph
		StmtIterator iter = model.listStatements(
				new SimpleSelector(null, DB.graphName, (RDFNode) null));
		assertTrue(iter.hasNext());

		Statement stmt = iter.nextStatement();
		assertTrue(iter.hasNext() == false);
		Resource graphURI = stmt.getSubject();
		Literal l = model.createLiteral(val);
		Statement s = model.createStatement(graphURI,prop,l);
		model.add(s);
		assertTrue(model.contains(s));
	}

	private void addOnModel(Model model, Statement stmt) 
	{
		model.add(stmt);
		assertTrue( model.contains(stmt) );
		model.add(stmt);
		assertTrue( model.contains(stmt) );
	}

	private void rmvOnModel(Model model, Statement stmt) 
	{
		assertTrue( model.contains(stmt) );
		model.remove(stmt);
		assertTrue( !model.contains(stmt) );
		model.add(stmt);
		assertTrue( model.contains(stmt) );
		model.remove(stmt);
		assertTrue( !model.contains(stmt) );
	}


	private void addRemove(Statement stmt) 
	{
		long cnt = model.size();
		addOnModel(model,stmt);
		addOnModel(dmod1,stmt);
		addOnModel(dmod2,stmt);
		addOnModel(nmod1,stmt);
		addOnModel(nmod2,stmt);
		assertTrue( model.size() == (cnt+1));
		assertTrue( dmod1.size() == 1);
		assertTrue( dmod2.size() == 1);
		assertTrue( nmod1.size() == 1);
		assertTrue( nmod2.size() == 1);

		rmvOnModel(nmod2,stmt);
		rmvOnModel(nmod1,stmt);
		rmvOnModel(dmod2,stmt);
		rmvOnModel(dmod1,stmt);
		rmvOnModel(model,stmt);
		assertTrue( model.size() == cnt);
		assertTrue( dmod1.size() == 0);
		assertTrue( dmod2.size() == 0);
		assertTrue( nmod1.size() == 0);
		assertTrue( nmod2.size() == 0);
	}

	public void testAddRemoveURI() 
	{
		Resource s = model.createResource("test#subject");
		Property p = model.createProperty("test#predicate");
		Resource o = model.createResource("test#object");

		addRemove( model.createStatement(s,p,o));    		
	}

	public void testAddRemoveLiteral() 
	{
		Resource s = model.createResource("test#subject");
		Property p = model.createProperty("test#predicate");
		Literal l = model.createLiteral("testLiteral");

		addRemove( model.createStatement(s,p,l));    	
	} 

	public void testSetLongObjectLenFailure() 
	{
		try {
			int len = dbDriver.getLongObjectLength();
			dbDriver.setLongObjectLength(len / 2);
			assertTrue(false);
		} catch (Exception e) {
		}
	}

	public void testLongObjectLen() 
	{
		long longLen = dbDriver.getLongObjectLength();
		assertTrue(longLen > 0 && longLen < 100000);

		String base = ".";
		StringBuffer buffer = new StringBuffer(1024 + (int) longLen);
		long minLongLen = longLen - 32;
		long maxLongLen = longLen + 32;
		assertTrue(minLongLen > 0);

		Resource s = model.createResource("test#subject");
		Property p = model.createProperty("test#predicate");
		Literal l;
		Statement stmt;
		while (buffer.length() < minLongLen) { /*( build base string */
			buffer.append(base);
		}
		/* add stmts with long literals */
		long modelSizeBeg = model.size();
		while (buffer.length() < maxLongLen) {
			l = model.createLiteral(buffer.toString());
			stmt = model.createStatement(s, p, l);
			model.add(stmt);
			assertTrue(model.contains(stmt));
			assertTrue(stmt.getObject().equals(l));
			buffer.append(base);
		}
		assertTrue(model.size() == (modelSizeBeg + maxLongLen - minLongLen));
		/* remove stmts with long literals */
		while (buffer.length() > minLongLen) {
			buffer.deleteCharAt(0);
			l = model.createLiteral(buffer.toString());
			stmt = model.createStatement(s, p, l);
			assertTrue(model.contains(stmt));
			model.remove(stmt);
			assertTrue(!model.contains(stmt));
		}
		assertTrue(model.size() == modelSizeBeg);
	}

	public void testSetLongObjectLen() 
	{
		int len = dbDriver.getLongObjectLength();
		int len2 = len - 2 ;
		try {
			tearDown();
			conn = TestPagedConnection.makeTestConnection();
			dbDriver = conn.getDriver();
			len = dbDriver.getLongObjectLength();
			dbDriver.setLongObjectLength(len2);
			model = PagedModelRDB.createModel(conn);
		} catch (Exception e) {
			assertTrue(false);
		}
		testLongObjectLen();

		// now make sure longObjectValue persists
		model.close();
		try {
			conn.close();
			conn = TestPagedConnection.makeTestConnection();
			dbDriver = conn.getDriver();
			assertTrue(len == dbDriver.getLongObjectLength());
			model = PagedModelRDB.open(conn);
			assertTrue(len2 == dbDriver.getLongObjectLength());
		} catch (Exception e) {
			assertTrue(false);
		}
	}

	public void testAddRemoveHugeLiteral() 
	{
		String base = Data.strLong ;
		StringBuffer buffer = new StringBuffer(4096);
		while(buffer.length() < 4000 )
			buffer.append(base);
		Resource s = model.createResource("test#subject");
		Property p = model.createProperty("test#predicate");
		Literal l = model.createLiteral(buffer.toString());

		addRemove( model.createStatement(s,p,l));    	
	} 

	public void testAddRemoveDatatype() 
	{
		Resource s = model.createResource("test#subject");
		Property p = model.createProperty("test#predicate");
		Literal l = model.createTypedLiteral("stringType");

		addRemove( model.createStatement(s,p,l));    	
	} 

	public void testAddRemoveHugeDatatype() 
	{
		String base = Data.strLong ;
		StringBuffer buffer = new StringBuffer(4096);
		while(buffer.length() < 4000 )
			buffer.append(base);
		Resource s = model.createResource("test#subject");
		Property p = model.createProperty("test#predicate");
		Literal l2 = model.createTypedLiteral(buffer.toString());

		addRemove( model.createStatement(s,p,l2));    	
	} 

	public void testAddRemoveBNode() 
	{
		Resource s = model.createResource();
		Property p = model.createProperty("test#predicate");
		Resource o = model.createResource();

		addRemove( model.createStatement(s,p,o));

	}

	public void testBNodeIdentityPreservation() 
	{
		Resource s = model.createResource();
		Property p = model.createProperty("test#predicate");
		Resource o = model.createResource();

		// Create two statements that differ only in
		// the identity of the bnodes - then perform
		// add-remove on one and verify the other is
		// unchanged.
		Statement spo = model.createStatement(s,p,o);
		Statement ops = model.createStatement(o,p,s);
		model.add(spo);
		addRemove(ops);   	
		assertTrue( model.contains(spo));
		model.remove(spo);
	}
}
/** Copyright (c) 2008-2010, The University of Texas at Dallas
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the The University of Texas at Dallas nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY The University of Texas at Dallas ''AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL The University of Texas at Dallas BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/