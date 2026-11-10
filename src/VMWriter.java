
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VMWriter {

    private final StringBuilder output;

    public VMWriter() {
        this.output = new StringBuilder();
    }

    public void writePush(String segment, int index) {
        validateSegment(segment);
        output.append("push ")
              .append(segment)
              .append(" ")
              .append(index)
              .append("\n");
    }

    public void writePop(String segment, int index) {
        validateSegment(segment);

        if (segment.equals("constant")) {
            throw new IllegalArgumentException(
                "Não é permitido usar pop constant."
            );
        }

        output.append("pop ")
              .append(segment)
              .append(" ")
              .append(index)
              .append("\n");
    }

    public void writeArithmetic(String command) {
        String[] commands = {
            "add", "sub", "neg", "eq",
            "gt", "lt", "and", "or", "not"
        };

        boolean valid = false;

        for (String c : commands) {
            if (c.equals(command)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            throw new IllegalArgumentException(
                "Comando aritmético inválido: " + command
            );
        }

        output.append(command).append("\n");
    }

    public void writeLabel(String label) {
        output.append("label ").append(label).append("\n");
    }

    public void writeGoto(String label) {
        output.append("goto ").append(label).append("\n");
    }

    public void writeIf(String label) {
        output.append("if-goto ").append(label).append("\n");
    }

    public void writeCall(String name, int nArgs) {
        output.append("call ")
              .append(name)
              .append(" ")
              .append(nArgs)
              .append("\n");
    }

    public void writeFunction(String name, int nLocals) {
        output.append("function ")
              .append(name)
              .append(" ")
              .append(nLocals)
              .append("\n");
    }

    public void writeReturn() {
        output.append("return\n");
    }

    public String getOutput() {
        return output.toString();
    }

    public void saveToFile(String filePath) throws IOException {
        Path path = Path.of(filePath);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Files.writeString(
            path,
            output.toString(),
            StandardCharsets.UTF_8
        );
    }

    private void validateSegment(String segment) {
        String[] segments = {
            "constant", "argument", "local",
            "static", "this", "that",
            "pointer", "temp"
        };

        for (String s : segments) {
            if (s.equals(segment)) {
                return;
            }
        }

        throw new IllegalArgumentException(
            "Segmento VM inválido: " + segment
        );
    }
}