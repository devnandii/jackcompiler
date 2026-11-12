import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ParserTest {

    private String normalizeXml(String content) {
        return content.lines()
                      .map(String::stripLeading)
                      .collect(Collectors.joining("\n"));
    }

    @Test
    public void testMainJack() throws Exception {
        JackTokenizer tokenizer = new JackTokenizer("examples/Main.jack");
        XmlWriter xml = new XmlWriter();
        CompilationEngine parser = new CompilationEngine(tokenizer, xml);
        parser.compileClass();
        
        String generatedPath = "output/Main.xml"; 
        xml.saveToFile(generatedPath);

        String generated = normalizeXml(Files.readString(Paths.get(generatedPath)));
        String expected = normalizeXml(Files.readString(Paths.get("test/expected_xml/Main.xml")));

        assertEquals(expected, generated, "A saída do Main.jack difere do oficial.");
    }

    @Test
public void testSquareJack() throws Exception {
    JackTokenizer tokenizer = new JackTokenizer("examples/SquareProgram/Square.jack");
    XmlWriter xml = new XmlWriter();
    CompilationEngine parser = new CompilationEngine(tokenizer, xml);
    parser.compileClass();

    String generatedPath = "output/Square.xml";
    xml.saveToFile(generatedPath);

    String generated = normalizeXml(Files.readString(Paths.get(generatedPath)));
    String expected = normalizeXml(Files.readString(Paths.get("test/expected_xml/Square.xml")));

    assertEquals(expected, generated, "A saída do Square.jack difere do oficial.");
    }

    @Test
    public void testSquareGameJack() throws Exception {
        JackTokenizer tokenizer = new JackTokenizer("examples/SquareProgram/SquareGame.jack");
        XmlWriter xml = new XmlWriter();
        CompilationEngine parser = new CompilationEngine(tokenizer, xml);
        parser.compileClass();
        
        String generatedPath = "output/SquareGame.xml";
        xml.saveToFile(generatedPath);

        String generated = normalizeXml(Files.readString(Paths.get(generatedPath)));
        String expected = normalizeXml(Files.readString(Paths.get("test/expected_xml/SquareGame.xml")));

        assertEquals(expected, generated, "A saída do SquareGame.jack difere do oficial.");
    }
}