package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.handler.CodeEnumTypeHandler;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminJoinInviteCodeResultBO;
import online.longlian.common.enumeration.OTPStatus;
import online.longlian.common.enumeration.OTPType;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JoinInviteCodeQueryTest {

    @Test
    void shouldPageUnexpiredCodesAndDeriveExhaustedStatus() throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("join-invite-page", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite::memory:", null)));
        configuration.setDefaultEnumTypeHandler(CodeEnumTypeHandler.class);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.getTypeHandlerRegistry().register(LocalDateTime.class, SqliteLocalDateTimeTypeHandler.class);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(OrganizationJoinOtpMapper.class);

        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 12, 0);
        try (var session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession()) {
            Connection connection = session.getConnection();
            connection.createStatement().execute("""
                    CREATE TABLE organization_join_otp (
                        id INTEGER PRIMARY KEY,
                        otp_id INTEGER NOT NULL,
                        org_id INTEGER NOT NULL,
                        deleted_at TEXT
                    )
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE one_time_password (
                        id INTEGER PRIMARY KEY,
                        code TEXT NOT NULL,
                        expired_at TEXT,
                        max_uses INTEGER,
                        use_count INTEGER,
                        biz_type INTEGER NOT NULL,
                        status INTEGER NOT NULL,
                        created_at TEXT
                    )
                    """);
            insert(connection, 201L, 301L, "OLD001", 1L, now.minusDays(3), now.plusDays(1), 0, null, 0, null);
            insert(connection, 102L, 102L, "NEW001", 1L, now.minusDays(2), now.plusDays(1), 1, 3, 0, null);
            insert(connection, 103L, 103L, "FULL01", 1L, now.minusDays(1), now.plusDays(1), 2, 2, 0, null);
            insert(connection, 104L, 104L, "GONE01", 1L, now.minusHours(1), now.minusMinutes(1), 0, null, 0, null);
            insert(connection, 105L, 105L, "OTHER1", 2L, now, now.plusDays(1), 0, null, 0, null);
            insert(connection, 106L, 106L, "DROP01", 1L, now.plusHours(1), now.plusDays(1), 0, null, 0, now);

            OrganizationJoinOtpMapper mapper = session.getMapper(OrganizationJoinOtpMapper.class);
            Page<OrgAdminJoinInviteCodeResultBO> first = mapper.selectActiveJoinInviteCodePage(
                    new Page<>(1, 2), 1L, null, OTPStatus.USED, OTPType.OrganizationUserInvite, now);
            Page<OrgAdminJoinInviteCodeResultBO> second = mapper.selectActiveJoinInviteCodePage(
                    new Page<>(2, 2), 1L, null, OTPStatus.USED, OTPType.OrganizationUserInvite, now);
            Page<OrgAdminJoinInviteCodeResultBO> pending = mapper.selectActiveJoinInviteCodePage(
                    new Page<>(1, 10), 1L, OTPStatus.PENDING, OTPStatus.USED, OTPType.OrganizationUserInvite, now);
            Page<OrgAdminJoinInviteCodeResultBO> used = mapper.selectActiveJoinInviteCodePage(
                    new Page<>(1, 10), 1L, OTPStatus.USED, OTPStatus.USED, OTPType.OrganizationUserInvite, now);

            assertThat(first.getTotal()).isEqualTo(3);
            assertThat(codes(first)).containsExactly("FULL01", "NEW001");
            assertThat(first.getRecords().get(0).getStatus()).isEqualTo("USED");
            assertThat(first.getRecords().get(0).getUseCount()).isEqualTo(2);
            assertThat(first.getRecords().get(1).getStatus()).isEqualTo("PENDING");
            assertThat(second.getTotal()).isEqualTo(3);
            assertThat(codes(second)).containsExactly("OLD001");
            assertThat(second.getRecords().get(0).getId()).isEqualTo(201L);
            assertThat(codes(pending)).containsExactly("NEW001", "OLD001");
            assertThat(codes(used)).containsExactly("FULL01");
        }
    }

    private static List<String> codes(Page<OrgAdminJoinInviteCodeResultBO> page) {
        return page.getRecords().stream().map(OrgAdminJoinInviteCodeResultBO::getInviteCode).toList();
    }

    private static void insert(Connection connection, long id, long otpId, String code, long orgId,
                                LocalDateTime createdAt, LocalDateTime expiredAt, int useCount, Integer maxUses,
                                int status, LocalDateTime deletedAt) throws Exception {
        try (PreparedStatement otp = connection.prepareStatement(
                "INSERT INTO one_time_password (id, code, expired_at, max_uses, use_count, biz_type, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, 2, ?, ?)")) {
            otp.setLong(1, otpId);
            otp.setString(2, code);
            otp.setString(3, SqliteLocalDateTimeTypeHandler.format(expiredAt));
            if (maxUses == null) {
                otp.setNull(4, Types.INTEGER);
            } else {
                otp.setInt(4, maxUses);
            }
            otp.setInt(5, useCount);
            otp.setInt(6, status);
            otp.setString(7, SqliteLocalDateTimeTypeHandler.format(createdAt));
            otp.executeUpdate();
        }
        try (PreparedStatement join = connection.prepareStatement(
                "INSERT INTO organization_join_otp (id, otp_id, org_id, deleted_at) VALUES (?, ?, ?, ?)")) {
            join.setLong(1, id);
            join.setLong(2, otpId);
            join.setLong(3, orgId);
            if (deletedAt == null) {
                join.setNull(4, Types.VARCHAR);
            } else {
                join.setString(4, SqliteLocalDateTimeTypeHandler.format(deletedAt));
            }
            join.executeUpdate();
        }
    }

    /**
     * SQLite 没有 DATETIME 类型。测试按 MariaDB 的文本比较语义读写，避免驱动把时间写成毫秒。
     */
    public static class SqliteLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        static String format(LocalDateTime value) {
            return value.format(FORMATTER);
        }

        @Override
        public void setNonNullParameter(PreparedStatement ps, int index, LocalDateTime parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setString(index, format(parameter));
        }

        @Override
        public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return parse(rs.getString(columnName));
        }

        @Override
        public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return parse(rs.getString(columnIndex));
        }

        @Override
        public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            return parse(cs.getString(columnIndex));
        }

        private static LocalDateTime parse(String value) {
            return value == null ? null : LocalDateTime.parse(value, FORMATTER);
        }
    }
}
