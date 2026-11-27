package persistence;

public interface CsvRecordMapper<T> {

    String[] toRecord(T entity);
    T fromRecord(String[] record);
}
