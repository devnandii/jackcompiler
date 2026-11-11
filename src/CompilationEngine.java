public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final XmlWriter xml;

    private final SymbolTable symbolTable;
    private final VMWriter vm;

    private String className;
    private String currentSubroutineName;
    private String currentSubroutineKind;

    public CompilationEngine(JackTokenizer tokenizer, XmlWriter xml) {  
    this(tokenizer, xml, null);
    }

    public CompilationEngine(
        JackTokenizer tokenizer,
        XmlWriter xml,
        VMWriter vm
    ) {
        this.tokenizer = tokenizer;
        this.xml = xml;
        this.vm = vm;
        this.symbolTable = new SymbolTable();
    }

    // ==========================================================
    // Métodos Auxiliares
    // ==========================================================

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
            throw new RuntimeException("Esperado " + expected + " mas encontrado " + actual + " na linha " + line);
        }
        xml.writeToken(tokenizer.peek());
        tokenizer.advance();
    }

    private void writePushVariable(String name) {
    if (vm == null) {
        return;
    }

    SymbolTable.Kind kind = symbolTable.kindOf(name);
    int index = symbolTable.indexOf(name);

    String segment;

    switch (kind) {
        case STATIC:
            segment = "static";
            break;
        case FIELD:
            segment = "this";
            break;
        case ARG:
            segment = "argument";
            break;
        case VAR:
            segment = "local";
            break;
        default:
            throw new RuntimeException(
                "Variável não declarada: " + name
            );
    }

    vm.writePush(segment, index);
    }

    private void writeArithmetic(String command) {
        if (vm != null) {
            vm.writeArithmetic(command);
        }
    }

    // ==========================================================
    // Regras da Gramática Jack
    // ==========================================================

     
    public void compileClass() {
        xml.openTag("class");

        consume(TokenType.KEYWORD, "class");

        // Guarda o nome da classe.
        className = tokenizer.peek().lexeme;
        consume(TokenType.IDENTIFIER, "className");

        consume(TokenType.SYMBOL, "{");

        // Declarações de variáveis da classe.
        while (match(TokenType.KEYWORD, "static")
                || match(TokenType.KEYWORD, "field")) {
            compileClassVarDec();
        }

        // Declarações de funções, métodos e construtores.
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
        ? SymbolTable.Kind.STATIC
        : SymbolTable.Kind.FIELD;

    consume(TokenType.KEYWORD, "keyword");

    String type = tokenizer.peek().lexeme;

    if (match(TokenType.KEYWORD, "int")
            || match(TokenType.KEYWORD, "char")
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


        

    public void compileVarDec() {
        xml.openTag("varDec");

        consume(TokenType.KEYWORD, "var");

        String type = tokenizer.peek().lexeme;

        if (match(TokenType.KEYWORD, "int")
                || match(TokenType.KEYWORD, "char")
                || match(TokenType.KEYWORD, "boolean")) {
            consume(TokenType.KEYWORD, "type");
        } else {
            consume(TokenType.IDENTIFIER, "type (className)");
        }

        String name = tokenizer.peek().lexeme;
        consume(TokenType.IDENTIFIER, "varName");

        symbolTable.define(
            name,
            type,
            SymbolTable.Kind.VAR
        );

        while (match(TokenType.SYMBOL, ",")) {
            consume(TokenType.SYMBOL, ",");

            name = tokenizer.peek().lexeme;
            consume(TokenType.IDENTIFIER, "varName");

            symbolTable.define(
                name,
                type,
                SymbolTable.Kind.VAR
            );
        }

        consume(TokenType.SYMBOL, ";");
        xml.closeTag("varDec");
    }


    public void compileSubroutine() {
    xml.openTag("subroutineDec");

    currentSubroutineKind = tokenizer.peek().lexeme;

    symbolTable.startSubroutine();

    consume(TokenType.KEYWORD, "keyword");

    if (currentSubroutineKind.equals("method")) {
        symbolTable.define(
            "this",
            className,
            SymbolTable.Kind.ARG
        );
    }

    if (match(TokenType.KEYWORD, "void")
            || match(TokenType.KEYWORD, "int")
            || match(TokenType.KEYWORD, "char")
            || match(TokenType.KEYWORD, "boolean")) {
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

        while (match(TokenType.KEYWORD, "var")) {
            compileVarDec();
        }

        // Gera o cabeçalho da função.
        if (vm != null) {
            vm.writeFunction(
                currentSubroutineName,
                symbolTable.varCount(SymbolTable.Kind.VAR)
            );

            // Construtor: aloca memória para os atributos da classe.
            if (currentSubroutineKind.equals("constructor")) {
                vm.writePush(
                    "constant",
                    symbolTable.varCount(SymbolTable.Kind.FIELD)
                );

                vm.writeCall("Memory.alloc", 1);
                vm.writePop("pointer", 0);
            }

            // Método: configura o ponteiro this.
            else if (currentSubroutineKind.equals("method")) {
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

        if (!match(TokenType.SYMBOL, ")")) {
            boolean moreParameters = true;

            while (moreParameters) {
                String type = tokenizer.peek().lexeme;

                if (match(TokenType.KEYWORD, "int")
                        || match(TokenType.KEYWORD, "char")
                        || match(TokenType.KEYWORD, "boolean")) {
                    consume(TokenType.KEYWORD, "type");
                } else {
                    consume(TokenType.IDENTIFIER, "type (className)");
                }

                String name = tokenizer.peek().lexeme;
                consume(TokenType.IDENTIFIER, "varName");

                symbolTable.define(
                    name,
                    type,
                    SymbolTable.Kind.ARG
                );

                if (match(TokenType.SYMBOL, ",")) {
                    consume(TokenType.SYMBOL, ",");
                } else {
                    moreParameters = false;
                }
            }
        }

        xml.closeTag("parameterList");
    }
        
        

    public void compileStatements() {
        xml.openTag("statements");

        boolean hasStatement = true;
        while (hasStatement) {
            if (match(TokenType.KEYWORD, "let")) {
                compileLet();
            } else if (match(TokenType.KEYWORD, "if")) {
                compileIf();
            } else if (match(TokenType.KEYWORD, "while")) {
                compileWhile();
            } else if (match(TokenType.KEYWORD, "do")) {
                compileDo();
            } else if (match(TokenType.KEYWORD, "return")) {
                compileReturn();
            } else {
                hasStatement = false;
            }
        }

        xml.closeTag("statements");
    }

    public void compileLet() {
        xml.openTag("letStatement");
        consume(TokenType.KEYWORD, "let");
        consume(TokenType.IDENTIFIER, "varName");
        
        if (match(TokenType.SYMBOL, "[")) {
            consume(TokenType.SYMBOL, "[");
            compileExpression();
            consume(TokenType.SYMBOL, "]");
        }
        
        consume(TokenType.SYMBOL, "=");
        compileExpression();
        consume(TokenType.SYMBOL, ";");
        xml.closeTag("letStatement");
    }

    public void compileIf() {
        xml.openTag("ifStatement");
        consume(TokenType.KEYWORD, "if");
        consume(TokenType.SYMBOL, "(");
        compileExpression();
        consume(TokenType.SYMBOL, ")");
        consume(TokenType.SYMBOL, "{");
        compileStatements();
        consume(TokenType.SYMBOL, "}");
        
        if (match(TokenType.KEYWORD, "else")) {
            consume(TokenType.KEYWORD, "else");
            consume(TokenType.SYMBOL, "{");
            compileStatements();
            consume(TokenType.SYMBOL, "}");
        }
        xml.closeTag("ifStatement");
    }

    public void compileWhile() {
        xml.openTag("whileStatement");
        consume(TokenType.KEYWORD, "while");
        consume(TokenType.SYMBOL, "(");
        compileExpression();
        consume(TokenType.SYMBOL, ")");
        consume(TokenType.SYMBOL, "{");
        compileStatements();
        consume(TokenType.SYMBOL, "}");
        xml.closeTag("whileStatement");
    }

    public void compileDo() {
        xml.openTag("doStatement");
        consume(TokenType.KEYWORD, "do");
        compileSubroutineCall();
        consume(TokenType.SYMBOL, ";");
        xml.closeTag("doStatement");
    }

    public void compileReturn() {
        xml.openTag("returnStatement");
        consume(TokenType.KEYWORD, "return");
        
        if (!match(TokenType.SYMBOL, ";")) {
            compileExpression();
        }
        consume(TokenType.SYMBOL, ";");
        xml.closeTag("returnStatement");
    }

    
    public void compileExpression() {
        xml.openTag("expression");

        compileTerm();

        while (
            match(TokenType.SYMBOL, "+")
            || match(TokenType.SYMBOL, "-")
            || match(TokenType.SYMBOL, "*")
            || match(TokenType.SYMBOL, "/")
            || match(TokenType.SYMBOL, "&")
            || match(TokenType.SYMBOL, "|")
            || match(TokenType.SYMBOL, "<")
            || match(TokenType.SYMBOL, ">")
            || match(TokenType.SYMBOL, "=")
        ) {
            String operator = tokenizer.peek().lexeme;

            consume(TokenType.SYMBOL, "op");

            compileTerm();

            switch (operator) {
                case "+":
                    writeArithmetic("add");
                    break;

                case "-":
                    writeArithmetic("sub");
                    break;

                case "*":
                    if (vm != null) {
                        vm.writeCall("Math.multiply", 2);
                    }
                    break;

                case "/":
                    if (vm != null) {
                        vm.writeCall("Math.divide", 2);
                    }
                    break;

                case "&":
                    writeArithmetic("and");
                    break;

                case "|":
                    writeArithmetic("or");
                    break;

                case "<":
                    writeArithmetic("lt");
                    break;

                case ">":
                    writeArithmetic("gt");
                    break;

                case "=":
                    writeArithmetic("eq");
                    break;
            }
        }

        xml.closeTag("expression");
    }


    
    public void compileTerm() {
        xml.openTag("term");

        // 1. Constante inteira
        if (match(TokenType.INT_CONST)) {
            int value = Integer.parseInt(tokenizer.peek().lexeme);

            consume(TokenType.INT_CONST, "integerConstant");

            if (vm != null) {
                vm.writePush("constant", value);
            }
        }

        // 2. Constante string
        else if (match(TokenType.STRING_CONST)) {
            consume(TokenType.STRING_CONST, "stringConstant");

            // A geração VM de strings será implementada posteriormente.
        }

        // 3. Constantes booleanas, null e this
        else if (
            match(TokenType.KEYWORD, "true")
            || match(TokenType.KEYWORD, "false")
            || match(TokenType.KEYWORD, "null")
            || match(TokenType.KEYWORD, "this")
        ) {
            String keyword = tokenizer.peek().lexeme;

            consume(TokenType.KEYWORD, "keywordConstant");

            if (vm != null) {
                switch (keyword) {
                    case "true":
                        vm.writePush("constant", 0);
                        vm.writeArithmetic("not");
                        break;

                    case "false":
                    case "null":
                        vm.writePush("constant", 0);
                        break;

                    case "this":
                        vm.writePush("pointer", 0);
                        break;
                }
            }
        }

        // 4. Expressão entre parênteses
        else if (match(TokenType.SYMBOL, "(")) {
            consume(TokenType.SYMBOL, "(");

            compileExpression();

            consume(TokenType.SYMBOL, ")");
        }

        // 5. Operações unárias: - e ~
        else if (
            match(TokenType.SYMBOL, "-")
            || match(TokenType.SYMBOL, "~")
        ) {
            String operator = tokenizer.peek().lexeme;

            consume(TokenType.SYMBOL, "unaryOp");

            compileTerm();

            if (operator.equals("-")) {
                writeArithmetic("neg");
            } else {
                writeArithmetic("not");
            }
        }

        // 6. Identificadores: variáveis, arrays e chamadas
        else if (match(TokenType.IDENTIFIER)) {
            String name = tokenizer.peek().lexeme;

            consume(TokenType.IDENTIFIER, "varName");

            // Acesso a array: nome[indice]
            if (match(TokenType.SYMBOL, "[")) {
                consume(TokenType.SYMBOL, "[");

                compileExpression();

                consume(TokenType.SYMBOL, "]");

                // Geração VM de arrays será implementada posteriormente.
            }

            // Chamada de método ou função: objeto.metodo(...)
            else if (match(TokenType.SYMBOL, ".")) {
                consume(TokenType.SYMBOL, ".");

                consume(TokenType.IDENTIFIER, "subroutineName");

                consume(TokenType.SYMBOL, "(");

                compileExpressionList();

                consume(TokenType.SYMBOL, ")");
            }

            // Chamada de função: funcao(...)
            else if (match(TokenType.SYMBOL, "(")) {
                consume(TokenType.SYMBOL, "(");

                compileExpressionList();

                consume(TokenType.SYMBOL, ")");
            }

            // Variável simples
            else {
                writePushVariable(name);
            }
        }

        else {
            throw new RuntimeException(
                "Termo inesperado: "
                + (tokenizer.peek() == null
                    ? "EOF"
                    : tokenizer.peek().lexeme)
            );
        }

        xml.closeTag("term");
    }


    public void compileSubroutineCall() {
        consume(TokenType.IDENTIFIER, "subroutineName");
        
        if (match(TokenType.SYMBOL, ".")) {
            consume(TokenType.SYMBOL, ".");
            consume(TokenType.IDENTIFIER, "subroutineName");
        }
        
        consume(TokenType.SYMBOL, "(");
        compileExpressionList();
        consume(TokenType.SYMBOL, ")");
    }

    public void compileExpressionList() {
        xml.openTag("expressionList");
        
        if (!match(TokenType.SYMBOL, ")")) {
            compileExpression();
            while (match(TokenType.SYMBOL, ",")) {
                consume(TokenType.SYMBOL, ",");
                compileExpression();
            }
        }
        
        xml.closeTag("expressionList");
    }
}