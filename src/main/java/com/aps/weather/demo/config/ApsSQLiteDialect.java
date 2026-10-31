package com.aps.weather.demo.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

/**
 * Ajustes mínimos ao {@link SQLiteDialect} do Hibernate 7.4. O schema é criado e
 * evoluído pelas migrações do Flyway ({@code ddl-auto=validate}), então esta
 * classe não participa da geração de DDL: ela existe para a validação de schema
 * e para a tradução de erros do SQLite. Ver {@code docs/adr/0002-dialeto-sqlite.md}.
 *
 * <p>Dois contornos permanecem:
 *
 * <p>1. {@link #equivalentTypes}: o SQLite só auto-incrementa uma PK declarada
 * exatamente {@code INTEGER PRIMARY KEY} (alias de rowid); {@code BIGINT} não é
 * alias. As migrações usam {@code INTEGER}, mas o mapeamento {@code Long} espera
 * {@code BIGINT}. O dialeto base considera {@code equivalentTypes(INTEGER, BIGINT)}
 * verdadeiro, porém não a recíproca (só trata TINYINT/SMALLINT/INTEGER como
 * conversíveis para tipos maiores). A validação de schema chama
 * {@code equivalentTypes(BIGINT, INTEGER)} e recusaria um banco criado pelas
 * migrações. Aceitar a direção inversa mantém {@code ddl-auto=validate} útil sem
 * trocar a grafia exigida pelo rowid. A equivalência vale para o dialeto inteiro,
 * e não só para PKs, mas no SQLite isso é seguro: INTEGER e BIGINT são o mesmo
 * inteiro de 64 bits na prática.
 *
 * <p>2. {@link #buildSQLExceptionConversionDelegate}: o SQLiteDialect não reconhece
 * o erro 19 (SQLITE_CONSTRAINT) — o driver xerial devolve SQLState nulo e o
 * delegate padrão trata apenas os códigos 5, 6, 10-15, 18, 20 e 26. Sem isto a
 * violação de unicidade chega ao Spring como JpaSystemException em vez de
 * DataIntegrityViolationException. O código primário do SQLite fica nos 8 bits
 * menos significativos; os códigos estendidos combinam 19 com o detalhe via OR.
 *
 * <p>Houve ainda um override de {@code getIdentityColumnSupport()} para contornar
 * um bug do exportador de DDL do Hibernate (a checagem da string de identity
 * considerava o CREATE TABLE inteiro). Como o Flyway é a fonte do schema e nenhum
 * CREATE TABLE é gerado em runtime, ele era código morto e foi removido.
 */
public class ApsSQLiteDialect extends SQLiteDialect {

    /**
     * Aceita {@code BIGINT → INTEGER} além das equivalências padrão.
     * O motivo está no Javadoc da classe.
     */
    @Override
    public boolean equivalentTypes(int typeCode1, int typeCode2) {
        return super.equivalentTypes(typeCode1, typeCode2)
                || typeCode1 == java.sql.Types.BIGINT && typeCode2 == java.sql.Types.INTEGER;
    }

    /**
     * O SQLiteDialect não reconhece o erro 19 (SQLITE_CONSTRAINT): o driver xerial
     * devolve SQLState nulo e o delegate padrão do dialeto trata apenas os códigos 5,
     * 6, 10-15, 18, 20 e 26. Sem isto a violação de unicidade chega ao Spring como
     * JpaSystemException em vez de DataIntegrityViolationException.
     *
     * O código primário do SQLite fica nos 8 bits menos significativos; os códigos
     * estendidos combinam 19 com o detalhe via OR.
     */
    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        SQLExceptionConversionDelegate padrao = super.buildSQLExceptionConversionDelegate();
        return (sqlException, message, sql) -> {
            if ((sqlException.getErrorCode() & 0xFF) == 19) {
                return new ConstraintViolationException(message, sqlException, sql);
            }
            return padrao == null ? null : padrao.convert(sqlException, message, sql);
        };
    }
}
