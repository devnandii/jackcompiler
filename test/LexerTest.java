import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/** Compara o XML de tokens gerado pelo scanner com os arquivos *T.xml oficiais do Nand2Tetris. */
public class LexerTest {

    private String normalizeXml(String content) {
        return content.lines()
                      .map(String::strip)
                      .collect(Collectors.joining("\n"));
    }

    private void assertTokens(String name) throws Exception {
        String sourcePath = name.equals("Square")
    ? "examples/SquareProgram/Square.jack"
    : "examples/" + name + ".jack";

    JackTokenizer tokenizer = new JackTokenizer(sourcePath);
        XmlWriter xml = new XmlWriter();
        xml.openTag("tokens");
        while (tokenizer.peek() != null) {
            xml.writeToken(tokenizer.peek());
            tokenizer.advance();
        }
        xml.closeTag("tokens");

        String generatedPath = "output/" + name + "T.xml";
        xml.saveToFile(generatedPath);

        String generated = normalizeXml(Files.readString(Paths.get(generatedPath)));
        String expected = normalizeXml(Files.readString(Paths.get("test/expected_xml/" + name + "T.xml")));
        assertEquals(expected, generated, "Tokens de " + name + ".jack diferem do oficial.");
    }

    @Test
    public void testMainTokens() throws Exception { assertTokens("Main"); }

    @Test
    public void testSquareTokens() throws Exception { assertTokens("Square"); }

    @Test
    public void testSquareGameTokens() throws Exception { assertTokens("SquareGame"); }
}
