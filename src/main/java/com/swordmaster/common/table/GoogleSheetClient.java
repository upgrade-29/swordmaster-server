package com.swordmaster.common.table;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class GoogleSheetClient {
    private static final String EXPORT_URL =
            "https://docs.google.com/spreadsheets/d/%s/export?format=csv&gid=%d";

    private static final Pattern  INTEGER          = Pattern.compile("-?\\d+");
    private static final Pattern  DECIMAL          = Pattern.compile("-?\\d+\\.\\d+");
    private static final Duration DURATION_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)    // 구글이 실제 파일이 위치한 주소로 리다이렉트
            .connectTimeout(DURATION_TIMEOUT)
            .build();

    private final String sheetId;

    public GoogleSheetClient(@Value("${game-data.sheet-id}") String sheetId) {
        this.sheetId = sheetId;
    }

    // gid에 해당하는 시트를 한 줄씩 변환
    public List<Map<String, Object>> readSheet(long gid) {
        return toRows(parseCsv(download(gid)));
    }

    private String download(long gid) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(EXPORT_URL.formatted(sheetId, gid)))
                .timeout(DURATION_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response;
        String               gidOutput = "(gid=" + gid + ")";

        // 모든 예외는 서버 부팅시 발생
        try {
            response = http.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "구글 시트 접속에 실패하였습니다. " + gidOutput + ": " + e.getMessage(), e
            );
        } catch (InterruptedException e) {          // 중단 신호가 올 경우
            Thread.currentThread().interrupt();     // 중단 로그 신호 기록

            throw new IllegalStateException("구글 시트 읽기가 중단되었습니다. " + gidOutput, e);
        }

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "구글 시트 다운로드에 실패하였습니다. (HTTP " + response.statusCode()
                    + ", " + gidOutput + ". sheet-id와 gid를 확인하세요."
            );
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("");

        // 시트가 공유가 안되있으면 구글은 에러 대신 로그인 화면(웹 페이지)를 성공(200)으로 응답하는 문제
        if (contentType.contains("text/html")) {
            throw new IllegalStateException(
                    "CSV 대신 웹 페이지가 왔습니다. " + gidOutput
                    + ". 시트가 '링크가 있는 모든 사용자'로 공유됐는지 확인하세요."
            );
        }

        String body = response.body();

        // 일부 파일 맨 앞에 붙는 잘 안보이는 'BOM' 문자 처리
        return body.startsWith("\uFEFF") ? body.substring(1) : body;
    }

    static List<List<String>> parseCsv(String text) {
        List<List<String>> rows     = new ArrayList<>();
        List<String>       row      = new ArrayList<>();
        StringBuilder      field    = new StringBuilder();
        boolean            inQuotes = false;    // 쉼표나 줄바꿈 전부 칸의 내용으로 취급할 지 여부

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;

                row.add(field.toString());
                field.setLength(0);
                rows.add(row);

                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }

        if (!field.isEmpty() || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }

        return rows;
    }

    static List<Map<String, Object>> toRows(List<List<String>> table) {
        if (table.isEmpty()) throw new IllegalStateException("시트가 비어 있습니다.");

        // 첫 행은 헤더 (컬럼 이름 리스트)
        List<String> headers = table.get(0).stream().map(String::strip).toList();
        Set<String>  seen    = new HashSet<>();

        for (String header : headers) {
            if (!header.isEmpty() && !seen.add(header)) {
                throw new IllegalStateException(header + " 컬럼 이름이 중복되어 있습니다.");
            }
        }

        if (seen.isEmpty()) throw new IllegalStateException("헤더 행이 존재하지 않습니다.");

        // 이후 행 들을 JSON 형식으로 변환
        List<Map<String, Object>> rows = new ArrayList<>();

        for (List<String> line : table.subList(1, table.size())) {
            Map<String, Object> row       = new LinkedHashMap<>();    // 시트의 컬럼 순서를 그대로 유지
            boolean             lineEmpty = true;

            for (int col = 0; col < headers.size(); col++) {
                String header = headers.get(col);

                if (header.isEmpty()) continue;     // 이름 없는 컬럼은 무시

                // String 형식이 아닌 데이터 가공
                Object value = convert(col < line.size() ? line.get(col) : "");

                if (value != null) lineEmpty = false;

                row.put(header, value);
            }

            if (!lineEmpty) rows.add(row);   // 완전히 빈 줄은 건너뜀
        }

        return rows;
    }

    static Object convert(String rawValue) {
        String text = rawValue.strip();

        if (text.isEmpty()) return null;

        if (INTEGER.matcher(text).matches()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("정수 범위를 벗어난 값입니다: " + text, e);
            }
        }

        if (DECIMAL.matcher(text).matches()) return Double.parseDouble(text);

        return text;
    }
}
