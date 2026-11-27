package persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

public abstract class AbstractCsvRepository<T> implements Repository<T, Integer> {


    private final String entityName;
    private final CsvFileStore store;
    private final CsvRecordMapper<T> mapper;
    private final ToIntFunction<T> idOf;
    private Map<Integer, T> entities;

    protected AbstractCsvRepository(String entityName, CsvFileStore store,
                                    CsvRecordMapper<T> mapper, ToIntFunction<T> idOf) {
        this.entityName = Objects.requireNonNull(entityName, "entityName");
        this.store = Objects.requireNonNull(store, "store");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.idOf = Objects.requireNonNull(idOf, "idOf");
        this.entities = load();
    }

    // ---------------------------------------------------------------- queries

    @Override
    public List<T> findAll() {
        return List.copyOf(entities.values());
    }

    @Override
    public T findById(Integer id) {
        return entities.get(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return entities.containsKey(id);
    }

    protected final Collection<T> all() {
        return Collections.unmodifiableCollection(entities.values());
    }

    // ---------------------------------------------------------------- changes

    @Override
    public void save(T entity) {
        Objects.requireNonNull(entity, "entity");
        int id = idOf.applyAsInt(entity);
        if (entities.containsKey(id)) {
            throw new IllegalArgumentException(entityName + " ID " + id + " already exists");
        }
        Map<Integer, T> next = new LinkedHashMap<>(entities);
        next.put(id, entity);
        commit(next);
    }

    @Override
    public void update(T entity) {
        Objects.requireNonNull(entity, "entity");
        int id = idOf.applyAsInt(entity);
        if (!entities.containsKey(id)) {
            throw new IllegalArgumentException(entityName + " ID " + id + " does not exist");
        }
        Map<Integer, T> next = new LinkedHashMap<>(entities);
        next.put(id, entity);
        commit(next);
    }

    @Override
    public void deleteById(Integer id) {
        if (!entities.containsKey(id)) {
            throw new IllegalArgumentException(entityName + " ID " + id + " does not exist");
        }
        Map<Integer, T> next = new LinkedHashMap<>(entities);
        next.remove(id);
        commit(next);
    }

    private void commit(Map<Integer, T> next) {
        List<String[]> records = new ArrayList<>(next.size());
        for (T entity : next.values()) {
            records.add(mapper.toRecord(entity));
        }
        store.writeAll(records);
        entities = next;
    }

    // ---------------------------------------------------------------- loading

    private Map<Integer, T> load() {
        Map<Integer, T> loaded = new LinkedHashMap<>();
        int recordNumber = 0;
        for (String[] record : store.readAll()) {
            recordNumber++;
            T entity;
            try {
                entity = mapper.fromRecord(record);
            } catch (RuntimeException e) {
                throw new PersistenceException(where(recordNumber) + e.getMessage(), e);
            }
            int id = idOf.applyAsInt(entity);
            if (loaded.putIfAbsent(id, entity) != null) {
                throw new PersistenceException(where(recordNumber)
                        + "duplicate " + entityName + " ID " + id);
            }
        }
        return loaded;
    }

    private String where(int recordNumber) {
        return "'" + store.getFilePath() + "', record " + recordNumber + ": ";
    }
}
