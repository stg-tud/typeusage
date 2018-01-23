package de.tud.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.FullyQualifiedName;

class Location {
    private final String location;

    Location(String location) {
        this.location = location;
    }

    String toSourceFileName() {
        return toFullyQualifiedName().toSourceFileName();
    }

    FullyQualifiedName toFullyQualifiedName() {
        return new FullyQualifiedName(location.split(":")[location.startsWith("location:") ? 1 : 0]);
    }
}
