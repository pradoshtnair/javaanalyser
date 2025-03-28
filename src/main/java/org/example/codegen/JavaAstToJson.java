package org.example.codegen;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaAstToJson {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java JavaAstToJson <path-to-java-project> <output-json-file>");
            System.exit(1);
        }

        String projectPath = args[0];
        String outputFilePath = args[1];
        File projectDir = new File(projectPath);

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Provided path is invalid or not a directory.");
            System.exit(1);
        }

        JavaParser parser = new JavaParser();

        List<File> javaFiles = listJavaFiles(projectDir);
        Map<String, Object> astMap = new HashMap<>();

        javaFiles.forEach(file -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                ParseResult<CompilationUnit> parseResult = parser.parse(fis);
                parseResult.getResult().ifPresent(cu ->
                        astMap.put(projectDir.toPath().relativize(file.toPath()).toString(), astToMap(cu))
                );
            } catch (IOException e) {
                System.err.println("Failed parsing file: " + file.getAbsolutePath());
            }
        });

        try (FileWriter writer = new FileWriter(outputFilePath)) {
            gson.toJson(astMap, writer);
        } catch (IOException e) {
            System.err.println("Failed to write AST to file: " + outputFilePath);
        }
    }

    private static List<File> listJavaFiles(File directory) {
        try (Stream<Path> walk = Files.walk(directory.toPath())) {
            return walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error traversing project directory", e);
        }
    }

    private static Map<String, Object> astToMap(Node node) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", node.getClass().getSimpleName());
        result.put("content", node.toString().split("\n")[0].trim());

        if (!node.getChildNodes().isEmpty()) {
            List<Map<String, Object>> children = node.getChildNodes()
                    .stream()
                    .map(JavaAstToJson::astToMap)
                    .collect(Collectors.toList());
            result.put("children", children);
        }

        return result;
    }
}
