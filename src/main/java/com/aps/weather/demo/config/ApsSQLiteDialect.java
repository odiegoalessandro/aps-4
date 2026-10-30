package com.aps.weather.demo.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.community.dialect.identity.SQLiteIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

/**
 * Contorna um bug do exportador de DDL do Hibernate 7.4 com o SQLiteDialect.
 *
 * Ao gerar o CREATE TABLE, o Hibernate verifica se o enunciado montado até ali
 * já contém a string do identity ("integer") para não repeti-la. Essa busca é
 * feita no CREATE TABLE inteiro, não apenas na coluna atual. Assim, quando uma
 * coluna anterior também é "integer" (ex.: humidity, weather_code), a coluna de
 * id acaba emitida sem tipo ("id"), o que no SQLite vira afinidade BLOB e deixa
 * de ser alias de rowid: o id é gravado como NULL e as linhas voltam nulas ao
 * serem lidas.
 *
 * Devolver "INTEGER" em caixa alta evita o falso positivo (a comparação
 * normaliza só o enunciado para minúsculas) e mantém o tipo exato exigido pelo
 * SQLite para auto-incremento via rowid.
 */
public class ApsSQLiteDialect extends SQLiteDialect {

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new SQLiteIdentityColumnSupport() {
            @Override
            public String getIdentityColumnString(int type) {
                return "INTEGER";
            }
        };
    }

    /**
     * As PKs são declaradas {@code INTEGER} (ver comentário acima), mas o mapeamento
     * {@code Long} espera {@code BIGINT}. O dialeto base considera
     * {@code equivalentTypes(INTEGER, BIGINT)} verdadeiro, porém não a recíproca
     * (só trata TINYINT/SMALLINT/INTEGER como conversíveis para tipos maiores). A
     * validação de schema chama {@code equivalentTypes(BIGINT, INTEGER)} e recusaria
     * um banco criado pelas migrações. Aceitar a direção inversa mantém
     * {@code ddl-auto=validate} útil sem trocar a grafia exigida pelo rowid.
     *
     * A equivalência vale para o dialeto inteiro, e não só para PKs, mas no SQLite
     * isso é seguro: INTEGER e BIGINT são o mesmo inteiro de 64 bits na prática.
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
