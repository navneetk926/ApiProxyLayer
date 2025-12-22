package example.swagger;
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
import java.util.*;

public class ExcelToSwaggerYaml {

    public static void main(String[] args) throws Exception {

        Workbook wb = WorkbookFactory.create(new File("C:\\Users\\ADMIN\\Downloads\\accounts.xlsx"));

        OpenAPI openAPI = new OpenAPI();

        // ======== Info Sheet ========
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

        // ======== Servers & Security Sheet ========
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

        // ======== APIs Sheet ========
        Map<String, PathItem> apiMap = readApis(wb);

        // ======== Parameters Sheet ========
        readParameters(wb, apiMap);

        // ======== RequestBody Sheet ========
        readRequestBodies(wb, apiMap);

        // ======== Responses Sheet ========
        readResponses(wb, apiMap);

        // ======== Set paths ========
        Paths paths = new Paths();
        apiMap.forEach(paths::addPathItem);
        openAPI.setPaths(paths);

        // ======== Write YAML ========
        ObjectMapper mapper = Yaml.mapper();
        mapper.writeValue(new File("C:\\Users\\ADMIN\\Downloads\\swagger.yaml"), openAPI);

        System.out.println("Swagger YAML generated successfully!");
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
                case "DELETE": item.setDelete(op); break;
            }
            map.put(path, item);
        }
        return map;
    }

    // ===================== Parameters Sheet =====================
    private static void readParameters(Workbook wb, Map<String, PathItem> map) {
        Sheet sheet = wb.getSheet("Parameters");
        if (sheet == null) return;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null || getCellValue(r, 0).isEmpty()) continue;

            Operation op = getOperation(map, getCellValue(r,0), getCellValue(r,1));
            if (op == null) continue;

            Parameter p = new Parameter();
            p.setName(getCellValue(r,2));
            p.setIn(getCellValue(r,3));
            p.setRequired(Boolean.parseBoolean(getCellValue(r,5)));
            p.setDescription(getCellValue(r,6));

            Schema<Object> schema = new Schema<>();
            schema.setType(getCellValue(r,4));

            // Enum
            String enumVal = getCellValue(r,7);
            if (!enumVal.isEmpty()) {
                List<Object> enums = new ArrayList<>();
                for (String v : enumVal.replace("[","").replace("]","").split(",")) enums.add(v.trim());
                schema.setEnum(enums);
            }

            String example = getCellValue(r,8);
            if (!example.isEmpty()) schema.setExample(example);

            p.setSchema(schema);
            op.addParametersItem(p);
        }
    }

    // ===================== RequestBody Sheet =====================
    private static void readRequestBodies(Workbook wb, Map<String, PathItem> map) {
        Sheet sheet = wb.getSheet("RequestBody");
        if (sheet == null) return;

        Map<String, ObjectSchema> requestBodies = new HashMap<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null || getCellValue(r,0).isEmpty()) continue;

            String path = getCellValue(r,0);
            String method = getCellValue(r,1);
            Operation op = getOperation(map, path, method);
            if (op == null) continue;

            String type = getCellValue(r,3);
            if ("object".equalsIgnoreCase(type)) continue; // ignore object rows

            String fieldPath = getCellValue(r,2);
            ObjectSchema root = requestBodies.computeIfAbsent(path+"_"+method, k -> new ObjectSchema());
            buildSchemaTree(root,
                    fieldPath,
                    type,
                    getCellValue(r,4),
                    getCellValue(r,5),
                    getCellValue(r,6));

            RequestBody rb = new RequestBody();
            rb.setContent(new Content().addMediaType("application/json", new MediaType().schema(root)));
            op.setRequestBody(rb);
        }
    }

    // ===================== Responses Sheet =====================
    private static void readResponses(Workbook wb, Map<String, PathItem> map) {
        Sheet sheet = wb.getSheet("Responses");
        if (sheet == null) return;

        Map<String, ObjectSchema> responseBodies = new HashMap<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null || getCellValue(r,0).isEmpty()) continue;

            String path = getCellValue(r,0);
            String method = getCellValue(r,1);
            String httpCode = getCellValue(r,2);
            Operation op = getOperation(map, path, method);
            if (op == null) continue;

            String fieldPath = getCellValue(r,3);
            String type = getCellValue(r,4);
            String summary = getCellValue(r,5);
            String example = getCellValue(r,6);

            ObjectSchema root = responseBodies.computeIfAbsent(path+"_"+method+"_"+httpCode, k -> new ObjectSchema());
            buildSchemaTree(root, fieldPath, type, summary, null, example);

            ApiResponse ar = new ApiResponse()
                    .description(summary)
                    .content(new Content().addMediaType("application/json", new MediaType().schema(root)));

            if (op.getResponses() == null) op.setResponses(new ApiResponses());
            op.getResponses().addApiResponse(httpCode, ar);
        }
    }

    private static void buildSchemaTree(
            Schema<?> root,
            String fieldPath,
            String type,
            String description,
            String enumVal,
            String example) {

        String[] parts = fieldPath.split("\\.");
        Schema<?> current = root;

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (current.getProperties() == null) current.setProperties(new LinkedHashMap<>());

            if (i == parts.length-1) {
                Schema<Object> leaf = new Schema<>();
                leaf.setType(type);
                leaf.setDescription(description);

                if (enumVal != null && !enumVal.isEmpty()) {
                    List<Object> enums = new ArrayList<>();
                    for (String v : enumVal.replace("[","").replace("]","").split(",")) enums.add(v.trim());
                    leaf.setEnum(enums);
                }

                if (example != null && !example.isEmpty()) leaf.setExample(example);
                current.addProperties(p, leaf);

            } else {
                Schema<?> next = current.getProperties().get(p);
                if (next == null) {
                    next = new ObjectSchema();
                    current.addProperties(p, next);
                }
                current = next; // no cast
            }
        }
    }

    private static Operation getOperation(Map<String, PathItem> map, String path, String method) {
        PathItem item = map.get(path);
        if (item == null) return null;
        switch (method.toUpperCase()) {
            case "POST": return item.getPost();
            case "GET": return item.getGet();
            case "PUT": return item.getPut();
            case "DELETE": return item.getDelete();
        }
        return null;
    }
}
