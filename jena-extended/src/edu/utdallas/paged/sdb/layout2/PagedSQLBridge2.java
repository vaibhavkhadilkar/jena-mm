package edu.utdallas.paged.sdb.layout2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.sdb.compiler.SqlBuilder;
import com.hp.hpl.jena.sdb.core.AliasesSql;
import com.hp.hpl.jena.sdb.core.SDBRequest;
import com.hp.hpl.jena.sdb.core.Scope;
import com.hp.hpl.jena.sdb.core.ScopeEntry;
import com.hp.hpl.jena.sdb.core.sqlexpr.S_Equal;
import com.hp.hpl.jena.sdb.core.sqlexpr.SqlColumn;
import com.hp.hpl.jena.sdb.core.sqlexpr.SqlExpr;
import com.hp.hpl.jena.sdb.core.sqlnode.SqlNode;
import com.hp.hpl.jena.sdb.core.sqlnode.SqlTable;
import com.hp.hpl.jena.sdb.layout2.TableDescNodes;
import com.hp.hpl.jena.sdb.layout2.ValueType;
import com.hp.hpl.jena.sdb.sql.ResultSetJDBC;
import com.hp.hpl.jena.sdb.sql.SQLUtils;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;

import edu.utdallas.paged.sdb.store.PagedSQLBridgeBase;

/**
 * @see com.hp.hpl.jena.sdb.layout2.SQLBridge2
 * @author vaibhav
 */
public class PagedSQLBridge2 extends PagedSQLBridgeBase
{
    private static Logger log = LoggerFactory.getLogger(PagedSQLBridge2.class) ;
    
    // Result nodes tables 
    private static final String NodeBase = AliasesSql.NodesResultAliasBase ;

	public PagedSQLBridge2(SDBRequest request, SqlNode sqlNode, Collection<Var> projectVars) 
	{ super(request, sqlNode, projectVars);	}

    @Override
    protected void buildValues()
    {
        SqlNode sqlNode = getSqlNode() ;
        for ( Var v : getProject() )
            sqlNode = insertValueGetter(request, sqlNode, v) ;
        setSqlNode(sqlNode) ;
    }
    
    @Override
    protected void buildProject()
    {
        for ( Var v : getProject() )
        {
            if ( ! v.isNamedVar() )
                continue ;
            ScopeEntry e = getSqlNode().getNodeScope().findScopeForVar(v) ;
            if ( e == null )
            {
                // Should be a column mentioned in the SELECT which is not mentioned in this block
                continue ;
            }
            SqlColumn vCol = e.getColumn() ; 
    
            SqlTable table = vCol.getTable() ;
            String sqlVarName = allocSqlName(v) ;
            
            // Need to allocate aliases because otherwise we need to access
            // "table.column" as a label and "." is illegal in a label
            
            // XXX sqlVarName => SqlColumn, not a Var.  A hack that fails. 
            
            String vLex = SQLUtils.gen(sqlVarName,"lex") ;
            SqlColumn cLex = new SqlColumn(table, "lex") ;
    
            String vDatatype = SQLUtils.gen(sqlVarName,"datatype") ;
            SqlColumn cDatatype = new SqlColumn(table, "datatype") ;
    
            String vLang = SQLUtils.gen(sqlVarName,"lang") ;
            SqlColumn cLang = new SqlColumn(table, "lang") ;
    
            String vType = SQLUtils.gen(sqlVarName,"type") ;
            SqlColumn cType = new SqlColumn(table, "type") ;
    
            addProject(cLex, vLex) ;
            addProject(cDatatype, vDatatype) ;
            addProject(cLang, vLang) ;
            addProject(cType, vType) ;
            
            addAnnotation(sqlVarName+"="+v.toString()) ;
        }
        setAnnotation() ; 
    }
    
    private SqlNode insertValueGetter(SDBRequest request, SqlNode sqlNode, Var var)
    {
        ScopeEntry e1 = sqlNode.getIdScope().findScopeForVar(var) ;
        if ( e1 == null )
        {
            // Debug.
            @SuppressWarnings("unused")
			Scope scope = sqlNode.getIdScope() ;
            // Variable not actually in results.
            return sqlNode ;
        }
        
        // Already in scope (from a condition)?
        ScopeEntry e2 = sqlNode.getNodeScope().findScopeForVar(var) ;
        if ( e2 != null )
            // Already there
            return sqlNode ;
        
        SqlColumn c1 = e1.getColumn() ;
        // Not in scope -- add a table to get it
        TableDescNodes nodeTableDesc = request.getStore().getNodeTableDesc() ;
        
        String tableAlias = request.genId(NodeBase) ; 
        SqlTable nTable = new SqlTable(tableAlias, nodeTableDesc.getTableName()) ;
        String nodeKeyColName = nodeTableDesc.getNodeRefColName() ;
        SqlColumn c2 = new SqlColumn(nTable, nodeKeyColName) ;

        nTable.setValueColumnForVar(var, c2) ;
        // Condition for value: triple table column = node table id/hash 
        nTable.addNote("Var: "+var) ;

        SqlExpr cond = new S_Equal(c1, c2) ;
        SqlNode n = SqlBuilder.leftJoin(request, sqlNode, nTable, cond) ;
        return n ;
    }
    
    @Override
    protected Binding assembleBinding(ResultSetJDBC rsHolder, Binding parent)
    {
        Binding b = new BindingMap(parent) ;
        ResultSet rs = rsHolder.get() ;
        for ( Var v : super.getProject() )
        {
            if ( ! v.isNamedVar() )
                // Skip bNodes and system variables
                continue ;

            String codename = super.getSqlName(v) ;
            if ( codename == null )
                // Not mentioned in query.
                continue ;
            try {
                int type        = rs.getInt(SQLUtils.gen(codename,"type")) ;
                // returns 0 on null : type 0 is not allocated
                // Test with "wasNull()" for safety
                if ( rs.wasNull() )
                    continue ;

                // Oracle: may be null (empty string).
                String lex = rs.getString(SQLUtils.gen(codename,"lex")) ;
                if ( lex == null )
                    lex = "" ;
                
                // byte bytes[] = rs.getBytes(SQLUtils.gen(codename,"lex")) ; // bytes
                // try {
                //     String $ = new String(bytes, "UTF-8") ;
                //     log.info("lex bytes : "+$+"("+$.length()+")") ;
                // } catch (Exception ex) {}
                // Same as rs.wasNull() for things that can return Java nulls.

                String datatype = rs.getString(SQLUtils.gen(codename,"datatype")) ;
                String lang     = rs.getString(SQLUtils.gen(codename,"lang")) ;
                ValueType vType = ValueType.lookup(type) ;
                Node r          = makeNode(lex, datatype, lang, vType) ;
                b.add(v, r) ;
            } catch (SQLException ex)
            { // Unknown variable?
                //log.warn("Not reconstructed: "+n) ;
            } 
        }
        return b ;
    }
    
    private static Node makeNode(String lex, String datatype, String lang, ValueType vType)
    {
        switch (vType)
        {
            case BNODE:
                return Node.createAnon(new AnonId(lex)) ;
            case URI:
                return Node.createURI(lex) ;
            case STRING:
                return Node.createLiteral(lex, lang, false) ;
            case XSDSTRING:
                return Node.createLiteral(lex, null, XSDDatatype.XSDstring) ;
            case INTEGER:
                return Node.createLiteral(lex, null, XSDDatatype.XSDinteger) ;
            case DOUBLE:
                return Node.createLiteral(lex, null, XSDDatatype.XSDdouble) ;
            case DATETIME:       
                return Node.createLiteral(lex, null, XSDDatatype.XSDdateTime) ;
            case OTHER:
                RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatype);
                return Node.createLiteral(lex, null, dt) ;
            default:
                log.warn("Unrecognized: ("+lex+", "+lang+", "+vType+")") ;
            return Node.createLiteral("UNRECOGNIZED") ; 
        }
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