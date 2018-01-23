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
            if (getSourceFileName(record).contains("org/jfree/chart/util/ShapeUtilities.java") && getMethodName(record).contains("equal(GeneralPath, GeneralPath)")) {
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
        String file = getSourceFileName(target);
        String method = getMethodName(target);
        DetectorFinding finding = new DetectorFinding(file, method);
        finding.put("type", getType(target));
        finding.put("firstcallline", getFirstCallLine(target));
        finding.put("presentcalls", getPresentCalls(target));
        finding.put("missingcalls", getMissingCalls(target));
        finding.put("strangeness", Double.toString(target.strangeness()));
        return finding;
    }

    private static String getType(ObjectTrace target) {
        return target.getType().split(":")[1];
    }

    private static String getFirstCallLine(ObjectTrace target) {
        String[] locationInfo = target.getLocation().split(":");
        if (locationInfo.length > 2) {
            return locationInfo[2];
        } else {
            return "unknown";
        }
    }

    private static Set<String> getPresentCalls(ObjectTrace target) {
        return target.calls.stream().map(call -> call.split(":")[1]).collect(Collectors.toSet());
    }

    private static List<String> getMissingCalls(ObjectTrace target) {
        return target.missingcalls.keySet().stream()
                .map(missingcall -> missingcall.split(":")[1])
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private static String getSourceFileName(ObjectTrace target) {
        return new Location(target.getLocation()).toSourceFileName();
    }

    public static String getMethodName(ObjectTrace target) {
        String methodName = target.getContext().split(":")[1].replace(",", ", ");
        if (methodName.startsWith("<init>")) {
            String typeName = new Location(target.getLocation()).toFullyQualifiedName().toString();
            typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
            typeName = typeName.substring(typeName.lastIndexOf("$") + 1);
            methodName = typeName + methodName.substring("<init>".length());
        }
        return methodName;
    }
}
