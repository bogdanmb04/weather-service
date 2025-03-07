package database.dao;

import database.DatabaseConnection;
import database.entities.City;
import database.entities.CityWeather;

import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class WeatherDao implements Dao<CityWeather, Optional<CityWeather>, Integer>{

    DatabaseConnection connection = new DatabaseConnection();

    public Optional<List<CityWeather>> getWeather(City city) {
        EntityTransaction transaction = connection.getEntityManager().getTransaction();
        try
        {
            connection.getEntityManager().getTransaction().begin();
            TypedQuery<CityWeather> query = connection.getEntityManager().createQuery("SELECT city_weather FROM CityWeather city_weather WHERE city_weather.city = :city", CityWeather.class);
            query.setParameter("city", city);
            List<CityWeather> cityWeathers = query.getResultList();
            connection.getEntityManager().getTransaction().commit();
            return Optional.of(cityWeathers);
        } catch (RuntimeException e)
        {
            System.err.println("Failed transaction " + e.getMessage());
            transaction.rollback();
        }
        return Optional.empty();
    }

    public Stream<CityWeather> getAllWeathers() {
        return connection.executeReturnTransaction(entityManager ->
                entityManager.createQuery("SELECT w FROM CityWeather w", CityWeather.class).getResultStream(), Stream.class);
    }

    @Override
    public void create(CityWeather weatherForecast) {
        connection.executeTransaction(entityManager -> {entityManager.persist(weatherForecast);});
    }

    @Override
    public void update(CityWeather weatherForecast) {
        connection.executeTransaction(entityManager -> {entityManager.merge(weatherForecast);});
    }

    @Override
    public void delete(CityWeather weatherForecast) {
        CityWeather managedWeather = connection.getEntityManager().merge(weatherForecast);
        connection.executeTransaction(entityManager -> {entityManager.remove(managedWeather);});
    }

    @Override
    public Optional<CityWeather> find(Integer id) {
        WeatherDao weather = connection.executeReturnTransaction(entityManager -> {
            TypedQuery<CityWeather> query = entityManager.createQuery(
                    "SELECT weather FROM CityWeather weather WHERE weather.id = :id", CityWeather.class);
            query.setParameter("id", id);
            try {
                return query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        }, WeatherDao.class);
        return Optional.empty();
    }

    public Optional<CityWeather> findWeatherByCityAndDate(City city, LocalDate date) {
        connection.executeReturnTransaction(entityManager -> {
            TypedQuery<CityWeather> query = entityManager.createQuery(
                    "SELECT weather FROM CityWeather weather " +
                            "WHERE weather.city = :city " +
                            "AND weather.date = :date", CityWeather.class);
            query.setParameter("city", city);
            query.setParameter("date", date);
            try {
                return query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        }, CityWeather.class);
        return Optional.empty();
    }

    public Stream<CityWeather> findWeatherByCity(City city) {
        connection.executeReturnTransaction(entityManager -> {
            TypedQuery<CityWeather> query = entityManager.createQuery(
                    "SELECT weather FROM CityWeather weather WHERE weather.city = :city", CityWeather.class);
            query.setParameter("city", city);
            try {
                return query.getResultStream();
            } catch (NoResultException e) {
                return null;
            }
        }, CityWeather.class);
        return null;
    }
}
