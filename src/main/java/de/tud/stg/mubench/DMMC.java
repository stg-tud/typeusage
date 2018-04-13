package de.tud.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.DetectorFinding;
import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.ObjectTrace;
import de.tud.stg.analysis.engine.EcoopEngine;
import typeusage.miner.FileTypeUsageCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DMMC {
    private final double minStrangeness;
    private final int maxNumberOfMissingCalls;

    public DMMC(double minStrangeness, int maxNumberOfMissingCalls) {
        this.minStrangeness = minStrangeness;
        this.maxNumberOfMissingCalls = maxNumberOfMissingCalls;
    }

    List<DetectorFinding> run(String modelFilename, FileTypeUsageCollector usageCollector,
                              Predicate<ObjectTrace> targetUsage, boolean filterMissingMethodsWithSmallSupport) throws Exception {
        try {
            usageCollector.run();
        } finally {
            usageCollector.close();
        }

        return detect(modelFilename, targetUsage, filterMissingMethodsWithSmallSupport);
    }

    private List<DetectorFinding> detect(String modelFilename,
                                                Predicate<ObjectTrace> targetUsage,
                                                boolean filterMissingMethodsWithSmallSupport)
            throws Exception {
        List<ObjectTrace> dataset = new DatasetReader().readObjects(modelFilename);
        EcoopEngine engine = new EcoopEngine(dataset);
        engine.option_filterIsEnabled = filterMissingMethodsWithSmallSupport;
        engine.dontConsiderContext();
        engine.setOption_k(maxNumberOfMissingCalls);

        int nanalyzed = 0;

        System.out.println("finding usages with a strangeness of more than " + minStrangeness + "...");
        List<ObjectTrace> violations = new ArrayList<>();
        for (ObjectTrace record : dataset) {
            DMMCMethodIdentifier methodIdentifier = new DMMCMethodIdentifier(record);
            if (methodIdentifier.getSourceFilePath().contains("org/jfree/chart/util/ShapeUtilities.java") &&
                    methodIdentifier.getSignature().contains("equal(GeneralPath, GeneralPath)")) {
                System.out.println("found it!");
            }
            System.out.print(nanalyzed + "/" + dataset.size());

            if (targetUsage.test(record)) {
                engine.query(record);
                double strangeness = record.strangeness();
                if (strangeness >= minStrangeness) {
                    System.out.print(" -> violation!");
                    violations.add(record);
                }
            }
            System.out.println();
            nanalyzed++;
        }

        violations.sort((f1, f2) -> Double.compare(f2.strangeness(), f1.strangeness()));

        List<DetectorFinding> findings = new ArrayList<>();
        for (ObjectTrace violation : violations) {
            findings.add(toFinding(violation));
        }
        return findings;
    }

    private static DetectorFinding toFinding(ObjectTrace target) throws IOException {
        DMMCMethodIdentifier methodIdentifier = new DMMCMethodIdentifier(target);
        String file = methodIdentifier.getSourceFilePath();
        String method = methodIdentifier.getSignature();
        DetectorFinding finding = new DetectorFinding(file, method);
        finding.put("type", methodIdentifier.getDeclaringTypeName());
        finding.put("firstcallline", methodIdentifier.getLine());
        finding.put("presentcalls", getPresentCalls(target));
        finding.put("missingcalls", getMissingCalls(target));
        finding.put("strangeness", Double.toString(target.strangeness()));
        return finding;
    }

    private static Set<String> getPresentCalls(ObjectTrace target) {
        return target.calls.stream().map(call -> call.split(":")[1]).collect(Collectors.toSet());
    }

    private static List<String> getMissingCalls(ObjectTrace target) {
        return target.missingcalls.keySet().stream()
                .map(missingcall -> missingcall.split(":")[1])
                .collect(Collectors.toCollection(LinkedList::new));
    }

}
