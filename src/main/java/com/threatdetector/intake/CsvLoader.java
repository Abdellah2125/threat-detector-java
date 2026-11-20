package com.threatdetector.intake;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.threatdetector.model.CsvLoadReport;
import com.threatdetector.model.LoadedRow;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.TargetKind;

/** Bounded, validated CSV intake. Dedicated backend service, no UI logic. */
public final class CsvLoader {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int MAX_ROWS = 5000;
    private static final int MAX_WARNINGS = 12;

    public CsvLoadReport load(Path path) throws LoadException {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            throw new LoadException("CSV file not found: " + path);
        }
        try {
            if (Files.size(path) > MAX_BYTES) {
                throw new LoadException("CSV file is larger than 5 MB.");
            }
            String content = Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
            return parse(path.getFileName().toString(), content);
        } catch (MalformedInputException exception) {
            throw new LoadException("CSV file is not valid UTF-8.", exception);
        } catch (IOException exception) {
            throw new LoadException("Could not read CSV file: " + exception.getMessage(), exception);
        }
    }

    public CsvLoadReport parse(String fileName, String content) throws LoadException {
        String text = content.startsWith("\uFEFF") ? content.substring(1) : content;
        String[] lines = text.split("\\R", -1);
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new LoadException("CSV file is empty.");
        }

        List<String> header = parseLine(lines[0]);
        if (header.size() < 2 || !header.get(0).equalsIgnoreCase("type")
                || !header.get(1).equalsIgnoreCase("target")) {
            throw new LoadException("CSV header must be: type,target");
        }

        int dataRows = 0;
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isBlank()) dataRows++;
        }
        if (dataRows > MAX_ROWS) {
            throw new LoadException("CSV has more than 5,000 rows.");
        }

        List<LoadedRow> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int valid = 0, invalid = 0;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) continue;
            int rowNumber = i + 1;
            List<String> cells = parseLine(line);
            String type = cells.size() > 0 ? cells.get(0).trim() : "";
            String target = cells.size() > 1 ? cells.get(1).trim() : "";

            TargetKind kind = null;
            if (type.equalsIgnoreCase("url")) kind = TargetKind.URL;
            else if (type.equalsIgnoreCase("message")) kind = TargetKind.MESSAGE;

            ScanOutcome preverified = null;
            if (kind == null) {
                preverified = ScanOutcome.notVerified("INVALID_CSV_ROW",
                        "Unsupported type '" + type + "'. Expected 'url' or 'message'.");
                addWarning(warnings, "Row " + rowNumber + " uses an unsupported type and will stay not verified.");
                invalid++;
            } else if (target.isEmpty()) {
                preverified = ScanOutcome.notVerified("INVALID_CSV_ROW", "The target cell is empty.");
                addWarning(warnings, "Row " + rowNumber + " has an empty target and will stay not verified.");
                invalid++;
            } else {
                valid++;
            }
            rows.add(new LoadedRow(rowNumber, kind, target, preverified));
        }

        if (rows.isEmpty()) {
            throw new LoadException("CSV has no data rows.");
        }
        return new CsvLoadReport(fileName, rows, valid, invalid, warnings);
    }

    private static void addWarning(List<String> warnings, String warning) {
        if (warnings.size() < MAX_WARNINGS) warnings.add(warning);
    }

    /** Minimal CSV line parser: supports commas and escaped quotes inside quoted cells. */
    static List<String> parseLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                cells.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString().trim());
        return cells;
    }
}
