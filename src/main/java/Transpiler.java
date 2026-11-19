import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Le pipeline complet :
 *   texte SARL -> Lexer -> tokens -> Parser -> arbre -> Semantic -> PythonGenerator -> texte Python
 *
 * Ligne de commande :  Transpiler <fichier.sarl> [dossier-de-sortie]   (défaut : ./generated)
 */
public final class Transpiler {

    /** python vaut null s'il y a au moins une erreur. */
    public record Result(String python, List<Diagnostics.Entry> errors) {
        public boolean success() {
            return python != null;
        }

        public boolean has(Diagnostics.Code code) {
            return errors.stream().anyMatch(e -> e.code() == code);
        }
    }

    private Transpiler() {
    }

    public static Result transpile(String source) {
        Diagnostics diag = new Diagnostics();

        List<Token> tokens = new Lexer(source, diag).tokenize();
        SyntaxTree.ProgramNode program = new Parser(tokens, diag).parse();
        if (!diag.hasErrors()) {
            new Semantic(diag).analyze(program);
        }
        if (diag.hasErrors()) {
            return new Result(null, diag.all());
        }
        String python = new PythonGenerator(diag).generate(program);
        return new Result(diag.hasErrors() ? null : python, diag.all());
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage : Transpiler <fichier.sarl> [dossier-de-sortie]");
            System.exit(2);
        }
        Path input = Path.of(args[0]);
        Path outputDir = args.length > 1 ? Path.of(args[1]) : Path.of("generated");

        Result result = transpile(Files.readString(input));
        result.errors().forEach(e -> System.err.println(e.format()));
        if (!result.success()) {
            System.err.println("Échec de la transpilation.");
            System.exit(1);
        }

        Files.createDirectories(outputDir);
        String baseName = input.getFileName().toString().replaceFirst("\\.sarl$", "");
        Path output = outputDir.resolve(baseName + ".py");
        Files.writeString(output, result.python());
        System.out.println("Fichier généré : " + output);
    }
}
