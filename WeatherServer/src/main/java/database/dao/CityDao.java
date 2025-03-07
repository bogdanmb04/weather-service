package database.dao;

import database.DatabaseConnection;
import database.entities.City;
import javafx.util.Pair;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Optional;
import java.util.stream.Stream;

public class CityDao implements Dao<City, Optional<City>, Pair<Double, Double>> {

    DatabaseConnection connection = new DatabaseConnection();

    public Stream<City> getAllCities() {
        return connection.executeReturnTransaction(entityManager ->
                entityManager.createQuery("SELECT c FROM City c", City.class).getResultStream(), Stream.class);
    }

//    public Optional<City> getCityByCoords (Pair<Float, Float> coords) {
//        EntityTransaction transaction = connection.getEntityManager().getTransaction();
//        try {
//            connection.getEntityManager().getTransaction().begin();
//            TypedQuery<City> query = connection.getEntityManager().createQuery("SELECT city FROM City city " +
//                    "WHERE city.latitude = :latitude AND city.longitude = :longitude", City.class);
//            query.setParameter("latitude", coords.getKey());
//            query.setParameter("longitude", coords.getValue());
//            connection.getEntityManager().getTransaction().commit();
//            return Optional.of(query.getSingleResult());
//        } catch (RuntimeException e) {
//            System.err.println("Failed transaction " + e.getMessage());
//            transaction.rollback();
//        }
//        return Optional.empty();
//    }

    @Override
    public void create(City city) {
        connection.executeTransaction(entityManager -> {entityManager.persist(city);});
    }

    @Override
    public void update(City city) {
        connection.executeTransaction(entityManager -> {entityManager.merge(city);});
    }

    @Override
    public void delete(City city) {
        City managedCity = connection.getEntityManager().merge(city);
        connection.executeTransaction(entityManager -> {entityManager.remove(managedCity);});
    }

    @Override
    public Optional<City> find(Pair<Double, Double> coords) {
        City city = connection.executeReturnTransaction(entityManager -> {
            TypedQuery<City> query = entityManager.createQuery(
                    "SELECT city FROM City city WHERE city.latitude = :latitude AND city.longitude = :longitude", City.class);
            query.setParameter("latitude", coords.getKey());
            query.setParameter("longitude", coords.getValue());
            try {
                return query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        }, City.class);
        return Optional.ofNullable(city);
    }

    public Optional<City> getCityByID(Long id) {
        City city = connection.executeReturnTransaction(entityManager -> {
            TypedQuery<City> query = entityManager.createQuery(
                    "SELECT city FROM City city WHERE city.id = :id", City.class);
            query.setParameter("id", id);
            try {
                return query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        }, City.class);
        return Optional.ofNullable(city);
    }

}
