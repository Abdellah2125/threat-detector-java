package com.threatdetector.intake;

import com.threatdetector.model.CsvLoadReport;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.TargetKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvLoaderTest {
    @TempDir
    Path tempDir;

    private Path write(String name, String content) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    private static final String VALID_CSV = """
            type,target
            url,https://example.com
            message,"URGENT: verify your password at bit.ly/x"
            """;

    @Test
    void loads_url_and_message_rows() throws Exception {
        CsvLoadReport report = new CsvLoader().load(write("inputs.csv", VALID_CSV));
        assertEquals("inputs.csv", report.fileName());
        assertEquals(2, report.rows().size());
        assertEquals(TargetKind.URL, report.rows().get(0).kind());
        assertEquals(TargetKind.MESSAGE, report.rows().get(1).kind());
        assertEquals(2, report.validCount());
        assertEquals(0, report.invalidCount());
    }

    @Test
    void rejects_missing_file() {
        assertThrows(LoadException.class,
                () -> new CsvLoader().load(tempDir.resolve("missing.csv")));
    }

    @Test
    void rejects_wrong_header() throws Exception {
        LoadException error = assertThrows(LoadException.class,
                () -> new CsvLoader().load(write("bad.csv", "kind,value\nurl,https://example.com\n")));
        assertTrue(error.getMessage().contains("type,target"));
    }

    @Test
    void unsupported_type_stays_not_verified() throws Exception {
        CsvLoadReport report = new CsvLoader().load(write("bad-row.csv",
                "type,target\nunknown,https://example.com\n"));
        ScanOutcome outcome = report.rows().get(0).outcome();
        assertEquals("INVALID_CSV_ROW", ((ScanOutcome.NotVerified) outcome).code());
        assertEquals(1, report.invalidCount());
        assertEquals(0, report.validCount());
    }

    @Test
    void empty_target_stays_not_verified() throws Exception {
        CsvLoadReport report = new CsvLoader().load(write("empty-target.csv",
                "type,target\nurl,\n"));
        assertEquals("INVALID_CSV_ROW", ((ScanOutcome.NotVerified) report.rows().get(0).outcome()).code());
    }

    @Test
    void warnings_are_reported_for_invalid_rows() throws Exception {
        CsvLoadReport report = new CsvLoader().load(write("warn.csv",
                "type,target\nurl,\nmessage,hello\n"));
        assertFalse(report.warnings().isEmpty());
        assertTrue(report.warnings().get(0).contains("Row 2"));
    }

    @Test
    void quoted_comma_in_target_is_parsed_as_one_cell() throws Exception {
        CsvLoadReport report = new CsvLoader().load(write("quoted.csv",
                "type,target\nmessage,\"Hello, verify your password now\"\n"));
        assertEquals("Hello, verify your password now", report.rows().get(0).target());
    }

    @Test
    void empty_csv_is_rejected() throws Exception {
        assertThrows(LoadException.class, () -> new CsvLoader().load(write("empty.csv", "")));
    }
}
