import java.io.*;
import java.util.*;

public class mylang
{




    public static void main(String[] args)
    {

	 /*
	java is also a main calss like this one where
	String []args has been defined and it takes the input in this 
        Similarly,I am thaking input as array 
	*/

        if (args.length < 1) {
            System.out.println("\n\njava mylang file.txt");//to give error if less than 1
            return;
        }

        String contents = readFile(args[0]);//read file is a methode not only this remember that arg[0] contains the path of the file because you write java mylang try.txt

        mylang mylob = new mylang();
        mylob.interpret(contents);

    }//end of main function 








    private final Map<String, Value> variables;//takes in integers along with the variable name eg : for expressinon (a=5) String/variable = a and 5 value 
    private final Map<String, Integer> labels;//takes in strings 
    private final BufferedReader lineIn;
    private int currentSta;





    public mylang() {
        variables = new HashMap<String, Value>();
        labels = new HashMap<String, Integer>();

        InputStreamReader converter = new InputStreamReader(System.in);
        lineIn = new BufferedReader(converter);
    }

    public void interpret(String source) {

        List<Token> tokens = tokenize(source);

        Parser parser = new Parser(tokens);
        List<Sta> Stas = parser.parse(labels);

        currentSta = 0;
        while (currentSta < Stas.size()) {
            int thisSta = currentSta;
            currentSta++;
            Stas.get(thisSta).execute();
        }
    }








//file handeling 

    private static String readFile(String path) {
        try {
            FileInputStream stream = new FileInputStream(path);//path form user

            try {
                InputStreamReader input = new InputStreamReader(stream);//here stream is a variable that take the values form the file
                Reader reader = new BufferedReader(input);

                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[8192];//the max langth of program that can be typed in a line
                int read;

                while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                    builder.append(buffer, 0, read);
                }

                builder.append("\n");

                return builder.toString();
            }
            finally {
                stream.close();
            }
        }
        catch (IOException ex) {
            return null;
        }
    }


    private static List<Token> tokenize(String source)
    {
        List<Token> tokens = new ArrayList<Token>();

        String token = "";
        TokenizeState state = TokenizeState.DEFAULT;

        String charTokens = "\n=+-*/<>()";
        TokenType[] tokenTypes = { TokenType.LINE, TokenType.EQUALS,
                TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR,
                TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR,
                TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN
            };

        for (int i = 0; i < source.length(); i++)
        {
            char c = source.charAt(i);
            switch (state)
            {
                case DEFAULT:
                if (charTokens.indexOf(c) != -1) {
                    tokens.add(new Token(Character.toString(c),
                            tokenTypes[charTokens.indexOf(c)]));
                }
                else if (Character.isLetter(c)) {
                    token += c;
                    state = TokenizeState.WORD;
                }
                else if (Character.isDigit(c)) {
                    token += c;
                    state = TokenizeState.NUMBER;
                }
                break;

                case WORD:
                if (Character.isLetterOrDigit(c)) {
                    token += c;
                }
                else if (c == ':') {
                    tokens.add(new Token(token, TokenType.LABEL));
                    token = "";
                    state = TokenizeState.DEFAULT;
                }
                else {
                    tokens.add(new Token(token, TokenType.WORD));
                    token = "";
                    state = TokenizeState.DEFAULT;
                    i--;
                }
                break;

                case NUMBER:
                if (Character.isDigit(c)) {
                    token += c;
                }
                else {
                    tokens.add(new Token(token, TokenType.NUMBER));
                    token = "";
                    state = TokenizeState.DEFAULT;
                    i--;
                }
                break;

                case STRING:
                if (c == '"') {
                    tokens.add(new Token(token, TokenType.STRING));
                    token = "";
                    state = TokenizeState.DEFAULT;
                }
                else {
                    token += c;
                }
                break;

                case COMMENT:
                if (c == '\n') {
                    state = TokenizeState.DEFAULT;
                }
                break;
            }
        }

        return tokens;
    }
    private enum TokenType {
        WORD, NUMBER, STRING, LABEL, LINE,
        EQUALS, OPERATOR, LEFT_PAREN, RIGHT_PAREN, EOF
    }

    private static class Token {
        public Token(String text, TokenType type) {
            this.text = text;
            this.type = type;
        }

        public final String text;
        public final TokenType type;
    }


    private enum TokenizeState {
        DEFAULT, WORD, NUMBER, STRING, COMMENT
    }
    private class Parser {
        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            position = 0;
        }

        public List<Sta> parse(Map<String, Integer> labels) {
            List<Sta> Stas = new ArrayList<Sta>();

            while (true) {

                while (match(TokenType.LINE));

                if (match(TokenType.LABEL)) {

                    labels.put(last(1).text, Stas.size());
                }
                else if (match(TokenType.WORD, TokenType.EQUALS)) {
                    String name = last(2).text;
                    Exp value = Exp();
                    Stas.add(new Asssta(name, value));
                }
                else if (match("print")) {
                    Stas.add(new Print(Exp()));
                }
                else break; 
            }

            return Stas;
        }

        private Exp Exp() {
            return operator();
        }

        private Exp operator() {
            Exp Exp = atomic();

            while (match(TokenType.OPERATOR) ||
            match(TokenType.EQUALS)) {
                char operator = last(1).text.charAt(0);
                Exp right = atomic();
                Exp = new Opeexp(Exp, operator, right);
            }

            return Exp;
        }

        private Exp atomic() {
            if (match(TokenType.WORD)) {

                return new Varexp(last(1).text);
            }
            else if (match(TokenType.NUMBER)) {
                return new Numval(Double.parseDouble(last(1).text));
            }
            else if (match(TokenType.STRING)) {
                return new Strval(last(1).text);
            }
            else if (match(TokenType.LEFT_PAREN)) {

                Exp Exp = Exp();
                consume(TokenType.RIGHT_PAREN);
                return Exp;
            }
            throw new Error("Couldn't parse :(");
        }

        private boolean match(TokenType type1, TokenType type2) {
            if (get(0).type != type1) return false;
            if (get(1).type != type2) return false;
            position += 2;
            return true;
        }

        private boolean match(TokenType type) {
            if (get(0).type != type) return false;
            position++;
            return true;
        }

        private boolean match(String name) {
            if (get(0).type != TokenType.WORD) return false;
            if (!get(0).text.equals(name)) return false;
            position++;
            return true;
        }

        private Token consume(TokenType type) {
            if (get(0).type != type) throw new Error("Expected " + type + ".");
            return tokens.get(position++);
        }

        private Token consume(String name) {
            if (!match(name)) throw new Error("Expected " + name + ".");
            return last(1);
        }

        private Token last(int offset) {
            return tokens.get(position - offset);
        }

        private Token get(int offset) {
            if (position + offset >= tokens.size()) {
                return new Token("", TokenType.EOF);
            }
            return tokens.get(position + offset);
        }

        private final List<Token> tokens;
        private int position;
    }
    public interface Sta {
        void execute();
    }
    public interface Exp {
        Value evaluate();
    }
    public class Print implements Sta {
        public Print(Exp Exp) {
            this.Exp = Exp;
        }

        public void execute() {
            System.out.println(Exp.evaluate().toString());
        }

        private final Exp Exp;
    }
    public class Asssta implements Sta {
        public Asssta(String name, Exp value) {
            this.name = name;
            this.value = value;
        }

        public void execute() {
            variables.put(name, value.evaluate());
        }

        private final String name;
        private final Exp value;
    }

    public class Varexp implements Exp {
        public Varexp(String name) {
            this.name = name;
        }

        public Value evaluate() {
            if (variables.containsKey(name)) {
                return variables.get(name);
            }
            return new Numval(0);
        }

        private final String name;
    }
    public class Opeexp implements Exp {
        public Opeexp(Exp left, char operator,
        Exp right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        public Value evaluate() {
            Value leftVal = left.evaluate();
            Value rightVal = right.evaluate();

            switch (operator) {
                case '=':

                if (leftVal instanceof Numval) {
                    return new Numval((leftVal.toNumber() ==
                            rightVal.toNumber()) ? 1 : 0);
                }
                else {
                    return new Numval(leftVal.toString().equals(
                            rightVal.toString()) ? 1 : 0);
                }
                case '+':
                if (leftVal instanceof Numval) {
                    return new Numval(leftVal.toNumber() +
                        rightVal.toNumber());
                }
                else {
                    return new Strval(leftVal.toString() +
                        rightVal.toString());
                }
                case '-':
                return new Numval(leftVal.toNumber() -
                    rightVal.toNumber());
                case '*':
                return new Numval(leftVal.toNumber() *
                    rightVal.toNumber());
                case '/':
                return new Numval(leftVal.toNumber() /
                    rightVal.toNumber());
            }
            throw new Error("Unknown operator.");
        }
        private final Exp left;
        private final char operator;
        private final Exp right;
    }

    public interface Value extends Exp {
        String toString();

        double toNumber();
    }

    public class Numval implements Value {
        public Numval(double value) {
            this.value = value;
        }

        @Override public String toString() { return Double.toString(value); }

        public double toNumber() { return value; }

        public Value evaluate() { return this; }

        private final double value;
    }

    public class Strval implements Value {
        public Strval(String value) {
            this.value = value;
        }

        @Override public String toString() { return value; }

        public double toNumber() { return Double.parseDouble(value); }

        public Value evaluate() { return this; }

        private final String value;
    }

}
