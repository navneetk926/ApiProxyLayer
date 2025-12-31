package org.example.swagger;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
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
    private static int rowNum = 1;
    public static void main(String[] args) throws Exception {

        String swaggerFile = "C:\\Users\\ADMIN\\Downloads\\paymentinitiation-pain103.yaml";
        String outputFile = "C:\\Users\\ADMIN\\Downloads\\uae.xlsx";

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().read(swaggerFile, null, options);

        Workbook workbook = new XSSFWorkbook();

        createInfoSheet(workbook, openAPI);
        createServerSecuritySheet(workbook, openAPI);
        createApiSheet(workbook, openAPI);
        createRequestResponseSheet(workbook, openAPI);

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



    // =================REQUEST RESPONSES SHEET =================
    private static void createRequestResponseSheet(Workbook wb, OpenAPI api) {
        Sheet sheet = createHeaderExcelRow(wb);
        rowNum = 1;
        api.getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, op) -> {
                    if(op.getParameters() != null) {
                        op.getParameters().forEach(parameter -> {
                            ApiFieldDef apiFieldDef = new ApiFieldDef();
                            apiFieldDef.setPath(path);
                            apiFieldDef.setMethod(method.name());
                            apiFieldDef.setHeaderOrBody("Header");
                            apiFieldDef.setMandatory(parameter.getRequired());
                            apiFieldDef.setDesc(parameter.getDescription());
                            if(parameter.getSchema() != null ) {
                                apiFieldDef.setTitle(parameter.getSchema().getTitle());
                                apiFieldDef.setType(parameter.getSchema().getType());
                            }
                            if(apiFieldDef.getTitle() == null) {
                                apiFieldDef.setTitle(parameter.getName());
                            }
                            apiFieldDef.setDesc(parameter.getDescription());
                            if(parameter.getExample() != null) {
                                apiFieldDef.setExample(parameter.getExample().toString());
                            }

                            apiFieldDef.setRequestOrResponse("Request");
                            writeSwaggerResponseToExcel(sheet, apiFieldDef);
                        });
                    }
                    if (op.getResponses() != null){
                        for (Map.Entry<String, ApiResponse> e : op.getResponses().entrySet()) {
                            ApiResponse resp = e.getValue();

                            if (resp.getHeaders() != null) {
                                resp.getHeaders().keySet().forEach(header -> {
                                    io.swagger.v3.oas.models.headers.Header h = resp.getHeaders().get(header);
                                    ApiFieldDef apiFieldDef = new ApiFieldDef();
                                    apiFieldDef.setPath(path);
                                    apiFieldDef.setMethod(method.name());
                                    apiFieldDef.setCode(e.getKey());
                                    if(h.getSchema() != null && h.getSchema().getTitle() != null) {
                                        apiFieldDef.setTitle(h.getSchema().getTitle());
                                    }
                                    if(h.getSchema() != null && h.getSchema().getType()!= null) {
                                        apiFieldDef.setType(h.getSchema().getType());
                                    }
                                    apiFieldDef.setSchemaPath(op.getOperationId()+e.getKey());
                                    apiFieldDef.setHeaderOrBody("Header");
                                    if(h.getSchema() != null && h.getSchema().getDescription() != null) {
                                        apiFieldDef.setDesc(h.getSchema().getDescription());
                                    }else{
                                        apiFieldDef.setDesc(resp.getDescription());
                                    }
                                    if(h.getSchema() != null && h.getSchema().getMinLength() != null) {
                                        apiFieldDef.setMinValue(Double.valueOf(h.getSchema().getMinLength()));
                                    }
                                    if(h.getSchema() != null && h.getSchema().getMaxLength() != null) {
                                        apiFieldDef.setMaxValue(Double.valueOf(h.getSchema().getMaxLength()));
                                    }
                                    if(h.getSchema() != null && h.getSchema().getExample() != null) {
                                        apiFieldDef.setExample(h.getSchema().getExample().toString());
                                    }else if(h.getExample() != null){
                                        apiFieldDef.setExample(h.getExample().toString());
                                    }

                                    apiFieldDef.setRequestOrResponse("Response");
                                    writeSwaggerResponseToExcel(sheet, apiFieldDef);
                                });

                            }
                            if (resp.getContent() != null) {
                                Set<Map.Entry<String, MediaType>> entrySet = resp.getContent().entrySet();

                                if(!entrySet.isEmpty()){
                                    Map.Entry<String, MediaType> entry = entrySet.iterator().next();
                                    String mediaType = entry.getKey();          // 🔥 application/json
                                    MediaType mt = entry.getValue();             // schema, examples, etc.
                                    //String contentType = entrySet.size() >1 ? "application/json,application/xml": mediaType;
                                    Schema<?> schema = mt.getSchema();
                                    prepareExcelObject(sheet,mediaType,
                                            schema, op.getOperationId()+e.getKey(), e.getKey(),
                                            path, method.name(), "Response", "", schema.getRequired());
                                }
                            }
                        }
                    }
                })
        );
    }

    private static Sheet createHeaderExcelRow(Workbook wb) {
        Sheet sheet = wb.createSheet("RequestResponses");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Path");
        header.createCell(1).setCellValue("Method");
        header.createCell(2).setCellValue("HTTP Code");
        header.createCell(3).setCellValue("Field Path");
        header.createCell(4).setCellValue("Type");
        header.createCell(5).setCellValue("Title");
        header.createCell(6).setCellValue("Example");
        header.createCell(7).setCellValue("Reference");
        header.createCell(8).setCellValue("Description");
        header.createCell(9).setCellValue("ContentType");
        header.createCell(10).setCellValue("BodyOrHeader");
        header.createCell(11).setCellValue("RequestOrResponse");
        header.createCell(12).setCellValue("IsMandatory");
        header.createCell(13).setCellValue("MinValue");
        header.createCell(14).setCellValue("MaxValue");
        header.createCell(15).setCellValue("EnumValue");
        return sheet;
    }

    private static void prepareExcelObject(Sheet sheet, String contentType, Schema<?> schema,
                                           String operationId, String code, String path, String method,
                                           String reqOrRes, String childProperty, List<String> requirestList){


        if(schema.getProperties() != null ){
            Iterator<Map.Entry<String, Schema>> propertyIterator = schema.getProperties().entrySet().iterator();
            while (propertyIterator.hasNext()) {
                Map.Entry<String, Schema> mapElement = propertyIterator.next();
                Schema<?> schemaValue = schema.getProperties().get(mapElement.getKey());
                ApiFieldDef apiFieldDef = getApiFieldDef(contentType, operationId, code, path, method, reqOrRes, mapElement.getKey(), schemaValue, requirestList, childProperty);
                if(reqOrRes.equalsIgnoreCase("Response") && schemaValue.getItems() != null && schemaValue.getItems().getProperties() != null){
                    schemaValue.getItems().getProperties().forEach((property,item)->{
                        Schema<?> childSchemaValue = schemaValue.getItems().getProperties().get(property);
                        prepareExcelObject(sheet,contentType,childSchemaValue, apiFieldDef.getSchemaPath(), code, path, method,
                            reqOrRes, property, schemaValue.getItems().getRequired());
                        });
                    }else if(reqOrRes.equalsIgnoreCase("Response") && schemaValue.getProperties() != null){
                        schemaValue.getProperties().forEach((property,item)->{
                            Schema<?> childSchemaValue = schemaValue.getProperties().get(property);

                            prepareExcelObject(sheet,contentType,childSchemaValue, apiFieldDef.getSchemaPath(), code, path, method,
                                  reqOrRes, property, schemaValue.getRequired());


                    });
                }else{
                        writeSwaggerResponseToExcel(sheet, apiFieldDef);
                    }
                }
        }else{
            ApiFieldDef apiFieldDef = getApiFieldDef(contentType, operationId, code, path, method, reqOrRes, schema.getTitle(), schema,requirestList, childProperty);
            writeSwaggerResponseToExcel(sheet, apiFieldDef);
        }
    }

    private static ApiFieldDef getApiFieldDef(String contentType, String operationId, String code, String path, String method,
                                              String reqOrRes, String subPath, Schema<?> schemaValue, List<String> requirestList,
                                              String property) {
        ApiFieldDef apiFieldDef = new ApiFieldDef();
        apiFieldDef.setPath(path);
        apiFieldDef.setMethod(method);
        apiFieldDef.setCode(code);
        if(property != null && !property.isEmpty()){
            apiFieldDef.setSchemaPath(operationId +"."+ property);
            apiFieldDef.setType(schemaValue.getType());
        }else{
            if(subPath != null && !subPath.isEmpty() && !subPath.equalsIgnoreCase("200")){
                apiFieldDef.setSchemaPath(operationId +"."+ subPath);
                apiFieldDef.setType( "string");
            }else{
                apiFieldDef.setSchemaPath(operationId );
                apiFieldDef.setType("object");
            }

        }


        apiFieldDef.setHeaderOrBody("Body");
        if(schemaValue.getExample() != null)
            apiFieldDef.setExample(schemaValue.getExample().toString());
        apiFieldDef.setTitle(schemaValue.getTitle());
        apiFieldDef.setDesc(schemaValue.getDescription());
        apiFieldDef.setContentType(contentType);
        apiFieldDef.setRequestOrResponse(reqOrRes);
        if(schemaValue.getMinLength() != null && schemaValue.getMinLength() > 0){
            apiFieldDef.setMinValue(schemaValue.getMinLength().doubleValue());
        }
        if(schemaValue.getMaxLength() != null && schemaValue.getMaxLength() > 0){
            apiFieldDef.setMaxValue(schemaValue.getMaxLength().doubleValue());
        }

        if(schemaValue.getEnum() != null)
            apiFieldDef.setEnumValue(schemaValue.getEnum().toString());
        if(requirestList != null && !requirestList.isEmpty() && requirestList.contains(property)){
            apiFieldDef.setMandatory(Boolean.TRUE);
        }else{
            apiFieldDef.setMandatory(Boolean.FALSE);
        }
        return apiFieldDef;
    }

    private static void writeSwaggerResponseToExcel(Sheet sheet, ApiFieldDef apiFieldDef) {

        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(apiFieldDef.getPath());
        row.createCell(1).setCellValue(apiFieldDef.getMethod());
        row.createCell(2).setCellValue(apiFieldDef.getCode());
        row.createCell(3).setCellValue(apiFieldDef.getSchemaPath());
        row.createCell(4).setCellValue(apiFieldDef.getType());
        row.createCell(5).setCellValue(apiFieldDef.getTitle());
        row.createCell(6).setCellValue(apiFieldDef.getExample());
        row.createCell(7).setCellValue(apiFieldDef.get$ref());
        row.createCell(8).setCellValue(apiFieldDef.getDesc());
        row.createCell(9).setCellValue(apiFieldDef.getContentType());
        row.createCell(10).setCellValue(apiFieldDef.getHeaderOrBody());
        row.createCell(11).setCellValue(apiFieldDef.getRequestOrResponse());
        row.createCell(12).setCellValue(apiFieldDef.isMandatory);
        if(apiFieldDef.getMinValue() != null)
            row.createCell(13).setCellValue(apiFieldDef.getMinValue());
        if(apiFieldDef.getMaxValue() != null)
            row.createCell(14).setCellValue(apiFieldDef.getMaxValue());
        row.createCell(15).setCellValue(apiFieldDef.getEnumValue());
    }


}
