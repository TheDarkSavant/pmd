/* Generated By:JJTree: Do not edit this line. ASTskip2NextOccurrence.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package net.sourceforge.pmd.lang.plsql.ast;

public
class ASTskip2NextOccurrence extends SimpleNode {
  public ASTskip2NextOccurrence(int id) {
    super(id);
  }

  public ASTskip2NextOccurrence(PLSQLParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(PLSQLParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=dfa73214397e915d80f3dfaefb81fc8a (do not edit this line) */