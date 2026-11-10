
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    public enum Kind {
        STATIC,
        FIELD,
        ARG,
        VAR,
        NONE
    }

    private static class Symbol {
        String type;
        Kind kind;
        int index;

        Symbol(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    // Escopo da classe
    private final Map<String, Symbol> classScope;

    // Escopo da sub-rotina
    private final Map<String, Symbol> subroutineScope;

    // Contadores de cada tipo
    private final Map<Kind, Integer> counts;

    public SymbolTable() {
        classScope = new HashMap<>();
        subroutineScope = new HashMap<>();
        counts = new HashMap<>();

        for (Kind kind : Kind.values()) {
            counts.put(kind, 0);
        }
    }

    // Inicia uma nova sub-rotina
    public void startSubroutine() {
        subroutineScope.clear();

        counts.put(Kind.ARG, 0);
        counts.put(Kind.VAR, 0);
    }

    // Registra uma nova variável
    public void define(String name, String type, Kind kind) {
        if (kind == Kind.NONE) {
            throw new IllegalArgumentException(
                "Kind NONE não pode ser utilizado em uma declaração."
            );
        }

        Map<String, Symbol> scope = isClassKind(kind)
            ? classScope
            : subroutineScope;

        if (scope.containsKey(name)) {
            throw new IllegalArgumentException(
                "Variável já declarada neste escopo: " + name
            );
        }

        int index = counts.get(kind);

        scope.put(name, new Symbol(type, kind, index));

        counts.put(kind, index + 1);
    }

    // Retorna a quantidade de variáveis de determinado tipo
    public int varCount(Kind kind) {
        return counts.get(kind);
    }

    // Retorna o tipo da variável
    public String typeOf(String name) {
        Symbol symbol = find(name);
        return symbol == null ? null : symbol.type;
    }

    // Retorna o tipo de declaração da variável
    public Kind kindOf(String name) {
        Symbol symbol = find(name);
        return symbol == null ? Kind.NONE : symbol.kind;
    }

    // Retorna o índice da variável
    public int indexOf(String name) {
        Symbol symbol = find(name);
        return symbol == null ? -1 : symbol.index;
    }

    // Procura primeiro no escopo da sub-rotina e depois no da classe
    private Symbol find(String name) {
        Symbol symbol = subroutineScope.get(name);

        if (symbol != null) {
            return symbol;
        }

        return classScope.get(name);
    }

    private boolean isClassKind(Kind kind) {
        return kind == Kind.STATIC || kind == Kind.FIELD;
    }
}
