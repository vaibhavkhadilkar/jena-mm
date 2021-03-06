package edu.utdallas.paged.mem.algorithm;

import edu.utdallas.paged.mem.algorithm.CacheAlgorithmBase;
import edu.utdallas.paged.mem.cache.CacheBase;

/**
 * A class that implements the degree centrality algorithm.
 * Nodes with a higher degree centrality are left in memory whereas nodes
 * with a lower degree centrality are written to disk
 * 
 * @author vaibhav
 */
public class NodeStampDegreeCentrality extends CacheAlgorithmBase
{
	/**
	 * Method that implements the comparable interface for degree centrality.
	 * @param o1 - nodestamp object against which the current object is compared
	 * @return an integer that determines if the current node must come before the 
	 *         parameter node based on the degree centrality
	 */
	public int compareTo(Object o1) 
	{
		NodeStampDegreeCentrality temp = (NodeStampDegreeCentrality)o1;

		//if the current node has a larger degree than the parameter node, leave that node in memory by shifting
		//positions with the parameter node, else leave the order unchanged
		if( ( this.connections / (CacheBase.size*1.0) ) > ( temp.connections / (CacheBase.size*1.0) ) )
			return 1;
		else
			return 0;
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