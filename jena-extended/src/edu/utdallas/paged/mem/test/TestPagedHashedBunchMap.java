package edu.utdallas.paged.mem.test;

import com.hp.hpl.jena.mem.TripleBunch;
import com.hp.hpl.jena.rdf.model.test.ModelTestBase;

import edu.utdallas.paged.mem.PagedHashCommon;
import edu.utdallas.paged.mem.PagedHashedBunchMap;
import edu.utdallas.paged.mem.PagedHashedTripleBunch;

public class TestPagedHashedBunchMap extends ModelTestBase
{
	public TestPagedHashedBunchMap( String name )
	{ super( name ); }

	public void testSize()
	{
		@SuppressWarnings("unused")
		PagedHashCommon b = new PagedHashedBunchMap();
	}

	public void testClearSetsSizeToZero()
	{
		TripleBunch a = new PagedHashedTripleBunch();
		PagedHashedBunchMap b = new PagedHashedBunchMap();
		b.clear();
		assertEquals( 0, b.size() );
		b.put( "key",  a );
		assertEquals( 1, b.size() );
		b.clear();
		assertEquals( 0, b.size() );
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