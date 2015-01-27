package cop5555sp15;


import static cop5555sp15.TokenStream.Kind.AND;
import static cop5555sp15.TokenStream.Kind.ARROW;
import static cop5555sp15.TokenStream.Kind.ASSIGN;
import static cop5555sp15.TokenStream.Kind.AT;
import static cop5555sp15.TokenStream.Kind.BAR;
import static cop5555sp15.TokenStream.Kind.BL_FALSE;
import static cop5555sp15.TokenStream.Kind.BL_TRUE;
import static cop5555sp15.TokenStream.Kind.COLON;
import static cop5555sp15.TokenStream.Kind.COMMA;
import static cop5555sp15.TokenStream.Kind.DIV;
import static cop5555sp15.TokenStream.Kind.DOT;
import static cop5555sp15.TokenStream.Kind.EOF;
import static cop5555sp15.TokenStream.Kind.EQUAL;
import static cop5555sp15.TokenStream.Kind.GE;
import static cop5555sp15.TokenStream.Kind.GT;
import static cop5555sp15.TokenStream.Kind.IDENT;
import static cop5555sp15.TokenStream.Kind.ILLEGAL_CHAR;
import static cop5555sp15.TokenStream.Kind.INT_LIT;
import static cop5555sp15.TokenStream.Kind.KW_BOOLEAN;
import static cop5555sp15.TokenStream.Kind.KW_CLASS;
import static cop5555sp15.TokenStream.Kind.KW_DEF;
import static cop5555sp15.TokenStream.Kind.KW_ELSE;
import static cop5555sp15.TokenStream.Kind.KW_IF;
import static cop5555sp15.TokenStream.Kind.KW_IMPORT;
import static cop5555sp15.TokenStream.Kind.KW_INT;
import static cop5555sp15.TokenStream.Kind.KW_PRINT;
import static cop5555sp15.TokenStream.Kind.KW_RETURN;
import static cop5555sp15.TokenStream.Kind.KW_STRING;
import static cop5555sp15.TokenStream.Kind.KW_WHILE;
import static cop5555sp15.TokenStream.Kind.LCURLY;
import static cop5555sp15.TokenStream.Kind.LE;
import static cop5555sp15.TokenStream.Kind.LPAREN;
import static cop5555sp15.TokenStream.Kind.LSHIFT;
import static cop5555sp15.TokenStream.Kind.LSQUARE;
import static cop5555sp15.TokenStream.Kind.LT;
import static cop5555sp15.TokenStream.Kind.MINUS;
import static cop5555sp15.TokenStream.Kind.MOD;
import static cop5555sp15.TokenStream.Kind.NL_NULL;
import static cop5555sp15.TokenStream.Kind.NOT;
import static cop5555sp15.TokenStream.Kind.NOTEQUAL;
import static cop5555sp15.TokenStream.Kind.PLUS;
import static cop5555sp15.TokenStream.Kind.QUESTION;
import static cop5555sp15.TokenStream.Kind.RANGE;
import static cop5555sp15.TokenStream.Kind.RCURLY;
import static cop5555sp15.TokenStream.Kind.RPAREN;
import static cop5555sp15.TokenStream.Kind.RSHIFT;
import static cop5555sp15.TokenStream.Kind.RSQUARE;
import static cop5555sp15.TokenStream.Kind.SEMICOLON;
import static cop5555sp15.TokenStream.Kind.STRING_LIT;
import static cop5555sp15.TokenStream.Kind.TIMES;
import static cop5555sp15.TokenStream.Kind.UNTERMINATED_COMMENT;
import static cop5555sp15.TokenStream.Kind.UNTERMINATED_STRING;
import cop5555sp15.TokenStream.Kind;
import cop5555sp15.TokenStream.Token;

public class Scanner {

	private TokenStream ts;
	private char[] inputChars;
	
	public Scanner(TokenStream stream) {
		ts = stream;
		inputChars = stream.inputChars;
	}

	public void scan() {

		int i = 0;
		int line = 1;
		int beg = 0;
		int end = 0;
		int offset = 0;
		char next;
		boolean stringset = false;
		boolean commentset = false;
		
		StringBuilder sb1 = new StringBuilder();	// strings 
		
		while(i<inputChars.length && inputChars[i] != '\0') {
			
			if(Character.isDigit(inputChars[i])) {	// this is a digit
				sb1.append(inputChars[i]);
				offset++;
			}
			else if(Character.isLetter(inputChars[i])) {	// character, maybe part of string or identifier
				sb1.append(inputChars[i]);
				offset++;
			}
			else if(Character.isWhitespace(inputChars[i])) {	// whitespace, newline
				
				if(inputChars[i] == '\n' || inputChars[i] == '\r' ) { // newline
					
					if(inputChars[i] == '\n') {
						line ++;							
					}
					else if(inputChars[i] == '\r' && inputChars[i+1] == '\n') {
						line ++;
						if(!stringset && !commentset)
							beg++;
						i++;
					}
					else if(inputChars[i] == '\r' && inputChars[i+1] != '\n') {
						line ++;
					}
					
					if(stringset || commentset) {	// append the whitespace, if part of string or comment
						offset++;
					}
					
					String temp = sb1.toString();
					
					if(temp.length()==0 && stringset) {	// string incomplete
						ts.tokens.add(ts.new Token(UNTERMINATED_STRING, beg, beg + offset, line));
						beg = beg + offset;
						offset = 0;
						sb1 = new StringBuilder();
					}
					
					if(temp.length() !=0 && !stringset && !commentset) {	// complete the remaining tokens
						
						if(isNumeric(temp)) {	// check if its number
							ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
							beg = beg + offset;
							offset = 0;
							sb1 = new StringBuilder(); 
						}
						else {
							if(Character.isJavaIdentifierStart(temp.charAt(0))) {	// check if token can be reserved keyword(implemented as special case of identifiers)
								Kind k = match(temp);
								ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
								beg = beg + offset;
								offset = 0;
							}
							else {			// this is combination of number and string "24a", separate the two parts
								int l = 0;
								while(!Character.isJavaIdentifierStart(temp.charAt(l))){
									l++;
								}
								ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
								beg = beg + l;
								offset = offset - l;
								ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
								beg = beg + offset;
								offset = 0;
							}
							sb1 = new StringBuilder();	// token detected so clear the previous token
						}		
					}
					if(!stringset && !commentset) beg++; // since token was found update the beg index
				}
				else { // just a space
					
					if(stringset || commentset) {	// if space is part of string or comment, just append and update the offset
						sb1.append(inputChars[i]);
						offset++;
					}
					
					else {	// complete any previous tokens, if any
						String temp = sb1.toString();
						if(temp.length() != 0) {
							if(isNumeric(temp)) {
								ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
								beg = beg + offset;
								offset = 0;
								sb1 = new StringBuilder();
							}
							else {
								if(Character.isJavaIdentifierStart(temp.charAt(0))) {
									Kind k = match(temp);
									ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
								}
								else {
									int l = 0;
									while(!Character.isJavaIdentifierStart(temp.charAt(l))){
										l++;
									}
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
									beg = beg + l;
									offset = offset - l;
									ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
								}
								sb1 = new StringBuilder();
							}
						}
						beg++;
					}
				}		
			}
			else {	// not a character, digit or whitespace...hence special chars go here
				String temp;
				switch(inputChars[i]) {
	
					case '"':					// indicates start/end of string
						if(commentset) {					// prioritize comment if, this quote is part of the comment, append it and continue
							sb1.append(inputChars[i]);
							offset ++;
						}								
						else if(stringset) {			// end of string, since stringset is true; hence complete the string token 
							sb1.append(inputChars[i]);
							offset ++;
							stringset = false;
							end = beg + offset;
							ts.tokens.add(ts.new Token(STRING_LIT, beg, end, line));
							beg = beg + offset;
							offset = 0;
							sb1 = new StringBuilder(); 
						}
						else {						// start of string, initialize things
							stringset = true;
							sb1.append(inputChars[i]);
							offset ++;
						}
						break;	
						
					case '{':
						if(stringset  || commentset) { 	// if string or comment, just append to it and continue  
							sb1.append(inputChars[i]);
							offset++;
						}
						else {	// complete any of earlier tokens
							temp = sb1.toString();
							if(temp.length() != 0) {	// some earlier token exists, classify it and add
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							// add the current token and update the variables
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(LCURLY, beg, end, line));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '}':
						if(stringset || commentset) {
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))) {
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(RCURLY, beg, end, line));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '[':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}						}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(LSQUARE,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case ']':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(RSQUARE,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '(':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(LPAREN,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case ')':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(RPAREN,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case ',':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(COMMA,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case ';':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(SEMICOLON,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case ':':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(COLON,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '?':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(QUESTION,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '@':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(AT,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
					
					case '%':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(MOD,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '|':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(BAR,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '&':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(AND,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '+':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(PLUS,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
						break;
						
					case '/':
						/*
						 * 1. string or comment continue 
						 * 2. check if any prev token going on, complete it
						 * 3. check if '/' is followed by '*', then comment 
						 * 4. else its DIVIDE
						 */
						if(stringset || commentset) {	// 1
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();	// 2
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
						
							if(i == inputChars.length -1) {	// edge case where this is last character, add current token and finish
								offset++;
								end = beg + offset;
								ts.tokens.add(ts.new Token(DIV,beg, end, line ));
								beg = beg + offset;
								offset = 0;	
							}
							else {
								next = inputChars[i+1];
								if(next == '*'){		// check if next char make it to start of comment; set appropriate variables
									offset+=2;
									commentset = true;
									i++;
								}
								else {				// else this is simple '/' token, add it and continue
									offset++;
									end = beg + offset;
									ts.tokens.add(ts.new Token(DIV,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
								}
							}
						}
						break;
						
					case '*':
						if(stringset) {						// if string append this and continue
							sb1.append(inputChars[i]);
							offset++;
						}
						else if(commentset) {				// if comment, potential for comment termination; check it 
							if(i < inputChars.length -1) {	// edge case
								next = inputChars[i+1];
								if(next == '/'){
									offset+=2;
									// update beg and offset, no need for adding token since its comment		
									beg = beg + offset;
									offset = 0;	
									commentset = false;
									sb1 = new StringBuilder();
									i++;
								}
								else {
									sb1.append(inputChars[i]);
									offset++;
								}
							}
							else {
								sb1.append(inputChars[i]);
								offset++;
							}
						}
						else {								// else fallback to normal token resolution
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(TIMES,beg, end, line ));
							beg = beg + offset;
							offset = 0;	
						}
						break;
						
						
					case '.':	
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {												
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							
							if(i == inputChars.length -1) {
								offset++;
								end = beg + offset;
								ts.tokens.add(ts.new Token(DOT,beg, end, line ));
								beg = beg + offset;
								offset = 0;	
							}
							else {
								next = inputChars[i+1];
								if(next == '.'){
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(RANGE,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								else{
									offset++;
									end = beg + offset;
									ts.tokens.add(ts.new Token(DOT,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
								}
							}
						}
						break;
					
					case '=':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							if(i == inputChars.length -1) {
								offset++;
								end = beg + offset;
								ts.tokens.add(ts.new Token(ASSIGN,beg, end, line ));
								beg = beg + offset;
								offset = 0;	
							}
							else {
								next = inputChars[i+1];
								if(next == '='){
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(EQUAL,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								else{
									offset++;
									end = beg + offset;
									ts.tokens.add(ts.new Token(ASSIGN,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
								}
							}
						}
						break;
						
					case '!':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							if(i == inputChars.length-1){
								offset++;
								end = beg + offset;
								ts.tokens.add(ts.new Token(NOT,beg, end, line ));
								beg = beg + offset;
								offset = 0;	
							}
							else{
								next = inputChars[i+1];
								if(next == '='){
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(NOTEQUAL,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								else{
									offset++;
									end = beg + offset;
									ts.tokens.add(ts.new Token(NOT,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
								}
							}		
						}
						break;	
						
					case '<':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							if(i == inputChars.length -1) {
								offset++;
								end = beg + offset;
								ts.tokens.add(ts.new Token(LT,beg, end, line ));
								beg = beg + offset;
								offset = 0;	
							}
							else {
								next = inputChars[i+1];
								if(next == '='){
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(LE,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								
								else if(next == '<') {
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(LSHIFT,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								
								else{
									offset++;
									end = beg + offset;
									ts.tokens.add(ts.new Token(LT,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
								}
							}
						}
						break;
					
					case '>':
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							if(i == inputChars.length -1) {
								offset++;
								end = beg + offset;
								ts.tokens.add(ts.new Token(GT,beg, end, line ));
								beg = beg + offset;
								offset = 0;	
							}
							else {
								next = inputChars[i+1];
								if(next == '='){
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(GE,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								
								else if(next == '>') {
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(RSHIFT,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								
								else{
									offset++;
									end = beg + offset;
									ts.tokens.add(ts.new Token(GT,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
								}
							}
						}
						break;
					
					case '-':
						
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							temp = sb1.toString();
							if(temp.length() != 0) {
								if(isNumeric(temp)) {
									ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
									beg = beg + offset;
									offset = 0;
									sb1 = new StringBuilder();
								}
								else {
									if(Character.isJavaIdentifierStart(temp.charAt(0))) {
										Kind k = match(temp);
										ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									else {
										int l = 0;
										while(!Character.isJavaIdentifierStart(temp.charAt(l))){
											l++;
										}
		
										ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
										beg = beg + l;
	
										offset = offset - l;
										ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
										beg = beg + offset;
										offset = 0;
									}
									sb1 = new StringBuilder();
								}
							}
							if(i == inputChars.length -1) {
								offset++;
								end = beg + offset;
								ts.tokens.add(ts.new Token(MINUS,beg, end, line ));
								beg = beg + offset;
								offset = 0;	
							}
							else {
								next = inputChars[i+1];
								if(next == '>'){
									offset+=2;
									end = beg + offset;
									ts.tokens.add(ts.new Token(ARROW,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
									i++;
								}
								else{
									offset++;
									end = beg + offset;
									ts.tokens.add(ts.new Token(MINUS,beg, end, line ));
									beg = beg + offset;
									offset = 0;	
								}
							}
						}
						break;
						
					default:
						if(stringset || commentset){
							sb1.append(inputChars[i]);
							offset++;
						}
						else {
							offset++;
							end = beg + offset;
							ts.tokens.add(ts.new Token(ILLEGAL_CHAR,beg, end, line ));
							beg = beg + offset;
							offset = 0;
						}
					}				
			}
			i++;
		}
		
		// edge case for unterminated string comment
		if(stringset) {
			ts.tokens.add(ts.new Token(UNTERMINATED_STRING, beg, beg + offset, line));
		}
		else if (commentset){
			ts.tokens.add(ts.new Token(UNTERMINATED_COMMENT, beg, beg + offset, line));
		}
		else {
			String rest = sb1.toString();
			if(rest.length() != 0){
				if(isNumeric(rest)){
					ts.tokens.add(ts.new Token(INT_LIT,beg,beg+offset,line));
					beg = beg + offset;
					offset = 0;
				}
				else {
					if(Character.isJavaIdentifierStart(rest.charAt(0))) {
						Kind k = match(rest);
						ts.tokens.add(ts.new Token(k,beg,beg+offset,line));
						beg = beg + offset;
						offset = 0;
					}
					else {
						int l = 0;
						while(!Character.isJavaIdentifierStart(rest.charAt(l))){
							l++;
						}
						ts.tokens.add(ts.new Token(INT_LIT,beg,beg+l,line));
						beg = beg + l;
						offset = offset - l;
						ts.tokens.add(ts.new Token(IDENT,beg,beg+offset,line));
						beg = beg + offset;
						offset = 0;
					}
					sb1 = new StringBuilder();
				}
			}
		}
		
		ts.tokens.add(ts.new Token(EOF,i,i,line));	// add the default EOF token no matter what
	}

	
	static boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}
	
	public static void main (String [] args) {
        TokenStream st = new TokenStream("sc.getVal();");
        System.out.println(st.toString());
        try{
                //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                //TokenStream st = new TokenStream(reader);
                Scanner sc = new Scanner(st);
                sc.scan();
                //System.out.println(st.tokenListToString());
                //System.out.println(st.tokenTextListToString());
                StringBuffer sb = new StringBuffer();
                for (Token t: st.tokens){
                        sb.append(t.beg +" "+ t.end + " " + t.getLineNumber() + " " + t.kind + " " + t.getText() + "\n");
                }
                String output = sb.toString();
                System.out.println(output);
        }
        catch(Exception e) {
                e.printStackTrace();
        }
        
	}
	
	static Kind match(String s) {	// matched reserved words, else returns identifier
		switch(s) {
			case "int":
				return KW_INT;
			case "string":
				return KW_STRING;
			case "boolean":
				return KW_BOOLEAN;
			case "import":
				return KW_IMPORT;
			case "class":
				return KW_CLASS;
			case "def":
				return KW_DEF;
			case "while":
				return KW_WHILE;
			case "if":
				return KW_IF;
			case "else":
				return KW_ELSE;
			case "return":
				return KW_RETURN;
			case "print":
				return KW_PRINT;
			case "true":
				return BL_TRUE;
			case "false":
				return BL_FALSE;
			case "null":
				return NL_NULL;
		}
		return IDENT;
	}
	
}

