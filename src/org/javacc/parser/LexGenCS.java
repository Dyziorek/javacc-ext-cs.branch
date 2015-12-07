/*
This module is part of the SoftIWS system
Copyright (c)SCC, Inc.  2010
All Rights Reserved

This document contains unpublished, confidential and proprietary
information of SCC, Inc. No disclosure or use of
any portion of the contents of these materials may be made without the
express written consent of Soft Computer Consultants, Inc.

Author:    Daniel Dus <ddus@softsystem.pl>

Created:   03 Sep 2012
 */
package org.javacc.parser;

import java.io.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.javacc.utils.JavaFileGenerator;
import static org.javacc.parser.JavaCCGlobals.*;

/**
 *
 * @author Daniel Dus <ddus@softsystem.pl>
 */
public class LexGenCS extends LexGen {
    @Override
  void PrintClassHead()
  {
    int i, j;

    List tn = new ArrayList(toolNames);
    tn.add(toolName);

    genCodeLine("/* " + getIdString(tn, tokMgrClassName + getFileExtension(Options.getOutputLanguage())) + " */");
        
    genCodeLine("using System;");
    genCodeLine("using System.Diagnostics;");


    int l = 0, kind;
    i = 1;
    for (;;)
    {
      if (cu_to_insertion_point_1.size() <= l)
        break;

      kind = ((Token)cu_to_insertion_point_1.get(l)).kind;
      if(kind == PACKAGE || kind == IMPORT) {
        for (; i < cu_to_insertion_point_1.size(); i++) {
          kind = ((Token)cu_to_insertion_point_1.get(i)).kind;
          if (kind == SEMICOLON ||
              kind == ABSTRACT ||
              kind == FINAL ||
              kind == PUBLIC ||
              kind == CLASS ||
              kind == INTERFACE)
          {
            cline = ((Token)(cu_to_insertion_point_1.get(l))).beginLine;
            ccol = ((Token)(cu_to_insertion_point_1.get(l))).beginColumn;
            for (j = l; j < i; j++) {
              printToken((Token)(cu_to_insertion_point_1.get(j)));
            }
            if (kind == SEMICOLON)
              printToken((Token)(cu_to_insertion_point_1.get(j)));
            genCodeLine("");
            break;
          }
        }
        l = ++i;
      }
      else
        break;
    }

    genCodeLine("");
    genCodeLine("/** Token Manager. */");
    //genCodeLine("@SuppressWarnings(\"unused\")");
    //genAnnotation("SuppressWarnings(\"unused\")");
    if(Options.getSupportClassVisibilityPublic()) {
    	//genModifier("public ");
    	genModifier("public ");
    }
    //genCodeLine("class " + tokMgrClassName + " implements " +
    		//cu_name + "Constants");
    //String superClass = Options.stringValue("TOKEN_MANAGER_SUPER_CLASS");
    genClassStart(null, tokMgrClassName, new String[]{}, new String[]{cu_name + "Constants"});
    //genCodeLine("{"); // }

    if (token_mgr_decls != null && token_mgr_decls.size() > 0)
    {
      Token t = (Token)token_mgr_decls.get(0);
      boolean commonTokenActionSeen = false;
      boolean commonTokenActionNeeded = Options.getCommonTokenAction();

      printTokenSetup((Token)token_mgr_decls.get(0));
      ccol = 1;

      for (j = 0; j < token_mgr_decls.size(); j++)
      {
        t = (Token)token_mgr_decls.get(j);
        if (t.kind == IDENTIFIER &&
            commonTokenActionNeeded &&
            !commonTokenActionSeen)
          commonTokenActionSeen = t.image.equals("CommonTokenAction");

        printToken(t);
      }

      genCodeLine("");
      if (commonTokenActionNeeded && !commonTokenActionSeen)
        JavaCCErrors.warning("You have the COMMON_TOKEN_ACTION option set. " +
            "But it appears you have not defined the method :\n"+
            "      " + staticString + "void CommonTokenAction(Token t)\n" +
        "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");

    }
    else if (Options.getCommonTokenAction())
    {
      JavaCCErrors.warning("You have the COMMON_TOKEN_ACTION option set. " +
          "But you have not defined the method :\n"+
          "      " + staticString + "void CommonTokenAction(Token t)\n" +
      "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");
    }

    genCodeLine("");
    genCodeLine("  /** Debug output. */");
    genCodeLine("  public " + staticString + " System.IO.TextWriter debugStream = System.Console.Out;");
    genCodeLine("  /** Set debug output. */");
    genCodeLine("  public " + staticString + " void setDebugStream(System.IO.TextWriter ds) { debugStream = ds; }");

    if(Options.getTokenManagerUsesParser() && !Options.getStatic()){
      genCodeLine("");
      genCodeLine("  /** The parser. */");
      genCodeLine("  public " + cu_name + " parser = null;");
    }
  }
  
  void DumpDebugMethods() throws IOException
  {
    writeTemplate("/templates/cs/DumpDebugMethods.template",
          "maxOrdinal", maxOrdinal,
          "stateSetSize", stateSetSize);
  }

  static void BuildLexStatesTable()
  {
    Iterator it = rexprlist.iterator();
    TokenProduction tp;
    int i;

    String[] tmpLexStateName = new String[lexstate_I2S.size()];
    while (it.hasNext())
    {
      tp = (TokenProduction)it.next();
      List respecs = tp.respecs;
      List tps;

      for (i = 0; i < tp.lexStates.length; i++)
      {
        if ((tps = (List)allTpsForState.get(tp.lexStates[i])) == null)
        {
          tmpLexStateName[maxLexStates++] = tp.lexStates[i];
          allTpsForState.put(tp.lexStates[i], tps = new ArrayList());
        }

        tps.add(tp);
      }

      if (respecs == null || respecs.size() == 0)
        continue;

      RegularExpression re;
      for (i = 0; i < respecs.size(); i++)
        if (maxOrdinal <= (re = ((RegExprSpec)respecs.get(i)).rexp).ordinal)
          maxOrdinal = re.ordinal + 1;
    }

    kinds = new int[maxOrdinal];
    toSkip = new long[maxOrdinal / 64 + 1];
    toSpecial = new long[maxOrdinal / 64 + 1];
    toMore = new long[maxOrdinal / 64 + 1];
    toToken = new long[maxOrdinal / 64 + 1];
    toToken[0] = 1L;
    actions = new Action[maxOrdinal];
    actions[0] = actForEof;
    hasTokenActions = actForEof != null;
    initStates = new Hashtable();
    canMatchAnyChar = new int[maxLexStates];
    canLoop = new boolean[maxLexStates];
    stateHasActions = new boolean[maxLexStates];
    lexStateName = new String[maxLexStates];
    singlesToSkip = new NfaState[maxLexStates];
    System.arraycopy(tmpLexStateName, 0, lexStateName, 0, maxLexStates);

    for (i = 0; i < maxLexStates; i++)
      canMatchAnyChar[i] = -1;

    hasNfa = new boolean[maxLexStates];
    mixed = new boolean[maxLexStates];
    maxLongsReqd = new int[maxLexStates];
    initMatch = new int[maxLexStates];
    newLexState = new String[maxOrdinal];
    newLexState[0] = nextStateForEof;
    hasEmptyMatch = false;
    lexStates = new int[maxOrdinal];
    ignoreCase = new boolean[maxOrdinal];
    rexprs = new RegularExpression[maxOrdinal];
    RStringLiteral.allImages = new String[maxOrdinal];
    canReachOnMore = new boolean[maxLexStates];
  }

  static int GetIndex(String name)
  {
    for (int i = 0; i < lexStateName.length; i++)
      if (lexStateName[i] != null && lexStateName[i].equals(name))
        return i;

    throw new Error(); // Should never come here
  }

  public static void AddCharToSkip(char c, int kind)
  {
    singlesToSkip[lexStateIndex].AddChar(c);
    singlesToSkip[lexStateIndex].kind = kind;
  }

  public void start() throws IOException
  {
    if (!Options.getBuildTokenManager() ||
        Options.getUserTokenManager() ||
        JavaCCErrors.get_error_count() > 0)
      return;

    keepLineCol = Options.getKeepLineColumn();
    List choices = new ArrayList();
    Enumeration e;
    TokenProduction tp;
    int i, j;

    staticString = (Options.getStatic() ? "static " : "");
    
    if (Options.stringValue("NAMESPACE").length() > 0) {
      genCodeLine("namespace " + Options.stringValue("NAMESPACE") +  "\n {");
    }

    
    tokMgrClassName = cu_name + "TokenManager";

    PrintClassHead();
    BuildLexStatesTable();

    e = allTpsForState.keys();

    boolean ignoring = false;

    while (e.hasMoreElements())
    {
      NfaState.ReInit();
      RStringLiteral.ReInit();

      String key = (String)e.nextElement();

      lexStateIndex = GetIndex(key);
      lexStateSuffix = "_" + lexStateIndex;
      List allTps = (List)allTpsForState.get(key);
      initStates.put(key, initialState = new NfaState());
      ignoring = false;

      singlesToSkip[lexStateIndex] = new NfaState();
      singlesToSkip[lexStateIndex].dummy = true;

      if (key.equals("DEFAULT"))
        defaultLexState = lexStateIndex;

      for (i = 0; i < allTps.size(); i++)
      {
        tp = (TokenProduction)allTps.get(i);
        int kind = tp.kind;
        boolean ignore = tp.ignoreCase;
        List rexps = tp.respecs;

        if (i == 0)
          ignoring = ignore;

        for (j = 0; j < rexps.size(); j++)
        {
          RegExprSpec respec = (RegExprSpec)rexps.get(j);
          curRE = respec.rexp;

          rexprs[curKind = curRE.ordinal] = curRE;
          lexStates[curRE.ordinal] = lexStateIndex;
          ignoreCase[curRE.ordinal] = ignore;

          if (curRE.private_rexp)
          {
            kinds[curRE.ordinal] = -1;
            continue;
          }

          if (curRE instanceof RStringLiteral &&
              !((RStringLiteral)curRE).image.equals(""))
          {
            ((RStringLiteral)curRE).GenerateDfa(this, curRE.ordinal);
            if (i != 0 && !mixed[lexStateIndex] && ignoring != ignore)
              mixed[lexStateIndex] = true;
          }
          else if (curRE.CanMatchAnyChar())
          {
            if (canMatchAnyChar[lexStateIndex] == -1 ||
                canMatchAnyChar[lexStateIndex] > curRE.ordinal)
              canMatchAnyChar[lexStateIndex] = curRE.ordinal;
          }
          else
          {
            Nfa temp;

            if (curRE instanceof RChoice)
              choices.add(curRE);

            temp = curRE.GenerateNfa(ignore);
            temp.end.isFinal = true;
            temp.end.kind = curRE.ordinal;
            initialState.AddMove(temp.start);
          }

          if (kinds.length < curRE.ordinal)
          {
            int[] tmp = new int[curRE.ordinal + 1];

            System.arraycopy(kinds, 0, tmp, 0, kinds.length);
            kinds = tmp;
          }
          //System.out.println("   ordina : " + curRE.ordinal);

          kinds[curRE.ordinal] = kind;

          if (respec.nextState != null &&
              !respec.nextState.equals(lexStateName[lexStateIndex]))
            newLexState[curRE.ordinal] = respec.nextState;

          if (respec.act != null && respec.act.getActionTokens() != null &&
              respec.act.getActionTokens().size() > 0)
            actions[curRE.ordinal] = respec.act;

          switch(kind)
          {
          case TokenProduction.SPECIAL :
            hasSkipActions |= (actions[curRE.ordinal] != null) ||
            (newLexState[curRE.ordinal] != null);
            hasSpecial = true;
            toSpecial[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
            toSkip[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
            break;
          case TokenProduction.SKIP :
            hasSkipActions |= (actions[curRE.ordinal] != null);
            hasSkip = true;
            toSkip[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
            break;
          case TokenProduction.MORE :
            hasMoreActions |= (actions[curRE.ordinal] != null);
            hasMore = true;
            toMore[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);

            if (newLexState[curRE.ordinal] != null)
              canReachOnMore[GetIndex(newLexState[curRE.ordinal])] = true;
            else
              canReachOnMore[lexStateIndex] = true;

            break;
          case TokenProduction.TOKEN :
            hasTokenActions |= (actions[curRE.ordinal] != null);
            toToken[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
            break;
          }
        }
      }

      // Generate a static block for initializing the nfa transitions
      NfaState.ComputeClosures();

      for (i = 0; i < initialState.epsilonMoves.size(); i++)
        ((NfaState)initialState.epsilonMoves.elementAt(i)).GenerateCode();

      if (hasNfa[lexStateIndex] = (NfaState.generatedStates != 0))
      {
        initialState.GenerateCode();
        initialState.GenerateInitMoves(this);
      }

      if (initialState.kind != Integer.MAX_VALUE && initialState.kind != 0)
      {
        if ((toSkip[initialState.kind / 64] & (1L << initialState.kind)) != 0L ||
            (toSpecial[initialState.kind / 64] & (1L << initialState.kind)) != 0L)
          hasSkipActions = true;
        else if ((toMore[initialState.kind / 64] & (1L << initialState.kind)) != 0L)
          hasMoreActions = true;
        else
          hasTokenActions = true;

        if (initMatch[lexStateIndex] == 0 ||
            initMatch[lexStateIndex] > initialState.kind)
        {
          initMatch[lexStateIndex] = initialState.kind;
          hasEmptyMatch = true;
        }
      }
      else if (initMatch[lexStateIndex] == 0)
        initMatch[lexStateIndex] = Integer.MAX_VALUE;

      RStringLiteral.FillSubString();

      if (hasNfa[lexStateIndex] && !mixed[lexStateIndex])
        RStringLiteral.GenerateNfaStartStates(this, initialState);

      RStringLiteral.DumpDfaCode(this);

      if (hasNfa[lexStateIndex])
        NfaState.DumpMoveNfa(this);

      if (stateSetSize < NfaState.generatedStates)
        stateSetSize = NfaState.generatedStates;
    }

    for (i = 0; i < choices.size(); i++)
      ((RChoice)choices.get(i)).CheckUnmatchability();

    NfaState.DumpStateSets(this);
    CheckEmptyStringMatch();
    NfaState.DumpNonAsciiMoveMethods(this);
    RStringLiteral.DumpStrLiteralImages(this);
    DumpFillToken();
    DumpGetNextToken();

    if (Options.getDebugTokenManager())
    {
      NfaState.DumpStatesForKind(this);
      DumpDebugMethods();
    }

    if (hasLoop)
    {
      switchToStaticsFile();
      genCodeLine("static int jjemptyLineNo[" + maxLexStates + "];");
      genCodeLine("static int  jjemptyColNo[" + maxLexStates + "];");
      genCodeLine("static bool jjbeenHere[" + maxLexStates + "];");
      switchToMainFile();
    }

    if (hasSkipActions)
      DumpSkipActions();
    if (hasMoreActions)
      DumpMoreActions();
    if (hasTokenActions)
      DumpTokenActions();

    NfaState.PrintBoilerPlate(this);

    String charStreamName;
    if (Options.getUserCharStream())
      charStreamName = "CharStream";
    else
    {
      if (Options.getJavaUnicodeEscape())
        charStreamName = "CSCharStream";
      else
        charStreamName = "SimpleCharStream";
    }

    writeTemplate("/templates/cs/TokenManagerBoilerPlateMethods.template",
      "charStreamName", charStreamName,
      "parserClassName", cu_name,
      "defaultLexState", "defaultLexState",
      "lexStateNameLength", lexStateName.length);

    //dumpBoilerPlateInHeader();

    // in the include file close the class signature
    DumpStaticVarDeclarations(charStreamName); // static vars actually inst

    genCodeLine(/*{*/ "}");
    String fileName = Options.getOutputDirectory() + File.separator +
                      tokMgrClassName +
                      getFileExtension(Options.getOutputLanguage());
    saveOutput(fileName);
  }

  private void DumpStaticVarDeclarations(String charStreamName) throws IOException
  {
    int i;

    genCodeLine("");
    genCodeLine("/** Lexer state names. */");
    genCodeLine("public static String[] lexStateNames = {");
    for (i = 0; i < maxLexStates; i++)
      genCodeLine("   \"" + lexStateName[i] + "\",");
    genCodeLine("};");

    if (maxLexStates > 1)
    {
      genCodeLine("");
      genCodeLine("/** Lex State array. */");
      genCode("public static int[] jjnewLexState = {");

      for (i = 0; i < maxOrdinal; i++)
      {
        if (i % 25 == 0)
          genCode("\n   ");

        if (newLexState[i] == null)
          genCode("-1, ");
        else
          genCode(GetIndex(newLexState[i]) + ", ");
      }
      genCodeLine("\n};");
    }

    if (hasSkip || hasMore || hasSpecial)
    {
      // Bit vector for TOKEN
      genCode("static ulong[] jjtoToken = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode("\n   ");
        genCode("0x" + Long.toHexString(toToken[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    if (hasSkip || hasSpecial)
    {
      // Bit vector for SKIP
      genCode("static ulong[] jjtoSkip = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode("\n   ");
        genCode("0x" + Long.toHexString(toSkip[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    if (hasSpecial)
    {
      // Bit vector for SPECIAL
      genCode("static ulong[] jjtoSpecial = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode("\n   ");
        genCode("0x" + Long.toHexString(toSpecial[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    if (hasMore)
    {
      // Bit vector for MORE
      genCode("static  ulong[] jjtoMore = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode("\n   ");
        genCode("0x" + Long.toHexString(toMore[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    writeTemplate("/templates/cs/DumpStaticVarDeclarations.template",
      "charStreamName", charStreamName,
      "protected", isJavaLanguage() ? "protected" : "",
      "private", isJavaLanguage() ? "private" : "",
      "final", isJavaLanguage() ? "sealed" : "",
      "lexStateNameLength", lexStateName.length);
    
   if (Options.stringValue("NAMESPACE").length() > 0) {
      genCodeLine(" }");
    }

    
  }

  void DumpFillToken()
  {
    final double tokenVersion = CSFiles.getVersion("Token.cs");
    final boolean hasBinaryNewToken = tokenVersion > 4.09;

    genCodeLine(staticString + "protected Token jjFillToken()");
    genCodeLine("{");
    genCodeLine("    Token t;");
    genCodeLine("    String curTokenImage;");
    if (keepLineCol)
    {
      genCodeLine("    int beginLine;");
      genCodeLine("    int endLine;");
      genCodeLine("    int beginColumn;");
      genCodeLine("    int endColumn;");
    }

    if (hasEmptyMatch)
    {
      genCodeLine("   if (jjmatchedPos < 0)");
      genCodeLine("   {");
      genCodeLine("      if (image == null)");
      genCodeLine("         curTokenImage = \"\";");
      genCodeLine("      else");
      genCodeLine("         curTokenImage = image.ToString();");

      if (keepLineCol)
      {
        genCodeLine("      beginLine = endLine = input_stream.getBeginLine();");
        genCodeLine("      beginColumn = endColumn = input_stream.getBeginColumn();");
      }

      genCodeLine("   }");
      genCodeLine("   else");
      genCodeLine("   {");
      genCodeLine("      String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("      curTokenImage = (im == null) ? input_stream.GetImage() : im;");

      if (keepLineCol)
      {
        genCodeLine("      beginLine = input_stream.getBeginLine();");
        genCodeLine("      beginColumn = input_stream.getBeginColumn();");
        genCodeLine("      endLine = input_stream.getEndLine();");
        genCodeLine("      endColumn = input_stream.getEndColumn();");
      }

      genCodeLine("   }");
    }
    else
    {
      genCodeLine("   String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("   curTokenImage = (im == null) ? input_stream.GetImage() : im;");
      if (keepLineCol)
      {
        genCodeLine("   beginLine = input_stream.getBeginLine();");
        genCodeLine("   beginColumn = input_stream.getBeginColumn();");
        genCodeLine("   endLine = input_stream.getEndLine();");
        genCodeLine("   endColumn = input_stream.getEndColumn();");
      }
    }

    if (Options.getTokenFactory().length() > 0) {
      genCodeLine("   t = " + Options.getTokenFactory() + ".newToken(jjmatchedKind, curTokenImage);");
    } else if (hasBinaryNewToken)
    {
      genCodeLine("   t = Token.newToken(jjmatchedKind, curTokenImage);");
    }
    else
    {
      genCodeLine("   t = Token.newToken(jjmatchedKind);");
      genCodeLine("   t.kind = jjmatchedKind;");
      genCodeLine("   t.image = curTokenImage;");
    }

    if (keepLineCol) {
      genCodeLine("");
      genCodeLine("   t.beginLine = beginLine;");
      genCodeLine("   t.endLine = endLine;");
      genCodeLine("   t.beginColumn = beginColumn;");
      genCodeLine("   t.endColumn = endColumn;");
    }

    genCodeLine("");
    genCodeLine("   return t;");
    genCodeLine("}");
  }

  void DumpGetNextToken()
  {
    int i;

    genCodeLine("");
    genCodeLine(staticString + "int curLexState = " + defaultLexState + ";");
    genCodeLine(staticString + "int defaultLexState = " + defaultLexState + ";");
    genCodeLine(staticString + "int jjnewStateCnt;");
    genCodeLine(staticString + "uint jjround;");
    genCodeLine(staticString + "int jjmatchedPos;");
    genCodeLine(staticString + "int jjmatchedKind;");
    genCodeLine("");
    genCodeLine("/** Get the next Token. */");
    genCodeLine("public " + staticString + "Token getNextToken()" +
    " ");
    genCodeLine("{");
    if (hasSpecial) {
      genCodeLine("  Token specialToken = null;");
    }
    genCodeLine("  Token matchedToken;");
    genCodeLine("  int curPos = 0;");
    genCodeLine("");
    genCodeLine("  EOFLoop :\n  for (;;)");
    genCodeLine("  {");
    genCodeLine("   if (!input_stream.BeginToken(ref curChar))");
    genCodeLine("   {");

    if (Options.getDebugTokenManager())
      genCodeLine("      debugStream.WriteLine(\"Returning the <EOF> token.\\n\");");

    genCodeLine("      jjmatchedKind = 0;");
    genCodeLine("      jjmatchedPos = -1;");
    genCodeLine("      matchedToken = jjFillToken();");

    if (hasSpecial)
      genCodeLine("      matchedToken.specialToken = specialToken;");

    if (nextStateForEof != null || actForEof != null)
      genCodeLine("      TokenLexicalActions(matchedToken);");

    if (Options.getCommonTokenAction())
      genCodeLine("      CommonTokenAction(matchedToken);");

    genCodeLine("      return matchedToken;");
    genCodeLine("   }");

    if (hasMoreActions || hasSkipActions || hasTokenActions)
    {
      genCodeLine("   image = jjimage;");
      genCodeLine("   image.Length = 0;");
      genCodeLine("   jjimageLen = 0;");
    }

    genCodeLine("");

    String prefix = "";
    if (hasMore)
    {
      genCodeLine("   for (;;)");
      genCodeLine("   {");
      prefix = "  ";
    }

    String endSwitch = "";
    String caseStr = "";
    // this also sets up the start state of the nfa
    if (maxLexStates > 1)
    {
      genCodeLine(prefix + "   switch(curLexState)");
      genCodeLine(prefix + "   {");
      endSwitch = prefix + "   }";
      caseStr = prefix + "     case ";
      prefix += "    ";
    }

    prefix += "   ";
    for(i = 0; i < maxLexStates; i++)
    {
      if (maxLexStates > 1)
        genCodeLine(caseStr + i + ":");

      if (singlesToSkip[i].HasTransitions())
      {
        // added the backup(0) to make JIT happy
        genCodeLine(prefix + "try { input_stream.backup(0);");
        if (singlesToSkip[i].asciiMoves[0] != 0L &&
            singlesToSkip[i].asciiMoves[1] != 0L)
        {
          genCodeLine(prefix + "   while ((curChar < 64" + " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
              "UL & (1UL << curChar)) != 0L) || \n" +
              prefix + "          (curChar >> 6) == 1" +
              " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
          "UL & (1UL << (curChar & 0x3F))) != 0UL)");
        }
        else if (singlesToSkip[i].asciiMoves[1] == 0L)
        {
          genCodeLine(prefix + "   while (curChar <= " +
              (int)MaxChar(singlesToSkip[i].asciiMoves[0]) + " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
          "UL & (1UL << curChar)) != 0UL)");
        }
        else if (singlesToSkip[i].asciiMoves[0] == 0L)
        {
          genCodeLine(prefix + "   while (curChar > 63 && curChar <= " +
              ((int)MaxChar(singlesToSkip[i].asciiMoves[1]) + 64) +
              " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
          "UL & (1UL << (curChar & 0x3F))) != 0UL)");
        }

        if (Options.getDebugTokenManager())
        {
          genCodeLine(prefix + "{");
          genCodeLine("      debugStream.WriteLine(" +
              (maxLexStates > 1 ?
                  "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                  "\"Skipping character : \" + " +
          "TokenMgrError.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \")\");");
        }
        genCodeLine(prefix + "      curChar = input_stream.BeginToken();");

        if (Options.getDebugTokenManager())
          genCodeLine(prefix + "}");

        genCodeLine(prefix + "}");
        genCodeLine(prefix + "catch (System.IO.IOException e1) { goto EOFLoop; }");
      }

      if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0)
      {
        if (Options.getDebugTokenManager())
          genCodeLine("      debugStream.WriteLine(\"   Matched the empty string as \" + tokenImage[" +
              initMatch[i] + "] + \" token.\");");

        genCodeLine(prefix + "jjmatchedKind = " + initMatch[i] + ";");
        genCodeLine(prefix + "jjmatchedPos = -1;");
        genCodeLine(prefix + "curPos = 0;");
      }
      else
      {
        genCodeLine(prefix + "jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        genCodeLine(prefix + "jjmatchedPos = 0;");
      }

      if (Options.getDebugTokenManager())
        genCodeLine("      debugStream.WriteLine(" +
            (maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
            "\"Current character : \" + " +
            "TokenMgrError.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
        "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");

      genCodeLine(prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");

      if (canMatchAnyChar[i] != -1)
      {
        if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0)
          genCodeLine(prefix + "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " +
              canMatchAnyChar[i] + "))");
        else
          genCodeLine(prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " +
              canMatchAnyChar[i] + ")");
        genCodeLine(prefix + "{");

        if (Options.getDebugTokenManager())
          genCodeLine("           debugStream.WriteLine(\"   Current character matched as a \" + tokenImage[" +
              canMatchAnyChar[i] + "] + \" token.\");");
        genCodeLine(prefix + "   jjmatchedKind = " + canMatchAnyChar[i] + ";");

        if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0)
          genCodeLine(prefix + "   jjmatchedPos = 0;");

        genCodeLine(prefix + "}");
      }

      if (maxLexStates > 1)
        genCodeLine(prefix + "break;");
    }

    if (maxLexStates > 1)
      genCodeLine(endSwitch);
    else if (maxLexStates == 0)
      genCodeLine("       jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");

    if (maxLexStates > 1)
      prefix = "  ";
    else
      prefix = "";

    if (maxLexStates > 0)
    {
      genCodeLine(prefix + "   if (jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
      genCodeLine(prefix + "   {");
      genCodeLine(prefix + "      if (jjmatchedPos + 1 < curPos)");

      if (Options.getDebugTokenManager())
      {
        genCodeLine(prefix + "      {");
        genCodeLine(prefix + "         debugStream.WriteLine(" +
        "\"   Putting back \" + (curPos - jjmatchedPos - 1) + \" characters into the input stream.\");");
      }

      genCodeLine(prefix + "         input_stream.backup(curPos - jjmatchedPos - 1);");

      if (Options.getDebugTokenManager())
        genCodeLine(prefix + "      }");

      if (Options.getDebugTokenManager())
      {
        if (Options.getJavaUnicodeEscape() ||
            Options.getUserCharStream())
          genCodeLine("    debugStream.WriteLine(" +
              "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
              "(\" + TokenMgrError.addEscapes(new String(input_stream.GetSuffix(jjmatchedPos + 1))) + " +
          "\") ******\\n\");");
        else
          genCodeLine("    debugStream.WriteLine(" +
              "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
              "(\" + TokenMgrError.addEscapes(new String(input_stream.GetSuffix(jjmatchedPos + 1))) + " +
          "\") ******\\n\");");
      }

      if (hasSkip || hasMore || hasSpecial)
      {
        genCodeLine(prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " +
        "(1UL << (jjmatchedKind & 0x3F))) != 0UL)");
        genCodeLine(prefix + "      {");
      }

      genCodeLine(prefix + "         matchedToken = jjFillToken();");

      if (hasSpecial)
        genCodeLine(prefix + "         matchedToken.specialToken = specialToken;");

      if (hasTokenActions)
        genCodeLine(prefix + "         TokenLexicalActions(matchedToken);");

      if (maxLexStates > 1)
      {
        genCodeLine("       if (jjnewLexState[jjmatchedKind] != -1)");
        genCodeLine(prefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      if (Options.getCommonTokenAction())
        genCodeLine(prefix + "         CommonTokenAction(matchedToken);");

      genCodeLine(prefix + "         return matchedToken;");

      if (hasSkip || hasMore || hasSpecial)
      {
        genCodeLine(prefix + "      }");

        if (hasSkip || hasSpecial)
        {
          if (hasMore)
          {
            genCodeLine(prefix + "      else if ((jjtoSkip[jjmatchedKind >> 6] & " +
            "(1UL << (jjmatchedKind & 0x3F))) != 0UL)");
          }
          else
            genCodeLine(prefix + "      else");

          genCodeLine(prefix + "      {");

          if (hasSpecial)
          {
            genCodeLine(prefix + "         if ((jjtoSpecial[jjmatchedKind >> 6] & " +
            "(1UL << (jjmatchedKind & 0x3F))) != 0UL)");
            genCodeLine(prefix + "         {");

            genCodeLine(prefix + "            matchedToken = jjFillToken();");

            genCodeLine(prefix + "            if (specialToken == null)");
            genCodeLine(prefix + "               specialToken = matchedToken;");
            genCodeLine(prefix + "            else");
            genCodeLine(prefix + "            {");
            genCodeLine(prefix + "               matchedToken.specialToken = specialToken;");
            genCodeLine(prefix + "               specialToken = (specialToken.next = matchedToken);");
            genCodeLine(prefix + "            }");

            if (hasSkipActions)
              genCodeLine(prefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine(prefix + "         }");

            if (hasSkipActions)
            {
              genCodeLine(prefix + "         else");
              genCodeLine(prefix + "            SkipLexicalActions(null);");
            }
          }
          else if (hasSkipActions)
            genCodeLine(prefix + "         SkipLexicalActions(null);");

          if (maxLexStates > 1)
          {
            genCodeLine("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine(prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine(prefix + "         goto EOFLoop;");
          genCodeLine(prefix + "      }");
        }

        if (hasMore)
        {
          if (hasMoreActions)
            genCodeLine(prefix + "      MoreLexicalActions();");
          else if (hasSkipActions || hasTokenActions)
            genCodeLine(prefix + "      jjimageLen += jjmatchedPos + 1;");

          if (maxLexStates > 1)
          {
            genCodeLine("      if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine(prefix + "      curLexState = jjnewLexState[jjmatchedKind];");
          }
          genCodeLine(prefix + "      curPos = 0;");
          genCodeLine(prefix + "      jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");

          genCodeLine(prefix + "      if (input_stream.readChar(ref curChar))");
          genCodeLine(prefix + "         {");

          if (Options.getDebugTokenManager())
            genCodeLine("   debugStream.WriteLine(" +
                (maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                "\"Current character : \" + " +
                "TokenMgrError.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
            "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          genCodeLine(prefix + "         continue;");
          genCodeLine(prefix + "      }");
        }
      }

      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   int error_line = input_stream.getEndLine();");
      genCodeLine(prefix + "   int error_column = input_stream.getEndColumn();");
      genCodeLine(prefix + "   String error_after = null;");
      genCodeLine(prefix + "   " + Options.getBooleanType() + " EOFSeen = false;");
      genCodeLine(prefix + "   char errChar = 'E';");
      genCodeLine(prefix + "   if (input_stream.readChar(ref errChar))");
      genCodeLine(prefix + "   { ");
      genCodeLine(prefix + "     input_stream.backup(1); ");
      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   else");
      genCodeLine(prefix + "   {");
      genCodeLine(prefix + "      EOFSeen = true;");
      genCodeLine(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
      genCodeLine(prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      genCodeLine(prefix + "         error_line++;");
      genCodeLine(prefix + "         error_column = 0;");
      genCodeLine(prefix + "      }");
      genCodeLine(prefix + "      else");
      genCodeLine(prefix + "         error_column++;");
      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   if (!EOFSeen) {");
      genCodeLine(prefix + "      input_stream.backup(1);");
      genCodeLine(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   throw new TokenMgrError(" +
      "EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);");
    }

    if (hasMore)
      genCodeLine(prefix + " }");

    genCodeLine("  }");
    genCodeLine("}");
    genCodeLine("");
  }

  public void DumpSkipActions()
  {
      Action act;

    genCodeLine(staticString + "void SkipLexicalActions(Token matchedToken)");
    genCodeLine("{");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
      for (int i = 0; i < maxOrdinal; i++)
      {
        if ((toSkip[i / 64] & (1L << (i % 64))) == 0L)
          continue;

        for (;;)
        {
          if (((act = (Action)actions[i]) == null ||
              act.getActionTokens() == null ||
              act.getActionTokens().size() == 0) && !canLoop[lexStates[i]])
            continue Outer;

          genCodeLine("      case " + i + " :");

          if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]])
          {
            genCodeLine("         if (jjmatchedPos == -1)");
            genCodeLine("         {");
            genCodeLine("            if (jjbeenHere[" + lexStates[i] + "] &&");
            genCodeLine("                jjemptyLineNo[" + lexStates[i] + "] == input_stream.getBeginLine() &&");
            genCodeLine("                jjemptyColNo[" + lexStates[i] + "] == input_stream.getBeginColumn())");
            genCodeLine("               throw new TokenMgrError(" +
                "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                "at line \" + input_stream.getBeginLine() + \", " +
            "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrError.LOOP_DETECTED);");
            genCodeLine("            jjemptyLineNo[" + lexStates[i] + "] = input_stream.getBeginLine();");
            genCodeLine("            jjemptyColNo[" + lexStates[i] + "] = input_stream.getBeginColumn();");
            genCodeLine("            jjbeenHere[" + lexStates[i] + "] = true;");
            genCodeLine("         }");
          }

          if ((act = (Action)actions[i]) == null ||
              act.getActionTokens().size() == 0)
            break;

          genCode(  "         image.Append");
          if (RStringLiteral.allImages[i] != null) {
            genCodeLine("(jjstrLiteralImages[" + i + "]);");
            genCodeLine("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
          } else {
            genCodeLine("(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
          }

          printTokenSetup((Token)act.getActionTokens().get(0));
          ccol = 1;

          for (int j = 0; j < act.getActionTokens().size(); j++)
            printToken((Token)act.getActionTokens().get(j));
          genCodeLine("");

          break;
        }

        genCodeLine("         break;");
      }

    genCodeLine("      default :");
    genCodeLine("         break;");
    genCodeLine("   }");
    genCodeLine("}");
  }

  public void DumpMoreActions()
  {
        Action act;

    genCodeLine(staticString + "void MoreLexicalActions()");
    genCodeLine("{");
    genCodeLine("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
      for (int i = 0; i < maxOrdinal; i++)
      {
        if ((toMore[i / 64] & (1L << (i % 64))) == 0L)
          continue;

        for (;;)
        {
          if (((act = (Action)actions[i]) == null ||
              act.getActionTokens() == null ||
              act.getActionTokens().size() == 0) && !canLoop[lexStates[i]])
            continue Outer;

          genCodeLine("      case " + i + " :");

          if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]])
          {
            genCodeLine("         if (jjmatchedPos == -1)");
            genCodeLine("         {");
            genCodeLine("            if (jjbeenHere[" + lexStates[i] + "] &&");
            genCodeLine("                jjemptyLineNo[" + lexStates[i] + "] == input_stream.getBeginLine() &&");
            genCodeLine("                jjemptyColNo[" + lexStates[i] + "] == input_stream.getBeginColumn())");
            genCodeLine("               throw new TokenMgrError(" +
                "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                "at line \" + input_stream.getBeginLine() + \", " +
            "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrError.LOOP_DETECTED);");
            genCodeLine("            jjemptyLineNo[" + lexStates[i] + "] = input_stream.getBeginLine();");
            genCodeLine("            jjemptyColNo[" + lexStates[i] + "] = input_stream.getBeginColumn();");
            genCodeLine("            jjbeenHere[" + lexStates[i] + "] = true;");
            genCodeLine("         }");
          }

          if ((act = (Action)actions[i]) == null ||
              act.getActionTokens().size() == 0)
          {
            break;
          }

          genCode(  "         image.append");

          if (RStringLiteral.allImages[i] != null)
            genCodeLine("(jjstrLiteralImages[" + i + "]);");
          else
            genCodeLine("(input_stream.GetSuffix(jjimageLen));");

          genCodeLine("         jjimageLen = 0;");
          printTokenSetup((Token)act.getActionTokens().get(0));
          ccol = 1;

          for (int j = 0; j < act.getActionTokens().size(); j++)
            printToken((Token)act.getActionTokens().get(j));
          genCodeLine("");

          break;
        }

        genCodeLine("         break;");
      }

    genCodeLine("      default :");
    genCodeLine("         break;");

    genCodeLine("   }");
    genCodeLine("}");

  }

  public void DumpTokenActions()
  {
    Action act;
    int i;

    genCodeLine(staticString + "void TokenLexicalActions(Token matchedToken)");
    genCodeLine("{");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
      for (i = 0; i < maxOrdinal; i++)
      {
        if ((toToken[i / 64] & (1L << (i % 64))) == 0L)
          continue;

        for (;;)
        {
          if (((act = (Action)actions[i]) == null ||
              act.getActionTokens() == null ||
              act.getActionTokens().size() == 0) && !canLoop[lexStates[i]])
            continue Outer;

          genCodeLine("      case " + i + " :");

          if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]])
          {
            genCodeLine("         if (jjmatchedPos == -1)");
            genCodeLine("         {");
            genCodeLine("            if (jjbeenHere[" + lexStates[i] + "] &&");
            genCodeLine("                jjemptyLineNo[" + lexStates[i] + "] == input_stream.getBeginLine() &&");
            genCodeLine("                jjemptyColNo[" + lexStates[i] + "] == input_stream.getBeginColumn())");
            genCodeLine("               throw new TokenMgrError(" +
                "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                "at line \" + input_stream.getBeginLine() + \", " +
            "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrError.LOOP_DETECTED);");
            genCodeLine("            jjemptyLineNo[" + lexStates[i] + "] = input_stream.getBeginLine();");
            genCodeLine("            jjemptyColNo[" + lexStates[i] + "] = input_stream.getBeginColumn();");
            genCodeLine("            jjbeenHere[" + lexStates[i] + "] = true;");
            genCodeLine("         }");
          }

          if ((act = (Action)actions[i]) == null ||
              act.getActionTokens().size() == 0)
            break;

          if (i == 0)
          {
            genCodeLine("      image.Length = 0;"); // For EOF no image is there
          }
          else
          {
            genCode(  "        image.Append");

            if (RStringLiteral.allImages[i] != null) {
              genCodeLine("(jjstrLiteralImages[" + i + "]);");
              genCodeLine("        lengthOfMatch = jjstrLiteralImages[" + i + "].Length;");
            } else {
              genCodeLine("(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
            }
          }

          printTokenSetup((Token)act.getActionTokens().get(0));
          ccol = 1;

          for (int j = 0; j < act.getActionTokens().size(); j++)
            printToken((Token)act.getActionTokens().get(j));
          genCodeLine("");

          break;
        }

        genCodeLine("         break;");
      }

    genCodeLine("      default :");
    genCodeLine("         break;");
    genCodeLine("   }");
    genCodeLine("}");
  }
}
