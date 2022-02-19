package org.mvnsearch.jbang;

import org.springframework.aot.build.GenerateBootstrapCommand;
import org.springframework.nativex.AotOptions;
import org.springframework.nativex.Mode;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JBangSpringNativeIntegration {

    public static Map<String, Object> postBuild(Path appClasses,
                                                Path pomFile,
                                                List<Map.Entry<String, String>> repositories,
                                                List<Map.Entry<String, Path>> originalDeps,
                                                List<String> comments,
                                                boolean nativeImage) throws IOException {
        String appClassesDir = appClasses.toAbsolutePath().toString();
        Path generatedSourcesDirectory = appClasses.resolve("spring-aot-generated-sources");
        // generate source code
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            List<String> fileDeps = originalDeps.stream().map(entry -> {
                String filePath = entry.getValue().toAbsolutePath().toString();
                return filePath.startsWith("file://") ? filePath.substring(7) : filePath;
            }).collect(Collectors.toList());
            String classpath = appClassesDir + File.pathSeparator + String.join(File.pathSeparator, fileDeps);
            if (!generatedSourcesDirectory.toFile().exists()) {
                generatedSourcesDirectory.toFile().mkdir();
            }
            Path sourcesPath = generatedSourcesDirectory.resolve(Paths.get("src", "main", "java"));
            List<String> generatorCmd = new ArrayList<>();
            generatorCmd.add("java");
            generatorCmd.add("-cp");
            generatorCmd.add(classpath);
            generatorCmd.add(GenerateBootstrapCommand.class.getCanonicalName());
            generatorCmd.add("--sources-out=" + sourcesPath.toAbsolutePath());
            generatorCmd.add("--resources-out=" + appClassesDir);
            generatorCmd.add("--resources=" + appClassesDir);
            generatorCmd.add("--classes=" + appClassesDir);
            final Map<String, String> aotConfig = extractAotConfig(comments);
            //apply aot options
            applyAotOptions(generatorCmd, getAotOptions(aotConfig));
            //set main-class
            if (aotConfig.containsKey("mainClass")) {
                generatorCmd.add("--main-class=" + aotConfig.get("mainClass"));
            }
            //set application-class
            if (aotConfig.containsKey("applicationClass")) {
                generatorCmd.add("--application-class" + aotConfig.get("applicationClass"));
            }

            new ProcessExecutor().command(generatorCmd)
                    .redirectOutput(bos)
                    .redirectError(bos)
                    .execute();
            // compile sourcecode
            String sourceFullPath = sourcesPath.toAbsolutePath().toString();
            try (Stream<Path> paths = Files.walk(sourcesPath)) {
                List<String> javaFiles = paths.filter(Files::isRegularFile)
                        .filter(filePath -> filePath.toString().endsWith(".java"))
                        .map(filePath -> filePath.toAbsolutePath().toString().substring(sourceFullPath.length() + 1))
                        .collect(Collectors.toList());
                Files.writeString(sourcesPath.resolve("sources.txt"), String.join(System.lineSeparator(), javaFiles));
            }
            List<String> compileCmd = new ArrayList<>();
            compileCmd.add("javac");
            compileCmd.add("@sources.txt");
            compileCmd.add("-nowarn");
            compileCmd.add("-cp");
            compileCmd.add(classpath);
            compileCmd.add("-d");
            compileCmd.add(appClassesDir);
            // compile spring aot source code
            new ProcessExecutor().directory(new File(sourceFullPath))
                    .command(compileCmd)
                    .redirectOutput(bos)
                    .redirectError(bos)
                    .execute();
        } catch (Exception ignore) {

        } finally {
            Files.walk(generatedSourcesDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        return Collections.emptyMap();
    }


    protected static AotOptions getAotOptions(Map<String, String> config) {
        AotOptions aotOptions = new AotOptions();
        aotOptions.setMode(Mode.NATIVE.toString());
        if (config.containsKey("removeXmlSupport")) {
            aotOptions.setRemoveXmlSupport(Boolean.parseBoolean(config.get("removeXmlSupport")));
        }
        if (config.containsKey("removeSpelSupport")) {
            aotOptions.setRemoveSpelSupport(Boolean.parseBoolean(config.get("removeSpelSupport")));
        }
        if (config.containsKey("removeYamlSupport")) {
            aotOptions.setRemoveYamlSupport(Boolean.parseBoolean(config.get("removeYamlSupport")));
        }
        if (config.containsKey("removeJmxSupport")) {
            aotOptions.setRemoveJmxSupport(Boolean.parseBoolean(config.get("removeJmxSupport")));
        }
        if (config.containsKey("removeXmlSupport")) {
            aotOptions.setRemoveXmlSupport(Boolean.parseBoolean(config.get("removeXmlSupport")));
        }
        return aotOptions;
    }

    protected static void applyAotOptions(List<String> args, AotOptions aotOptions) {
        args.add("--mode=" + aotOptions.toMode());
        if (aotOptions.isRemoveXmlSupport()) {
            args.add("--remove-xml");
        }
        if (aotOptions.isRemoveJmxSupport()) {
            args.add("--remove-jmx");
        }
        if (aotOptions.isRemoveSpelSupport()) {
            args.add("--remove-spel");
        }
        if (aotOptions.isRemoveYamlSupport()) {
            args.add("--remove-yaml");
        }
    }

    /**
     * extract AOT config, and format alike //AOT:CONFIG removeXmlSupport=true
     *
     * @param comments JBang comments
     * @return AOT configuration
     */
    public static Map<String, String> extractAotConfig(List<String> comments) {
        return comments.stream()
                .filter(comment -> comment.startsWith("//AOT:CONFIG "))
                .map(comment -> comment.substring(comment.indexOf(" ") + 1).trim())
                .flatMap(pairs -> Arrays.stream(pairs.split("\\s")).map(s -> s.split("=", 2)))
                .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
    }

}
