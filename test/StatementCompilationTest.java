import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StatementCompilationTest {

    @Test
    public void testStatementsArraysCallsAndStrings() throws Exception {
        JackTokenizer tokenizer = new JackTokenizer("examples/Statements.jack");
        XmlWriter xml = new XmlWriter();
        VMWriter vm = new VMWriter();

        CompilationEngine compiler = new CompilationEngine(tokenizer, xml, vm);
        compiler.compileClass();

        String output = vm.getOutput();

        assertTrue(output.contains("function Statements.test 2\n"));
        assertTrue(output.contains("label WHILE_EXP"));
        assertTrue(output.contains("if-goto WHILE_END"));
        assertTrue(output.contains("label IF_TRUE"));
        assertTrue(output.contains("label IF_FALSE"));
        assertTrue(output.contains("call Output.printInt 1\n"));
        assertTrue(output.contains("push constant 0\nreturn\n"));

        // Leitura de array: calcula o endereço e lê através de THAT.
        assertTrue(output.contains(
            "push argument 0\n" +
            "push argument 1\n" +
            "add\n" +
            "pop pointer 1\n" +
            "push that 0\n" +
            "return\n"
        ));

        // Escrita em array: guarda o valor temporariamente e grava em THAT 0.
        assertTrue(output.contains(
            "push argument 0\n" +
            "push argument 1\n" +
            "add\n" +
            "push argument 2\n" +
            "pop temp 0\n" +
            "pop pointer 1\n" +
            "push temp 0\n" +
            "pop that 0\n"
        ));

        // String "ok": cria a string e acrescenta os dois caracteres.
        assertTrue(output.contains(
            "push constant 2\n" +
            "call String.new 1\n" +
            "push constant 111\n" +
            "call String.appendChar 2\n" +
            "push constant 107\n" +
            "call String.appendChar 2\n" +
            "return\n"
        ));
    }
}
