import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.operation.validator.validation.OperationValidator;
import org.openapi4j.operation.validator.util.PathNormalizer;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class OpenApiSpecRegistry {
    private final ConcurrentHashMap<String, OpenApi3> specCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OperationValidator> validatorCache = new ConcurrentHashMap<>();

    // Load spec from file or resource, key could be API-id or basePath
    public OpenApi3 loadSpec(String key, Path yamlPath) throws Exception {
        return specCache.computeIfAbsent(key, k -> {
            try {
                OpenApi3 api = new OpenApi3Parser().parse(yamlPath.toFile(), false);
                return api;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public OperationValidator getValidator(OpenApi3 openApi, String path, String method) {
        // key based on openApi identity + normalized path + method
        String vkey = openApi.hashCode() + "|" + path + "|" + method.toUpperCase();
        return validatorCache.computeIfAbsent(vkey, kk -> {
            return new OperationValidator(openApi);
        });
    }

    // Optionally support reload / watch file changes
}
