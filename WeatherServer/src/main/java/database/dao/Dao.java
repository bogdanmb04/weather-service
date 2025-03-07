package database.dao;

/**
 * CREATE
 * READ
 * UPDATE
 * DELETE
 */

public interface Dao <T, U, V>{
    public void create(T object);
    public void update(T object);
    public void delete(T object);
    public U find(V object);
}
