package database.dao;

import database.DatabaseConnection;
import database.entities.AppUser;

import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Optional;

public class AppUserDao implements Dao<AppUser, Optional<AppUser>, String> {

    DatabaseConnection connection = new DatabaseConnection();

    @Override
    public void create(AppUser appUser) {
       connection.executeTransaction(entityManager -> {entityManager.persist(appUser);});
    }

    @Override
    public Optional<AppUser> find(String username) {
        AppUser user = connection.executeReturnTransaction(entityManager -> {
            TypedQuery<AppUser> query = entityManager.createQuery(
                    "SELECT user FROM AppUser user WHERE user.username = :username", AppUser.class);
            query.setParameter("username", username);
            try {
                return query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        }, AppUser.class);
        return Optional.ofNullable(user);
    }

    @Override
    public void delete(AppUser user) {
        AppUser managedUser = connection.getEntityManager().merge(user);
        connection.executeTransaction(entityManager -> {entityManager.remove(managedUser);});
    }

    @Override
    public void update(AppUser user) {
        connection.executeTransaction(entityManager -> {entityManager.merge(user);});
    }

    public Optional<AppUser> getUserByName(String username) {
        EntityTransaction transaction = connection.getEntityManager().getTransaction();
        try
        {
            connection.getEntityManager().getTransaction().begin();
            TypedQuery<AppUser> query = connection.getEntityManager().createQuery("SELECT user FROM AppUser user WHERE user.username = :username", AppUser.class);
            query.setParameter("username", username);
            transaction.commit();
            return Optional.of(query.getSingleResult());
        } catch (RuntimeException e)
        {
            System.err.println("Failed transaction " + e.getMessage());
            transaction.rollback();
        }
        return Optional.empty();
    }
}
