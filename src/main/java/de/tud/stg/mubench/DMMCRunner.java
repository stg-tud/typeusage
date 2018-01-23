package de.tud.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.*;
import typeusage.miner.FileTypeUsageCollector;
import typeusage.miner.TypeUsage;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class DMMCRunner {

    public static void main(String[] args) throws Exception {
        new MuBenchRunner()
                .withDetectOnlyStrategy(new DetectionStrategy() {
                    @Override
                    public DetectorOutput detectViolations(DetectorArgs detectorArgs, DetectorOutput.Builder output) throws Exception {
                        final String misuseClassPath = detectorArgs.getTargetClassPaths()[0];
                        final String trainingSrcPath = detectorArgs.getPatternSrcPath();
                        final String trainingClassPath = detectorArgs.getPatternClassPath();

                        final int patternFrequency = 50;
                        final double minStrangeness = 0.01;
                        final int maxNumberOfMissingCalls = Integer.MAX_VALUE;

                        String modelFilename = Files.createTempFile("model", ".dat").toString();
                        FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename) {
                            @Override
                            protected String[] buildSootArgs() {
                                return generateRunArgs(misuseClassPath, trainingClassPath);
                            }

                            @Override
                            public void receive(TypeUsage t) {
                                // duplicate evidence for pattern to ensure it gets mined
                                int numberOfCopies = isFromPattern(trainingSrcPath, t.getLocation()) ? patternFrequency : 1;
                                for (int i = 0; i < numberOfCopies; i++) {
                                    super.receive(t);
                                }
                            }
                        };

                        List<DetectorFinding> findings = new DMMC(minStrangeness, maxNumberOfMissingCalls)
                                .run(modelFilename, collector, usage -> !isFromPattern(trainingSrcPath, usage.getLocation()), false);

                        return output
                                .withRunInfo("min strangeness", minStrangeness)
                                .withRunInfo("max number of missing calls", maxNumberOfMissingCalls)
                                .withRunInfo("pattern evidence", patternFrequency)
                                .withFindings(findings);
                    }

                    private String[] generateRunArgs(String misuseClasspath, String patternClasspath) {
                        return new String[]{"-soot-classpath", misuseClasspath + ":" + patternClasspath,
                                "-pp", /* prepend is not required */
                                "-process-dir", misuseClasspath, "-process-dir", patternClasspath};
                    }

                    private boolean isFromPattern(String patternsSrcPath, String location) {
                        String fileName = new Location(location).toSourceFileName();
                        return new File(patternsSrcPath, fileName).exists();
                    }
                })
                .withMineAndDetectStrategy((detectorArgs, output) -> {
                    // using values from the paper
                    final double minStrangeness = 0.5;
                    final int maxNumberOfMissingCalls = 1;

                    String modelFilename = Files.createTempFile("output", ".dat").toString();
                    FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename);
                    collector.setDirToProcess(String.join(":", detectorArgs.getTargetClassPaths()));

                    List<DetectorFinding> findings = new DMMC(minStrangeness, maxNumberOfMissingCalls)
                            .run(modelFilename, collector, usage -> true, true);

                    return output
                            .withRunInfo("min strangeness", minStrangeness)
                            .withRunInfo("max number of missing calls", maxNumberOfMissingCalls)
                            .withFindings(findings);
                })
                .run(args);
    }
}
