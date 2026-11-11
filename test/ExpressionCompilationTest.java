
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExpressionCompilationTest {

    @Test
    public void testArithmeticExpressions() throws Exception {
        JackTokenizer tokenizer =
            new JackTokenizer("examples/Expressions.jack");

        XmlWriter xml = new XmlWriter();
        VMWriter vm = new VMWriter();

        CompilationEngine compiler =
            new CompilationEngine(tokenizer, xml, vm);

        compiler.compileClass();

        String output = vm.getOutput();

        // Soma, subtração e multiplicação.
        assertTrue(output.contains(
            "push argument 0\n" +
            "push argument 1\n" +
            "add\n" +
            "push argument 0\n" +
            "push argument 1\n" +
            "sub\n" +
            "call Math.multiply 2\n"
        ));

        // Operação unária.
        assertTrue(output.contains(
            "function Expressions.negative 0\n" +
            "push argument 0\n" +
            "neg\n"
        ));

        // Comparação.
        assertTrue(output.contains(
            "function Expressions.compare 0\n" +
            "push argument 0\n" +
            "push argument 1\n" +
            "lt\n"
        ));

        // Constante inteira.
        assertTrue(output.contains(
            "function Expressions.constants 0\n" +
            "push constant 42\n"
        ));
    }
}
