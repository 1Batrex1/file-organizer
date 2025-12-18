package bartek.fileorganizer.model;

import java.time.LocalDate;

public record Rule(
        String extension,
        String targetFolder,
        String nameContains
) {

    public Boolean matches(String fileName) {
        boolean matchesExtension = extension == null || extension.isEmpty() || fileName.endsWith(extension);
        boolean matchesNameContains = nameContains == null || nameContains.isEmpty() || fileName.contains(nameContains);
        return matchesExtension && matchesNameContains;
    }
}
