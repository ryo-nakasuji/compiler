package enshud.s1.lexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

public class Lexer {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Lexer().run("data/pas/normal01.pas", "tmp/out1.ts");
		new Lexer().run("data/pas/normal02.pas", "tmp/out2.ts");
		new Lexer().run("data/pas/normal03.pas", "tmp/out3.ts");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるLexer実行メソッド．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたpasファイルを読み込み，トークン列に分割する．
	 * トークン列は第二引数で指定されたtsファイルに書き出すこと．
	 * 正常に処理が終了した場合は標準出力に"OK"を，
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 * 
	 * @param inputFileName 入力pasファイル名
	 * @param outputFileName 出力tsファイル名
	 */
	public void run(final String inputFileName, final String outputFileName) {
		// TODO
		try
		{
			FileReader fr = new FileReader(inputFileName);
			FileWriter fw = new FileWriter(outputFileName);
			BufferedReader br = new BufferedReader(fr);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			int lineCount = 0;	//行数
			boolean commentCheck = false;
			String line = br.readLine();
			while( line != null) {
				lineCount++;
				String[] lineArray = line.split("");
				for(int i = 0; i < lineArray.length; i++) {
					if(commentCheck) {
						if(lineArray[i].equals("}")) {
							commentCheck = false;
							continue;
						}
						continue;
					}else{
						if(lineArray[i].equals(" ") || lineArray[i].equals("\t")) {
							continue;
						}else if(lineArray[i].equals("{")) {
							commentCheck = true;
							continue;
						}else if(lineArray[i].equals("'")) {
							int j;
							String buf = lineArray[i];
							for(j = i + 1; !(lineArray[j].equals("'")) && j < line.length(); j++){
								buf = buf + lineArray[j];
							}
							buf = buf + lineArray[j];
							i = j;
							printToFile(buf, "SSTRING", 45, lineCount, pw);
							continue;
						}else if(Pattern.matches("^[0-9]+$", lineArray[i])) {
							int j;
							String buf = lineArray[i];
							for(j = i + 1; Pattern.matches("^[0-9]+$", lineArray[j]); j++) {
								buf = buf + lineArray[j];
								if(j == line.length() - 1) {
									j++;
									break;
								}
							}
							i = j - 1;
							printToFile(buf, "SCONSTANT", 44, lineCount, pw);
							continue;
						}else if(Pattern.matches("^[a-zA-Z]+$", lineArray[i])) {
							int j;
							String buf = lineArray[i];
							for(j = i + 1; Pattern.matches("^[0-9a-zA-Z]+$", lineArray[j]); j++) {
								buf = buf + lineArray[j];
								if(j == line.length() - 1) {
									j++;
									break;
								}
							}
							i = j - 1;
							int id = getKeywordId(buf);
							if(id == -1) {
								printToFile(buf, "SIDENTIFIER", 43, lineCount, pw);
							}else {
								printToFile(buf, getName(id), id,lineCount, pw);								
							}
							continue;
						}else if(isSymbol(lineArray[i])) {
							//特殊文字の処理
							if(i < line.length() - 1) {
								String buf = lineArray[i] + lineArray[i+1];
								int id = getSymbolId(buf);
								if(id == -1) {
									id = getSymbolId(lineArray[i]);
									printToFile(lineArray[i], getName(id), id,lineCount, pw);															
								}else {
									printToFile(buf, getName(id), id,lineCount, pw);	
									i = i + 1;
								}								
							}else {
								int id = getSymbolId(lineArray[i]);
								printToFile(lineArray[i], getName(id), id,lineCount, pw);															
							}
							continue;
						}else if(lineArray[i].equals("\n")){
							continue;
						}else {
							//error
						}
					}
				}
				line = br.readLine();
			}
			br.close();
			pw.close();
			System.out.println("OK");
		}
		catch(final FileNotFoundException e) {
			System.err.println("File not found");
		}
		catch(final IOException e){
			System.err.println("File not found");
		}
	}
	
	/**
	 * isSymbol関数 引数が特殊文字の頭文字であればtrue,
	 * そうでなければfalseを返す.
	 */
	private boolean isSymbol(String str) {
		String symbolChar[] = { "=", "<", ">", "+", "-", "*", "/", "(", ")", "[", "]", ";", ":", ".", ","};
		for(String s : symbolChar) {
			if(str.equals(s)) return true;
		}
		return false;
 	}
	
	/** 
	 * getSymbolId関数 
	 * 引数に対応する特殊記号のトークンIDを返す.
	 * 特殊記号でなければ-1を返す. 
	 */
	private int getSymbolId(String str){
		/* symbol配列に特殊記号に含まれる記号列20個を格納する. */
		String symbol[] = { "=", "<>", "<", "<=", ">=", ">", "+", "-", "*", "(", ")", "[", "]", ";", ":", "..", ":=", ",", ".", "/" };
	    for(int i = 0; i < symbol.length - 1; i++){
	    	if(str.equals(symbol[i])) return i + 24;
	    }
	    // "/"だけIDが離れているので別で扱う.
	    if(str.equals(symbol[symbol.length - 1])){
	        return 5;
	    }
	    // 特殊文字でなかったら-1を返す.
	    return -1;
	}
	    
	/** 
	 * getKeywordId関数
	 * 引数に対応する綴り記号のトークンIDを返す.
	 * 綴り記号でなければ-1を返す. 
	 */
	private int getKeywordId(String str){
		/* keyword配列に綴り記号に含まれる文字列24個を格納する. */
		String keyword[] = { "and", "array", "begin", "boolean", "char", "div", "do", "else", "end", "false", "if", "integer", "mod", "not", "of", "or", "procedure", "program", "readln", "then", "true", "var", "while", "writeln" };
	    for(int i = 0; i < keyword.length; i++){
	        if(str.equals(keyword[i])){
	            return i;
	        }
	    }
	    return -1;
	}
	
	/**
	 * トークンIDを受け取り,そのトークン名を返す.
	 * 
	 * @param num トークンID
	 * @return tokenName トークン名
	 */
	private String getName(int num){
		String[] tokenName = { "SAND", "SARRAY", "SBEGIN", "SBOOLEAN", "SCHAR", "SDIVD", "SDO", "SELSE", "SEND", "SFALSE", "SIF", "SINTEGER", "SMOD", "SNOT", "SOF" ,"SOR", "SPROCEDURE", "SPROGRAM", "SREADLN", "STHEN", "STRUE", "SVAR", "SWHILE", "SWRITELN", "SEQUAL", "SNOTEQUAL", "SLESS", "SLESSEQUAL", "SGREATEQUAL", "SGREAT", "SPLUS", "SMINUS", "SSTAR", "SLPAREN", "SRPAREN", "SLBRACKET", "SRBRACKET", "SSEMICOLON", "SCOLON", "SRANGE", "SASSIGN", "SCOMMA", "SDOT", "SIDENTIFIER", "SCONSTANT", "SSTRING" };
		return tokenName[num];
	}
	
	/**
	 * トークン,トークン名,トークンID,行数をファイルに出力する.
	 * 
	 * @param token
	 * @param tokenName
	 * @param tokenId
	 * @param count
	 * @param pw
	 */
	private void printToFile(String token, String name, int id, int count, PrintWriter pw) {
		pw.println(token + "\t" + name + "\t" + id + "\t" + count);
		return;
	}
	
}