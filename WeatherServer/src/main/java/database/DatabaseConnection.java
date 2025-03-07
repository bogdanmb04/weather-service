package database;

import lombok.Getter;

import javax.persistence.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public class DatabaseConnection {

    @PersistenceUnit
    private EntityManager entityManager;
    private EntityManagerFactory entityManagerFactory;

    public DatabaseConnection() {
        this.initTransaction();
    }

    public void initTransaction() {
        try
        {
            this.entityManagerFactory = Persistence.createEntityManagerFactory("weatherAppPersistenceUnit");
            this.entityManager = entityManagerFactory.createEntityManager();
        } catch (Exception ex) {
            System.err.println("Database init error " + ex.getMessage());
        }
    }

    public void executeTransaction(Consumer<EntityManager> action) {
        EntityTransaction entityTransaction = this.entityManager.getTransaction();
        try {
            entityTransaction.begin();
            action.accept(this.entityManager);
            entityTransaction.commit();
        } catch (RuntimeException e) {
            System.err.println("Transaction error " + e.getMessage());
            entityTransaction.rollback();
        }
    }

    public <T, R> R executeReturnTransaction(Function<EntityManager, T> action, Class<R> result) {
        EntityTransaction entityTransaction = this.entityManager.getTransaction();
        Object query = null;
        try {
            entityTransaction.begin();
            query = action.apply(this.entityManager);
            entityTransaction.commit();
        } catch (RuntimeException e) {
            System.err.println("Transaction error " + e.getMessage());
            entityTransaction.rollback();
        }

        return (R) query;
    }

    public void close()
    {
        entityManager.close();
        entityManagerFactory.close();
    }
}
