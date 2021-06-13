package enshud.s3.checker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

class CheckerError extends Exception{
	
	public CheckerError(int line, String s){
		System.err.println( s + " error: line " + line);
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

class NameData {
	
	static private HashMap<String, HashMap<String,List<String>>> nameHashMap = new HashMap<String,HashMap<String,List<String>>>();

	public NameData(){
		
	}
	
	static void setVarList(String scope){
		HashMap<String,List<String>> varHashMap = new HashMap<String, List<String>>();
		nameHashMap.put(scope, varHashMap);
	}
	
	static void setName(String scope, String varName, String type) {
		List<String> list = new ArrayList<String>();
		if(type == "ARRAY_INTEGER") {
			list.add("ARRAY");
			list.add("INTEGER");
		}else if(type == "ARRAY_CHAR") {
			list.add("ARRAY");
			list.add("CHAR");
		}else if(type == "ARRAY_BOOLEAN") {
			list.add("ARRAY");
			list.add("BOOLEAN");
		}else list.add(type);
		nameHashMap.get(scope).put(varName,list);
	}
	
	static String getType(String name, String scope) {
		if(nameHashMap.get(scope).get(name).get(0) == "ARRAY") {
			return nameHashMap.get(scope).get(name).get(1);
		}else {
			return nameHashMap.get(scope).get(name).get(0);
		}
	}
	
	static String getArrayType(String name, String scope) {
		return nameHashMap.get(scope).get(name).get(0);
	}
	
	static boolean containsName(String name, String scope) {
		if(nameHashMap.get(scope).containsKey(name)) {
			return true;
		}else return false;
	}
	
	static boolean containsProcedureName(String name) {
		if(nameHashMap.containsKey(name)) {
			return true;
		}else return false;
	}
	
	static void clr() {
		nameHashMap.clear();
	}
	
}

public class Checker {
	
	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Checker().run("data/ts/normal12.ts");
		new Checker().run("data/ts/normal02.ts");

		// synerrの確認
		new Checker().run("data/ts/synerr01.ts");
		new Checker().run("data/ts/synerr02.ts");

		// semerrの確認
		new Checker().run("data/ts/semerr01.ts");
		new Checker().run("data/ts/semerr02.ts");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるChecker実行メソッド．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，意味解析を行う．
	 * 意味的に正しい場合は標準出力に"OK"を，正しくない場合は"Semantic error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Semantic error: line 6"）．
	 * また，構文的なエラーが含まれる場合もエラーメッセージを表示すること（例： "Syntax error: line 1"）．
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
			NameData.clr();
		}
		catch(final FileNotFoundException e) {
			System.err.println("File not found");
		}
		catch(final IOException e){
			System.err.println("File not found");
		}
		catch(final CheckerError e) {
			//エラー
			TokenData.clr();
			NameData.clr();
		}
	}
	
	private static void terminator(final TokenData tokenData, String id) throws CheckerError{
		if(!(tokenData.getId().equals(id))) throw new CheckerError(tokenData.getLine(), "Syntax");
		TokenData.inc();
	}
	
	private static void program(final TokenData[] tokenArray) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SPROGRAM");
		programName(tokenArray);
		NameData.setVarList("GROBAL");
		terminator(tokenArray[TokenData.n],"SSEMICOLON");
		block(tokenArray);
		compositeSentence(tokenArray, "GROBAL");
		terminator(tokenArray[TokenData.n],"SDOT");
	}
	
	private static void programName(final TokenData[] tokenArray) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
	}

	private static void block(TokenData[] tokenArray) throws CheckerError{
		varDeclare(tokenArray, "GROBAL");
		subProgramDeclareGroup(tokenArray);
	}
	
	private static void varDeclare(final TokenData[] tokenArray, String scope) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SVAR")) {
			terminator(tokenArray[TokenData.n],"SVAR");
			varDeclareList(tokenArray,scope);
		}
	}
	
	private static void varDeclareList(final TokenData[] tokenArray, String scope) throws CheckerError{

		do {
			List<String> varNameList = new ArrayList<>();
			int varCount = varNameList(tokenArray,scope,varNameList);
			terminator(tokenArray[TokenData.n],"SCOLON");
			String varType = type(tokenArray);
			for(int i = 0; i < varCount; i++) {
				NameData.setName(scope,varNameList.get(i),varType);				
			}
			terminator(tokenArray[TokenData.n],"SSEMICOLON");	
		}while(tokenArray[TokenData.n].getId().equals("SIDENTIFIER"));
	}

	private static int varNameList(final TokenData[] tokenArray, String scope, List<String> varNameList) throws CheckerError{
		varName(tokenArray,scope,varNameList);
		int varCount;
		for(varCount = 1; tokenArray[TokenData.n].getId().equals("SCOMMA"); varCount++) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			varName(tokenArray,scope,varNameList);
		}
		return varCount;
	}

	private static void varName(final TokenData[] tokenArray, String scope, List<String> varNameList) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
		if(NameData.containsName(tokenArray[TokenData.n-1].getName(), scope)) {
			 throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		}
		varNameList.add(tokenArray[TokenData.n-1].getName());
	}
	
	private static void varName(final TokenData[] tokenArray, String scope) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
	}

	private static String type(final TokenData[] tokenArray) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SINTEGER") || tokenArray[TokenData.n].getId().equals("SCHAR") || tokenArray[TokenData.n].getId().equals("SBOOLEAN")) {
			return normalType(tokenArray);
		}else if(tokenArray[TokenData.n].getId().equals("SARRAY")) {
			return arrayType(tokenArray);
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static String normalType(final TokenData[] tokenArray) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SINTEGER")){
			terminator(tokenArray[TokenData.n],"SINTEGER");
			return "INTEGER";
		}else if(tokenArray[TokenData.n].getId().equals("SCHAR")) {
			terminator(tokenArray[TokenData.n],"SCHAR");
			return "CHAR";
		}else if(tokenArray[TokenData.n].getId().equals("SBOOLEAN")) {
			terminator(tokenArray[TokenData.n],"SBOOLEAN");
			return "BOOLEAN";
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}
	
	private static String arrayType(final TokenData[] tokenArray) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SARRAY");
		terminator(tokenArray[TokenData.n],"SLBRACKET");
		minSubscript(tokenArray);
		terminator(tokenArray[TokenData.n],"SRANGE");
		maxSubscript(tokenArray);
		terminator(tokenArray[TokenData.n],"SRBRACKET");
		terminator(tokenArray[TokenData.n],"SOF");
		String type = normalType(tokenArray);
		if(type.equals("INTEGER")) {
			return "ARRAY_INTEGER";
		}else if(type.equals("CHAR")) {
			return "ARRAY_CHAR";
		}else return "ARRAY_BOOLEAN";
	}

	private static void minSubscript(final TokenData[] tokenArray) throws CheckerError{
		integer(tokenArray);
	}

	private static void maxSubscript(final TokenData[] tokenArray) throws CheckerError{
		integer(tokenArray);
	}
	
	private static void integer(final TokenData[] tokenArray) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SPLUS") || tokenArray[TokenData.n].getId().equals("SMINUS")){
			sign(tokenArray);
		}
		terminator(tokenArray[TokenData.n],"SCONSTANT");
	}
	
	private static void sign(final TokenData[] tokenArray) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SPLUS")){
			terminator(tokenArray[TokenData.n],"SPLUS");
		}else if(tokenArray[TokenData.n].getId().equals("SMINUS")) {
			terminator(tokenArray[TokenData.n],"SMINUS");
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static void subProgramDeclareGroup(final TokenData[] tokenArray) throws CheckerError{
		while(tokenArray[TokenData.n].getId().equals("SPROCEDURE")) {
			subProgramDeclare(tokenArray);
			terminator(tokenArray[TokenData.n],"SSEMICOLON");
		}
	}

	private static void subProgramDeclare(final TokenData[] tokenArray) throws CheckerError{
		String subProgramName = subProgramHead(tokenArray);
		varDeclare(tokenArray,subProgramName);
		compositeSentence(tokenArray, subProgramName);
	}

	private static String subProgramHead(final TokenData[] tokenArray) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SPROCEDURE");
		String subProgramName = procedureName(tokenArray);
		NameData.setVarList(subProgramName);
		tempParameter(tokenArray,subProgramName);
		terminator(tokenArray[TokenData.n],"SSEMICOLON");
		return subProgramName;
	}

	private static String procedureName(final TokenData[] tokenArray) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
		return tokenArray[TokenData.n-1].getName();
	}
	
	private static void tempParameter(final TokenData[] tokenArray, String scope) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			tempParameterList(tokenArray,scope);
			terminator(tokenArray[TokenData.n],"SRPAREN");
		}
	}

	private static void tempParameterList(final TokenData[] tokenArray, String scope) throws CheckerError{
		while(true){
			List<String> varNameList = new ArrayList<>();
			int varCount = tempParameterNameList(tokenArray,scope,varNameList);
			terminator(tokenArray[TokenData.n],"SCOLON");
			String varType = normalType(tokenArray);
			for(int i = 0; i < varCount; i++) {
				NameData.setName(scope, varNameList.get(i), varType);
			}
			if(tokenArray[TokenData.n].getId().equals("SSEMICOLON")) {
				terminator(tokenArray[TokenData.n],"SSEMICOLON");
			}else break;
		}
	}

	private static int tempParameterNameList(final TokenData[] tokenArray, String scope, List<String> varNameList) throws CheckerError{
		tempParameterName(tokenArray,scope,varNameList);
		int varCount;
		for(varCount = 1; tokenArray[TokenData.n].getId().equals("SCOMMA"); varCount++) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			tempParameterName(tokenArray,scope,varNameList);
		}
		return varCount;
	}

	private static void tempParameterName(final TokenData[] tokenArray, String scope, List<String> varNameList) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SIDENTIFIER");
		if(NameData.containsName(tokenArray[TokenData.n-1].getName(), scope)) {
			 throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		}
		varNameList.add(tokenArray[TokenData.n-1].getName());
	}

	private static void compositeSentence(final TokenData[] tokenArray, String scope) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SBEGIN");
		sentenceList(tokenArray,scope);
		terminator(tokenArray[TokenData.n],"SEND");
	}

	private static void sentenceList(final TokenData[] tokenArray, String scope) throws CheckerError{
		String tempTokenId;
		do {
			sentence(tokenArray,scope);
			terminator(tokenArray[TokenData.n],"SSEMICOLON");
			tempTokenId = tokenArray[TokenData.n].getId();
		}while(tempTokenId.equals("SIDENTIFIER") || tempTokenId.equals("SPROCEDURE") || tempTokenId.equals("SREADLN") || tempTokenId.equals("SWRITELN") || tempTokenId.equals("SBEGIN") || tempTokenId.equals("SIF") || tempTokenId.equals("SWHILE"));
	}

	private static void sentence(final TokenData[] tokenArray, String scope) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SIDENTIFIER") || tempTokenId.equals("SREADLN") || tempTokenId.equals("SWRITELN") || tempTokenId.equals("SBEGIN")) {
			basicSentence(tokenArray, scope);
		}else if(tempTokenId.equals("SIF")){
			terminator(tokenArray[TokenData.n],"SIF");	
			String formulaType = formula(tokenArray, scope);
			if(formulaType != "BOOLEAN") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			terminator(tokenArray[TokenData.n],"STHEN");
			compositeSentence(tokenArray, scope);
			if(tokenArray[TokenData.n].getId().equals("SELSE")) {
				terminator(tokenArray[TokenData.n],"SELSE");
				compositeSentence(tokenArray, scope);
			}
		}else if(tempTokenId.equals("SWHILE")) {
			terminator(tokenArray[TokenData.n],"SWHILE");
			String formulaType = formula(tokenArray, scope);
			if(formulaType != "BOOLEAN") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			terminator(tokenArray[TokenData.n],"SDO");
			compositeSentence(tokenArray, scope);
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static void basicSentence(final TokenData[] tokenArray, String scope) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SIDENTIFIER")) {
			TokenData.inc();
			tempTokenId = tokenArray[TokenData.n].getId();
			if(tempTokenId.equals("SASSIGN") || tempTokenId.equals("SLBRACKET")) {
				TokenData.dec();
				assignmentSentence(tokenArray, scope);	
			}else if(tempTokenId.equals("SLPAREN") || tempTokenId.equals("SSEMICOLON")){
				TokenData.dec();
				procedureCallSentence(tokenArray, scope);
			}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
		}else if(tempTokenId.equals("SREADLN") || tempTokenId.equals("SWRITELN")) {
			inputOutputSentence(tokenArray, scope);
		}else if(tempTokenId.equals("SBEGIN")) {
			compositeSentence(tokenArray, scope);
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static void assignmentSentence(final TokenData[] tokenArray, String scope) throws CheckerError{
		String leftSideType = leftSide(tokenArray, scope);
		//if(NameData.getArrayType(tokenArray[TokenData.n-1].getName(),scope) == "ARRAY") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		terminator(tokenArray[TokenData.n],"SASSIGN");
		String formulaType = formula(tokenArray, scope);
		if(leftSideType != formulaType) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");

	}

	private static String leftSide(final TokenData[] tokenArray, String scope) throws CheckerError{
		String varType = variable(tokenArray,scope,true);
		if(varType == "ARRAY") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		return varType;
	}

	private static String variable(final TokenData[] tokenArray, String scope, boolean leftSideFlag) throws CheckerError{
		TokenData.inc();
		if(tokenArray[TokenData.n].getId().equals("SLBRACKET")) {
			TokenData.dec();
			return subscriptVar(tokenArray,scope);
		}else {
			TokenData.dec();
			return genuineVar(tokenArray,scope,leftSideFlag);
		}
	}
	
	private static String genuineVar(final TokenData[] tokenArray, String scope, boolean leftSideFlag) throws CheckerError{
		varName(tokenArray,scope);
		if(NameData.containsName(tokenArray[TokenData.n-1].getName(), scope)){
			if(leftSideFlag == true) {
				return NameData.getArrayType(tokenArray[TokenData.n-1].getName(),scope);
			}else return NameData.getType(tokenArray[TokenData.n-1].getName(),scope);
		}else if(NameData.containsName(tokenArray[TokenData.n-1].getName(), "GROBAL")) {
			if(leftSideFlag == true) {
				return NameData.getArrayType(tokenArray[TokenData.n-1].getName(),"GROBAL");
			}else return NameData.getType(tokenArray[TokenData.n-1].getName(),"GROBAL");
		}else throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
	}

	private static String subscriptVar(final TokenData[] tokenArray, String scope) throws CheckerError{
		varName(tokenArray,scope);
		String varType;
		if(NameData.containsName(tokenArray[TokenData.n-1].getName(), scope)) {
			varType = NameData.getType(tokenArray[TokenData.n-1].getName(),scope);
		}else if(NameData.containsName(tokenArray[TokenData.n-1].getName(), "GROBAL")) {
			varType = NameData.getType(tokenArray[TokenData.n-1].getName(),"GROBAL");			
		}else throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		terminator(tokenArray[TokenData.n],"SLBRACKET");
		subscript(tokenArray, scope);
		terminator(tokenArray[TokenData.n],"SRBRACKET");
		return varType;
	}
	
	private static void subscript(final TokenData[] tokenArray, String scope) throws CheckerError{
		String formulaType = formula(tokenArray, scope);
		if(formulaType != "INTEGER") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
	}
	
	private static void procedureCallSentence(final TokenData[] tokenArray, String scope) throws CheckerError{
		String name = procedureName(tokenArray);
		if(!NameData.containsProcedureName(name)) {
			 throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		}
		if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			formulaList(tokenArray, scope);
			terminator(tokenArray[TokenData.n],"SRPAREN");
		}
	}

	private static void formulaList(final TokenData[] tokenArray, String scope) throws CheckerError{
		formula(tokenArray, scope);
		while(tokenArray[TokenData.n].getId().equals("SCOMMA")) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			formula(tokenArray, scope);
		}
	}
	
	private static String formula(final TokenData[] tokenArray, String scope) throws CheckerError{
		String formulaType1 = simpleFormula(tokenArray, scope);
		String formulaType2 = formulaType1;
		String tempTokenId = tokenArray[TokenData.n].getId();
		boolean relationFlag = false;
		if(tempTokenId.equals("SEQUAL") || tempTokenId.equals("SNOTEQUAL") || tempTokenId.equals("SLESS") || tempTokenId.equals("SLESSEQUAL") || tempTokenId.equals("SGREAT") || tempTokenId.equals("SGREATEQUAL")) {
			if(relationOpe(tokenArray) == "BOOLEAN") {
				relationFlag = true;
			};
			formulaType2 = simpleFormula(tokenArray, scope);
		}
		if(formulaType1 != formulaType2) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		if(relationFlag == true) {
			return "BOOLEAN";
		}else {
			return formulaType1;			
		}
	}

	private static String simpleFormula(final TokenData[] tokenArray, String scope) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SPLUS") || tokenArray[TokenData.n].getId().equals("SMINUS")){
			sign(tokenArray);
		}
		String termType1 = term(tokenArray, scope);
		String termType2 = termType1;
		String returnType = termType1;
		String tempTokenId = tokenArray[TokenData.n].getId();
		while(tempTokenId.equals("SPLUS") || tempTokenId.equals("SMINUS") || tempTokenId.equals("SOR")) {
			returnType = additionOpe(tokenArray);
			termType2 = term(tokenArray, scope);
			if(termType1 != termType2) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "INTEGER" && termType1 != "INTEGER")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "BOOLEAN" && termType1 != "BOOLEAN")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");		
			tempTokenId = tokenArray[TokenData.n].getId();
		}
		return returnType;
	}
	
	private static String term(final TokenData[] tokenArray, String scope) throws CheckerError{
		String factType1 = fact(tokenArray, scope);
		String factType2 = factType1;
		String returnType = factType1;
		String tempTokenId = tokenArray[TokenData.n].getId();
		while(tempTokenId.equals("SSTAR") || tempTokenId.equals("SDIVD") || tempTokenId.equals("SMOD") || tempTokenId.equals("SAND")) {
			returnType = multiOpe(tokenArray);
			factType2 = fact(tokenArray, scope);
			if(factType1 != factType2) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "INTEGER" && factType1 != "INTEGER")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "BOOLEAN" && factType1 != "BOOLEAN")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");		
			tempTokenId = tokenArray[TokenData.n].getId();
		}
		return returnType;
	}

	private static String fact(final TokenData[] tokenArray, String scope) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SIDENTIFIER")) {
			return variable(tokenArray,scope,false);
		}else if(tempTokenId.equals("SCONSTANT") || tempTokenId.equals("SSTRING") || tempTokenId.equals("SFALSE") || tempTokenId.equals("STRUE")) {
			return constant(tokenArray);
		}else if(tempTokenId.equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			String formulaType = formula(tokenArray, scope);
			terminator(tokenArray[TokenData.n],"SRPAREN");
			return formulaType;
		}else if(tempTokenId.equals("SNOT")) {
			terminator(tokenArray[TokenData.n],"SNOT");
			String factType = fact(tokenArray, scope);
			if(factType != "BOOLEAN")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			return factType;
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static String relationOpe(final TokenData[] tokenArray) throws CheckerError{
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
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
		return "BOOLEAN";
	}

	private static String additionOpe(final TokenData[] tokenArray) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SPLUS")) {
			terminator(tokenArray[TokenData.n],"SPLUS");
			return "INTEGER";
		}else if(tempTokenId.equals("SMINUS")) {
			terminator(tokenArray[TokenData.n],"SMINUS");
			return "INTEGER";
		}else if(tempTokenId.equals("SOR")) {
			terminator(tokenArray[TokenData.n],"SOR");
			return "BOOLEAN";
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static String multiOpe(final TokenData[] tokenArray) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SSTAR")) {
			terminator(tokenArray[TokenData.n],"SSTAR");
			return "INTEGER";
		}else if(tempTokenId.equals("SDIVD")) {
			terminator(tokenArray[TokenData.n],"SDIVD");
			return "INTEGER";
		}else if(tempTokenId.equals("SMOD")) {
			terminator(tokenArray[TokenData.n],"SMOD");
			return "INTEGER";
		}else if(tempTokenId.equals("SAND")) {
			terminator(tokenArray[TokenData.n],"SAND");
			return "BOOLEAN";
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}
	
	private static void inputOutputSentence(final TokenData[] tokenArray, String scope) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SREADLN")) {
			terminator(tokenArray[TokenData.n],"SREADLN");
			if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
				terminator(tokenArray[TokenData.n],"SLPAREN");
				varList(tokenArray, scope);
				terminator(tokenArray[TokenData.n],"SRPAREN");				
			}
		}else if(tokenArray[TokenData.n].getId().equals("SWRITELN")) {
			terminator(tokenArray[TokenData.n],"SWRITELN");
			if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
				terminator(tokenArray[TokenData.n],"SLPAREN");
				formulaList(tokenArray,scope);
				terminator(tokenArray[TokenData.n],"SRPAREN");				
			}
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static void varList(final TokenData[] tokenArray, String scope) throws CheckerError{
		variable(tokenArray,scope,false);
		while(tokenArray[TokenData.n].getId().equals("SCOMMA")) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			variable(tokenArray,scope,false);
		}
	}
	
	private static String constant(final TokenData[] tokenArray) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SCONSTANT")) {
			terminator(tokenArray[TokenData.n],"SCONSTANT");
			return "INTEGER";
		}else if(tempTokenId.equals("SSTRING")) {
			terminator(tokenArray[TokenData.n],"SSTRING");
			return "CHAR";
		}else if(tempTokenId.equals("SFALSE")) {
			terminator(tokenArray[TokenData.n],"SFALSE");
			return "BOOLEAN";
		}else if(tempTokenId.equals("STRUE")) {
			terminator(tokenArray[TokenData.n],"STRUE");
			return "BOOLEAN";
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}
	
}
