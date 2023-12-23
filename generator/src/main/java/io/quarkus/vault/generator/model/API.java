package io.quarkus.vault.generator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record API(
        Optional<String> prefix,
        Optional<String> category,
        Optional<String> name,
        Optional<String> packageName,
        Optional<String> relativePackageName,
        Optional<String> traceNameTag,
        Optional<String> basePath,
        Optional<Boolean> mountable,
        Optional<Boolean> namespaced,
        Optional<List<POJO>> types,
        Optional<List<Operation>> operations,
        Optional<List<POJO.Method>> methods) {

    public String getPrefix() {
        return prefix.orElse("Vault");
    }

    public String getCategory() {
        return category.orElse("");
    }

    public String getName() {
        return name.orElse("");
    }

    public String getPackageName() {
        return packageName.orElse("io.quarkus.vault.client");
    }

    public String getPackageName(String... suffixes) {
        if (suffixes == null || suffixes.length == 0) {
            return getPackageName();
        }
        return getPackageName() + "." + String.join(".", suffixes);
    }

    public String getCommonPackageName() {
        return getPackageName("common");
    }

    public String getCommonAPIPackageName() {
        return getPackageName("api", "common");
    }

    public String getLocalAPIPackageName() {
        return getPackageName("api", getRelativePackageName());
    }

    public String getAPIPackageName() {
        return getPackageName("api");
    }

    public String getRelativePackageName() {
        if (relativePackageName.isEmpty() || relativePackageName.get().isEmpty()) {
            var subPackages = new ArrayList<String>();
            if (!getCategory().isEmpty()) {
                subPackages.add(getCategory().toLowerCase());
            }
            if (name.isPresent() && !name.get().isEmpty()) {
                subPackages.add(name.get().toLowerCase());
            }
            return String.join(".", subPackages);
        }
        return relativePackageName.get();
    }

    public boolean isMountable() {
        return mountable.orElse(true);
    }

    public boolean isNamespaced() {
        return namespaced.orElse(true);
    }

    public List<POJO> getTypes() {
        return types.orElse(List.of());
    }

    public String getTraceNamePrefix() {
        var tag = traceNameTag.orElseGet(() -> getName().toLowerCase());
        return "[" + getCategory().toUpperCase() + " (" + tag + ")]";
    }

    public static final API EMPTY = new API(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

}
