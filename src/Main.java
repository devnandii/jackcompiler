import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                "Uso: java -cp bin Main <arquivo.jack | pasta> "
                + "[--xml saida.xml] [--lexer saida.xml]"
            );
            System.exit(1);
        }

        try {
            Path input = Paths.get(args[0]);
            String xmlPath = null;
            String lexerPath = null;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("--xml") && i + 1 < args.length) {
                    xmlPath = args[++i];
                } else if (args[i].equals("--lexer") && i + 1 < args.length) {
                    lexerPath = args[++i];
                } else {
                    throw new IllegalArgumentException(
                        "Argumento não reconhecido: " + args[i]
                    );
                }
            }

            if (lexerPath != null) {
                runLexer(input, lexerPath);
                return;
            }

            List<Path> files = collectJackFiles(input);

            if (xmlPath != null && files.size() > 1) {
                throw new IllegalArgumentException(
                    "--xml só pode ser usado com um único arquivo .jack."
                );
            }

            for (Path jack : files) {
                try {
                    compileFile(jack, xmlPath);
                } catch (RuntimeException e) {
                    throw new RuntimeException(
                        jack.getFileName() + ": " + e.getMessage(), e
                    );
                }
            }

        } catch (IOException | RuntimeException e) {
            System.err.println("Erro: " + e.getMessage());
            System.exit(1);
        }
    }

    // Arquivo -> lista com 1 arquivo; pasta -> todos os .jack dela (sem subpastas)
    private static List<Path> collectJackFiles(Path input) throws IOException {
        List<Path> files = new ArrayList<>();

        if (Files.isDirectory(input)) {
            try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(input, "*.jack")) {
                for (Path p : stream) {
                    if (Files.isRegularFile(p)) {
                        files.add(p);
                    }
                }
            }
            Collections.sort(files);

            if (files.isEmpty()) {
                throw new IllegalArgumentException(
                    "Nenhum arquivo .jack encontrado em: " + input
                );
            }
        } else if (Files.isRegularFile(input)) {
            if (!input.getFileName().toString().endsWith(".jack")) {
                throw new IllegalArgumentException(
                    "A entrada precisa ser um arquivo .jack ou uma pasta: " + input
                );
            }
            files.add(input);
        } else {
            throw new IllegalArgumentException("Caminho não encontrado: " + input);
        }

        return files;
    }

    // Square.jack -> Square.vm, na mesma pasta do .jack
    private static void compileFile(Path jack, String xmlPath) throws IOException {
        JackTokenizer tokenizer = new JackTokenizer(jack.toString());
        XmlWriter xml = new XmlWriter();
        VMWriter vm = new VMWriter();

        // um CompilationEngine (e uma SymbolTable) novo para cada classe
        CompilationEngine engine = new CompilationEngine(tokenizer, xml, vm);
        engine.compileClass();

        String name = jack.getFileName().toString();
        String base = name.substring(0, name.length() - ".jack".length());
        Path vmPath = jack.resolveSibling(base + ".vm");

        vm.saveToFile(vmPath.toString());

        if (xmlPath != null) {
            xml.saveToFile(xmlPath);
        }

        System.out.println(jack + " -> " + vmPath);
    }

    private static void runLexer(Path input, String outputPath) throws IOException {
        JackTokenizer tokenizer = new JackTokenizer(input.toString());
        XmlWriter xml = new XmlWriter();

        xml.openTag("tokens");
        while (tokenizer.peek() != null) {
            xml.writeToken(tokenizer.peek());
            tokenizer.advance();
        }
        xml.closeTag("tokens");

        xml.saveToFile(outputPath);
        System.out.println("XML de tokens gerado: " + outputPath);
    }
}