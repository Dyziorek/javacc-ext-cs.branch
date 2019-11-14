// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.javacc.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.javacc.parser.JavaCCGlobals.*;

/**
 * Generate the parser.
 */
public class ParseGenCS extends CodeGenerator implements JavaCCParserConstants {

  public void start() throws MetaParseException {

    Token t = null;

    if (JavaCCErrors.get_error_count() != 0) throw new MetaParseException();

    // ignore package directive as used only fo java
    // so remove it from prefix code in CSharp mode
    
    if (cu_to_insertion_point_1.size() != 0 &&
          ((Token)cu_to_insertion_point_1.get(0)).kind == PACKAGE
      ) {
        int endIndex = 0;
        for (int i = 1; i < cu_to_insertion_point_1.size(); i++) {
          if (((Token)cu_to_insertion_point_1.get(i)).kind == SEMICOLON) {
            endIndex = i;
          }
        }
        
        if (endIndex > 0)
        {
          List subEntries = new ArrayList(cu_to_insertion_point_1.subList(0, endIndex+1));
          cu_to_insertion_point_1.removeAll(subEntries);
        }
      }
    
    if (Options.getBuildParser()) {
      List tn = new ArrayList(toolNames);
      tn.add(toolName);
      genCodeLine("/* " + getIdString(tn, cu_name + ".cs") + " */");

     if (Options.stringValue("NAMESPACE").length() > 0) {
      genCodeLine("namespace " + Options.stringValue("NAMESPACE") + " {");
    }
      boolean implementsExists = false;

      if (cu_to_insertion_point_1.size() != 0) {
        printTokenSetup((Token)(cu_to_insertion_point_1.get(0))); ccol = 1;
        for (Iterator it = cu_to_insertion_point_1.iterator(); it.hasNext();) {
          t = (Token)it.next();
           if (t.kind == IMPLEMENTS || t.kind == COLON || (t.next.specialToken != null && t.next.specialToken.kind == MULTI_LINE_COMMENT)) {
            implementsExists = true;
          } else if (t.kind == CLASS) {
            implementsExists = false;
          }

          printToken(t);
        }
      }

            
      if (cu_to_insertion_point_2.size() != 0) {
        printTokenSetup((Token)(cu_to_insertion_point_2.get(0)));
        if (((Token)cu_to_insertion_point_2.get(0)).specialToken != null)
        {
          implementsExists = true;
        }
       
        
        genCode(" : ");
        
//        getSpecialStringToPrint((Token)(cu_to_insertion_point_2.get(0)));
        //printSpecialToken((Token)(cu_to_insertion_point_2.get(0)));
        //if (implementsExists) {
//          genCode(", ");
//        } else {
//          genCode(" : ");
//        }

        genCode(cu_name + "Constants ");
        //printTokenOnly((Token)(cu_to_insertion_point_2.get(0)));
        //cu_to_insertion_point_2.remove(0);
        
        
        for (Iterator it = cu_to_insertion_point_2.iterator(); it.hasNext();) {
          t = (Token)it.next();
          printTokenOnly(t);
        }
      }
      else
      {
        if (implementsExists) {
          genCode(", ");
        } else {
          genCode(" : ");
        }
        genCode(cu_name + "Constants ");
      }
      
      genCodeLine("");
      genCodeLine("");

      new ParseEngine().build(this);

      if (Options.getStatic()) {
        genCodeLine("  static private " + Options.getBooleanType() + " jj_initialized_once = false;");
      }
      if (Options.getUserTokenManager()) {
        genCodeLine("  /** User defined Token Manager. */");
        genCodeLine("  " + staticOpt() + "public TokenManager token_source;");
      } else {
        genCodeLine("  /** Generated Token Manager. */");
        genCodeLine("  " + staticOpt() + "public " + cu_name + "TokenManager token_source;");
        if (!Options.getUserCharStream()) {
          if (Options.getJavaUnicodeEscape()) {
            genCodeLine("  " + staticOpt() + "JavaCharStream jj_input_stream;");
          } else {
            genCodeLine("  " + staticOpt() + "SimpleCharStream jj_input_stream;");
          }
        }
      }
      genCodeLine("  /** Current token. */");
      genCodeLine("  " + staticOpt() + "public Token token;");
      genCodeLine("  /** Next token. */");
      genCodeLine("  " + staticOpt() + "public Token jj_nt;");
      if (!Options.getCacheTokens()) {
        genCodeLine("  " + staticOpt() + "private int jj_ntk;");
      }
      if (jj2index != 0) {
        genCodeLine("  " + staticOpt() + "private Token jj_scanpos, jj_lastpos;");
        genCodeLine("  " + staticOpt() + "private int jj_la;");
        if (lookaheadNeeded) {
          genCodeLine("  /** Whether we are looking ahead. */");
          genCodeLine("  " + staticOpt() + "private " + Options.getBooleanType() + " jj_lookingAhead = false;");
          genCodeLine("  " + staticOpt() + "private " + Options.getBooleanType() + " jj_semLA;");
        }
      }
      if (Options.getErrorReporting()) {
        genCodeLine("  " + staticOpt() + "private int jj_gen;");
        genCodeLine("  " + staticOpt() + "private int[] jj_la1 = new int[" + maskindex + "];");
        int tokenMaskSize = (tokenCount-1)/32 + 1;

        for (int i = 0; i < tokenMaskSize; i++) {
          genCodeLine("  static uint[] jj_la1_" + i + " = new uint[] {");
          for (Iterator it = maskVals.iterator(); it.hasNext();) {
            int[] tokenMask = (int[])(it.next());
            genCode("0x" + Integer.toHexString(tokenMask[i]) + ",");
          }
          genCodeLine("};");
        }
      }
      if (jj2index != 0 && Options.getErrorReporting()) {
        genCodeLine("  " + staticOpt() + "private JJCalls[] jj_2_rtns = new JJCalls[" + jj2index + "];");
        genCodeLine("  " + staticOpt() + "private " + Options.getBooleanType() + " jj_rescan = false;");
        genCodeLine("  " + staticOpt() + "private int jj_gc = 0;");
      }
      genCodeLine("");

      if (!Options.getUserTokenManager()) {
        if (Options.getUserCharStream()) {
          genCodeLine("  /** Constructor with user supplied CharStream. */");
          genCodeLine("  public " + cu_name + "(CharStream stream) {");
          if (Options.getStatic()) {
            genCodeLine("    if (jj_initialized_once) {");
            genCodeLine("      System.out.println(\"ERROR: Second call to constructor of static parser.  \");");
            genCodeLine("      System.out.println(\"       You must either use ReInit() " +
                    "or set the JavaCC option STATIC to false\");");
            genCodeLine("      System.out.println(\"       during parser generation.\");");
            genCodeLine("      throw new Error();");
            genCodeLine("    }");
            genCodeLine("    jj_initialized_once = true;");
          }
          if(Options.getTokenManagerUsesParser() && !Options.getStatic()){
            genCodeLine("    token_source = new " + cu_name + "TokenManager(this, stream);");
          } else {
            genCodeLine("    token_source = new " + cu_name + "TokenManager(stream);");
          }
          genCodeLine("    token = new Token();");
          if (Options.getCacheTokens()) {
            genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
          } else {
            genCodeLine("    jj_ntk = -1;");
          }
          if (Options.getErrorReporting()) {
            genCodeLine("    jj_gen = 0;");
            if (maskindex > 0) {
              genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
            }
            if (jj2index != 0) {
              genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine("  }");
          genCodeLine("");
          genCodeLine("  /** Reinitialise. */");
          genCodeLine("  " + staticOpt() + "public void ReInit(CharStream stream) {");
          genCodeLine("    token_source.ReInit(stream);");
          genCodeLine("    token = new Token();");
          if (Options.getCacheTokens()) {
            genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
          } else {
            genCodeLine("    jj_ntk = -1;");
          }
          if (lookaheadNeeded) {
            genCodeLine("    jj_lookingAhead = false;");
          }
          if (jjtreeGenerated) {
            genCodeLine("    jjtree.reset();");
          }
          if (Options.getErrorReporting()) {
            genCodeLine("    jj_gen = 0;");
            if (maskindex > 0) {
              genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
            }
            if (jj2index != 0) {
              genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine("  }");
        } else {
          genCodeLine("  /** Constructor with InputStream. */");
          genCodeLine("  public " + cu_name + "(System.IO.Stream stream) : this(stream, null)");
          genCodeLine("  {");
          genCodeLine("  }");
          genCodeLine("  /** Constructor with InputStream and supplied encoding */");
          genCodeLine("  public " + cu_name + "(System.IO.Stream stream, String encoding) {");
          if (Options.getStatic()) {
            genCodeLine("    if (jj_initialized_once) {");
            genCodeLine("      System.out.println(\"ERROR: Second call to constructor of static parser.  \");");
            genCodeLine("      System.out.println(\"       You must either use ReInit() or " +
                    "set the JavaCC option STATIC to false\");");
            genCodeLine("      System.out.println(\"       during parser generation.\");");
            genCodeLine("      throw new Error();");
            genCodeLine("    }");
            genCodeLine("    jj_initialized_once = true;");
          }
          if (Options.getJavaUnicodeEscape()) {
              
                genCodeLine("    jj_input_stream = new JavaCharStream(stream, encoding, 1, 1); } ");
              
          } else {
             genCodeLine("    jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1);");
          }
          if(Options.getTokenManagerUsesParser() && !Options.getStatic()){
            genCodeLine("    token_source = new " + cu_name + "TokenManager(this, jj_input_stream);");
          } else {
            genCodeLine("    token_source = new " + cu_name + "TokenManager(jj_input_stream);");
          }
          genCodeLine("    token = new Token();");
          if (Options.getCacheTokens()) {
            genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
          } else {
            genCodeLine("    jj_ntk = -1;");
          }
          if (Options.getErrorReporting()) {
            genCodeLine("    jj_gen = 0;");
            if (maskindex > 0) {
              genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
            }
            if (jj2index != 0) {
              genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine("  }");
          genCodeLine("");
          genCodeLine("  /** Reinitialise. */");
          genCodeLine("  " + staticOpt() + "public void ReInit(System.IO.Stream stream) {");
          genCodeLine("     ReInit(stream, null);");
          genCodeLine("  }");
          genCodeLine("  /** Reinitialise. */");
          genCodeLine("  " + staticOpt() + "public void ReInit(System.IO.Stream stream, String encoding) {");
          genCodeLine("    jj_input_stream.ReInit(stream, encoding, 1, 1); ");
          
          genCodeLine("    token_source.ReInit(jj_input_stream);");
          genCodeLine("    token = new Token();");
          if (Options.getCacheTokens()) {
            genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
          } else {
            genCodeLine("    jj_ntk = -1;");
          }
          if (jjtreeGenerated) {
            genCodeLine("    jjtree.reset();");
          }
          if (Options.getErrorReporting()) {
            genCodeLine("    jj_gen = 0;");
            genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
            if (jj2index != 0) {
              genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine("  }");
          genCodeLine("");
          genCodeLine("  /** Constructor. */");
          genCodeLine("  public " + cu_name + "(System.IO.StreamReader stream) {");
          if (Options.getStatic()) {
            genCodeLine("    if (jj_initialized_once) {");
            genCodeLine("      System.out.println(\"ERROR: Second call to constructor of static parser. \");");
            genCodeLine("      System.out.println(\"       You must either use ReInit() or " +
                    "set the JavaCC option STATIC to false\");");
            genCodeLine("      System.out.println(\"       during parser generation.\");");
            genCodeLine("      throw new Error();");
            genCodeLine("    }");
            genCodeLine("    jj_initialized_once = true;");
          }
          if (Options.getJavaUnicodeEscape()) {
            genCodeLine("    jj_input_stream = new JavaCharStream(stream, 1, 1);");
          } else {
            genCodeLine("    jj_input_stream = new SimpleCharStream(stream, 1, 1);");
          }
          if(Options.getTokenManagerUsesParser() && !Options.getStatic()){
            genCodeLine("    token_source = new " + cu_name + "TokenManager(this, jj_input_stream);");
          } else {
            genCodeLine("    token_source = new " + cu_name + "TokenManager(jj_input_stream);");
          }
          genCodeLine("    token = new Token();");
          if (Options.getCacheTokens()) {
            genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
          } else {
            genCodeLine("    jj_ntk = -1;");
          }
          if (Options.getErrorReporting()) {
            genCodeLine("    jj_gen = 0;");
            if (maskindex > 0) {
              genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
            }
            if (jj2index != 0) {
              genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine("  }");
          genCodeLine("");
          genCodeLine("  /** Reinitialise. */");
          genCodeLine("  " + staticOpt() + "public void ReInit(System.IO.StreamReader stream) {");
          if (Options.getJavaUnicodeEscape()) {
            genCodeLine("    jj_input_stream.ReInit(stream, 1, 1);");
          } else {
            genCodeLine("    jj_input_stream.ReInit(stream, 1, 1);");
          }
          genCodeLine("    token_source.ReInit(jj_input_stream);");
          genCodeLine("    token = new Token();");
          if (Options.getCacheTokens()) {
            genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
          } else {
            genCodeLine("    jj_ntk = -1;");
          }
          if (jjtreeGenerated) {
            genCodeLine("    jjtree.reset();");
          }
          if (Options.getErrorReporting()) {
            genCodeLine("    jj_gen = 0;");
            if (maskindex > 0) {
              genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
            }
            if (jj2index != 0) {
              genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine("  }");
        }
      }
      genCodeLine("");
      if (Options.getUserTokenManager()) {
        genCodeLine("  /** Constructor with user supplied Token Manager. */");
        genCodeLine("  public " + cu_name + "(TokenManager tm) {");
      } else {
        genCodeLine("  /** Constructor with generated Token Manager. */");
        genCodeLine("  public " + cu_name + "(" + cu_name + "TokenManager tm) {");
      }
      if (Options.getStatic()) {
        genCodeLine("    if (jj_initialized_once) {");
        genCodeLine("      System.out.println(\"ERROR: Second call to constructor of static parser. \");");
        genCodeLine("      System.out.println(\"       You must either use ReInit() or " +
                "set the JavaCC option STATIC to false\");");
        genCodeLine("      System.out.println(\"       during parser generation.\");");
        genCodeLine("      throw new Error();");
        genCodeLine("    }");
        genCodeLine("    jj_initialized_once = true;");
      }
      genCodeLine("    token_source = tm;");
      genCodeLine("    token = new Token();");
      if (Options.getCacheTokens()) {
        genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
      } else {
        genCodeLine("    jj_ntk = -1;");
      }
      if (Options.getErrorReporting()) {
        genCodeLine("    jj_gen = 0;");
        if (maskindex > 0) {
          genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
        }
        if (jj2index != 0) {
          genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
        }
      }
      genCodeLine("  }");
      genCodeLine("");
      if (Options.getUserTokenManager()) {
        genCodeLine("  /** Reinitialise. */");
        genCodeLine("  public void ReInit(TokenManager tm) {");
      } else {
        genCodeLine("  /** Reinitialise. */");
        genCodeLine("  public void ReInit(" + cu_name + "TokenManager tm) {");
      }
      genCodeLine("    token_source = tm;");
      genCodeLine("    token = new Token();");
      if (Options.getCacheTokens()) {
        genCodeLine("    token.next = jj_nt = token_source.getNextToken();");
      } else {
        genCodeLine("    jj_ntk = -1;");
      }
      if (jjtreeGenerated) {
        genCodeLine("    jjtree.reset();");
      }
      if (Options.getErrorReporting()) {
        genCodeLine("    jj_gen = 0;");
        if (maskindex > 0) {
          genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) jj_la1[i] = -1;");
        }
        if (jj2index != 0) {
          genCodeLine("    for (int i = 0; i < jj_2_rtns.Length; i++) jj_2_rtns[i] = new JJCalls();");
        }
      }
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("  " + staticOpt() + "private Token jj_consume_token(int kind) {");
      if (Options.getCacheTokens()) {
        genCodeLine("    Token oldToken = token;");
        genCodeLine("    if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
        genCodeLine("    else jj_nt = jj_nt.next = token_source.getNextToken();");
      } else {
        genCodeLine("    Token oldToken;");
        genCodeLine("    if ((oldToken = token).next != null) token = token.next;");
        genCodeLine("    else token = token.next = token_source.getNextToken();");
        genCodeLine("    jj_ntk = -1;");
      }
      genCodeLine("    if (token.kind == kind) {");
      if (Options.getErrorReporting()) {
        genCodeLine("      jj_gen++;");
        if (jj2index != 0) {
          genCodeLine("      if (++jj_gc > 100) {");
          genCodeLine("        jj_gc = 0;");
          genCodeLine("        for (int i = 0; i < jj_2_rtns.Length; i++) {");
          genCodeLine("          JJCalls c = jj_2_rtns[i];");
          genCodeLine("          while (c != null) {");
          genCodeLine("            if (c.gen < jj_gen) c.first = null;");
          genCodeLine("            c = c.next;");
          genCodeLine("          }");
          genCodeLine("        }");
          genCodeLine("      }");
        }
      }
      if (Options.getDebugParser()) {
        genCodeLine("      trace_token(token, \"\");");
      }
      genCodeLine("      return token;");
      genCodeLine("    }");
      if (Options.getCacheTokens()) {
        genCodeLine("    jj_nt = token;");
      }
      genCodeLine("    token = oldToken;");
      if (Options.getErrorReporting()) {
        genCodeLine("    jj_kind = kind;");
      }
      genCodeLine("    throw generateParseException();");
      genCodeLine("  }");
      genCodeLine("");
      if (jj2index != 0) {
        genCodeLine("  private class LookaheadSuccess : Exception { }");
        genCodeLine("  " + staticOpt() + "private LookaheadSuccess jj_ls = new LookaheadSuccess();");
        genCodeLine("  " + staticOpt() + "private " + Options.getBooleanType() + " jj_scan_token(int kind) {");
        genCodeLine("    if (jj_scanpos == jj_lastpos) {");
        genCodeLine("      jj_la--;");
        genCodeLine("      if (jj_scanpos.next == null) {");
        genCodeLine("        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();");
        genCodeLine("      } else {");
        genCodeLine("        jj_lastpos = jj_scanpos = jj_scanpos.next;");
        genCodeLine("      }");
        genCodeLine("    } else {");
        genCodeLine("      jj_scanpos = jj_scanpos.next;");
        genCodeLine("    }");
        if (Options.getErrorReporting()) {
          genCodeLine("    if (jj_rescan) {");
          genCodeLine("      int i = 0; Token tok = token;");
          genCodeLine("      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }");
          genCodeLine("      if (tok != null) jj_add_error_token(kind, i);");
          if (Options.getDebugLookahead()) {
            genCodeLine("    } else {");
            genCodeLine("      trace_scan(jj_scanpos, kind);");
          }
          genCodeLine("    }");
        } else if (Options.getDebugLookahead()) {
          genCodeLine("    trace_scan(jj_scanpos, kind);");
        }
        genCodeLine("    if (jj_scanpos.kind != kind) return true;");
        genCodeLine("    if (jj_la == 0 && jj_scanpos == jj_lastpos) return false;");
        genCodeLine("    return false;");
        genCodeLine("  }");
        genCodeLine("");
      }
      genCodeLine("");
      genCodeLine("/** Get the next Token. */");
      genCodeLine("  " + staticOpt() + "public Token getNextToken() {");
      if (Options.getCacheTokens()) {
        genCodeLine("    if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
        genCodeLine("    else jj_nt = jj_nt.next = token_source.getNextToken();");
      } else {
        genCodeLine("    if (token.next != null) token = token.next;");
        genCodeLine("    else token = token.next = token_source.getNextToken();");
        genCodeLine("    jj_ntk = -1;");
      }
      if (Options.getErrorReporting()) {
        genCodeLine("    jj_gen++;");
      }
      if (Options.getDebugParser()) {
        genCodeLine("      trace_token(token, \" (in getNextToken)\");");
      }
      genCodeLine("    return token;");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("/** Get the specific Token. */");
      genCodeLine("  " + staticOpt() + "public Token getToken(int index) {");
      if (lookaheadNeeded) {
        genCodeLine("    Token t = jj_lookingAhead ? jj_scanpos : token;");
      } else {
        genCodeLine("    Token t = token;");
      }
      genCodeLine("    for (int i = 0; i < index; i++) {");
      genCodeLine("      if (t.next != null) t = t.next;");
      genCodeLine("      else t = t.next = token_source.getNextToken();");
      genCodeLine("    }");
      genCodeLine("    return t;");
      genCodeLine("  }");
      genCodeLine("");
      if (!Options.getCacheTokens()) {
        genCodeLine("  " + staticOpt() + "private int jj_ntk_f() {");
        genCodeLine("    if ((jj_nt=token.next) == null)");
        genCodeLine("      return (jj_ntk = (token.next=token_source.getNextToken()).kind);");
        genCodeLine("    else");
        genCodeLine("      return (jj_ntk = jj_nt.kind);");
        genCodeLine("  }");
        genCodeLine("");
      }
      if (Options.getErrorReporting()) {
        if (!Options.getGenerateGenerics())
          genCodeLine("  " + staticOpt() + "private System.Collections.ArrayList jj_expentries = new System.Collections.ArrayList();");
        else
          genCodeLine("  " + staticOpt() +
                  "private System.Collections.Generic.List<int[]> jj_expentries = new System.Collections.Generic.List<int[]>();");
        genCodeLine("  " + staticOpt() + "private int[] jj_expentry;");
        genCodeLine("  " + staticOpt() + "private int jj_kind = -1;");
        if (jj2index != 0) {
          genCodeLine("  " + staticOpt() + "private int[] jj_lasttokens = new int[100];");
          genCodeLine("  " + staticOpt() + "private int jj_endpos;");
          genCodeLine("");
          genCodeLine("  " + staticOpt() + "private void jj_add_error_token(int kind, int pos) {");
          genCodeLine("    if (pos >= 100) return;");
          genCodeLine("    if (pos == jj_endpos + 1) {");
          genCodeLine("      jj_lasttokens[jj_endpos++] = kind;");
          genCodeLine("    } else if (jj_endpos != 0) {");
          genCodeLine("      jj_expentry = new int[jj_endpos];");
          genCodeLine("      for (int i = 0; i < jj_endpos; i++) {");
          genCodeLine("        jj_expentry[i] = jj_lasttokens[i];");
          genCodeLine("      }");
          genCodeLine("      foreach (int[] it in jj_expentries) {");
          genCodeLine("        int[] oldentry = it;");
          genCodeLine("        if (oldentry.Length == jj_expentry.Length) {");
          genCodeLine("          for (int i = 0; i < jj_expentry.Length; i++) {");
          genCodeLine("            if (oldentry[i] != jj_expentry[i]) {");
          genCodeLine("              continue;");
          genCodeLine("            }");
          genCodeLine("          }");
          genCodeLine("          jj_expentries.Add(jj_expentry);");
          genCodeLine("          goto jj_entries_loop;");
          genCodeLine("        }");
          genCodeLine("      }");
          genCodeLine("  jj_entries_loop:");
          genCodeLine("      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;");
          genCodeLine("    }");
          genCodeLine("  }");
        }
        genCodeLine("");
        genCodeLine("  /** Generate ParseException. */");
        genCodeLine("  " + staticOpt() + "public ParseException generateParseException() {");
        genCodeLine("    jj_expentries.Clear();");
        genCodeLine("    " + Options.getBooleanType() + "[] la1tokens = new " + Options.getBooleanType() + "[" + tokenCount + "];");
        genCodeLine("    if (jj_kind >= 0) {");
        genCodeLine("      la1tokens[jj_kind] = true;");
        genCodeLine("      jj_kind = -1;");
        genCodeLine("    }");
        genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) {");
        genCodeLine("      if (jj_la1[i] == jj_gen) {");
        genCodeLine("        for (int j = 0; j < 32; j++) {");
        for (int i = 0; i < (tokenCount-1)/32 + 1; i++) {
          genCodeLine("          if ((jj_la1_" + i + "[i] & (1<<j)) != 0) {");
          genCode("            la1tokens[");
          if (i != 0) {
            genCode((32*i) + "+");
          }
          genCodeLine("j] = true;");
          genCodeLine("          }");
        }
        genCodeLine("        }");
        genCodeLine("      }");
        genCodeLine("    }");
        genCodeLine("    for (int i = 0; i < " + tokenCount + "; i++) {");
        genCodeLine("      if (la1tokens[i]) {");
        genCodeLine("        jj_expentry = new int[1];");
        genCodeLine("        jj_expentry[0] = i;");
        genCodeLine("        jj_expentries.Add(jj_expentry);");
        genCodeLine("      }");
        genCodeLine("    }");
        if (jj2index != 0) {
          genCodeLine("    jj_endpos = 0;");
          genCodeLine("    jj_rescan_token();");
          genCodeLine("    jj_add_error_token(0, 0);");
        }
        genCodeLine("    int[][] exptokseq = new int[jj_expentries.Count][];");
        genCodeLine("    for (int i = 0; i < jj_expentries.Count; i++) {");
        if (!Options.getGenerateGenerics())
           genCodeLine("      exptokseq[i] = (int[])jj_expentries[i];");
        else
           genCodeLine("      exptokseq[i] = jj_expentries[i];");
        genCodeLine("    }");
        genCodeLine("    return new ParseException(token, exptokseq, tokenImage);");
        genCodeLine("  }");
      } else {
        genCodeLine("  /** Generate ParseException. */");
        genCodeLine("  " + staticOpt() + "public ParseException generateParseException() {");
        genCodeLine("    Token errortok = token.next;");
        if (Options.getKeepLineColumn())
           genCodeLine("    int line = errortok.beginLine, column = errortok.beginColumn;");
        genCodeLine("    String mess = (errortok.kind == 0) ? tokenImage[0] : errortok.image;");
        if (Options.getKeepLineColumn())
           genCodeLine("    return new ParseException(" +
               "\"Parse error at line \" + line + \", column \" + column + \".  " +
               "Encountered: \" + mess);");
        else
           genCodeLine("    return new ParseException(\"Parse error at <unknown location>.  " +
                   "Encountered: \" + mess);");
        genCodeLine("  }");
      }
      genCodeLine("");

      if (Options.getDebugParser()) {
        genCodeLine("  " + staticOpt() + "private int trace_indent = 0;");
        genCodeLine("  " + staticOpt() + "private " + Options.getBooleanType() + " trace_enabled = true;");
        genCodeLine("");
        genCodeLine("/** Enable tracing. */");
        genCodeLine("  " + staticOpt() + "public void enable_tracing() {");
        genCodeLine("    trace_enabled = true;");
        genCodeLine("  }");
        genCodeLine("");
        genCodeLine("/** Disable tracing. */");
        genCodeLine("  " + staticOpt() + "public void disable_tracing() {");
        genCodeLine("    trace_enabled = false;");
        genCodeLine("  }");
        genCodeLine("");
        genCodeLine("  " + staticOpt() + "private void trace_call(String s) {");
        genCodeLine("    if (trace_enabled) {");
        genCodeLine("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        genCodeLine("      System.out.println(\"Call:   \" + s);");
        genCodeLine("    }");
        genCodeLine("    trace_indent = trace_indent + 2;");
        genCodeLine("  }");
        genCodeLine("");
        genCodeLine("  " + staticOpt() + "private void trace_return(String s) {");
        genCodeLine("    trace_indent = trace_indent - 2;");
        genCodeLine("    if (trace_enabled) {");
        genCodeLine("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        genCodeLine("      System.out.println(\"Return: \" + s);");
        genCodeLine("    }");
        genCodeLine("  }");
        genCodeLine("");
        genCodeLine("  " + staticOpt() + "private void trace_token(Token t, String where) {");
        genCodeLine("    if (trace_enabled) {");
        genCodeLine("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        genCodeLine("      System.out.print(\"Consumed token: <\" + tokenImage[t.kind]);");
        genCodeLine("      if (t.kind != 0 && !tokenImage[t.kind].equals(\"\\\"\" + t.image + \"\\\"\")) {");
        genCodeLine("        System.out.print(\": \\\"\" + t.image + \"\\\"\");");
        genCodeLine("      }");
        genCodeLine("      System.out.println(\" at line \" + t.beginLine + " +
                "\" column \" + t.beginColumn + \">\" + where);");
        genCodeLine("    }");
        genCodeLine("  }");
        genCodeLine("");
        genCodeLine("  " + staticOpt() + "private void trace_scan(Token t1, int t2) {");
        genCodeLine("    if (trace_enabled) {");
        genCodeLine("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        genCodeLine("      System.out.print(\"Visited token: <\" + tokenImage[t1.kind]);");
        genCodeLine("      if (t1.kind != 0 && !tokenImage[t1.kind].equals(\"\\\"\" + t1.image + \"\\\"\")) {");
        genCodeLine("        System.out.print(\": \\\"\" + t1.image + \"\\\"\");");
        genCodeLine("      }");
        genCodeLine("      System.out.println(\" at line \" + t1.beginLine + \"" +
                " column \" + t1.beginColumn + \">; Expected token: <\" + tokenImage[t2] + \">\");");
        genCodeLine("    }");
        genCodeLine("  }");
        genCodeLine("");
      } else {
        genCodeLine("  /** Enable tracing. */");
        genCodeLine("  " + staticOpt() + "public void enable_tracing() {");
        genCodeLine("  }");
        genCodeLine("");
        genCodeLine("  /** Disable tracing. */");
        genCodeLine("  " + staticOpt() + "public void disable_tracing() {");
        genCodeLine("  }");
        genCodeLine("");
      }

      if (jj2index != 0 && Options.getErrorReporting()) {
        genCodeLine("  " + staticOpt() + "private void jj_rescan_token() {");
        genCodeLine("    jj_rescan = true;");
        genCodeLine("    for (int i = 0; i < " + jj2index + "; i++) {");
        genCodeLine("    try {");
        genCodeLine("      JJCalls p = jj_2_rtns[i];");
        genCodeLine("      do {");
        genCodeLine("        if (p.gen > jj_gen) {");
        genCodeLine("          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;");
        genCodeLine("          switch (i) {");
        for (int i = 0; i < jj2index; i++) {
          genCodeLine("            case " + i + ": jj_3_" + (i+1) + "(); break;");
        }
        genCodeLine("          }");
        genCodeLine("        }");
        genCodeLine("        p = p.next;");
        genCodeLine("      } while (p != null);");
        genCodeLine("      } catch(LookaheadSuccess ) { }");
        genCodeLine("    }");
        genCodeLine("    jj_rescan = false;");
        genCodeLine("  }");
        genCodeLine("");
        genCodeLine("  " + staticOpt() + "private void jj_save(int index, int xla) {");
        genCodeLine("    JJCalls p = jj_2_rtns[index];");
        genCodeLine("    while (p.gen > jj_gen) {");
        genCodeLine("      if (p.next == null) { p = p.next = new JJCalls(); break; }");
        genCodeLine("      p = p.next;");
        genCodeLine("    }");
        genCodeLine("    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;");
        genCodeLine("  }");
        genCodeLine("");
      }

      if (jj2index != 0 && Options.getErrorReporting()) {
        genCodeLine("  class JJCalls {");
        genCodeLine("    public int gen;");
        genCodeLine("    public Token first;");
        genCodeLine("    public int arg;");
        genCodeLine("    public JJCalls next;");
        genCodeLine("  }");
        genCodeLine("");
      }

      if (cu_from_insertion_point_2.size() != 0) {
        printTokenSetup((Token)(cu_from_insertion_point_2.get(0))); ccol = 1;
        for (Iterator it = cu_from_insertion_point_2.iterator(); it.hasNext();) {
          t = (Token)it.next();
          printToken(t);
        }
        printTrailingComments(t);
      }

      genCodeLine(" }");

    if (Options.stringValue("NAMESPACE").length() > 0) {
      genCodeLine("}");
    }
      saveOutput(Options.getOutputDirectory() + File.separator + cu_name + getFileExtension(Options.getOutputLanguage()));

    } // matches "if (Options.getBuildParser())"

  }

   public static void reInit()
   {
      lookaheadNeeded = false;
   }

}
