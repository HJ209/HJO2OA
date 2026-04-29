package com.hjo2oa.infra.dictionary.application;

import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SystemEnumDictionaryService {

    public static final String SYSTEM_ENUM_CATEGORY = "system-enum";

    private static final String BASE_PACKAGE = "com.hjo2oa";
    private static final String CODE_PREFIX = "system.enum.";

    private final DictionaryTypeApplicationService dictionaryTypeApplicationService;
    private final Clock clock;

    @Autowired
    public SystemEnumDictionaryService(DictionaryTypeApplicationService dictionaryTypeApplicationService) {
        this(dictionaryTypeApplicationService, Clock.systemUTC());
    }

    SystemEnumDictionaryService(
            DictionaryTypeApplicationService dictionaryTypeApplicationService,
            Clock clock
    ) {
        this.dictionaryTypeApplicationService = Objects.requireNonNull(
                dictionaryTypeApplicationService,
                "dictionaryTypeApplicationService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public List<SystemEnumDictionaryView> previewSystemEnums() {
        return discoverSystemEnums();
    }

    public SystemEnumImportResult importSystemEnums() {
        List<SystemEnumDictionaryView> discoveredEnums = discoverSystemEnums();
        int createdTypes = 0;
        int createdItems = 0;
        List<String> importedCodes = new ArrayList<>();

        for (SystemEnumDictionaryView discoveredEnum : discoveredEnums) {
            DictionaryTypeView dictionaryType = dictionaryTypeApplicationService
                    .queryByCode(null, discoveredEnum.code())
                    .orElse(null);
            if (dictionaryType == null) {
                dictionaryType = dictionaryTypeApplicationService.createType(
                        discoveredEnum.code(),
                        discoveredEnum.name(),
                        SYSTEM_ENUM_CATEGORY,
                        false,
                        true,
                        null
                );
                createdTypes++;
            }

            Set<String> existingItemCodes = dictionaryType.items().stream()
                    .map(item -> item.itemCode())
                    .collect(java.util.stream.Collectors.toSet());
            for (SystemEnumItemView enumItem : discoveredEnum.items()) {
                if (existingItemCodes.contains(enumItem.code())) {
                    continue;
                }
                dictionaryType = dictionaryTypeApplicationService.addItem(
                        dictionaryType.id(),
                        enumItem.code(),
                        enumItem.name(),
                        null,
                        enumItem.sortOrder()
                );
                createdItems++;
            }
            importedCodes.add(discoveredEnum.code());
        }

        return new SystemEnumImportResult(
                discoveredEnums.size(),
                createdTypes,
                createdItems,
                importedCodes
        );
    }

    private List<SystemEnumDictionaryView> discoverSystemEnums() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new Hjo2oaClassFilter());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .map(candidate -> candidate.getBeanClassName())
                .filter(Objects::nonNull)
                .map(className -> loadEnumClass(classLoader, className))
                .filter(Objects::nonNull)
                .map(this::toDictionaryView)
                .sorted(Comparator.comparing(SystemEnumDictionaryView::code))
                .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Class<? extends Enum<?>> loadEnumClass(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            if (!clazz.isEnum() || !Modifier.isPublic(clazz.getModifiers())) {
                return null;
            }
            return (Class<? extends Enum<?>>) clazz;
        } catch (LinkageError | ClassNotFoundException ex) {
            return null;
        }
    }

    private SystemEnumDictionaryView toDictionaryView(Class<? extends Enum<?>> enumClass) {
        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        List<SystemEnumItemView> items = new ArrayList<>();
        for (int index = 0; index < enumConstants.length; index++) {
            Enum<?> enumConstant = enumConstants[index];
            items.add(new SystemEnumItemView(enumConstant.name(), toDisplayName(enumConstant.name()), index));
        }
        return new SystemEnumDictionaryView(
                toDictionaryCode(enumClass),
                enumClass.getSimpleName(),
                enumClass.getName(),
                SYSTEM_ENUM_CATEGORY,
                items
        );
    }

    private String toDictionaryCode(Class<? extends Enum<?>> enumClass) {
        String readableName = toSnakeCase(enumClass.getSimpleName());
        String hash = shortHash(enumClass.getName());
        String code = CODE_PREFIX + readableName + "." + hash;
        return code.length() <= 64 ? code : CODE_PREFIX + hash;
    }

    private String toDisplayName(String enumName) {
        String normalized = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = normalized.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.length() == 0 ? enumName : builder.toString();
    }

    private String toSnakeCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isUpperCase(current) && index > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }

    private String shortHash(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String hash = Long.toHexString(crc32.getValue());
        return hash.length() <= 8 ? hash : hash.substring(0, 8);
    }

    public record SystemEnumDictionaryView(
            String code,
            String name,
            String className,
            String category,
            List<SystemEnumItemView> items
    ) {
    }

    public record SystemEnumItemView(
            String code,
            String name,
            int sortOrder
    ) {
    }

    public record SystemEnumImportResult(
            int discoveredTypes,
            int createdTypes,
            int createdItems,
            List<String> importedCodes
    ) {
    }

    private static final class Hjo2oaClassFilter implements TypeFilter {

        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
            return metadataReader.getClassMetadata().getClassName().startsWith(BASE_PACKAGE + ".");
        }
    }
}
