
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompilationIntegrationTest {

    @Test
    public void testSquareConstructorAndMethods() throws Exception {
        JackTokenizer tokenizer =
            new JackTokenizer("examples/Square.jack");

        XmlWriter xml = new XmlWriter();
        VMWriter vm = new VMWriter();

        CompilationEngine compiler =
            new CompilationEngine(tokenizer, xml, vm);

        compiler.compileClass();

        String output = vm.getOutput();

        // Construtor: três atributos na classe Square.
        assertTrue(output.contains("function Square.new 0\n"));
        assertTrue(output.contains(
            "push constant 3\n" +
            "call Memory.alloc 1\n" +
            "pop pointer 0\n"
        ));

        // Métodos: inicialização do ponteiro this.
        assertTrue(output.contains(
            "function Square.dispose 0\n" +
            "push argument 0\n" +
            "pop pointer 0\n"
        ));

        assertTrue(output.contains(
            "function Square.draw 0\n" +
            "push argument 0\n" +
            "pop pointer 0\n"
        ));
    }

    @Test
    public void testLocalVariableCount() throws Exception {
        JackTokenizer tokenizer =
            new JackTokenizer("examples/Main.jack");

        XmlWriter xml = new XmlWriter();
        VMWriter vm = new VMWriter();

        CompilationEngine compiler =
            new CompilationEngine(tokenizer, xml, vm);

        compiler.compileClass();

        String output = vm.getOutput();

        assertTrue(output.contains("function Main.main 1\n"));
        assertTrue(output.contains("function Main.more 4\n"));
    }
}
