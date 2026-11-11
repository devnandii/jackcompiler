public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final XmlWriter xml;
    private final SymbolTable symbolTable;
    private final VMWriter vm;

    private String className;
    private String currentSubroutineName;
    private String currentSubroutineKind;
    private int labelCounter = 0;

    public CompilationEngine(JackTokenizer tokenizer, XmlWriter xml) {
        this(tokenizer, xml, null);
    }

    public CompilationEngine(JackTokenizer tokenizer, XmlWriter xml, VMWriter vm) {
        this.tokenizer = tokenizer;
        this.xml = xml;
        this.vm = vm;
        this.symbolTable = new SymbolTable();
    }

    private boolean match(TokenType type) {
        return tokenizer.peek() != null && tokenizer.peek().type == type;
    }

    private boolean match(TokenType type, String value) {
        return match(type) && tokenizer.peek().lexeme.equals(value);
    }

    private void consume(TokenType type, String expected) {
        if (!match(type)) {
            String actual = tokenizer.peek() == null ? "EOF" : tokenizer.peek().lexeme;
            int line = tokenizer.peek() == null ? 0 : tokenizer.peek().line;
            throw new RuntimeException(
                "Esperado " + expected + " mas encontrado " + actual + " na linha " + line
            );
        }
        xml.writeToken(tokenizer.peek());
        tokenizer.advance();
    }

    private void writePushVariable(String name) {
        if (vm == null) return;

        SymbolTable.Kind kind = symbolTable.kindOf(name);
        int index = symbolTable.indexOf(name);
        String segment;

        switch (kind) {
            case STATIC: segment = "static"; break;
            case FIELD: segment = "this"; break;
            case ARG: segment = "argument"; break;
            case VAR: segment = "local"; break;
            default:
                throw new RuntimeException("Variável não declarada: " + name);
        }
        vm.writePush(segment, index);
    }

    private void writePopVariable(String name) {
        if (vm == null) return;

        SymbolTable.Kind kind = symbolTable.kindOf(name);
        int index = symbolTable.indexOf(name);
        String segment;

        switch (kind) {
            case STATIC: segment = "static"; break;
            case FIELD: segment = "this"; break;
            case ARG: segment = "argument"; break;
            case VAR: segment = "local"; break;
            default:
                throw new RuntimeException("Variável não declarada: " + name);
        }
        vm.writePop(segment, index);
    }

    private void writeArithmetic(String command) {
        if (vm != null) vm.writeArithmetic(command);
    }

    private String newLabel(String prefix) {
        return prefix + labelCounter++;
    }

    public void compileClass() {
        xml.openTag("class");
        consume(TokenType.KEYWORD, "class");
        className = tokenizer.peek().lexeme;
        consume(TokenType.IDENTIFIER, "className");
        consume(TokenType.SYMBOL, "{");

        while (match(TokenType.KEYWORD, "static") || match(TokenType.KEYWORD, "field")) {
            compileClassVarDec();
        }

        while (match(TokenType.KEYWORD, "constructor")
                || match(TokenType.KEYWORD, "function")
                || match(TokenType.KEYWORD, "method")) {
            compileSubroutine();
        }

        consume(TokenType.SYMBOL, "}");
        xml.closeTag("class");
    }

    public void compileClassVarDec() {
        xml.openTag("classVarDec");
        String kindText = tokenizer.peek().lexeme;
        SymbolTable.Kind kind = kindText.equals("static")
            ? SymbolTable.Kind.STATIC : SymbolTable.Kind.FIELD;
        consume(TokenType.KEYWORD, "keyword");

        String type = tokenizer.peek().lexeme;
        if (match(TokenType.KEYWORD, "int") || match(TokenType.KEYWORD, "char")
                || match(TokenType.KEYWORD, "boolean")) {
            consume(TokenType.KEYWORD, "type");
        } else {
            consume(TokenType.IDENTIFIER, "type (className)");
        }

        String name = tokenizer.peek().lexeme;
        consume(TokenType.IDENTIFIER, "varName");
        symbolTable.define(name, type, kind);

        while (match(TokenType.SYMBOL, ",")) {
            consume(TokenType.SYMBOL, ",");
            name = tokenizer.peek().lexeme;
            consume(TokenType.IDENTIFIER, "varName");
            symbolTable.define(name, type, kind);
        }

        consume(TokenType.SYMBOL, ";");
        xml.closeTag("classVarDec");
    }

    public void compileSubroutine() {
        xml.openTag("subroutineDec");
        currentSubroutineKind = tokenizer.peek().lexeme;
        symbolTable.startSubroutine();

        consume(TokenType.KEYWORD, "keyword");

        if (currentSubroutineKind.equals("method")) {
            symbolTable.define("this", className, SymbolTable.Kind.ARG);
        }

        if (match(TokenType.KEYWORD, "void") || match(TokenType.KEYWORD, "int")
                || match(TokenType.KEYWORD, "char") || match(TokenType.KEYWORD, "boolean")) {
            consume(TokenType.KEYWORD, "returnType");
        } else {
            consume(TokenType.IDENTIFIER, "returnType (className)");
        }

        String subroutineName = tokenizer.peek().lexeme;
        currentSubroutineName = className + "." + subroutineName;
        consume(TokenType.IDENTIFIER, "subroutineName");

        consume(TokenType.SYMBOL, "(");
        compileParameterList();
        consume(TokenType.SYMBOL, ")");
        compileSubroutineBody();

        xml.closeTag("subroutineDec");
    }

    public void compileSubroutineBody() {
        xml.openTag("subroutineBody");
        consume(TokenType.SYMBOL, "{");

        while (match(TokenType.KEYWORD, "var")) compileVarDec();

        if (vm != null) {
            vm.writeFunction(currentSubroutineName, symbolTable.varCount(SymbolTable.Kind.VAR));

            if (currentSubroutineKind.equals("constructor")) {
                vm.writePush("constant", symbolTable.varCount(SymbolTable.Kind.FIELD));
                vm.writeCall("Memory.alloc", 1);
                vm.writePop("pointer", 0);
            } else if (currentSubroutineKind.equals("method")) {
                vm.writePush("argument", 0);
                vm.writePop("pointer", 0);
            }
        }

        compileStatements();
        consume(TokenType.SYMBOL, "}");
        xml.closeTag("subroutineBody");
    }

    public void compileParameterList() {
        xml.openTag("parameterList");
        while (!match(TokenType.SYMBOL, ")")) {
            String type = tokenizer.peek().lexeme;
            if (match(TokenType.KEYWORD, "int") || match(TokenType.KEYWORD, "char")
                    || match(TokenType.KEYWORD, "boolean")) {
                consume(TokenType.KEYWORD, "type");
            } else {
                consume(TokenType.IDENTIFIER, "type (className)");
            }

            String name = tokenizer.peek().lexeme;
            consume(TokenType.IDENTIFIER, "varName");
            symbolTable.define(name, type, SymbolTable.Kind.ARG);

            if (match(TokenType.SYMBOL, ",")) consume(TokenType.SYMBOL, ",");
            else break;
        }
        xml.closeTag("parameterList");
    }

    public void compileVarDec() {
        xml.openTag("varDec");
        consume(TokenType.KEYWORD, "var");

        String type = tokenizer.peek().lexeme;
        if (match(TokenType.KEYWORD, "int") || match(TokenType.KEYWORD, "char")
                || match(TokenType.KEYWORD, "boolean")) {
            consume(TokenType.KEYWORD, "type");
        } else {
            consume(TokenType.IDENTIFIER, "type (className)");
        }

        String name = tokenizer.peek().lexeme;
        consume(TokenType.IDENTIFIER, "varName");
        symbolTable.define(name, type, SymbolTable.Kind.VAR);

        while (match(TokenType.SYMBOL, ",")) {
            consume(TokenType.SYMBOL, ",");
            name = tokenizer.peek().lexeme;
            consume(TokenType.IDENTIFIER, "varName");
            symbolTable.define(name, type, SymbolTable.Kind.VAR);
        }

        consume(TokenType.SYMBOL, ";");
        xml.closeTag("varDec");
    }

    public void compileStatements() {
        xml.openTag("statements");
        while (true) {
            if (match(TokenType.KEYWORD, "let")) compileLet();
            else if (match(TokenType.KEYWORD, "if")) compileIf();
            else if (match(TokenType.KEYWORD, "while")) compileWhile();
            else if (match(TokenType.KEYWORD, "do")) compileDo();
            else if (match(TokenType.KEYWORD, "return")) compileReturn();
            else break;
        }
        xml.closeTag("statements");
    }

    public void compileLet() {
        xml.openTag("letStatement");
        consume(TokenType.KEYWORD, "let");

        String name = tokenizer.peek().lexeme;
        consume(TokenType.IDENTIFIER, "varName");

        boolean isArray = match(TokenType.SYMBOL, "[");
        if (isArray) {
            writePushVariable(name);
            consume(TokenType.SYMBOL, "[");
            compileExpression();
            consume(TokenType.SYMBOL, "]");
            writeArithmetic("add");
        }

        consume(TokenType.SYMBOL, "=");
        compileExpression();
        consume(TokenType.SYMBOL, ";");

        if (isArray) {
            if (vm != null) {
                vm.writePop("temp", 0);
                vm.writePop("pointer", 1);
                vm.writePush("temp", 0);
                vm.writePop("that", 0);
            }
        } else {
            writePopVariable(name);
        }

        xml.closeTag("letStatement");
    }

    public void compileIf() {
        xml.openTag("ifStatement");
        String trueLabel = newLabel("IF_TRUE");
        String falseLabel = newLabel("IF_FALSE");
        String endLabel = newLabel("IF_END");

        consume(TokenType.KEYWORD, "if");
        consume(TokenType.SYMBOL, "(");
        compileExpression();
        consume(TokenType.SYMBOL, ")");

        if (vm != null) {
            vm.writeIf(trueLabel);
            vm.writeGoto(falseLabel);
            vm.writeLabel(trueLabel);
        }

        consume(TokenType.SYMBOL, "{");
        compileStatements();
        consume(TokenType.SYMBOL, "}");

        if (match(TokenType.KEYWORD, "else")) {
            if (vm != null) {
                vm.writeGoto(endLabel);
                vm.writeLabel(falseLabel);
            }
            consume(TokenType.KEYWORD, "else");
            consume(TokenType.SYMBOL, "{");
            compileStatements();
            consume(TokenType.SYMBOL, "}");
            if (vm != null) {
                vm.writeLabel(endLabel);
            }
        } else if (vm != null) {
            vm.writeLabel(falseLabel);
        }

        xml.closeTag("ifStatement");
    }

    public void compileWhile() {
        xml.openTag("whileStatement");
        String startLabel = newLabel("WHILE_EXP");
        String endLabel = newLabel("WHILE_END");

        if (vm != null) vm.writeLabel(startLabel);

        consume(TokenType.KEYWORD, "while");
        consume(TokenType.SYMBOL, "(");
        compileExpression();
        consume(TokenType.SYMBOL, ")");

        if (vm != null) {
            vm.writeArithmetic("not");
            vm.writeIf(endLabel);
        }

        consume(TokenType.SYMBOL, "{");
        compileStatements();
        consume(TokenType.SYMBOL, "}");

        if (vm != null) {
            vm.writeGoto(startLabel);
            vm.writeLabel(endLabel);
        }

        xml.closeTag("whileStatement");
    }

    public void compileDo() {
        xml.openTag("doStatement");
        consume(TokenType.KEYWORD, "do");
        compileSubroutineCall();
        consume(TokenType.SYMBOL, ";");
        if (vm != null) vm.writePop("temp", 0);
        xml.closeTag("doStatement");
    }

    public void compileReturn() {
        xml.openTag("returnStatement");
        consume(TokenType.KEYWORD, "return");

        if (match(TokenType.SYMBOL, ";")) {
            if (vm != null) vm.writePush("constant", 0);
        } else {
            compileExpression();
        }

        consume(TokenType.SYMBOL, ";");
        if (vm != null) vm.writeReturn();
        xml.closeTag("returnStatement");
    }

    public void compileExpression() {
        xml.openTag("expression");
        compileTerm();

        while (match(TokenType.SYMBOL, "+") || match(TokenType.SYMBOL, "-")
                || match(TokenType.SYMBOL, "*") || match(TokenType.SYMBOL, "/")
                || match(TokenType.SYMBOL, "&") || match(TokenType.SYMBOL, "|")
                || match(TokenType.SYMBOL, "<") || match(TokenType.SYMBOL, ">")
                || match(TokenType.SYMBOL, "=")) {
            String operator = tokenizer.peek().lexeme;
            consume(TokenType.SYMBOL, "op");
            compileTerm();

            switch (operator) {
                case "+": writeArithmetic("add"); break;
                case "-": writeArithmetic("sub"); break;
                case "*": if (vm != null) vm.writeCall("Math.multiply", 2); break;
                case "/": if (vm != null) vm.writeCall("Math.divide", 2); break;
                case "&": writeArithmetic("and"); break;
                case "|": writeArithmetic("or"); break;
                case "<": writeArithmetic("lt"); break;
                case ">": writeArithmetic("gt"); break;
                case "=": writeArithmetic("eq"); break;
            }
        }
        xml.closeTag("expression");
    }

    public void compileTerm() {
        xml.openTag("term");

        if (match(TokenType.INT_CONST)) {
            int value = Integer.parseInt(tokenizer.peek().lexeme);
            consume(TokenType.INT_CONST, "integerConstant");
            if (vm != null) vm.writePush("constant", value);

        } else if (match(TokenType.STRING_CONST)) {
            String value = tokenizer.peek().lexeme;
            consume(TokenType.STRING_CONST, "stringConstant");
            if (vm != null) {
                vm.writePush("constant", value.length());
                vm.writeCall("String.new", 1);
                for (int i = 0; i < value.length(); i++) {
                    vm.writePush("constant", (int) value.charAt(i));
                    vm.writeCall("String.appendChar", 2);
                }
            }

        } else if (match(TokenType.KEYWORD, "true") || match(TokenType.KEYWORD, "false")
                || match(TokenType.KEYWORD, "null") || match(TokenType.KEYWORD, "this")) {
            String keyword = tokenizer.peek().lexeme;
            consume(TokenType.KEYWORD, "keywordConstant");
            if (vm != null) {
                if (keyword.equals("true")) {
                    vm.writePush("constant", 0);
                    vm.writeArithmetic("not");
                } else if (keyword.equals("this")) {
                    vm.writePush("pointer", 0);
                } else {
                    vm.writePush("constant", 0);
                }
            }

        } else if (match(TokenType.SYMBOL, "(")) {
            consume(TokenType.SYMBOL, "(");
            compileExpression();
            consume(TokenType.SYMBOL, ")");

        } else if (match(TokenType.SYMBOL, "-") || match(TokenType.SYMBOL, "~")) {
            String operator = tokenizer.peek().lexeme;
            consume(TokenType.SYMBOL, "unaryOp");
            compileTerm();
            writeArithmetic(operator.equals("-") ? "neg" : "not");

        } else if (match(TokenType.IDENTIFIER)) {
            String name = tokenizer.peek().lexeme;
            consume(TokenType.IDENTIFIER, "identifier");

            if (match(TokenType.SYMBOL, "[")) {
                writePushVariable(name);
                consume(TokenType.SYMBOL, "[");
                compileExpression();
                consume(TokenType.SYMBOL, "]");
                if (vm != null) {
                    vm.writeArithmetic("add");
                    vm.writePop("pointer", 1);
                    vm.writePush("that", 0);
                }

            } else if (match(TokenType.SYMBOL, ".") || match(TokenType.SYMBOL, "(")) {
                compileSubroutineCallAfterName(name);

            } else {
                writePushVariable(name);
            }

        } else {
            throw new RuntimeException(
                "Termo inesperado: " + (tokenizer.peek() == null ? "EOF" : tokenizer.peek().lexeme)
            );
        }

        xml.closeTag("term");
    }

    public void compileSubroutineCall() {
        String name = tokenizer.peek().lexeme;
        consume(TokenType.IDENTIFIER, "subroutineName");
        compileSubroutineCallAfterName(name);
    }

    private void compileSubroutineCallAfterName(String name) {
        String callName;
        int nArgs = 0;

        if (match(TokenType.SYMBOL, ".")) {
            consume(TokenType.SYMBOL, ".");
            String subroutineName = tokenizer.peek().lexeme;
            consume(TokenType.IDENTIFIER, "subroutineName");

            if (symbolTable.kindOf(name) != SymbolTable.Kind.NONE) {
                writePushVariable(name);
                nArgs++;
                callName = symbolTable.typeOf(name) + "." + subroutineName;
            } else {
                callName = name + "." + subroutineName;
            }
        } else {
            if (vm != null) vm.writePush("pointer", 0);
            nArgs++;
            callName = className + "." + name;
        }

        consume(TokenType.SYMBOL, "(");
        nArgs += compileExpressionListAndCount();
        consume(TokenType.SYMBOL, ")");

        if (vm != null) vm.writeCall(callName, nArgs);
    }

    public void compileExpressionList() {
        compileExpressionListAndCount();
    }

    private int compileExpressionListAndCount() {
        xml.openTag("expressionList");
        int count = 0;

        if (!match(TokenType.SYMBOL, ")")) {
            compileExpression();
            count++;
            while (match(TokenType.SYMBOL, ",")) {
                consume(TokenType.SYMBOL, ",");
                compileExpression();
                count++;
            }
        }

        xml.closeTag("expressionList");
        return count;
    }
}
