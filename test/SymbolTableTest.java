
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SymbolTableTest {

    @Test
    public void testDefineAndLookup() {
        SymbolTable table = new SymbolTable();

        table.define("idade", "int", SymbolTable.Kind.FIELD);

        assertEquals("int", table.typeOf("idade"));
        assertEquals(SymbolTable.Kind.FIELD, table.kindOf("idade"));
        assertEquals(0, table.indexOf("idade"));
    }

    @Test
    public void testIndexes() {
        SymbolTable table = new SymbolTable();

        table.define("x", "int", SymbolTable.Kind.FIELD);
        table.define("y", "int", SymbolTable.Kind.FIELD);
        table.define("contador", "int", SymbolTable.Kind.STATIC);

        assertEquals(0, table.indexOf("x"));
        assertEquals(1, table.indexOf("y"));
        assertEquals(0, table.indexOf("contador"));

        assertEquals(2, table.varCount(SymbolTable.Kind.FIELD));
        assertEquals(1, table.varCount(SymbolTable.Kind.STATIC));
    }

    @Test
    public void testStartSubroutine() {
        SymbolTable table = new SymbolTable();

        table.define("x", "int", SymbolTable.Kind.FIELD);
        table.define("arg1", "int", SymbolTable.Kind.ARG);
        table.define("local1", "int", SymbolTable.Kind.VAR);

        table.startSubroutine();

        assertEquals(1, table.varCount(SymbolTable.Kind.FIELD));
        assertEquals(0, table.varCount(SymbolTable.Kind.ARG));
        assertEquals(0, table.varCount(SymbolTable.Kind.VAR));

        assertEquals(-1, table.indexOf("arg1"));
        assertEquals(-1, table.indexOf("local1"));
        assertEquals(0, table.indexOf("x"));
    }

    @Test
    public void testSubroutineScopeOverridesClassScope() {
        SymbolTable table = new SymbolTable();

        table.define("x", "int", SymbolTable.Kind.FIELD);
        table.define("x", "boolean", SymbolTable.Kind.VAR);

        assertEquals("boolean", table.typeOf("x"));
        assertEquals(SymbolTable.Kind.VAR, table.kindOf("x"));
    }
}
