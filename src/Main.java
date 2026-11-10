import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                "Uso: java -cp bin Main <arquivo.jack> " +
                "[saida.xml] [--lexer] [--vm saida.vm]"
            );
            System.exit(1);
        }

        try {
            Path input = Paths.get(args[0]);

            String xmlOutputPath = "output/saida.xml";
            String vmOutputPath = null;

            boolean runLexerOnly = false;
            boolean xmlOutputSpecified = false;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("--lexer")) {
                    runLexerOnly = true;
                } else if (args[i].equals("--vm")) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException(
                            "Informe o caminho do arquivo .vm após --vm."
                        );
                    }

                    vmOutputPath = args[++i];
                } else if (!xmlOutputSpecified) {
                    xmlOutputPath = args[i];
                    xmlOutputSpecified = true;
                } else {
                    throw new IllegalArgumentException(
                        "Argumento não reconhecido: " + args[i]
                    );
                }
            }

            if (runLexerOnly && vmOutputPath != null) {
                throw new IllegalArgumentException(
                    "As opções --lexer e --vm não podem ser usadas juntas."
                );
            }

            if (runLexerOnly) {
                JackTokenizer tokenizer =
                    new JackTokenizer(input.toString());

                XmlWriter xml = new XmlWriter();

                xml.openTag("tokens");

                while (tokenizer.peek() != null) {
                    xml.writeToken(tokenizer.peek());
                    tokenizer.advance();
                }

                xml.closeTag("tokens");
                xml.saveToFile(xmlOutputPath);

                System.out.println(
                    "XML de Tokens (Lexer) gerado com sucesso: "
                    + xmlOutputPath
                );

            } else {
                JackTokenizer tokenizer =
                    new JackTokenizer(input.toString());

                XmlWriter xml = new XmlWriter();

                VMWriter vm = vmOutputPath == null
                    ? null
                    : new VMWriter();

                CompilationEngine parser =
                    new CompilationEngine(tokenizer, xml, vm);

                parser.compileClass();

                xml.saveToFile(xmlOutputPath);

                System.out.println(
                    "XML do Parser gerado com sucesso: "
                    + xmlOutputPath
                );

                if (vm != null) {
                    vm.saveToFile(vmOutputPath);

                    System.out.println(
                        "Arquivo VM gerado: " + vmOutputPath
                    );
                }
            }

        } catch (IOException | RuntimeException e) {
            System.err.println("Erro: " + e.getMessage());
            System.exit(1);
        }
    }
}
