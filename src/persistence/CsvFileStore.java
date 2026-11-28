package persistence;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CsvFileStore {

    private static final char BOM = '\uFEFF';
    private static final String TEMP_SUFFIX = ".tmp";
    private final Path path;
    private final String[] header;
    public CsvFileStore(String filePath, String... header) {
        this.path = Paths.get(Objects.requireNonNull(filePath, "filePath"));
        this.header = Objects.requireNonNull(header, "header").clone();
    }

    public String getFilePath() {
        return path.toString();
    }

    // =====================================================================
    // Reading
    // =====================================================================

    public List<String[]> readAll() {
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }

        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("Cannot read '" + path + "': " + describe(e), e);
        }

        List<String[]> records;
        try {
            records = parse(text);
        } catch (PersistenceException e) {
            throw new PersistenceException("'" + path + "' is not valid CSV, " + e.getMessage(), e);
        }

        if (header.length > 0 && !records.isEmpty() && isHeader(records.get(0))) {
            records.remove(0);
        }
        return records;
    }

    private boolean isHeader(String[] first) {
        return first.length > 0 && first[0].trim().equalsIgnoreCase(header[0]);
    }

    static List<String[]> parse(String text) {
        if (!text.isEmpty() && text.charAt(0) == BOM) {
            text = text.substring(1);
        }

        List<String[]> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quoted = false;
        boolean closed = false;
        int line = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || (c == '\r' && !(i + 1 < text.length() && text.charAt(i + 1) == '\n'))) {
                line++;
            }

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                        closed = true;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                if (quoted || field.length() > 0) {
                    throw new PersistenceException("line " + line + ": unexpected quote character");
                }
                inQuotes = true;
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                quoted = false;
                closed = false;
            } else if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                // first half of a CRLF line ending; the '\n' that follows ends the record
            } else if (c == '\n' || c == '\r') {   // a lone '\r' is an old-Mac line ending
                boolean blankLine = fields.isEmpty() && field.length() == 0 && !quoted;
                if (!blankLine) {
                    fields.add(field.toString());
                    records.add(fields.toArray(new String[0]));
                }
                fields.clear();
                field.setLength(0);
                quoted = false;
                closed = false;
            } else {
                if (closed) {
                    throw new PersistenceException("line " + line + ": text after a closing quote");
                }
                field.append(c);
            }
        }

        if (inQuotes) {
            throw new PersistenceException("line " + line + ": a quoted field is never closed");
        }
        if (!fields.isEmpty() || field.length() > 0 || quoted) {   // last line without a line break
            fields.add(field.toString());
            records.add(fields.toArray(new String[0]));
        }
        return records;
    }

    // =====================================================================
    // Writing
    // =====================================================================

    public void writeAll(List<String[]> records) {
        List<String> lines = new ArrayList<>(records.size() + 1);
        if (header.length > 0) {
            lines.add(toLine(header));
        }
        for (String[] record : records) {
            lines.add(toLine(record));
        }

        Path temp = path.resolveSibling(path.getFileName() + TEMP_SUFFIX);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(temp, lines, StandardCharsets.UTF_8);
            moveIntoPlace(temp);
        } catch (IOException e) {
            throw new PersistenceException("Cannot write '" + path + "': " + describe(e), e);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // nothing useful can be done; the real file is already consistent
            }
        }
    }

    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);   // rare file systems
        }
    }

    private static String toLine(String[] record) {
        if (record.length == 0) {
            throw new IllegalArgumentException("A CSV record needs at least one field");
        }

        if (record.length == 1 && (record[0] == null || record[0].isEmpty())) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < record.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(record[i]));
        }
        return sb.toString();
    }

    private static String escape(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuotes = field.indexOf(',') >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;
        return needsQuotes ? "\"" + field.replace("\"", "\"\"") + "\"" : field;
    }

    private static String describe(IOException e) {
        if (e instanceof CharacterCodingException) {
            return "the file is not valid UTF-8 text";
        }
        if (e instanceof AccessDeniedException) {
            return "access denied (is the file read-only or open in another program?)";
        }
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
