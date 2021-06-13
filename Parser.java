package enshud.s2.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ParserError extends Exception{
	
	public ParserError(int line){
		System.err.println("Syntax error: line " + line);
	}
	
}

class TokenData {
	
	static int n = 0;
	private String name;
	private String id;
	private int line;

	public TokenData(String[] tsLine){
		name = tsLine[0];
		id = tsLine[1];
		line = Integer.parseInt(tsLine[3]);
	}
	
	static void inc() {
		n++;
	}
	
	static void dec() {
		n--;
	}
	
	static void clr() {
		n = 0;
	}
	
	String getName() {
		return name;
	}
	
	String getId() {
		return id;
	}

	int getLine() {
		return line;
	}

	
}

public class Parser {
	static int n = 0;
	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Parser().run("data/ts/normal11.ts");
		new Parser().run("data/ts/normal02.ts");

		// synerrの確認
		new Parser().run("data/ts/synerr01.ts");
		new Parser().run("data/ts/synerr02.ts");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるParser実行メソッド．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，構文解析を行う．
	 * 構文が正しい場合は標準出力に"OK"を，正しくない場合は"Syntax error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Syntax error: line 1"）．
	 * 入力ファイル内に複数のエラーが含まれる場合は，最初に見つけたエラーのみを出力すること．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 * 
	 * @param inputFileName 入力tsファイル名
	 */
	public void run(final String inputFileName) {
		try
		{
			FileReader fr = new FileReader(inputFileName);
			BufferedReader br = new BufferedReader(fr);
			List<String> lineList = new ArrayList<String>();
			int tsLineCount;
			String line = br.readLine();
			for(tsLineCount = 0; line != null; tsLineCount++) {
				lineList.add(line);
				line = br.readLine();
			}
			br.close();
			TokenData tokenArray[] = new TokenData[tsLineCount];			
			for(int i = 0; i < tsLineCount; i++) {
				tokenArray[i] = new TokenData(lineList.get(i).split("\t"));
			}
			program(tokenArray);
			System.out.println("OK");	
			TokenData.clr();
		}
		catch(final FileNotFoundException e) {
			System.err.println("File not found");
		}
		catch(final IOException e){
			System.err.println("File not found");
		}
		catch(final ParserError e) {
			//構文エラー
			TokenData.clr();
		}
	}
	
	private static void terminator(final TokenData tokenData, String id) throws ParserError{
		if(!(tokenData.getId().equals(id))) throw new ParserError(tokenData.getLine());
		TokenData.inc();
	}
	
	private static void program(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SPROGRAM");
		programName(tokenArray);
		terminator(tokenArray[TokenData.n],"SSEMICOLON");
		block(tokenArray);
		compositeSentence(tokenArray);
		terminator(tokenArray[TokenData.n],"SDOT");
	}
	
	private static void programName(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
	}

	private static void block(TokenData[] tokenArray) throws ParserError{
		varDeclare(tokenArray);
		subProgramDeclareGroup(tokenArray);
	}
	
	private static void varDeclare(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SVAR")) {
			terminator(tokenArray[TokenData.n],"SVAR");
			varDeclareList(tokenArray);
		}
	}
	
	private static void varDeclareList(final TokenData[] tokenArray) throws ParserError{
		do {
			varNameList(tokenArray);
			terminator(tokenArray[TokenData.n],"SCOLON");
			type(tokenArray);
			terminator(tokenArray[TokenData.n],"SSEMICOLON");	
		}while(tokenArray[TokenData.n].getId().equals("SIDENTIFIER"));
	}

	private static void varNameList(final TokenData[] tokenArray) throws ParserError{
		varName(tokenArray);
		while(tokenArray[TokenData.n].getId().equals("SCOMMA")) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			varName(tokenArray);
		}
	}

	private static void varName(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
	}

	private static void type(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SINTEGER") || tokenArray[TokenData.n].getId().equals("SCHAR") || tokenArray[TokenData.n].getId().equals("SBOOLEAN")) {
			normalType(tokenArray);
		}else if(tokenArray[TokenData.n].getId().equals("SARRAY")) {
			arrayType(tokenArray);
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void normalType(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SINTEGER")){
			terminator(tokenArray[TokenData.n],"SINTEGER");
		}else if(tokenArray[TokenData.n].getId().equals("SCHAR")) {
			terminator(tokenArray[TokenData.n],"SCHAR");
		}else if(tokenArray[TokenData.n].getId().equals("SBOOLEAN")) {
			terminator(tokenArray[TokenData.n],"SBOOLEAN");
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}
	
	private static void arrayType(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SARRAY");
		terminator(tokenArray[TokenData.n],"SLBRACKET");
		minSubscript(tokenArray);
		terminator(tokenArray[TokenData.n],"SRANGE");
		maxSubscript(tokenArray);
		terminator(tokenArray[TokenData.n],"SRBRACKET");
		terminator(tokenArray[TokenData.n],"SOF");
		normalType(tokenArray);
	}

	private static void minSubscript(final TokenData[] tokenArray) throws ParserError{
		integer(tokenArray);
	}

	private static void maxSubscript(final TokenData[] tokenArray) throws ParserError{
		integer(tokenArray);
	}
	
	private static void integer(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SPLUS") || tokenArray[TokenData.n].getId().equals("SMINUS")){
			sign(tokenArray);
		}
		terminator(tokenArray[TokenData.n],"SCONSTANT");
	}
	
	private static void sign(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SPLUS")){
			terminator(tokenArray[TokenData.n],"SPLUS");
		}else if(tokenArray[TokenData.n].getId().equals("SMINUS")) {
			terminator(tokenArray[TokenData.n],"SMINUS");
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void subProgramDeclareGroup(final TokenData[] tokenArray) throws ParserError{
		while(tokenArray[TokenData.n].getId().equals("SPROCEDURE")) {
			subProgramDeclare(tokenArray);
			terminator(tokenArray[TokenData.n],"SSEMICOLON");
		}
	}

	private static void subProgramDeclare(final TokenData[] tokenArray) throws ParserError{
		subProgramHead(tokenArray);
		varDeclare(tokenArray);
		compositeSentence(tokenArray);
	}

	private static void subProgramHead(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SPROCEDURE");
		procedureName(tokenArray);
		tempParameter(tokenArray);
		terminator(tokenArray[TokenData.n],"SSEMICOLON");
	}

	private static void procedureName(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
	}
	
	private static void tempParameter(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			tempParameterList(tokenArray);
			terminator(tokenArray[TokenData.n],"SRPAREN");
		}
	}

	private static void tempParameterList(final TokenData[] tokenArray) throws ParserError{
		tempParameterNameList(tokenArray);
		terminator(tokenArray[TokenData.n],"SCOLON");
		normalType(tokenArray);
		while(tokenArray[TokenData.n].getId().equals("SSEMICOLON")) {
			terminator(tokenArray[TokenData.n],"SSEMICOLON");
			tempParameterNameList(tokenArray);
			terminator(tokenArray[TokenData.n],"SCOLON");
			normalType(tokenArray);
		}
	}

	private static void tempParameterNameList(final TokenData[] tokenArray) throws ParserError{
		tempParameterName(tokenArray);
		while(tokenArray[TokenData.n].getId().equals("SCOMMA")) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			tempParameterName(tokenArray);
		}
	}

	private static void tempParameterName(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
	}

	private static void compositeSentence(final TokenData[] tokenArray) throws ParserError{
		terminator(tokenArray[TokenData.n],"SBEGIN");
		sentenceList(tokenArray);
		terminator(tokenArray[TokenData.n],"SEND");
	}

	private static void sentenceList(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId;
		do {
			sentence(tokenArray);
			terminator(tokenArray[TokenData.n],"SSEMICOLON");
			tempTokenId = tokenArray[TokenData.n].getId();
		}while(tempTokenId.equals("SIDENTIFIER") || tempTokenId.equals("SPROCEDURE") || tempTokenId.equals("SREADLN") || tempTokenId.equals("SWRITELN") || tempTokenId.equals("SBEGIN") || tempTokenId.equals("SIF") || tempTokenId.equals("SWHILE"));
	}

	private static void sentence(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SIDENTIFIER") || tempTokenId.equals("SREADLN") || tempTokenId.equals("SWRITELN") || tempTokenId.equals("SBEGIN")) {
			basicSentence(tokenArray);
		}else if(tempTokenId.equals("SIF")){
			terminator(tokenArray[TokenData.n],"SIF");	
			formula(tokenArray);
			terminator(tokenArray[TokenData.n],"STHEN");
			compositeSentence(tokenArray);
			if(tokenArray[TokenData.n].getId().equals("SELSE")) {
				terminator(tokenArray[TokenData.n],"SELSE");
				compositeSentence(tokenArray);
			}
		}else if(tempTokenId.equals("SWHILE")) {
			terminator(tokenArray[TokenData.n],"SWHILE");
			formula(tokenArray);
			terminator(tokenArray[TokenData.n],"SDO");
			compositeSentence(tokenArray);
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void basicSentence(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SIDENTIFIER")) {
			TokenData.inc();
			tempTokenId = tokenArray[TokenData.n].getId();
			if(tempTokenId.equals("SASSIGN") || tempTokenId.equals("SLBRACKET")) {
				TokenData.dec();
				assignmentSentence(tokenArray);	
			}else if(tempTokenId.equals("SLPAREN") || tempTokenId.equals("SSEMICOLON")){
				TokenData.dec();
				procedureCallSentence(tokenArray);
			}else throw new ParserError(tokenArray[TokenData.n].getLine());
		}else if(tempTokenId.equals("SREADLN") || tempTokenId.equals("SWRITELN")) {
			inputOutputSentence(tokenArray);
		}else if(tempTokenId.equals("SBEGIN")) {
			compositeSentence(tokenArray);
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void assignmentSentence(final TokenData[] tokenArray) throws ParserError{
		leftSide(tokenArray);
		terminator(tokenArray[TokenData.n],"SASSIGN");
		formula(tokenArray);
	}

	private static void leftSide(final TokenData[] tokenArray) throws ParserError{
		variable(tokenArray);
	}
	
	private static void variable(final TokenData[] tokenArray) throws ParserError{
		TokenData.inc();
		if(tokenArray[TokenData.n].getId().equals("SLBRACKET")) {
			TokenData.dec();
			subscriptVar(tokenArray);
		}else {
			TokenData.dec();
			genuineVar(tokenArray);
		}
	}

	private static void genuineVar(final TokenData[] tokenArray) throws ParserError{
		varName(tokenArray);
	}
	
	private static void subscriptVar(final TokenData[] tokenArray) throws ParserError{
		varName(tokenArray);
		terminator(tokenArray[TokenData.n],"SLBRACKET");
		subscript(tokenArray);
		terminator(tokenArray[TokenData.n],"SRBRACKET");
	}

	private static void subscript(final TokenData[] tokenArray) throws ParserError{
		formula(tokenArray);
	}
	
	private static void procedureCallSentence(final TokenData[] tokenArray) throws ParserError{
		procedureName(tokenArray);
		if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			formulaList(tokenArray);
			terminator(tokenArray[TokenData.n],"SRPAREN");
		}
	}

	private static void formulaList(final TokenData[] tokenArray) throws ParserError{
		formula(tokenArray);
		while(tokenArray[TokenData.n].getId().equals("SCOMMA")) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			formula(tokenArray);
		}
	}
	
	private static void formula(final TokenData[] tokenArray) throws ParserError{
		simpleFormula(tokenArray);
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SEQUAL") || tempTokenId.equals("SNOTEQUAL") || tempTokenId.equals("SLESS") || tempTokenId.equals("SLESSEQUAL") || tempTokenId.equals("SGREAT") || tempTokenId.equals("SGREATEQUAL")) {
			relationOpe(tokenArray);
			simpleFormula(tokenArray);
		}
	}

	private static void simpleFormula(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SPLUS") || tokenArray[TokenData.n].getId().equals("SMINUS")){
			sign(tokenArray);
		}
		term(tokenArray);
		String tempTokenId = tokenArray[TokenData.n].getId();
		while(tempTokenId.equals("SPLUS") || tempTokenId.equals("SMINUS") || tempTokenId.equals("SOR")) {
			additionOpe(tokenArray);
			term(tokenArray);
			tempTokenId = tokenArray[TokenData.n].getId();
		}
	}
	
	private static void term(final TokenData[] tokenArray) throws ParserError{
		fact(tokenArray);
		String tempTokenId = tokenArray[TokenData.n].getId();
		while(tempTokenId.equals("SSTAR") || tempTokenId.equals("SDIVD") || tempTokenId.equals("SMOD") || tempTokenId.equals("SAND")) {
			multiOpe(tokenArray);
			fact(tokenArray);
			tempTokenId = tokenArray[TokenData.n].getId();
		}
	}

	private static void fact(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SIDENTIFIER")) {
			variable(tokenArray);
		}else if(tempTokenId.equals("SCONSTANT") || tempTokenId.equals("SSTRING") || tempTokenId.equals("SFALSE") || tempTokenId.equals("STRUE")) {
			constant(tokenArray);
		}else if(tempTokenId.equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			formula(tokenArray);
			terminator(tokenArray[TokenData.n],"SRPAREN");
		}else if(tempTokenId.equals("SNOT")) {
			terminator(tokenArray[TokenData.n],"SNOT");
			fact(tokenArray);
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void relationOpe(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SEQUAL")) {
			terminator(tokenArray[TokenData.n],"SEQUAL");
		}else if(tempTokenId.equals("SNOTEQUAL")) {
			terminator(tokenArray[TokenData.n],"SNOTEQUAL");
		}else if(tempTokenId.equals("SLESS")) {
			terminator(tokenArray[TokenData.n],"SLESS");
		}else if(tempTokenId.equals("SLESSEQUAL")) {
			terminator(tokenArray[TokenData.n],"SLESSEQUAL");
		}else if(tempTokenId.equals("SGREAT")) {
			terminator(tokenArray[TokenData.n],"SGREAT");
		}else if(tempTokenId.equals("SGREATEQUAL")) {
			terminator(tokenArray[TokenData.n],"SGREATEQUAL");
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void additionOpe(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SPLUS")) {
			terminator(tokenArray[TokenData.n],"SPLUS");
		}else if(tempTokenId.equals("SMINUS")) {
			terminator(tokenArray[TokenData.n],"SMINUS");
		}else if(tempTokenId.equals("SOR")) {
			terminator(tokenArray[TokenData.n],"SOR");
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void multiOpe(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SSTAR")) {
			terminator(tokenArray[TokenData.n],"SSTAR");
		}else if(tempTokenId.equals("SDIVD")) {
			terminator(tokenArray[TokenData.n],"SDIVD");
		}else if(tempTokenId.equals("SMOD")) {
			terminator(tokenArray[TokenData.n],"SMOD");
		}else if(tempTokenId.equals("SAND")) {
			terminator(tokenArray[TokenData.n],"SAND");
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}
	
	private static void inputOutputSentence(final TokenData[] tokenArray) throws ParserError{
		if(tokenArray[TokenData.n].getId().equals("SREADLN")) {
			terminator(tokenArray[TokenData.n],"SREADLN");
			if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
				terminator(tokenArray[TokenData.n],"SLPAREN");
				varList(tokenArray);
				terminator(tokenArray[TokenData.n],"SRPAREN");				
			}
		}else if(tokenArray[TokenData.n].getId().equals("SWRITELN")) {
			terminator(tokenArray[TokenData.n],"SWRITELN");
			if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
				terminator(tokenArray[TokenData.n],"SLPAREN");
				formulaList(tokenArray);
				terminator(tokenArray[TokenData.n],"SRPAREN");				
			}
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}

	private static void varList(final TokenData[] tokenArray) throws ParserError{
		variable(tokenArray);
		while(tokenArray[TokenData.n].getId().equals("SCOMMA")) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			variable(tokenArray);
		}
	}
	
	private static void constant(final TokenData[] tokenArray) throws ParserError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SCONSTANT")) {
			terminator(tokenArray[TokenData.n],"SCONSTANT");
		}else if(tempTokenId.equals("SSTRING")) {
			terminator(tokenArray[TokenData.n],"SSTRING");
		}else if(tempTokenId.equals("SFALSE")) {
			terminator(tokenArray[TokenData.n],"SFALSE");
		}else if(tempTokenId.equals("STRUE")) {
			terminator(tokenArray[TokenData.n],"STRUE");
		}else throw new ParserError(tokenArray[TokenData.n].getLine());
	}
	
}
