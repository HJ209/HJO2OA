package com.hjo2oa.infra.dictionary.application;

import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
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
        return discoverSystemEnums().stream()
                .map(this::withDiff)
                .toList();
    }

    public SystemEnumImportResult importSystemEnums() {
        List<SystemEnumDictionaryView> discoveredEnums = discoverSystemEnums();
        int createdTypes = 0;
        int createdItems = 0;
        int updatedItems = 0;
        int disabledItems = 0;
        List<String> importedCodes = new ArrayList<>();

        for (SystemEnumDictionaryView discoveredEnum : discoveredEnums) {
            DictionaryTypeView dictionaryType = dictionaryTypeApplicationService
                    .queryExactByCode(null, discoveredEnum.code())
                    .orElse(null);
            if (dictionaryType == null) {
                dictionaryType = dictionaryTypeApplicationService.createSystemType(
                        discoveredEnum.code(),
                        discoveredEnum.name(),
                        SYSTEM_ENUM_CATEGORY,
                        0,
                        null
                );
                createdTypes++;
            } else if (!dictionaryType.systemManaged()) {
                throw new BizException(
                        SharedErrorDescriptors.CONFLICT,
                        "Dictionary code conflicts with a non-system dictionary: " + discoveredEnum.code()
                );
            }

            SystemEnumDictionaryView diff = withDiff(discoveredEnum, dictionaryType);
            for (SystemEnumItemView enumItem : discoveredEnum.items()) {
                boolean existed = dictionaryType.items().stream()
                        .anyMatch(item -> item.itemCode().equals(enumItem.code()));
                dictionaryType = dictionaryTypeApplicationService.upsertSystemItem(
                        dictionaryType.id(),
                        enumItem.code(),
                        enumItem.name(),
                        enumItem.sortOrder(),
                        true
                );
                if (existed && diff.changedItemCodes().contains(enumItem.code())) {
                    updatedItems++;
                } else if (!existed) {
                    createdItems++;
                }
            }
            dictionaryType = dictionaryTypeApplicationService.disableSystemItemsExcept(
                    dictionaryType.id(),
                    discoveredEnum.items().stream()
                            .map(SystemEnumItemView::code)
                            .collect(java.util.stream.Collectors.toSet())
            );
            disabledItems += diff.disabledItemCodes().size();
            importedCodes.add(discoveredEnum.code());
        }

        return new SystemEnumImportResult(
                discoveredEnums.size(),
                createdTypes,
                createdItems,
                updatedItems,
                disabledItems,
                importedCodes
        );
    }

    private SystemEnumDictionaryView withDiff(SystemEnumDictionaryView discoveredEnum) {
        return withDiff(
                discoveredEnum,
                dictionaryTypeApplicationService.queryExactByCode(null, discoveredEnum.code()).orElse(null)
        );
    }

    private SystemEnumDictionaryView withDiff(
            SystemEnumDictionaryView discoveredEnum,
            DictionaryTypeView dictionaryType
    ) {
        if (dictionaryType == null) {
            return discoveredEnum.withDiff(false, discoveredEnum.items().stream()
                    .map(SystemEnumItemView::code)
                    .toList(), List.of(), List.of());
        }
        Set<String> discoveredCodes = discoveredEnum.items().stream()
                .map(SystemEnumItemView::code)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> existingCodes = dictionaryType.items().stream()
                .map(item -> item.itemCode())
                .collect(java.util.stream.Collectors.toSet());
        List<String> newItemCodes = discoveredCodes.stream()
                .filter(code -> !existingCodes.contains(code))
                .sorted()
                .toList();
        List<String> changedItemCodes = discoveredEnum.items().stream()
                .filter(item -> dictionaryType.items().stream().anyMatch(existing ->
                        existing.itemCode().equals(item.code())
                                && (!existing.displayName().equals(item.name())
                                || existing.sortOrder() != item.sortOrder()
                                || !existing.enabled())))
                .map(SystemEnumItemView::code)
                .toList();
        List<String> disabledItemCodes = dictionaryType.items().stream()
                .filter(item -> !discoveredCodes.contains(item.itemCode()))
                .filter(item -> item.enabled())
                .map(item -> item.itemCode())
                .sorted()
                .toList();
        return discoveredEnum.withDiff(true, newItemCodes, changedItemCodes, disabledItemCodes);
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
            boolean imported,
            List<String> newItemCodes,
            List<String> changedItemCodes,
            List<String> disabledItemCodes,
            List<SystemEnumItemView> items
    ) {
        public SystemEnumDictionaryView(
                String code,
                String name,
                String className,
                String category,
                List<SystemEnumItemView> items
        ) {
            this(code, name, className, category, false, List.of(), List.of(), List.of(), items);
        }

        public SystemEnumDictionaryView withDiff(
                boolean imported,
                List<String> newItemCodes,
                List<String> changedItemCodes,
                List<String> disabledItemCodes
        ) {
            return new SystemEnumDictionaryView(
                    code,
                    name,
                    className,
                    category,
                    imported,
                    List.copyOf(newItemCodes),
                    List.copyOf(changedItemCodes),
                    List.copyOf(disabledItemCodes),
                    items
            );
        }
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
            int updatedItems,
            int disabledItems,
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
