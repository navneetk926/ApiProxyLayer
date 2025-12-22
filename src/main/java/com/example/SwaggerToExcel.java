package example.swagger;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SwaggerToExcel {

    public static void main(String[] args) throws Exception {

        String swaggerFile = "C:\\Users\\ADMIN\\Downloads\\accounts.yaml";
        String outputFile = "C:\\Users\\ADMIN\\Downloads\\accounts.xlsx";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().read(swaggerFile, null, options);

        Workbook workbook = new XSSFWorkbook();

        createInfoSheet(workbook, openAPI);
        createServerSecuritySheet(workbook, openAPI);
        createApiSheet(workbook, openAPI);
        createParameterSheet(workbook, openAPI);
        createRequestBodySheet(workbook, openAPI);
        createResponseSheet(workbook, openAPI);

        FileOutputStream fos = new FileOutputStream(outputFile);
        workbook.write(fos);
        workbook.close();
        fos.close();

        System.out.println("Excel generated: " + outputFile);
    }

    // ================= INFO SHEET =================
    private static void createInfoSheet(Workbook wb, OpenAPI api) {
        Sheet sheet = wb.createSheet("Info");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Title");
        header.createCell(1).setCellValue("Version");
        header.createCell(2).setCellValue("Description");

        Row row = sheet.createRow(1);
        if (api.getInfo() != null) {
            row.createCell(0).setCellValue(api.getInfo().getTitle() != null ? api.getInfo().getTitle() : "");
            row.createCell(1).setCellValue(api.getInfo().getVersion() != null ? api.getInfo().getVersion() : "");
            row.createCell(2).setCellValue(api.getInfo().getDescription() != null ? api.getInfo().getDescription() : "");
        }
    }

    // ================= SERVERS & SECURITY SHEET =================
    private static void createServerSecuritySheet(Workbook wb, OpenAPI api) {
        Sheet sheet = wb.createSheet("ServersSecurity");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("URL");
        header.createCell(1).setCellValue("Description");
        header.createCell(2).setCellValue("Security");

        AtomicInteger rowNum = new AtomicInteger(1);

        if (api.getServers() != null) {
            for (Server s : api.getServers()) {
                Row row = sheet.createRow(rowNum.getAndIncrement());
                row.createCell(0).setCellValue(s.getUrl() != null ? s.getUrl() : "");
                row.createCell(1).setCellValue(s.getDescription() != null ? s.getDescription() : "");
                row.createCell(2).setCellValue(""); // security handled separately
            }
        }

        if (api.getSecurity() != null) {
            for (SecurityRequirement sec : api.getSecurity()) {
                Row row = sheet.createRow(rowNum.getAndIncrement());
                row.createCell(0).setCellValue("");
                row.createCell(1).setCellValue("");
                row.createCell(2).setCellValue(String.join(",", sec.keySet()));
            }
        }
    }

    // ================= API SHEET =================
    private static void createApiSheet(Workbook wb, OpenAPI api) {
        Sheet sheet = wb.createSheet("APIs");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Path");
        header.createCell(1).setCellValue("Method");
        header.createCell(2).setCellValue("Summary");
        header.createCell(3).setCellValue("Description");
        header.createCell(4).setCellValue("Tags");
        header.createCell(5).setCellValue("Enum");

        AtomicInteger rowNum = new AtomicInteger(1);
        api.getPaths().forEach((path, item) -> {
            item.readOperationsMap().forEach((method, op) -> {
                Row row = sheet.createRow(rowNum.getAndIncrement());
                row.createCell(0).setCellValue(path);
                row.createCell(1).setCellValue(method.name());
                row.createCell(2).setCellValue(op.getSummary() != null ? op.getSummary() : "");
                row.createCell(3).setCellValue(op.getDescription() != null ? op.getDescription() : "");
                row.createCell(4).setCellValue(op.getTags() != null ? String.join(",", op.getTags()) : "");

                // Optional: enum from requestBody if any
                String enums = "";
                if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
                    for (MediaType mt : op.getRequestBody().getContent().values()) {
                        Schema<?> schema = mt.getSchema();
                        if (schema != null && schema.getEnum() != null) {
                            enums = schema.getEnum().toString();
                            break;
                        }
                    }
                }
                row.createCell(5).setCellValue(enums);
            });
        });
    }

    // ================= PARAMETERS SHEET =================
    private static void createParameterSheet(Workbook wb, OpenAPI api) {
        Sheet sheet = wb.createSheet("Parameters");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Path");
        header.createCell(1).setCellValue("Method");
        header.createCell(2).setCellValue("Name");
        header.createCell(3).setCellValue("In");
        header.createCell(4).setCellValue("Type");
        header.createCell(5).setCellValue("Required");
        header.createCell(6).setCellValue("Description");
        header.createCell(7).setCellValue("Enum");
        header.createCell(8).setCellValue("Example");
        header.createCell(9).setCellValue("Reference");

        AtomicInteger rowNum = new AtomicInteger(1);
        api.getPaths().forEach((path, item) -> {
            item.readOperationsMap().forEach((method, op) -> {
                if (op.getParameters() != null) {
                    for (Parameter p : op.getParameters()) {
                        Row row = sheet.createRow(rowNum.getAndIncrement());
                        row.createCell(0).setCellValue(path);
                        row.createCell(1).setCellValue(method.name());
                        row.createCell(2).setCellValue(p.getName());
                        row.createCell(3).setCellValue(p.getIn());
                        Schema<?> schema = p.getSchema();
                        if (schema != null) {
                            row.createCell(4).setCellValue(schema.getType() != null ? schema.getType() : "");
                            row.createCell(7).setCellValue(schema.getEnum() != null ? schema.getEnum().toString() : "");
                            row.createCell(8).setCellValue(schema.getExample() != null ? schema.getExample().toString() : "");
                            row.createCell(9).setCellValue(schema.get$ref() != null ? schema.get$ref() : "");
                        }
                        row.createCell(5).setCellValue(p.getRequired() != null && p.getRequired());
                        row.createCell(6).setCellValue(p.getDescription() != null ? p.getDescription() : "");
                    }
                }
            });
        });
    }

    // ================= REQUEST BODY SHEET =================
    private static void createRequestBodySheet(Workbook wb, OpenAPI api) {
        Sheet sheet = wb.createSheet("RequestBody");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Path");
        header.createCell(1).setCellValue("Method");
        header.createCell(2).setCellValue("Field Path");
        header.createCell(3).setCellValue("Type");
        header.createCell(4).setCellValue("Description");
        header.createCell(5).setCellValue("Enum");
        header.createCell(6).setCellValue("Example");
        header.createCell(7).setCellValue("Reference");

        int[] rowNum = {1};
        api.getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, op) -> {
                    if (op.getRequestBody() == null || op.getRequestBody().getContent() == null) return;

                    for (MediaType mt : op.getRequestBody().getContent().values()) {
                        flattenSchema(sheet, path, method.name(), "", mt.getSchema(), rowNum);
                    }
                })
        );
    }

    // ================= RESPONSES SHEET =================
    private static void createResponseSheet(Workbook wb, OpenAPI api) {
        Sheet sheet = wb.createSheet("Responses");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Path");
        header.createCell(1).setCellValue("Method");
        header.createCell(2).setCellValue("HTTP Code");
        header.createCell(3).setCellValue("Field Path");
        header.createCell(4).setCellValue("Type");
        header.createCell(5).setCellValue("Summary");
        header.createCell(6).setCellValue("Example");
        header.createCell(7).setCellValue("Reference");

        int[] rowNum = {1};
        api.getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, op) -> {
                    if (op.getResponses() == null) return;
                    for (Map.Entry<String, ApiResponse> e : op.getResponses().entrySet()) {
                        String code = e.getKey();
                        ApiResponse resp = e.getValue();
                        if (resp.getContent() == null) continue;

                        for (MediaType mt : resp.getContent().values()) {
                            flattenSchemaResponse(sheet, path, method.name(), code, "", mt.getSchema(), resp.getDescription(), rowNum);
                        }
                    }
                })
        );
    }

    // ================= Flatten Schema for RequestBody =================
    private static void flattenSchema(Sheet sheet, String path, String method, String prefix, Schema<?> schema, int[] r) {
        if (schema == null) return;

        String fieldName = prefix.isEmpty() ? "" : prefix;

        if (schema.getProperties() != null) {
            for (Map.Entry<String, Schema> e : schema.getProperties().entrySet()) {
                Schema<?> s = e.getValue();
                String fieldPath = fieldName.isEmpty() ? e.getKey() : fieldName + "." + e.getKey();

                Row row = sheet.createRow(r[0]++);
                row.createCell(0).setCellValue(path);
                row.createCell(1).setCellValue(method);
                row.createCell(2).setCellValue(fieldPath);
                row.createCell(3).setCellValue(s.getType() != null ? s.getType() : "object");
                row.createCell(4).setCellValue(s.getDescription() != null ? s.getDescription() : "");
                row.createCell(5).setCellValue(s.getEnum() != null ? s.getEnum().toString() : "");
                row.createCell(6).setCellValue(s.getExample() != null ? s.getExample().toString() : "");
                if (schema instanceof ArraySchema) {
                    Schema<?> items = ((ArraySchema) schema).getItems();
                    if (items.get$ref() != null) {
                        row.createCell(7).setCellValue(items.get$ref());
                    }
                    flattenSchema(sheet, path, method, fieldPath + "[]", items, r);
                }else if (schema.get$ref() != null) {
                    row.createCell(7).setCellValue(schema.get$ref());
                } else {
                    row.createCell(7).setCellValue("");
                }

                flattenSchema(sheet, path, method, fieldPath, s, r);
            }
        }
    }

    // ================= Flatten Schema for Response =================
    private static void flattenSchemaResponse(Sheet sheet, String path, String method, String code, String prefix, Schema<?> schema, String summary, int[] r) {
        if (schema == null) return;

        String fieldName = prefix.isEmpty() ? "" : prefix;

        if (schema.getProperties() != null) {
            for (Map.Entry<String, Schema> e : schema.getProperties().entrySet()) {
                Schema<?> s = e.getValue();
                String fieldPath = fieldName.isEmpty() ? e.getKey() : fieldName + "." + e.getKey();

                Row row = sheet.createRow(r[0]++);
                row.createCell(0).setCellValue(path);
                row.createCell(1).setCellValue(method);
                row.createCell(2).setCellValue(code);
                row.createCell(3).setCellValue(fieldPath);
                row.createCell(4).setCellValue(s.getType() != null ? s.getType() : "object");
                row.createCell(5).setCellValue(summary != null ? summary : "");
                row.createCell(6).setCellValue(s.getExample() != null ? s.getExample().toString() : "");
                if (schema instanceof ArraySchema) {
                    Schema<?> items = ((ArraySchema) schema).getItems();
                    if (items.get$ref() != null) {
                        row.createCell(7).setCellValue(items.get$ref());
                    }
                    flattenSchema(sheet, path, method, fieldPath + "[]", items, r);
                }else if (schema.get$ref() != null) {
                    row.createCell(7).setCellValue(schema.get$ref());
                } else {
                    row.createCell(7).setCellValue("");
                }

                flattenSchemaResponse(sheet, path, method, code, fieldPath, s, summary, r);
            }
        }
    }
}
