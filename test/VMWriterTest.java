import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VMWriterTest {

    @Test
    public void testPushAndPop() {
        VMWriter vm = new VMWriter();

        vm.writePush("constant", 5);
        vm.writePop("local", 2);

        assertEquals("push constant 5\npop local 2\n", vm.getOutput());
    }

    @Test
    public void testArithmetic() {
        VMWriter vm = new VMWriter();

        vm.writeArithmetic("add");
        vm.writeArithmetic("not");

        assertEquals("add\nnot\n", vm.getOutput());
    }

    @Test
    public void testCallFunctionAndReturn() {
        VMWriter vm = new VMWriter();

        vm.writeFunction("Main.main", 2);
        vm.writeCall("Math.abs", 1);
        vm.writeReturn();

        assertEquals(
            "function Main.main 2\ncall Math.abs 1\nreturn\n",
            vm.getOutput()
        );
    }

    @Test
    public void testLabelsAndJumps() {
        VMWriter vm = new VMWriter();

        vm.writeLabel("LOOP");
        vm.writeIf("END");
        vm.writeGoto("LOOP");

        assertEquals(
            "label LOOP\nif-goto END\ngoto LOOP\n",
            vm.getOutput()
        );
    }

    @Test
    public void testInvalidInputIsRejected() {
        VMWriter vm = new VMWriter();

        assertThrows(IllegalArgumentException.class,
            () -> vm.writePop("constant", 1));
        assertThrows(IllegalArgumentException.class,
            () -> vm.writePush("heap", 0));
        assertThrows(IllegalArgumentException.class,
            () -> vm.writeArithmetic("mul"));
    }
}