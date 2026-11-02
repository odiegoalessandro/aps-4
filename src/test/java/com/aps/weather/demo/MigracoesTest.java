package com.aps.weather.demo;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aplica V2 e V3 sobre bancos povoados com duplicados, como os que existem em produção.
 * Os dados são inseridos com JDBC puro, na versão anterior à migração testada.
 */
class MigracoesTest {

    private static final String PREVISAO = """
            INSERT INTO weather_forecasts
                (id, location_id, forecast_at, temperature, apparent_temperature, humidity,
                 precipitation, weather_code, wind_speed, wind_direction, pressure, cloud_cover,
                 updated_at)
            VALUES (%d, %d, '%s', 20, 20, 50, 0, 3, 5, 90, 1010, 10, '%s')
            """;

    @TempDir
    Path pasta;

    @Test
    void v2ColapsaDuplicadosMantendoOMaiorIdECriaIndiceUnico() throws Exception {
        String url = migrarAte("1");

        executar(url,
                "INSERT INTO locations (id, city, latitude, longitude, timezone)"
                        + " VALUES (1, 'Cidade', -23.5, -46.6, 'UTC')",
                previsao(1, 1, "2026-01-01 00:00:00"),
                previsao(2, 1, "2026-01-01 00:00:00"));

        migrarTudo(url);

        assertThat(contar(url, "SELECT COUNT(*) FROM weather_forecasts")).isEqualTo(1);
        assertThat(contar(url, "SELECT id FROM weather_forecasts")).isEqualTo(2);

        assertViolacaoDeUnicidade(url, previsao(3, 1, "2026-01-01 00:00:00"));
        assertThat(contar(url, "SELECT COUNT(*) FROM weather_forecasts")).isEqualTo(1);
    }

    @Test
    void v3ColapsaCidadesRepointaPrevisoesENaoDeixaOrfaos() throws Exception {
        String url = migrarAte("2");

        executar(url,
                "INSERT INTO locations (id, city, latitude, longitude, timezone)"
                        + " VALUES (10, 'São Paulo', -23.5, -46.6, 'America/Sao_Paulo')",
                "INSERT INTO locations (id, city, latitude, longitude, timezone)"
                        + " VALUES (11, 'são paulo', -23.5, -46.6, 'America/Sao_Paulo')",
                // mesmo horário nas duas grafias: após o repontamento vira duplicado e sobra uma
                previsao(100, 10, "2026-01-01 00:00:00"),
                previsao(101, 11, "2026-01-01 00:00:00"),
                // previsão exclusiva do perdedor: precisa ser repontada, não apagada
                previsao(102, 11, "2026-01-01 01:00:00"));

        migrarTudo(url);

        assertThat(contar(url, "SELECT COUNT(*) FROM locations")).isEqualTo(1);
        assertThat(texto(url, "SELECT city FROM locations")).isEqualTo("São Paulo");
        assertThat(contar(url, "SELECT COUNT(*) FROM weather_forecasts")).isEqualTo(2);
        assertThat(contar(url, "SELECT COUNT(*) FROM weather_forecasts WHERE location_id = 10"))
                .isEqualTo(2);
        assertThat(contar(url, """
                SELECT COUNT(*) FROM weather_forecasts
                WHERE location_id NOT IN (SELECT id FROM locations)
                """)).isZero();

        assertViolacaoDeUnicidade(url,
                "INSERT INTO locations (id, city, city_normalizada, latitude, longitude, timezone)"
                        + " VALUES (12, 'são paulo', 'sao paulo', -23.5, -46.6, 'America/Sao_Paulo')");
    }

    @Test
    void v4ColapsaCidadesQueDiferemSoPeloAcentoENormaliza() throws Exception {
        String url = migrarAte("3");

        executar(url,
                "INSERT INTO locations (id, city, latitude, longitude, timezone)"
                        + " VALUES (20, 'Brasília', -15.79, -47.88, 'America/Sao_Paulo')",
                "INSERT INTO locations (id, city, latitude, longitude, timezone)"
                        + " VALUES (21, 'BRASILIA', -15.79, -47.88, 'America/Sao_Paulo')",
                previsao(200, 20, "2026-01-01 00:00:00"),
                previsao(201, 21, "2026-01-01 00:00:00"));

        migrarTudo(url);

        assertThat(contar(url, "SELECT COUNT(*) FROM locations")).isEqualTo(1);
        assertThat(texto(url, "SELECT city FROM locations")).isEqualTo("Brasília");
        assertThat(texto(url, "SELECT city_normalizada FROM locations")).isEqualTo("brasilia");
        assertThat(contar(url, "SELECT COUNT(*) FROM weather_forecasts")).isEqualTo(1);
        // A previsão da cidade perdedora foi repontada para a vencedora (id 20)
        assertThat(contar(url, "SELECT location_id FROM weather_forecasts")).isEqualTo(20);

        assertViolacaoDeUnicidade(url,
                "INSERT INTO locations (id, city, city_normalizada, latitude, longitude, timezone)"
                        + " VALUES (22, 'BRASÍLIA', 'brasilia', -15.79, -47.88, 'UTC')");
    }

    @Test
    void v5RecusaCidadeSemCidadeNormalizada() throws Exception {
        String url = migrarAte("5");

        assertThatThrownBy(() -> executar(url,
                "INSERT INTO locations (id, city, latitude, longitude, timezone)"
                        + " VALUES (30, 'Cidade', -1.0, -2.0, 'UTC')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("city_normalizada deve ser preenchida");
    }

    @Test
    void v6CriaPrevisaoHorariaComUnicidadeEPresencaDeLocalizacao() throws Exception {
        String url = migrarAte("5");
        executar(url, localizacao(40));
        migrarTudo(url);

        String horaria = """
                INSERT INTO hourly_forecasts (location_id, forecast_at, temperature, updated_at)
                VALUES (%d, '2026-11-02 00:00:00', 20, '2026-11-02 00:00:00')
                """;

        executar(url, horaria.formatted(40));

        assertThat(contar(url, "SELECT COUNT(*) FROM hourly_forecasts")).isEqualTo(1);
        assertViolacaoDeUnicidade(url, horaria.formatted(40));
        assertThatThrownBy(() -> executar(url, horaria.formatted(999)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }

    @Test
    void v6CriaPrevisaoDiariaComUnicidadeEPresencaDeLocalizacao() throws Exception {
        String url = migrarAte("5");
        executar(url, localizacao(41));
        migrarTudo(url);

        String diaria = """
                INSERT INTO daily_forecasts
                    (location_id, forecast_date, min_temperature, max_temperature, updated_at)
                VALUES (%d, '2026-11-02', 18, 27, '2026-11-02 00:00:00')
                """;

        executar(url, diaria.formatted(41));

        assertThat(contar(url, "SELECT COUNT(*) FROM daily_forecasts")).isEqualTo(1);
        assertViolacaoDeUnicidade(url, diaria.formatted(41));
        assertThatThrownBy(() -> executar(url, diaria.formatted(999)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }

    private static String localizacao(long id) {
        return """
                INSERT INTO locations (id, city, city_normalizada, latitude, longitude, timezone)
                VALUES (%d, 'Cidade %d', 'cidade %d', -1.0, -2.0, 'UTC')
                """.formatted(id, id, id);
    }

    private String migrarAte(String versao) {
        String url = urlDoBanco(versao);
        // Produção roda com PRAGMA foreign_keys=ON (connection-init-sql do Hikari);
        // as migrações precisam ser exercitadas com a mesma semântica.
        Flyway.configure()
                .dataSource(url, null, null)
                .initSql("PRAGMA foreign_keys=ON")
                .target(versao)
                .load()
                .migrate();
        return url;
    }

    private static void migrarTudo(String url) {
        Flyway.configure()
                .dataSource(url, null, null)
                .initSql("PRAGMA foreign_keys=ON")
                .load()
                .migrate();
    }

    private String urlDoBanco(String nome) {
        return "jdbc:sqlite:" + pasta.resolve("migracao-" + nome + ".db").toAbsolutePath();
    }

    private static String previsao(long id, long locationId, String horario) {
        return PREVISAO.formatted(id, locationId, horario, horario);
    }

    private static void executar(String url, String... comandos) throws SQLException {
        try (Connection conexao = DriverManager.getConnection(url);
             Statement statement = conexao.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            for (String comando : comandos) {
                statement.executeUpdate(comando);
            }
        }
    }

    private static void assertViolacaoDeUnicidade(String url, String comando) {
        assertThatThrownBy(() -> executar(url, comando))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("UNIQUE constraint failed");
    }

    private static long contar(String url, String sql) throws SQLException {
        try (Connection conexao = DriverManager.getConnection(url);
             Statement statement = conexao.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {
            assertThat(resultado.next()).isTrue();
            return resultado.getLong(1);
        }
    }

    private static String texto(String url, String sql) throws SQLException {
        try (Connection conexao = DriverManager.getConnection(url);
             Statement statement = conexao.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {
            assertThat(resultado.next()).isTrue();
            return resultado.getString(1);
        }
    }
}
