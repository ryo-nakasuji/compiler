package enshud.s4.compiler;

import enshud.casl.CaslSimulator;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;


//エラー出力用
class CheckerError extends Exception{
	
	public CheckerError(int line, String s){
		System.err.println( s + " error: line " + line);
	}
	
}

//tsファイルのトークンを扱う
class TokenData {
	
	static int n = 0; //何番目のトークンか(つまりtsファイルの行数-1)
	private String name;
	private String id;
	private int line;

	public TokenData(String[] tsLine){
		name = tsLine[0];
		id = tsLine[1];
		line = Integer.parseInt(tsLine[3]);
	}
	
	//次のトークンを選ぶ
	static void inc() {
		n++;
	}
	
	//前のトークンを選ぶ
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

//変数名や手続き名を扱う スコープ管理もここで
class NameData {
	
	static private HashMap<String, HashMap<String,List<String>>> nameHashMap = new HashMap<String,HashMap<String,List<String>>>();

	public NameData(){
		
	}
	
	static void setVarList(String scope){
		HashMap<String,List<String>> varHashMap = new HashMap<String, List<String>>();
		nameHashMap.put(scope, varHashMap);
	}
	
	static void setName(String scope, String varName, String type, String variableNum) {
		List<String> list = new ArrayList<String>();
		if(type == "ARRAY_INTEGER") {
			list.add("ARRAY");
			list.add("INTEGER");
			list.add(variableNum);
		}else if(type == "ARRAY_CHAR") {
			list.add("ARRAY");
			list.add("CHAR");
			list.add(variableNum);
		}else if(type == "ARRAY_BOOLEAN") {
			list.add("ARRAY");
			list.add("BOOLEAN");
			list.add(variableNum);
		}else {
			list.add(type);
			list.add(type);
			list.add(variableNum);
		}
		nameHashMap.get(scope).put(varName,list);
	}
	
	static String getVarNum(String name, String scope) {
		if(nameHashMap.get(scope).containsKey(name)) {
			return nameHashMap.get(scope).get(name).get(2);			
		}else {
			return nameHashMap.get("GROBAL").get(name).get(2);
		}
	}
	
	static String getType(String name, String scope) {
		return nameHashMap.get(scope).get(name).get(1);
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

//CASL2のコードを一時的に保存しておくためのクラス
class CodeBuffer {
	
	static private List<String> bufList = new ArrayList<String>();
	static private String bufLine = "";
	
	public CodeBuffer(){
		
	}
	
	public static void addLine(String str) {
		bufLine = bufLine + str + "\t";
	}
	
	public static void addCode(){
		bufLine = bufLine + ";\n";
		bufList.add(bufLine);
		bufLine = "";
	}
	
	public static void addCode(String str) {
		bufList.add(str+"\n");
	}
	
	public static void writeCode( final String outputFileName) {
		try {
			FileWriter fw = new FileWriter(outputFileName);
			for(String str : bufList) fw.write(str);
			fw.close();
		}
		catch(final IOException e){
			System.err.println("File not found");
		}
	}
	
	static public void clr() {
		bufList.clear();
		bufLine = "";
	}
	
}

//write文などに出現する文字列を扱う
class StringStack{
	
	static private List<String> stringList = new ArrayList<String>();

	public StringStack() {
		
	}
	
	public static void stringStack(String str) {
		if(!(stringList.contains(str))) {
			stringList.add(str);
		}
	}
	
	public static int stringLength(String str) {
		return (str.length() - 2);
	}
	
	public static int stringNumber(String str) {
		return stringList.indexOf(str);
	}	
	
	public static int stringCount() {
		return stringList.size();
	}
	
	public static String getString(int i) {
		return stringList.get(i);
	}
	
	static public void clr() {
		stringList.clear();
	}
	
}


public class Compiler {

	private static int variableCount = 0; //変数の番号
	private static int ifwhileRabelCount = 0; //条件分岐分のラベル番号
	private static int relationRabelCount = 0; //関係演算子のラベル番号
	static private List<String> subProgramList = new ArrayList<String>();
	
	public static void main(final String[] args) {
		new Compiler().run("data/ts/normal20.ts", "tmp/out.cas");
		CaslSimulator.run("tmp/out.cas", "tmp/out.ans");
	}

	public void run(final String inputFileName, final String outputFileName) {
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
			fr.close();
			TokenData tokenArray[] = new TokenData[tsLineCount];			
			for(int i = 0; i < tsLineCount; i++) {
				tokenArray[i] = new TokenData(lineList.get(i).split("\t"));
			}
			program(tokenArray); //解析開始
			FileReader fr2 = new FileReader("data/cas/lib.cas");
			BufferedReader br2 = new BufferedReader(fr2);
			line = br2.readLine();
			while(line != null) {
				CodeBuffer.addCode(line);
				line = br2.readLine();
			}
			br2.close();
			fr2.close();
			System.out.println("OK");	
			CodeBuffer.writeCode(outputFileName);
			initial();
		}
		catch(final FileNotFoundException e) {
			System.err.println("File not found");
		}
		catch(final IOException e){
			System.err.println("File not found");
		}
		catch(final CheckerError e) {
			initial();
		}
	}
	
	private void initial() {
		TokenData.clr();
		NameData.clr();
		CodeBuffer.clr();
		variableCount = 0;
		ifwhileRabelCount = 0;
		relationRabelCount = 0;
		subProgramList.clear();
	}
	
	private static void createLine(String label, String op, String target) {
		CodeBuffer.addLine(label);
		CodeBuffer.addLine(op);
		CodeBuffer.addLine(target);
		CodeBuffer.addCode();
	}
	
	private static void terminator(final TokenData tokenData, String id) throws CheckerError{
		if(!(tokenData.getId().equals(id))) throw new CheckerError(tokenData.getLine(), "Syntax");
		TokenData.inc();
	}
	
	private static void program(final TokenData[] tokenArray) throws CheckerError{
		terminator(tokenArray[TokenData.n],"SPROGRAM");
		programName(tokenArray);
		createLine("CASL","START","BEGIN");
		NameData.setVarList("GROBAL");
		terminator(tokenArray[TokenData.n],"SSEMICOLON");
		block(tokenArray);
		createLine("BEGIN","LAD","GR6, 0");
		createLine("","LAD","GR7,LIBBUF");
		compositeSentence(tokenArray, "GROBAL");
		terminator(tokenArray[TokenData.n],"SDOT");
		createLine("","RET","");		
		if(variableCount>0) createLine("VAR","DS",String.valueOf(variableCount));
		for(int i = 0; i < StringStack.stringCount(); i++) {
			createLine("CHAR"+i,"DC",StringStack.getString(i));
		}
		createLine("LIBBUF","DS","256");
		createLine("","END","");
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
			int tempCount = variableCount;
			String varType = type(tokenArray, scope, varNameList, varCount);
			if(varType.equals("ARRAY_INTEGER") || varType.equals("ARRAY_CHAR") || varType.equals("ARRAY_BOOLEAN")) {
				for(int i = 0; i < varCount; i++,variableCount++) {
					NameData.setName(scope,varNameList.get(i),varType,String.valueOf(tempCount));
				}
			}else {
				for(int i = 0; i < varCount; i++,variableCount++) {
					NameData.setName(scope,varNameList.get(i),varType,String.valueOf(variableCount));
				}				
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

	private static String type(final TokenData[] tokenArray, String scope, List<String> varNameList, int count) throws CheckerError{
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
		int min = minSubscript(tokenArray);
		terminator(tokenArray[TokenData.n],"SRANGE");
		int max = maxSubscript(tokenArray);
		if(min >= max) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		terminator(tokenArray[TokenData.n],"SRBRACKET");
		terminator(tokenArray[TokenData.n],"SOF");
		String type = normalType(tokenArray);
		variableCount += (max - min + 1);
		if(type.equals("INTEGER")) {
			return "ARRAY_INTEGER";
		}else if(type.equals("CHAR")) {
			return "ARRAY_CHAR";
		}else return "ARRAY_BOOLEAN";
	}

	private static int minSubscript(final TokenData[] tokenArray) throws CheckerError{
		int val = integer(tokenArray);
		if(val < 0) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		return val;
	}

	private static int maxSubscript(final TokenData[] tokenArray) throws CheckerError{
		int val = integer(tokenArray);
		if(val < 0) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		return val;
	}
	
	private static int integer(final TokenData[] tokenArray) throws CheckerError{
		String sign = "";
		if(tokenArray[TokenData.n].getId().equals("SPLUS") || tokenArray[TokenData.n].getId().equals("SMINUS")) {
			sign = sign(tokenArray);
		}
		terminator(tokenArray[TokenData.n],"SCONSTANT");
		if(sign.equals("SMINUS")) {
			return 0 - Integer.parseInt(tokenArray[TokenData.n-1].getName());
		}
		return Integer.parseInt(tokenArray[TokenData.n-1].getName());
	}
	
	private static String sign(final TokenData[] tokenArray) throws CheckerError{
		if(tokenArray[TokenData.n].getId().equals("SPLUS")){
			terminator(tokenArray[TokenData.n],"SPLUS");
			return "SPLUS";
		}else if(tokenArray[TokenData.n].getId().equals("SMINUS")) {
			terminator(tokenArray[TokenData.n],"SMINUS");
			return "SMINUS";
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static void subProgramDeclareGroup(final TokenData[] tokenArray) throws CheckerError{
		while(tokenArray[TokenData.n].getId().equals("SPROCEDURE")) {
			int rabel = subProgramList.size();;
			createLine("PROC"+rabel,"NOP","");
			subProgramDeclare(tokenArray);
			terminator(tokenArray[TokenData.n],"SSEMICOLON");
			createLine("","RET","");
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
		subProgramList.add(subProgramName);
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
			int paraCount = 0;
			for(paraCount = 0; paraCount < varCount; paraCount++,variableCount++) {
				NameData.setName(scope, varNameList.get(paraCount), varType,String.valueOf(variableCount));
			}
			createLine("","LD","GR1, GR8");
			createLine("","ADDA","GR1, ="+paraCount);
			for(int i = 0; i < paraCount; i++) {
				createLine("","LD","GR2, 0, GR1");
				createLine("","LD","GR3, ="+(variableCount-paraCount+i));
				createLine("","ST","GR2, VAR, GR3");
				createLine("","SUBA","GR1, =1");
			}
			createLine("","LD","GR1, 0, GR8");
			createLine("","ADDA","GR8, ="+paraCount);				
			createLine("","ST","GR1, 0, GR8");
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
			int rabel = ifwhileRabelCount;
			ifwhileRabelCount++;
			String formulaType = formula(tokenArray, scope, false);
			if(formulaType != "BOOLEAN") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			terminator(tokenArray[TokenData.n],"STHEN");
			createLine("","POP","GR1");
			createLine("","CPA","GR1, =#FFFF");
			createLine("","JZE","ELSE"+rabel);
			compositeSentence(tokenArray, scope);
			createLine("","JUMP","ENDIF"+rabel);
			createLine("ELSE"+rabel,"NOP","");
			if(tokenArray[TokenData.n].getId().equals("SELSE")) {
				terminator(tokenArray[TokenData.n],"SELSE");
				compositeSentence(tokenArray, scope);
			}
			createLine("ENDIF"+rabel,"NOP","");
		}else if(tempTokenId.equals("SWHILE")) {
			terminator(tokenArray[TokenData.n],"SWHILE");
			int rabel = ifwhileRabelCount;
			ifwhileRabelCount++;
			createLine("LOOP"+rabel,"NOP","");
			String formulaType = formula(tokenArray, scope, false);
			if(formulaType != "BOOLEAN") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			terminator(tokenArray[TokenData.n],"SDO");
			createLine("","POP","GR1");
			createLine("","CPL","GR1, =#FFFF");
			createLine("","JZE","ENDLP"+rabel);
			compositeSentence(tokenArray, scope);
			createLine("","JUMP","LOOP"+rabel);
			createLine("ENDLP"+rabel,"NOP","");
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
		terminator(tokenArray[TokenData.n],"SASSIGN");
		String formulaType = formula(tokenArray, scope, false);
		if(leftSideType != formulaType) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		createLine("","POP","GR1");
		createLine("","POP","GR2");
		createLine("","ST","GR1,VAR,GR2");
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
			return subscriptVar(tokenArray,scope,leftSideFlag);
		}else {
			TokenData.dec();
			return genuineVar(tokenArray,scope,leftSideFlag);
		}
	}
	
	private static String genuineVar(final TokenData[] tokenArray, String scope, boolean leftSideFlag) throws CheckerError{
		varName(tokenArray,scope);
		if(NameData.containsName(tokenArray[TokenData.n-1].getName(), scope)){
			if(leftSideFlag == true) {
				createLine("","PUSH",NameData.getVarNum(tokenArray[TokenData.n-1].getName(),scope));
				return NameData.getArrayType(tokenArray[TokenData.n-1].getName(),scope);
			}else{
				createLine("","LD","GR2, =" + NameData.getVarNum(tokenArray[TokenData.n-1].getName(),scope));
				createLine("","LD","GR1, VAR, GR2");
				createLine("","PUSH","0, GR1");
				return NameData.getType(tokenArray[TokenData.n-1].getName(),scope);
			}
		}else if(NameData.containsName(tokenArray[TokenData.n-1].getName(), "GROBAL")) {
			if(leftSideFlag == true) {
				createLine("","PUSH",NameData.getVarNum(tokenArray[TokenData.n-1].getName(),"GROBAL"));
				return NameData.getArrayType(tokenArray[TokenData.n-1].getName(),"GROBAL");
			}else {
				createLine("","LD","GR2, =" + NameData.getVarNum(tokenArray[TokenData.n-1].getName(),"GROBAL"));
				createLine("","LD","GR1, VAR, GR2");
				createLine("","PUSH","0, GR1");
				return NameData.getType(tokenArray[TokenData.n-1].getName(),"GROBAL");
			}
		}else throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
	}

	private static String subscriptVar(final TokenData[] tokenArray, String scope, boolean leftSideFlag) throws CheckerError{
		varName(tokenArray,scope);
		String varType;
		if(NameData.containsName(tokenArray[TokenData.n-1].getName(), scope)) {
			varType = NameData.getType(tokenArray[TokenData.n-1].getName(),scope);
		}else if(NameData.containsName(tokenArray[TokenData.n-1].getName(), "GROBAL")) {
			varType = NameData.getType(tokenArray[TokenData.n-1].getName(),"GROBAL");			
		}else throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		createLine("","PUSH",NameData.getVarNum(tokenArray[TokenData.n-1].getName(),scope));
		terminator(tokenArray[TokenData.n],"SLBRACKET");
		subscript(tokenArray, scope);
		createLine("","POP","GR1");
		createLine("","POP","GR2");
		createLine("","ADDA","GR1, GR2");
		if(!leftSideFlag) {
			createLine("","LD","GR2, VAR, GR1");
			createLine("","PUSH","0, GR2");			
		}else {
			createLine("","PUSH","0, GR1");			
		}
		terminator(tokenArray[TokenData.n],"SRBRACKET");
		return varType;
	}
	
	private static void subscript(final TokenData[] tokenArray, String scope) throws CheckerError{
		String formulaType = formula(tokenArray, scope, false);
		if(formulaType != "INTEGER") throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
	}
	
	private static void procedureCallSentence(final TokenData[] tokenArray, String scope) throws CheckerError{
		String name = procedureName(tokenArray);
		int rabel = subProgramList.indexOf(name);
		if(!NameData.containsProcedureName(name)) {
			 throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		}
		if(tokenArray[TokenData.n].getId().equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			formulaList(tokenArray, scope, false);
			terminator(tokenArray[TokenData.n],"SRPAREN");
		}
		createLine("","CALL","PROC"+rabel);
	}

	private static void formulaList(final TokenData[] tokenArray, String scope, boolean outputFlag) throws CheckerError{
		String formulaType = formula(tokenArray, scope,outputFlag);
		if(outputFlag == false) {
			
		}else {
			if(formulaType == "INTEGER") {
				createLine("","POP","GR2");	
				createLine("","CALL","WRTINT");				
			}else if(formulaType == "STRING") {
				createLine("","POP","GR2");	
				createLine("","POP","GR1");	
				createLine("","CALL","WRTSTR");			
			}else if(formulaType == "CHAR"){
				createLine("","POP","GR2");	
				createLine("","CALL","WRTCH");	
			}else {
			}
		}
		while(tokenArray[TokenData.n].getId().equals("SCOMMA")) {
			terminator(tokenArray[TokenData.n],"SCOMMA");
			formulaType = formula(tokenArray, scope,outputFlag);
			if(outputFlag == false) {
				
			}else {
				if(formulaType == "INTEGER") {
					createLine("","POP","GR2");	
					createLine("","CALL","WRTINT");				
				}else if(formulaType == "STRING") {
					createLine("","POP","GR2");	
					createLine("","POP","GR1");	
					createLine("","CALL","WRTSTR");			
				}else if(formulaType == "CHAR"){
					createLine("","POP","GR2");	
					createLine("","CALL","WRTCH");	
				}else {
				}
			}
		}
		if(outputFlag) {
			createLine("","CALL","WRTLN");			
		}
	}
	
	private static String formula(final TokenData[] tokenArray, String scope, boolean outputFlag) throws CheckerError{
		String formulaType1 = simpleFormula(tokenArray, scope,outputFlag);
		String formulaType2 = formulaType1;
		String tempTokenId = tokenArray[TokenData.n].getId();
		boolean relationFlag = false;
		String relationType = "";
		int rabel = relationRabelCount;
		if(tempTokenId.equals("SEQUAL") || tempTokenId.equals("SNOTEQUAL") || tempTokenId.equals("SLESS") || tempTokenId.equals("SLESSEQUAL") || tempTokenId.equals("SGREAT") || tempTokenId.equals("SGREATEQUAL")) {
			relationType = relationOpe(tokenArray);
			relationRabelCount++;
			relationFlag = true;
			formulaType2 = simpleFormula(tokenArray, scope, outputFlag);
			createLine("","POP","GR2");
			createLine("","POP","GR1");
		}
		if(formulaType1 != formulaType2) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
		if(relationFlag == true) {
			createLine("","CPA","GR1, GR2");
			if(relationType.equals("SEQUAL")) {
				createLine("","JZE","TRUE"+rabel);		
				createLine("","LD","GR1, =#FFFF");
				createLine("","JUMP","BOTH"+rabel);
				createLine("TRUE"+rabel,"LD","GR1, =#0000");
				createLine("BOTH"+rabel,"PUSH","0, GR1");
			}else if(relationType.equals("SNOTEQUAL")) {
				createLine("","JNZ","TRUE"+rabel);	
				createLine("","LD","GR1, =#FFFF");
				createLine("","JUMP","BOTH"+rabel);
				createLine("TRUE"+rabel,"LD","GR1, =#0000");
				createLine("BOTH"+rabel,"PUSH","0, GR1");
			}else if(relationType.equals("SLESS")) {
				createLine("","JMI","TRUE"+rabel);	
				createLine("","LD","GR1, =#FFFF");
				createLine("","JUMP","BOTH"+rabel);
				createLine("TRUE"+rabel,"LD","GR1, =#0000");
				createLine("BOTH"+rabel,"PUSH","0, GR1");
			}else if(relationType.equals("SLESSEQUAL")) {
				createLine("","JPL","FALSE"+rabel);		
				createLine("","LD","GR1, =#0000");
				createLine("","JUMP","BOTH"+rabel);
				createLine("FALSE"+rabel,"LD","GR1, =#FFFF");
				createLine("BOTH"+rabel,"PUSH","0, GR1");
			}else if(relationType.equals("SGREAT")) {
				createLine("","JPL","TRUE"+rabel);		
				createLine("","LD","GR1, =#FFFF");
				createLine("","JUMP","BOTH"+rabel);
				createLine("TRUE"+rabel,"LD","GR1, =#0000");
				createLine("BOTH"+rabel,"PUSH","0, GR1");
			}else if(relationType.equals("SGREATEQUAL")) {
				createLine("","JMI","FALSE"+rabel);		
				createLine("","LD","GR1, =#0000");
				createLine("","JUMP","BOTH"+rabel);
				createLine("FALSE"+rabel,"LD","GR1, =#FFFF");
				createLine("BOTH"+rabel,"PUSH","0, GR1");
			}
			return "BOOLEAN";
		}else {
			return formulaType1;			
		}
	}

	private static String simpleFormula(final TokenData[] tokenArray, String scope, boolean outputFlag) throws CheckerError{
		String sign = "";
		if(tokenArray[TokenData.n].getId().equals("SPLUS") || tokenArray[TokenData.n].getId().equals("SMINUS")){
			sign = sign(tokenArray);
		}
		String termType1 = term(tokenArray, scope, outputFlag);
		String termType2 = termType1;
		String returnType = termType1;
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(sign.equals("SMINUS")) {
			createLine("","POP","GR2");
			createLine("","LD","GR1, =0");
			createLine("","SUBA","GR1, GR2");
			createLine("","PUSH","0, GR1");
		}
		while(tempTokenId.equals("SPLUS") || tempTokenId.equals("SMINUS") || tempTokenId.equals("SOR")) {
			returnType = additionOpe(tokenArray);
			termType2 = term(tokenArray, scope, outputFlag);
			createLine("","POP","GR2");
			createLine("","POP","GR1");
			if(termType1 != termType2) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "INTEGER" && termType1 != "INTEGER")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "BOOLEAN" && termType1 != "BOOLEAN")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");	
			if(tempTokenId.equals("SPLUS")) {
				createLine("","ADDA","GR1,GR2");
			}else if(tempTokenId.equals("SMINUS")) {
				createLine("","SUBA","GR1,GR2");
			}else if(tempTokenId.equals("SOR")) {
				createLine("","OR","GR1,GR2");
			}
			createLine("","PUSH","0,GR1");
			tempTokenId = tokenArray[TokenData.n].getId();
		}
		return returnType;
	}
	
	private static String term(final TokenData[] tokenArray, String scope, boolean outputFlag) throws CheckerError{
		String factType1 = fact(tokenArray, scope, outputFlag);
		String factType2 = factType1;
		String returnType = factType1;
		String tempTokenId = tokenArray[TokenData.n].getId();
		while(tempTokenId.equals("SSTAR") || tempTokenId.equals("SDIVD") || tempTokenId.equals("SMOD") || tempTokenId.equals("SAND")) {
			returnType = multiOpe(tokenArray);
			factType2 = fact(tokenArray, scope, outputFlag);
			createLine("","POP","GR2");
			createLine("","POP","GR1");
			if(factType1 != factType2) throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "INTEGER" && factType1 != "INTEGER")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(returnType == "BOOLEAN" && factType1 != "BOOLEAN")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			if(tempTokenId.equals("SSTAR")) {
				createLine("","CALL","MULT");
				createLine("","PUSH","0,GR2");
			}else if(tempTokenId.equals("SDIVD")) {
				createLine("","CALL","DIV");
				createLine("","PUSH","0,GR2");
			}else if(tempTokenId.equals("SMOD")) {
				createLine("","CALL","DIV");
				createLine("","PUSH","0,GR1");
			}else if(tempTokenId.equals("SAND")) {
				createLine("","AND","GR1,GR2");
				createLine("","PUSH","0,GR1");
			}
			tempTokenId = tokenArray[TokenData.n].getId();
		}
		return returnType;
	}

	private static String fact(final TokenData[] tokenArray, String scope, boolean outputFlag) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SIDENTIFIER")) {
			return variable(tokenArray,scope,false);
		}else if(tempTokenId.equals("SCONSTANT") || tempTokenId.equals("SSTRING") || tempTokenId.equals("SFALSE") || tempTokenId.equals("STRUE")) {
			return constant(tokenArray, outputFlag);
		}else if(tempTokenId.equals("SLPAREN")) {
			terminator(tokenArray[TokenData.n],"SLPAREN");
			String formulaType = formula(tokenArray, scope, outputFlag);
			terminator(tokenArray[TokenData.n],"SRPAREN");
			return formulaType;
		}else if(tempTokenId.equals("SNOT")) {
			terminator(tokenArray[TokenData.n],"SNOT");
			String factType = fact(tokenArray, scope, outputFlag);
			if(factType != "BOOLEAN")  throw new CheckerError(tokenArray[TokenData.n-1].getLine(), "Semantic");
			createLine("","POP","GR1");
			createLine("","XOR","GR1, =#FFFF");
			createLine("","PUSH","0, GR1");
			return factType;
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}

	private static String relationOpe(final TokenData[] tokenArray) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SEQUAL")) {
			terminator(tokenArray[TokenData.n],"SEQUAL");
			return "SEQUAL";
		}else if(tempTokenId.equals("SNOTEQUAL")) {
			terminator(tokenArray[TokenData.n],"SNOTEQUAL");
			return "SNOTEQUAL";
		}else if(tempTokenId.equals("SLESS")) {
			terminator(tokenArray[TokenData.n],"SLESS");
			return "SLESS";
		}else if(tempTokenId.equals("SLESSEQUAL")) {
			terminator(tokenArray[TokenData.n],"SLESSEQUAL");
			return "SLESSEQUAL";
		}else if(tempTokenId.equals("SGREAT")) {
			terminator(tokenArray[TokenData.n],"SGREAT");
			return "SGREAT";
		}else if(tempTokenId.equals("SGREATEQUAL")) {
			terminator(tokenArray[TokenData.n],"SGREATEQUAL");
			return "SGREATEQUAL";
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
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
				formulaList(tokenArray,scope, true);
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
	
	private static String constant(final TokenData[] tokenArray, boolean outputFlag) throws CheckerError{
		String tempTokenId = tokenArray[TokenData.n].getId();
		if(tempTokenId.equals("SCONSTANT")) {
			String num = tokenArray[TokenData.n].getName();
			terminator(tokenArray[TokenData.n],"SCONSTANT");
			createLine("","PUSH",num);
			return "INTEGER";
		}else if(tempTokenId.equals("SSTRING")) {
			String str = tokenArray[TokenData.n].getName();
			terminator(tokenArray[TokenData.n],"SSTRING");
			if(outputFlag) {    
				if((str.length() - 2) == 1) {
					createLine("","LD","GR1, ="+tokenArray[TokenData.n-1].getName());
					createLine("","PUSH","0, GR1");	
					return "CHAR";
				}
				StringStack.stringStack(str);
				int strLen = StringStack.stringLength(str);
				int strNum = StringStack.stringNumber(str);
				createLine("","LD","GR1, ="+strLen);
				createLine("","PUSH","0, GR1");				
				createLine("","LAD","GR2, CHAR"+strNum);
				createLine("","PUSH","0, GR2");
				return "STRING";
			}else {
				createLine("","LD","GR1, ="+tokenArray[TokenData.n-1].getName());
				createLine("","PUSH","0, GR1");	
				return "CHAR";
			}
		}else if(tempTokenId.equals("SFALSE")) {
			terminator(tokenArray[TokenData.n],"SFALSE");
			createLine("","PUSH","#FFFF");
			return "BOOLEAN";
		}else if(tempTokenId.equals("STRUE")) {
			terminator(tokenArray[TokenData.n],"STRUE");
			createLine("","PUSH","#0000");
			return "BOOLEAN";
		}else throw new CheckerError(tokenArray[TokenData.n].getLine(), "Syntax");
	}
	
}