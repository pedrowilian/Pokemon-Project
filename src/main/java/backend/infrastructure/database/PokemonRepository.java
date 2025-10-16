package backend.infrastructure.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import backend.domain.model.Pokemon;
import backend.domain.service.IPokemonRepository;

/**
 * Pokemon repository implementation using SQLite
 * Reuses existing database queries from the GUI layer
 */
public class PokemonRepository implements IPokemonRepository {
    private final Connection connection;

    public PokemonRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Pokemon> findAll() throws SQLException {
        String sql = "SELECT * FROM pokedex ORDER BY id";
        return executePokemonQuery(sql);
    }

    @Override
    public Pokemon findById(int id) throws SQLException {
        String sql = "SELECT * FROM pokedex WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPokemon(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Pokemon> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM pokedex WHERE LOWER(name) LIKE ? ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "%" + name.toLowerCase() + "%");
            return executePreparedQuery(ps);
        }
    }

    @Override
    public List<Pokemon> findWithFilters(String filters, List<Object> params) throws SQLException {
        String sql = "SELECT * FROM pokedex";
        if (filters != null && !filters.isEmpty()) {
            sql += " WHERE " + filters;
        }
        sql += " ORDER BY id";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return executePreparedQuery(ps);
        }
    }

    @Override
    public List<Pokemon> findRandom(int count) throws SQLException {
        String sql = "SELECT * FROM pokedex ORDER BY RANDOM() LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, count);
            return executePreparedQuery(ps);
        }
    }

    @Override
    public List<Pokemon> findByGeneration(int generation) throws SQLException {
        String sql = "SELECT * FROM pokedex WHERE generation = ? ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, generation);
            return executePreparedQuery(ps);
        }
    }

    @Override
    public List<Pokemon> findByType(String type) throws SQLException {
        String sql = "SELECT * FROM pokedex WHERE type1 = ? OR type2 = ? ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, type);
            return executePreparedQuery(ps);
        }
    }

    @Override
    public AttributeMaxValues getMaxAttributeValues() throws SQLException {
        String sql = "SELECT MAX(HP) as maxHP, MAX(Attack) as maxAttack, MAX(Defense) as maxDefense, " +
                    "MAX(SpAtk) as maxSpAtk, MAX(SpDef) as maxSpDef, MAX(Speed) as maxSpeed FROM pokedex";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new AttributeMaxValues(
                    rs.getInt("maxHP"),
                    rs.getInt("maxAttack"),
                    rs.getInt("maxDefense"),
                    rs.getInt("maxSpAtk"),
                    rs.getInt("maxSpDef"),
                    rs.getInt("maxSpeed")
                );
            }
        }
        return new AttributeMaxValues(0, 0, 0, 0, 0, 0);
    }

    @Override
    public List<String> getAllTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT Type1 FROM pokedex UNION SELECT DISTINCT Type2 FROM pokedex WHERE Type2 IS NOT NULL ORDER BY 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                types.add(rs.getString(1));
            }
        }
        return types;
    }

    private List<Pokemon> executePokemonQuery(String sql) throws SQLException {
        List<Pokemon> pokemonList = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                pokemonList.add(mapResultSetToPokemon(rs));
            }
        }
        return pokemonList;
    }

    private List<Pokemon> executePreparedQuery(PreparedStatement ps) throws SQLException {
        List<Pokemon> pokemonList = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pokemonList.add(mapResultSetToPokemon(rs));
            }
        }
        return pokemonList;
    }

    private Pokemon mapResultSetToPokemon(ResultSet rs) throws SQLException {
        return new Pokemon(
            rs.getInt("ID"),
            rs.getString("Name"),
            rs.getString("Form"),
            rs.getString("Type1"),
            rs.getString("Type2"),
            rs.getInt("Total"),
            rs.getInt("HP"),
            rs.getInt("Attack"),
            rs.getInt("Defense"),
            rs.getInt("SpAtk"),
            rs.getInt("SpDef"),
            rs.getInt("Speed"),
            rs.getInt("Generation")
        );
    }
}
