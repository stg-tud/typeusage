package de.tud.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.identifiers.MethodIdentifier;
import de.tu_darmstadt.stg.mubench.cli.identifiers.SourceCodeMethodIdentifier;
import de.tud.stg.analysis.ObjectTrace;

public class DMMCMethodIdentifier implements MethodIdentifier {
    private final int line;
    private final SourceCodeMethodIdentifier identifier;

    DMMCMethodIdentifier(ObjectTrace trace) {
        String location = trace.getLocation();
        String context = trace.getContext();

        String[] splitLocation = location.split(":");
        String fullyQualifiedType = splitLocation[1];
        String methodSignature = context.substring(context.indexOf(":") + 1);

        this.line = Integer.parseInt(splitLocation[2]);
        this.identifier = new SourceCodeMethodIdentifier(fullyQualifiedType + "." + methodSignature);
    }

    @Override
    public String getSignature() {
        return identifier.getSignature();
    }

    @Override
    public String getSourceFilePath() {
        return identifier.getSourceFilePath();
    }

    public String getDeclaringTypeName() { return identifier.getDeclaringTypeName(); }

    public int getLine() {
        return line;
    }
}
