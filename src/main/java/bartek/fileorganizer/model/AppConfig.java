package bartek.fileorganizer.model;

import java.util.List;

public record AppConfig(
        String sourceDirectory,
        List<Rule> rules
) {}
