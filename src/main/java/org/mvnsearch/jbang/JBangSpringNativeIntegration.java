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
            List<String> args = new ArrayList<>();
            args.add("java");
            args.add("-cp");
            args.add(classpath);
            args.add(GenerateBootstrapCommand.class.getCanonicalName());
            args.add("--sources-out=" + sourcesPath.toAbsolutePath());
            args.add("--resources-out=" + appClassesDir);
            args.add("--resources=" + appClassesDir);
            args.add("--classes=" + appClassesDir);
            applyAotOptions(args);
            // generate source code
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new ProcessExecutor().command(args)
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
            List<String> compileArgs = new ArrayList<>();
            compileArgs.add("javac");
            compileArgs.add("-target");
            compileArgs.add("11");
            compileArgs.add("@sources.txt");
            compileArgs.add("-nowarn");
            compileArgs.add("-cp");
            compileArgs.add(classpath);
            compileArgs.add("-d");
            compileArgs.add(appClassesDir);
            // compile spring aot source code
            new ProcessExecutor().directory(new File(sourceFullPath))
                    .command(compileArgs)
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
        return new HashMap<>();
    }


    protected static AotOptions getAotOptions() {
        AotOptions aotOptions = new AotOptions();
        aotOptions.setMode(Mode.NATIVE.toString());
        aotOptions.setDebugVerify(false);
        aotOptions.setVerify(false);
        aotOptions.setRemoveYamlSupport(true);
        aotOptions.setRemoveJmxSupport(true);
        aotOptions.setRemoveXmlSupport(true);
        aotOptions.setRemoveSpelSupport(true);
        return aotOptions;
    }

    protected static void applyAotOptions(List<String> args) {
        AotOptions aotOptions = getAotOptions();
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

}
