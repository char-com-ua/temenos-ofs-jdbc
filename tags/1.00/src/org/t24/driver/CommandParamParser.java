package org.t24.driver;

import java.util.ArrayList;
import java.util.List;



class CommandParamParser {
	enum State { DEF, TOK, QTOK, QTOK_END };
	
	public static List<String> parse(String sin)throws T24ParseException{
		List<String> out=new ArrayList<String>();
		int len=sin.length();
		
		State state=State.DEF;
		StringBuilder token=new StringBuilder();
		
		for(int i=0; i<len; i++){
			char c=sin.charAt(i);
			switch(c){
				case ' ':
				case '\t':
					switch(state) {
						case DEF:
							//nothing to do
							break;
						case TOK:
							out.add(token.toString());
							token=new StringBuilder();
							state=State.DEF;
							break;
						case QTOK:
							token.append(c);
							break;
						case QTOK_END:
							out.add(token.toString());
							token=new StringBuilder();
							state=State.DEF;
							break;
					}
					break;
				case '"':
					switch(state) {
						case DEF:
							state=State.QTOK;
							break;
						case TOK:
							token.append(c);
							break;
						case QTOK:
							state=State.QTOK_END;
							break;
						case QTOK_END:
							token.append(c);
							state=State.QTOK;
							break;
					}
					break;
				default:
					switch(state) {
						case DEF:
							token.append(c);
							state=State.TOK;
							break;
						case TOK:
							token.append(c);
							break;
						case QTOK:
							token.append(c);
							break;
						case QTOK_END:
							throw new T24ParseException("After doublequote should be space or another doublequote");
					}
					break;
			}
		}
		switch(state) {
			case DEF:
				//nothing to do
				break;
			case TOK:
				out.add(token.toString());
				break;
			case QTOK:
				throw new T24ParseException("Command parser error: last token not quoted");
			case QTOK_END:
				out.add(token.toString());
				break;
		}
		return out;
	}
	
	
}
