package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MemberPaginationQueryTest {


    @ParameterizedTest
    @NullSource
    @EnumSource(SortDirection.class)
    void shouldKeepMembersInStableOrderAcrossPages(SortDirection direction) throws Exception {
        var query = new MemberQueryBuilder(mock(UserMapper.class)).buildMemberListQuery(
                OrgMemberListParamsBO.builder().orgId(1L).orderDir(direction).build());
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("member-pagination", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite::memory:", null)));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(OrganizationMemberMapper.class);
        // 在内存库执行实际 Mapper 分页，避免测试只检查 SQL 字符串。
        try (var session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession();
             var statement = session.getConnection().createStatement()) {
            var connection = session.getConnection();
            statement.execute("CREATE TABLE organization_member (id BIGINT PRIMARY KEY, org_id BIGINT, joined_at TEXT, deleted_at TEXT)");
            try (var insert = connection.prepareStatement("INSERT INTO organization_member (id, org_id, joined_at) VALUES (?, ?, ?)")) {
                for (long id = 1; id <= 44; id++) {
                    insert.setLong(1, id);
                    insert.setLong(2, id == 44 ? 2L : 1L);
                    insert.setString(3, id == 1 ? "2026-01-02 12:00:00" : "2026-01-01 12:00:00");
                    insert.executeUpdate();
                }
            }
            boolean ascending = direction == SortDirection.ASC;
            List<Long> expected = new ArrayList<>(LongStream.rangeClosed(2, 43)
                    .map(id -> ascending ? id : 45 - id).boxed().toList());
            expected.add(ascending ? expected.size() : 0, 1L);
            List<Long> actual = new ArrayList<>();
            var mapper = session.getMapper(OrganizationMemberMapper.class);
            query.select(OrganizationMember::getId);
            for (int pageNum = 1; pageNum <= 6; pageNum++) {
                var page = mapper.selectPage(new Page<>(pageNum, 10), query);
                List<Long> ids = page.getRecords().stream().map(OrganizationMember::getId).toList();
                int start = Math.min((pageNum - 1) * 10, 43);
                assertThat(page.getTotal()).isEqualTo(43);
                assertThat(ids).containsExactlyElementsOf(expected.subList(start, Math.min(start + 10, 43)));
                actual.addAll(ids);
            }
            assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
        }
    }
}
