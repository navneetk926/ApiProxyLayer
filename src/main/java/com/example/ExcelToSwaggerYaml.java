package org.example.swagger;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Operation;
import org.apache.poi.ss.usermodel.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelToSwaggerYaml {

    public static void main(String[] args) throws Exception {

        Workbook wb = WorkbookFactory.create(new File("C:\\Users\\ADMIN\\Downloads\\uae.xlsx"));

        OpenAPI openAPI = new OpenAPI();

        // ======== Info Sheet ========
        generateInfoTag(wb, openAPI);

        // ======== Servers & Security Sheet ========
        generateSecurityTag(wb, openAPI);

        // ======== APIs Sheet ========
        Map<String, PathItem> apiMap = readApis(wb);

        // ======== Request Responses Sheet ========
        readRequestResponses(wb, apiMap, openAPI);

        // ======== Set paths ========
        Paths paths = new Paths();
        apiMap.forEach(paths::addPathItem);
        openAPI.setPaths(paths);

        // ======== Write YAML ========
        ObjectMapper mapper = Yaml.mapper();
        mapper.writeValue(new File("C:\\Users\\ADMIN\\Downloads\\swagger.yaml"), openAPI);

        System.out.println("Swagger YAML generated successfully!");
    }

    private static void generateSecurityTag(Workbook wb, OpenAPI openAPI) {
        Sheet serverSheet = wb.getSheet("ServersSecurity");
        if (serverSheet != null) {

            Map<String, Integer> headerIndex = new HashMap<>();
            Row header = serverSheet.getRow(0);

            if (header != null) {
                for (Cell cell : header) {
                    headerIndex.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
                }
            }

            List<Server> servers = new ArrayList<>();
            List<SecurityRequirement> securityList = new ArrayList<>();

            for (int i = 1; i <= serverSheet.getLastRowNum(); i++) {
                Row r = serverSheet.getRow(i);
                if (r == null) continue;

                String url = headerIndex.containsKey("url")
                        ? getCellValue(r, headerIndex.get("url")) : "";

                if (url.isEmpty()) continue;

                String description = headerIndex.containsKey("description")
                        ? getCellValue(r, headerIndex.get("description")) : "";

                String securityName = headerIndex.containsKey("security")
                        ? getCellValue(r, headerIndex.get("security")) : "";

                Server server = new Server();
                server.setUrl(url);
                server.setDescription(description);
                servers.add(server);

                if (!securityName.isEmpty()) {
                    SecurityRequirement sec = new SecurityRequirement();
                    sec.addList(securityName.trim());
                    securityList.add(sec);
                }
            }

            if (!servers.isEmpty()) {
                openAPI.setServers(servers);
            }

            if (!securityList.isEmpty()) {
                openAPI.setSecurity(securityList);
            }
        }
    }

    private static void generateInfoTag(Workbook wb, OpenAPI openAPI) {
        Sheet infoSheet = wb.getSheet("Info");
        if (infoSheet != null) {
            Map<String, Integer> headerIndex = new HashMap<>();
            Row header = infoSheet.getRow(0);

            if (header != null) {
                for (Cell cell : header) {
                    headerIndex.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
                }
            }
            Row dataRow = infoSheet.getRow(1);
            if (dataRow != null) {

                Info info = new Info();

                if (headerIndex.containsKey("title")) {
                    info.setTitle(getCellValue(dataRow, headerIndex.get("title")));
                }

                if (headerIndex.containsKey("version")) {
                    info.setVersion(getCellValue(dataRow, headerIndex.get("version")));
                }

                if (headerIndex.containsKey("description")) {
                    info.setDescription(getCellValue(dataRow, headerIndex.get("description")));
                }

                openAPI.setInfo(info);
            }
        }
    }

    private static String getCellValue(Row row, int index) {
        if (row == null) return "";
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
    private static Boolean getBooleanCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return Boolean.FALSE;
        return cell.getBooleanCellValue();
    }
    private static Double getDoubleCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;


        return Double.valueOf(cell.getNumericCellValue());
    }
    // ===================== APIs Sheet =====================
    private static Map<String, PathItem> readApis(Workbook wb) {
        Sheet sheet = wb.getSheet("APIs");
        Map<String, PathItem> map = new LinkedHashMap<>();
        if (sheet == null) return map;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null || getCellValue(r, 0).isEmpty()) continue;

            String path = getCellValue(r, 0);
            String method = getCellValue(r, 1).toUpperCase();

            PathItem item = map.computeIfAbsent(path, k -> new PathItem());
            Operation op = new Operation();
            op.setSummary(getCellValue(r, 2));
            op.setDescription(getCellValue(r, 3));

            String tags = getCellValue(r, 4);
            if (!tags.isEmpty()) op.setTags(Arrays.asList(tags.split(",")));

            // Optional enum for API
            String enumVal = getCellValue(r, 5);
            if (!enumVal.isEmpty()) {
                Schema<Object> schema = new Schema<>();
                List<Object> enums = new ArrayList<>();
                for (String v : enumVal.replace("[","").replace("]","").split(",")) {
                    enums.add(v.trim());
                }
                schema.setEnum(enums);
            }

            switch (method) {
                case "POST": item.setPost(op); break;
                case "GET": item.setGet(op); break;
                case "PUT": item.setPut(op); break;
                case "PATCH": item.setPatch(op); break;
                case "HEAD": item.setHead(op); break;
                case "DELETE": item.setDelete(op); break;
            }
            map.put(path, item);
        }
        return map;
    }


    // ===================== Responses Sheet =====================
    private static ApiFieldDef getSwaggerObject(Row row){
        int index = 0;
        ApiFieldDef swaggerdto = new ApiFieldDef();
        swaggerdto.setPath(getCellValue(row,index++));
        swaggerdto.setMethod(getCellValue(row,index++));
        swaggerdto.setCode(getCellValue(row,index++));
        swaggerdto.setSchemaPath(getCellValue(row,index++));
        swaggerdto.setType(getCellValue(row,index++));
        swaggerdto.setTitle(getCellValue(row,index++));
        swaggerdto.setExample(getCellValue(row,index++));
        index++;
        swaggerdto.setDesc(getCellValue(row,index++));
        swaggerdto.setContentType(getCellValue(row,index++));
        swaggerdto.setHeaderOrBody(getCellValue(row,index++));
        swaggerdto.setRequestOrResponse(getCellValue(row,index++));
        swaggerdto.setMandatory(getBooleanCellValue(row,index++));
        swaggerdto.setMinValue(getDoubleCellValue(row,index++));
        swaggerdto.setMaxValue(getDoubleCellValue(row,index++));

        return swaggerdto;
    }
    private static List<ApiFieldDef> readExcel(Sheet sheet){
        List<ApiFieldDef> map = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            ApiFieldDef swaggerdto = getSwaggerObject(r);
            map.add(swaggerdto);
        }
        return map;
    }
    // ===================== Request Parameters Sheet =====================
    private static void readRequestParameters( List<ApiFieldDef> defs, Map<String, PathItem> map) {

        List<ApiFieldDef> filteredLanguages = defs.stream()
                .filter(item -> item.getHeaderOrBody().equalsIgnoreCase("Header"))
                .toList();
        filteredLanguages.forEach((item) -> {
            if(item.getRequestOrResponse().equalsIgnoreCase("Request")){
                Operation op = getOperation(map, item.getPath(), item.getMethod());
                Parameter p = new Parameter();
                p.setName(item.getTitle());
                p.setIn("Header");
                //p.setIn(getCellValue(r,3));
                p.setRequired(item.isMandatory);
                p.setDescription(item.getDesc());
                Schema<Object> schema = new Schema<>();
                schema.setType(item.getType());
                String enumVal = item.getEnumValue();
                if (enumVal != null && !enumVal.isEmpty()) {
                    List<Object> enums = new ArrayList<>();
                    for (String v : enumVal.replace("[","").replace("]","").split(",")) enums.add(v.trim());
                    schema.setEnum(enums);
                }

                String example = item.getExample();
                if (!example.isEmpty()) schema.setExample(example);

                p.setSchema(schema);
                if(op.getParameters() == null) {
                    op.setParameters(new ArrayList<>());
                }
                op.getParameters().add(p);
            }

        });
    }
    // ===================== Request  =====================

    public static void buildHierarchyRecursive(
            Schema<?> parent,
            String[] parts,
            int index,
            String leafType,
            String description,
            Object example,
            boolean isMandatory
    ) {
        if (index >= parts.length) {
            return;
        }

        if (parent.getProperties() == null) {
            parent.setProperties(new LinkedHashMap<>());
        }

        String currentPart = parts[index];
        boolean isArray = currentPart.endsWith("[]");
        String fieldName = isArray
                ? currentPart.substring(0, currentPart.length() - 2)
                : currentPart;

        if(isMandatory){
            if(parent.getRequired() == null){
                parent.required(new ArrayList<>());
            }
            if(!parent.getRequired().contains(currentPart))
                parent.getRequired().add(currentPart);

        }
        // ===== LEAF NODE =====
        if (index == parts.length - 1) {
            Schema<Object> leaf = new Schema<>();
            leaf.setType(leafType);
            leaf.setDescription(description);
            leaf.setExample(example);

            parent.addProperties(fieldName, leaf);
            return;
        }

        // ===== INTERMEDIATE NODE =====
        Schema<?> next = parent.getProperties().get(fieldName);

        if (next == null) {
            if (isArray) {
                ArraySchema arraySchema = new ArraySchema();
                arraySchema.setItems(new ObjectSchema());
                next = arraySchema;
                parent.addProperties(fieldName, arraySchema);
                buildHierarchyRecursive(
                        arraySchema.getItems(),
                        parts,
                        index + 1,
                        leafType,
                        description,
                        example,
                        isMandatory
                );
                return;
            } else {
                next = new ObjectSchema();
                parent.addProperties(fieldName, next);
            }
        }

        buildHierarchyRecursive(
                next,
                parts,
                index + 1,
                leafType,
                description,
                example, isMandatory
        );
    }

    public static void readRequest(List<ApiFieldDef> defs, OpenAPI openAPI, Map<String, PathItem> apiMap){

        Components components = new Components();
        openAPI.setComponents(components);
        components.setSchemas(new LinkedHashMap<>());

        List<ApiFieldDef> filteredLanguages = defs.stream()
                .filter(item -> item.getHeaderOrBody().equalsIgnoreCase("Body") &&
                        item.getRequestOrResponse().equalsIgnoreCase("Request"))
                .toList();
        filteredLanguages.forEach((item) -> {
            if(item.getSchemaPath() != null){
                String[] parts = item.getSchemaPath().split("\\.");
                String rootName = parts[0];
                String[] childParts = Arrays.copyOfRange(parts, 1, parts.length);
                Schema<?> rootSchema = components.getSchemas()
                        .computeIfAbsent(rootName, k -> new ObjectSchema());
                Operation op = getOperation(apiMap, item.getPath(), item.getMethod());

                buildHierarchyRecursive(
                        rootSchema,
                        childParts,
                        0,
                        item.getType(),       // leaf type
                        item.getDesc(),             // description
                        item.getExample(),               // example
                        item.isMandatory
                );
                attachRequestBody(
                        op,
                        item.getSchemaPath(),
                        item.isMandatory,
                        item.contentType
                );
            }
        });
    }

    private static void attachRequestBody(
            Operation op,
            String rootSchemaName,
            boolean required,
            String contentTypes) {

        RequestBody rb = new RequestBody();
        Content content = new Content();

        for (String ct : contentTypes.split(",")) {
            MediaType mt = new MediaType();
            Schema<?> ref = new Schema<>();

            if(rootSchemaName.contains(".")){
                ref.set$ref("#/components/schemas/" + rootSchemaName.split("\\.")[0]);
            }else{
                ref.set$ref("#/components/schemas/" + rootSchemaName);
            }

            mt.setSchema(ref);
            content.addMediaType(ct.trim(), mt);
        }

        rb.setContent(content);
        op.setRequestBody(rb);
    }

    private static void readRequestResponses(
            Workbook wb,
            Map<String, PathItem> apiMap,
            OpenAPI openAPI) {

        try {
            Sheet sheet = wb.getSheet("RequestResponses");
            List<ApiFieldDef> defs =  readExcel(sheet);
            readRequestParameters(defs,apiMap);
            readRequest(defs, openAPI,apiMap);
            readResponse(defs, openAPI,apiMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void readResponse(List<ApiFieldDef> defs, OpenAPI openAPI, Map<String, PathItem> apiMap) {

        List<ApiFieldDef> filteredLanguages = defs.stream()
                .filter(item -> item.getRequestOrResponse().equalsIgnoreCase("Response"))
                .toList();
        filteredLanguages.forEach((item) -> {
            Operation op = getOperation(apiMap, item.getPath(), item.getMethod());
            buildResponsesFromExcel(op, item );
        });
    }
    private static void buildResponsesFromExcel(
            Operation op,ApiFieldDef item

    ) {

        if (op.getResponses() == null) {
            op.setResponses(new ApiResponses());
        }

        ApiResponse response = op.getResponses()
                .computeIfAbsent(item.getCode(), c -> new ApiResponse().description(item.getDesc()));

        // -------- HEADERS --------
        if ("Header".equalsIgnoreCase(item.getHeaderOrBody())) {
            if (response.getHeaders() == null) {
                response.setHeaders(new LinkedHashMap<>());
            }

            io.swagger.v3.oas.models.headers.Header header = new io.swagger.v3.oas.models.headers.Header();
            if (item.get$ref() != null && !item.get$ref().isEmpty()) {
                header.set$ref("#/components/headers/" + item.get$ref());
            }

            response.getHeaders().put(extractLeaf(item.schemaPath), header);
            return;
        }

        // -------- BODY --------
        if (response.getContent() == null) {
            response.setContent(new Content());
        }

        MediaType mediaType = response.getContent()
                .computeIfAbsent(item.getContentType(), ct -> new MediaType());

        if (mediaType.getSchema() == null) {
            mediaType.setSchema(new ObjectSchema());
        }

        // If full schema ref exists → stop hierarchy
        if (item.get$ref() != null && !item.get$ref().isEmpty()) {
            Schema<?> refSchema = new Schema<>();
            refSchema.set$ref("#/components/schemas/" + item.get$ref());
            mediaType.setSchema(refSchema);
            return;
        }

        buildSchemaTree(mediaType.getSchema(), item);
    }


    private static void buildSchemaTree(
            Schema<?> root,
            ApiFieldDef item
    ) {

        String[] parts = item.getSchemaPath().split("\\.");
        Schema<?> current = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (current.getProperties() == null) {
                current.setProperties(new LinkedHashMap<>());
            }

            boolean isLeaf = (i == parts.length - 1);

            Schema<?> child = current.getProperties().get(part);
            if (child == null) {
                child = isLeaf ? new Schema<>() : new ObjectSchema();

                current.getProperties().put(part, child);
            }

            if (isLeaf) {
                child.setTitle(part);
                child.setType(mapType(item.getType()));
                child.setDescription(item.getDesc());
                if (item.getExample() != null && !item.getExample().isEmpty()) {
                    child.setExample(item.getExample());
                }
                if(item.isMandatory()){
                    if(current.getRequired() == null ){
                        current.required(new ArrayList<>());
                    }
                    current.getRequired().add(part);
                }
            }

            current = child;
        }
    }

    private static String extractLeaf(String path) {
        return path.contains(".")
                ? path.substring(path.lastIndexOf('.') + 1)
                : path;
    }

    private static String mapType(String excelType) {
        if (excelType == null) return "string";
        switch (excelType.toLowerCase()) {
            case "string": return "string";
            case "array": return "array";
            case "integer": return "integer";
            case "number": return "number";
            case "boolean": return "boolean";
            default: return "string";
        }
    }


    // ================= SCHEMA =================

    private static Operation getOperation(Map<String, PathItem> map, String path, String method) {
        PathItem item = map.get(path);
        if (item == null) return null;
        switch (method.toUpperCase()) {
            case "POST": return item.getPost();
            case "GET": return item.getGet();
            case "PUT": return item.getPut();
            case "PATCH": return item.getPatch();
            case "HEAD": return item.getHead();
            case "DELETE": return item.getDelete();
        }
        return null;
    }
}
